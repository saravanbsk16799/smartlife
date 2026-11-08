package com.stark.smartlife.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stark.smartlife.R
import com.stark.smartlife.ble.LinkState
import com.stark.smartlife.ui.MainViewModel
import com.stark.smartlife.ui.components.ArcReactor
import com.stark.smartlife.ui.components.HudCard
import com.stark.smartlife.ui.components.MetricTile
import com.stark.smartlife.ui.components.SectionTitle
import java.util.Locale

fun fmtInt(v: Double): String = String.format(Locale.getDefault(), "%,d", v.toLong())
fun fmtKm(m: Double): String = String.format(Locale.getDefault(), "%.2f", m / 1000.0)
fun fmtHours(min: Double): String = String.format(Locale.getDefault(), "%.1f", min / 60.0)

@Composable
fun HomeScreen(vm: MainViewModel) {
    val steps by vm.steps.collectAsState()
    val kcal by vm.calories.collectAsState()
    val dist by vm.distanceM.collectAsState()
    val hr by vm.heartRate.collectAsState()
    val spo2 by vm.spo2.collectAsState()
    val sleep by vm.sleepLast.collectAsState()
    val profile by vm.profile.collectAsState()
    val dev by vm.device.collectAsState()
    val noData = stringResource(R.string.no_data)

    val progress = (steps / profile.stepGoal.coerceAtLeast(1)).toFloat()
    val anyData = steps > 0 || hr != null || spo2 != null || sleep != null

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (profile.name.isNotBlank()) "${stringResource(R.string.jarvis_title)} · ${profile.name}"
            else stringResource(R.string.jarvis_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            ArcReactor(
                progress = progress,
                centerText = fmtInt(steps),
                caption = "${stringResource(R.string.steps)} / ${fmtInt(profile.stepGoal.toDouble())}",
            )
        }
        if (!anyData) {
            HudCard { Text(stringResource(R.string.jarvis_no_data), style = MaterialTheme.typography.bodyMedium) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(stringResource(R.string.calories), if (kcal > 0) fmtInt(kcal) else noData,
                stringResource(R.string.unit_kcal), Modifier.weight(1f))
            MetricTile(stringResource(R.string.distance), if (dist > 0) fmtKm(dist) else noData,
                stringResource(R.string.unit_km), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(stringResource(R.string.heart_rate), hr?.let { fmtInt(it.value) } ?: noData,
                stringResource(R.string.unit_bpm), Modifier.weight(1f))
            MetricTile(stringResource(R.string.spo2), spo2?.let { "${fmtInt(it.value)}%" } ?: noData, "", Modifier.weight(1f))
        }
        MetricTile(stringResource(R.string.sleep), sleep?.let { fmtHours(it.value) } ?: noData,
            stringResource(R.string.unit_hours), Modifier.fillMaxWidth())

        SectionTitle(stringResource(R.string.device_title))
        HudCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (dev.link) {
                        LinkState.CONNECTED -> stringResource(R.string.status_connected)
                        LinkState.CONNECTING -> stringResource(R.string.status_connecting)
                        LinkState.DISCONNECTED -> stringResource(R.string.status_disconnected)
                    } + if (dev.deviceName.isNotBlank()) " · ${dev.deviceName}" else "",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            dev.battery?.let {
                Spacer(Modifier.height(4.dp))
                Text("${stringResource(R.string.battery)}: $it%", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
