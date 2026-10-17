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
    // Synchronous commit() ensures flush to non-volatile storage before returning
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
 * Preserved record for corrupted or tamper-detected memory payloads.
 * Ensures corrupted data is retained for audit/recovery rather than silently discarded or exposed as valid plaintext.
 */
data class CorruptedMemoryRecord(
  val id: String?,
  val key: String?,
  val rawPayload: String,
  val errorReason: String,
  val detectedAt: Long = System.currentTimeMillis()
)

/**
 * Safe, dependable storage manager for persistent user memory with:
 * - Atomic failure-safe writes (synchronous commit)
 * - Immediate read-back verification (Requested → Validated → Saved → Verified → Retrievable)
 * - Transparent fail-closed AES-256-GCM encryption
 * - Resilient corrupted-data handling (preserves corrupted records without silent data loss)
 * - Explicit schema validation and safe migration
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

  // Preserved corrupted records for audit and recovery
  private val corruptedRecords = mutableListOf<CorruptedMemoryRecord>()

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
  fun getCorruptedRecords(): List<CorruptedMemoryRecord> = corruptedRecords.toList()

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

    // 2. Prepare Record (Handling duplicate keys on insertion)
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
    val serialized = try {
      serializeFacts(activeFacts)
    } catch (e: Exception) {
      activeFacts.clear()
      activeFacts.addAll(backupList)
      return MemorySaveResult.StorageFailed("فشل تشفير البيانات للتخزين: ${e.message}")
    }

    val writeOk = driver.writeRawData(serialized)
    if (!writeOk) {
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

  /**
   * Updates an existing fact specifically by its unique ID.
   * Prevents accidental collision or silently overwriting another record with a conflicting key.
   */
  @Synchronized
  fun updateFact(id: String, newKey: String, newValue: String, category: FactCategory? = null): MemorySaveResult {
    val trimmedKey = newKey.trim()
    val trimmedValue = newValue.trim()

    val idx = activeFacts.indexOfFirst { it.id == id }
    if (idx < 0) {
      return MemorySaveResult.ValidationFailed("لم يتم العثور على المعلومة المطلوب تعديلها في الذاكرة.")
    }

    // Validation
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

    // Prevent key conflict with a DIFFERENT existing record
    val conflictingRecord = activeFacts.find { it.id != id && it.key.equals(trimmedKey, ignoreCase = true) }
    if (conflictingRecord != null) {
      return MemorySaveResult.ValidationFailed("يوجد معلومة أخرى مسجلة بنفس العنوان: \"$trimmedKey\". يرجى اختيار عنوان مختلف لتجنب استبدال بيانات أخرى.")
    }

    val existing = activeFacts[idx]
    val now = System.currentTimeMillis()
    val updatedFact = existing.copy(
      category = category ?: existing.category,
      key = trimmedKey,
      value = trimmedValue,
      source = MemorySource.USER_UI_MANUAL,
      isConfirmed = true,
      updatedAt = now
    )

    val backupList = ArrayList(activeFacts)
    activeFacts[idx] = updatedFact

    val serialized = try {
      serializeFacts(activeFacts)
    } catch (e: Exception) {
      activeFacts.clear()
      activeFacts.addAll(backupList)
      return MemorySaveResult.StorageFailed("فشل تشفير أو تجهيز البيانات للتخزين: ${e.message}")
    }

    val writeOk = driver.writeRawData(serialized)
    if (!writeOk) {
      activeFacts.clear()
      activeFacts.addAll(backupList)
      return MemorySaveResult.StorageFailed("فشلت عملية الكتابة إلى التخزين الدائم للجهاز.")
    }

    // Immediate read-back verification
    val readBackRaw = driver.readRawData()
    if (readBackRaw == null) {
      activeFacts.clear()
      activeFacts.addAll(backupList)
      return MemorySaveResult.VerificationFailed("فشل التحقق من قراءة البيانات المحفوظة بعد كتابتها.")
    }

    val verifiedFacts = parseFacts(readBackRaw)
    val foundVerified = verifiedFacts.find { it.id == updatedFact.id && it.key == updatedFact.key && it.value == updatedFact.value }
    if (foundVerified == null) {
      activeFacts.clear()
      activeFacts.addAll(backupList)
      return MemorySaveResult.VerificationFailed("عدم تطابق بين البيانات المحدثة والمقروءة من الذاكرة.")
    }

    return MemorySaveResult.Success(updatedFact, "تم تحديث المعلومة في الذاكرة وتأكيد تخزينها محلياً.", isUpdated = true)
  }

  @Synchronized
  fun deleteFact(id: String): Boolean {
    val idx = activeFacts.indexOfFirst { it.id == id }
    if (idx < 0) return false

    val backup = ArrayList(activeFacts)
    activeFacts.removeAt(idx)

    val serialized = try {
      serializeFacts(activeFacts)
    } catch (_: Exception) {
      activeFacts.clear()
      activeFacts.addAll(backup)
      return false
    }

    val writeOk = driver.writeRawData(serialized)
    if (!writeOk) {
      activeFacts.clear()
      activeFacts.addAll(backup)
      return false
    }

    // Verification step for deletion: read back from storage and confirm id is removed
    val readBack = driver.readRawData()
    if (readBack == null) {
      // Storage read failed! Never treat missing read-back as success
      activeFacts.clear()
      activeFacts.addAll(backup)
      return false
    }
    val verified = parseFacts(readBack)
    if (verified.any { it.id == id }) {
      // Deletion failed verification: record is still present in storage
      activeFacts.clear()
      activeFacts.addAll(backup)
      return false
    }
    return true
  }

  @Synchronized
  fun clearAll(): Boolean {
    val backup = ArrayList(activeFacts)
    val backupCorrupted = ArrayList(corruptedRecords)
    activeFacts.clear()
    temporarySessionFacts.clear()
    corruptedRecords.clear()

    val ok = driver.clearRawData()
    if (!ok) {
      activeFacts.addAll(backup)
      corruptedRecords.addAll(backupCorrupted)
      return false
    }

    // Read-back verification: verify expected empty state before reporting success
    val readBack = driver.readRawData()
    if (readBack != null && readBack.isNotBlank()) {
      val remaining = parseFacts(readBack)
      if (remaining.isNotEmpty()) {
        // Verification failed: storage still contains active records
        activeFacts.addAll(backup)
        corruptedRecords.addAll(backupCorrupted)
        return false
      }
    }
    return true
  }

  private fun loadAndVerify() {
    activeFacts.clear()
    corruptedRecords.clear()
    val raw = driver.readRawData() ?: return
    try {
      val parsed = parseFacts(raw)
      activeFacts.addAll(parsed)
    } catch (e: Exception) {
      // Preserve top-level unparseable payload as a corrupted record
      corruptedRecords.add(
        CorruptedMemoryRecord(
          id = null,
          key = null,
          rawPayload = raw,
          errorReason = "Top-level JSON parse failure: ${e.message}"
        )
      )
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

      // Encrypt sensitive value if enabled (fails closed if encryption fails)
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

    // Preserve previously detected corrupted records in non-volatile storage
    if (corruptedRecords.isNotEmpty()) {
      val corrArr = JSONArray()
      for (cr in corruptedRecords) {
        val crObj = JSONObject()
        crObj.put("id", cr.id)
        crObj.put("key", cr.key)
        crObj.put("rawPayload", cr.rawPayload)
        crObj.put("errorReason", cr.errorReason)
        crObj.put("detectedAt", cr.detectedAt)
        corrArr.put(crObj)
      }
      root.put("corruptedRecords", corrArr)
    }

    return root.toString()
  }

  private fun parseFacts(jsonStr: String): List<ZahraUserFact> {
    val list = mutableListOf<ZahraUserFact>()
    val trimmed = jsonStr.trim()
    if (trimmed.isEmpty()) return list

    // Handle both wrapped versioned format and legacy raw array without throwing unhandled exceptions
    val arr = if (trimmed.startsWith("[")) {
      // Legacy unversioned format (Schema Version 1)
      try {
        JSONArray(trimmed)
      } catch (e: Exception) {
        corruptedRecords.add(CorruptedMemoryRecord(null, null, trimmed, "Invalid legacy JSON array: ${e.message}"))
        return list
      }
    } else {
      val root = try {
        JSONObject(trimmed)
      } catch (e: Exception) {
        corruptedRecords.add(CorruptedMemoryRecord(null, null, trimmed, "Invalid JSON object structure: ${e.message}"))
        return list
      }

      val schemaVersion = root.optInt("schemaVersion", 1)
      if (schemaVersion > CURRENT_SCHEMA_VERSION) {
        // Forward incompatibility: do not attempt to overwrite or mutate newer schema versions
        corruptedRecords.add(
          CorruptedMemoryRecord(
            id = null,
            key = null,
            rawPayload = trimmed,
            errorReason = "Unsupported future schema version: $schemaVersion (supported: <= $CURRENT_SCHEMA_VERSION)"
          )
        )
        return list
      }

      // Restore previously saved corrupted records
      val savedCorrupted = root.optJSONArray("corruptedRecords")
      if (savedCorrupted != null) {
        for (j in 0 until savedCorrupted.length()) {
          val crObj = savedCorrupted.getJSONObject(j)
          val crId = if (crObj.has("id") && !crObj.isNull("id")) crObj.getString("id") else null
          val crKey = if (crObj.has("key") && !crObj.isNull("key")) crObj.getString("key") else null
          val crRaw = crObj.optString("rawPayload", "")
          val crReason = crObj.optString("errorReason", "Stored corruption")
          val crTime = crObj.optLong("detectedAt", System.currentTimeMillis())
          // Avoid duplicate entries
          if (corruptedRecords.none { it.rawPayload == crRaw }) {
            corruptedRecords.add(CorruptedMemoryRecord(crId, crKey, crRaw, crReason, crTime))
          }
        }
      }

      root.optJSONArray("facts") ?: JSONArray()
    }

    for (i in 0 until arr.length()) {
      val obj = try {
        arr.getJSONObject(i)
      } catch (e: Exception) {
        corruptedRecords.add(CorruptedMemoryRecord(null, null, "Item index $i", "Malformed JSON item: ${e.message}"))
        continue
      }

      val id = obj.optString("id", java.util.UUID.randomUUID().toString())
      val catStr = obj.optString("category", FactCategory.PERSONAL.name)
      val category = try { FactCategory.valueOf(catStr) } catch (_: Exception) { FactCategory.PERSONAL }
      val key = obj.optString("key", "")
      val rawValue = obj.optString("value", "")
      val isEncrypted = obj.optBoolean("isEncrypted", false)

      if (key.isBlank()) {
        corruptedRecords.add(CorruptedMemoryRecord(id, key, obj.toString(), "Missing or blank fact key"))
        continue
      }

      val decryptedValue = if (isEncrypted) {
        try {
          ZahraCrypto.decrypt(rawValue)
        } catch (e: SecurityException) {
          // Explicit data-integrity failure: NEVER expose raw ciphertext as the user's decrypted value
          corruptedRecords.add(
            CorruptedMemoryRecord(
              id = id,
              key = key,
              rawPayload = obj.toString(),
              errorReason = "Data integrity/decryption failure: ${e.message}"
            )
          )
          null // Reject record from active facts
        }
      } else {
        rawValue
      }

      // If decryption failed, do not add corrupted data to active facts
      if (decryptedValue == null) {
        continue
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
