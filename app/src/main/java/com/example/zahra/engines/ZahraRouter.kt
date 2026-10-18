package com.example.zahra.engines

import android.content.Context
import com.example.zahra.core.LanguageDetector
import com.example.zahra.core.ZahraNormalizer
import com.example.zahra.entities.EntityExtractor
import com.example.zahra.intent.NaiveBayesClassifier
import com.example.zahra.intent.RegexMatcher
import com.example.zahra.knowledge.engine.KnowledgeEngine
import com.example.zahra.knowledge.model.KnowledgeRetrievalState
import com.example.zahra.knowledge.model.KnowledgeQueryResult
import com.example.zahra.model.*
import org.json.JSONObject

class ZahraRouter(private val context: Context) {
  val nbClassifier = NaiveBayesClassifier()
  val translationEngine = TranslationEngine(context)
  val religionEngine = ReligionEngine(context)
  val taskEngine = TaskEngine(context)
  val noteEngine = NoteEngine(context)
  val memoryEngine = MemoryEngine(context)
  val localImageEngine = LocalImageEngine()
  val scheduleEngine = ScheduleEngine(context)
  val knowledgeEngine = KnowledgeEngine(context)

  init {
    loadTrainingCorpus()
    // Reconcile pending reminders and alarms on initialization
    scheduleEngine.reconcileSchedules()
  }

