package com.example.zahra.engines

import com.example.zahra.core.ZahraNormalizer
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max

data class ExtractiveSummary(
  val summary: String,
  val keySentences: List<String>,
  val compressionRatio: Double,
  val source: String
)

object SummarizeEngine {
  fun summarize(text: String, maxSentences: Int = 3): ExtractiveSummary {
    if (text.isBlank()) {
      return ExtractiveSummary("", emptyList(), 0.0, "خوارزمية TextRank الاستخلاصية")
    }

    val sentences = text.split(Regex("(?<=[.!?؟\\n؛])\\s+"))
      .map { it.trim() }
      .filter { it.length > 15 }

    if (sentences.size <= maxSentences) {
      return ExtractiveSummary(
        summary = text,
        keySentences = sentences,
        compressionRatio = 1.0,
        source = "خوارزمية TextRank الاستخلاصية (Mihalcea & Tarau)"
      )
    }

    val tokenSets = sentences.map { s -> ZahraNormalizer.extractWords(s).toSet() }
    val n = sentences.size
    val weights = Array(n) { DoubleArray(n) { 0.0 } }

    for (i in 0 until n) {
      for (j in i + 1 until n) {
        val setA = tokenSets[i]
        val setB = tokenSets[j]
        val overlap = setA.intersect(setB).size
        if (overlap > 0) {
          val denom = ln(max(2, setA.size).toDouble()) + ln(max(2, setB.size).toDouble())
          val sim = overlap / denom
          weights[i][j] = sim
          weights[j][i] = sim
        }
      }
    }

    // PageRank power iteration
    val d = 0.85
    var scores = DoubleArray(n) { 1.0 }
    val maxIter = 30
    val threshold = 0.0001

    for (iter in 0 until maxIter) {
      val nextScores = DoubleArray(n) { 1.0 - d }
      for (i in 0 until n) {
        var sum = 0.0
        for (j in 0 until n) {
          if (i == j) continue
          val w = weights[j][i]
          if (w > 0.0) {
            val outWeight = weights[j].sum()
            if (outWeight > 0.0) {
              sum += (w / outWeight) * scores[j]
            }
          }
        }
        nextScores[i] += d * sum
      }

      var diff = 0.0
      for (i in 0 until n) {
        diff += abs(nextScores[i] - scores[i])
      }
      scores = nextScores
      if (diff < threshold) break
    }

    val rankedIndices = scores.mapIndexed { index, score -> index to score }
      .sortedByDescending { it.second }
      .take(maxSentences)
      .map { it.first }
      .sorted()

    val chosen = rankedIndices.map { sentences[it] }
    val summaryText = chosen.joinToString("\n• ", prefix = "• ")
    val ratio = if (text.isNotEmpty()) summaryText.length.toDouble() / text.length else 0.0

    return ExtractiveSummary(
      summary = summaryText,
      keySentences = chosen,
      compressionRatio = (ratio * 100).toInt() / 100.0,
      source = "خوارزمية TextRank الاستخلاصية (Mihalcea & Tarau)"
    )
  }
}
