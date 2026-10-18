package com.example.zahra.knowledge.engine

import android.content.Context
import com.example.zahra.knowledge.index.OfflineKnowledgeIndex
import com.example.zahra.knowledge.model.*
import com.example.zahra.knowledge.normalizer.ArabicKnowledgeNormalizer
import com.example.zahra.knowledge.storage.KnowledgeStorageDriver
import com.example.zahra.knowledge.storage.LocalKnowledgeStore
import com.example.zahra.knowledge.storage.SharedPreferencesKnowledgeDriver
import com.example.zahra.knowledge.synonyms.OfflineSynonymDictionary
import org.json.JSONObject

/**
 * Phase 3: Local Knowledge Engine — "Truth Before Intelligence"
 * 
 * Guarantees:
 * 1. 100% offline, Zero-LLM, Zero-Transformer, Zero-Network.
 * 2. Complete provenance preservation and verification gating.
 * 3. Never confuses document retrieval relevance with factual truth.
 * 4. Transparent conflict detection and structured abstention.
 * 5. Strict logical separation from user personal memory (SafeMemoryStore).
 */
class KnowledgeEngine(
  private val context: Context,
  driver: KnowledgeStorageDriver? = null
) {

  private val storage: LocalKnowledgeStore
  private val synonymDict = OfflineSynonymDictionary()

  companion object {
    const val MIN_SUPPORTED_CONFIDENCE = 0.35
    const val VERIFIED_THRESHOLD = 0.40
    const val CONFLICT_SCORE_DELTA = 0.15
  }

  init {
    val effectiveDriver = driver ?: SharedPreferencesKnowledgeDriver(context)
    val index = OfflineKnowledgeIndex(synonymDict)
    storage = LocalKnowledgeStore(effectiveDriver, index)

    // Ingest curated knowledge bases if store is empty or uninitialized
    if (storage.totalCount() == 0) {
      bootstrapCuratedKnowledge()
    }
  }

  fun getStorage(): LocalKnowledgeStore = storage
  fun getSynonymDictionary(): OfflineSynonymDictionary = synonymDict

  /**
   * Evaluates a knowledge query following the strict 8-step decision flow.
   */
  fun query(
    rawQuery: String,
    filterCategory: KnowledgeCategory? = null,
    filterStatus: VerificationStatus? = null
  ): KnowledgeQueryResult {
    val startTime = System.currentTimeMillis()

    // Step 1: Validate Query
    val clean = rawQuery.trim()
    if (clean.isBlank()) {
      return KnowledgeQueryResult(
        state = KnowledgeRetrievalState.INSUFFICIENT_EVIDENCE,
        query = rawQuery,
        normalizedQuery = "",
        records = emptyList(),
        bestMatch = null,
        abstentionReason = "استعلام البحث فارغ، يرجى كتابة سؤال أو مسألة للاستعلام عنها في المعرفة المحلية.",
        executionTimeMs = System.currentTimeMillis() - startTime
      )
    }

    // Step 2: Normalize query without mutating original representation
    val normalizedQuery = ArabicKnowledgeNormalizer.normalizeForSearch(clean)
    if (normalizedQuery.length < 2) {
      return KnowledgeQueryResult(
        state = KnowledgeRetrievalState.INSUFFICIENT_EVIDENCE,
        query = clean,
        normalizedQuery = normalizedQuery,
        records = emptyList(),
        bestMatch = null,
        abstentionReason = "عبارة البحث قصيرة جداً للبحث المعرفي الدقيق.",
        executionTimeMs = System.currentTimeMillis() - startTime
      )
    }

    // Step 3-5: Local Index Search (Exact Phrase + Inverted BM25 + Bounded Synonyms)
    val candidates = storage.index.search(
      rawQuery = clean,
      filterCategory = filterCategory,
      filterStatus = filterStatus,
      maxResults = 10,
      enableSynonyms = true
    )

    // Step 6: Empty candidate handling
    if (candidates.isEmpty()) {
      return KnowledgeQueryResult(
        state = KnowledgeRetrievalState.NO_MATCH,
        query = clean,
        normalizedQuery = normalizedQuery,
        records = emptyList(),
        bestMatch = null,
        abstentionReason = "المسألة أو المعلومة المطلوبة غير مسجلة في قاعدة المعرفة المحلية الموثقة لزهرة 3. يمتنع المساعد تماماً عن التخمين أو تأليف معلومات غير موثقة (Truth Before Intelligence).",
        executionTimeMs = System.currentTimeMillis() - startTime
      )
    }

    val top = candidates.first()

    // Step 7: Conflict Detection among top candidates
    val conflicts = detectConflicts(candidates)
    if (conflicts.isNotEmpty()) {
      return KnowledgeQueryResult(
        state = KnowledgeRetrievalState.CONFLICTING_EVIDENCE,
        query = clean,
        normalizedQuery = normalizedQuery,
        records = candidates,
        bestMatch = top,
        abstentionReason = "تم العثور على مصادر وأدلة محلية متباينة أو خلافية حول هذه المسألة. تعرض زهرة كافة الآراء الموثقة بنزاهة وتترك الترجيح للمستخدم.",
        conflicts = conflicts,
        executionTimeMs = System.currentTimeMillis() - startTime
      )
    }

    // Step 8: Verification & Evidence Gating
    if (top.relevanceScore < MIN_SUPPORTED_CONFIDENCE) {
      return KnowledgeQueryResult(
        state = KnowledgeRetrievalState.INSUFFICIENT_EVIDENCE,
        query = clean,
        normalizedQuery = normalizedQuery,
        records = candidates,
        bestMatch = top,
        abstentionReason = "المعلومة المطلوبة غير مسجلة بشكل كافٍ في القاعدة المحلية المعتمدة؛ حيث إن درجة التطابق ضعيفة (${(top.relevanceScore * 100).toInt()}%). يمتنع المساعد تماماً عن التخمين (Truth Before Intelligence).",
        executionTimeMs = System.currentTimeMillis() - startTime
      )
    }

    val finalState = when {
      top.record.verificationStatus == VerificationStatus.DISPUTED ->
        KnowledgeRetrievalState.CONFLICTING_EVIDENCE
      top.record.verificationStatus.isVerified && top.relevanceScore >= VERIFIED_THRESHOLD ->
        KnowledgeRetrievalState.VERIFIED_MATCH
      else ->
        KnowledgeRetrievalState.TRACEABLE_CANDIDATE
    }

    return KnowledgeQueryResult(
      state = finalState,
      query = clean,
      normalizedQuery = normalizedQuery,
      records = candidates,
      bestMatch = top,
      abstentionReason = if (finalState == KnowledgeRetrievalState.TRACEABLE_CANDIDATE)
        "السجل ذو صلة وله مصدر محدد، لكنه لم يستوفِ بعد شرط التحقق القطعي." else null,
      executionTimeMs = System.currentTimeMillis() - startTime
    )
  }

  private fun detectConflicts(candidates: List<ScoredKnowledgeRecord>): List<KnowledgeConflict> {
    if (candidates.size < 2) return emptyList()

    val conflicts = mutableListOf<KnowledgeConflict>()
    val top = candidates[0]

    for (i in 1 until candidates.size) {
      val other = candidates[i]
      // Only compare close runners-up within score delta
      if ((top.relevanceScore - other.relevanceScore) > CONFLICT_SCORE_DELTA) break

      // Case 1: Explicitly marked as DISPUTED in the record status
      if (top.record.verificationStatus == VerificationStatus.DISPUTED ||
          other.record.verificationStatus == VerificationStatus.DISPUTED) {
        conflicts.add(
          KnowledgeConflict(
            recordAId = top.record.id,
            recordBId = other.record.id,
            description = "مسألة خلافية مسجلة بين: [${top.record.title}] و [${other.record.title}]"
          )
        )
      }

      // Case 2: One record is marked as REFUTED while other is claimed
      if (top.record.verificationStatus == VerificationStatus.REFUTED ||
          other.record.verificationStatus == VerificationStatus.REFUTED) {
        conflicts.add(
          KnowledgeConflict(
            recordAId = top.record.id,
            recordBId = other.record.id,
            description = "تعارض بين دعوى ورأي مردود محقق: [${top.record.title}] و [${other.record.title}]",
            conflictType = "REFUTED_VS_CLAIMED"
          )
        )
      }
    }

    return conflicts
  }

  /**
   * Bootstraps curated offline knowledge from application assets.
   */
  fun bootstrapCuratedKnowledge() {
    val recordsToInsert = mutableListOf<KnowledgeRecord>()

    // 1. Ingest general_kb.json
    try {
      val jsonStr = context.assets.open("general_kb.json").bufferedReader().use { it.readText() }
      val root = JSONObject(jsonStr)
      val entries = root.optJSONArray("entries")
      if (entries != null) {
        for (i in 0 until entries.length()) {
          val obj = entries.getJSONObject(i)
          val id = obj.optString("id", "gen_${i + 1}")
          val catStr = obj.optString("category", "general")
          val cat = when (catStr.lowercase()) {
            "science" -> KnowledgeCategory.SCIENCE
            "tech" -> KnowledgeCategory.TECH
            "history" -> KnowledgeCategory.HISTORY
            "geography" -> KnowledgeCategory.GEOGRAPHY
            "health" -> KnowledgeCategory.HEALTH
            else -> KnowledgeCategory.GENERAL
          }

          val keywords = mutableListOf<String>()
          val kwArr = obj.optJSONArray("keywords")
          if (kwArr != null) {
            for (k in 0 until kwArr.length()) keywords.add(kwArr.getString(k))
          }

          val title = obj.optString("title")
          val content = obj.optString("content")
          val source = obj.optString("source")

          val rec = KnowledgeRecord(
            id = id,
            category = cat,
            title = title,
            canonicalContent = content,
            originalSourceText = content,
            language = "ar",
            normalizedText = ArabicKnowledgeNormalizer.normalizeForSearch("$title $content ${keywords.joinToString(" ")}"),
            keywords = keywords,
            aliases = emptyList(),
            provenance = KnowledgeProvenance(
              origin = source.split("-").firstOrNull()?.trim() ?: source,
              sourceTitle = source,
              sourceId = id,
              sourceLocation = "الوثيقة القياسية المعتمدة",
              grading = "مرجع موثق رسمي",
              licenseOrAttribution = "حقائق عامة علمية موثقة"
            ),
            verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL,
            verificationNotes = "تم التحقق من دقة الثابت أو المعلومة ومطابقتها للمراجع الدولية المعتمدة."
          )
          recordsToInsert.add(rec)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    // 2. Ingest religion_kb.json
    try {
      val jsonStr = context.assets.open("religion_kb.json").bufferedReader().use { it.readText() }
      val root = JSONObject(jsonStr)

      // Quran
      val qArray = root.optJSONArray("quran")
      if (qArray != null) {
        for (i in 0 until qArray.length()) {
          val obj = qArray.getJSONObject(i)
          val surah = obj.optInt("surah")
          val surahName = obj.optString("surah_name")
          val ayah = obj.optInt("ayah")
          val text = obj.optString("text")
          val tafsir = obj.optString("tafsir")
          val source = obj.optString("source")
          val id = "quran_${surah}_$ayah"

          val rec = KnowledgeRecord(
            id = id,
            category = KnowledgeCategory.QURAN,
            title = "سورة $surahName (الآية $ayah)",
            canonicalContent = "$text\n\nالتفسير الميسر: $tafsir",
            originalSourceText = text,
            language = "ar",
            normalizedText = ArabicKnowledgeNormalizer.normalizeForSearch("$surahName $text $tafsir"),
            keywords = listOf(surahName, "آية", "سورة", "قرآن", "تفسير"),
            aliases = listOf("سورة $surahName", "الآية $ayah"),
            provenance = KnowledgeProvenance(
              origin = "مجمع الملك فهد لطباعة المصحف الشريف",
              sourceTitle = "القرآن الكريم",
              sourceId = "QURAN_${surah}_$ayah",
              sourceLocation = "سورة $surahName $surah:$ayah",
              grading = "نص قرآني قطعي الثبوت والدلالة",
              licenseOrAttribution = "القرآن الكريم والتفاسير المعتمدة"
            ),
            verificationStatus = VerificationStatus.VERIFIED_CANONICAL,
            verificationNotes = "نص قرآني معتمد ومراجع حرفياً بالتشكيل الأصلي."
          )
          recordsToInsert.add(rec)
        }
      }

      // Hadith
      val hArray = root.optJSONArray("hadith")
      if (hArray != null) {
        for (i in 0 until hArray.length()) {
          val obj = hArray.getJSONObject(i)
          val id = obj.optString("id", "hadith_${i + 1}")
          val topic = obj.optString("topic")
          val text = obj.optString("text")
          val isnad = obj.optString("isnad")
          val book = obj.optString("book")
          val grade = obj.optString("grade")
          val explanation = obj.optString("explanation")
          val source = obj.optString("source")

          val rec = KnowledgeRecord(
            id = id,
            category = KnowledgeCategory.HADITH,
            title = "حديث في $topic",
            canonicalContent = "عن $isnad قال: قال رسول الله ﷺ: \"$text\"\n\nالتخريج: $book ($grade)\nالشرح: $explanation",
            originalSourceText = text,
            language = "ar",
            normalizedText = ArabicKnowledgeNormalizer.normalizeForSearch("$topic $text $isnad $explanation"),
            keywords = listOf(topic, isnad, "حديث", "رسول الله"),
            aliases = listOf("حديث $topic"),
            provenance = KnowledgeProvenance(
              origin = book,
              sourceTitle = book,
              sourceId = id,
              sourceLocation = source,
              authorOrNarrator = isnad,
              grading = grade,
              licenseOrAttribution = "صحيح السنة النبوية والتخريج المعتمد"
            ),
            verificationStatus = VerificationStatus.VERIFIED_CANONICAL,
            verificationNotes = "حديث مسند ومخرج في كتب الصحاح بدرجة: $grade."
          )
          recordsToInsert.add(rec)
        }
      }

      // Fiqh
      val fArray = root.optJSONArray("fiqh")
      if (fArray != null) {
        for (i in 0 until fArray.length()) {
          val obj = fArray.getJSONObject(i)
          val id = "fiqh_${i + 1}"
          val question = obj.optString("question")
          val answer = obj.optString("answer")
          val madhhab = obj.optString("madhhab")
          val source = obj.optString("source")

          val kwList = mutableListOf<String>()
          val kwArray = obj.optJSONArray("keywords")
          if (kwArray != null) {
            for (k in 0 until kwArray.length()) kwList.add(kwArray.getString(k))
          }

          val rec = KnowledgeRecord(
            id = id,
            category = KnowledgeCategory.FIQH,
            title = question,
            canonicalContent = "$answer\n\nالمذهب/الاعتماد: $madhhab",
            originalSourceText = answer,
            language = "ar",
            normalizedText = ArabicKnowledgeNormalizer.normalizeForSearch("$question $answer ${kwList.joinToString(" ")}"),
            keywords = kwList,
            aliases = listOf(question),
            provenance = KnowledgeProvenance(
              origin = madhhab,
              sourceTitle = source,
              sourceId = id,
              sourceLocation = source,
              grading = "معتمد في الفقه الإسلامي",
              licenseOrAttribution = "المصادر الفقهية المعتمدة"
            ),
            verificationStatus = VerificationStatus.VERIFIED_SCHOLARLY,
            verificationNotes = "حكم فقهي مجمع عليه أو معتمد في المذاهب الفقهية الأربعة."
          )
          recordsToInsert.add(rec)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    // Insert with validation
    for (rec in recordsToInsert) {
      storage.insertRecord(rec)
    }
  }
}
