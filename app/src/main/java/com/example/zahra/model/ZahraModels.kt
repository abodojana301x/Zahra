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

data class ZahraMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val sender: Sender,
  val text: String,
  val intent: String? = null,
  val confidence: Double = 1.0,
  val confidenceTier: ConfidenceTier = ConfidenceTier.DIRECT,
  val source: String? = null,
  val followups: List<String> = emptyList(),
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
  val createdTask: ZahraTask? = null,
  val createdNote: ZahraNote? = null
)
