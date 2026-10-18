package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.zahra.knowledge.engine.KnowledgeEngine
import com.example.zahra.knowledge.model.*
import com.example.zahra.knowledge.storage.InMemoryKnowledgeDriver
import com.example.zahra.knowledge.storage.LocalKnowledgeStore
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Phase3PerformanceBenchmarkTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun runKnowledgeEngineBenchmarks() {
    println("=== PHASE 3 KNOWLEDGE ENGINE BENCHMARK REPORT ===")

    // 1. Cold-start and bootstrap initialization
    val coldStartStart = System.currentTimeMillis()
    val driver = InMemoryKnowledgeDriver()
    val engine = KnowledgeEngine(context, driver)
    val coldStartDuration = System.currentTimeMillis() - coldStartStart
    val initialRecordCount = engine.getStorage().totalCount()
    println("Cold Start & Bootstrap Duration: ${coldStartDuration}ms")
    println("Initial Curated Record Count: $initialRecordCount records")
    assertTrue("Cold start must be under 500ms", coldStartDuration < 500)
    assertTrue("Initial records must be loaded", initialRecordCount >= 16)

    // 2. Ingestion Throughput Test (Ingest 100 structured records)
    val store = LocalKnowledgeStore(driver)
    val ingestStart = System.currentTimeMillis()
    val batchSize = 100
    for (i in 1..batchSize) {
      val rec = KnowledgeRecord(
        id = "bench_rec_$i",
        category = KnowledgeCategory.SCIENCE,
        title = "ثابت تجريبي رقم $i في الفيزياء الحديثة",
        canonicalContent = "معلومة علمية محققة رقم $i حول قياسات الطاقة الحركية والكتلة في الفراغ النسبي.",
        originalSourceText = "معلومة علمية محققة رقم $i حول قياسات الطاقة الحركية والكتلة في الفراغ النسبي.",
        normalizedText = "ثابت تجريبي رقم $i فيزياء طاقة كتلة",
        provenance = KnowledgeProvenance(
          origin = "مختبر القياسات الوطنية",
          sourceTitle = "النشرة الدورية للفيزياء",
          sourceId = "BENCH_$i",
          sourceLocation = "التقرير $i"
        ),
        verificationStatus = VerificationStatus.VERIFIED_EMPIRICAL
      )
      store.insertRecord(rec)
    }
    val ingestDuration = System.currentTimeMillis() - ingestStart
    val recordsPerSec = (batchSize.toDouble() / (ingestDuration.toDouble() / 1000.0)).toInt()
    println("Ingested $batchSize records in ${ingestDuration}ms (${recordsPerSec} records/sec)")
    assertTrue("Ingestion throughput must be at least 100 records/sec", recordsPerSec >= 100)

    // 3. Query Latency & Distribution (100 sequential queries)
    val queries = listOf(
      "سرعة الضوء في الفراغ",
      "درجة غليان الماء",
      "معيار التشفير المتقدم AES",
      "أعلى قمة في العالم إفرست",
      "الخوارزمي ومؤسس علم الجبر",
      "ساعات النوم الصحي للبالغين",
      "آية الكرسي وتفسيرها",
      "حديث إنما الأعمال بالنيات",
      "أركان الوضوء في الفقه",
      "سورة الفاتحة"
    )

    val latencies = mutableListOf<Long>()
    // Warmup 10 queries
    for (q in queries) {
      engine.query(q)
    }

    // Benchmark 100 queries
    for (i in 1..100) {
      val q = queries[i % queries.size]
      val qStart = System.nanoTime()
      val res = engine.query(q)
      val qDurationUs = (System.nanoTime() - qStart) / 1000 // microseconds
      latencies.add(qDurationUs)
      assertTrue(res.records.isNotEmpty() || res.bestMatch != null)
    }

    latencies.sort()
    val p50 = latencies[(latencies.size * 0.50).toInt()] / 1000.0 // ms
    val p90 = latencies[(latencies.size * 0.90).toInt()] / 1000.0 // ms
    val p99 = latencies[(latencies.size * 0.99).toInt()] / 1000.0 // ms
    val avg = (latencies.sum() / latencies.size.toDouble()) / 1000.0 // ms

    println("Query Latency Distribution (100 queries):")
    println("  - P50: ${p50}ms")
    println("  - P90: ${p90}ms")
    println("  - P99: ${p99}ms")
    println("  - Average: ${avg}ms")
    assertTrue("P50 latency must be under 50ms", p50 < 50.0)
    assertTrue("P99 latency must be under 100ms", p99 < 100.0)

    // 4. Memory footprint and payload serialization
    val serialized = driver.readRawData() ?: ""
    println("Total Storage Footprint (Raw JSON): ${serialized.toByteArray().size} bytes")
    println("=== BENCHMARK COMPLETED SUCCESSFULLY ===")
  }
}
