package com.example.data.local

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PosSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.posDataStore: DataStore<Preferences> by preferencesDataStore(name = "flame_pos_prefs")

class SecurityPreferences(private val context: Context) {

    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_LENGTH = intPreferencesKey("pin_length")
        val IS_DEFAULT_PIN = booleanPreferencesKey("is_default_pin")
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")
    }

    val settingsFlow: Flow<PosSettings> = context.posDataStore.data.map { prefs ->
        PosSettings(
            isDefaultPin = prefs[Keys.IS_DEFAULT_PIN] ?: true,
            pinLength = prefs[Keys.PIN_LENGTH] ?: DEFAULT_PIN.length,
            shopName = prefs[Keys.SHOP_NAME] ?: DEFAULT_SHOP_NAME,
            receiptFooter = prefs[Keys.RECEIPT_FOOTER] ?: DEFAULT_RECEIPT_FOOTER
        )
    }

    suspend fun ensureInitialized() {
        val prefs = context.posDataStore.data.first()
        if (prefs[Keys.PIN_HASH].isNullOrEmpty() || prefs[Keys.PIN_SALT].isNullOrEmpty()) {
            val salt = generateSalt()
            val hash = hashPin(DEFAULT_PIN, salt)
            context.posDataStore.edit { editable ->
                editable[Keys.PIN_SALT] = salt
                editable[Keys.PIN_HASH] = hash
                editable[Keys.PIN_LENGTH] = DEFAULT_PIN.length
                editable[Keys.IS_DEFAULT_PIN] = true
                if (editable[Keys.SHOP_NAME] == null) {
                    editable[Keys.SHOP_NAME] = DEFAULT_SHOP_NAME
                }
                if (editable[Keys.RECEIPT_FOOTER] == null) {
                    editable[Keys.RECEIPT_FOOTER] = DEFAULT_RECEIPT_FOOTER
                }
            }
        }
    }

    suspend fun verifyPin(pinInput: String): Boolean {
        ensureInitialized()
        val prefs = context.posDataStore.data.first()
        val salt = prefs[Keys.PIN_SALT] ?: return false
        val storedHash = prefs[Keys.PIN_HASH] ?: return false
        val candidateHash = hashPin(pinInput, salt)
        return MessageDigest.isEqual(
            storedHash.toByteArray(Charsets.UTF_8),
            candidateHash.toByteArray(Charsets.UTF_8)
        )
    }

    suspend fun changePin(currentPin: String, newPin: String): Result<Unit> {
        if (newPin.length !in 4..6 || !newPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("PIN must be 4 to 6 digits."))
        }
        if (!verifyPin(currentPin)) {
            return Result.failure(IllegalArgumentException("Current PIN is incorrect."))
        }
        val newSalt = generateSalt()
        val newHash = hashPin(newPin, newSalt)
        context.posDataStore.edit { prefs ->
            prefs[Keys.PIN_SALT] = newSalt
            prefs[Keys.PIN_HASH] = newHash
            prefs[Keys.PIN_LENGTH] = newPin.length
            prefs[Keys.IS_DEFAULT_PIN] = false
        }
        return Result.success(Unit)
    }

    suspend fun updateShopSettings(shopName: String, receiptFooter: String) {
        val cleanName = shopName.trim().ifEmpty { DEFAULT_SHOP_NAME }
        val cleanFooter = receiptFooter.trim().ifEmpty { DEFAULT_RECEIPT_FOOTER }
        context.posDataStore.edit { prefs ->
            prefs[Keys.SHOP_NAME] = cleanName
            prefs[Keys.RECEIPT_FOOTER] = cleanFooter
        }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hashPin(pin: String, saltBase64: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Base64.decode(saltBase64, Base64.NO_WRAP))
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    companion object {
        const val DEFAULT_PIN = "1234"
        const val DEFAULT_SHOP_NAME = "FlamePOS Fast Food"
        const val DEFAULT_RECEIPT_FOOTER = "Thank you for dining with us!"
    }
}
