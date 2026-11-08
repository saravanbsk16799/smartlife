package com.stark.smartlife.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.stark.smartlife.ui.MainViewModel
import com.stark.smartlife.ui.components.BarChart
import com.stark.smartlife.ui.components.HudCard
import com.stark.smartlife.ui.components.SectionTitle
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private val sports = listOf("walk" to R.string.sport_walk, "run" to R.string.sport_run, "cycle" to R.string.sport_cycle,
    "yoga" to R.string.sport_yoga, "gym" to R.string.sport_gym)

@Composable
fun ActivityScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val stepsWeek by vm.stepsWeek.collectAsState()
    val kcalWeek by vm.kcalWeek.collectAsState()
    val active by vm.activeWorkout.collectAsState()
    val workouts by vm.workouts.collectAsState()
    val alarms by vm.alarms.collectAsState()

    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(active) {
        while (active != null) { elapsed = (System.currentTimeMillis() - active!!.second) / 1000; delay(1000) }
        elapsed = 0
    }
    var label by remember { mutableStateOf("") }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(stringResource(R.string.steps) + " · " + stringResource(R.string.last_7_days))
        HudCard { BarChart(stepsWeek.map { it.total }, stepsWeek.map { it.day }) }
        SectionTitle(stringResource(R.string.calories) + " · " + stringResource(R.string.last_7_days))
        HudCard { BarChart(kcalWeek.map { it.total }, kcalWeek.map { it.day }, MaterialTheme.colorScheme.secondary) }

        SectionTitle(stringResource(R.string.workouts_title))
        HudCard {
            Text(stringResource(R.string.workout_phone_only), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (active == null) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sports.take(3).forEach { (id, res) ->
                        AssistChip(onClick = { vm.startWorkout(id) }, label = { Text(stringResource(res)) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sports.drop(3).forEach { (id, res) ->
                        AssistChip(onClick = { vm.startWorkout(id) }, label = { Text(stringResource(res)) })
                    }
                }
            } else {
                Text("%02d:%02d".format(elapsed / 60, elapsed % 60), style = MaterialTheme.typography.headlineLarge)
                Button(onClick = { vm.stopWorkout() }) { Text(stringResource(R.string.stop_workout)) }
            }
            workouts.take(5).forEach { w ->
                val mins = (w.endTime - w.startTime) / 60_000
                val name = sports.firstOrNull { it.first == w.sport }?.let { stringResource(it.second) } ?: w.sport
                Text("$name · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(w.startTime))} · $mins min",
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
        }

        SectionTitle(stringResource(R.string.alarms_title))
        HudCard {
            Text(stringResource(R.string.alarms_phone_only), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(label, { label = it }, label = { Text(stringResource(R.string.alarm_label_hint)) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedButton(onClick = {
                TimePickerDialog(ctx, { _, h, m -> vm.addAlarm(label, h, m); label = "" }, 7, 0, true).show()
            }) { Text(stringResource(R.string.add_alarm)) }
            alarms.forEach { al ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("%02d:%02d".format(al.hour, al.minute), style = MaterialTheme.typography.titleMedium)
                        if (al.label.isNotBlank()) Text(al.label, style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(al.enabled, { vm.toggleAlarm(al) })
                    TextButton(onClick = { vm.deleteAlarm(al) }) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
}
