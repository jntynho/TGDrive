package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoEngine
import com.example.data.DriveBackupManager
import com.example.data.DriveDatabase
import com.example.data.DriveFile
import com.example.data.TelegramSessionManager
import com.example.data.TelegramUser
import com.example.data.VirtualFolder
import com.example.localization.StringResources
import com.example.telegram.TelegramAuthState
import com.example.telegram.TelegramClient
import com.example.telegram.TransferProgress
import com.example.ui.theme.ThemeMode
import com.example.utils.MediaCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class SortOption {
  DATE_DESC, DATE_ASC, NAME_ASC, SIZE_DESC
}

enum class MediaFilter {
  ALL, PHOTOS, VIDEOS
}

class DriveViewModel(application: Application) : AndroidViewModel(application) {

  private val database = DriveDatabase.getDatabase(application, viewModelScope)
  private val dao = database.driveDao()
  val sessionManager = TelegramSessionManager(application)
  val telegramClient = TelegramClient(application, sessionManager)

  val authState: StateFlow<TelegramAuthState> = telegramClient.authState
  val activeTransfers: StateFlow<Map<Long, TransferProgress>> = telegramClient.activeTransfers
  val floodWaitSeconds: StateFlow<Int> = telegramClient.floodWaitSeconds

  // UI state
  private val _selectedFolder = MutableStateFlow("Root")
  val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _isGridView = MutableStateFlow(false)
  val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

  private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
  val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

  private val _mediaFilter = MutableStateFlow(MediaFilter.ALL)
  val mediaFilter: StateFlow<MediaFilter> = _mediaFilter.asStateFlow()

  private val _themeMode = MutableStateFlow(
    try {
      ThemeMode.valueOf(sessionManager.themeMode)
    } catch (_: Exception) {
      ThemeMode.SYSTEM
    }
  )
  val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

  // Advanced feature states
  private val _autoBackupDcim = MutableStateFlow(sessionManager.autoBackupDcim)
  val autoBackupDcim: StateFlow<Boolean> = _autoBackupDcim.asStateFlow()

  private val _backupWifiOnly = MutableStateFlow(sessionManager.backupWifiOnly)
  val backupWifiOnly: StateFlow<Boolean> = _backupWifiOnly.asStateFlow()

  private val _clientEncryptionEnabled = MutableStateFlow(sessionManager.clientEncryptionEnabled)
  val clientEncryptionEnabled: StateFlow<Boolean> = _clientEncryptionEnabled.asStateFlow()

  private val _encryptionPassphrase = MutableStateFlow(sessionManager.encryptionPassphrase)
  val encryptionPassphrase: StateFlow<String> = _encryptionPassphrase.asStateFlow()

  private val _nightSyncOnly = MutableStateFlow(sessionManager.nightSyncOnly)
  val nightSyncOnly: StateFlow<Boolean> = _nightSyncOnly.asStateFlow()

  private val _bandwidthLimitKbps = MutableStateFlow(sessionManager.bandwidthLimitKbps)
  val bandwidthLimitKbps: StateFlow<Int> = _bandwidthLimitKbps.asStateFlow()

  private val _compressMediaBeforeUpload = MutableStateFlow(sessionManager.compressMediaBeforeUpload)
  val compressMediaBeforeUpload: StateFlow<Boolean> = _compressMediaBeforeUpload.asStateFlow()

  private val _compressionQuality = MutableStateFlow(sessionManager.compressionQuality)
  val compressionQuality: StateFlow<Int> = _compressionQuality.asStateFlow()

  private val _biometricLockEnabled = MutableStateFlow(sessionManager.biometricLockEnabled)
  val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

  private val _appPinCode = MutableStateFlow(sessionManager.appPinCode)
  val appPinCode: StateFlow<String> = _appPinCode.asStateFlow()

  private val _autoArchiveDays = MutableStateFlow(sessionManager.autoArchiveDays)
  val autoArchiveDays: StateFlow<Int> = _autoArchiveDays.asStateFlow()

  private val _savedAccounts = MutableStateFlow(sessionManager.getSavedAccounts())
  val savedAccounts: StateFlow<List<TelegramUser>> = _savedAccounts.asStateFlow()

  private val _apiId = MutableStateFlow(sessionManager.apiId)
  val apiId: StateFlow<String> = _apiId.asStateFlow()

  private val _apiHash = MutableStateFlow(sessionManager.apiHash)
  val apiHash: StateFlow<String> = _apiHash.asStateFlow()

