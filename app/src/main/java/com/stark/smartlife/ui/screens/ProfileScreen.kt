package com.stark.smartlife.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.stark.smartlife.R
import com.stark.smartlife.data.Profile
import com.stark.smartlife.data.ThemeMode
import com.stark.smartlife.ui.MainViewModel
import com.stark.smartlife.ui.components.HudCard
import com.stark.smartlife.ui.components.SectionTitle

@Composable
fun ProfileScreen(vm: MainViewModel) {
    val profile by vm.profile.collectAsState()
    val theme by vm.theme.collectAsState()
    val language by vm.language.collectAsState()

    var name by remember(profile) { mutableStateOf(profile.name) }
    var age by remember(profile) { mutableStateOf(profile.age.takeIf { it > 0 }?.toString().orEmpty()) }
    var height by remember(profile) { mutableStateOf(profile.heightCm.takeIf { it > 0 }?.toString().orEmpty()) }
    var weight by remember(profile) { mutableStateOf(profile.weightKg.takeIf { it > 0 }?.toString().orEmpty()) }
    var goal by remember(profile) { mutableStateOf(profile.stepGoal.toString()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val num = KeyboardOptions(keyboardType = KeyboardType.Number)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleLarge)
        HudCard {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name_label)) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(age, { age = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.age_label)) },
                    singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                OutlinedTextField(height, { height = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.height_label)) },
                    singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                OutlinedTextField(weight, { weight = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.weight_label)) },
                    singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(goal, { goal = it.filter(Char::isDigit).take(6) }, label = { Text(stringResource(R.string.step_goal_label)) },
                singleLine = true, keyboardOptions = num, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Button(
                onClick = {
                    vm.saveProfile(Profile(name.trim(), age.toIntOrNull() ?: 0, height.toIntOrNull() ?: 0,
                        weight.toIntOrNull() ?: 0, goal.toIntOrNull() ?: 8000))
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(stringResource(R.string.save)) }
        }

        SectionTitle(stringResource(R.string.theme_title))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ThemeMode.STARK to R.string.theme_stark, ThemeMode.DARK to R.string.theme_dark,
                ThemeMode.LIGHT to R.string.theme_light, ThemeMode.SYSTEM to R.string.theme_system,
            ).forEach { (m, res) ->
                FilterChip(selected = theme == m, onClick = { vm.setTheme(m) }, label = { Text(stringResource(res)) })
            }
        }

        SectionTitle(stringResource(R.string.language_title))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("" to R.string.language_system, "en" to R.string.language_english,
                "ta" to R.string.language_tamil, "hi" to R.string.language_hindi).forEach { (tag, res) ->
                FilterChip(selected = language == tag, onClick = { vm.setLanguage(tag) }, label = { Text(stringResource(res)) })
            }
        }

        SectionTitle(stringResource(R.string.privacy_title))
        HudCard {
            Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.delete_data))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_data)) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = { TextButton(onClick = { vm.deleteAllData(); confirmDelete = false }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
