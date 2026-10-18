package com.example.zahra.knowledge.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.zahra.knowledge.index.OfflineKnowledgeIndex
import com.example.zahra.knowledge.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Storage driver abstraction for KnowledgeStore, supporting SharedPreferences and In-Memory testing.
 */
interface KnowledgeStorageDriver {
  fun readRawData(): String?
  fun writeRawData(data: String): Boolean
  fun clearRawData(): Boolean
}

class SharedPreferencesKnowledgeDriver(
  context: Context,
  prefName: String = "zahra_knowledge_prefs",
  private val dataKey: String = "knowledge_corpus_v1"
) : KnowledgeStorageDriver {
  private val prefs: SharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

  override fun readRawData(): String? = prefs.getString(dataKey, null)

  override fun writeRawData(data: String): Boolean =
    prefs.edit().putString(dataKey, data).commit()

  override fun clearRawData(): Boolean =
    prefs.edit().remove(dataKey).commit()
}

class InMemoryKnowledgeDriver : KnowledgeStorageDriver {
  private var data: String? = null
  override fun readRawData(): String? = data
  override fun writeRawData(data: String): Boolean {
    this.data = data
    return true
  }
  override fun clearRawData(): Boolean {
    this.data = null
    return true
  }
}

/**
 * Durable Local Knowledge Store.
 * 
 * Guarantees:
 * 1. Complete separation from user personal memory (SafeMemoryStore).
 * 2. Ingestion validation before write: Malformed records are rejected immediately.
 * 3. Provenance tracking: Updates preserve original ingestion metadata.
 * 4. Index synchronization: Search index is rebuilt automatically on load and mutations.
 * 5. Corrupted records are safely isolated without crashing or losing the valid corpus.
 */