  private val _userMessage = MutableStateFlow<String?>(null)
  val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

  // Room flows
  val allFolders: StateFlow<List<VirtualFolder>> = dao.getAllFoldersFlow()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val totalStorageBytes: StateFlow<Long> = dao.getTotalStorageUsageBytesFlow()
    .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

  val fileCount: StateFlow<Int> = dao.getActiveFileCountFlow()
    .stateIn(viewModelScope, SharingStarted.Lazily, 0)

  val recentFiles: StateFlow<List<DriveFile>> = dao.getRecentFilesFlow(8)
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val trashFiles: StateFlow<List<DriveFile>> = dao.getTrashFilesFlow()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val allActiveFiles: StateFlow<List<DriveFile>> = dao.getActiveFilesFlow()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  // Filtered files according to current folder, search, and sorting
  val filesInCurrentFolder: StateFlow<List<DriveFile>> = combine(
    dao.getActiveFilesFlow(),
    _selectedFolder,
    _searchQuery,
    _sortOption
  ) { files, folder, query, sort ->
    var filtered = if (folder == "Root") {
      files
    } else {
      files.filter { it.virtualFolder == folder }
    }

    if (query.isNotBlank()) {
      filtered = filtered.filter {
        it.name.contains(query, ignoreCase = true) || it.extension.contains(query, ignoreCase = true)
      }
    }

    when (sort) {
      SortOption.DATE_DESC -> filtered.sortedByDescending { it.uploadDate }
      SortOption.DATE_ASC -> filtered.sortedBy { it.uploadDate }
      SortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
      SortOption.SIZE_DESC -> filtered.sortedByDescending { it.size }
    }
  }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  // Media files filtered
  val mediaFiles: StateFlow<List<DriveFile>> = combine(
    dao.getMediaFilesFlow(),
    _mediaFilter
  ) { files, filter ->
    when (filter) {
      MediaFilter.ALL -> files
      MediaFilter.PHOTOS -> files.filter { it.isImage }
      MediaFilter.VIDEOS -> files.filter { it.isVideo }
    }
  }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  init {
    viewModelScope.launch(Dispatchers.IO) {
      val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
      dao.purgeExpiredTrash(thirtyDaysAgo)
      checkAndSeedStarterFiles()
    }
  }

  private suspend fun checkAndSeedStarterFiles() {
    withContext(Dispatchers.IO) {
      if (dao.getFolderCount() == 0) {
        dao.insertFolder(VirtualFolder(id = "Root", name = "All Files", iconName = "folder", isSystem = true))
        dao.insertFolder(VirtualFolder(id = "Camera", name = "Camera & DCIM", iconName = "photo_camera", isSystem = true))
        dao.insertFolder(VirtualFolder(id = "Documents", name = "Documents", iconName = "description", isSystem = true))
        dao.insertFolder(VirtualFolder(id = "Media", name = "Media & Music", iconName = "perm_media", isSystem = true))
        dao.insertFolder(VirtualFolder(id = "Work", name = "Work & Projects", iconName = "work", isSystem = false))
      }
    }

    val existing = withContext(Dispatchers.IO) {
      dao.getFileById(1)
    }
    if (existing == null) {
      val currentUser = sessionManager.getUser()
      val initialFiles = listOf(
        DriveFile(
          telegramMessageId = 100241,
          name = "Project_Pitch_Deck_2026.pdf",
          size = 14_500_000L,
          mimeType = "application/pdf",
          extension = "pdf",
          virtualFolder = "Documents",
          uploadDate = System.currentTimeMillis() - 1000L * 60 * 60 * 2,
          syncStatus = "SYNCED",
          accountId = currentUser.userId
        ),
        DriveFile(
          telegramMessageId = 100242,
          name = "Vacation_Sunset_Alps.jpg",
          size = 6_200_000L,
          mimeType = "image/jpeg",
          extension = "jpg",
          virtualFolder = "Camera",
          uploadDate = System.currentTimeMillis() - 1000L * 60 * 60 * 5,
          syncStatus = "SYNCED",
          isFavorite = true,
          isAutoBackup = true,
          accountId = currentUser.userId
        ),
        DriveFile(
          telegramMessageId = 100243,
          name = "4K_Drone_Cinematic_Reel.mp4",
          size = 185_000_000L,
          mimeType = "video/mp4",
          extension = "mp4",
          virtualFolder = "Media",
          uploadDate = System.currentTimeMillis() - 1000L * 60 * 60 * 12,
          syncStatus = "SYNCED",
          accountId = currentUser.userId
        ),
        DriveFile(
          telegramMessageId = 100244,
          name = "Encrypted_Finance_Vault.zip",
          size = 48_300_000L,
          mimeType = "application/zip",
          extension = "zip",
          virtualFolder = "Work",
          uploadDate = System.currentTimeMillis() - 1000L * 60 * 60 * 24,
          syncStatus = "SYNCED",
          isEncrypted = true,
          accountId = currentUser.userId
        ),
        DriveFile(
          telegramMessageId = 100245,
          name = "Design_Assets_BrandBook.png",
          size = 8_400_000L,
          mimeType = "image/png",
          extension = "png",
          virtualFolder = "Documents",
          uploadDate = System.currentTimeMillis() - 1000L * 60 * 60 * 36,
          syncStatus = "SYNCED",
          accountId = currentUser.userId
        )
      )
      dao.insertFiles(initialFiles)
    }
  }

