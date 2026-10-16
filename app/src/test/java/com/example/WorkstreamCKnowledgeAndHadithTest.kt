package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.zahra.engines.ReligionEngine
import com.example.zahra.model.OutcomeState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkstreamCKnowledgeAndHadithTest {

  private lateinit var context: Context
  private lateinit var religionEngine: ReligionEngine

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    religionEngine = ReligionEngine(context)
  }

  // --- C1 & C2: Generic Keyword Traps and First-Record Bias Prevention ---

  @Test
  fun testNoGenericKeywordTrap_hadithAboutKnowledgeReturnsHadith4_notHadith1() {
    // The previous implementation had a defect where any query containing "حديث"
    // returned hadith_1 (النية) because it blindly returned the first loop match.
    val resKnowledge = religionEngine.query("حديث عن طلب العلم وسلوك طريقه")
    assertEquals(OutcomeState.SUPPORTED, resKnowledge.outcomeState)
    assertEquals("hadith_4", resKnowledge.matchedRecordId)
    val normK = com.example.zahra.core.ZahraNormalizer.normalize(resKnowledge.text)
    assertTrue("Must contain hadith 4 text", normK.contains("سلك طريقا") || normK.contains("علما"))
    assertFalse("Must not return hadith 1 by default", normK.contains("الاعمال بالنيات"))
  }

  @Test
  fun testNoGenericKeywordTrap_hadithAboutGoodMannersReturnsHadith2() {
    val res = religionEngine.query("حديث عن حسن الخلق ومحبة الخير للناس")
    assertEquals(OutcomeState.SUPPORTED, res.outcomeState)
    assertEquals("hadith_2", res.matchedRecordId)
    val norm = com.example.zahra.core.ZahraNormalizer.normalize(res.text)
    assertTrue(norm.contains("يحب لاخيه"))
  }

  @Test
  fun testNoGenericKeywordTrap_hadithAboutEaseReturnsHadith3() {
    val res = religionEngine.query("أريد حديث عن التيسير والرفق")
    assertEquals(OutcomeState.SUPPORTED, res.outcomeState)
    assertEquals("hadith_3", res.matchedRecordId)
    val norm = com.example.zahra.core.ZahraNormalizer.normalize(res.text)
    assertTrue(norm.contains("يسروا ولا تعسروا"))
  }

  @Test
  fun testNoGenericKeywordTrap_hadithAboutIntentionReturnsHadith1() {
    val res = religionEngine.query("حديث نبوي عن النية وإخلاص العمل")
    assertEquals(OutcomeState.SUPPORTED, res.outcomeState)
    assertEquals("hadith_1", res.matchedRecordId)
    val norm = com.example.zahra.core.ZahraNormalizer.normalize(res.text)
    assertTrue(norm.contains("الاعمال بالنيات"))
  }

  @Test
  fun testNoGenericKeywordTrap_hadithAboutTongueReturnsHadith5() {
    val res = religionEngine.query("حديث عن كف الأذى وحفظ اللسان")
    assertEquals(OutcomeState.SUPPORTED, res.outcomeState)
    assertEquals("hadith_5", res.matchedRecordId)
    val norm = com.example.zahra.core.ZahraNormalizer.normalize(res.text)
    assertTrue(norm.contains("سلم المسلمون من لسانه"))
  }

  @Test
  fun testFirstRecordBiasPrevention_allDistinctTopicsReturnTheirOwnRecord() {
    val queriesAndExpectedIds = listOf(
      "حديث عن النية" to "hadith_1",
      "حديث عن محبة الخير للغير" to "hadith_2",
      "حديث عن التيسير" to "hadith_3",
      "حديث عن فضل العلم" to "hadith_4",
      "حديث عن حفظ اللسان واليد" to "hadith_5"
    )

    val matchedIds = mutableSetOf<String>()
    for ((query, expectedId) in queriesAndExpectedIds) {
      val res = religionEngine.query(query)
      assertEquals("Query '$query' should match $expectedId", expectedId, res.matchedRecordId)
      matchedIds.add(res.matchedRecordId ?: "")
    }

    // Verify all 5 distinct records were retrieved, proving zero first-record bias
    assertEquals(5, matchedIds.size)
  }

  // --- C4: Hadith Authenticity Verification Integrity ---

  @Test
  fun testAuthenticityQuery_knownHadithReturnsExactVerifiedGradeAndBook() {
    val res = religionEngine.query("هل حديث إنما الأعمال بالنيات صحيح وما تخريجه؟")
    assertEquals(OutcomeState.SUPPORTED, res.outcomeState)
    assertTrue("Must mention sahih", res.text.contains("صحيح متفق عليه"))
    assertTrue("Must mention source book", res.text.contains("صحيح البخاري"))
    assertTrue("Must include narrator", res.text.contains("عمر بن الخطاب"))
  }

  @Test
  fun testAuthenticityQuery_unrecordedHadithRefusesToGuessOrFabricate() {
    // Unrecorded text in our dataset:
    val res = religionEngine.query("هل حديث اطلبوا العلم ولو في الصين صحيح وما حكمه؟")
    assertEquals(OutcomeState.UNSUPPORTED, res.outcomeState)
    assertTrue("Must honestly refuse to fabricate judgment", res.text.contains("غير مسجل") || res.text.contains("يمتنع"))
    assertFalse("Must not claim Bukhari", res.text.contains("صحيح البخاري"))
  }

  // --- C5: Quran and Fiqh Retrieval ---

  @Test
  fun testQuranRetrieval_ayatAlKursi() {
    val res = religionEngine.query("ما هو تفسير آية الكرسي في سورة البقرة؟")
    assertEquals(OutcomeState.SUPPORTED, res.outcomeState)
    val norm = com.example.zahra.core.ZahraNormalizer.normalize(res.text)
    assertTrue(norm.contains("الله لا اله الا هو الحي القيوم"))
    assertTrue(res.source.contains("سورة البقرة"))
  }

  @Test
  fun testFiqhRetrieval_wuduPillars() {
    val res = religionEngine.query("ما هي أركان الوضوء في الفقه الإسلامي؟")
    assertEquals(OutcomeState.SUPPORTED, res.outcomeState)
    val norm = com.example.zahra.core.ZahraNormalizer.normalize(res.text)
    assertTrue(norm.contains("غسل الوجه"))
    assertTrue(res.text.contains("المذاهب الأربعة") || res.text.contains("الفقهاء"))
  }

  @Test
  fun testUnrelatedReligiousQuery_refusesToFabricate() {
    val res = religionEngine.query("ما حكم الاستنساخ البشري في المريخ؟")
    assertEquals(OutcomeState.UNSUPPORTED, res.outcomeState)
    assertTrue(res.text.contains("غير مسجل") || res.text.contains("يمتنع"))
  }

  // --- Benchmark: 300 Curated Knowledge & Hadith Retrieval Cases ---

  @Test
  fun testBenchmark_300KnowledgeRetrievalCases() {
    val topics = listOf(
      "النية" to "hadith_1",
      "حب الخير" to "hadith_2",
      "التيسير" to "hadith_3",
      "طلب العلم" to "hadith_4",
      "حفظ اللسان" to "hadith_5"
    )

    var passedCount = 0
    val totalCases = 300

    for (i in 0 until totalCases) {
      when (i % 5) {
        0, 1, 2 -> {
          // Topic query with varying phrasing
          val (keyword, expectedId) = topics[i % topics.size]
          val query = "أريد حديثاً نبوياً شريفاً عن $keyword برقم $i"
          val res = religionEngine.query(query)
          if (res.outcomeState == OutcomeState.SUPPORTED && res.matchedRecordId == expectedId) {
            passedCount++
          }
        }
        3 -> {
          // Authenticity check on unrecorded text -> must return UNSUPPORTED
          val fakeHadithQuery = "هل حديث رقم $i في موضوع غير موجود صحيح؟"
          val res = religionEngine.query(fakeHadithQuery)
          if (res.outcomeState == OutcomeState.UNSUPPORTED) {
            passedCount++
          }
        }
        4 -> {
          // Quran or Fiqh query
          val query = if (i % 2 == 0) "تفسير آية الكرسي رقم $i" else "ما هي نواقض الوضوء الأساسية في الفقه؟"
          val res = religionEngine.query(query)
          if (res.outcomeState == OutcomeState.SUPPORTED) {
            passedCount++
          }
        }
      }
    }

    assertEquals(totalCases, passedCount)
  }
}
