package com.stark.smartlife.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Profile(
    val name: String = "",
    val age: Int = 0,
    val heightCm: Int = 0,
    val weightKg: Int = 0,
    val stepGoal: Int = 8000,
)

data class PairedDevice(val address: String, val name: String)

/**
 * Personal profile and last-used watch, encrypted at rest with a hardware-backed
 * Android Keystore master key (via EncryptedSharedPreferences).
 */
class SecureStore(context: Context) {
    private val prefs: SharedPreferences = run {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            "secure_profile",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _profile = MutableStateFlow(readProfile())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private val _device = MutableStateFlow(readDevice())
    val device: StateFlow<PairedDevice?> = _device.asStateFlow()

    private fun readProfile() = Profile(
        name = prefs.getString("name", "") ?: "",
        age = prefs.getInt("age", 0),
        heightCm = prefs.getInt("height", 0),
        weightKg = prefs.getInt("weight", 0),
        stepGoal = prefs.getInt("goal", 8000),
    )

    private fun readDevice(): PairedDevice? {
        val a = prefs.getString("dev_addr", null) ?: return null
        return PairedDevice(a, prefs.getString("dev_name", "") ?: "")
    }

    fun saveProfile(p: Profile) {
        prefs.edit()
            .putString("name", p.name)
            .putInt("age", p.age)
            .putInt("height", p.heightCm)
            .putInt("weight", p.weightKg)
            .putInt("goal", p.stepGoal.coerceAtLeast(1))
            .apply()
        _profile.value = readProfile()
    }

    fun saveDevice(d: PairedDevice?) {
        prefs.edit().apply {
            if (d == null) remove("dev_addr").remove("dev_name")
            else putString("dev_addr", d.address).putString("dev_name", d.name)
        }.apply()
        _device.value = readDevice()
    }

    fun wipe() {
        prefs.edit().clear().apply()
        _profile.value = readProfile()
        _device.value = null
    }
}
