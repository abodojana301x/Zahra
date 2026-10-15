package com.example.zahra.model

enum class ConfidenceTier {
  DIRECT,    // >= 0.75
  QUALIFIED, // 0.40 - 0.74
  UNCERTAIN  // < 0.40
}

enum class Sender {
  USER,
  ZAHRA
}

enum class FactCategory(val labelAr: String, val descriptionAr: String) {
  PERSONAL("شخصي", "الاسم والبيانات الشخصية"),
  LOCATION_WORK("المدينة والعمل", "الموقع الجغرافي والمهنة"),
  PREFERENCE("التفضيلات", "تفضيلاتك وأسلوبك المفضل"),
  INTERESTS("الاهتمامات", "المجالات والمشاريع المتابعة")
}

data class ZahraUserFact(
  val id: String = java.util.UUID.randomUUID().toString(),
  val category: FactCategory,
  val key: String,
  val value: String,
  val confidence: Double = 1.0,
  val createdAt: Long = System.currentTimeMillis()
)

sealed interface ZahraToolPayload {
  data class TaskPayload(
    val task: ZahraTask
  ) : ZahraToolPayload

  data class NotePayload(
    val note: ZahraNote
  ) : ZahraToolPayload

  data class TranslatePayload(
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String
  ) : ZahraToolPayload

  data class MathPayload(
    val expression: String,
    val result: String,
    val calculationType: String = "حساب دقيق" // "حساب دقيق" or "تحويل وحدات"
  ) : ZahraToolPayload

  data class ReligionPayload(
    val category: String, // "مواقيت الصلاة", "حديث شريف", "قرآن كريم", "أذكار وتسبيح", "فقه"
    val title: String,
    val content: String,
    val source: String,
    val isPrayerTime: Boolean = false,
    val prayerTimes: List<Pair<String, String>> = emptyList(), // e.g. [("الفجر", "04:45"), ("الظهر", "11:58"), ...]
    val isTasbih: Boolean = false,
    val tasbihTarget: Int = 33
  ) : ZahraToolPayload

  data class SummarizePayload(
    val summary: String,
    val originalLength: Int,
    val summaryLength: Int,
    val compressionRatio: Double,
    val bulletPoints: List<String> = emptyList()
  ) : ZahraToolPayload

  data class MemoryFactPayload(
    val fact: ZahraUserFact,
    val isNew: Boolean = true
  ) : ZahraToolPayload

  data class ImageGenerationPayload(
    val prompt: String,
    val title: String,
    val mode: String = "vector", // "vector", "quote_card", "pixel_art"
    val shapeStyle: String = "galaxy", // "galaxy", "mandala", "nature", "geometric", "cyber"
    val primaryColorHex: Long = 0xFF3B82F6,
    val secondaryColorHex: Long = 0xFF8B5CF6,
    val quoteArabic: String = "",
    val quoteAuthor: String = "حكمة عربية",
    val pixelGrid: List<List<Int>> = emptyList(), // 16x16 color index matrix
    val palette: List<Long> = emptyList(),
    val seed: Long = System.currentTimeMillis()
  ) : ZahraToolPayload

  data class SchedulePayload(
    val schedule: ZahraSchedule,
    val isNew: Boolean = true,
    val executionSummary: String = "تمت الجدولة بنجاح"
  ) : ZahraToolPayload
}

data class DeepThinkingPayload(
  val steps: List<ThinkingStep>,
  val finalSynthesis: String,
  val confidence: Double = 0.98,
  val processingTimeMs: Long = 1200L
) : ZahraToolPayload

data class ThinkingStep(
  val id: String = java.util.UUID.randomUUID().toString(),
  val phaseName: String, // e.g. "تحليل النية والأمر", "استرجاع الذاكرة والسياق", "التخطيط المنطقي للوكيل", "التحقق والتدقيق"
  val thoughtDetail: String,
  val isCompleted: Boolean = true
)

data class ZahraSchedule(
  val id: String = java.util.UUID.randomUUID().toString(),
  val title: String,
  val message: String,
  val timeDisplay: String, // e.g. "08:30 ص"
  val hour: Int,
  val minute: Int,
  val isDaily: Boolean = true,
  val isEnabled: Boolean = true,
  val actionType: String = "REMINDER", // "REMINDER", "DAILY_MOTIVATION", "PRAYER_ALERT", "TASK_CHECK"
  val createdAt: Long = System.currentTimeMillis()
)

data class ZahraMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val sender: Sender,
  val text: String,
  val intent: String? = null,
  val confidence: Double = 1.0,
  val confidenceTier: ConfidenceTier = ConfidenceTier.DIRECT,
  val source: String? = null,
  val followups: List<String> = emptyList(),
  val toolPayload: ZahraToolPayload? = null,
  val deepThinking: DeepThinkingPayload? = null,
  val timestamp: Long = System.currentTimeMillis()
)

data class ZahraTask(
  val id: String = java.util.UUID.randomUUID().toString(),
  val title: String,
  val category: String, // عمل, شخصي, عاجل, مالي, صحي, عام
  val priorityScore: Int = 2, // 1 to 5
  val dueDate: String? = null,
  val completed: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

data class ZahraNote(
  val id: String = java.util.UUID.randomUUID().toString(),
  val title: String,
  val content: String,
  val tags: List<String> = emptyList(),
  val createdAt: Long = System.currentTimeMillis()
)

data class ZahraResponse(
  val text: String,
  val intent: String,
  val confidence: Double,
  val confidenceTier: ConfidenceTier,
  val source: String? = null,
  val followups: List<String> = emptyList(),
  val toolPayload: ZahraToolPayload? = null,
  val deepThinking: DeepThinkingPayload? = null,
  val createdTask: ZahraTask? = null,
  val createdNote: ZahraNote? = null,
  val createdSchedule: ZahraSchedule? = null
)

data class ZahraSession(
  val id: String = java.util.UUID.randomUUID().toString(),
  val title: String,
  val messages: List<ZahraMessage> = emptyList(),
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
