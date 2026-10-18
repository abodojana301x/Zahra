package com.example.zahra.knowledge.model

/**
 * Validates KnowledgeRecord integrity at ingestion, insertion, and update boundaries.
 * Enforces architectural constraints:
 * - Structural validity != verified truth.
 * - Non-empty required fields.
 * - Non-blank provenance and citations.
 * - Identifier formatting and stability.
 */
object KnowledgeRecordValidator {

  private val ID_REGEX = Regex("^[a-zA-Z0-9_-]{3,100}$")

  fun validate(record: KnowledgeRecord): KnowledgeValidationResult {
    val errors = mutableListOf<String>()

    // 1. Identity validation
    if (record.id.isBlank()) {
      errors.add("معرف السجل (id) لا يمكن أن يكون فارغاً.")
    } else if (!ID_REGEX.matches(record.id)) {
      errors.add("معرف السجل [${record.id}] غير صالح. يجب أن يتكون من 3 إلى 100 حرف (أحرف إنجليزية، أرقام، أو _ أو -).")
    }

    // 2. Content validation
    if (record.title.isBlank()) {
      errors.add("عنوان السجل المعرفي لا يمكن أن يكون فارغاً.")
    } else if (record.title.length > 300) {
      errors.add("عنوان السجل طويل جداً (تجاوز 300 حرف).")
    }

    if (record.canonicalContent.isBlank()) {
      errors.add("المحتوى المعتمد للسجل لا يمكن أن يكون فارغاً.")
    }

    if (record.originalSourceText.isBlank()) {
      errors.add("النص الأصلي من المصدر لا يمكن أن يكون فارغاً (يجب حفظ النص الخام للاقتباس).")
    }

    // 3. Provenance validation (Mandatory for Epistemic Integrity)
    val prov = record.provenance
    if (prov.origin.isBlank()) {
      errors.add("جهة الإسناد أو جهة الإصدار (provenance.origin) غير محددة.")
    }
    if (prov.sourceTitle.isBlank()) {
      errors.add("عنوان المصدر (provenance.sourceTitle) غير محدد.")
    }
    if (prov.sourceLocation.isBlank()) {
      errors.add("الموضع الدقيق في المصدر (provenance.sourceLocation) غير محدد (سورة، رقم حديث، صفحة، أو وثيقة).")
    }

    // 4. Schema version
    if (record.schemaVersion < 1) {
      errors.add("إصدار المخطط (${record.schemaVersion}) غير صالح.")
    }

    return if (errors.isEmpty()) {
      KnowledgeValidationResult.Valid(record)
    } else {
      KnowledgeValidationResult.Invalid(errors, record.id.ifBlank { null })
    }
  }
}
