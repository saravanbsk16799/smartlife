package com.stark.smartlife.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stark.smartlife.R
import com.stark.smartlife.aod.AodCapability
import com.stark.smartlife.ble.BleService
import com.stark.smartlife.ble.LinkState
import com.stark.smartlife.ui.MainViewModel
import com.stark.smartlife.ui.components.HudCard
import com.stark.smartlife.ui.components.SectionTitle
import java.text.DateFormat
import java.util.Date

@Composable
fun DeviceScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val dev by vm.device.collectAsState()
    val alerts by vm.alerts.collectAsState()
    var permsOk by remember { mutableStateOf(vm.ble.hasPermissions()) }

    val btLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permsOk = vm.ble.hasPermissions()
        if (permsOk) vm.ble.startScan()
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.device_title), style = MaterialTheme.typography.titleLarge)

        HudCard {
            Text(
                when (dev.link) {
                    LinkState.CONNECTED -> stringResource(R.string.status_connected)
                    LinkState.CONNECTING -> stringResource(R.string.status_connecting)
                    LinkState.DISCONNECTED -> stringResource(R.string.status_disconnected)
                } + if (dev.deviceName.isNotBlank()) " · ${dev.deviceName}" else "",
                style = MaterialTheme.typography.titleMedium,
            )
            dev.battery?.let { Text("${stringResource(R.string.battery)}: $it%") }
            dev.message?.let { code ->
                val text = when {
                    code == "permission" -> stringResource(R.string.bt_permission_needed)
                    code == "bluetooth_off" -> stringResource(R.string.msg_bluetooth_off)
                    code.startsWith("scan_failed") -> stringResource(R.string.msg_scan_failed)
                    else -> stringResource(R.string.msg_gatt_error)
                }
                Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!permsOk) {
                    Button(onClick = { btLauncher.launch(vm.ble.requiredPermissions()) }) { Text(stringResource(R.string.grant_permission)) }
                } else if (dev.link == LinkState.DISCONNECTED) {
                    Button(onClick = { if (dev.scanning) vm.ble.stopScan() else vm.ble.startScan() }) {
                        Text(stringResource(if (dev.scanning) R.string.scanning else R.string.scan))
                    }
                } else {
                    Button(onClick = { BleService.start(ctx) }) { Text(stringResource(R.string.sync_now)) }
                    OutlinedButton(onClick = { vm.ble.disconnect(); BleService.stop(ctx) }) {
                        Text(stringResource(R.string.disconnect))
                    }
                }
            }
        }

        if (dev.results.isNotEmpty() && dev.link == LinkState.DISCONNECTED) {
            HudCard {
                dev.results.forEach { r ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.titleMedium)
                            Text("${r.address} · ${r.rssi} dBm", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { vm.ble.connect(r.address, r.name) }) { Text(stringResource(R.string.connect)) }
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.protocol_unverified_title))
        HudCard { Text(stringResource(R.string.protocol_unverified_body), style = MaterialTheme.typography.bodyMedium) }

        if (dev.gatt.isNotEmpty()) {
            SectionTitle(stringResource(R.string.gatt_inspector))
            HudCard {
                dev.gatt.forEach { g ->
                    if (g.characteristic == null) {
                        Text("S ${g.service}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 6.dp))
                    } else {
                        Text("  C ${g.characteristic}  [${g.properties}]", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.aod_title))
        HudCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.aod_title), Modifier.weight(1f))
                Switch(checked = false, onCheckedChange = null, enabled = AodCapability.supported)
            }
            Text(stringResource(R.string.aod_unsupported_body), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionTitle(stringResource(R.string.watchfaces_title))
        HudCard { Text(stringResource(R.string.watchfaces_body), style = MaterialTheme.typography.bodyMedium) }

        SectionTitle(stringResource(R.string.calling_title))
        HudCard {
            Text(stringResource(R.string.calling_body), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.open_bt_settings)) }
        }

        SectionTitle(stringResource(R.string.notif_title))
        HudCard {
            Text(stringResource(R.string.notif_body), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Build.VERSION.SDK_INT >= 33) {
                    OutlinedButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
                Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                    Text(stringResource(R.string.notif_grant))
                }
            }
            if (alerts.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.notif_recent), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { vm.clearAlerts() }) { Text(stringResource(R.string.notif_clear)) }
                }
                alerts.take(8).forEach { a ->
                    val prefix = if (a.isCall) stringResource(R.string.alert_call) + " · " else ""
                    Text("$prefix${a.app} · ${a.title} · ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(a.time))}",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
