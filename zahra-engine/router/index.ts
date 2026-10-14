/**
 * Zahra AI Engine — Layer 4: Central Reasoning Router
 * Coordinates Normalization, Intent Classification, Entity Parsing, and Dispatches to Engines.
 */

import { normalizeText } from '../core/normalizer';
import { detectLanguage } from '../core/languageDetector';
import { matchRegexCommand } from '../intent/regexMatcher';
import { NaiveBayesClassifier } from '../intent/naiveBayes';
import { evaluateMath, convertPhysicalUnit } from '../engines/mathEngine';
import { parseUnitRequest } from '../entities/unitParser';
import { TranslationEngine } from '../engines/translationEngine';
import { ReligionEngine } from '../engines/religionEngine';
import { SummarizeEngine } from '../engines/summarizeEngine';
import { TaskEngine } from '../engines/taskEngine';
import { NoteEngine } from '../engines/noteEngine';
import { DialogEngine } from '../engines/dialogEngine';
import { FallbackEngine } from '../engines/fallbackEngine';

export interface ZahraResponse {
  text: string;
  intent: string;
  confidence: number;
  confidenceTier: 'DIRECT' | 'QUALIFIED' | 'UNCERTAIN';
  source?: string;
  followups?: string[];
  actions?: Array<{ type: string; payload: any }>;
}

export class ZahraRouter {
  private nbClassifier: NaiveBayesClassifier;
  private translationEngine: TranslationEngine;
  private religionEngine: ReligionEngine;
  private summarizeEngine: SummarizeEngine;
  public taskEngine: TaskEngine;
  public noteEngine: NoteEngine;
  private dialogEngine: DialogEngine;
  private fallbackEngine: FallbackEngine;

  constructor(corpus?: Record<string, string[]>, kbData?: any) {
    this.nbClassifier = new NaiveBayesClassifier(corpus);
    this.translationEngine = new TranslationEngine(kbData?.translation);
    this.religionEngine = new ReligionEngine(kbData?.religion);
    this.summarizeEngine = new SummarizeEngine();
    this.taskEngine = new TaskEngine();
    this.noteEngine = new NoteEngine();
    this.dialogEngine = new DialogEngine();
    this.fallbackEngine = new FallbackEngine();
  }

