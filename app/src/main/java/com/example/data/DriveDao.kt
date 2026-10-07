package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveDao {

  @Query("SELECT * FROM drive_files WHERE isTrash = 0 ORDER BY uploadDate DESC")
  fun getActiveFilesFlow(): Flow<List<DriveFile>>

  @Query("SELECT * FROM drive_files WHERE isTrash = 0 AND virtualFolder = :folder ORDER BY uploadDate DESC")
  fun getFilesInFolderFlow(folder: String): Flow<List<DriveFile>>

  @Query("SELECT * FROM drive_files WHERE isTrash = 0 AND (mimeType LIKE 'image/%' OR mimeType LIKE 'video/%' OR extension IN ('jpg', 'jpeg', 'png', 'webp', 'mp4', 'mkv', 'mov')) ORDER BY uploadDate DESC")
  fun getMediaFilesFlow(): Flow<List<DriveFile>>

  @Query("SELECT * FROM drive_files WHERE isTrash = 1 ORDER BY trashedDate DESC")
  fun getTrashFilesFlow(): Flow<List<DriveFile>>

  @Query("SELECT * FROM drive_files WHERE isTrash = 0 AND (name LIKE '%' || :query || '%' OR extension LIKE '%' || :query || '%') ORDER BY uploadDate DESC")
  fun searchFilesFlow(query: String): Flow<List<DriveFile>>

  @Query("SELECT * FROM drive_files WHERE isTrash = 0 ORDER BY uploadDate DESC LIMIT :limit")
  fun getRecentFilesFlow(limit: Int = 10): Flow<List<DriveFile>>

  @Query("SELECT * FROM drive_files WHERE id = :fileId LIMIT 1")
  suspend fun getFileById(fileId: Long): DriveFile?

  @Query("SELECT COALESCE(SUM(size), 0) FROM drive_files WHERE isTrash = 0")
  fun getTotalStorageUsageBytesFlow(): Flow<Long>

  @Query("SELECT COUNT(*) FROM drive_files WHERE isTrash = 0")
  fun getActiveFileCountFlow(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFile(file: DriveFile): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFiles(files: List<DriveFile>): List<Long>

  @Update
  suspend fun updateFile(file: DriveFile)

  @Query("UPDATE drive_files SET syncStatus = :status, uploadProgress = :progress WHERE id = :fileId")
  suspend fun updateProgress(fileId: Long, status: String, progress: Float)

  @Query("UPDATE drive_files SET isTrash = 1, trashedDate = :trashedDate WHERE id = :fileId")
  suspend fun moveToTrash(fileId: Long, trashedDate: Long = System.currentTimeMillis())

  @Query("UPDATE drive_files SET isTrash = 0, trashedDate = NULL WHERE id = :fileId")
  suspend fun restoreFromTrash(fileId: Long)

  @Query("DELETE FROM drive_files WHERE id = :fileId")
  suspend fun deletePermanently(fileId: Long)

  @Query("DELETE FROM drive_files WHERE isTrash = 1")
  suspend fun emptyTrash()

  @Query("DELETE FROM drive_files WHERE isTrash = 1 AND trashedDate < :thresholdMillis")
  suspend fun purgeExpiredTrash(thresholdMillis: Long)

  @Query("UPDATE drive_files SET virtualFolder = :newFolder WHERE id = :fileId")
  suspend fun moveFileToFolder(fileId: Long, newFolder: String)

  @Query("UPDATE drive_files SET isFavorite = :favorite WHERE id = :fileId")
  suspend fun toggleFavorite(fileId: Long, favorite: Boolean)

  @Query("SELECT * FROM drive_files ORDER BY uploadDate DESC")
  suspend fun getAllFilesList(): List<DriveFile>

  @Query("SELECT * FROM drive_files WHERE isTrash = 0 AND isLocalArchived = 0 AND uploadDate < :thresholdMillis")
  suspend fun getFilesEligibleForArchive(thresholdMillis: Long): List<DriveFile>

  @Query("UPDATE drive_files SET isLocalArchived = 1, localUri = NULL WHERE id = :fileId")
  suspend fun markLocalArchived(fileId: Long)

  @Query("UPDATE drive_files SET isEncrypted = :encrypted WHERE id = :fileId")
  suspend fun setFileEncrypted(fileId: Long, encrypted: Boolean)

  // Virtual Folders
  @Query("SELECT COUNT(*) FROM virtual_folders")
  suspend fun getFolderCount(): Int

  @Query("SELECT * FROM virtual_folders ORDER BY isSystem DESC, name ASC")
  fun getAllFoldersFlow(): Flow<List<VirtualFolder>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFolder(folder: VirtualFolder)

  @Query("DELETE FROM virtual_folders WHERE id = :folderId AND isSystem = 0")
  suspend fun deleteFolder(folderId: String)
}
