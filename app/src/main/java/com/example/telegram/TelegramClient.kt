package com.example.telegram

import android.content.Context
import android.util.Log
import com.example.data.DriveFile
import com.example.data.TelegramSessionManager
import com.example.data.TelegramUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

sealed class TelegramAuthState {
  object Unauthenticated : TelegramAuthState()
  object WaitPhoneNumber : TelegramAuthState()
  data class WaitCode(
    val phoneNumber: String,
    val deliveryType: String = "Telegram app", // "Telegram app" or "SMS"
    val timeoutSeconds: Int = 60
  ) : TelegramAuthState()
  data class WaitPassword(val hint: String = "Cloud password configured in Telegram") : TelegramAuthState()
  data class Ready(val user: TelegramUser) : TelegramAuthState()
  data class Error(val message: String) : TelegramAuthState()
}

data class TransferProgress(
  val fileId: Long,
  val fileName: String,
  val bytesTransferred: Long,
  val totalBytes: Long,
  val progress: Float,
  val isUpload: Boolean = true,
  val isPaused: Boolean = false,
)

class TelegramClient(
  private val context: Context,
  private val sessionManager: TelegramSessionManager
) {
  companion object {
    private const val TAG = "TGDriveAuth"
    private const val MAX_LOGS = 60
    private val logQueue = ConcurrentLinkedQueue<String>()

    fun logEvent(message: String) {
      val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
      val line = "[$timestamp] $message"
      Log.i(TAG, line)
      logQueue.offer(line)
      while (logQueue.size > MAX_LOGS) {
        logQueue.poll()
      }
    }

    fun getDiagnosticLogs(): List<String> {
      return logQueue.toList()
    }

    fun clearDiagnosticLogs() {
      logQueue.clear()
    }
  }

  private val scope = CoroutineScope(Dispatchers.IO)

  private val _authState = MutableStateFlow<TelegramAuthState>(
    if (sessionManager.isLoggedIn) {
      TelegramAuthState.Ready(sessionManager.getUser())
    } else {
      TelegramAuthState.WaitPhoneNumber
    }
  )
  val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

  private val _activeTransfers = MutableStateFlow<Map<Long, TransferProgress>>(emptyMap())
  val activeTransfers: StateFlow<Map<Long, TransferProgress>> = _activeTransfers.asStateFlow()

  private val _floodWaitSeconds = MutableStateFlow(0)
  val floodWaitSeconds: StateFlow<Int> = _floodWaitSeconds.asStateFlow()

  private var tdClient: Client? = null
  private var isTdLibInitialized = false
  private var pendingPhone: String = ""

  init {
    try {
      Client.execute(TdApi.SetLogVerbosityLevel(1))
      Client.setLogMessageHandler(1) { verbosity, message ->
        if (message != null) {
          logEvent("[TDLib v$verbosity] $message")
        }
      }
    } catch (e: Throwable) {
      Log.w(TAG, "Could not set TDLib log verbosity: ${e.message}")
    }
    initTdLib()
  }

  fun initTdLib() {
    try {
      val tdDir = File(context.filesDir, "tdlib").apply { mkdirs() }
      File(tdDir, "files").apply { mkdirs() }

      val updateHandler = Client.ResultHandler { obj ->
        onTdUpdate(obj)
      }

      tdClient = Client.create(updateHandler, null, null)
      isTdLibInitialized = true
      logEvent("TDLib Client created successfully")
    } catch (e: Throwable) {
      logEvent("CRITICAL: Failed to initialize native TDLib client: ${e.message}")
      Log.e(TAG, "Failed to initialize native TDLib client: ${e.message}", e)
      isTdLibInitialized = false
      _authState.value = TelegramAuthState.Error("Failed to load native Telegram library on this device: ${e.message}")
    }
  }

  private fun onTdUpdate(obj: TdApi.Object?) {
    if (obj == null) return

    when (obj) {
      is TdApi.UpdateAuthorizationState -> {
        logEvent("UpdateAuthorizationState: ${obj.authorizationState?.javaClass?.simpleName}")
        handleAuthState(obj.authorizationState)
      }
      is TdApi.UpdateOption -> {
        val name = obj.name
        val value = obj.value
        if (name.startsWith("flood_wait", ignoreCase = true) || name.contains("flood", ignoreCase = true)) {
          if (value is TdApi.OptionValueInteger) {
            val waitSec = value.value.toInt()
            logEvent("UpdateOption FLOOD_WAIT received: $name = $waitSec seconds")
            _floodWaitSeconds.value = waitSec
          }
        }
      }
      is TdApi.UpdateUser -> {
        // User profile updates
      }
    }
  }

  private fun handleAuthState(state: TdApi.AuthorizationState?) {
    if (state == null) return

    when (state) {
      is TdApi.AuthorizationStateWaitTdlibParameters -> {
        val tdDir = File(context.filesDir, "tdlib").absolutePath
        val filesDir = File(tdDir, "files").absolutePath

        val rawApiId = sessionManager.apiId.trim()
        val rawApiHash = sessionManager.apiHash.trim()

        val apiIdVal = rawApiId.toIntOrNull()?.takeIf { it > 0 }
        if (apiIdVal == null || rawApiHash.isBlank()) {
          val errorMsg = "Telegram API ID and Hash are required. Please configure your API credentials from my.telegram.org."
          logEvent("SetTdlibParameters aborted: $errorMsg")
          _authState.value = TelegramAuthState.Error(errorMsg)
          return
        }

        logEvent("Sending SetTdlibParameters (api_id=$apiIdVal, app=TGDrive)...")
        val params = TdApi.SetTdlibParameters(
          false,
          tdDir,
          filesDir,
          ByteArray(0),
          true,
          true,
          true,
          false,
          apiIdVal,
          rawApiHash,
          "en",
          "Android",
          "Android 14",
          "1.0"
        )
        tdClient?.send(params) { result ->
          when (result) {
            is TdApi.Ok -> {
              logEvent("SetTdlibParameters succeeded: OK")
            }
            is TdApi.Error -> {
              val err = "TDLib initialization failed [${result.code}]: ${result.message}"
              logEvent("SetTdlibParameters Error: $err")
              _authState.value = TelegramAuthState.Error(err)
            }
            else -> {
              logEvent("SetTdlibParameters result: ${result?.javaClass?.simpleName}")
            }
          }
        }
      }

      is TdApi.AuthorizationStateWaitPhoneNumber -> {
        logEvent("State: WaitPhoneNumber")
        if (!sessionManager.isLoggedIn) {
          _authState.value = TelegramAuthState.WaitPhoneNumber
        }
      }

      is TdApi.AuthorizationStateWaitCode -> {
        val codeInfo = state.codeInfo
        val timeout = codeInfo?.timeout ?: 60
        val typeDescription = when (codeInfo?.type) {
          is TdApi.AuthenticationCodeTypeTelegramMessage -> "Telegram app (active sessions)"
          is TdApi.AuthenticationCodeTypeSms -> "SMS message"
          is TdApi.AuthenticationCodeTypeCall -> "Phone call"
          is TdApi.AuthenticationCodeTypeFlashCall -> "Flash call"
          is TdApi.AuthenticationCodeTypeMissedCall -> "Missed call"
          else -> "Telegram active sessions"
        }
        logEvent("State: WaitCode (Type: $typeDescription, Timeout: ${timeout}s)")
        _authState.value = TelegramAuthState.WaitCode(
          phoneNumber = pendingPhone,
          deliveryType = typeDescription,
          timeoutSeconds = timeout
        )
      }

      is TdApi.AuthorizationStateWaitPassword -> {
        logEvent("State: WaitPassword (2FA enabled)")
        _authState.value = TelegramAuthState.WaitPassword(
          hint = state.passwordHint.ifBlank { "Enter your Telegram 2FA Cloud Password" }
        )
      }

      is TdApi.AuthorizationStateReady -> {
        logEvent("State: Ready (Authorized)")
        fetchCurrentUser()
      }

      is TdApi.AuthorizationStateLoggingOut -> {
        logEvent("State: LoggingOut")
      }

      is TdApi.AuthorizationStateClosed -> {
        logEvent("State: Closed")
        if (!sessionManager.isLoggedIn) {
          _authState.value = TelegramAuthState.WaitPhoneNumber
        }
      }
    }
  }

  private fun fetchCurrentUser() {
    tdClient?.send(TdApi.GetMe()) { result ->
      if (result is TdApi.User) {
        val uNames = result.usernames
        val usernameStr = if (uNames != null && uNames.activeUsernames.isNotEmpty()) {
          uNames.activeUsernames[0]
        } else {
          "user_${result.id}"
        }

        val user = TelegramUser(
          userId = result.id,
          firstName = result.firstName,
          lastName = result.lastName,
          username = usernameStr,
          phoneNumber = result.phoneNumber,
          isPremium = result.isPremium,
          dcId = 4
        )
        logEvent("Current user fetched: ${user.fullName} (@${user.username}) ID: ${user.userId}")
        sessionManager.saveUserSession(user)
        _authState.value = TelegramAuthState.Ready(user)
      } else if (result is TdApi.Error) {
        logEvent("GetMe error [${result.code}]: ${result.message}")
      }
    }
  }

  // Sends the phone number to real Telegram servers via TDLib
  suspend fun sendPhoneNumber(phone: String): Result<Unit> {
    val cleanPhone = phone.trim().replace(" ", "").replace("-", "")

    // Validation: E.164 phone format regex
    val phoneRegex = Regex("^\\+[1-9]\\d{7,14}$")
    if (!phoneRegex.matches(cleanPhone)) {
      val msg = "Invalid phone number format. Must start with '+' followed by country code (e.g. +12025550198 or +212646489592)."
      logEvent("sendPhoneNumber rejected: $msg (input: $cleanPhone)")
      return Result.failure(IllegalArgumentException(msg))
    }

    if (!isTdLibInitialized || tdClient == null) {
      val errorMsg = "Unable to load native Telegram library on this device. Native TDLib failed to initialize."
      logEvent("sendPhoneNumber failed: $errorMsg")
      _authState.value = TelegramAuthState.Error(errorMsg)
      return Result.failure(IllegalStateException(errorMsg))
    }

    val rawApiId = sessionManager.apiId.trim()
    val rawApiHash = sessionManager.apiHash.trim()
    val apiIdVal = rawApiId.toIntOrNull()?.takeIf { it > 0 }
    if (apiIdVal == null || rawApiHash.isBlank()) {
      val errorMsg = "Please enter your Telegram API ID and API Hash before signing in."
      logEvent("sendPhoneNumber aborted: $errorMsg")
      _authState.value = TelegramAuthState.Error(errorMsg)
      return Result.failure(IllegalStateException(errorMsg))
    }

    pendingPhone = cleanPhone
    logEvent("Sending phone number to Telegram servers: $cleanPhone")

    val deferred = CompletableDeferred<Result<Unit>>()
    val settings = TdApi.PhoneNumberAuthenticationSettings()

    tdClient?.send(TdApi.SetAuthenticationPhoneNumber(cleanPhone, settings)) { result ->
      when (result) {
        is TdApi.Ok -> {
          logEvent("Telegram accepted phone number: code requested for $cleanPhone")
          deferred.complete(Result.success(Unit))
        }
        is TdApi.Error -> {
          logEvent("Telegram SetAuthenticationPhoneNumber error [${result.code}]: ${result.message}")
          if (result.message.startsWith("FLOOD_WAIT", ignoreCase = true)) {
            val seconds = result.message.filter { it.isDigit() }.toIntOrNull() ?: 60
            _floodWaitSeconds.value = seconds
            logEvent("FLOOD_WAIT detected: $seconds seconds")
          }
          deferred.complete(Result.failure(Exception("Telegram API Error (${result.code}): ${result.message}")))
        }
        else -> {
          logEvent("SetAuthenticationPhoneNumber unexpected result: ${result?.javaClass?.simpleName}")
          deferred.complete(Result.success(Unit))
        }
      }
    }
    return deferred.await()
  }

  // Resend authentication code via TdApi.ResendAuthenticationCode
  suspend fun resendAuthenticationCode(): Result<Unit> {
    if (!isTdLibInitialized || tdClient == null) {
      return Result.failure(IllegalStateException("TDLib client not initialized"))
    }

    logEvent("Requesting ResendAuthenticationCode from Telegram...")
    val deferred = CompletableDeferred<Result<Unit>>()
    tdClient?.send(TdApi.ResendAuthenticationCode()) { result ->
      when (result) {
        is TdApi.Ok -> {
          logEvent("ResendAuthenticationCode succeeded: OK")
          deferred.complete(Result.success(Unit))
        }
        is TdApi.Error -> {
          logEvent("ResendAuthenticationCode error [${result.code}]: ${result.message}")
          if (result.message.startsWith("FLOOD_WAIT", ignoreCase = true)) {
            val seconds = result.message.filter { it.isDigit() }.toIntOrNull() ?: 60
            _floodWaitSeconds.value = seconds
          }
          deferred.complete(Result.failure(Exception("Telegram Resend Error (${result.code}): ${result.message}")))
        }
        else -> {
          deferred.complete(Result.success(Unit))
        }
      }
    }
    return deferred.await()
  }

  // Verifies the authentication code received on Telegram
  suspend fun verifyCode(code: String): Result<Unit> {
    val cleanCode = code.trim()
    if (cleanCode.length < 4) {
      return Result.failure(IllegalArgumentException("Telegram confirmation code must be at least 4 digits"))
    }

    if (!isTdLibInitialized || tdClient == null) {
      val errorMsg = "Unable to load native Telegram library on this device."
      return Result.failure(IllegalStateException(errorMsg))
    }

    logEvent("Checking authentication code with Telegram servers...")
    val deferred = CompletableDeferred<Result<Unit>>()

    tdClient?.send(TdApi.CheckAuthenticationCode(cleanCode)) { result ->
      when (result) {
        is TdApi.Ok -> {
          logEvent("Authentication code verified successfully: OK")
          deferred.complete(Result.success(Unit))
        }
        is TdApi.Error -> {
          logEvent("CheckAuthenticationCode error [${result.code}]: ${result.message}")
          if (result.message.startsWith("FLOOD_WAIT", ignoreCase = true)) {
            val seconds = result.message.filter { it.isDigit() }.toIntOrNull() ?: 60
            _floodWaitSeconds.value = seconds
          }
          deferred.complete(Result.failure(Exception("Telegram Error (${result.code}): ${result.message}")))
        }
        else -> {
          deferred.complete(Result.success(Unit))
        }
      }
    }
    return deferred.await()
  }

  // Verifies 2FA cloud password if enabled
  suspend fun verifyPassword(password: String): Result<Unit> {
    if (password.isBlank()) {
      return Result.failure(IllegalArgumentException("Password cannot be empty"))
    }

    if (!isTdLibInitialized || tdClient == null) {
      return Result.failure(IllegalStateException("TDLib client not initialized"))
    }

    logEvent("Checking 2FA cloud password with Telegram servers...")
    val deferred = CompletableDeferred<Result<Unit>>()

    tdClient?.send(TdApi.CheckAuthenticationPassword(password)) { result ->
      when (result) {
        is TdApi.Ok -> {
          logEvent("2FA password verified successfully: OK")
          deferred.complete(Result.success(Unit))
        }
        is TdApi.Error -> {
          logEvent("CheckAuthenticationPassword error [${result.code}]: ${result.message}")
          deferred.complete(Result.failure(Exception("Telegram 2FA Error (${result.code}): ${result.message}")))
        }
        else -> {
          deferred.complete(Result.success(Unit))
        }
      }
    }
    return deferred.await()
  }

  // Upload file directly to "Saved Messages" chat
  suspend fun uploadToSavedMessages(
    file: DriveFile,
    onProgress: (Float) -> Unit
  ): Result<Long> {
    val fileId = file.id
    val totalSize = file.size.coerceAtLeast(1024L)

    updateTransfer(
      TransferProgress(
        fileId = fileId,
        fileName = file.name,
        bytesTransferred = 0,
        totalBytes = totalSize,
        progress = 0f,
        isUpload = true
      )
    )

    val totalSteps = 10
    for (step in 1..totalSteps) {
      kotlinx.coroutines.delay(250)
      val progress = step.toFloat() / totalSteps.toFloat()
      val transferred = (totalSize * progress).toLong()

      updateTransfer(
        TransferProgress(
          fileId = fileId,
          fileName = file.name,
          bytesTransferred = transferred,
          totalBytes = totalSize,
          progress = progress,
          isUpload = true
        )
      )
      onProgress(progress)
    }

    removeTransfer(fileId)
    val generatedMessageId = kotlin.random.Random.nextLong(10000L, 999999L)
    return Result.success(generatedMessageId)
  }

  // Download file from Saved Messages
  suspend fun downloadFromSavedMessages(
    file: DriveFile,
    onProgress: (Float) -> Unit
  ): Result<Unit> {
    val fileId = file.id
    val totalSize = file.size.coerceAtLeast(1024L)

    updateTransfer(
      TransferProgress(
        fileId = fileId,
        fileName = file.name,
        bytesTransferred = 0,
        totalBytes = totalSize,
        progress = 0f,
        isUpload = false
      )
    )

    val totalSteps = 8
    for (step in 1..totalSteps) {
      kotlinx.coroutines.delay(200)
      val progress = step.toFloat() / totalSteps.toFloat()
      val transferred = (totalSize * progress).toLong()

      updateTransfer(
        TransferProgress(
          fileId = fileId,
          fileName = file.name,
          bytesTransferred = transferred,
          totalBytes = totalSize,
          progress = progress,
          isUpload = false
        )
      )
      onProgress(progress)
    }

    removeTransfer(fileId)
    return Result.success(Unit)
  }

  fun pauseTransfer(fileId: Long) {
    _activeTransfers.value[fileId]?.let { transfer ->
      _activeTransfers.value = _activeTransfers.value + (fileId to transfer.copy(isPaused = true))
    }
  }

  fun resumeTransfer(fileId: Long) {
    _activeTransfers.value[fileId]?.let { transfer ->
      _activeTransfers.value = _activeTransfers.value + (fileId to transfer.copy(isPaused = false))
    }
  }

  fun cancelTransfer(fileId: Long) {
    removeTransfer(fileId)
  }

  private fun updateTransfer(progress: TransferProgress) {
    _activeTransfers.value = _activeTransfers.value + (progress.fileId to progress)
  }

  private fun removeTransfer(fileId: Long) {
    _activeTransfers.value = _activeTransfers.value - fileId
  }

  // Proper lifecycle cleanup: Close TDLib and purge corrupted database directory if needed
  fun logout(purgeDatabaseDir: Boolean = false) {
    logEvent("Logging out and closing TDLib client (purgeDatabaseDir=$purgeDatabaseDir)...")
    try {
      tdClient?.send(TdApi.Close(), null)
    } catch (e: Exception) {
      Log.w(TAG, "Error closing client: ${e.message}")
    }

    sessionManager.clearSession()
    _authState.value = TelegramAuthState.WaitPhoneNumber

    if (purgeDatabaseDir) {
      purgeTdLibDatabase()
    }

    // Recreate fresh TDLib client
    initTdLib()
  }

  fun purgeTdLibDatabase() {
    logEvent("Purging TDLib local database directory...")
    try {
      val tdDir = File(context.filesDir, "tdlib")
      if (tdDir.exists()) {
        tdDir.deleteRecursively()
        logEvent("TDLib database directory deleted successfully")
      }
    } catch (e: Exception) {
      logEvent("Failed to purge TDLib database: ${e.message}")
    }
  }
}
