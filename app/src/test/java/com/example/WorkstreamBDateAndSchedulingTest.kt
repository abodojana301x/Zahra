package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.zahra.engines.ScheduleEngine
import com.example.zahra.engines.ScheduleValidationResult
import com.example.zahra.entities.EntityExtractor
import com.example.zahra.model.OutcomeState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class WorkstreamBDateAndSchedulingTest {

  private lateinit var context: Context
  private lateinit var scheduleEngine: ScheduleEngine
  private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    scheduleEngine = ScheduleEngine(context)
  }

  // --- B1 & B2: Relative Arabic Dates & Substring Order Verification ---

  @Test
  fun testBaadBokra_resolvesToTwoDaysLater_notTomorrow() {
    // Critical verification for the bug where "بعد بكرة" previously matched "بكرة" and returned 1 day
    val refCal = Calendar.getInstance().apply {
      set(2026, Calendar.MAY, 10, 10, 0, 0)
    }

    val parsedBaadBokra = EntityExtractor.parseRelativeDate("فكرني بعد بكرة الساعة 5", refCal)
    assertNotNull(parsedBaadBokra)
    assertEquals("بعد غد", parsedBaadBokra?.rawExpression)
    assertEquals(2, parsedBaadBokra?.daysOffset)
    assertEquals("2026-05-12", parsedBaadBokra?.formattedDate)

    val parsedBaadGhad = EntityExtractor.parseRelativeDate("سجل موعد بعد غد", refCal)
    assertNotNull(parsedBaadGhad)
    assertEquals("بعد غد", parsedBaadGhad?.rawExpression)
    assertEquals(2, parsedBaadGhad?.daysOffset)
    assertEquals("2026-05-12", parsedBaadGhad?.formattedDate)
  }

  @Test
  fun testBokraAndGhadan_resolvesToTomorrow() {
    val refCal = Calendar.getInstance().apply {
      set(2026, Calendar.MAY, 10, 10, 0, 0)
    }

    val parsedBokra = EntityExtractor.parseRelativeDate("فكرني بكرة الساعة 5", refCal)
    assertNotNull(parsedBokra)
    assertEquals(1, parsedBokra?.daysOffset)
    assertEquals("2026-05-11", parsedBokra?.formattedDate)

    val parsedGhadan = EntityExtractor.parseRelativeDate("سجل مهمة غداً", refCal)
    assertNotNull(parsedGhadan)
    assertEquals(1, parsedGhadan?.daysOffset)
    assertEquals("2026-05-11", parsedGhadan?.formattedDate)
  }

  @Test
  fun testAmsAndImbareh_resolvesToYesterday() {
    val refCal = Calendar.getInstance().apply {
      set(2026, Calendar.MAY, 10, 10, 0, 0)
    }

    val parsedAms = EntityExtractor.parseRelativeDate("تقرير أمس", refCal)
    assertNotNull(parsedAms)
    assertEquals(-1, parsedAms?.daysOffset)
    assertEquals("2026-05-09", parsedAms?.formattedDate)

    val parsedImbareh = EntityExtractor.parseRelativeDate("تقرير امبارح", refCal)
    assertNotNull(parsedImbareh)
    assertEquals(-1, parsedImbareh?.daysOffset)
    assertEquals("2026-05-09", parsedImbareh?.formattedDate)
  }

  @Test
  fun testBaadUsboo_resolvesToSevenDaysLater() {
    val refCal = Calendar.getInstance().apply {
      set(2026, Calendar.MAY, 10, 10, 0, 0)
    }

    val parsed = EntityExtractor.parseRelativeDate("فكرني بعد أسبوع", refCal)
    assertNotNull(parsed)
    assertEquals(7, parsed?.daysOffset)
    assertEquals("2026-05-17", parsed?.formattedDate)
  }

  // --- B3: Calendar Boundaries with Controllable Reference Clocks ---

  @Test
  fun testBoundary_monthEndTransition() {
    // Jan 31 -> "بعد بكرة" must transition to Feb 02
    val refCal = Calendar.getInstance().apply {
      set(2026, Calendar.JANUARY, 31, 14, 0, 0)
    }

    val parsed = EntityExtractor.parseRelativeDate("بعد بكرة", refCal)
    assertNotNull(parsed)
    assertEquals("2026-02-02", parsed?.formattedDate)
  }

  @Test
  fun testBoundary_leapYearTransition() {
    // Feb 28, 2024 (leap year) -> "بعد بكرة" must transition to March 01, 2024
    val refCal = Calendar.getInstance().apply {
      set(2024, Calendar.FEBRUARY, 28, 12, 0, 0)
    }

    val parsed = EntityExtractor.parseRelativeDate("بعد بكرة", refCal)
    assertNotNull(parsed)
    assertEquals("2024-03-01", parsed?.formattedDate)
  }

  @Test
  fun testBoundary_yearEndTransition() {
    // Dec 31, 2026 -> "بعد بكرة" must transition to Jan 02, 2027
    val refCal = Calendar.getInstance().apply {
      set(2026, Calendar.DECEMBER, 31, 20, 0, 0)
    }

    val parsed = EntityExtractor.parseRelativeDate("بعد بكرة", refCal)
    assertNotNull(parsed)
    assertEquals("2027-01-02", parsed?.formattedDate)
  }

  // --- Relative Delays in Time ---

  @Test
  fun testRelativeTime_delays() {
    val refCal = Calendar.getInstance().apply {
      set(2026, Calendar.JUNE, 1, 10, 0, 0)
    }

    val parsedHour = EntityExtractor.parseTimeOrDelay("ذكرني بعد ساعة", refCal)
    assertNotNull(parsedHour)
    assertTrue(parsedHour?.isRelativeDelay == true)
    assertEquals(60, parsedHour?.delayMinutes)
    assertEquals(11, parsedHour?.hour)

    val parsedHalfHour = EntityExtractor.parseTimeOrDelay("ذكرني بعد نصف ساعة", refCal)
    assertNotNull(parsedHalfHour)
    assertTrue(parsedHalfHour?.isRelativeDelay == true)
    assertEquals(30, parsedHalfHour?.delayMinutes)
    assertEquals(30, parsedHalfHour?.minute)

    val parsed10Min = EntityExtractor.parseTimeOrDelay("ذكرني بعد 10 دقائق", refCal)
    assertNotNull(parsed10Min)
    assertTrue(parsed10Min?.isRelativeDelay == true)
    assertEquals(10, parsed10Min?.delayMinutes)
    assertEquals(10, parsed10Min?.minute)
  }

  // --- B4 & B5: Scheduling Validation and Side-Effect Safety ---

  @Test
  fun testScheduling_missingTimeIsRejectedSafely() {
    val initialCount = scheduleEngine.getSchedules().size

    // User asks for a reminder without specifying date or time
    val result = scheduleEngine.validateAndSchedule("فكرني بمذاكرة مادة الرياضيات والفيزياء")

    assertTrue("Missing time request must return Ambiguous", result is ScheduleValidationResult.Ambiguous)
    assertEquals(OutcomeState.AMBIGUOUS, (result as ScheduleValidationResult.Ambiguous).outcomeState)

    // Side effect verification: NO schedule or alarm was created!
    val newCount = scheduleEngine.getSchedules().size
    assertEquals(initialCount, newCount)
  }

  @Test
  fun testScheduling_completeValidRequestSucceeds() {
    val refCal = Calendar.getInstance().apply {
      set(2026, Calendar.OCTOBER, 15, 10, 0, 0)
    }

    val result = scheduleEngine.validateAndSchedule("فكرني بكرة الساعة 5 مساء بميعاد الطبيب", refCal)
    assertTrue("Valid request must succeed", result is ScheduleValidationResult.Success)

    val success = result as ScheduleValidationResult.Success
    assertEquals(OutcomeState.SUPPORTED, success.outcomeState)
    assertEquals(17, success.schedule.hour)
    assertEquals(0, success.schedule.minute)
    assertTrue(success.schedule.timeDisplay.contains("05:00 م"))
    assertTrue(success.schedule.title.contains("الطبيب"))
  }

  @Test
  fun testScheduling_unsupportedRecurrenceRejectedSafely() {
    val initialCount = scheduleEngine.getSchedules().size

    val result = scheduleEngine.validateAndSchedule("فكرني كل شهر بدفع فاتورة الكهرباء")
    assertTrue("Unsupported monthly recurrence must be rejected", result is ScheduleValidationResult.Unsupported)
    assertEquals(OutcomeState.UNSUPPORTED, (result as ScheduleValidationResult.Unsupported).outcomeState)

    // Verify zero side effect
    assertEquals(initialCount, scheduleEngine.getSchedules().size)
  }

  // --- Benchmark: 300 Parameterized Date & Scheduling Cases ---

  @Test
  fun testBenchmark_300DateAndSchedulingCases() {
    var passedCount = 0
    val totalCases = 300

    for (i in 0 until totalCases) {
      val dayOffset = (i % 30) + 1
      val refCal = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 1, 10, 0, 0)
        add(Calendar.DAY_OF_YEAR, i)
      }

      when (i % 4) {
        0 -> {
          // "بعد بكرة" verification across varying reference dates
          val parsed = EntityExtractor.parseRelativeDate("بعد بكرة", refCal)
          assertNotNull(parsed)
          assertEquals(2, parsed?.daysOffset)
          passedCount++
        }
        1 -> {
          // "غداً" verification
          val parsed = EntityExtractor.parseRelativeDate("غداً", refCal)
          assertNotNull(parsed)
          assertEquals(1, parsed?.daysOffset)
          passedCount++
        }
        2 -> {
          // Scheduling validation: missing time must be Ambiguous
          val res = scheduleEngine.validateAndSchedule("فكرني بالمهام رقم $i")
          assertTrue(res is ScheduleValidationResult.Ambiguous)
          passedCount++
        }
        3 -> {
          // Relative time delays
          val parsed = EntityExtractor.parseTimeOrDelay("بعد $dayOffset دقيقة", refCal)
          assertNotNull(parsed)
          assertTrue(parsed?.isRelativeDelay == true)
          assertEquals(dayOffset, parsed?.delayMinutes)
          passedCount++
        }
      }
    }

    assertEquals(totalCases, passedCount)
  }
}
