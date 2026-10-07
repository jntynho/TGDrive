package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object MediaCompressor {

  fun estimateCompressedSize(originalSize: Long, quality: Int, isVideo: Boolean): Long {
    val factor = if (isVideo) {
      0.45f + (quality / 100f) * 0.35f
    } else {
      0.30f + (quality / 100f) * 0.40f
    }
    return (originalSize * factor).toLong().coerceAtLeast(1024L)
  }

  fun compressImageUri(context: Context, uri: Uri, quality: Int): File? {
    return try {
      val inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val bitmap = BitmapFactory.decodeStream(inputStream)
      inputStream.close()
      if (bitmap == null) return null

      val cacheFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
      val outputStream = FileOutputStream(cacheFile)
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(30, 95), outputStream)
      outputStream.flush()
      outputStream.close()
      cacheFile
    } catch (_: Exception) {
      null
    }
  }
}
