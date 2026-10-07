package com.example.localization

import androidx.compose.ui.unit.LayoutDirection

data class LocalizedStrings(
  val appTitle: String = "TGDrive",
  val homeTab: String = "Home",
  val filesTab: String = "Files",
  val mediaTab: String = "Media",
  val settingsTab: String = "Settings",
  val queueTab: String = "Upload Queue",
  val storageUsed: String = "Storage Used",
  val unlimitedPlan: String = "Unlimited Plan",
  val uploadFile: String = "Upload File",
  val backupDcim: String = "Backup DCIM",
  val newFolder: String = "New Folder",
  val recentUploads: String = "Recent Uploads",
  val searchPlaceholder: String = "Search files by name or extension...",
  val allFiles: String = "All Files",
  val move: String = "Move",
  val download: String = "Download",
  val trash: String = "Trash Bin",
  val preview: String = "Preview",
  val multiAccount: String = "Multi-Account",
  val switchAccount: String = "Switch Account",
  val addAccount: String = "Add Telegram Account",
  val encryption: String = "Client-Side Encryption (AES-256)",
  val encryptionDesc: String = "Encrypt files with a passphrase before uploading to Telegram for absolute privacy.",
  val smartSync: String = "Smart Sync & Scheduling",
  val wifiOnly: String = "Upload on Wi-Fi Only",
  val nightSyncOnly: String = "Nighttime Upload Only (12 AM - 6 AM)",
  val bandwidthLimit: String = "Bandwidth Throttle Limit",
  val mediaCompression: String = "Media Compression Before Upload",
  val compressionDesc: String = "Save mobile bandwidth and speed up uploads with high quality compression.",
  val biometricLock: String = "Biometric Lock (Fingerprint / PIN)",
  val biometricDesc: String = "Protect the app with device biometric lock separate from Telegram login.",
  val smartArchive: String = "Smart Archiving (Free Phone Storage)",
  val smartArchiveDesc: String = "Remove uploaded originals from device while retaining thumbnails locally.",
  val archiveNow: String = "Archive Old Files Now",
  val stats: String = "Usage Statistics & Charts",
  val statsDesc: String = "Monitor monthly uploaded data volume and file distribution.",
  val exportBackup: String = "Export Index (JSON)",
  val importBackup: String = "Import Index (JSON)",
  val aboutTitle: String = "About & Zero-Server Transparency",
  val aboutTransparency: String = "This app connects directly to official Telegram servers via MTProto without any intermediate proxy or third-party storage. All files belong solely to your Saved Messages.",
  val logout: String = "Log Out",
  val uploadSuccess: String = "Uploaded to Telegram Saved Messages!",
  val queueEmpty: String = "No active upload operations in queue",
  val encryptedBadge: String = "AES-256 Encrypted",
  val enterPassphrase: String = "Encryption Passphrase",
)

object StringResources {
  private val englishStrings = LocalizedStrings()

  fun get(): LocalizedStrings = englishStrings

  fun getLayoutDirection(): LayoutDirection = LayoutDirection.Ltr
}
