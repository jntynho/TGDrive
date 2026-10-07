package com.example.data

import com.example.crypto.CryptoEngine
import org.json.JSONArray
import org.json.JSONObject

object DriveBackupManager {

  fun exportIndexToJson(files: List<DriveFile>, passphrase: String? = null): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("exportedAt", System.currentTimeMillis())
    root.put("app", "TGDrive CloudGram")

    val filesArray = JSONArray()
    for (f in files) {
      val item = JSONObject()
      item.put("telegramMessageId", f.telegramMessageId)
      item.put("name", f.name)
      item.put("size", f.size)
      item.put("mimeType", f.mimeType)
      item.put("extension", f.extension)
      item.put("virtualFolder", f.virtualFolder)
      item.put("uploadDate", f.uploadDate)
      item.put("isFavorite", f.isFavorite)
      item.put("isEncrypted", f.isEncrypted)
      item.put("accountId", f.accountId)
      filesArray.put(item)
    }
    root.put("files", filesArray)

    val jsonString = root.toString(2)
    return if (!passphrase.isNullOrBlank()) {
      CryptoEngine.encryptText(jsonString, passphrase)
    } else {
      jsonString
    }
  }

  fun importIndexFromJson(payload: String, passphrase: String? = null): Result<List<DriveFile>> {
    return try {
      val rawJson = if (!passphrase.isNullOrBlank()) {
        CryptoEngine.decryptText(payload, passphrase).getOrThrow()
      } else {
        payload
      }

      val root = JSONObject(rawJson)
      val filesArray = root.getJSONArray("files")
      val list = mutableListOf<DriveFile>()

      for (i in 0 until filesArray.length()) {
        val item = filesArray.getJSONObject(i)
        list.add(
          DriveFile(
            telegramMessageId = item.optLong("telegramMessageId", 0L),
            name = item.getString("name"),
            size = item.getLong("size"),
            mimeType = item.optString("mimeType", "application/octet-stream"),
            extension = item.optString("extension", "bin"),
            virtualFolder = item.optString("virtualFolder", "Root"),
            uploadDate = item.optLong("uploadDate", System.currentTimeMillis()),
            isFavorite = item.optBoolean("isFavorite", false),
            isEncrypted = item.optBoolean("isEncrypted", false),
            accountId = item.optLong("accountId", 0L),
            syncStatus = "SYNCED",
            uploadProgress = 1f
          )
        )
      }
      Result.success(list)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
