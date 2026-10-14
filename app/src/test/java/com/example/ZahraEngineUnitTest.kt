package com.example

import com.example.zahra.core.DetectedLang
import com.example.zahra.core.LanguageDetector
import com.example.zahra.core.ZahraNormalizer
import com.example.zahra.engines.MathEngine
import com.example.zahra.engines.SummarizeEngine
import com.example.zahra.entities.EntityExtractor
import org.junit.Assert.*
import org.junit.Test

class ZahraEngineUnitTest {

  @Test
  fun testNormalizer_stripsTashkeelAndFoldsAlef() {
    val raw = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ، أَهْلاً بِكُمْ"
    val normalized = ZahraNormalizer.normalize(raw)
    assertFalse(normalized.contains("ً"))
    assertFalse(normalized.contains("ْ"))
    assertFalse(normalized.contains("أ"))
    assertTrue(normalized.contains("اهلا"))
  }

  @Test
  fun testNormalizer_unifiesArabicIndicDigits() {
    val raw = "احسب ٢٥ + ٧٥"
    val normalized = ZahraNormalizer.normalize(raw)
    assertEquals("احسب 25 + 75", normalized)
  }

  @Test
  fun testMathEngine_evaluatesArithmetic() {
    val res1 = MathEngine.evaluate("25 * 4 + 10")
    assertTrue(res1.formattedText.contains("110"))

    val res2 = MathEngine.evaluate("جذر 144")
    assertTrue(res2.formattedText.contains("12"))
  }

  @Test
  fun testMathEngine_convertsUnits() {
    val conv = MathEngine.convertUnit(10.0, "km", "mi")
    assertTrue(conv.formattedText.contains("6.21"))
    assertEquals(1.0, conv.confidence, 0.01)
  }

  @Test
  fun testLanguageDetector_identifiesScripts() {
    val ar = LanguageDetector.detect("السلام عليكم ورحمة الله وبركاته")
    assertEquals(DetectedLang.AR, ar.language)

    val en = LanguageDetector.detect("Hello and welcome to Zahra assistant")
    assertEquals(DetectedLang.EN, en.language)
  }

  @Test
  fun testEntityExtractor_parsesRelativeDates() {
    val parsed = EntityExtractor.parseRelativeDate("سجل مهمة للغد")
    assertNotNull(parsed)
    assertEquals("غداً", parsed?.rawExpression)
  }

  @Test
  fun testSummarizeEngine_extractsSentencesWithoutHallucination() {
    val article = "الذكاء الاصطناعي الحتمي يعتمد على القواعد الدقيقة والمنطق الصارم. النماذج التوليدية قد تصاب بالهلوسة في بعض الأحيان. خوارزمية تكست رانك تعتمد على نظرية البيانات لحساب أهم الجمل الرياضية. زهرة تعمل بدون أي اتصال بالإنترنت نهائياً لحفظ الخصوصية التامة."
    val summary = SummarizeEngine.summarize(article, maxSentences = 2)
    assertNotNull(summary.summary)
    assertTrue(summary.keySentences.isNotEmpty())
    // Verify zero hallucination: all returned sentences are substrings of original article
    for (sentence in summary.keySentences) {
      assertTrue(article.contains(sentence))
    }
  }
}
