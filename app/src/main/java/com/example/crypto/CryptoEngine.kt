package com.example.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
  private const val ALGORITHM = "AES/GCM/NoPadding"
  private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
  private const val ITERATION_COUNT = 65536
  private const val KEY_LENGTH = 256
  private const val TAG_LENGTH_BIT = 128
  private const val IV_LENGTH_BYTE = 12
  private const val SALT_LENGTH_BYTE = 16

  fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
    val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
    val keyBytes = factory.generateSecret(spec).encoded
    return SecretKeySpec(keyBytes, "AES")
  }

  fun encryptBytes(data: ByteArray, passphrase: String): ByteArray {
    val random = SecureRandom()
    val salt = ByteArray(SALT_LENGTH_BYTE)
    random.nextBytes(salt)

    val iv = ByteArray(IV_LENGTH_BYTE)
    random.nextBytes(iv)

    val key = deriveKey(passphrase, salt)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BIT, iv))
    val encrypted = cipher.doFinal(data)

    // Layout: [SALT (16 bytes)][IV (12 bytes)][ENCRYPTED DATA + GCM TAG]
    val output = ByteArray(salt.size + iv.size + encrypted.size)
    System.arraycopy(salt, 0, output, 0, salt.size)
    System.arraycopy(iv, 0, output, salt.size, iv.size)
    System.arraycopy(encrypted, 0, output, salt.size + iv.size, encrypted.size)
    return output
  }

  fun decryptBytes(encryptedWithHeader: ByteArray, passphrase: String): Result<ByteArray> {
    return try {
      if (encryptedWithHeader.size < SALT_LENGTH_BYTE + IV_LENGTH_BYTE) {
        return Result.failure(IllegalArgumentException("Invalid encrypted payload size"))
      }

      val salt = ByteArray(SALT_LENGTH_BYTE)
      System.arraycopy(encryptedWithHeader, 0, salt, 0, SALT_LENGTH_BYTE)

      val iv = ByteArray(IV_LENGTH_BYTE)
      System.arraycopy(encryptedWithHeader, SALT_LENGTH_BYTE, iv, 0, IV_LENGTH_BYTE)

      val cipherTextSize = encryptedWithHeader.size - SALT_LENGTH_BYTE - IV_LENGTH_BYTE
      val cipherText = ByteArray(cipherTextSize)
      System.arraycopy(encryptedWithHeader, SALT_LENGTH_BYTE + IV_LENGTH_BYTE, cipherText, 0, cipherTextSize)

      val key = deriveKey(passphrase, salt)
      val cipher = Cipher.getInstance(ALGORITHM)
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BIT, iv))
      val decrypted = cipher.doFinal(cipherText)
      Result.success(decrypted)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun encryptText(text: String, passphrase: String): String {
    val encryptedBytes = encryptBytes(text.toByteArray(Charsets.UTF_8), passphrase)
    return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
  }

  fun decryptText(base64Payload: String, passphrase: String): Result<String> {
    return try {
      val bytes = Base64.decode(base64Payload, Base64.NO_WRAP)
      val decryptedBytes = decryptBytes(bytes, passphrase).getOrThrow()
      Result.success(String(decryptedBytes, Charsets.UTF_8))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