  /**
   * Main entrypoint: processes a raw user message through all 6 AI layers.
   */
  public process(rawInput: string): ZahraResponse {
    // 1. Layer 1: Normalization & Language
    const normalized = normalizeText(rawInput);
    const langResult = detectLanguage(rawInput);

    // 2. Layer 4: Check Dialog FSM for active multi-turn flow
    const dialogResult = this.dialogEngine.handleTurn(rawInput);
    if (dialogResult.handled) {
      return {
        text: dialogResult.replyText ?? '',
        intent: 'dialog_followup',
        confidence: 0.95,
        confidenceTier: 'DIRECT',
        source: 'محرك المحادثة الحتمي (FSM Dialog Engine)',
      };
    }

    // 3. Layer 2: Intent Classification (Regex first, then Naive Bayes)
    const regexMatch = matchRegexCommand(normalized);
    let intent = regexMatch ? regexMatch.intent : '';
    let confidence = regexMatch ? regexMatch.confidence : 0;

    if (!regexMatch) {
      const nbResult = this.nbClassifier.classify(normalized);
      intent = nbResult.intent;
      confidence = nbResult.confidence;
    }

    // 4. Confidence Gate (< 0.40 triggers fallback directly)
    if (confidence < 0.40) {
      const fb = this.fallbackEngine.generateFallback(rawInput);
      return {
        text: fb.text,
        intent: 'unknown',
        confidence: fb.confidence,
        confidenceTier: 'UNCERTAIN',
        source: fb.honestNote,
        followups: fb.suggestions,
      };
    }

    // 5. Layer 4: Reasoning Dispatcher
    let responseText = '';
    let source = '';
    let followups: string[] | undefined;
    let actions: Array<{ type: string; payload: any }> | undefined;

    switch (intent) {
      case 'calculate': {
        const mathExpr = regexMatch?.extractedCommand || normalized.replace(/^(?:احسب|calc)\s+/i, '');
        const mathRes = evaluateMath(mathExpr);
        responseText = mathRes.formattedText;
        source = mathRes.source;
        confidence = mathRes.confidence;
        break;
      }

      case 'convert_unit': {
        const parsedUnit = parseUnitRequest(normalized);
        if (parsedUnit && parsedUnit.toUnit) {
          const convRes = convertPhysicalUnit(parsedUnit.value, parsedUnit.fromUnit, parsedUnit.toUnit);
          responseText = convRes.formattedText;
          source = convRes.source;
        } else {
          responseText = 'يرجى تحديد القيمة والوحدة المراد التحويل منها وإليها (مثال: حول 10 كم إلى ميل).';
          source = 'محول الوحدات الفيزيائية';
        }
        break;
      }

      case 'translate': {
        const toTranslate = regexMatch?.extractedCommand || normalized.replace(/^(?:ترجم|translate)\s+/i, '');
        const transRes = this.translationEngine.translate(toTranslate, langResult.language === 'ar' ? 'en' : 'ar');
        responseText = `الترجمة:\n"${transRes.translatedText}"`;
        source = transRes.source;
        confidence = transRes.confidence;
        break;
      }

      case 'summarize': {
        const toSum = regexMatch?.extractedCommand || normalized.replace(/^(?:لخص|summarize)\s+/i, '');
        const sumRes = this.summarizeEngine.summarize(toSum);
        responseText = `الملخص الاستخلاصي:\n${sumRes.summary}\n\n(نسبة الاختصار: ${Math.round((1 - sumRes.compressionRatio) * 100)}%)`;
        source = sumRes.source;
        break;
      }

      case 'ask_religion':
      case 'ask_prayer_time':
      case 'ask_quran':
      case 'ask_hadith': {
        const relRes = this.religionEngine.query(normalized);
        responseText = relRes.text;
        source = `[المصدر: ${relRes.source.reference}]`;
        confidence = relRes.confidence;
        followups = relRes.followups;
        break;
      }

      case 'create_task': {
        const t = this.taskEngine.createTask(rawInput);
        responseText = `تم إنشاء المهمة بنجاح:\n• العنوان: ${t.title}\n• التصنيف التلقائي: ${t.category}\n• الأولوية: ${t.priorityScore}/5${t.dueDate ? `\n• الموعد: ${t.dueDate}` : ''}`;
        source = 'محرك المهام المحلي والذاكرة الآمنة';
        actions = [{ type: 'TASK_CREATED', payload: t }];
        break;
      }

      case 'list_tasks': {
        const tasks = this.taskEngine.listTasks();
        if (tasks.length === 0) {
          responseText = 'قائمة مهامك فارغة حالياً. يمكنك إضافة مهمة بكتابة: "أضف مهمة إرسال التقرير غداً".';
        } else {
          responseText = `قائمة المهام المسجلة (${tasks.length}):\n` +
            tasks.map((t) => `${t.completed ? '✅' : '⏳'} [${t.category}] ${t.title} (أولوية: ${t.priorityScore}/5)`).join('\n');
        }
        source = 'قاعدة بيانات المهام المحلية';
        break;
      }

      case 'remember_note': {
        const n = this.noteEngine.createNote(rawInput);
        responseText = `تم حفظ الملاحظة:\n"${n.title}"\n\nالوسوم المستخرجة تلقائياً (RAKE): #${n.tags.join(' #')}`;
        source = 'خوارزمية RAKE ومستودع الملاحظات المحلي';
        actions = [{ type: 'NOTE_CREATED', payload: n }];
        break;
      }

      case 'list_notes': {
        const notes = this.noteEngine.listNotes();
        if (notes.length === 0) {
          responseText = 'لا توجد ملاحظات مسجلة بعد. لحفظ ملاحظة اكتب: "سجل ملاحظة: فكرة لمشروع جديد".';
        } else {
          responseText = `الملاحظات المحفوظة (${notes.length}):\n` +
            notes.map((n) => `📝 ${n.title} (وسوم: ${n.tags.join(', ')})`).join('\n');
        }
        source = 'مستودع الملاحظات المحلي';
        break;
      }

      case 'greet': {
        responseText = 'وعليكم السلام ورحمة الله وبركاته! أنا زهرة، مساعدك الذكي الخاص الذي يعمل 100% بدون إنترنت وبدون أي نماذج لغوية خارجية أو تتبع. كيف يمكنني مساعدتك اليوم؟';
        source = 'نظام زهرة الترحيبي';
        followups = ['احسب 120 * 4', 'مواقيت الصلاة', 'ترجم سلام إلى الإنجليزية', 'أضف مهمة جديدة'];
        break;
      }

      case 'farewell': {
        responseText = 'في أمان الله وحفظه! كل بياناتك وملاحظاتك محفوظة بأمان تام ومشفرة على جهازك فقط.';
        source = 'نظام زهرة';
        break;
      }

      case 'help': {
        responseText = 'أنا "زهرة" (Zahra 2.0) — مساعد ذكاء حاسوبي وخبير حتمي يعمل بالكامل بدون إنترنت:\n\n' +
          '1. 🧮 الحساب الدقيق: احسب 25 * 12، جذر 144، نسب مئوية\n' +
          '2. ⚖️ تحويل الوحدات: حول 5 كيلو إلى باوند، كم إلى ميل، مئوية لفهرنهايت\n' +
          '3. 🌐 الترجمة: معجم عربي-إنجليزي-فرنسي مع تصريف الكلمات\n' +
          '4. 🕌 الشريعة والمواقيت: آيات، أحاديث، فقه، مواقيت الصلاة الفلكية، القبلة\n' +
          '5. 📄 التلخيص: استخلاص أهم جمل المقالات بخوارزمية TextRank\n' +
          '6. ✅ المهام والملاحظات: استخراج المواعيد والتصنيف التلقائي ووسوم RAKE\n' +
          '7. 🔒 الأمان التام: كل شيء محلي ومشفر 100% دون خوادم خارجية';
        source = 'دليل قدرات نظام زهرة 2.0';
        followups = ['احسب 500 / 4', 'متى صلاة المغرب', 'أضف مهمة جديدة'];
        break;
      }

      case 'thank': {
        responseText = 'على الرحب والسعة دائماً! يسعدني خدمتك بدقة وأمان تام.';
        source = 'نظام زهرة';
        break;
      }

      default: {
        const fb = this.fallbackEngine.generateFallback(rawInput);
        responseText = fb.text;
        source = fb.honestNote;
        confidence = fb.confidence;
        followups = fb.suggestions;
        break;
      }
    }

    // Determine confidence tier
    const tier = confidence >= 0.75 ? 'DIRECT' : confidence >= 0.40 ? 'QUALIFIED' : 'UNCERTAIN';
    if (tier === 'QUALIFIED') {
      responseText = `${responseText}\n\n(ثقة متوسطة)`;
    }

    return {
      text: responseText,
      intent,
      confidence,
      confidenceTier: tier,
      source,
      followups,
      actions,
    };
  }
}
