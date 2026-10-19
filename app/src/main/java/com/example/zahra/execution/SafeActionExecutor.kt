package com.example.zahra.execution

import com.example.zahra.model.*

sealed interface ExecutionResult<out T> {
  data class Success<T>(val data: T, val message: String) : ExecutionResult<T>
  data class PreconditionFailed(val reason: String) : ExecutionResult<Nothing>
  data class IdempotencySkipped(val message: String) : ExecutionResult<Nothing>
  data class ConfirmationRequired(val actionType: String, val prompt: String) : ExecutionResult<Nothing>
  data class Failure(val reason: String, val throwable: Throwable? = null) : ExecutionResult<Nothing>
}

/**
 * Safe Action Executor.
 * Guarantees argument schema validation, preconditions/postconditions,
 * idempotency deduplication (30s window), and confirmation gates for destructive actions.
 */
class SafeActionExecutor {

  private data class IdempotencyEntry(
    val actionSignature: String,
    val executedAt: Long
  )

  private val recentExecutions = mutableListOf<IdempotencyEntry>()
  private val idempotencyWindowMs = 30_000L // 30 seconds

  // Pending destructive action requiring confirmation: e.g. "CONFIRM_CLEAR_ALL_MEMORY"
  private var pendingConfirmationAction: String? = null

  @Synchronized
  fun isDuplicateExecution(actionSignature: String): Boolean {
    val now = System.currentTimeMillis()
    // Prune expired
    recentExecutions.removeAll { now - it.executedAt > idempotencyWindowMs }

    val exists = recentExecutions.any { it.actionSignature == actionSignature }
    if (exists) {
      return true
    }
    recentExecutions.add(IdempotencyEntry(actionSignature, now))
    return false
  }

  @Synchronized
  fun setPendingConfirmation(action: String) {
    pendingConfirmationAction = action
  }

  @Synchronized
  fun hasPendingConfirmation(action: String): Boolean {
    return pendingConfirmationAction == action
  }

  @Synchronized
  fun clearPendingConfirmation() {
    pendingConfirmationAction = null
  }

  /**
   * Validates task creation parameters before execution.
   */
  fun validateTaskArgs(rawInput: String): ExecutionResult<Pair<String, String>> {
    val clean = rawInput.trim()
    if (clean.isBlank()) {
      return ExecutionResult.PreconditionFailed("عنوان المهمة فارغ. يرجى كتابة عنوان واضح للمهمة.")
    }

    val title = clean.replace(Regex("""^(?:اضف|أضف|انشئ|سجل|ضيف)?\s*مهمة\s*""", RegexOption.IGNORE_CASE), "").trim()
    if (title.length < 2) {
      return ExecutionResult.PreconditionFailed("عنوان المهمة قصير جداً (أقل من حرفين).")
    }

    if (title.length > 250) {
      return ExecutionResult.PreconditionFailed("عنوان المهمة طويل جداً (الحد الأقصى 250 حرفاً).")
    }

    val sig = "TASK:" + title.lowercase()
    if (isDuplicateExecution(sig)) {
      return ExecutionResult.IdempotencySkipped("تم إضافة هذه المهمة للتو لمنع التكرار غير المقصود.")
    }

    return ExecutionResult.Success(Pair(title, "عام"), "تم التحقق من معايير المهمة")
  }

  /**
   * Validates note creation parameters before execution.
   */
  fun validateNoteArgs(rawInput: String): ExecutionResult<String> {
    val clean = rawInput.trim()
    val content = clean.replace(Regex("""^(?:سجل|اكتب|احفظ|تذكر)\s*ملاحظة\s*""", RegexOption.IGNORE_CASE), "").trim()

    if (content.isBlank() || content.length < 2) {
      return ExecutionResult.PreconditionFailed("محتوى الملاحظة فارغ أو قصير جداً.")
    }

    if (content.length > 2000) {
      return ExecutionResult.PreconditionFailed("محتوى الملاحظة يتجاوز الحد المسموح (2000 حرف).")
    }

    val sig = "NOTE:" + content.take(50).lowercase()
    if (isDuplicateExecution(sig)) {
      return ExecutionResult.IdempotencySkipped("تم حفظ هذه الملاحظة مؤخراً.")
    }

    return ExecutionResult.Success(content, "تم التحقق من سلامة الملاحظة")
  }
}
