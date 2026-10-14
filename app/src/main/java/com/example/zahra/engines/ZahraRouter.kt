package com.example.zahra.engines

import android.content.Context
import com.example.zahra.core.LanguageDetector
import com.example.zahra.core.ZahraNormalizer
import com.example.zahra.entities.EntityExtractor
import com.example.zahra.intent.NaiveBayesClassifier
import com.example.zahra.intent.RegexMatcher
import com.example.zahra.model.ConfidenceTier
import com.example.zahra.model.ZahraResponse
import org.json.JSONObject

class ZahraRouter(private val context: Context) {
  val nbClassifier = NaiveBayesClassifier()
  val translationEngine = TranslationEngine(context)
  val religionEngine = ReligionEngine(context)
  val taskEngine = TaskEngine()
  val noteEngine = NoteEngine()

  init {
    loadTrainingCorpus()
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

  fun process(rawInput: String): ZahraResponse {
    val clean = rawInput.trim()
    val normalized = ZahraNormalizer.normalize(clean)
    val langInfo = LanguageDetector.detect(clean)

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

    // Confidence gating: Below 0.40 triggers honest fallback
    if (confidence < 0.40) {
      return generateFallback(rawInput)
    }

    var responseText = ""
    var source: String? = null
    var followups = listOf<String>()
    var createdTask: com.example.zahra.model.ZahraTask? = null
    var createdNote: com.example.zahra.model.ZahraNote? = null

    when (intent) {
      "calculate" -> {
        val mathExpr = regexMatch?.extractedCommand ?: normalized.replace(Regex("^(?:احسب|calc)\\s+"), "")
        val res = MathEngine.evaluate(mathExpr)
        responseText = res.formattedText
        source = res.source
        confidence = res.confidence
      }

      "convert_unit" -> {
        val parsed = EntityExtractor.parseUnitRequest(normalized)
        if (parsed != null && parsed.toUnit.isNotBlank()) {
          val res = MathEngine.convertUnit(parsed.value, parsed.fromUnit, parsed.toUnit)
          responseText = res.formattedText
          source = res.source
        } else {
          responseText = "يرجى تحديد القيمة والوحدة المراد التحويل منها وإليها (مثال: حول 10 كم إلى ميل أو حول 5 كيلو إلى باوند)."
          source = "محول الوحدات الفيزيائية"
        }
      }

      "translate" -> {
        val toTrans = regexMatch?.extractedCommand ?: normalized.replace(Regex("^(?:ترجم|translate)\\s+"), "")
        val target = if (langInfo.language == com.example.zahra.core.DetectedLang.AR) "en" else "ar"
        val res = translationEngine.translate(toTrans, target)
        responseText = "الترجمة:\n\"${res.translatedText}\""
        source = res.source
        confidence = res.confidence
      }

      "summarize" -> {
        val toSum = regexMatch?.extractedCommand ?: normalized.replace(Regex("^(?:لخص|summarize)\\s+"), "")
        val sumRes = SummarizeEngine.summarize(toSum)
        responseText = "الملخص الاستخلاصي:\n${sumRes.summary}\n\n(نسبة الاختصار: ${((1.0 - sumRes.compressionRatio) * 100).toInt()}%)"
        source = sumRes.source
      }

      "ask_religion", "ask_prayer_time", "ask_quran", "ask_hadith" -> {
        val relRes = religionEngine.query(normalized)
        responseText = relRes.text
        source = "[المصدر: ${relRes.source}]"
        confidence = relRes.confidence
        followups = relRes.followups
      }

      "create_task" -> {
        val task = taskEngine.createTask(rawInput)
        createdTask = task
        responseText = "تم حفظ المهمة بنجاح في جدول مهامك:\n• العنوان: ${task.title}\n• التصنيف التلقائي: ${task.category}\n• الأولوية: ${task.priorityScore}/5${if (task.dueDate != null) "\n• الموعد المستخرج: ${task.dueDate}" else ""}"
        source = "محرك المهام المحلي والذاكرة الآمنة"
      }

      "list_tasks" -> {
        val tasks = taskEngine.getTasks()
        responseText = if (tasks.isEmpty()) {
          "قائمة مهامك فارغة حالياً. لإضافة مهمة اكتب: \"أضف مهمة إرسال التقرير غداً\"."
        } else {
          "قائمة المهام المسجلة (${tasks.size}):\n" +
            tasks.joinToString("\n") { "${if (it.completed) "✅" else "⏳"} [${it.category}] ${it.title} (أولوية: ${it.priorityScore}/5)" }
        }
        source = "قاعدة بيانات المهام المحلية"
      }

      "remember_note" -> {
        val note = noteEngine.createNote(rawInput)
        createdNote = note
        responseText = "تم حفظ الملاحظة:\n\"${note.title}\"\n\nالوسوم المستخرجة تلقائياً (RAKE): #${note.tags.joinToString(" #")}"
        source = "خوارزمية RAKE ومستودع الملاحظات المحلي"
      }

      "list_notes" -> {
        val notes = noteEngine.getNotes()
        responseText = if (notes.isEmpty()) {
          "لا توجد ملاحظات مسجلة بعد. لحفظ ملاحظة اكتب: \"سجل ملاحظة: فكرة لمشروع جديد\"."
        } else {
          "الملاحظات المحفوظة (${notes.size}):\n" +
            notes.joinToString("\n") { "📝 ${it.title} (وسوم: ${it.tags.joinToString(", ")})" }
        }
        source = "مستودع الملاحظات المحلي"
      }

      "greet" -> {
        responseText = "وعليكم السلام ورحمة الله وبركاته! أهلاً بك، أنا زهرة (Zahra 2.0) — مساعدك الذكي المستقل الذي يعمل 100% بدون إنترنت وبدون أي نماذج لغوية أو تتبع. كيف يمكنني مساعدتك؟"
        source = "نظام زهرة الترحيبي"
        followups = listOf("احسب 150 * 4", "مواقيت الصلاة اليوم", "ترجم سلام إلى الإنجليزية", "أضف مهمة جديدة")
      }

      "farewell" -> {
        responseText = "في أمان الله وحفظه! جميع سجلاتك وملاحظاتك محفوظة ومشفرة محلياً على جهازك."
        source = "نظام زهرة"
      }

      "help" -> {
        responseText = "أنا \"زهرة\" (Zahra 2.0) — نظام ذكاء حاسوبي وخبير حتمي يعمل 100% محلياً:\n\n" +
          "1. 🧮 الحساب الدقيق: احسب 25 * 12، جذر 144، نسب مئوية\n" +
          "2. ⚖️ تحويل الوحدات: حول 5 كيلو إلى باوند، كم إلى ميل، مئوية لفهرنهايت\n" +
          "3. 🌐 الترجمة: معجم عربي-إنجليزي-فرنسي مع تصريف الكلمات\n" +
          "4. 🕌 الشريعة والمواقيت: آيات، أحاديث، فقه، مواقيت الصلاة الفلكية، القبلة\n" +
          "5. 📄 التلخيص: استخلاص أهم جمل المقالات بخوارزمية TextRank\n" +
          "6. ✅ المهام والملاحظات: تصنيف تلقائي TF-IDF، استخراج مواعيد، ووسوم RAKE\n" +
          "7. 🔒 الأمان التام: zero-model, zero-network, local encrypted"
        source = "دليل قدرات نظام زهرة 2.0"
        followups = listOf("احسب 500 / 4", "متى صلاة المغرب", "أضف مهمة جديدة")
      }

      "thank" -> {
        responseText = "على الرحب والسعة دائماً! يسعدني خدمتك بدقة وموثوقية تامة."
        source = "نظام زهرة"
      }

      else -> {
        return generateFallback(rawInput)
      }
    }

    val tier = when {
      confidence >= 0.75 -> ConfidenceTier.DIRECT
      confidence >= 0.40 -> ConfidenceTier.QUALIFIED
      else -> ConfidenceTier.UNCERTAIN
    }

    if (tier == ConfidenceTier.QUALIFIED) {
      responseText += "\n\n(ثقة متوسطة)"
    }

    return ZahraResponse(
      text = responseText,
      intent = intent,
      confidence = confidence,
      confidenceTier = tier,
      source = source,
      followups = followups,
      createdTask = createdTask,
      createdNote = createdNote
    )
  }

  private fun generateFallback(input: String): ZahraResponse {
    return ZahraResponse(
      text = "مش متأكد من الإجابة بدقة، ولم أجد مصدراً موثوقاً في قواعد البيانات المدمجة. بصفتي نظاماً خبيرًا بلا نموذج لغوي احتمالي، أمتنع تماماً عن التخمين أو التوليد غير المؤكد.",
      intent = "unknown",
      confidence = 0.15,
      confidenceTier = ConfidenceTier.UNCERTAIN,
      source = "قيد الأمان والنزاهة الحتمي لزهرة (لا تخمين)",
      followups = listOf(
        "احسب 250 * 4",
        "حول 10 كم إلى ميل",
        "مواقيت الصلاة اليوم",
        "أضف مهمة جديدة"
      )
    )
  }
}
