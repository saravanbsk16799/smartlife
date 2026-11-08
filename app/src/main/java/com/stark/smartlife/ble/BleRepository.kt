package com.stark.smartlife.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.stark.smartlife.data.AppDatabase
import com.stark.smartlife.data.PairedDevice
import com.stark.smartlife.data.Reading
import com.stark.smartlife.data.ReadingType
import com.stark.smartlife.data.SecureStore
import com.stark.smartlife.data.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque

/**
 * Generic BLE central. It scans, connects, lists every GATT service/characteristic the watch
 * exposes, and subscribes ONLY to the standard Heart Rate and Battery characteristics when
 * the watch actually offers them. It never writes to the watch.
 */
@SuppressLint("MissingPermission") // every entry point checks [hasPermissions] first
class BleRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val secure: SecureStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private val adapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    private val _state = MutableStateFlow(DeviceUiState(deviceName = secure.device.value?.name.orEmpty()))
    val state: StateFlow<DeviceUiState> = _state.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private var lastHrStoredAt = 0L

    // GATT operations must run one at a time.
    private val ops = ArrayDeque<() -> Boolean>()
    private var opRunning = false

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31)
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    val bluetoothOn: Boolean get() = adapter?.isEnabled == true

    // ---------- scanning ----------

    fun startScan() {
        if (!hasPermissions()) return message("permission")
        val scanner = adapter?.bluetoothLeScanner ?: return message("bluetooth_off")
        stopScan()
        _state.update { it.copy(scanning = true, results = emptyList(), message = null) }
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.scanRecord?.deviceName ?: result.device.name ?: return
                val item = ScanResultItem(result.device.address, name, result.rssi)
                _state.update { s ->
                    s.copy(results = (s.results.filterNot { it.address == item.address } + item)
                        .sortedByDescending { it.rssi })
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _state.update { it.copy(scanning = false, message = "scan_failed:$errorCode") }
            }
        }
        scanCallback = cb
        scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), cb)
        main.postDelayed({ stopScan() }, 15_000)
    }

    fun stopScan() {
        val cb = scanCallback ?: return
        if (hasPermissions()) adapter?.bluetoothLeScanner?.stopScan(cb)
        scanCallback = null
        _state.update { it.copy(scanning = false) }
    }

    // ---------- connection ----------

    fun connect(address: String, name: String) {
        if (!hasPermissions()) return message("permission")
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull()
            ?: return message("bluetooth_off")
        stopScan()
        closeGatt()
        secure.saveDevice(PairedDevice(address, name))
        _state.update {
            it.copy(link = LinkState.CONNECTING, deviceName = name, gatt = emptyList(),
                hasHeartRate = false, hasBattery = false, message = null)
        }
        gatt = if (Build.VERSION.SDK_INT >= 23)
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        else device.connectGatt(context, false, callback)
    }

    fun reconnectLast() {
        secure.device.value?.let { connect(it.address, it.name) }
    }

    fun disconnect() {
        gatt?.disconnect()
    }

    fun forget() {
        closeGatt()
        secure.saveDevice(null)
        _state.value = DeviceUiState()
    }

    private fun closeGatt() {
        ops.clear()
        opRunning = false
        gatt?.close()
        gatt = null
    }

    private fun message(code: String) = _state.update { it.copy(message = code) }

    // ---------- GATT ----------

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                _state.update { it.copy(link = LinkState.CONNECTED) }
                g.discoverServices()
            } else {
                closeGatt()
                _state.update {
                    it.copy(
                        link = LinkState.DISCONNECTED,
                        message = if (status != BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_DISCONNECTED)
                            "gatt_error:$status" else it.message,
                    )
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return message("gatt_error:$status")
            val entries = buildList {
                g.services.forEach { s ->
                    add(GattEntry(s.uuid.toString(), null, "", Gatt.isStandard(s.uuid)))
                    s.characteristics.forEach { c ->
                        add(GattEntry(s.uuid.toString(), c.uuid.toString(), describe(c.properties), Gatt.isStandard(c.uuid)))
                    }
                }
            }
            val hr = g.getService(Gatt.HEART_RATE_SERVICE)?.getCharacteristic(Gatt.HEART_RATE_MEASUREMENT)
            val bat = g.getService(Gatt.BATTERY_SERVICE)?.getCharacteristic(Gatt.BATTERY_LEVEL)
            _state.update { it.copy(gatt = entries, hasHeartRate = hr != null, hasBattery = bat != null) }

            if (bat != null) enqueue { g.readCharacteristic(bat) }
            if (bat != null && bat.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) enqueueNotify(g, bat)
            if (hr != null && hr.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) enqueueNotify(g, hr)
        }

        @Deprecated("Used below API 33")
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT < 33 && status == BluetoothGatt.GATT_SUCCESS) handle(c.uuid, c.value ?: ByteArray(0))
            opDone()
        }

        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) handle(c.uuid, value)
            opDone()
        }

        @Deprecated("Used below API 33")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) handle(c.uuid, c.value ?: ByteArray(0))
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            handle(c.uuid, value)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            opDone()
        }
    }

    private fun handle(uuid: java.util.UUID, value: ByteArray) {
        val now = System.currentTimeMillis()
        when (uuid) {
            Gatt.HEART_RATE_MEASUREMENT -> parseHeartRate(value)?.let { bpm ->
                _state.update { it.copy(lastHr = bpm) }
                if (now - lastHrStoredAt >= 10_000) { // keep the database small
                    lastHrStoredAt = now
                    scope.launch { db.readings().insert(Reading(type = ReadingType.HEART_RATE, value = bpm.toDouble(), time = now, source = Source.WATCH_BLE)) }
                }
            }
            Gatt.BATTERY_LEVEL -> value.firstOrNull()?.toInt()?.and(0xFF)?.takeIf { it in 0..100 }?.let { pct ->
                _state.update { it.copy(battery = pct) }
                scope.launch { db.readings().insert(Reading(type = ReadingType.BATTERY, value = pct.toDouble(), time = now, source = Source.WATCH_BLE)) }
            }
        }
    }

    private fun enqueueNotify(g: BluetoothGatt, c: BluetoothGattCharacteristic) = enqueue {
        val cccd = c.getDescriptor(Gatt.CCCD) ?: return@enqueue false
        if (!g.setCharacteristicNotification(c, true)) return@enqueue false
        if (Build.VERSION.SDK_INT >= 33) {
            g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            run {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                g.writeDescriptor(cccd)
            }
        }
    }

    private fun enqueue(op: () -> Boolean) {
        ops.add(op)
        if (!opRunning) next()
    }

    private fun next() {
        val op = ops.poll() ?: run { opRunning = false; return }
        opRunning = true
        if (!op()) next() // failed to start: skip it
    }

    private fun opDone() = main.post { next() }

    private fun describe(p: Int): String = buildList {
        if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("read")
        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("write")
        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) add("write-no-resp")
        if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("notify")
        if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("indicate")
    }.joinToString(", ")
}
