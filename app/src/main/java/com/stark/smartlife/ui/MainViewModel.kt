package com.stark.smartlife.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stark.smartlife.data.Alarm
import com.stark.smartlife.data.Alert
import com.stark.smartlife.data.DayAvg
import com.stark.smartlife.data.DayTotal
import com.stark.smartlife.data.Profile
import com.stark.smartlife.data.Reading
import com.stark.smartlife.data.ReadingType
import com.stark.smartlife.data.ThemeMode
import com.stark.smartlife.data.Workout
import com.stark.smartlife.health.HcAvailability
import com.stark.smartlife.starkApp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HcUi(val availability: HcAvailability = HcAvailability.UNAVAILABLE, val granted: Boolean = false,
                val importing: Boolean = false, val lastImportCount: Int? = null, val error: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app.starkApp
    private val readings = a.db.readings()

    private fun startOfDay(daysAgo: Long = 0): Long =
        LocalDate.now().minusDays(daysAgo).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val day = MutableStateFlow(startOfDay())
    fun refreshDay() { day.value = startOfDay() }

    private fun <T> eager(f: kotlinx.coroutines.flow.Flow<T>, init: T): StateFlow<T> =
        f.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), init)

    // ----- today -----
    val steps = eager(day.flatMapLatest { readings.sumSince(ReadingType.STEPS, it) }, 0.0)
    val calories = eager(day.flatMapLatest { readings.sumSince(ReadingType.CALORIES, it) }, 0.0)
    val distanceM = eager(day.flatMapLatest { readings.sumSince(ReadingType.DISTANCE_M, it) }, 0.0)
    val heartRate: StateFlow<Reading?> = eager(readings.latest(ReadingType.HEART_RATE), null)
    val spo2: StateFlow<Reading?> = eager(readings.latest(ReadingType.SPO2), null)
    val sleepLast: StateFlow<Reading?> = eager(readings.latest(ReadingType.SLEEP_MIN), null)
    val batteryReading: StateFlow<Reading?> = eager(readings.latest(ReadingType.BATTERY), null)

    // ----- 7-day history -----
    private val weekStart get() = startOfDay(6)
    val stepsWeek: StateFlow<List<DayTotal>> = eager(readings.dailyTotals(ReadingType.STEPS, weekStart), emptyList())
    val sleepWeek: StateFlow<List<DayTotal>> = eager(readings.dailyTotals(ReadingType.SLEEP_MIN, weekStart), emptyList())
    val kcalWeek: StateFlow<List<DayTotal>> = eager(readings.dailyTotals(ReadingType.CALORIES, weekStart), emptyList())
    val hrWeek: StateFlow<List<DayAvg>> = eager(readings.dailyAverages(ReadingType.HEART_RATE, weekStart), emptyList())
    val spo2Week: StateFlow<List<DayAvg>> = eager(readings.dailyAverages(ReadingType.SPO2, weekStart), emptyList())
    val hrToday: StateFlow<List<Reading>> = eager(day.flatMapLatest { readings.since(ReadingType.HEART_RATE, it) }, emptyList())

    // ----- profile / settings -----
    val profile: StateFlow<Profile> = a.secure.profile
    val theme = eager(a.settings.theme, ThemeMode.STARK)
    val language = eager(a.settings.language, "")

    fun saveProfile(p: Profile) = a.secure.saveProfile(p)
    fun setTheme(m: ThemeMode) = viewModelScope.launch { a.settings.setTheme(m) }
    fun setLanguage(tag: String) = viewModelScope.launch {
        a.settings.setLanguage(tag)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    // ----- device -----
    val device = a.ble.state
    val ble get() = a.ble

    // ----- workouts -----
    val workouts: StateFlow<List<Workout>> = eager(a.db.workouts().recent(), emptyList())
    private val _active = MutableStateFlow<Pair<String, Long>?>(null)
    val activeWorkout: StateFlow<Pair<String, Long>?> = _active.asStateFlow()
    fun startWorkout(sport: String) { _active.value = sport to System.currentTimeMillis() }
    fun stopWorkout() {
        val (sport, start) = _active.value ?: return
        _active.value = null
        viewModelScope.launch { a.db.workouts().insert(Workout(sport = sport, startTime = start, endTime = System.currentTimeMillis())) }
    }

    // ----- alarms -----
    val alarms: StateFlow<List<Alarm>> = eager(a.db.alarms().all(), emptyList())
    fun addAlarm(label: String, hour: Int, minute: Int) = viewModelScope.launch {
        val alarm = Alarm(label = label.trim(), hour = hour, minute = minute)
        val id = a.db.alarms().insert(alarm)
        a.alarms.schedule(alarm.copy(id = id))
    }
    fun toggleAlarm(al: Alarm) = viewModelScope.launch {
        val u = al.copy(enabled = !al.enabled)
        a.db.alarms().update(u)
        a.alarms.schedule(u)
    }
    fun deleteAlarm(al: Alarm) = viewModelScope.launch {
        a.alarms.cancel(al)
        a.db.alarms().delete(al)
    }

    // ----- alerts -----
    val alerts: StateFlow<List<Alert>> = eager(a.db.alerts().recent(), emptyList())
    fun clearAlerts() = viewModelScope.launch { a.db.alerts().clear() }

    // ----- Health Connect -----
    private val _hc = MutableStateFlow(HcUi())
    val hc: StateFlow<HcUi> = _hc.asStateFlow()
    val hcPermissions get() = a.health.permissions

    fun refreshHc() = viewModelScope.launch {
        val av = a.health.availability()
        val granted = runCatching { a.health.hasAllPermissions() }.getOrDefault(false)
        _hc.value = _hc.value.copy(availability = av, granted = granted)
    }

    fun importHc() = viewModelScope.launch {
        _hc.value = _hc.value.copy(importing = true, error = null)
        runCatching { a.health.importDays(7) }
            .onSuccess { _hc.value = _hc.value.copy(importing = false, lastImportCount = it) }
            .onFailure { _hc.value = _hc.value.copy(importing = false, error = it.message ?: it.javaClass.simpleName) }
    }

    // ----- privacy -----
    fun deleteAllData() = viewModelScope.launch {
        a.ble.forget()
        a.db.alarms().allNow().forEach { a.alarms.cancel(it) }
        a.db.wipe()
        a.secure.wipe()
    }
}
