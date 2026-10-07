package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drive_files")
data class DriveFile(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val telegramMessageId: Long = 0,
  val name: String,
  val size: Long,
  val mimeType: String,
  val extension: String,
  val virtualFolder: String = "Root",
  val localUri: String? = null,
  val telegramFileId: String = "",
  val uploadDate: Long = System.currentTimeMillis(),
  val isTrash: Boolean = false,
  val trashedDate: Long? = null,
  val syncStatus: String = "SYNCED", // SYNCED, UPLOADING, DOWNLOADING, FAILED
  val uploadProgress: Float = 1.0f,
  val isFavorite: Boolean = false,
  val isAutoBackup: Boolean = false,
  val accountId: Long = 0,
  val isEncrypted: Boolean = false,
  val isLocalArchived: Boolean = false,
) {
  val isImage: Boolean
    get() = mimeType.startsWith("image/") || extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

  val isVideo: Boolean
    get() = mimeType.startsWith("video/") || extension.lowercase() in listOf("mp4", "mkv", "mov", "avi", "webm")

  val isAudio: Boolean
    get() = mimeType.startsWith("audio/") || extension.lowercase() in listOf("mp3", "wav", "flac", "ogg", "m4a")

  val isPdf: Boolean
    get() = mimeType.contains("pdf") || extension.lowercase() == "pdf"

  val isArchive: Boolean
    get() = extension.lowercase() in listOf("zip", "rar", "7z", "tar", "gz")
}
