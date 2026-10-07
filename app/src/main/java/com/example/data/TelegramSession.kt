package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class TelegramUser(
  val userId: Long = 0,
  val firstName: String = "",
  val lastName: String = "",
  val username: String = "",
  val phoneNumber: String = "",
  val isPremium: Boolean = false,
  val photoUrl: String? = null,
  val dcId: Int = 4, // Default Telegram Datacenter 4 (Amsterdam/Europe)
  val accountLabel: String = "Personal" // e.g., "Personal", "Work"
) {
  val fullName: String
    get() = if (lastName.isBlank()) firstName else "$firstName $lastName"

  val maxFileSizeLimitBytes: Long
    get() = if (isPremium) 4L * 1024 * 1024 * 1024 else 2L * 1024 * 1024 * 1024 // 4GB vs 2GB
}

class TelegramSessionManager(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("tgdrive_secure_prefs", Context.MODE_PRIVATE)

  companion object {
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_FIRST_NAME = "first_name"
    private const val KEY_LAST_NAME = "last_name"
    private const val KEY_USERNAME = "username"
    private const val KEY_PHONE = "phone_number"
    private const val KEY_IS_PREMIUM = "is_premium"
    private const val KEY_DC_ID = "dc_id"
    private const val KEY_ACCOUNT_LABEL = "account_label"
    private const val KEY_SAVED_ACCOUNTS = "saved_accounts_json"

    // Settings
    private const val KEY_API_ID = "api_id"
    private const val KEY_API_HASH = "api_hash"
    private const val KEY_AUTO_BACKUP_DCIM = "auto_backup_dcim"
    private const val KEY_BACKUP_WIFI_ONLY = "backup_wifi_only"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_LANGUAGE = "app_language"

    // Advanced Features
    private const val KEY_CLIENT_ENCRYPTION = "client_encryption_enabled"
    private const val KEY_ENCRYPTION_PASSPHRASE = "encryption_passphrase"
    private const val KEY_NIGHT_SYNC = "night_sync_only"
    private const val KEY_BANDWIDTH_LIMIT_KBPS = "bandwidth_limit_kbps"
    private const val KEY_COMPRESS_MEDIA = "compress_media_enabled"
    private const val KEY_COMPRESS_QUALITY = "compress_quality"
    private const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
    private const val KEY_APP_PIN = "app_pin_code"
    private const val KEY_AUTO_ARCHIVE_DAYS = "auto_archive_days"

    // Official Telegram application credentials from my.telegram.org
    const val DEFAULT_API_ID = "37947557"
    const val DEFAULT_API_HASH = "1256203a758ab333f6f54459040d7455"
  }

  var isLoggedIn: Boolean
    get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

  var apiId: String
    get() = prefs.getString(KEY_API_ID, DEFAULT_API_ID) ?: DEFAULT_API_ID
    set(value) = prefs.edit().putString(KEY_API_ID, value).apply()

  var apiHash: String
    get() = prefs.getString(KEY_API_HASH, DEFAULT_API_HASH) ?: DEFAULT_API_HASH
    set(value) = prefs.edit().putString(KEY_API_HASH, value).apply()

  var autoBackupDcim: Boolean
    get() = prefs.getBoolean(KEY_AUTO_BACKUP_DCIM, false)
    set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP_DCIM, value).apply()

  var backupWifiOnly: Boolean
    get() = prefs.getBoolean(KEY_BACKUP_WIFI_ONLY, true)
    set(value) = prefs.edit().putBoolean(KEY_BACKUP_WIFI_ONLY, value).apply()

  var themeMode: String
    get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

  var language: String
    get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

  var clientEncryptionEnabled: Boolean
    get() = prefs.getBoolean(KEY_CLIENT_ENCRYPTION, false)
    set(value) = prefs.edit().putBoolean(KEY_CLIENT_ENCRYPTION, value).apply()

  var encryptionPassphrase: String
    get() = prefs.getString(KEY_ENCRYPTION_PASSPHRASE, "CloudGram@Pass2026") ?: "CloudGram@Pass2026"
    set(value) = prefs.edit().putString(KEY_ENCRYPTION_PASSPHRASE, value).apply()

  var nightSyncOnly: Boolean
    get() = prefs.getBoolean(KEY_NIGHT_SYNC, false)
    set(value) = prefs.edit().putBoolean(KEY_NIGHT_SYNC, value).apply()

  var bandwidthLimitKbps: Int
    get() = prefs.getInt(KEY_BANDWIDTH_LIMIT_KBPS, 0) // 0 = unlimited
    set(value) = prefs.edit().putInt(KEY_BANDWIDTH_LIMIT_KBPS, value).apply()

  var compressMediaBeforeUpload: Boolean
    get() = prefs.getBoolean(KEY_COMPRESS_MEDIA, true)
    set(value) = prefs.edit().putBoolean(KEY_COMPRESS_MEDIA, value).apply()

  var compressionQuality: Int
    get() = prefs.getInt(KEY_COMPRESS_QUALITY, 80)
    set(value) = prefs.edit().putInt(KEY_COMPRESS_QUALITY, value).apply()

  var biometricLockEnabled: Boolean
    get() = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
    set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, value).apply()

  var appPinCode: String
    get() = prefs.getString(KEY_APP_PIN, "1234") ?: "1234"
    set(value) = prefs.edit().putString(KEY_APP_PIN, value).apply()

  var autoArchiveDays: Int
    get() = prefs.getInt(KEY_AUTO_ARCHIVE_DAYS, 30)
    set(value) = prefs.edit().putInt(KEY_AUTO_ARCHIVE_DAYS, value).apply()

  fun saveUserSession(user: TelegramUser) {
    prefs.edit()
      .putBoolean(KEY_IS_LOGGED_IN, true)
      .putLong(KEY_USER_ID, user.userId)
      .putString(KEY_FIRST_NAME, user.firstName)
      .putString(KEY_LAST_NAME, user.lastName)
      .putString(KEY_USERNAME, user.username)
      .putString(KEY_PHONE, user.phoneNumber)
      .putBoolean(KEY_IS_PREMIUM, user.isPremium)
      .putInt(KEY_DC_ID, user.dcId)
      .putString(KEY_ACCOUNT_LABEL, user.accountLabel)
      .apply()

    // Add or update in saved accounts list
    addOrUpdateAccount(user)
  }

  fun getUser(): TelegramUser {
    return TelegramUser(
      userId = prefs.getLong(KEY_USER_ID, 128475923L),
      firstName = prefs.getString(KEY_FIRST_NAME, "Telegram") ?: "Telegram",
      lastName = prefs.getString(KEY_LAST_NAME, "User") ?: "User",
      username = prefs.getString(KEY_USERNAME, "tg_user") ?: "tg_user",
      phoneNumber = prefs.getString(KEY_PHONE, "") ?: "",
      isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false),
      dcId = prefs.getInt(KEY_DC_ID, 4),
      accountLabel = prefs.getString(KEY_ACCOUNT_LABEL, "Personal") ?: "Personal"
    )
  }

  fun getSavedAccounts(): List<TelegramUser> {
    val jsonString = prefs.getString(KEY_SAVED_ACCOUNTS, null) ?: return listOf(getUser())
    return try {
      val jsonArray = JSONArray(jsonString)
      val list = mutableListOf<TelegramUser>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
          TelegramUser(
            userId = obj.getLong("userId"),
            firstName = obj.optString("firstName", "User"),
            lastName = obj.optString("lastName", ""),
            username = obj.optString("username", ""),
            phoneNumber = obj.optString("phoneNumber", ""),
            isPremium = obj.optBoolean("isPremium", false),
            dcId = obj.optInt("dcId", 4),
            accountLabel = obj.optString("accountLabel", if (i == 0) "Personal" else "Work")
          )
        )
      }
      if (list.isEmpty()) listOf(getUser()) else list
    } catch (_: Exception) {
      listOf(getUser())
    }
  }

  fun addOrUpdateAccount(user: TelegramUser) {
    val accounts = getSavedAccounts().toMutableList()
    val index = accounts.indexOfFirst { it.userId == user.userId }
    if (index >= 0) {
      accounts[index] = user
    } else {
      accounts.add(user)
    }
    saveAccountsList(accounts)
  }

  fun switchAccount(targetUserId: Long): TelegramUser? {
    val accounts = getSavedAccounts()
    val target = accounts.find { it.userId == targetUserId } ?: return null
    saveUserSession(target)
    return target
  }

  fun removeAccount(userId: Long) {
    val accounts = getSavedAccounts().filter { it.userId != userId }
    saveAccountsList(accounts)
    if (prefs.getLong(KEY_USER_ID, 0L) == userId) {
      if (accounts.isNotEmpty()) {
        saveUserSession(accounts.first())
      } else {
        clearSession()
      }
    }
  }

  private fun saveAccountsList(accounts: List<TelegramUser>) {
    val jsonArray = JSONArray()
    for (acc in accounts) {
      val obj = JSONObject()
      obj.put("userId", acc.userId)
      obj.put("firstName", acc.firstName)
      obj.put("lastName", acc.lastName)
      obj.put("username", acc.username)
      obj.put("phoneNumber", acc.phoneNumber)
      obj.put("isPremium", acc.isPremium)
      obj.put("dcId", acc.dcId)
      obj.put("accountLabel", acc.accountLabel)
      jsonArray.put(obj)
    }
    prefs.edit().putString(KEY_SAVED_ACCOUNTS, jsonArray.toString()).apply()
  }

  fun clearSession() {
    prefs.edit()
      .putBoolean(KEY_IS_LOGGED_IN, false)
      .remove(KEY_USER_ID)
      .remove(KEY_FIRST_NAME)
      .remove(KEY_LAST_NAME)
      .remove(KEY_USERNAME)
      .remove(KEY_PHONE)
      .remove(KEY_IS_PREMIUM)
      .apply()
  }
}
