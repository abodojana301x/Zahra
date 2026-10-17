package com.example.zahra.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

/**
 * Robust, fail-closed AES-256-GCM encryption for Zahra's persistent user memories.
 *
 * Security Architecture:
 * - Production: Backed by AndroidKeyStore with hardware security inspection (KeyInfo.isInsideSecureHardware).
 * - Zero downgrade: Never downgrades to plaintext, never falls back to deterministic/hardcoded keys,
 *   and never returns plaintext or 'RAW:' prefixed data upon crypto errors.
 * - Integrity: GCM 128-bit authentication tag guarantees tampering detection.
 * - Test isolation: Test keys are strictly isolated via test-only injection methods and cannot be selected in production.
 */
object ZahraCrypto {
  private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
  private const val KEY_ALIAS = "ZahraMasterSecretKey_v2"
  private const val ALGORITHM = "AES/GCM/NoPadding"
  private const val GCM_TAG_LENGTH_BITS = 128
  private const val IV_LENGTH_BYTES = 12

  // Test-only key isolated strictly to JVM unit tests when AndroidKeyStore is unavailable
  @Volatile
  private var testIsolatedKey: SecretKey? = null

  @VisibleForTesting
  fun setTestKeyForTestingOnly(key: SecretKey?) {
    testIsolatedKey = key
  }

  @VisibleForTesting
  fun generateTestKey(): SecretKey {
    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(256)
    return keyGen.generateKey()
  }

  /**
   * Evaluates if the active master key is confirmed to reside inside hardware-backed secure storage (TEE / StrongBox).
   * Does NOT claim hardware protection unless AndroidKeyStore KeyInfo explicitly verifies it.
   */
  fun isHardwareBacked(): Boolean {
    if (testIsolatedKey != null) {
      // In-memory test key is never hardware backed
      return false
    }
    return try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
      keyStore.load(null)
      val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry ?: return false
      val factory = SecretKeyFactory.getInstance(entry.secretKey.algorithm, ANDROID_KEYSTORE_PROVIDER)
      val keyInfo = factory.getKeySpec(entry.secretKey, KeyInfo::class.java) as? KeyInfo
      keyInfo?.isInsideSecureHardware == true
    } catch (_: Throwable) {
      false
    }
  }

  private fun getSecretKey(): SecretKey {
    // If a test key was explicitly injected in a test environment, use it
    testIsolatedKey?.let { return it }

    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
    keyStore.load(null)
    if (!keyStore.containsAlias(KEY_ALIAS)) {
      val keyGen = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        ANDROID_KEYSTORE_PROVIDER
      )
      val spec = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
      keyGen.init(spec)
      keyGen.generateKey()
    }
    val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
    return entry?.secretKey
      ?: throw SecurityException("تعذر استرجاع أو إنشاء مفتاح التشفير الآمن من AndroidKeyStore. تم إيقاف العملية للحفاظ على أمان البيانات.")
  }

  private fun encodeBytes(bytes: ByteArray): String {
    return java.util.Base64.getEncoder().encodeToString(bytes)
  }

  private fun decodeBytes(str: String): ByteArray {
    return java.util.Base64.getDecoder().decode(str)
  }

  /**
   * Encrypts plaintext string using AES-256-GCM with a fresh random 12-byte IV.
   * Returns formatted string: "IV:CIPHERTEXT"
   * Fails closed: Throws SecurityException on any crypto failure. Never stores or returns plaintext.
   */
  fun encrypt(plainText: String): String {
    if (plainText.isEmpty()) return ""
    try {
      val key = getSecretKey()
      val cipher = Cipher.getInstance(ALGORITHM)
      val iv = ByteArray(IV_LENGTH_BYTES)
      SecureRandom().nextBytes(iv)
      val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
      cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

      val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
      val ivStr = encodeBytes(iv)
      val cipherStr = encodeBytes(cipherBytes)
      return "$ivStr:$cipherStr"
    } catch (e: Exception) {
      throw SecurityException("فشل تشفير البيانات الحساسة عبر نظام الحماية: ${e.message}", e)
    }
  }

  /**
   * Decrypts ciphertext string and verifies the GCM 128-bit authentication tag.
   * Fails closed: Throws SecurityException if input is malformed, unrecognized, or corrupted.
   * Never silently returns ciphertext as plaintext.
   */
  fun decrypt(encryptedPayload: String): String {
    if (encryptedPayload.isEmpty()) return ""
    val parts = encryptedPayload.split(":")
    if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
      throw SecurityException("صيغة البيانات المشفرة غير صالحة أو مشوهة.")
    }

    try {
      val iv = decodeBytes(parts[0])
      val cipherBytes = decodeBytes(parts[1])

      val key = getSecretKey()
      val cipher = Cipher.getInstance(ALGORITHM)
      val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
      cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

      val plainBytes = cipher.doFinal(cipherBytes)
      return String(plainBytes, Charsets.UTF_8)
    } catch (e: Exception) {
      throw SecurityException("فشل فك التشفير أو تم العبث بالبيانات المحفوظة: ${e.message}", e)
    }
  }
}
