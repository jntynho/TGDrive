package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [DriveFile::class, VirtualFolder::class],
  version = 2,
  exportSchema = false
)
abstract class DriveDatabase : RoomDatabase() {
  abstract fun driveDao(): DriveDao

  companion object {
    @Volatile
    private var INSTANCE: DriveDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): DriveDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          DriveDatabase::class.java,
          "tgdrive_database.db"
        )
          .addCallback(DatabaseCallback(scope))
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback(
      private val scope: CoroutineScope
    ) : Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            val dao = database.driveDao()
            // Seed standard virtual folders
            dao.insertFolder(VirtualFolder(id = "Root", name = "All Files", iconName = "folder", isSystem = true))
            dao.insertFolder(VirtualFolder(id = "Camera", name = "Camera & DCIM", iconName = "photo_camera", isSystem = true))
            dao.insertFolder(VirtualFolder(id = "Documents", name = "Documents", iconName = "description", isSystem = true))
            dao.insertFolder(VirtualFolder(id = "Media", name = "Media & Music", iconName = "perm_media", isSystem = true))
            dao.insertFolder(VirtualFolder(id = "Work", name = "Work & Projects", iconName = "work", isSystem = false))
          }
        }
      }
    }
  }
}
