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

  @Test
  fun testMemoryEngine_extractsFactsAndManagesMemory() {
    val memoryEngine = com.example.zahra.engines.MemoryEngine()
    val initialCount = memoryEngine.getFacts().size
    assertEquals(0, initialCount)

    // Test automatic name extraction
    val learnedFact = memoryEngine.scanAndExtractFact("اسمي عبد الرحمن")
    assertNotNull(learnedFact)
    assertEquals("عبد الرحمن", learnedFact?.value)

    // Test automatic location extraction
    val locFact = memoryEngine.scanAndExtractFact("أعيش في المدينة المنورة")
    assertNotNull(locFact)
    assertEquals("المدينة المنورة", locFact?.value)

    // Test manual fact addition and deletion
    val manual = memoryEngine.addFact(
      com.example.zahra.model.FactCategory.PREFERENCE,
      "الخط المفضل",
      "الخط الكوفي المودرن"
    )
    assertTrue(memoryEngine.getFacts().any { it.id == manual.id })

    memoryEngine.deleteFact(manual.id)
    assertFalse(memoryEngine.getFacts().any { it.id == manual.id })
  }

  @Test
  fun testFrancoAndColloquial_detectionAndNormalization() {
    // 1. Test Franco-Arabic detection
    val francoLang = LanguageDetector.detect("ezayak ya habibi 3amel eh")
    assertEquals(DetectedLang.FRANCO, francoLang.language)

    // 2. Test Franco-Arabic normalization into Arabic keywords
    val normalizedFranco = ZahraNormalizer.normalize("shokran ya habibi 3amel eh")
    assertTrue(normalizedFranco.contains("شكرا"))
    assertTrue(normalizedFranco.contains("حبيبي"))

    // 3. Test Egyptian and Colloquial Arabic normalization
    val colloquial = ZahraNormalizer.normalize("عايز اعرف مواقيت الصلاة دلوقتي وبكره")
    assertTrue(colloquial.contains("الان"))
    assertTrue(colloquial.contains("غدا"))
  }

  @Test
  fun testLocalImageEngine_supportsAllThreeModes() {
    val engine = com.example.zahra.engines.LocalImageEngine()

    // Mode 1: Vector Canvas
    val vectorRes = engine.generateImage("ارسم فضاء كوني", forcedMode = "vector")
    assertEquals("vector", vectorRes.mode)
    assertEquals("galaxy", vectorRes.shapeStyle)
    assertTrue(vectorRes.title.isNotBlank())

    // Mode 2: Quote & Calligraphy Card
    val quoteRes = engine.generateImage("بطاقة اقتباس وحكمة", forcedMode = "quote_card")
    assertEquals("quote_card", quoteRes.mode)
    assertTrue(quoteRes.quoteArabic.isNotBlank())
    assertTrue(quoteRes.quoteAuthor.isNotBlank())

    // Mode 3: Generative Pixel Art
    val pixelRes = engine.generateImage("بكسل آرت 8bit", forcedMode = "pixel_art", forcedStyle = "cyber")
    assertEquals("pixel_art", pixelRes.mode)
    assertEquals(16, pixelRes.pixelGrid.size)
    assertEquals(16, pixelRes.pixelGrid[0].size)
  }

  @Test
  fun testImagePrompt_routesCorrectly() {
    val rawPrompt = "أنشئ لي صورة إجرائية تعبيرية جميلة"
    val normalized = ZahraNormalizer.normalize(rawPrompt)
    val match = com.example.zahra.intent.RegexMatcher.match(normalized)
    assertNotNull(match)
    assertEquals("generate_image", match?.intent)
  }
}

