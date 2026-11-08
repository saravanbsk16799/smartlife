package com.stark.smartlife.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.stark.smartlife.R
import com.stark.smartlife.health.HcAvailability
import com.stark.smartlife.ui.MainViewModel
import com.stark.smartlife.ui.components.BarChart
import com.stark.smartlife.ui.components.HudCard
import com.stark.smartlife.ui.components.LineChart
import com.stark.smartlife.ui.components.SectionTitle

@Composable
fun HealthScreen(vm: MainViewModel) {
    val hc by vm.hc.collectAsState()
    val hrWeek by vm.hrWeek.collectAsState()
    val spo2Week by vm.spo2Week.collectAsState()
    val sleepWeek by vm.sleepWeek.collectAsState()
    val hrToday by vm.hrToday.collectAsState()
    val dev by vm.device.collectAsState()

    LaunchedEffect(Unit) { vm.refreshHc() }
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        vm.refreshHc()
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.report_title), style = MaterialTheme.typography.titleLarge)

        SectionTitle(stringResource(R.string.hc_title))
        HudCard {
            when (hc.availability) {
                HcAvailability.UNAVAILABLE -> Text(stringResource(R.string.hc_unavailable))
                HcAvailability.UPDATE_REQUIRED -> Text(stringResource(R.string.hc_update_required))
                HcAvailability.AVAILABLE -> {
                    if (!hc.granted) {
                        Button(onClick = { launcher.launch(vm.hcPermissions) }) { Text(stringResource(R.string.hc_grant)) }
                    } else {
                        Text(stringResource(R.string.hc_connected), color = MaterialTheme.colorScheme.tertiary)
                        Button(onClick = { vm.importHc() }, enabled = !hc.importing) {
                            Text(stringResource(R.string.hc_import))
                        }
                        hc.lastImportCount?.let { Text("$it", style = MaterialTheme.typography.labelSmall) }
                        hc.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            Text(stringResource(R.string.hc_source_note), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }

        SectionTitle(stringResource(R.string.heart_rate) + " · " + stringResource(R.string.today))
        HudCard {
            if (dev.lastHr != null) Text("${stringResource(R.string.live_hr)}: ${dev.lastHr} ${stringResource(R.string.unit_bpm)}")
            else if (dev.gatt.isNotEmpty() && !dev.hasHeartRate) Text(stringResource(R.string.no_hr_service))
            LineChart(hrToday.map { it.value })
        }

        SectionTitle(stringResource(R.string.heart_rate) + " · " + stringResource(R.string.last_7_days))
        HudCard { BarChart(hrWeek.map { it.avg }, hrWeek.map { it.day }) }

        SectionTitle(stringResource(R.string.spo2) + " · " + stringResource(R.string.last_7_days))
        HudCard { BarChart(spo2Week.map { it.avg }, spo2Week.map { it.day }, MaterialTheme.colorScheme.tertiary) }

        SectionTitle(stringResource(R.string.sleep) + " · " + stringResource(R.string.last_7_days))
        HudCard { BarChart(sleepWeek.map { it.total / 60.0 }, sleepWeek.map { it.day }, MaterialTheme.colorScheme.secondary) }

        SectionTitle(stringResource(R.string.stress))
        HudCard { Text(stringResource(R.string.stress_unavailable), style = MaterialTheme.typography.bodyMedium) }
    }
}
