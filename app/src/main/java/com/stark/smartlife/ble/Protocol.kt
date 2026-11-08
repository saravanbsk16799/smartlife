package com.stark.smartlife.ble

import java.util.UUID

/**
 * Only Bluetooth SIG–assigned UUIDs are used here. No vendor (Fastrack/Titan) UUID or
 * command is hard-coded: the Jupiter R2's proprietary protocol is not publicly documented,
 * and an unverified claim found online for a different Fastrack model was deliberately NOT adopted.
 *
 * Vendor characteristics are exposed read-only through the GATT inspector so a developer can
 * verify them against real hardware before any adapter is written.
 */
object Gatt {
    private fun sig(short: Int): UUID =
        UUID.fromString("0000%04x-0000-1000-8000-00805f9b34fb".format(short))

    val HEART_RATE_SERVICE: UUID = sig(0x180D)
    val HEART_RATE_MEASUREMENT: UUID = sig(0x2A37)
    val BATTERY_SERVICE: UUID = sig(0x180F)
    val BATTERY_LEVEL: UUID = sig(0x2A19)
    val CCCD: UUID = sig(0x2902)

    fun isStandard(uuid: UUID): Boolean =
        uuid.toString().endsWith("-0000-1000-8000-00805f9b34fb")
}

/** Decodes the Heart Rate Measurement characteristic (Bluetooth SIG GATT spec, 0x2A37). */
fun parseHeartRate(data: ByteArray): Int? {
    if (data.size < 2) return null
    val flags = data[0].toInt()
    val is16Bit = flags and 0x01 != 0
    return if (is16Bit) {
        if (data.size < 3) null
        else (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
    } else {
        data[1].toInt() and 0xFF
    }.takeIf { it in 20..250 }
}

data class GattEntry(
    val service: String,
    val characteristic: String?,
    val properties: String,
    val standard: Boolean,
)

data class ScanResultItem(val address: String, val name: String, val rssi: Int)

enum class LinkState { DISCONNECTED, CONNECTING, CONNECTED }

data class DeviceUiState(
    val link: LinkState = LinkState.DISCONNECTED,
    val deviceName: String = "",
    val scanning: Boolean = false,
    val results: List<ScanResultItem> = emptyList(),
    val gatt: List<GattEntry> = emptyList(),
    val hasHeartRate: Boolean = false,
    val hasBattery: Boolean = false,
    val lastHr: Int? = null,
    val battery: Int? = null,
    val message: String? = null,
)
