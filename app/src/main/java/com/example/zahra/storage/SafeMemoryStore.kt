package com.example.zahra.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.zahra.model.FactCategory
import com.example.zahra.model.MemorySaveResult
import com.example.zahra.model.MemorySource
import com.example.zahra.model.ZahraUserFact
import com.example.zahra.security.ZahraCrypto
import org.json.JSONArray
import org.json.JSONObject

/**
 * Storage contract for Zahra's Persistent Memory.
 * Can be mocked or substituted in JVM / unit tests.
 */
interface MemoryStorageDriver {
  fun readRawData(): String?
  fun writeRawData(data: String): Boolean
  fun clearRawData(): Boolean
}

class SharedPreferencesMemoryDriver(
  context: Context,
  prefName: String = "zahra_memory_prefs",
  private val dataKey: String = "user_facts_payload_v2"
) : MemoryStorageDriver {
  private val prefs: SharedPreferences =
    context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

  override fun readRawData(): String? {
    return prefs.getString(dataKey, null)
  }

  override fun writeRawData(data: String): Boolean {
    // Crucial: Use synchronous commit() to ensure data is flushed to persistent storage before returning
    return prefs.edit().putString(dataKey, data).commit()
  }

  override fun clearRawData(): Boolean {
    return prefs.edit().remove(dataKey).commit()
  }
}

/**
 * In-memory test driver for unit tests where Context is not available or for simulating failures.
 */
class InMemoryMemoryDriver(
  private var shouldFailWrites: Boolean = false
) : MemoryStorageDriver {
  private var storedData: String? = null

  fun setSimulateWriteFailure(fail: Boolean) {
    shouldFailWrites = fail
  }

  override fun readRawData(): String? = storedData

  override fun writeRawData(data: String): Boolean {
    if (shouldFailWrites) return false
    storedData = data
    return true
  }

  override fun clearRawData(): Boolean {
    if (shouldFailWrites) return false
    storedData = null
    return true
  }
}

/**
 * Safe, dependable storage manager for persistent user memory with:
 * - Atomic failure-safe writes (synchronous commit)
 * - Immediate read-back verification (Requested → Validated → Saved → Verified → Retrievable)
 * - Transparent AES-256-GCM encryption of stored values
 * - Resilient corrupted-data handling
 * - Clean separation of temporary session context vs confirmed persistent facts
 */