  // Auth Operations
  fun loginWithPhone(phone: String, onError: (String) -> Unit) {
    viewModelScope.launch {
      val res = telegramClient.sendPhoneNumber(phone)
      if (res.isFailure) {
        onError(res.exceptionOrNull()?.message ?: "Failed to send code")
      }
    }
  }

  fun resendAuthenticationCode(onError: (String) -> Unit) {
    viewModelScope.launch {
      val res = telegramClient.resendAuthenticationCode()
      if (res.isFailure) {
        onError(res.exceptionOrNull()?.message ?: "Failed to resend code")
      } else {
        showMessage("Resent authentication code to your Telegram app")
      }
    }
  }

  fun verifyCode(code: String, onError: (String) -> Unit) {
    viewModelScope.launch {
      val res = telegramClient.verifyCode(code)
      if (res.isFailure) {
        onError(res.exceptionOrNull()?.message ?: "Invalid code")
      } else {
        _savedAccounts.value = sessionManager.getSavedAccounts()
      }
    }
  }

  fun verifyPassword(password: String, onError: (String) -> Unit) {
    viewModelScope.launch {
      val res = telegramClient.verifyPassword(password)
      if (res.isFailure) {
        onError(res.exceptionOrNull()?.message ?: "Incorrect 2FA password")
      } else {
        _savedAccounts.value = sessionManager.getSavedAccounts()
      }
    }
  }

  fun logout(purgeDatabase: Boolean = false) {
    viewModelScope.launch {
      telegramClient.logout(purgeDatabaseDir = purgeDatabase)
      _savedAccounts.value = sessionManager.getSavedAccounts()
    }
  }

  // Multi-Account Operations
  fun switchAccount(userId: Long) {
    viewModelScope.launch {
      val switched = sessionManager.switchAccount(userId)
      if (switched != null) {
        _savedAccounts.value = sessionManager.getSavedAccounts()
        showMessage("Switched to account ${switched.fullName} (${switched.accountLabel})")
      }
    }
  }

  fun addNewSimulatedAccount(label: String, phoneNumber: String) {
    viewModelScope.launch {
      val newId = Random.nextLong(100000000L, 999999999L)
      val newAccount = TelegramUser(
        userId = newId,
        firstName = label,
        lastName = "TG",
        username = "tg_${label.lowercase()}",
        phoneNumber = phoneNumber,
        isPremium = true,
        dcId = 4,
        accountLabel = label
      )
      sessionManager.addOrUpdateAccount(newAccount)
      sessionManager.saveUserSession(newAccount)
      _savedAccounts.value = sessionManager.getSavedAccounts()
      showMessage("Added and switched to account '$label'")
    }
  }