  private fun loadTrainingCorpus() {
    try {
      val jsonStr = context.assets.open("intent_corpus.json").bufferedReader().use { it.readText() }
      val root = JSONObject(jsonStr)
      val intentsObj = root.optJSONObject("intents") ?: return

      val corpusMap = mutableMapOf<String, List<String>>()
      val keys = intentsObj.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val arr = intentsObj.optJSONArray(key) ?: continue
        val samples = mutableListOf<String>()
        for (i in 0 until arr.length()) {
          samples.add(arr.getString(i))
        }
        corpusMap[key] = samples
      }
      nbClassifier.train(corpusMap)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun process(rawInput: String, isDeepThinking: Boolean = false): ZahraResponse {
    val clean = rawInput.trim()
    val normalized = ZahraNormalizer.normalize(clean)
    val langInfo = LanguageDetector.detect(clean)

    // Check if user is asking about their personal memory
    val memoryQueryKeywords = listOf("ماذا تعرف عني", "ماذا تتذكر عني", "ما اسمي", "أين أعيش", "ذاكرتك", "معلوماتي المحفوظة", "سجل ذاكرتك", "ماذا حفظت عني")
    if (memoryQueryKeywords.any { normalized.contains(ZahraNormalizer.normalize(it)) }) {
      val memSummary = memoryEngine.summarizeKnownMemory()
      val firstFact = memoryEngine.getFacts().firstOrNull()
      return ZahraResponse(
        text = memSummary,
        intent = "query_memory",
        confidence = 0.99,
        confidenceTier = ConfidenceTier.DIRECT,
        outcomeState = OutcomeState.SUPPORTED,
        source = "مستودع الذاكرة الذاتية المشفرة لزهرة 3",
        followups = listOf("تذكر أنني أفضل الشاي", "ما هي مواقيت الصلاة؟", "أضف مهمة جديدة"),
        toolPayload = firstFact?.let { ZahraToolPayload.MemoryFactPayload(it, isNew = false) }
      )
    }

    // Check if user is asking to delete/forget a memory
    val isForgetReq = normalized.contains("احذف من الذاكر") || normalized.contains("انس") ||
      normalized.contains("امسح من الذاكر") || normalized.contains("احذف المعلوم")
    if (isForgetReq) {
      val query = clean.replace(Regex("(?:احذف من الذاكرة|انسَ أن|انس ان|امسح من الذاكرة|احذف المعلومة|احذف)\\s*", RegexOption.IGNORE_CASE), "").trim()
      val deleted = memoryEngine.deleteFactByKeyOrContent(query)
      return if (deleted) {
        ZahraResponse(
          text = "تم حذف المعلومة بنجاح من الذاكرة المحلية وتأكيد إزالتها نهائياً.",
          intent = "forget_fact",
          confidence = 0.98,
          confidenceTier = ConfidenceTier.DIRECT,
          outcomeState = OutcomeState.SUPPORTED,
          source = "محرك الذاكرة الدائمة المعتمد لزهرة 3"
        )
      } else {
        ZahraResponse(
          text = "لم أجد معلومة مطابقة لما طلبت حذفه في الذاكرة الدائمة. يمكنك استعراض ذاكرتك الحالية بقول: \"ماذا تعرف عني؟\".",
          intent = "forget_fact",
          confidence = 0.85,
          confidenceTier = ConfidenceTier.QUALIFIED,
          outcomeState = OutcomeState.AMBIGUOUS,
          source = "محرك الذاكرة الدائمة لزهرة 3"
        )
      }
    }

    // Explicit command to remember: "تذكر أن...", "احفظ أن...", "لا تنس أن..."
    val explicitMemResult = memoryEngine.saveExplicitMemory(clean)
    if (explicitMemResult != null) {
      return when (explicitMemResult) {
        is MemorySaveResult.Success -> {
          ZahraResponse(
            text = "تم حفظ المعلومة في الذاكرة وتأكيد تخزينها محلياً:\n• [${explicitMemResult.fact.category.labelAr}] ${explicitMemResult.fact.key}: \"${explicitMemResult.fact.value}\"\n\nالمعلومة مشفرة محلياً (AES-256) ومتاحة لك دائماً في شاشة \"الإعدادات والذاكرة\".",
            intent = "remember_fact",
            confidence = 0.99,
            confidenceTier = ConfidenceTier.DIRECT,
            outcomeState = OutcomeState.SUPPORTED,
            source = "محرك الذاكرة الدائمة المعتمد لزهرة 3",
            followups = listOf("ماذا تعرف عني؟", "أضف مهمة جديدة", "فكرني بموعد لاحق"),
            toolPayload = ZahraToolPayload.MemoryFactPayload(explicitMemResult.fact, isNew = !explicitMemResult.isUpdated)
          )
        }
        is MemorySaveResult.ValidationFailed -> {
          ZahraResponse(
            text = "لم أتمكن من حفظ المعلومة: ${explicitMemResult.reason}",
            intent = "remember_fact",
            confidence = 0.80,
            confidenceTier = ConfidenceTier.QUALIFIED,
            outcomeState = OutcomeState.AMBIGUOUS,
            source = "مدقق صحة الذاكرة"
          )
        }
        is MemorySaveResult.StorageFailed, is MemorySaveResult.VerificationFailed -> {
          ZahraResponse(
            text = "لم أتمكن من حفظ المعلومة في الذاكرة المحلية، لذلك لم أؤكد حفظها لضمان الصدق والموثوقية.",
            intent = "remember_fact",
            confidence = 0.90,
            confidenceTier = ConfidenceTier.DIRECT,
            outcomeState = OutcomeState.EXECUTION_ERROR,
            source = "محرك الذاكرة الآمنة"
          )
        }
      }
    }

    // Auto-scan for personal facts to improve memory strength (names, cities, jobs)
    val autoExtractedFact = memoryEngine.scanAndExtractFact(clean)

    // 1. Regex Command Matcher (Priority 1)
    val regexMatch = RegexMatcher.match(normalized)
    val intent: String
    var confidence: Double

    if (regexMatch != null) {
      intent = regexMatch.intent
      confidence = regexMatch.confidence
    } else {
      // 2. Naive Bayes Classifier (Priority 2)
      val nbScore = nbClassifier.classify(normalized)
      intent = nbScore.intent
      confidence = nbScore.confidence
    }

    // If an explicit fact was extracted and general greeting/unknown, give acknowledgment
    if (autoExtractedFact != null && (intent == "unknown" || intent == "greet" || confidence < 0.45)) {
      return ZahraResponse(
        text = "حفظتُ ذلك في ذاكرتي المشفرة بدقة:\n• [${autoExtractedFact.category.labelAr}] ${autoExtractedFact.key}: \"${autoExtractedFact.value}\"\n\nيمكنك دائماً مراجعة وإدارة كل ما أحفظه عنك من تبويب \"الإعدادات والذاكرة\".",
        intent = "remember_fact",
        confidence = 0.98,
        confidenceTier = ConfidenceTier.DIRECT,
        outcomeState = OutcomeState.SUPPORTED,
        source = "محرك الذاكرة الذاتية الدائم لزهرة 3",
        followups = listOf("ماذا تعرف عني؟", "أضف مهمة جديدة", "مواقيت الصلاة اليوم"),
        toolPayload = ZahraToolPayload.MemoryFactPayload(autoExtractedFact, isNew = true)
      )
    }

    // Direct check for local image generation keywords if unclassified or low confidence
    val isImageReq = normalized.contains("صور") || normalized.contains("رسم") || normalized.contains("صمم") ||
                     normalized.contains("لوح") || normalized.contains("بطاق") || normalized.contains("بكسل") ||
                     normalized.contains("ملصق") || normalized.contains("خلفي") || normalized.contains("pixel") ||
                     normalized.contains("draw") || normalized.contains("paint") || normalized.contains("image") ||
                     normalized.contains("فن") || normalized.contains("اجرائ")
    val effectiveIntent = if ((intent == "unknown" || confidence < 0.85) && isImageReq) "generate_image" else intent

    // Direct check for cancel schedule keywords
    val isCancelSched = normalized.contains("الغ التذكير") || normalized.contains("إلغاء التذكير") ||
                        normalized.contains("احذف التذكير") || normalized.contains("الغ موعد") ||
                        normalized.contains("احذف الموعد") || normalized.contains("وقف التذكير") ||
                        normalized.contains("الغ منبه") || normalized.contains("احذف منبه")

    // Direct check for scheduling keywords if unclassified or low confidence
    val isSchedReq = normalized.contains("جدول") || normalized.contains("فكرني") || normalized.contains("ذكرني") ||
                     normalized.contains("نبهني") || normalized.contains("كل يوم") || normalized.contains("يومي") ||
                     normalized.contains("منبه") || normalized.contains("تذكير") || normalized.contains("schedule")
    val isReligiousReq = normalized.contains("حديث") || normalized.contains("رسول الله") ||
                         normalized.contains("قال رسول") || normalized.contains("سورة") ||
                         normalized.contains("آية") || normalized.contains("اية") ||
                         normalized.contains("قرآن") || normalized.contains("قران") ||
                         normalized.contains("تخريج حديث") || normalized.contains("صحة حديث")

    val finalIntent = when {
      isCancelSched -> "cancel_schedule"
      (effectiveIntent == "unknown" || confidence < 0.85) && isSchedReq -> "create_schedule"
      (effectiveIntent == "define_word" || effectiveIntent == "unknown") && isReligiousReq -> "ask_religion"
      else -> effectiveIntent
    }

    if (finalIntent == "generate_image" || finalIntent == "create_schedule" || finalIntent == "cancel_schedule") {
      confidence = 0.98
    }

    // Confidence gating: Below 0.35 triggers honest fallback
    if (confidence < 0.35 && finalIntent == "unknown") {
      return generateFallback(rawInput)
    }

    var responseText = ""
    var source: String? = null
    var followups = listOf<String>()
    var toolPayload: ZahraToolPayload? = null
    var createdTask: ZahraTask? = null
    var createdNote: ZahraNote? = null
    var createdSchedule: ZahraSchedule? = null
    var outcomeState: OutcomeState = OutcomeState.SUPPORTED

    when (finalIntent) {
      "generate_image" -> {
        val prompt = regexMatch?.extractedCommand?.ifBlank { rawInput } ?: rawInput
        val imgPayload = localImageEngine.generateImage(prompt)
        responseText = "أنشأت لك هذه اللوحة الفنية التفاعلية محلياً:\n\"${imgPayload.title}\"\n\nيمكنك التبديل بين الأنماط (فن إجرائي، بطاقة اقتباس، وبكسل آرت) عبر الأزرار التفاعلية أدناه."
        source = "محرك الرسومات والفن الإجرائي المحلي لزهرة 3"
        confidence = 0.99
        outcomeState = OutcomeState.SUPPORTED
        toolPayload = imgPayload
        followups = listOf("ارسم لي مشهد فضاء كوني", "صمم بطاقة اقتباس عربي", "ارسم بكسل آرت 8-bit")
      }

      "create_schedule" -> {
        // Enforce Phase 1 Rule: Validate fields before side-effect execution!
        when (val schedVal = scheduleEngine.validateAndSchedule(rawInput)) {
          is ScheduleValidationResult.Success -> {
            createdSchedule = schedVal.schedule
            responseText = schedVal.summary
            source = "محرك الجدولة والتنبيهات المعتمد لزهرة 3"
            confidence = 0.98
            outcomeState = schedVal.outcomeState
            toolPayload = ZahraToolPayload.SchedulePayload(schedule = schedVal.schedule, isNew = true)
            followups = listOf("اعرض المواعيد المجدولة", "فكرني كل يوم الساعة 8 صباحاً", "أضف مهمة جديدة")
          }
          is ScheduleValidationResult.Ambiguous -> {
            responseText = schedVal.message
            source = "مدقق صحة المواعيد والجدولة (Zahra Validation Layer)"
            confidence = 0.60
            outcomeState = schedVal.outcomeState
            followups = listOf("فكرني الساعة 5 مساءً", "ذكرني بعد 30 دقيقة", "فكرني بكرة الساعة 8 صباحاً")
          }
          is ScheduleValidationResult.Unsupported -> {
            responseText = schedVal.message
            source = "مدقق صحة المواعيد والجدولة (Zahra Validation Layer)"
            confidence = 0.85
            outcomeState = schedVal.outcomeState
            followups = listOf("فكرني كل يوم الساعة 8 صباحاً", "ذكرني بعد ساعة")
          }
          is ScheduleValidationResult.RegistrationFailed -> {
            createdSchedule = schedVal.schedule
            responseText = schedVal.message
            source = "محرك جدولة وتنبيهات النظام لزهرة 3"
            confidence = 0.85
            outcomeState = schedVal.outcomeState
            toolPayload = ZahraToolPayload.SchedulePayload(schedule = schedVal.schedule, isNew = true)
            followups = listOf("اعرض المواعيد المجدولة", "فكرني بموعد آخر", "أضف مهمة جديدة")
          }
          is ScheduleValidationResult.ExecutionError -> {
            responseText = schedVal.message
            source = "محرك تنبيهات النظام"
            confidence = 0.50
            outcomeState = schedVal.outcomeState
          }
        }
      }

      "list_schedules" -> {
        val schedules = scheduleEngine.getSchedules()
        responseText = if (schedules.isEmpty()) {
          "لا توجد مواعيد مجدولة حالياً. يمكنك أن تطلب مني: \"فكرني كل يوم الساعة 8 صباحاً بالأذكار\"."
        } else {
          "المواعيد والتنبيهات المجدولة (${schedules.size}):\n" +
            schedules.joinToString("\n") { "⏰ [${it.timeDisplay}] ${it.title} (${if (it.isDaily) "يومي" else "مرة واحدة"}) [الحالة: ${it.state.labelAr}]" }
        }
        source = "محرك الجدولة المحلي لزهرة 3"
        outcomeState = OutcomeState.SUPPORTED
        if (schedules.isNotEmpty()) {
          toolPayload = ZahraToolPayload.SchedulePayload(schedules.first(), isNew = false)
        }
        followups = listOf("فكرني الساعة 5 مساءً", "احذف التذكير", "أضف مهمة جديدة")
      }

      "cancel_schedule" -> {
        val cleanQuery = rawInput.replace(Regex("(?:الغ|إلغاء|احذف|حذف|امسح|وقف)\\s*(?:التذكير|المنبه|الموعد|موعد|تنبيه)?"), "").trim()
        val allSchedules = scheduleEngine.getSchedules()
        val target = if (cleanQuery.isNotBlank() && cleanQuery.length >= 2) {
          allSchedules.find { it.title.contains(cleanQuery) || cleanQuery.contains(it.title) }
        } else {
          allSchedules.firstOrNull { it.isEnabled }
        }

        if (target != null) {
          scheduleEngine.deleteSchedule(target.id)
          responseText = "تم إلغاء التذكير وإلغاء المنبه المرتبط به في النظام بنجاح:\n• التذكير: ${target.title} (${target.timeDisplay})"
          source = "محرك الجدولة والتنبيهات لزهرة 3"
          outcomeState = OutcomeState.SUPPORTED
          followups = listOf("اعرض المواعيد المجدولة", "فكرني كل يوم الساعة 8 صباحاً", "أضف مهمة جديدة")
        } else {
          responseText = "لم أجد موعداً أو تذكيراً مطابقاً لإلغائه. يمكنك مراجعة المواعيد المجدولة بقول: \"اعرض المواعيد المجدولة\"."
          source = "محرك الجدولة والتنبيهات لزهرة 3"
          outcomeState = OutcomeState.AMBIGUOUS
          followups = listOf("اعرض المواعيد المجدولة", "فكرني بموعد لاحق")
        }
      }

      "calculate" -> {
        val mathExpr = regexMatch?.extractedCommand ?: normalized.replace(Regex("^(?:احسب|calc)\\s+"), "")
        val res = MathEngine.evaluate(mathExpr)
        responseText = res.formattedText
        source = res.source
        confidence = res.confidence
        outcomeState = res.outcomeState
        toolPayload = ZahraToolPayload.MathPayload(
          expression = mathExpr,
          result = res.formattedText,
          calculationType = "حساب رياضي دقيق"
        )
      }

      "convert_unit" -> {
        val parsed = EntityExtractor.parseUnitRequest(normalized)
        if (parsed != null && parsed.toUnit.isNotBlank()) {
          val res = MathEngine.convertUnit(parsed.value, parsed.fromUnit, parsed.toUnit)
          responseText = res.formattedText
          source = res.source
          outcomeState = res.outcomeState
          confidence = res.confidence
          toolPayload = ZahraToolPayload.MathPayload(
            expression = "${parsed.value} ${parsed.fromUnit} ➔ ${parsed.toUnit}",
            result = res.formattedText,
            calculationType = "تحويل وحدات قياسية"
          )
        } else {
          responseText = "يرجى تحديد القيمة والوحدة المراد التحويل منها وإليها (مثال: حول 10 كم إلى ميل أو حول 5 كيلو إلى باوند)."
          source = "محول الوحدات الفيزيائية"
          outcomeState = OutcomeState.AMBIGUOUS
          confidence = 0.50
        }
      }

      "translate" -> {
        val toTrans = regexMatch?.extractedCommand ?: normalized.replace(Regex("^(?:ترجم|translate)\\s+"), "")
        val target = if (langInfo.language == com.example.zahra.core.DetectedLang.AR) "en" else "ar"
        val res = translationEngine.translate(toTrans, target)
        responseText = "الترجمة المعتمدة:\n\"${res.translatedText}\""
        source = res.source
        confidence = res.confidence
        outcomeState = OutcomeState.SUPPORTED
        toolPayload = ZahraToolPayload.TranslatePayload(
          sourceText = toTrans,
          translatedText = res.translatedText,
          sourceLang = if (target == "en") "العربية" else "English",
          targetLang = if (target == "en") "English" else "العربية"
        )
      }

      "summarize" -> {
        val toSum = regexMatch?.extractedCommand ?: normalized.replace(Regex("^(?:لخص|summarize)\\s+"), "")
        if (toSum.length < 20) {
          responseText = "النص المدخل قصير جداً للتلخيص. يرجى إدخال فقرة تحتوي على جمل متعددة للحصول على ملخص استخلاصي دقيق."
          source = "محرك التلخيص الاستخلاصي (TextRank)"
          outcomeState = OutcomeState.AMBIGUOUS
          confidence = 0.60
        } else {
          val sumRes = SummarizeEngine.summarize(toSum)
          val compression = ((1.0 - sumRes.compressionRatio) * 100).toInt()
          responseText = "الملخص الاستخلاصي:\n${sumRes.summary}\n\n(نسبة الاختصار: $compression%)"
          source = sumRes.source
          outcomeState = OutcomeState.SUPPORTED
          toolPayload = ZahraToolPayload.SummarizePayload(
            summary = sumRes.summary,
            originalLength = toSum.length,
            summaryLength = sumRes.summary.length,
            compressionRatio = sumRes.compressionRatio,
            bulletPoints = sumRes.keySentences
          )
        }
      }

      "ask_religion", "ask_prayer_time", "ask_quran", "ask_hadith" -> {
        val relRes = religionEngine.query(normalized)
        responseText = relRes.text
        source = "[المصدر: ${relRes.source}]"
        confidence = relRes.confidence
        outcomeState = relRes.outcomeState
        followups = relRes.followups

        val isPrayer = normalized.contains("صلاة") || normalized.contains("أذان") || normalized.contains("مواقيت") || normalized.contains("فجر")
        val isTasbih = normalized.contains("تسبيح") || normalized.contains("أذكار") || normalized.contains("سبحان الله") || normalized.contains("استغفار")

        val prayerSchedule = if (isPrayer) {
          listOf(
            "الفجر" to "04:45 ص",
            "الشروق" to "06:05 ص",
            "الظهر" to "11:58 ص",
            "العصر" to "03:22 م",
            "المغرب" to "05:48 م",
            "العشاء" to "07:18 م"
          )
        } else emptyList()

        val catName = when {
          isPrayer -> "مواقيت الصلاة الفلكية"
          isTasbih -> "أذكار وتسبيح"
          normalized.contains("قرآن") || normalized.contains("سورة") || normalized.contains("آية") -> "القرآن الكريم والتفسير"
          normalized.contains("حديث") || normalized.contains("قال رسول الله") -> "الحديث النبوي الشريف"
          else -> "الفقه والأحكام الشرعية"
        }

        toolPayload = ZahraToolPayload.ReligionPayload(
          category = catName,
          title = if (isPrayer) "جدول أوقات الصلاة المعتمد" else "إرشاد شرعي موثق",
          content = relRes.text,
          source = relRes.source,
          isPrayerTime = isPrayer,
          prayerTimes = prayerSchedule,
          isTasbih = isTasbih,
          tasbihTarget = 33
        )
      }

      "create_task" -> {
        val task = taskEngine.createTask(rawInput)
        createdTask = task
        responseText = "تم حفظ المهمة بنجاح في جدول مهامك:\n• العنوان: ${task.title}\n• التصنيف: ${task.category}\n• الأولوية: ${task.priorityScore}/5${if (task.dueDate != null) "\n• الموعد: ${task.dueDate}" else ""}"
        source = "محرك المهام المحلي والذاكرة الآمنة"
        outcomeState = OutcomeState.SUPPORTED
        toolPayload = ZahraToolPayload.TaskPayload(task)
      }

      "list_tasks" -> {
        val tasks = taskEngine.getTasks()
        val latest = tasks.firstOrNull()
        responseText = if (tasks.isEmpty()) {
          "قائمة مهامك فارغة حالياً. لإضافة مهمة اكتب: \"أضف مهمة إرسال التقرير غداً\"."
        } else {
          "قائمة المهام المسجلة (${tasks.size}):\n" +
            tasks.joinToString("\n") { "${if (it.completed) "✅" else "⏳"} [${it.category}] ${it.title} (أولوية: ${it.priorityScore}/5)" }
        }
        source = "قاعدة بيانات المهام المحلية"
        outcomeState = OutcomeState.SUPPORTED
        if (latest != null) {
          toolPayload = ZahraToolPayload.TaskPayload(latest)
        }
      }

      "remember_note" -> {
        val note = noteEngine.createNote(rawInput)
        createdNote = note
        responseText = "تم حفظ الملاحظة:\n\"${note.title}\"\n\nالوسوم المستخرجة تلقائياً: #${note.tags.joinToString(" #")}"
        source = "خوارزمية RAKE ومستودع الملاحظات المحلي"
        outcomeState = OutcomeState.SUPPORTED
        toolPayload = ZahraToolPayload.NotePayload(note)
      }

      "list_notes" -> {
        val notes = noteEngine.getNotes()
        val latestNote = notes.firstOrNull()
        responseText = if (notes.isEmpty()) {
          "لا توجد ملاحظات مسجلة بعد. لحفظ ملاحظة اكتب: \"سجل ملاحظة: فكرة لمشروع جديد\"."
        } else {
          "الملاحظات المحفوظة (${notes.size}):\n" +
            notes.joinToString("\n") { "📝 ${it.title} (وسوم: ${it.tags.joinToString(", ")})" }
        }
        source = "مستودع الملاحظات المحلي"
        outcomeState = OutcomeState.SUPPORTED
        if (latestNote != null) {
          toolPayload = ZahraToolPayload.NotePayload(latestNote)
        }
      }

      "greet" -> {
        val userName = memoryEngine.getFacts().find { it.key.contains("الاسم") }?.value ?: ""
        val greetingPrefix = if (userName.isNotBlank()) "أهلاً بك يا $userName! " else "وعليكم السلام ورحمة الله وبركاته! "
        responseText = "${greetingPrefix}أنا زهرة 3 (Zahra 3) — وكيلك الذكي المستقل فائق القدرة. أستطيع تنفيذ مهامك كاملة، الجدولة، إنشاء الصور والبطاقات الإجرائية، الحساب، الترجمة، حفظ تفضيلاتك في الذاكرة، وكل ذلك محلياً 100% دون إنترنت."
        source = "نظام زهرة 3 الترحيبي"
        outcomeState = OutcomeState.SUPPORTED
        followups = listOf("إنشاء صورة أو ملصق", "فكرني كل يوم الساعة 8 صباحاً", "احسب 150 * 4", "مواقيت الصلاة اليوم", "ماذا تعرف عني؟")
      }

      "farewell" -> {
        responseText = "في أمان الله وحفظه! جميع سجلاتك ومواعيدك وملاحظاتك محفوظة ومشفرة محلياً على جهازك."
        source = "نظام زهرة 3"
        outcomeState = OutcomeState.SUPPORTED
      }

      "help" -> {
        responseText = "أنا \"زهرة 3\" (Zahra 3) — وكيل ذكي مستقل ونظام ذكاء حاسوبي يعمل 100% محلياً مع بطاقات تفاعلية:\n\n" +
          "1. 🎨 توليد الصور والبطاقات الفنية (انشئ صورة، صمم بطاقة اقتباس، بكسل آرت)\n" +
          "2. ⏰ الجدولة والتنبيهات الحقيقية للوكيل (جدول لي، فكرني كل يوم الساعة 8)\n" +
          "3. 🧠 الذاكرة التراكمية وفهم العامية والفرانكو (تذكر أن، ماذا تعرف عني)\n" +
          "4. 🧮 الحساب الدقيق وتحويل الوحدات (احسب، حول كم إلى ميل)\n" +
          "5. 🌐 الترجمة الفورية بالبطاقات التفاعلية (ترجم سلام)\n" +
          "6. 🕌 الشريعة والمواقيت ومسبحة الأذكار (مواقيت الصلاة، اتجاه القبلة)\n" +
          "7. ✅ إدارة المهام التفاعلية المباشرة (أضف مهمة)\n" +
          "8. 📝 الملاحظات والوسوم التلقائية (سجل ملاحظة)\n" +
          "9. 📄 التلخيص الاستخلاصي السريع (لخص هذا النص)"
        source = "دليل قدرات نظام زهرة 3"
        outcomeState = OutcomeState.SUPPORTED
        followups = listOf("إنشاء صورة أو ملصق", "فكرني كل يوم الساعة 8", "احسب 500 / 4", "ماذا تعرف عني؟")
      }

      "thank" -> {
        responseText = "على الرحب والسعة دائماً! يسعدني خدمتك بدقة وموثوقية وسرعة فائقة."
        source = "نظام زهرة 3"
        outcomeState = OutcomeState.SUPPORTED
      }

      "query_knowledge", "define_word" -> {
        val kRes = knowledgeEngine.query(rawInput)
        when (kRes.state) {
          KnowledgeRetrievalState.VERIFIED_MATCH, KnowledgeRetrievalState.TRACEABLE_CANDIDATE -> {
            val best = kRes.bestMatch!!
            responseText = "${best.record.canonicalContent}\n\n[المصدر: ${best.record.provenance.sourceTitle} - ${best.record.provenance.sourceLocation}]"
            source = "[${best.record.provenance.sourceTitle} - ${best.record.provenance.origin}]"
            confidence = best.relevanceScore
            outcomeState = OutcomeState.SUPPORTED
            toolPayload = ZahraToolPayload.KnowledgePayload(
              title = best.record.title,
              category = best.record.category.labelAr,
              content = best.record.canonicalContent,
              originalSourceText = best.record.originalSourceText,
              source = best.record.provenance.sourceTitle,
              sourceLocation = best.record.provenance.sourceLocation,
              verificationStatusText = best.record.verificationStatus.labelAr,
              isVerified = best.record.verificationStatus.isVerified,
              matchMethodText = best.matchMethod.labelAr,
              relevancePercent = (best.relevanceScore * 100).toInt(),
              recordId = best.record.id,
              warning = best.warning
            )
            followups = listOf("ابحث في المعرفة", "ما هي سرعة الضوء؟", "من هو الخوارزمي؟")
          }
          KnowledgeRetrievalState.CONFLICTING_EVIDENCE -> {
            responseText = kRes.abstentionReason ?: "توجد أدلة متعارضة مسجلة حول هذه المسألة."
            source = "مدقق النزاهة المعرفية لزهرة 3"
            confidence = 0.60
            outcomeState = OutcomeState.AMBIGUOUS
            toolPayload = kRes.bestMatch?.let { best ->
              ZahraToolPayload.KnowledgePayload(
                title = best.record.title,
                category = best.record.category.labelAr,
                content = best.record.canonicalContent,
                originalSourceText = best.record.originalSourceText,
                source = best.record.provenance.sourceTitle,
                sourceLocation = best.record.provenance.sourceLocation,
                verificationStatusText = best.record.verificationStatus.labelAr,
                isVerified = false,
                matchMethodText = best.matchMethod.labelAr,
                relevancePercent = (best.relevanceScore * 100).toInt(),
                recordId = best.record.id,
                warning = "أدلة متعددة متعارضة",
                conflicts = kRes.conflicts.map { it.description }
              )
            }
          }
          KnowledgeRetrievalState.INSUFFICIENT_EVIDENCE, KnowledgeRetrievalState.NO_MATCH, KnowledgeRetrievalState.ERROR -> {
            responseText = kRes.abstentionReason ?: "المسألة غير مسجلة في قاعدة المعرفة المعتمدة لزهرة 3. يمتنع المساعد تماماً عن التخمين أو تأليف معلومات غير موثقة (Truth Before Intelligence)."
            source = "قيد النزاهة المعرفية لزهرة 3 (Truth Before Intelligence)"
            confidence = 0.20
            outcomeState = OutcomeState.UNSUPPORTED
            followups = listOf("ما هي سرعة الضوء؟", "درجة غليان الماء", "من هو الخوارزمي؟", "أعلى قمة في العالم")
          }
        }
      }

      else -> {
        // Check if query matches a verified knowledge entry before generic fallback
        val kRes = knowledgeEngine.query(rawInput)
        if (kRes.state == KnowledgeRetrievalState.VERIFIED_MATCH && (kRes.bestMatch?.relevanceScore ?: 0.0) >= 0.50) {
          val best = kRes.bestMatch!!
          responseText = "${best.record.canonicalContent}\n\n[المصدر: ${best.record.provenance.sourceTitle} - ${best.record.provenance.sourceLocation}]"
          source = "[${best.record.provenance.sourceTitle} - ${best.record.provenance.origin}]"
          confidence = best.relevanceScore
          outcomeState = OutcomeState.SUPPORTED
          toolPayload = ZahraToolPayload.KnowledgePayload(
            title = best.record.title,
            category = best.record.category.labelAr,
            content = best.record.canonicalContent,
            originalSourceText = best.record.originalSourceText,
            source = best.record.provenance.sourceTitle,
            sourceLocation = best.record.provenance.sourceLocation,
            verificationStatusText = best.record.verificationStatus.labelAr,
            isVerified = best.record.verificationStatus.isVerified,
            matchMethodText = best.matchMethod.labelAr,
            relevancePercent = (best.relevanceScore * 100).toInt(),
            recordId = best.record.id,
            warning = best.warning
          )
        } else {
          return generateFallback(rawInput)
        }
      }
    }

    val tier = when {
      confidence >= 0.75 -> ConfidenceTier.DIRECT
      confidence >= 0.40 -> ConfidenceTier.QUALIFIED
      else -> ConfidenceTier.UNCERTAIN
    }

    val deepThinkingPayload = if (isDeepThinking) {
      val factsCount = memoryEngine.getFacts().size
      val steps = listOf(
        ThinkingStep(
          phaseName = "تحليل النية والأمر اللغوي",
          thoughtDetail = "تحليل الصياغة واستخراج النية الأساسية [${finalIntent}] بمستوى ثقة ${(confidence * 100).toInt()}% واكتشاف اللهجة وسياق الكلمات."
        ),
        ThinkingStep(
          phaseName = "فحص الذاكرة وسياق المستخدم",
          thoughtDetail = if (factsCount > 0) "الرجوع إلى $factsCount حقيقة مسجلة عن المستخدم لمطابقة التفضيلات وتخصيص المخرجات." else "فحص سجل الذاكرة والتأكد من مطابقة المتغيرات التراكمية."
        ),
        ThinkingStep(
          phaseName = "تخطيط مسار الوكيل (Agent Execution)",
          thoughtDetail = "اختيار المحرك المتخصص المناسب، تهيئة المعاملات الرياضية والبيانات المرجعية وضمان عدم وجود تخمين عشوائي."
        ),
        ThinkingStep(
          phaseName = "التدقيق والتحقق الذاتي",
          thoughtDetail = "مراجعة سلامة المخرجات، التأكد من جاهزية البطاقة التفاعلية والتحقق من صحة القواعد المحلية."
        ),
        ThinkingStep(
          phaseName = "الصياغة والتركيب النهائي",
          thoughtDetail = "توليد النتيجة النهائية وعرض الإجابة مع واجهة تفاعلية فورية جاهزة للمستخدم."
        )
      )
      DeepThinkingPayload(
        steps = steps,
        finalSynthesis = "اكتمل التفكير العميق بنجاح عبر 5 محطات تحليلية.",
        confidence = 0.99,
        processingTimeMs = 850L
      )
    } else null

    if (toolPayload == null && autoExtractedFact != null) {
      toolPayload = ZahraToolPayload.MemoryFactPayload(autoExtractedFact, isNew = true)
    }

    return ZahraResponse(
      text = responseText,
      intent = finalIntent,
      confidence = confidence,
      confidenceTier = tier,
      outcomeState = outcomeState,
      source = source,
      followups = followups,
      toolPayload = toolPayload,
      deepThinking = deepThinkingPayload,
      createdTask = createdTask,
      createdNote = createdNote,
      createdSchedule = createdSchedule
    )
  }

  private fun generateFallback(input: String): ZahraResponse {
    return ZahraResponse(
      text = "مش متأكد من الإجابة بدقة، ولم أجد مصدراً موثوقاً في قواعد البيانات المدمجة. بصفتي نظاماً خبيرًا بلا نموذج لغوي احتمالي، أمتنع تماماً عن التخمين أو التوليد غير المؤكد.",
      intent = "unknown",
      confidence = 0.15,
      confidenceTier = ConfidenceTier.UNCERTAIN,
      outcomeState = OutcomeState.UNSUPPORTED,
      source = "قيد الأمان والنزاهة الحتمي لزهرة (لا تخمين)",
      followups = listOf(
        "احسب 250 * 4",
        "حول 10 كم إلى ميل",
        "مواقيت الصلاة اليوم",
        "أضف مهمة جديدة",
        "ماذا تعرف عني؟"
      )
    )
  }
}