class SafeMemoryStore(
  private val driver: MemoryStorageDriver,
  private val enableEncryption: Boolean = true
) {
  companion object {
    const val CURRENT_SCHEMA_VERSION = 2
    const val MAX_KEY_LENGTH = 100
    const val MAX_VALUE_LENGTH = 1500
  }

  // Active verified cached facts
  private val activeFacts = mutableListOf<ZahraUserFact>()

  // Ephemeral session context (facts observed in current conversation, NOT yet permanently saved/confirmed)
  private val temporarySessionFacts = mutableListOf<ZahraUserFact>()

  init {
    loadAndVerify()
  }

  @Synchronized
  fun getConfirmedFacts(): List<ZahraUserFact> = activeFacts.toList()

  @Synchronized
  fun getTemporaryFacts(): List<ZahraUserFact> = temporarySessionFacts.toList()

  @Synchronized
  fun addTemporaryFact(category: FactCategory, key: String, value: String): ZahraUserFact {
    val temp = ZahraUserFact(
      category = category,
      key = key.trim(),
      value = value.trim(),
      confidence = 0.70,
      source = MemorySource.INFERRED_OBSERVED,
      isConfirmed = false
    )
    temporarySessionFacts.removeAll { it.key.equals(temp.key, ignoreCase = true) }
    temporarySessionFacts.add(0, temp)
    return temp
  }

  /**
   * Complete memory lifecycle: Requested → Validated → Saved → Verified → Retrievable
   */
  @Synchronized
  fun saveFact(
    category: FactCategory,
    key: String,
    value: String,
    confidence: Double = 1.0,
    source: MemorySource = MemorySource.USER_EXPLICIT,
    isConfirmed: Boolean = true
  ): MemorySaveResult {
    val trimmedKey = key.trim()
    val trimmedValue = value.trim()

    // 1. Validation Step
    if (trimmedKey.isBlank()) {
      return MemorySaveResult.ValidationFailed("عنوان أو مفتاح المعلومة لا يمكن أن يكون فارغاً.")
    }
    if (trimmedValue.isBlank()) {
      return MemorySaveResult.ValidationFailed("محتوى المعلومة المراد حفظها لا يمكن أن يكون فارغاً.")
    }
    if (trimmedKey.length > MAX_KEY_LENGTH) {
      return MemorySaveResult.ValidationFailed("عنوان المعلومة طويل جداً (الحد الأقصى $MAX_KEY_LENGTH حرف).")
    }
    if (trimmedValue.length > MAX_VALUE_LENGTH) {
      return MemorySaveResult.ValidationFailed("محتوى المعلومة يتجاوز الحد المسموح ($MAX_VALUE_LENGTH حرف).")
    }
    // Reject dangerous control characters
    if (trimmedKey.any { it.isISOControl() && it != '\n' && it != '\t' } ||
      trimmedValue.any { it.isISOControl() && it != '\n' && it != '\t' }) {
      return MemorySaveResult.ValidationFailed("تحتوي المعلومة على محارف تحكم غير صالحة.")
    }

    // 2. Prepare Record (Handling duplicates & updates)
    val existingIndex = activeFacts.indexOfFirst { it.key.equals(trimmedKey, ignoreCase = true) }
    val isUpdate = existingIndex >= 0

    val now = System.currentTimeMillis()
    val factToSave = if (isUpdate) {
      val existing = activeFacts[existingIndex]
      existing.copy(
        category = category,
        key = trimmedKey,
        value = trimmedValue,
        confidence = confidence,
        source = source,
        isConfirmed = isConfirmed,
        updatedAt = now
      )
    } else {
      ZahraUserFact(
        category = category,
        key = trimmedKey,
        value = trimmedValue,
        confidence = confidence,
        source = source,
        isConfirmed = isConfirmed,
        createdAt = now,
        updatedAt = now
      )
    }

    // Clone current list for failure-safe rollback
    val backupList = ArrayList(activeFacts)
    if (isUpdate) {
      activeFacts[existingIndex] = factToSave
    } else {
      activeFacts.add(0, factToSave)
    }

    // 3. Save Step (Serialize & Flush to durable storage)
    val serialized = serializeFacts(activeFacts)
    val writeOk = driver.writeRawData(serialized)
    if (!writeOk) {
      // Rollback memory cache
      activeFacts.clear()
      activeFacts.addAll(backupList)
      return MemorySaveResult.StorageFailed("فشلت عملية الكتابة إلى التخزين الدائم للجهاز.")
    }

    // 4. Verification Step (Immediate read-back confirmation from disk)
    val readBackRaw = driver.readRawData()
    if (readBackRaw == null) {
      activeFacts.clear()
      activeFacts.addAll(backupList)
      return MemorySaveResult.VerificationFailed("فشل التحقق من قراءة البيانات المحفوظة بعد كتابتها.")
    }

    val verifiedFacts = parseFacts(readBackRaw)
    val foundVerified = verifiedFacts.find { it.id == factToSave.id && it.key == factToSave.key && it.value == factToSave.value }
    if (foundVerified == null) {
      // Discrepancy detected between written and read-back data
      activeFacts.clear()
      activeFacts.addAll(backupList)
      return MemorySaveResult.VerificationFailed("عدم تطابق بين البيانات المكتوبة والمقروءة من الذاكرة.")
    }

    // 5. Retrievable: Remove from temporary session facts if it was previously temporary
    temporarySessionFacts.removeAll { it.key.equals(trimmedKey, ignoreCase = true) }

    val msg = if (isUpdate) {
      "تم تحديث المعلومة في الذاكرة وتأكيد تخزينها محلياً."
    } else {
      "تم حفظ المعلومة في الذاكرة وتأكيد تخزينها محلياً."
    }
    return MemorySaveResult.Success(factToSave, msg, isUpdated = isUpdate)
  }

  @Synchronized
  fun updateFact(id: String, newKey: String, newValue: String, category: FactCategory? = null): MemorySaveResult {
    val idx = activeFacts.indexOfFirst { it.id == id }
    if (idx < 0) {
      return MemorySaveResult.ValidationFailed("لم يتم العثور على المعلومة المطلوب تعديلها في الذاكرة.")
    }
    val existing = activeFacts[idx]
    return saveFact(
      category = category ?: existing.category,
      key = newKey,
      value = newValue,
      confidence = existing.confidence,
      source = MemorySource.USER_UI_MANUAL,
      isConfirmed = true
    )
  }

  @Synchronized
  fun deleteFact(id: String): Boolean {
    val idx = activeFacts.indexOfFirst { it.id == id }
    if (idx < 0) return false

    val backup = ArrayList(activeFacts)
    activeFacts.removeAt(idx)

    val serialized = serializeFacts(activeFacts)
    val writeOk = driver.writeRawData(serialized)
    if (!writeOk) {
      activeFacts.clear()
      activeFacts.addAll(backup)
      return false
    }

    // Verification step for deletion
    val readBack = driver.readRawData() ?: return true
    val verified = parseFacts(readBack)
    if (verified.any { it.id == id }) {
      // Deletion failed verification
      activeFacts.clear()
      activeFacts.addAll(backup)
      return false
    }
    return true
  }

  @Synchronized
  fun clearAll(): Boolean {
    val backup = ArrayList(activeFacts)
    activeFacts.clear()
    temporarySessionFacts.clear()
    val ok = driver.clearRawData()
    if (!ok) {
      activeFacts.addAll(backup)
      return false
    }
    return true
  }

  private fun loadAndVerify() {
    activeFacts.clear()
    val raw = driver.readRawData() ?: return
    try {
      val parsed = parseFacts(raw)
      activeFacts.addAll(parsed)
    } catch (_: Exception) {
      // Handle corrupted data safely: Do not crash
    }
  }

  private fun serializeFacts(list: List<ZahraUserFact>): String {
    val root = JSONObject()
    root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
    root.put("updatedAt", System.currentTimeMillis())

    val arr = JSONArray()
    for (fact in list) {
      val obj = JSONObject()
      obj.put("id", fact.id)
      obj.put("category", fact.category.name)
      obj.put("key", fact.key)

      // Encrypt sensitive value if enabled
      val valueToStore = if (enableEncryption) ZahraCrypto.encrypt(fact.value) else fact.value
      obj.put("value", valueToStore)
      obj.put("isEncrypted", enableEncryption)

      obj.put("confidence", fact.confidence)
      obj.put("source", fact.source.name)
      obj.put("isConfirmed", fact.isConfirmed)
      obj.put("createdAt", fact.createdAt)
      obj.put("updatedAt", fact.updatedAt)
      arr.put(obj)
    }
    root.put("facts", arr)
    return root.toString()
  }

  private fun parseFacts(jsonStr: String): List<ZahraUserFact> {
    val list = mutableListOf<ZahraUserFact>()
    val trimmed = jsonStr.trim()
    if (trimmed.isEmpty()) return list

    // Handle both wrapped versioned format and legacy raw array without throwing JSONException
    val arr = if (trimmed.startsWith("[")) {
      JSONArray(trimmed)
    } else {
      val root = JSONObject(trimmed)
      root.optJSONArray("facts") ?: JSONArray()
    }

    for (i in 0 until arr.length()) {
      val obj = arr.getJSONObject(i)
      val id = obj.optString("id", java.util.UUID.randomUUID().toString())
      val catStr = obj.optString("category", FactCategory.PERSONAL.name)
      val category = try { FactCategory.valueOf(catStr) } catch (_: Exception) { FactCategory.PERSONAL }
      val key = obj.getString("key")
      val rawValue = obj.getString("value")
      val isEncrypted = obj.optBoolean("isEncrypted", false)

      val decryptedValue = if (isEncrypted) {
        try {
          ZahraCrypto.decrypt(rawValue)
        } catch (_: Exception) {
          // If decryption fails due to key mismatch or corrupted block, safe fallback
          rawValue
        }
      } else {
        rawValue
      }

      val confidence = obj.optDouble("confidence", 1.0)
      val sourceStr = obj.optString("source", MemorySource.USER_EXPLICIT.name)
      val source = try { MemorySource.valueOf(sourceStr) } catch (_: Exception) { MemorySource.USER_EXPLICIT }
      val isConfirmed = obj.optBoolean("isConfirmed", true)
      val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
      val updatedAt = obj.optLong("updatedAt", createdAt)

      list.add(
        ZahraUserFact(
          id = id,
          category = category,
          key = key,
          value = decryptedValue,
          confidence = confidence,
          source = source,
          isConfirmed = isConfirmed,
          createdAt = createdAt,
          updatedAt = updatedAt
        )
      )
    }
    return list
  }
}
