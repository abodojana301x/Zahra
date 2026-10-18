package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.zahra.engines.ZahraRouter
import com.example.zahra.knowledge.engine.KnowledgeEngine
import com.example.zahra.knowledge.index.OfflineKnowledgeIndex
import com.example.zahra.knowledge.model.*
import com.example.zahra.knowledge.normalizer.ArabicKnowledgeNormalizer
import com.example.zahra.knowledge.storage.InMemoryKnowledgeDriver
import com.example.zahra.knowledge.storage.LocalKnowledgeStore
import com.example.zahra.knowledge.synonyms.OfflineSynonymDictionary
import com.example.zahra.model.FactCategory
import com.example.zahra.model.MemorySource
import com.example.zahra.model.OutcomeState
import com.example.zahra.model.ZahraToolPayload
import com.example.zahra.security.ZahraCrypto
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Phase3KnowledgeEngineTest {

  private lateinit var context: Context
  private lateinit var inMemoryDriver: InMemoryKnowledgeDriver
  private lateinit var store: LocalKnowledgeStore
  private lateinit var engine: KnowledgeEngine
  private lateinit var router: ZahraRouter

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    ZahraCrypto.setTestKeyForTestingOnly(ZahraCrypto.generateTestKey())
    inMemoryDriver = InMemoryKnowledgeDriver()
    store = LocalKnowledgeStore(inMemoryDriver)
    engine = KnowledgeEngine(context, inMemoryDriver)
    router = ZahraRouter(context)
  }

  @After
  fun tearDown() {
    ZahraCrypto.setTestKeyForTestingOnly(null)
  }

  // =========================================================================
  // 1. KNOWLEDGE-RECORD VALIDATION & SCHEMA
  // =========================================================================

  @Test
  fun testRecordValidation_validRecordSucceeds() {
    val rec = KnowledgeRecord(
      id = "sci_test_01",
      category = KnowledgeCategory.SCIENCE,
      title = "سرعة الصوت في الهواء",
      canonicalContent = "تبلغ سرعة الصوت في الهواء الجاف عند درجة حرارة 20 مئوية حوالي 343 متراً في الثانية.",
      originalSourceText = "تبلغ سرعة الصوت في الهواء الجاف عند درجة حرارة 20 مئوية حوالي 343 متراً في الثانية.",
      language = "ar",
      normalizedText = "سرعة الصوت في الهواء",
      provenance = KnowledgeProvenance(
        origin = "الجمعية الصوتية الأمريكية (ASA)",
        sourceTitle = "دليل علم الصوتيات القياسي",
        sourceId = "ASA_SOUND_SPEED_20C",
        sourceLocation = "الجدول 3.1"
      ),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
    )

    val res = KnowledgeRecordValidator.validate(rec)
    assertTrue("Valid record must pass validation", res is KnowledgeValidationResult.Valid)
  }

  @Test
  fun testRecordValidation_rejectsBlankIdAndMalformedId() {
    val emptyIdRec = KnowledgeRecord(
      id = "",
      category = KnowledgeCategory.SCIENCE,
      title = "عنوان",
      canonicalContent = "محتوى",
      originalSourceText = "نص أصلي",
      normalizedText = "عنوان",
      provenance = KnowledgeProvenance("أصل", "عنوان مصدر", "id", "موضع"),
      verificationStatus = VerificationStatus.UNVERIFIED_CLAIM
    )
    val resEmpty = KnowledgeRecordValidator.validate(emptyIdRec)
    assertTrue("Empty ID must be rejected", resEmpty is KnowledgeValidationResult.Invalid)

    val malformedIdRec = emptyIdRec.copy(id = "bad id with spaces!")
    val resMalformed = KnowledgeRecordValidator.validate(malformedIdRec)
    assertTrue("Malformed ID with spaces must be rejected", resMalformed is KnowledgeValidationResult.Invalid)
  }

  @Test
  fun testRecordValidation_rejectsMissingProvenance() {
    val missingProvRec = KnowledgeRecord(
      id = "claim_without_source",
      category = KnowledgeCategory.GENERAL,
      title = "ادعاء عشوائي",
      canonicalContent = "محتوى غير مدعوم",
      originalSourceText = "نص أصلي",
      normalizedText = "ادعاء",
      provenance = KnowledgeProvenance(origin = "", sourceTitle = "", sourceId = "", sourceLocation = ""),
      verificationStatus = VerificationStatus.UNVERIFIED_CLAIM
    )

    val res = KnowledgeRecordValidator.validate(missingProvRec)
    assertTrue("Missing provenance must be rejected", res is KnowledgeValidationResult.Invalid)
    val errors = (res as KnowledgeValidationResult.Invalid).errors
    assertTrue(errors.any { it.contains("origin") || it.contains("جهة") })
    assertTrue(errors.any { it.contains("sourceTitle") || it.contains("المصدر") })
  }

  @Test
  fun testRecordValidation_structuralValidityDoesNotConferVerifiedStatus() {
    val unverifiedClaim = KnowledgeRecord(
      id = "claim_valid_schema",
      category = KnowledgeCategory.GENERAL,
      title = "ادعاء بصري",
      canonicalContent = "ادعاء مستوفٍ للأركان الهيكلية لكنه غير موثق علمياً.",
      originalSourceText = "ادعاء مستوفٍ للأركان الهيكلية لكنه غير موثق علمياً.",
      normalizedText = "ادعاء",
      provenance = KnowledgeProvenance(
        origin = "موقع إنترنت غير محقق",
        sourceTitle = "مدونة شخصية",
        sourceId = "BLOG_01",
        sourceLocation = "مقال رأي"
      ),
      verificationStatus = VerificationStatus.UNVERIFIED_CLAIM
    )

    val res = KnowledgeRecordValidator.validate(unverifiedClaim)
    assertTrue("Record is structurally valid", res is KnowledgeValidationResult.Valid)
    val validRec = (res as KnowledgeValidationResult.Valid).record
    assertFalse("Passing schema validation must NEVER automatically make a record verified", validRec.verificationStatus.isVerified)
  }

  // =========================================================================
  // 2. LOCAL STORAGE CRUD, PROVENANCE & ISOLATION
  // =========================================================================

  @Test
  fun testStoreCRUD_insertAndRetrieve() {
    val rec = KnowledgeRecord(
      id = "custom_rec_01",
      category = KnowledgeCategory.TECH,
      title = "بروتوكول HTTPS",
      canonicalContent = "HTTPS هو امتداد آمن لبروتوكول HTTP يستخدم تشفير TLS لحماية سرية البيانات وسلامتها.",
      originalSourceText = "HTTPS هو امتداد آمن لبروتوكول HTTP يستخدم تشفير TLS لحماية سرية البيانات وسلامتها.",
      normalizedText = "بروتوكول https تشفير tls",
      provenance = KnowledgeProvenance(
        origin = "IETF",
        sourceTitle = "RFC 2818",
        sourceId = "RFC_2818",
        sourceLocation = "القسم 2"
      ),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
    )

    val insertRes = store.insertRecord(rec)
    assertTrue(insertRes is KnowledgeValidationResult.Valid)

    val fetched = store.getRecordById("custom_rec_01")
    assertNotNull(fetched)
    assertEquals("بروتوكول HTTPS", fetched!!.title)
    assertEquals("IETF", fetched.provenance.origin)
    assertEquals("RFC 2818", fetched.provenance.sourceTitle)
  }

  @Test
  fun testStoreCRUD_preventsAccidentalOverwrite() {
    val rec1 = KnowledgeRecord(
      id = "dup_id",
      category = KnowledgeCategory.TECH,
      title = "السجل الأصلي",
      canonicalContent = "محتوى أصلي",
      originalSourceText = "محتوى أصلي",
      normalizedText = "السجل الأصلي",
      provenance = KnowledgeProvenance("IETF", "RFC 1", "RFC_1", "ص 1"),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
    )
    val rec2 = rec1.copy(title = "سجل متسلل جديد")

    val res1 = store.insertRecord(rec1)
    assertTrue(res1 is KnowledgeValidationResult.Valid)

    val res2 = store.insertRecord(rec2)
    assertTrue("Duplicate ID insertion must be rejected", res2 is KnowledgeValidationResult.Invalid)

    // Verify original content was not overwritten
    val current = store.getRecordById("dup_id")
    assertEquals("السجل الأصلي", current!!.title)
  }

  @Test
  fun testStoreCRUD_updatePreservesOriginalCreationTimeAndProvenance() {
    val initialTime = System.currentTimeMillis() - 10000L
    val rec = KnowledgeRecord(
      id = "upd_rec_01",
      category = KnowledgeCategory.TECH,
      title = "عنوان قبل التحديث",
      canonicalContent = "محتوى قديم",
      originalSourceText = "محتوى قديم",
      normalizedText = "محتوى",
      provenance = KnowledgeProvenance("الجهة المؤسسة", "المصدر الأولي", "SRC_01", "القسم 1"),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL,
      createdAt = initialTime
    )
    store.insertRecord(rec)

    val updatedRec = rec.copy(
      title = "عنوان بعد التحديث",
      canonicalContent = "محتوى محدث ومدقق"
    )
    val updateRes = store.updateRecord(updatedRec)
    assertTrue(updateRes is KnowledgeValidationResult.Valid)

    val retrieved = store.getRecordById("upd_rec_01")!!
    assertEquals("عنوان بعد التحديث", retrieved.title)
    assertEquals("الجهة المؤسسة", retrieved.provenance.origin)
    assertEquals("Creation time must be preserved across updates", initialTime, retrieved.createdAt)
    assertTrue("Update time must be greater than creation time", retrieved.updatedAt >= initialTime)
  }

  @Test
  fun testStoreCRUD_deleteSafelyRemovesFromStoreAndIndex() {
    val rec = KnowledgeRecord(
      id = "del_rec_01",
      category = KnowledgeCategory.HISTORY,
      title = "معركة اليرموك",
      canonicalContent = "معركة فاصلة بين المسلمين والروم وقعت عام 15 هـ / 636 م.",
      originalSourceText = "معركة فاصلة بين المسلمين والروم وقعت عام 15 هـ / 636 م.",
      normalizedText = "معركة اليرموك",
      provenance = KnowledgeProvenance("تاريخ الطبري", "تاريخ الرسل والملوك", "TABARI_YARMOUK", "الجزء 3"),
      verificationStatus = VerificationStatus.VERIFIED_SCHOLARLY
    )
    store.insertRecord(rec)
    assertNotNull(store.getRecordById("del_rec_01"))

    // Search matches before deletion
    val searchBefore = store.index.search("اليرموك")
    assertTrue(searchBefore.any { it.record.id == "del_rec_01" })

    val deleted = store.deleteRecord("del_rec_01")
    assertTrue(deleted)
    assertNull(store.getRecordById("del_rec_01"))

    // Search no longer matches after deletion
    val searchAfter = store.index.search("اليرموك")
    assertFalse(searchAfter.any { it.record.id == "del_rec_01" })
  }

  @Test
  fun testStoreFilter_byCategoryAndVerificationStatus() {
    val recA = KnowledgeRecord(
      id = "filter_a",
      category = KnowledgeCategory.SCIENCE,
      title = "قانون الجاذبية",
      canonicalContent = "محتوى",
      originalSourceText = "نص",
      normalizedText = "جاذبية",
      provenance = KnowledgeProvenance("نيوتن", "المبادئ الرياضية", "PRINCIPIA", "كتاب 1"),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
    )
    val recB = KnowledgeRecord(
      id = "filter_b",
      category = KnowledgeCategory.HISTORY,
      title = "رأي تاريخي غير مثبت",
      canonicalContent = "محتوى",
      originalSourceText = "نص",
      normalizedText = "تاريخ",
      provenance = KnowledgeProvenance("مؤرخ", "كتاب", "BK_1", "ص 1"),
      verificationStatus = VerificationStatus.UNVERIFIED_CLAIM
    )
    store.insertRecord(recA)
    store.insertRecord(recB)

    val scienceRecords = store.listRecords(category = KnowledgeCategory.SCIENCE)
    assertEquals(1, scienceRecords.size)
    assertEquals("filter_a", scienceRecords[0].id)

    val verifiedRecords = store.listRecords(status = VerificationStatus.VERIFIED_EMPIRICAL)
    assertEquals(1, verifiedRecords.size)
    assertEquals("filter_a", verifiedRecords[0].id)
  }

  @Test
  fun testIndexRebuild_reconstructsInvertedIndexFromCorpus() {
    val rec = KnowledgeRecord(
      id = "rebuild_01",
      category = KnowledgeCategory.TECH,
      title = "الذكاء الاصطناعي الرمزي",
      canonicalContent = "الذكاء الرمزي هو نهج يعتمد على القواعد الحتمية والمنطق الرياضي الصارم.",
      originalSourceText = "الذكاء الرمزي هو نهج يعتمد على القواعد الحتمية والمنطق الرياضي الصارم.",
      normalizedText = "الذكاء الاصطناعي الرمزي",
      provenance = KnowledgeProvenance("ACM", "Symbolic AI Survey", "ACM_SYM_AI", "ص 12"),
      verificationStatus = VerificationStatus.VERIFIED_ACADEMIC_PEER
    )
    store.insertRecord(rec)

    val indexedCount = store.rebuildIndex()
    assertEquals(1, indexedCount)

    val hits = store.index.search("الرمزي")
    assertEquals(1, hits.size)
    assertEquals("rebuild_01", hits[0].record.id)
  }

  // =========================================================================
  // 3. ARABIC LANGUAGE HANDLING & NORMALIZATION
  // =========================================================================

  @Test
  fun testArabicNormalizer_tashkeelAndTatweelStripping() {
    val withTashkeel = "سُرْعَةُ الضَّوْءِ فِـي الْفَـرَاغِ"
    val normalized = ArabicKnowledgeNormalizer.normalizeForSearch(withTashkeel)
    assertEquals("سرعه الضوء في الفراغ", normalized)
  }

  @Test
  fun testArabicNormalizer_unifiesAlefAndYaaAndTaaMarbuta() {
    val variants = "أحمد إبراهيم آية ٱستغفار علي هدى شجرة"
    val normalized = ArabicKnowledgeNormalizer.normalizeForSearch(variants)
    assertEquals("احمد ابراهيم ايه استغفار علي هدي شجره", normalized)
  }

  @Test
  fun testArabicNormalizer_unifiesArabicIndicDigits() {
    val indicStr = "تبلغ السرعة ٢٩٩٧٩٢٤٥٨ متراً ودرجة الحرارة ١٠٠"
    val normalized = ArabicKnowledgeNormalizer.normalizeForSearch(indicStr)
    assertTrue("Indic digits must be unified to ASCII 299792458", normalized.contains("299792458"))
    assertTrue("Indic digits 100 must be unified", normalized.contains("100"))
  }

  @Test
  fun testArabicNormalizer_preservesOriginalSourceText() {
    val originalTextWithTashkeel = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ"
    val rec = KnowledgeRecord(
      id = "hadith_preserve_test",
      category = KnowledgeCategory.HADITH,
      title = "حديث النية",
      canonicalContent = originalTextWithTashkeel,
      originalSourceText = originalTextWithTashkeel,
      normalizedText = ArabicKnowledgeNormalizer.normalizeForSearch(originalTextWithTashkeel),
      provenance = KnowledgeProvenance("صحيح البخاري", "صحيح البخاري", "H1", "حديث 1"),
      verificationStatus = VerificationStatus.VERIFIED_CANONICAL
    )
    store.insertRecord(rec)

    val fetched = store.getRecordById("hadith_preserve_test")!!
    assertEquals(
      "Original text must remain pristine with full diacritics",
      originalTextWithTashkeel,
      fetched.originalSourceText
    )
  }

  @Test
  fun testArabicNormalizer_boundedPrefixStemmingPreservesRoots() {
    // "الله" must never be stripped to "له"
    val stemsAllah = ArabicKnowledgeNormalizer.getBoundedStems("الله")
    assertFalse("Root 'الله' must never be stripped to 'له'", stemsAllah.contains("له"))

    // "وقت" must never be stripped to "قت"
    val stemsWaqt = ArabicKnowledgeNormalizer.getBoundedStems("وقت")
    assertFalse("Root 'وقت' must never be stripped to 'قت'", stemsWaqt.contains("قت"))

    // "الكتاب" should yield "كتاب"
    val stemsKitab = ArabicKnowledgeNormalizer.getBoundedStems("الكتاب")
    assertTrue(stemsKitab.contains("كتاب"))
  }

  // =========================================================================
  // 4. OFFLINE INDEXING, EXACT MATCHES, PHRASES, AND RANKING
  // =========================================================================

  @Test
  fun testExactPhraseSearch_outranksPartialTokenOverlap() {
    val recExact = KnowledgeRecord(
      id = "rec_exact",
      category = KnowledgeCategory.SCIENCE,
      title = "سرعة الضوء في الفراغ",
      canonicalContent = "سرعة الضوء في الفراغ تبلغ حوالي 300 ألف كم/ث.",
      originalSourceText = "سرعة الضوء في الفراغ تبلغ حوالي 300 ألف كم/ث.",
      normalizedText = "سرعة الضوء في الفراغ",
      provenance = KnowledgeProvenance("BIPM", "SI", "SI_C", "ثوابت"),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
    )
    val recPartial = KnowledgeRecord(
      id = "rec_partial",
      category = KnowledgeCategory.SCIENCE,
      title = "انكسار الضوء عبر الأوساط",
      canonicalContent = "تختلف سرعة انتشار الأشعة في الفراغ عنها في الماء والزجاج.",
      originalSourceText = "تختلف سرعة انتشار الأشعة في الفراغ عنها في الماء والزجاج.",
      normalizedText = "انكسار الضوء سرعة انتشار الفراغ",
      provenance = KnowledgeProvenance("فيزياء", "بصريات", "OPT_1", "ص 10"),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
    )
    store.insertRecord(recPartial)
    store.insertRecord(recExact)

    val results = store.index.search("سرعة الضوء في الفراغ")
    assertTrue(results.isNotEmpty())
    assertEquals("Exact phrase match must rank first", "rec_exact", results[0].record.id)
    assertEquals(MatchMethod.EXACT_PHRASE, results[0].matchMethod)
  }

  @Test
  fun testMultiWordQuery_coverageAndRanking() {
    val hits = engine.query("كم درجة غليان الماء النقي تحت الضغط الجوي")
    assertEquals(KnowledgeRetrievalState.VERIFIED_MATCH, hits.state)
    assertNotNull(hits.bestMatch)
    assertEquals("sci_02", hits.bestMatch!!.record.id)
    assertTrue(hits.bestMatch!!.record.canonicalContent.contains("100"))
  }

  // =========================================================================
  // 5. BOUNDED SYNONYMS & ALTERNATIVE PHRASINGS
  // =========================================================================

  @Test
  fun testBoundedSynonymExpansion_matchesSynonymsWithoutLoops() {
    // "فيزياء الضوء" is registered as a synonym for "سرعة الضوء"
    val res = engine.query("فيزياء الضوء")
    assertEquals(KnowledgeRetrievalState.VERIFIED_MATCH, res.state)
    assertNotNull(res.bestMatch)
    assertEquals("sci_01", res.bestMatch!!.record.id)
  }

  @Test
  fun testSynonymExpansion_discountedWeight_directMatchAlwaysWins() {
    val recDirect = KnowledgeRecord(
      id = "direct_match_win",
      category = KnowledgeCategory.GENERAL,
      title = "النوم الصحي للبالغين",
      canonicalContent = "النوم الصحي هو راحة ضرورية للدماغ.",
      originalSourceText = "النوم الصحي هو راحة ضرورية للدماغ.",
      normalizedText = "النوم الصحي",
      provenance = KnowledgeProvenance("AASM", "دليل النوم", "AASM_1", "ص 1"),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
    )
    val recSynonym = KnowledgeRecord(
      id = "synonym_target",
      category = KnowledgeCategory.GENERAL,
      title = "ساعات النوم",
      canonicalContent = "توصيات مدة النوم اليومي.",
      originalSourceText = "توصيات مدة النوم اليومي.",
      normalizedText = "ساعات النوم مدة النوم",
      provenance = KnowledgeProvenance("CDC", "دليل النوم", "CDC_1", "ص 2"),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
    )
    store.insertRecord(recSynonym)
    store.insertRecord(recDirect)

    // Direct match for "النوم الصحي" vs synonym expansion for "ساعات النوم"
    val hits = store.index.search("النوم الصحي")
    assertTrue(hits.isNotEmpty())
    assertEquals("Direct match must outrank expanded synonym match", "direct_match_win", hits[0].record.id)
  }

  @Test
  fun testDeterministicTieBreaking_stableOrderingOnEqualScores() {
    val recA = KnowledgeRecord(
      id = "rec_alpha_01",
      category = KnowledgeCategory.GENERAL,
      title = "مصطلح مشترك",
      canonicalContent = "بيان مشترك",
      originalSourceText = "بيان مشترك",
      normalizedText = "مصطلح مشترك",
      provenance = KnowledgeProvenance("جهة", "مصدر", "S1", "ص 1"),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
    )
    val recB = KnowledgeRecord(
      id = "rec_beta_02",
      category = KnowledgeCategory.GENERAL,
      title = "مصطلح مشترك",
      canonicalContent = "بيان مشترك",
      originalSourceText = "بيان مشترك",
      normalizedText = "مصطلح مشترك",
      provenance = KnowledgeProvenance("جهة", "مصدر", "S2", "ص 2"),
      verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
    )
    store.insertRecord(recB)
    store.insertRecord(recA)

    val hits1 = store.index.search("مصطلح مشترك")
    val hits2 = store.index.search("مصطلح مشترك")

    assertEquals(2, hits1.size)
    assertEquals(2, hits2.size)
    assertEquals(
      "Tie-breaking must produce identical deterministic ordering on repeated runs",
      hits1[0].record.id,
      hits2[0].record.id
    )
    assertEquals("rec_alpha_01", hits1[0].record.id)
  }

  // =========================================================================
  // 6. ADVERSARIAL AND NEGATIVE SEARCH TESTS
  // =========================================================================

  @Test
  fun testAdversarialQuery_emptyOrWhitespaceYieldsInsufficientEvidence() {
    val resEmpty = engine.query("")
    assertEquals(KnowledgeRetrievalState.INSUFFICIENT_EVIDENCE, resEmpty.state)
    assertNull(resEmpty.bestMatch)

    val resSpaces = engine.query("     ")
    assertEquals(KnowledgeRetrievalState.INSUFFICIENT_EVIDENCE, resSpaces.state)
    assertNull(resSpaces.bestMatch)
  }

  @Test
  fun testAdversarialQuery_veryLongInputClampedSafely() {
    val massiveString = "سرعة الضوء " + "فيزياء ".repeat(1000)
    val res = engine.query(massiveString)
    // Must complete safely without OutOfMemory or StackOverflow
    assertNotNull(res)
    assertTrue(res.executionTimeMs >= 0)
  }

  @Test
  fun testAdversarialQuery_sqlOrRegexInjectionHandledSafely() {
    val injection1 = "'; DROP TABLE knowledge; --"
    val res1 = engine.query(injection1)
    assertNotNull(res1)
    assertEquals(KnowledgeRetrievalState.NO_MATCH, res1.state)

    val injection2 = "(.*+)+[a-z]{0,999999999}"
    val res2 = engine.query(injection2)
    assertNotNull(res2)
  }

  @Test
  fun testEpistemicHonesty_absentTopicReturnsNoMatchOrInsufficientEvidence() {
    val res = engine.query("ما هي عاصمة كوكب المشتري في عام 3000؟")
    assertEquals(KnowledgeRetrievalState.NO_MATCH, res.state)
    assertNull(res.bestMatch)
    assertTrue("Must state truth before intelligence abstention", res.abstentionReason!!.contains("غير مسجل") || res.abstentionReason!!.contains("Truth Before Intelligence"))
  }

  // =========================================================================
  // 7. CONFLICT DETECTION & UNVERIFIED RECORD WARNINGS
  // =========================================================================

  @Test
  fun testConflictDetection_surfacesConflictingRecordsTransparently() {
    val recDisputedA = KnowledgeRecord(
      id = "fiqh_conflict_a",
      category = KnowledgeCategory.FIQH,
      title = "حكم قراءة الفاتحة للمأموم في الصلاة الجهرية",
      canonicalContent = "يرى الشافعية وجوب قراءة الفاتحة على المأموم في كل ركعة سرية كانت أو جهرية.",
      originalSourceText = "يرى الشافعية وجوب قراءة الفاتحة على المأموم في كل ركعة سرية كانت أو جهرية.",
      normalizedText = "حكم قراءة الفاتحة للماموم في الصلاة الجهرية وجوب",
      provenance = KnowledgeProvenance("المذهب الشافعي", "المجموع شرح المهذب", "MAJMU_FATIHA", "الجزء 3"),
      verificationStatus = VerificationStatus.DISPUTED
    )
    val recDisputedB = KnowledgeRecord(
      id = "fiqh_conflict_b",
      category = KnowledgeCategory.FIQH,
      title = "حكم قراءة الفاتحة للمأموم في الصلاة الجهرية",
      canonicalContent = "يرى الحنفية والمالكية والحنابلة سقوط القراءة عن المأموم في الجهرية لقوله تعالى وإذا قرئ القرآن فاستمعوا له.",
      originalSourceText = "يرى الحنفية والمالكية والحنابلة سقوط القراءة عن المأموم في الجهرية لقوله تعالى وإذا قرئ القرآن فاستمعوا له.",
      normalizedText = "حكم قراءة الفاتحة للماموم في الصلاة الجهرية سقوط القراءة",
      provenance = KnowledgeProvenance("جمهور الفقهاء", "المغني لابن قدامة", "MUGHNI_FATIHA", "الجزء 1"),
      verificationStatus = VerificationStatus.DISPUTED
    )
    store.insertRecord(recDisputedA)
    store.insertRecord(recDisputedB)

    val queryEngine = KnowledgeEngine(context, inMemoryDriver)
    val res = queryEngine.query("حكم قراءة الفاتحة للمأموم في الصلاة الجهرية")
    assertEquals(KnowledgeRetrievalState.CONFLICTING_EVIDENCE, res.state)
    assertTrue("Conflicts list must be surfaced", res.conflicts.isNotEmpty())
  }

  @Test
  fun testUnverifiedRecord_returnsTraceableCandidateWithWarning() {
    val unverifiedRec = KnowledgeRecord(
      id = "unverified_claim_test",
      category = KnowledgeCategory.HISTORY,
      title = "أسطورة مدينة إرم ذات العماد تحت الرمال",
      canonicalContent = "ادعاء باكتشاف مدينة أثرية مفقودة في صحراء الربع الخالي دون توثيق أثري معتمد.",
      originalSourceText = "ادعاء باكتشاف مدينة أثرية مفقودة في صحراء الربع الخالي دون توثيق أثري معتمد.",
      normalizedText = "اسطورة مدينة ارم ذات العماد الربع الخالي",
      provenance = KnowledgeProvenance("صحيفة غير محققة", "مقال صحفي", "NEWS_01", "ص 5"),
      verificationStatus = VerificationStatus.UNVERIFIED_CLAIM
    )
    store.insertRecord(unverifiedRec)

    val queryEngine = KnowledgeEngine(context, inMemoryDriver)
    val res = queryEngine.query("مدينة إرم ذات العماد تحت الرمال")
    assertEquals(KnowledgeRetrievalState.TRACEABLE_CANDIDATE, res.state)
    assertNotNull(res.bestMatch)
    assertNotNull("Must contain verification warning", res.bestMatch!!.warning)
  }

  // =========================================================================
  // 8. ROUTER INTEGRATION & MEMORY SEPARATION
  // =========================================================================

  @Test
  fun testRouterIntegration_knowledgeQueryReturnsSupportedWithPayload() {
    val res = router.process("ما هي سرعة الضوء في الفراغ؟")
    assertEquals(OutcomeState.SUPPORTED, res.outcomeState)
    assertTrue(res.text.contains("299,792,458"))
    assertTrue(res.source!!.contains("المكتب الدولي للأوزان والمقاييس") || res.source!!.contains("BIPM") || res.source!!.contains("SI"))

    assertTrue("Tool payload must be KnowledgePayload", res.toolPayload is ZahraToolPayload.KnowledgePayload)
    val kPayload = res.toolPayload as ZahraToolPayload.KnowledgePayload
    assertEquals("sci_01", kPayload.recordId)
    assertTrue(kPayload.isVerified)
  }

  @Test
  fun testRouterIntegration_honestAbstentionOnMissingKnowledge() {
    val res = router.process("ابحث في المعرفة عن تاريخ بناء الأهرامات على كوكب المريخ")
    assertEquals(OutcomeState.UNSUPPORTED, res.outcomeState)
    assertTrue(res.text.contains("غير مسجل") || res.text.contains("Truth Before Intelligence") || res.text.contains("يمتنع") || res.text.contains("نمتنع"))
  }

  @Test
  fun testSeparationOfMemoryAndKnowledge_searchNeverModifiesMemory() {
    val initialFactsCount = router.memoryEngine.getFacts().size

    // Execute multiple search queries across knowledge corpus
    router.process("ما هي سرعة الضوء في الفراغ؟")
    router.process("درجة غليان الماء")
    router.process("من هو الخوارزمي؟")

    val finalFactsCount = router.memoryEngine.getFacts().size
    assertEquals("Knowledge queries must NEVER create or alter personal user facts", initialFactsCount, finalFactsCount)
  }

  @Test
  fun testSeparationOfMemoryAndKnowledge_clearingKnowledgeDoesNotClearPersonalMemory() {
    // 1. Store a personal memory fact
    router.memoryEngine.saveFactWithResult(
      category = FactCategory.PERSONAL,
      key = "لوني المفضل",
      value = "الأخضر الزمردي",
      source = MemorySource.USER_EXPLICIT
    )
    val userFactsBefore = router.memoryEngine.getFacts().size
    assertTrue(userFactsBefore >= 1)

    // 2. Clear knowledge store
    store.clearAll()
    assertEquals(0, store.totalCount())

    // 3. User personal memory must remain intact
    val userFactsAfter = router.memoryEngine.getFacts().size
    assertEquals("Personal memory must remain completely isolated from knowledge store clearing", userFactsBefore, userFactsAfter)
  }
}