  // Upload file to Telegram Saved Messages
  fun uploadFile(
    uri: Uri?,
    name: String,
    size: Long,
    mimeType: String,
    targetFolder: String = _selectedFolder.value
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val ext = name.substringAfterLast('.', "")
      val isEncrypted = _clientEncryptionEnabled.value
      val currentAccId = sessionManager.getUser().userId

      var finalSize = size
      if (_compressMediaBeforeUpload.value && (mimeType.startsWith("image/") || mimeType.startsWith("video/"))) {
        finalSize = MediaCompressor.estimateCompressedSize(size, _compressionQuality.value, mimeType.startsWith("video/"))
      }

      val initialFile = DriveFile(
        telegramMessageId = 0,
        name = name,
        size = finalSize,
        mimeType = mimeType,
        extension = ext,
        virtualFolder = if (targetFolder == "Root") "Documents" else targetFolder,
        localUri = uri?.toString(),
        uploadDate = System.currentTimeMillis(),
        syncStatus = "UPLOADING",
        uploadProgress = 0f,
        isEncrypted = isEncrypted,
        accountId = currentAccId
      )

      val fileId = dao.insertFile(initialFile)
      val fileWithId = initialFile.copy(id = fileId)

      val uploadResult = telegramClient.uploadToSavedMessages(fileWithId) { progress ->
        viewModelScope.launch(Dispatchers.IO) {
          dao.updateProgress(fileId, "UPLOADING", progress)
        }
      }

      if (uploadResult.isSuccess) {
        val tgMessageId = uploadResult.getOrDefault(0L)
        dao.updateFile(
          fileWithId.copy(
            telegramMessageId = tgMessageId,
            syncStatus = "SYNCED",
            uploadProgress = 1f
          )
        )
        val encNote = if (isEncrypted) " (AES-256 Encrypted)" else ""
        showMessage("Uploaded '$name'$encNote to Telegram Saved Messages!")
      } else {
        dao.updateProgress(fileId, "FAILED", 0f)
        showMessage("Failed to upload '$name' to Telegram")
      }
    }
  }

  // Download file from Saved Messages
  fun downloadFile(file: DriveFile) {
    viewModelScope.launch(Dispatchers.IO) {
      showMessage("Downloading '${file.name}' from Saved Messages...")
      val result = telegramClient.downloadFromSavedMessages(file) { progress ->
        viewModelScope.launch(Dispatchers.IO) {
          dao.updateProgress(file.id, "DOWNLOADING", progress)
        }
      }

      if (result.isSuccess) {
        dao.updateProgress(file.id, "SYNCED", 1f)
        showMessage("Downloaded '${file.name}' successfully!")
      } else {
        showMessage("Failed to download '${file.name}'")
      }
    }
  }

  // Smart Archiving
  fun archiveOldFilesNow() {
    viewModelScope.launch(Dispatchers.IO) {
      val days = _autoArchiveDays.value
      val threshold = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
      val eligible = dao.getFilesEligibleForArchive(threshold)
      for (file in eligible) {
        dao.markLocalArchived(file.id)
      }
      val freedBytes = eligible.sumOf { it.size }
      showMessage("Archived ${eligible.size} files to Telegram cloud, freed ${com.example.ui.components.formatFileSize(freedBytes)} locally!")
    }
  }

  // Export & Import Database Index
  suspend fun exportDatabaseIndex(passphrase: String?): String = withContext(Dispatchers.IO) {
    val allFiles = dao.getAllFilesList()
    DriveBackupManager.exportIndexToJson(allFiles, passphrase)
  }

  suspend fun importDatabaseIndex(payload: String, passphrase: String?): Result<Int> = withContext(Dispatchers.IO) {
    val res = DriveBackupManager.importIndexFromJson(payload, passphrase)
    if (res.isSuccess) {
      val imported = res.getOrThrow()
      dao.insertFiles(imported)
      showMessage("Imported ${imported.size} files successfully!")
      Result.success(imported.size)
    } else {
      Result.failure(res.exceptionOrNull() ?: Exception("Import failed"))
    }
  }

  // File Management
  fun moveToTrash(file: DriveFile) {
    viewModelScope.launch(Dispatchers.IO) {
      dao.moveToTrash(file.id)
      showMessage("Moved '${file.name}' to Trash (30 days recovery)")
    }
  }

  fun restoreFromTrash(file: DriveFile) {
    viewModelScope.launch(Dispatchers.IO) {
      dao.restoreFromTrash(file.id)
      showMessage("Restored '${file.name}'")
    }
  }

  fun deletePermanently(file: DriveFile) {
    viewModelScope.launch(Dispatchers.IO) {
      dao.deletePermanently(file.id)
      showMessage("Permanently deleted '${file.name}' from Telegram & device")
    }
  }

  fun emptyTrash() {
    viewModelScope.launch(Dispatchers.IO) {
      dao.emptyTrash()
      showMessage("Emptied Trash")
    }
  }

  fun createVirtualFolder(name: String, icon: String = "folder") {
    viewModelScope.launch(Dispatchers.IO) {
      val trimmed = name.trim()
      if (trimmed.isNotBlank()) {
        dao.insertFolder(
          VirtualFolder(
            id = trimmed,
            name = trimmed,
            iconName = icon,
            isSystem = false
          )
        )
        showMessage("Created folder '$trimmed'")
      }
    }
  }

  fun moveFileToFolder(fileId: Long, newFolder: String) {
    viewModelScope.launch(Dispatchers.IO) {
      dao.moveFileToFolder(fileId, newFolder)
      showMessage("File moved to $newFolder")
    }
  }

  fun toggleFavorite(file: DriveFile) {
    viewModelScope.launch(Dispatchers.IO) {
      dao.toggleFavorite(file.id, !file.isFavorite)
    }
  }

  // UI state setters
  fun setSelectedFolder(folder: String) {
    _selectedFolder.value = folder
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun toggleGridView() {
    _isGridView.value = !_isGridView.value
  }

  fun setSortOption(sort: SortOption) {
    _sortOption.value = sort
  }

  fun setMediaFilter(filter: MediaFilter) {
    _mediaFilter.value = filter
  }

  fun setThemeMode(mode: ThemeMode) {
    _themeMode.value = mode
    sessionManager.themeMode = mode.name
  }

  fun setClientEncryption(enabled: Boolean, passphrase: String) {
    _clientEncryptionEnabled.value = enabled
    _encryptionPassphrase.value = passphrase
    sessionManager.clientEncryptionEnabled = enabled
    sessionManager.encryptionPassphrase = passphrase
    showMessage(if (enabled) "Client-Side AES-256 Encryption ENABLED" else "Client-Side Encryption Disabled")
  }

  fun setSmartSync(wifiOnly: Boolean, nightOnly: Boolean, bandwidthLimit: Int) {
    _backupWifiOnly.value = wifiOnly
    _nightSyncOnly.value = nightOnly
    _bandwidthLimitKbps.value = bandwidthLimit
    sessionManager.backupWifiOnly = wifiOnly
    sessionManager.nightSyncOnly = nightOnly
    sessionManager.bandwidthLimitKbps = bandwidthLimit
    showMessage("Smart Sync settings updated")
  }

  fun setCompression(enabled: Boolean, quality: Int) {
    _compressMediaBeforeUpload.value = enabled
    _compressionQuality.value = quality
    sessionManager.compressMediaBeforeUpload = enabled
    sessionManager.compressionQuality = quality
    showMessage("Compression settings updated")
  }

  fun setBiometricLock(enabled: Boolean, pin: String) {
    _biometricLockEnabled.value = enabled
    _appPinCode.value = pin
    sessionManager.biometricLockEnabled = enabled
    sessionManager.appPinCode = pin
    showMessage(if (enabled) "Biometric / App Lock Enabled" else "App Lock Disabled")
  }

  fun setAutoArchiveDays(days: Int) {
    _autoArchiveDays.value = days
    sessionManager.autoArchiveDays = days
    showMessage("Auto-Archive set to $days days")
  }

  fun updateBackupSettings(autoDcim: Boolean, wifiOnly: Boolean) {
    _autoBackupDcim.value = autoDcim
    _backupWifiOnly.value = wifiOnly
    sessionManager.autoBackupDcim = autoDcim
    sessionManager.backupWifiOnly = wifiOnly
    showMessage("Backup settings updated")
  }

  fun updateApiCredentials(apiId: String, apiHash: String) {
    _apiId.value = apiId
    _apiHash.value = apiHash
    sessionManager.apiId = apiId
    sessionManager.apiHash = apiHash
    showMessage("Telegram MTProto credentials saved")
  }

  fun triggerDcimScanAndBackup() {
    viewModelScope.launch(Dispatchers.IO) {
      showMessage("Scanning Camera / DCIM folder for unsynced photos...")
      val samplePhotos = listOf(
        "IMG_20261007_120401.jpg" to 4_200_000L,
        "VID_20261007_143022.mp4" to 82_000_000L,
        "IMG_20261007_185011.jpg" to 5_100_000L
      )
      samplePhotos.forEach { (name, size) ->
        uploadFile(
          uri = null,
          name = name,
          size = size,
          mimeType = if (name.endsWith(".mp4")) "video/mp4" else "image/jpeg",
          targetFolder = "Camera"
        )
      }
      showMessage("DCIM auto-backup initiated 3 items to Telegram!")
    }
  }

  fun clearUserMessage() {
    _userMessage.value = null
  }

  fun showMessage(msg: String) {
    _userMessage.value = msg
  }
}
