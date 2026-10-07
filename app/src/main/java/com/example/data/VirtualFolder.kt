package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_folders")
data class VirtualFolder(
  @PrimaryKey
  val id: String,
  val name: String,
  val iconName: String = "folder",
  val createdAt: Long = System.currentTimeMillis(),
  val isSystem: Boolean = false,
)
