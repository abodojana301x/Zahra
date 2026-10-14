/**
 * Zahra AI Engine — Layer 2: Regex Command Matcher
 * Provides deterministic, instant pattern matching for explicit user commands.
 */

export interface RegexMatchResult {
  intent: string;
  confidence: number;
  extractedCommand?: string;
  matchedPattern: string;
}

interface CommandPattern {
  intent: string;
  pattern: RegExp;
  confidence: number;
  extractGroupIndex?: number;
}

const COMMAND_PATTERNS: CommandPattern[] = [
  // Math & Calculations
  {
    intent: 'calculate',
    pattern: /^(?:احسب|كم\s+يساوي|ناتج|calc(?:ulate)?)\s+([0-9+\-*/^().\s%]+)/i,
    confidence: 0.98,
    extractGroupIndex: 1,
  },
  // Unit conversion
  {
    intent: 'convert_unit',
    pattern: /^(?:حول|تحويل|convert)\s+(.+?)\s+(?:الى|إلى|to)\s+(.+)/i,
    confidence: 0.95,
  },
  // Translation
  {
    intent: 'translate',
    pattern: /^(?:ترجم(?:لي)?|ما\s+ترجمة|translate)\s+(.+?)(?:\s+(?:الى|إلى|to)\s+(.+))?$/i,
    confidence: 0.95,
    extractGroupIndex: 1,
  },
  // Definition / General knowledge
  {
    intent: 'define_word',
    pattern: /^(?:ما\s+هو|ما\s+هي|ما\s+معنى|عرف(?:لي)?|what\s+is)\s+(.+)/i,
    confidence: 0.92,
    extractGroupIndex: 1,
  },
  // Summarization
  {
    intent: 'summarize',
    pattern: /^(?:لخص(?:لي)?|تلخيص|أعطني\s+ملخص|summarize)\s+(.+)/is,
    confidence: 0.95,
    extractGroupIndex: 1,
  },
  // Note recording
  {
    intent: 'remember_note',
    pattern: /^(?:سجل\s+ملاحظة|اكتب\s+ملاحظة|احفظ\s+ملاحظة|تذكر\s+ان|تذكر\s+أن|note:?)\s*(.+)/is,
    confidence: 0.96,
    extractGroupIndex: 1,
  },
  // Task creation
  {
    intent: 'create_task',
    pattern: /^(?:اضف\s+مهمة|أضف\s+مهمة|انشئ\s+مهمة|سجل\s+مهمة|مهمة\s+جديدة|task:?)\s*(.+)/is,
    confidence: 0.96,
    extractGroupIndex: 1,
  },
  // Prayer times
  {
    intent: 'ask_prayer_time',
    pattern: /^(?:متى\s+صلاة|مواقيت\s+الصلاة|موعد\s+اذان|موعد\s+أذان|كم\s+بقي\s+على\s+صلاة|prayer\s+times?)/i,
    confidence: 0.96,
  },
  // Qibla
  {
    intent: 'ask_religion',
    pattern: /^(?:اين\s+القبلة|أين\s+القبلة|اتجاه\s+القبلة|qibla)/i,
    confidence: 0.95,
  },
  // Quran
  {
    intent: 'ask_quran',
    pattern: /^(?:سورة|اية|آية|تفسير\s+اية|تفسير\s+سورة|قران|قرآن)\s+(.+)/i,
    confidence: 0.93,
    extractGroupIndex: 1,
  },
  // Hadith
  {
    intent: 'ask_hadith',
    pattern: /^(?:حديث|هل\s+هذا\s+حديث|تخريج\s+حديث|قال\s+رسول\s+الله)\s*(.*)/i,
    confidence: 0.92,
  },
  // Greetings
  {
    intent: 'greet',
    pattern: /^(?:السلام\s+عليكم|مرحبا|أهلا|اهلا|صباح\s+الخير|مساء\s+الخير|hi|hello|bonjour)\b/i,
    confidence: 0.95,
  },
  // Farewells
  {
    intent: 'farewell',
    pattern: /^(?:مع\s+السلامة|إلى\s+اللقاء|الى\s+اللقاء|وداعا|باي|bye|goodbye|au\s+revoir)\b/i,
    confidence: 0.95,
  },
  // Help
  {
    intent: 'help',
    pattern: /^(?:ساعدني|مساعدة|ماذا\s+تستطيعين|كيف\s+استخدمك|دليل|help)\b/i,
    confidence: 0.95,
  },
];

/**
 * Evaluates input text against high-priority exact regex commands.
 * @param normalizedText Normalized user input
 * @returns Match result or null if no command matches
 */
export function matchRegexCommand(normalizedText: string): RegexMatchResult | null {
  const clean = normalizedText.trim();
  for (const cmd of COMMAND_PATTERNS) {
    const match = clean.match(cmd.pattern);
    if (match) {
      const extracted = cmd.extractGroupIndex ? match[cmd.extractGroupIndex]?.trim() : undefined;
      return {
        intent: cmd.intent,
        confidence: cmd.confidence,
        extractedCommand: extracted,
        matchedPattern: cmd.pattern.source,
      };
    }
  }
  return null;
}
