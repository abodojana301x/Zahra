package com.example.zahra.knowledge.model

/**
 * Knowledge Categories supported by Zahra 3 Local Knowledge Engine.
 */
enum class KnowledgeCategory(val labelAr: String, val labelEn: String) {
  QURAN("القرآن الكريم", "Quran"),
  HADITH("الحديث النبوي", "Hadith"),
  FIQH("الفقه والأحكام", "Fiqh"),
  SCIENCE("العلوم الطبيعية والفيزياء", "Science"),
  HISTORY("التاريخ والحضارات", "History"),
  GEOGRAPHY("الجغرافيا والمعالم", "Geography"),
  TECH("التقنية والمعلوماتية", "Technology"),
  HEALTH("الصحة والطب الوقائي", "Health"),
  GENERAL("معارف عامة موثقة", "General Knowledge")
}

/**
 * Verification Status strictly distinguishing truth and consensus from unverified claims.
 * A record passing schema validation NEVER automatically receives verified status.
 */
enum class VerificationStatus(val labelAr: String, val isVerified: Boolean) {
  VERIFIED_CANONICAL("موثق قطعي - مصدر أولي معتمد", true),
  VERIFIED_SCHOLARLY("موثق - إجماع أو اعتماد علمي/فقهي", true),
  VERIFIED_EMPIRICAL("موثق - قياس تجريبي/علمي قياسي", true),
  VERIFIED_ACADEMIC_PEER("موثق - مراجعة أقران أكاديمية", true),
  UNVERIFIED_CLAIM("ادعاء غير مثبت - يفتقر لسند أولي", false),
  DISPUTED("مسألة خلافية - تتعدد فيها الآراء الموثقة", false),
  REFUTED("مردود / منسوب خطأً بعد التحقيق", false)
}

/**
 * Explicit retrieval method so callers know how a result was found.
 */
enum class MatchMethod(val labelAr: String, val baseConfidence: Double) {
  EXACT_PHRASE("مطابقة تامة للعبارة", 0.98),
  EXACT_TOKEN("مطابقة لفظية مباشرة", 0.90),
  TOKEN_COVERAGE("تطابق الكلمات الدلالية", 0.80),
  BOUNDED_SYNONYM("توسيع معجمي مقيد ومحلي", 0.65),
  FUZZY_TOKEN("مطابقة تقريبية موجهة", 0.50)
}

/**
 * High-level Epistemic Retrieval State.
 * Zahra NEVER conflates a document match with factual truth.
 */
enum class KnowledgeRetrievalState(val labelAr: String) {
  VERIFIED_MATCH("مطابقة موثقة مكتملة الإسناد"),
  TRACEABLE_CANDIDATE("نتيجة مرجعية ذات مصدر محدد"),
  CONFLICTING_EVIDENCE("أدلة متعارضة أو متنازع عليها"),
  INSUFFICIENT_EVIDENCE("الأدلة المحلية غير كافية (امتناع صادق)"),
  NO_MATCH("لا توجد سجلات مطابقة في القاعدة المحلية"),
  ERROR("خطأ في استعلام المعرفة")
}

/**
 * Provenance metadata guaranteeing complete traceability of every claim.
 */
data class KnowledgeProvenance(
  val origin: String,                 // e.g. "المكتب الدولي للأوزان والمقاييس BIPM", "مجمع الملك فهد لطباعة المصحف الشريف"
  val sourceTitle: String,            // e.g. "النظام الدولي للوحدات SI", "القرآن الكريم", "صحيح البخاري"
  val sourceId: String,               // e.g. "BIPM_SI_C", "QURAN_1_1", "BUKHARI_1"
  val sourceLocation: String,         // e.g. "الجدول 1 - الثوابت الفيزيائية", "سورة الفاتحة 1:1", "حديث رقم 1"
  val authorOrNarrator: String? = null,// e.g. "عمر بن الخطاب رضي الله عنه"
  val grading: String? = null,        // e.g. "صحيح متفق عليه", "قياسي رسمي"
  val ingestDate: Long = System.currentTimeMillis(),
  val licenseOrAttribution: String = "محقق وموثق محلياً"
)

/**
 * Structured, versioned knowledge record.
 */
data class KnowledgeRecord(
  val id: String,
  val category: KnowledgeCategory,
  val title: String,
  val canonicalContent: String,
  val originalSourceText: String,      // Strictly unadulterated original text with diacritics/accents preserved
  val language: String = "ar",        // "ar", "en", "mixed"
  val normalizedText: String,          // Derived text used for indexing and search matching
  val keywords: List<String> = emptyList(),
  val aliases: List<String> = emptyList(),
  val provenance: KnowledgeProvenance,
  val verificationStatus: VerificationStatus,
  val verificationNotes: String = "",
  val relatedRecordIds: List<String> = emptyList(),
  val schemaVersion: Int = 1,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Sealed result for record validation at ingestion and mutation boundaries.
 */
sealed interface KnowledgeValidationResult {
  data class Valid(val record: KnowledgeRecord) : KnowledgeValidationResult
  data class Invalid(val errors: List<String>, val rejectedRecordId: String?) : KnowledgeValidationResult
}

/**
 * Represents a conflict between two records on the same domain or entity.
 */
data class KnowledgeConflict(
  val recordAId: String,
  val recordBId: String,
  val description: String,
  val conflictType: String = "CONTRADICTORY_CLAIMS"
)

/**
 * Scored record from retrieval ranking.
 */
data class ScoredKnowledgeRecord(
  val record: KnowledgeRecord,
  val relevanceScore: Double,          // 0.0 to 1.0 (represents retrieval relevance, NOT factual truth)
  val matchMethod: MatchMethod,
  val matchedTokens: List<String>,
  val relevanceExplanation: String,
  val warning: String? = null
)

/**
 * Full structured result returned by the Knowledge Engine.
 */
data class KnowledgeQueryResult(
  val state: KnowledgeRetrievalState,
  val query: String,
  val normalizedQuery: String,
  val records: List<ScoredKnowledgeRecord>,
  val bestMatch: ScoredKnowledgeRecord?,
  val abstentionReason: String? = null,
  val conflicts: List<KnowledgeConflict> = emptyList(),
  val executionTimeMs: Long = 0L
)
