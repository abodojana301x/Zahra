package com.example.zahra.knowledge.index

import com.example.zahra.knowledge.model.*
import com.example.zahra.knowledge.normalizer.ArabicKnowledgeNormalizer
import com.example.zahra.knowledge.synonyms.OfflineSynonymDictionary
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance deterministic Inverted Index for Offline Local Search.
 * 
 * Features:
 * 1. Token Inverted Index with BM25-style scoring and length normalization.
 * 2. Field weighting (Title > Keywords > Aliases > Content).
 * 3. Exact phrase detection with distinct match method tagging.
 * 4. Bounded synonym query expansion with discounted scoring.
 * 5. Deterministic tie-breaking on identical scores using record ID.
 * 6. Hardened against adversarial, empty, or multi-kilobyte queries.
 */
class OfflineKnowledgeIndex(
  private val synonymDict: OfflineSynonymDictionary = OfflineSynonymDictionary()
) {

  data class Posting(
    val recordId: String,
    val termFreq: Int,
    val fieldWeight: Double
  )

  private val recordsMap = LinkedHashMap<String, KnowledgeRecord>()
  // Term -> List<Posting>
  private val invertedIndex = mutableMapOf<String, MutableList<Posting>>()
  // RecordId -> total token count
  private val recordLengths = mutableMapOf<String, Int>()
  private var avgRecordLength = 1.0

  companion object {
    const val WEIGHT_TITLE = 3.0
    const val WEIGHT_KEYWORDS = 2.5
    const val WEIGHT_ALIASES = 2.0
    const val WEIGHT_CONTENT = 1.0

    // BM25 parameters
    private const val K1 = 1.2
    private const val B = 0.75

    // Query safety limits
    const val MAX_QUERY_LENGTH = 1000
    const val MIN_SEARCHABLE_CHARS = 2
  }

  @Synchronized
  fun addRecord(record: KnowledgeRecord) {
    recordsMap[record.id] = record
    indexRecord(record)
    recalculateAvgLength()
  }

  @Synchronized
  fun addAll(records: Collection<KnowledgeRecord>) {
    for (r in records) {
      recordsMap[r.id] = r
      indexRecord(r)
    }
    recalculateAvgLength()
  }

  @Synchronized
  fun removeRecord(recordId: String): Boolean {
    val removed = recordsMap.remove(recordId) != null
    if (removed) {
      recordLengths.remove(recordId)
      // Clean inverted index postings for this record
      for (entry in invertedIndex.values) {
        entry.removeAll { it.recordId == recordId }
      }
      recalculateAvgLength()
    }
    return removed
  }

  @Synchronized
  fun clear() {
    recordsMap.clear()
    invertedIndex.clear()
    recordLengths.clear()
    avgRecordLength = 1.0
  }

  @Synchronized
  fun getRecord(id: String): KnowledgeRecord? = recordsMap[id]

  @Synchronized
  fun getAllRecords(): List<KnowledgeRecord> = recordsMap.values.toList()

  @Synchronized
  fun size(): Int = recordsMap.size

  private fun indexRecord(record: KnowledgeRecord) {
    // 1. Title tokens
    val titleTokens = ArabicKnowledgeNormalizer.extractTokens(record.title, filterStopWords = false)
    // 2. Keywords
    val kwTokens = record.keywords.flatMap { ArabicKnowledgeNormalizer.extractTokens(it, filterStopWords = false) }
    // 3. Aliases
    val aliasTokens = record.aliases.flatMap { ArabicKnowledgeNormalizer.extractTokens(it, filterStopWords = false) }
    // 4. Content tokens
    val contentTokens = ArabicKnowledgeNormalizer.extractTokens(record.canonicalContent, filterStopWords = true)

    val totalTokens = titleTokens.size + kwTokens.size + aliasTokens.size + contentTokens.size
    recordLengths[record.id] = max(1, totalTokens)

    // Build field frequency maps
    fun addPostings(tokens: List<String>, weight: Double) {
      val tfMap = mutableMapOf<String, Int>()
      for (tok in tokens) {
        val stems = ArabicKnowledgeNormalizer.getBoundedStems(tok)
        for (stem in stems) {
          tfMap[stem] = (tfMap[stem] ?: 0) + 1
        }
      }
      for ((stem, freq) in tfMap) {
        val list = invertedIndex.getOrPut(stem) { mutableListOf() }
        list.add(Posting(record.id, freq, weight))
      }
    }

    addPostings(titleTokens, WEIGHT_TITLE)
    addPostings(kwTokens, WEIGHT_KEYWORDS)
    addPostings(aliasTokens, WEIGHT_ALIASES)
    addPostings(contentTokens, WEIGHT_CONTENT)
  }

  private fun recalculateAvgLength() {
    val total = recordLengths.values.sum()
    avgRecordLength = if (recordLengths.isNotEmpty()) max(1.0, total.toDouble() / recordLengths.size) else 1.0
  }

  /**
   * Search execution with transparent scoring, deterministic tie-breaking, and boundary protections.
   */
  @Synchronized
  fun search(
    rawQuery: String,
    filterCategory: KnowledgeCategory? = null,
    filterStatus: VerificationStatus? = null,
    filterLanguage: String? = null,
    maxResults: Int = 10,
    enableSynonyms: Boolean = true
  ): List<ScoredKnowledgeRecord> {
    if (rawQuery.isBlank()) return emptyList()

    // 1. Sanitize & clamp query length against adversarial large payloads
    val safeQuery = if (rawQuery.length > MAX_QUERY_LENGTH) rawQuery.substring(0, MAX_QUERY_LENGTH) else rawQuery
    val normalizedQuery = ArabicKnowledgeNormalizer.normalizeForSearch(safeQuery)
    if (normalizedQuery.length < MIN_SEARCHABLE_CHARS) return emptyList()

    val queryTokens = ArabicKnowledgeNormalizer.extractTokens(safeQuery, filterStopWords = true)
    if (queryTokens.isEmpty()) return emptyList()

    val totalDocs = max(1, recordsMap.size)
    val docScores = mutableMapOf<String, Double>()
    val docMatchMethods = mutableMapOf<String, MatchMethod>()
    val docMatchedTokens = mutableMapOf<String, MutableSet<String>>()

    // A. Check exact phrase match across corpus
    for ((recId, rec) in recordsMap) {
      if (!matchesFilter(rec, filterCategory, filterStatus, filterLanguage)) continue

      val normTitle = ArabicKnowledgeNormalizer.normalizeForSearch(rec.title)
      val normContent = ArabicKnowledgeNormalizer.normalizeForSearch(rec.canonicalContent)
      val normOriginal = ArabicKnowledgeNormalizer.normalizeForSearch(rec.originalSourceText)

      if (normTitle.contains(normalizedQuery) || normContent.contains(normalizedQuery) || normOriginal.contains(normalizedQuery)) {
        docScores[recId] = (docScores[recId] ?: 0.0) + 1.2 // Exact phrase decisive boost
        docMatchMethods[recId] = MatchMethod.EXACT_PHRASE
        docMatchedTokens.getOrPut(recId) { mutableSetOf() }.add(normalizedQuery)
      }
    }

    // B. Token-based full-text matching with BM25-style saturation and field weights
    for (qToken in queryTokens) {
      val stems = ArabicKnowledgeNormalizer.getBoundedStems(qToken)
      for (stem in stems) {
        val postings = invertedIndex[stem] ?: continue
        val docFreq = postings.map { it.recordId }.distinct().size
        val idf = ln((totalDocs - docFreq + 0.5) / (docFreq + 0.5) + 1.0)

        for (p in postings) {
          val rec = recordsMap[p.recordId] ?: continue
          if (!matchesFilter(rec, filterCategory, filterStatus, filterLanguage)) continue

          val docLen = recordLengths[p.recordId] ?: 1
          val tf = p.termFreq
          val tfNorm = (tf * (K1 + 1.0)) / (tf + K1 * (1.0 - B + B * (docLen / avgRecordLength)))
          val tokenScore = idf * tfNorm * p.fieldWeight

          docScores[p.recordId] = (docScores[p.recordId] ?: 0.0) + tokenScore
          docMatchedTokens.getOrPut(p.recordId) { mutableSetOf() }.add(qToken)
          if (!docMatchMethods.containsKey(p.recordId)) {
            docMatchMethods[p.recordId] = MatchMethod.EXACT_TOKEN
          }
        }
      }
    }

    // C. Bounded synonym query expansion (discounted weight to never outrank direct matches)
    if (enableSynonyms) {
      val expansions = synonymDict.getBoundedExpansions(safeQuery, maxExpansions = 3)
      for (expandedPhrase in expansions) {
        val expTokens = ArabicKnowledgeNormalizer.extractTokens(expandedPhrase, filterStopWords = true)
        for (expTok in expTokens) {
          if (queryTokens.contains(expTok)) continue // Avoid double-counting direct tokens
          val stems = ArabicKnowledgeNormalizer.getBoundedStems(expTok)
          for (stem in stems) {
            val postings = invertedIndex[stem] ?: continue
            val docFreq = postings.map { it.recordId }.distinct().size
            val idf = ln((totalDocs - docFreq + 0.5) / (docFreq + 0.5) + 1.0)

            for (p in postings) {
              val rec = recordsMap[p.recordId] ?: continue
              if (!matchesFilter(rec, filterCategory, filterStatus, filterLanguage)) continue

              val docLen = recordLengths[p.recordId] ?: 1
              val tf = p.termFreq
              val tfNorm = (tf * (K1 + 1.0)) / (tf + K1 * (1.0 - B + B * (docLen / avgRecordLength)))
              // Apply 0.60 discount factor for synonym expansions
              val tokenScore = idf * tfNorm * p.fieldWeight * 0.60

              docScores[p.recordId] = (docScores[p.recordId] ?: 0.0) + tokenScore
              docMatchedTokens.getOrPut(p.recordId) { mutableSetOf() }.add("مرادف: $expTok")
              if (docMatchMethods[p.recordId] == null) {
                docMatchMethods[p.recordId] = MatchMethod.BOUNDED_SYNONYM
              }
            }
          }
        }
      }
    }

    if (docScores.isEmpty()) return emptyList()

    // Normalize scores to 0.0 .. 1.0 range
    val maxScore = docScores.values.maxOrNull() ?: 1.0
    val scoredList = docScores.mapNotNull { (recId, rawScore) ->
      val rec = recordsMap[recId] ?: return@mapNotNull null
      val matched = docMatchedTokens[recId]?.toList() ?: emptyList()
      val method = docMatchMethods[recId] ?: MatchMethod.TOKEN_COVERAGE

      // Calculate substantive query token coverage
      val substantiveMatches = matched.filter { !it.startsWith("مرادف:") }
      val coverageRatio = if (method == MatchMethod.BOUNDED_SYNONYM) {
        0.85
      } else if (queryTokens.isNotEmpty()) {
        min(1.0, substantiveMatches.size.toDouble() / queryTokens.size)
      } else 1.0

      // Exact phrase retains full base score.
      // Multi-word queries must scale by token coverage so a single stray token in a long query
      // cannot yield an artificially high score.
      val baseScore = if (maxScore > 0.0) min(0.99, max(0.10, rawScore / maxScore)) else 0.50
      val normScore = if (method == MatchMethod.EXACT_PHRASE) {
        baseScore
      } else if (queryTokens.size >= 3 && coverageRatio < 0.40) {
        // Less than 40% of substantive query tokens matched -> low confidence (abstention)
        min(0.25, baseScore * coverageRatio)
      } else {
        baseScore * (0.35 + 0.65 * coverageRatio)
      }

      val explanation = when (method) {
        MatchMethod.EXACT_PHRASE -> "مطابقة تامة لعبارة البحث في نص السجل"
        MatchMethod.EXACT_TOKEN -> "تطابق ${matched.size} كلمة دلالية مباشرة"
        MatchMethod.BOUNDED_SYNONYM -> "تطابق عبر توسيع مرادفات معجمية محلية محددة"
        else -> "تطابق جزئي في الكلمات"
      }

      val warning = if (!rec.verificationStatus.isVerified) {
        "تنبيه نزاهة: السجل مصنف [${rec.verificationStatus.labelAr}] ولم يكتمل التحقق القطعي منه."
      } else null

      ScoredKnowledgeRecord(
        record = rec,
        relevanceScore = normScore,
        matchMethod = method,
        matchedTokens = matched,
        relevanceExplanation = explanation,
        warning = warning
      )
    }

    // Deterministic sorting with stable tie-breaking:
    // Primary: relevanceScore descending
    // Secondary: record.verificationStatus.isVerified descending (verified preferred on equal score)
    // Tertiary: record.id ascending (deterministic tie-breaker)
    return scoredList.sortedWith(
      compareByDescending<ScoredKnowledgeRecord> { it.relevanceScore }
        .thenByDescending { it.record.verificationStatus.isVerified }
        .thenBy { it.record.id }
    ).take(maxResults)
  }

  private fun matchesFilter(
    record: KnowledgeRecord,
    filterCategory: KnowledgeCategory?,
    filterStatus: VerificationStatus?,
    filterLanguage: String?
  ): Boolean {
    if (filterCategory != null && record.category != filterCategory) return false
    if (filterStatus != null && record.verificationStatus != filterStatus) return false
    if (filterLanguage != null && record.language != filterLanguage && record.language != "mixed") return false
    return true
  }
}
