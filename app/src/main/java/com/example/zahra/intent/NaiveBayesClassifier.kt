package com.example.zahra.intent

import com.example.zahra.core.ZahraNormalizer
import kotlin.math.exp
import kotlin.math.ln

data class IntentScore(
  val intent: String,
  val confidence: Double
)

class NaiveBayesClassifier {
  private val classDocCounts = mutableMapOf<String, Int>()
  private val classWordCounts = mutableMapOf<String, MutableMap<String, Int>>()
  private val classTotalWords = mutableMapOf<String, Int>()
  private val vocabulary = mutableSetOf<String>()
  private var totalDocs: Int = 0

  fun train(corpus: Map<String, List<String>>) {
    for ((intent, samples) in corpus) {
      val wordMap = classWordCounts.getOrPut(intent) { mutableMapOf() }
      var totalWords = classTotalWords.getOrDefault(intent, 0)
      var docCount = classDocCounts.getOrDefault(intent, 0)

      for (sample in samples) {
        totalDocs++
        docCount++
        val words = ZahraNormalizer.extractWords(sample)
        for (w in words) {
          vocabulary.add(w)
          wordMap[w] = (wordMap[w] ?: 0) + 1
          totalWords++
        }
      }
      classDocCounts[intent] = docCount
      classTotalWords[intent] = totalWords
    }
  }

  fun classify(text: String): IntentScore {
    val words = ZahraNormalizer.extractWords(text)
    val vocabSize = vocabulary.size.coerceAtLeast(1)

    if (words.isEmpty() || totalDocs == 0) {
      return IntentScore("unknown", 0.1)
    }

    val logScores = mutableListOf<Pair<String, Double>>()

    for ((intent, docCount) in classDocCounts) {
      // Prior in log-space
      var score = ln(docCount.toDouble() / totalDocs)
      val wordMap = classWordCounts[intent] ?: emptyMap()
      val totalWords = classTotalWords[intent] ?: 0

      // Likelihood with Laplace smoothing (+1 / +vocabSize)
      for (w in words) {
        val count = wordMap[w] ?: 0
        val prob = (count + 1.0) / (totalWords + vocabSize)
        score += ln(prob)
      }
      logScores.add(intent to score)
    }

    if (logScores.isEmpty()) {
      return IntentScore("unknown", 0.1)
    }

    logScores.sortByDescending { it.second }
    val maxLog = logScores.first().second

    // Softmax probabilities
    val expScores = logScores.map { it.first to exp(it.second - maxLog) }
    val sumExp = expScores.sumOf { it.second }.coerceAtLeast(1e-9)

    val top = expScores.first()
    val prob = top.second / sumExp

    return IntentScore(top.first, (prob * 100).toInt() / 100.0)
  }
}
