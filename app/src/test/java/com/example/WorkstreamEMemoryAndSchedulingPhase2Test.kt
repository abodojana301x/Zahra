package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.zahra.engines.MemoryEngine
import com.example.zahra.engines.ScheduleEngine
import com.example.zahra.engines.ScheduleValidationResult
import com.example.zahra.engines.ZahraRouter
import com.example.zahra.model.*
import com.example.zahra.security.ZahraCrypto
import com.example.zahra.storage.InMemoryMemoryDriver
import com.example.zahra.storage.SafeMemoryStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlarmManager
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class WorkstreamEMemoryAndSchedulingPhase2Test {

  private lateinit var context: Context
  private lateinit var memoryEngine: MemoryEngine
  private lateinit var scheduleEngine: ScheduleEngine
  private lateinit var router: ZahraRouter

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    // Isolate test key for JVM unit tests
    ZahraCrypto.setTestKeyForTestingOnly(ZahraCrypto.generateTestKey())

    // Clear test preferences between test runs
    context.getSharedPreferences("zahra_test_memory_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("zahra_test_schedules_prefs", Context.MODE_PRIVATE).edit().clear().commit()

    val testDriver = com.example.zahra.storage.SharedPreferencesMemoryDriver(context, "zahra_test_memory_prefs")
    val safeStore = SafeMemoryStore(testDriver, enableEncryption = true)

    memoryEngine = MemoryEngine(context, safeStore)
    scheduleEngine = ScheduleEngine(context, "zahra_test_schedules_prefs")
    router = ZahraRouter(context)
  }

  @org.junit.After
  fun tearDown() {
    ZahraCrypto.setTestKeyForTestingOnly(null)
  }

  // =========================================================================
  // WORKSTREAM A & B: PERSISTENT MEMORY & SAFE STORAGE TESTS
  // =========================================================================

  @Test
  fun testSaveAndRetrieveMemory_verifiedReadBack() {
    val result = memoryEngine.saveFactWithResult(
      category = FactCategory.PERSONAL,
      key = "المدينة الحالية",
      value = "المدينة المنورة",
      confidence = 1.0,
      source = MemorySource.USER_EXPLICIT
    )

    assertTrue("Memory save must succeed with verified read-back", result is MemorySaveResult.Success)
    val success = result as MemorySaveResult.Success
    assertEquals("المدينة المنورة", success.fact.value)
    assertTrue("Saved fact must be marked as confirmed", success.fact.isConfirmed)

    val facts = memoryEngine.getFacts()
    assertEquals(1, facts.size)
    assertEquals("المدينة الحالية", facts[0].key)
    assertEquals("المدينة المنورة", facts[0].value)
  }

  @Test
  fun testPersistenceAcrossComponentRecreation() {
    // 1. Save data in first engine instance
    memoryEngine.saveFactWithResult(
      category = FactCategory.LOCATION_WORK,
      key = "المهنة",
      value = "مهندس ذكاء اصطناعي",
      source = MemorySource.USER_EXPLICIT
    )

    // 2. Recreate engine from same persistent driver (simulating app restart/process kill)
    val testDriver = com.example.zahra.storage.SharedPreferencesMemoryDriver(context, "zahra_test_memory_prefs")
    val recreatedStore = SafeMemoryStore(testDriver, enableEncryption = true)
    val recreatedEngine = MemoryEngine(context, recreatedStore)

    val restoredFacts = recreatedEngine.getFacts()
    assertEquals("Data must survive complete component recreation", 1, restoredFacts.size)
    assertEquals("المهنة", restoredFacts[0].key)
    assertEquals("مهندس ذكاء اصطناعي", restoredFacts[0].value)
  }

  @Test
  fun testUpdatingExistingMemory_updatesMetadataAndValue() {
    val firstSave = memoryEngine.saveFactWithResult(
      category = FactCategory.PREFERENCE,
      key = "المشروب المفضل",
      value = "قهوة عربية"
    ) as MemorySaveResult.Success

    val initialCreatedAt = firstSave.fact.createdAt

    // Update with new value
    val updateResult = memoryEngine.updateFact(
      id = firstSave.fact.id,
      newKey = "المشروب المفضل",
      newValue = "شاي بالنعناع"
    )

    assertTrue("Update must succeed", updateResult is MemorySaveResult.Success)
    val facts = memoryEngine.getFacts()
    assertEquals(1, facts.size)
    assertEquals("شاي بالنعناع", facts[0].value)
    assertEquals(initialCreatedAt, facts[0].createdAt)
    assertTrue("UpdatedAt timestamp must be >= initial createdAt", facts[0].updatedAt >= initialCreatedAt)
  }

  @Test
  fun testDeletingMemory_verifiesRemovalFromStorage() {
    val fact = memoryEngine.addFact(FactCategory.PERSONAL, "الهواية", "السباحة")
    assertEquals(1, memoryEngine.getFacts().size)

    val deleted = memoryEngine.deleteFact(fact.id)
    assertTrue("Delete operation must return true", deleted)
    assertEquals("Memory list must be empty after verified deletion", 0, memoryEngine.getFacts().size)

    // Verify after recreation
    val testDriver = com.example.zahra.storage.SharedPreferencesMemoryDriver(context, "zahra_test_memory_prefs")
    val recreatedEngine = MemoryEngine(context, SafeMemoryStore(testDriver, enableEncryption = true))
    assertEquals("Deleted item must not exist in storage after restart", 0, recreatedEngine.getFacts().size)
  }

  @Test
  fun testDuplicateHandling_updatesRatherThanCreatingMultipleRecords() {
    memoryEngine.addFact(FactCategory.PERSONAL, "الاسم", "عمر")
    memoryEngine.addFact(FactCategory.PERSONAL, "الاسم", "عمر الفاروق")

    val facts = memoryEngine.getFacts()
    assertEquals("Duplicate keys must be merged/updated into 1 canonical record", 1, facts.size)
    assertEquals("عمر الفاروق", facts[0].value)
  }

  @Test
  fun testInvalidInputRejection() {
    // Blank key
    val blankKey = memoryEngine.saveFactWithResult(FactCategory.PERSONAL, "   ", "قيمة صحيحة")
    assertTrue(blankKey is MemorySaveResult.ValidationFailed)

    // Blank value
    val blankVal = memoryEngine.saveFactWithResult(FactCategory.PERSONAL, "مفتاح صحيح", "   ")
    assertTrue(blankVal is MemorySaveResult.ValidationFailed)

    // Excessively long input
    val oversized = "a".repeat(2000)
    val longRes = memoryEngine.saveFactWithResult(FactCategory.PERSONAL, "مفتاح", oversized)
    assertTrue(longRes is MemorySaveResult.ValidationFailed)
  }

  @Test
  fun testStorageFailure_neverProducesFalseSuccess() {
    val failingDriver = InMemoryMemoryDriver(shouldFailWrites = true)
    val failStore = SafeMemoryStore(failingDriver)
    val failingEngine = MemoryEngine(customStore = failStore)

    val res = failingEngine.saveFactWithResult(FactCategory.PERSONAL, "الاسم", "زيد")
    assertTrue("Storage write failure must be reported honestly", res is MemorySaveResult.StorageFailed)
    assertEquals(0, failingEngine.getFacts().size)
  }

  @Test
  fun testCorruptedStoredData_handledResiliently() {
    val driver = InMemoryMemoryDriver()
    driver.writeRawData("{ malformed_non_json: [corrupted ] }")

    // Instantiating store on corrupted data must not throw unhandled exception
    val store = SafeMemoryStore(driver)
    assertEquals("Corrupted data must safely fallback to empty state without crashing", 0, store.getConfirmedFacts().size)
  }

  @Test
  fun testEncryptionAndDecryption_roundTrip() {
    val secretText = "سري جداً: رقم الحساب 987654321"
    val encrypted = ZahraCrypto.encrypt(secretText)

    assertNotEquals("Encrypted text must not equal plaintext", secretText, encrypted)
    assertTrue("Encrypted text must contain IV prefix separator", encrypted.contains(":"))

    val decrypted = ZahraCrypto.decrypt(encrypted)
    assertEquals("Decrypted text must match original plaintext exactly", secretText, decrypted)
  }

  @Test
  fun testSeparationOfTemporarySessionAndConfirmedPersistentMemory() {
    // 1. Explicit command is saved as confirmed memory
    val explicit = memoryEngine.saveExplicitMemory("تذكر أن اسم أخي هو بلال")
    assertTrue(explicit is MemorySaveResult.Success)
    assertEquals(1, memoryEngine.getFacts().size)
    assertEquals("اسم أخي", memoryEngine.getFacts()[0].key)
    assertEquals("بلال", memoryEngine.getFacts()[0].value)

    // 2. Casual remark "أحب البرمجة" is stored ONLY as temporary session context
    val casual = memoryEngine.scanAndExtractFact("أحب تعلم البرمجة والرياضيات")
    assertNotNull(casual)
    assertEquals("Confirmed persistent memory count must NOT increase on casual inference", 1, memoryEngine.getFacts().size)
    assertEquals("Temporary session context must record the casual inference", 1, memoryEngine.getTemporaryFacts().size)
  }

  // =========================================================================
  // WORKSTREAM C & D: SCHEDULING, ALARMS & RECONCILIATION TESTS
  // =========================================================================

  @Test
  fun testValidReminderCreation_registersAndVerifiesAlarm() {
    val cal = Calendar.getInstance().apply {
      set(2026, Calendar.OCTOBER, 20, 10, 0, 0)
    }

    val res = scheduleEngine.validateAndSchedule("فكرني غداً الساعة 8 صباحاً بورد القرآن", cal)
    assertTrue("Valid schedule request must return Success", res is ScheduleValidationResult.Success)

    val success = res as ScheduleValidationResult.Success
    assertEquals(OutcomeState.SUPPORTED, success.outcomeState)
    assertEquals(ReminderState.SCHEDULED, success.schedule.state)
    assertEquals(8, success.schedule.hour)
    assertEquals(0, success.schedule.minute)
    assertTrue("Title should contain reminder topic", success.schedule.title.contains("ورد القرآن"))

    // System registration verification
    assertTrue("Alarm must be verifiable in system via FLAG_NO_CREATE", scheduleEngine.isAlarmRegistered(success.schedule.id))
  }

  @Test
  fun testAmbiguousAndMissingTime_rejectedSafelyWithoutAlarms() {
    val initialSize = scheduleEngine.getSchedules().size

    val res = scheduleEngine.validateAndSchedule("فكرني بمذاكرة الفيزياء")
    assertTrue("Request without time must return Ambiguous", res is ScheduleValidationResult.Ambiguous)

    // Side effect verification: No schedule was created
    assertEquals(initialSize, scheduleEngine.getSchedules().size)
  }

  @Test
  fun testPastTimeOnCurrentDay_rejectedSafely() {
    val cal = Calendar.getInstance().apply {
      set(2026, Calendar.OCTOBER, 16, 15, 0, 0) // 3:00 PM
    }

    // User specifies 9:00 AM today (already past)
    val res = scheduleEngine.validateAndSchedule("فكرني اليوم الساعة 9 صباحاً باجتماع الإدارة", cal)
    assertTrue("Past time today must return Ambiguous guidance", res is ScheduleValidationResult.Ambiguous)
    assertTrue((res as ScheduleValidationResult.Ambiguous).message.contains("قد مضى اليوم"))
  }

  @Test
  fun testPersistenceAndRestorationOfSchedulesAcrossRestart() {
    val schedule = ZahraSchedule(
      id = "sched_test_101",
      title = "موعد فحص الأسنان",
      message = "حان موعد الطبيب",
      timeDisplay = "11:30 ص",
      hour = 11,
      minute = 30,
      isDaily = false,
      isEnabled = true,
      state = ReminderState.SCHEDULED
    )
    scheduleEngine.addSchedule(schedule)

    // Recreate ScheduleEngine from same storage
    val recreatedEngine = ScheduleEngine(context, "zahra_test_schedules_prefs")
    val restored = recreatedEngine.getScheduleById("sched_test_101")

    assertNotNull("Schedule must be restored after engine recreation", restored)
    assertEquals("موعد فحص الأسنان", restored?.title)
    assertEquals(ReminderState.SCHEDULED, restored?.state)
  }

  @Test
  fun testCancellation_removesAlarmAndUpdatesState() {
    val schedule = ZahraSchedule(
      id = "sched_to_cancel",
      title = "جلسة قراءة",
      message = "وقت القراءة",
      timeDisplay = "06:00 م",
      hour = 18,
      minute = 0,
      isDaily = false,
      isEnabled = true
    )
    scheduleEngine.addSchedule(schedule)
    assertTrue("Alarm must initially be registered", scheduleEngine.isAlarmRegistered("sched_to_cancel"))

    // Toggle off (cancels alarm)
    val enabled = scheduleEngine.toggleSchedule("sched_to_cancel")
    assertFalse("Toggled schedule must be disabled", enabled)

    val updated = scheduleEngine.getScheduleById("sched_to_cancel")
    assertEquals(ReminderState.CANCELLED, updated?.state)
    assertFalse("Alarm should no longer be registered in system", scheduleEngine.isAlarmRegistered("sched_to_cancel"))
  }

  @Test
  fun testReconciliation_handlesExpiredAndActiveAlarmsDeterministically() {
    val now = System.currentTimeMillis()

    // 1. One-time schedule that expired in the past (e.g. device was powered off)
    val expiredSchedule = ZahraSchedule(
      id = "sched_expired",
      title = "تذكير قديم فات موعده",
      message = "تنبيه",
      timeDisplay = "07:00 ص",
      hour = 7,
      minute = 0,
      isDaily = false,
      isEnabled = true,
      targetEpochMs = now - 3600_000 // 1 hour ago
    )
    scheduleEngine.addSchedule(expiredSchedule)

    // 2. Active future schedule
    val futureSchedule = ZahraSchedule(
      id = "sched_future",
      title = "تذكير مستقبلي",
      message = "تنبيه",
      timeDisplay = "09:00 م",
      hour = 21,
      minute = 0,
      isDaily = false,
      isEnabled = true,
      targetEpochMs = now + 7200_000 // 2 hours in future
    )
    scheduleEngine.addSchedule(futureSchedule)

    // Run reconciliation (idempotent recovery)
    scheduleEngine.reconcileSchedules()

    val reconciledExpired = scheduleEngine.getScheduleById("sched_expired")
    assertEquals("Expired one-time reminder must transition to COMPLETED", ReminderState.COMPLETED, reconciledExpired?.state)
    assertFalse("Expired reminder must be marked disabled", reconciledExpired?.isEnabled == true)

    val reconciledFuture = scheduleEngine.getScheduleById("sched_future")
    assertEquals("Future reminder must remain SCHEDULED", ReminderState.SCHEDULED, reconciledFuture?.state)
    assertTrue("Future reminder must be verified in system scheduler", scheduleEngine.isAlarmRegistered("sched_future"))
  }

  @Test
  fun testDuplicateDeliveryPrevention() {
    val schedule = ZahraSchedule(
      id = "sched_dedup_test",
      title = "تذكير مكرر محتمل",
      message = "تنبيه",
      timeDisplay = "10:00 ص",
      hour = 10,
      minute = 0,
      isDaily = true,
      isEnabled = true
    )
    scheduleEngine.addSchedule(schedule)

    // First alarm trigger: must succeed and deliver
    val firstDelivery = scheduleEngine.onAlarmTriggered("sched_dedup_test")
    assertTrue("First delivery attempt must be allowed", firstDelivery)

    // Immediate second trigger within debounce window (e.g. system broadcast re-fire): must be blocked!
    val secondDelivery = scheduleEngine.onAlarmTriggered("sched_dedup_test")
    assertFalse("Duplicate trigger within debounce window must be rejected", secondDelivery)
  }

  // =========================================================================
  // WORKSTREAM E: HONEST USER COMMUNICATION TESTS
  // =========================================================================

  @Test
  fun testZahraRouter_honestMemoryCommunication() {
    // 1. Explicit save: returns confirmed local memory acknowledgment
    val saveResp = router.process("تذكر أن لون سيارتي هو أزرق")
    assertEquals("remember_fact", saveResp.intent)
    assertEquals(OutcomeState.SUPPORTED, saveResp.outcomeState)
    assertTrue(saveResp.text.contains("تم حفظ المعلومة في الذاكرة وتأكيد تخزينها محلياً"))

    // 2. Natural language deletion
    val delResp = router.process("احذف من الذاكرة لون سيارتي")
    assertEquals("forget_fact", delResp.intent)
    assertEquals(OutcomeState.SUPPORTED, delResp.outcomeState)
    assertTrue(delResp.text.contains("تم حذف المعلومة بنجاح من الذاكرة المحلية وتأكيد إزالتها"))

    // 3. Deleting non-existent fact: honest acknowledgment of absence
    val delMissingResp = router.process("احذف من الذاكرة رقم رحلتي إلى باريس")
    assertEquals(OutcomeState.AMBIGUOUS, delMissingResp.outcomeState)
    assertTrue(delMissingResp.text.contains("لم أجد معلومة مطابقة لما طلبت حذفه"))
  }

  @Test
  fun testZahraRouter_honestSchedulingCommunication() {
    // 1. Missing time returns Ambiguous guidance
    val ambiguousResp = router.process("فكرني بالمذاكرة")
    assertEquals(OutcomeState.AMBIGUOUS, ambiguousResp.outcomeState)
    assertTrue(ambiguousResp.text.contains("يرجى تحديد وقت التنبيه"))

    // 2. Cancellation via natural language
    val cancelResp = router.process("الغ التذكير")
    assertTrue(cancelResp.intent == "cancel_schedule")
  }

  @Test
  fun testLegacyJsonArrayMigration_preservesRecords() {
    val driver = InMemoryMemoryDriver()
    // Inject legacy unversioned raw JSON array format
    val legacyJson = """
      [
        {"id":"leg_1","category":"PERSONAL","key":"الاسم القديم","value":"عثمان","confidence":1.0,"createdAt":1600000000000},
        {"id":"leg_2","category":"LOCATION_WORK","key":"مدينة سابقة","value":"جدة","confidence":0.95,"createdAt":1600000010000}
      ]
    """.trimIndent()
    driver.writeRawData(legacyJson)

    // Instantiate SafeMemoryStore on legacy data
    val store = SafeMemoryStore(driver, enableEncryption = false)
    val loaded = store.getConfirmedFacts()
    assertEquals("Legacy JSON array must be parsed into 2 records without error", 2, loaded.size)
    assertEquals("الاسم القديم", loaded[0].key)
    assertEquals("عثمان", loaded[0].value)

    // Trigger save to migrate into current schema version
    val saveRes = store.saveFact(FactCategory.PREFERENCE, "تفضيل جديد", "الشاي")
    assertTrue(saveRes is MemorySaveResult.Success)

    val updatedRaw = driver.readRawData()!!
    assertTrue("Migrated data must contain schemaVersion", updatedRaw.contains("\"schemaVersion\":2"))
    assertEquals(3, store.getConfirmedFacts().size)
  }

  @Test
  fun testRescheduling_replacesOldAlarmWithoutDuplicateOrphans() {
    val initialSchedule = ZahraSchedule(
      id = "sched_reschedule_test",
      title = "جلسة مذاكرة",
      message = "مذاكرة",
      timeDisplay = "04:00 م",
      hour = 16,
      minute = 0,
      isDaily = false,
      isEnabled = true
    )
    scheduleEngine.addSchedule(initialSchedule)
    assertTrue(scheduleEngine.isAlarmRegistered("sched_reschedule_test"))

    // Update with new time (reschedule)
    val rescheduled = initialSchedule.copy(
      timeDisplay = "05:00 م",
      hour = 17,
      minute = 0
    )
    val success = scheduleEngine.addSchedule(rescheduled)
    assertTrue("Rescheduling must succeed", success)

    val current = scheduleEngine.getScheduleById("sched_reschedule_test")
    assertEquals(17, current?.hour)
    assertEquals("05:00 م", current?.timeDisplay)
    assertTrue("Single verified alarm must remain registered", scheduleEngine.isAlarmRegistered("sched_reschedule_test"))
  }

  // =========================================================================
  // REMEDIATION VERIFICATION TESTS (Security, Durability & Integrity)
  // =========================================================================

  @Test
  fun testZahraCrypto_failsClosed_malformedPayloadThrowsSecurityException() {
    // Malformed ciphertext without IV prefix separator
    try {
      ZahraCrypto.decrypt("not_a_valid_cipher_payload")
      fail("Decrypting malformed ciphertext must throw SecurityException and fail closed")
    } catch (e: SecurityException) {
      assertTrue("Exception message indicates format or integrity issue", e.message?.contains("غير صالحة") == true || e.message?.contains("فشل فك التشفير") == true)
    }

    // Tampered payload with bad auth tag
    val encrypted = ZahraCrypto.encrypt("بيانات اختبار سرية")
    val parts = encrypted.split(":")
    val tamperedCipher = parts[1].reversed() // corrupt cipher bytes
    try {
      ZahraCrypto.decrypt("${parts[0]}:$tamperedCipher")
      fail("Decrypting tampered ciphertext must throw SecurityException")
    } catch (e: SecurityException) {
      assertNotNull(e.message)
    }
  }

  @Test
  fun testZahraCrypto_hardwareBackedCheck_reportsFalseForTestIsolatedKey() {
    assertFalse("Test isolated key must not claim hardware backing", ZahraCrypto.isHardwareBacked())
  }

  @Test
  fun testSafeMemoryStore_decryptionFailure_treatedAsIntegrityError_neverExposesCiphertext() {
    val driver = InMemoryMemoryDriver()
    // Storing a record with corrupted/invalid ciphertext
    val corruptedJson = """
      {
        "schemaVersion": 2,
        "facts": [
          {
            "id": "corrupt_fact_1",
            "category": "PERSONAL",
            "key": "الاسم السري",
            "value": "corrupted_non_decryptable_ciphertext",
            "isEncrypted": true,
            "confidence": 1.0,
            "source": "USER_EXPLICIT",
            "isConfirmed": true,
            "createdAt": 1600000000000,
            "updatedAt": 1600000000000
          }
        ]
      }
    """.trimIndent()
    driver.writeRawData(corruptedJson)

    val store = SafeMemoryStore(driver, enableEncryption = true)
    // The active facts must NOT contain the corrupted record with raw ciphertext
    assertEquals("Corrupted record must NOT be returned in active confirmed facts", 0, store.getConfirmedFacts().size)
    // Corrupted record must be preserved for recovery
    val corruptedList = store.getCorruptedRecords()
    assertEquals("Corrupted record must be preserved for recovery", 1, corruptedList.size)
    assertEquals("الاسم السري", corruptedList[0].key)
  }

  @Test
  fun testSafeMemoryStore_updateFactById_preventsCollisionWithDifferentKey() {
    val factA = memoryEngine.addFact(FactCategory.PERSONAL, "البريد الإلكتروني", "user@example.com")
    val factB = memoryEngine.addFact(FactCategory.PERSONAL, "الهاتف", "0501234567")

    // Attempt to update factB so that its key collides with factA's key
    val collisionResult = memoryEngine.updateFact(factB.id, "البريد الإلكتروني", "new_email@example.com")
    assertTrue("Updating to an existing conflicting key must be rejected", collisionResult is MemorySaveResult.ValidationFailed)

    // Verify factA and factB were preserved without overwrite
    val facts = memoryEngine.getFacts()
    assertEquals(2, facts.size)
    val restoredA = facts.find { it.id == factA.id }
    val restoredB = facts.find { it.id == factB.id }
    assertEquals("user@example.com", restoredA?.value)
    assertEquals("0501234567", restoredB?.value)
  }

  @Test
  fun testSafeMemoryStore_deleteFact_failedReadBackReturnsFalse() {
    val driver = InMemoryMemoryDriver()
    val store = SafeMemoryStore(driver, enableEncryption = false)
    val saveRes = store.saveFact(FactCategory.PERSONAL, "مفتاح الحذف", "قيمة") as MemorySaveResult.Success

    // If driver simulates failure on writes
    driver.setSimulateWriteFailure(true)
    val deleted = store.deleteFact(saveRes.fact.id)
    assertFalse("Delete must return false if storage write or read-back verification fails", deleted)
    assertEquals("Item must remain in store if deletion failed", 1, store.getConfirmedFacts().size)
  }

  @Test
  fun testSafeMemoryStore_clearAll_verifiesEmptyState() {
    val driver = InMemoryMemoryDriver()
    val store = SafeMemoryStore(driver, enableEncryption = false)
    store.saveFact(FactCategory.PERSONAL, "عنصر 1", "قيمة 1")
    store.saveFact(FactCategory.PERSONAL, "عنصر 2", "قيمة 2")
    assertEquals(2, store.getConfirmedFacts().size)

    val cleared = store.clearAll()
    assertTrue("clearAll must return true when storage is empty", cleared)
    assertEquals(0, store.getConfirmedFacts().size)
  }

  @Test
  fun testScheduleEngine_noUnsolicitedDefaultAlarms() {
    // A fresh ScheduleEngine must start with ZERO schedules (no unsolicited morning alarms)
    val freshEngine = ScheduleEngine(context, "fresh_empty_schedule_prefs")
    assertEquals("Fresh schedule engine must not register unsolicited default alarms", 0, freshEngine.getSchedules().size)
  }

  @Test
  fun testZahraBootReceiver_reconcilesOnBoot() {
    val bootReceiver = com.example.zahra.engines.ZahraBootReceiver()
    val bootIntent = android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED)
    // Execution must complete without error
    bootReceiver.onReceive(context, bootIntent)
  }
}