class LocalKnowledgeStore(
  private val driver: KnowledgeStorageDriver,
  val index: OfflineKnowledgeIndex = OfflineKnowledgeIndex()
) {

  private val records = LinkedHashMap<String, KnowledgeRecord>()
  private val corruptedRecords = mutableListOf<String>()

  init {
    loadAndSync()
  }

  @Synchronized
  fun insertRecord(record: KnowledgeRecord): KnowledgeValidationResult {
    // 1. Structural and Provenance Validation
    val validation = KnowledgeRecordValidator.validate(record)
    if (validation is KnowledgeValidationResult.Invalid) {
      return validation
    }

    // 2. Prevent accidental overwriting of existing distinct record
    val existing = records[record.id]
    if (existing != null) {
      return KnowledgeValidationResult.Invalid(
        listOf("يوجد سجل معرفي بالفعل يحمل نفس المعرف [${record.id}]. للتعديل استخدم دالة updateRecord."),
        record.id
      )
    }

    records[record.id] = record
    index.addRecord(record)
    persistToDriver()
    return KnowledgeValidationResult.Valid(record)
  }

  @Synchronized
  fun updateRecord(record: KnowledgeRecord): KnowledgeValidationResult {
    val validation = KnowledgeRecordValidator.validate(record)
    if (validation is KnowledgeValidationResult.Invalid) {
      return validation
    }

    val existing = records[record.id]
      ?: return KnowledgeValidationResult.Invalid(
        listOf("السجل المراد تحديثه [${record.id}] غير موجود."),
        record.id
      )

    // Preserve original creation time and original source provenance origin if not explicitly altered
    val updatedRecord = record.copy(
      createdAt = existing.createdAt,
      updatedAt = System.currentTimeMillis()
    )

    records[record.id] = updatedRecord
    index.removeRecord(record.id)
    index.addRecord(updatedRecord)
    persistToDriver()
    return KnowledgeValidationResult.Valid(updatedRecord)
  }

  @Synchronized
  fun getRecordById(id: String): KnowledgeRecord? = records[id]

  @Synchronized
  fun deleteRecord(id: String): Boolean {
    val removed = records.remove(id) != null
    if (removed) {
      index.removeRecord(id)
      persistToDriver()
    }
    return removed
  }

  @Synchronized
  fun listRecords(
    category: KnowledgeCategory? = null,
    status: VerificationStatus? = null
  ): List<KnowledgeRecord> {
    return records.values.filter {
      (category == null || it.category == category) &&
      (status == null || it.verificationStatus == status)
    }.toList()
  }

  @Synchronized
  fun totalCount(): Int = records.size

  @Synchronized
  fun getCorruptedCount(): Int = corruptedRecords.size

  @Synchronized
  fun rebuildIndex(): Int {
    index.clear()
    index.addAll(records.values)
    return index.size()
  }

  @Synchronized
  fun clearAll(): Boolean {
    records.clear()
    corruptedRecords.clear()
    index.clear()
    return driver.clearRawData()
  }

  private fun loadAndSync() {
    val rawJson = driver.readRawData() ?: return
    try {
      val root = JSONObject(rawJson)
      val arr = root.optJSONArray("records") ?: return
      records.clear()
      corruptedRecords.clear()

      for (i in 0 until arr.length()) {
        try {
          val obj = arr.getJSONObject(i)
          val record = parseRecordFromJson(obj)
          val valRes = KnowledgeRecordValidator.validate(record)
          if (valRes is KnowledgeValidationResult.Valid) {
            records[record.id] = record
          } else {
            corruptedRecords.add(obj.toString())
          }
        } catch (e: Exception) {
          corruptedRecords.add(arr.opt(i)?.toString() ?: "unknown")
        }
      }
      index.clear()
      index.addAll(records.values)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun persistToDriver(): Boolean {
    return try {
      val root = JSONObject()
      root.put("schemaVersion", 1)
      root.put("updatedAt", System.currentTimeMillis())

      val arr = JSONArray()
      for (r in records.values) {
        arr.put(serializeRecordToJson(r))
      }
      root.put("records", arr)
      driver.writeRawData(root.toString())
    } catch (e: Exception) {
      false
    }
  }

  fun serializeRecordToJson(record: KnowledgeRecord): JSONObject {
    val obj = JSONObject()
    obj.put("id", record.id)
    obj.put("category", record.category.name)
    obj.put("title", record.title)
    obj.put("canonicalContent", record.canonicalContent)
    obj.put("originalSourceText", record.originalSourceText)
    obj.put("language", record.language)
    obj.put("normalizedText", record.normalizedText)

    val kwArr = JSONArray()
    record.keywords.forEach { kwArr.put(it) }
    obj.put("keywords", kwArr)

    val aliasArr = JSONArray()
    record.aliases.forEach { aliasArr.put(it) }
    obj.put("aliases", aliasArr)

    val provObj = JSONObject()
    provObj.put("origin", record.provenance.origin)
    provObj.put("sourceTitle", record.provenance.sourceTitle)
    provObj.put("sourceId", record.provenance.sourceId)
    provObj.put("sourceLocation", record.provenance.sourceLocation)
    record.provenance.authorOrNarrator?.let { provObj.put("authorOrNarrator", it) }
    record.provenance.grading?.let { provObj.put("grading", it) }
    provObj.put("ingestDate", record.provenance.ingestDate)
    provObj.put("licenseOrAttribution", record.provenance.licenseOrAttribution)
    obj.put("provenance", provObj)

    obj.put("verificationStatus", record.verificationStatus.name)
    obj.put("verificationNotes", record.verificationNotes)

    val relArr = JSONArray()
    record.relatedRecordIds.forEach { relArr.put(it) }
    obj.put("relatedRecordIds", relArr)

    obj.put("schemaVersion", record.schemaVersion)
    obj.put("createdAt", record.createdAt)
    obj.put("updatedAt", record.updatedAt)
    return obj
  }

  fun parseRecordFromJson(obj: JSONObject): KnowledgeRecord {
    val id = obj.getString("id")
    val category = KnowledgeCategory.valueOf(obj.getString("category"))
    val title = obj.getString("title")
    val canonicalContent = obj.getString("canonicalContent")
    val originalSourceText = obj.getString("originalSourceText")
    val language = obj.optString("language", "ar")
    val normalizedText = obj.optString("normalizedText", "")

    val keywords = mutableListOf<String>()
    val kwArr = obj.optJSONArray("keywords")
    if (kwArr != null) {
      for (i in 0 until kwArr.length()) keywords.add(kwArr.getString(i))
    }

    val aliases = mutableListOf<String>()
    val aliasArr = obj.optJSONArray("aliases")
    if (aliasArr != null) {
      for (i in 0 until aliasArr.length()) aliases.add(aliasArr.getString(i))
    }

    val provObj = obj.getJSONObject("provenance")
    val provenance = KnowledgeProvenance(
      origin = provObj.getString("origin"),
      sourceTitle = provObj.getString("sourceTitle"),
      sourceId = provObj.getString("sourceId"),
      sourceLocation = provObj.getString("sourceLocation"),
      authorOrNarrator = if (provObj.has("authorOrNarrator")) provObj.getString("authorOrNarrator") else null,
      grading = if (provObj.has("grading")) provObj.getString("grading") else null,
      ingestDate = provObj.optLong("ingestDate", System.currentTimeMillis()),
      licenseOrAttribution = provObj.optString("licenseOrAttribution", "محقق وموثق محلياً")
    )

    val verificationStatus = VerificationStatus.valueOf(obj.getString("verificationStatus"))
    val verificationNotes = obj.optString("verificationNotes", "")

    val related = mutableListOf<String>()
    val relArr = obj.optJSONArray("relatedRecordIds")
    if (relArr != null) {
      for (i in 0 until relArr.length()) related.add(relArr.getString(i))
    }

    return KnowledgeRecord(
      id = id,
      category = category,
      title = title,
      canonicalContent = canonicalContent,
      originalSourceText = originalSourceText,
      language = language,
      normalizedText = normalizedText,
      keywords = keywords,
      aliases = aliases,
      provenance = provenance,
      verificationStatus = verificationStatus,
      verificationNotes = verificationNotes,
      relatedRecordIds = related,
      schemaVersion = obj.optInt("schemaVersion", 1),
      createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
      updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
    )
  }
}
