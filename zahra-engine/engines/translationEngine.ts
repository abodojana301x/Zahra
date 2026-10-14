/**
 * Zahra AI Engine — Layer 4: Offline Translation Engine
 * Bilingual glossaries (AR <-> EN <-> FR) + phrase table + morphological light stemmer.
 */

export interface TranslationEntry {
  ar: string;
  en: string;
  fr: string;
  pos?: string;
}

export interface TranslationResult {
  translatedText: string;
  sourceLang: string;
  targetLang: string;
  confidence: number;
  source: string;
  breakdown?: Array<{ original: string; translated: string; pos?: string }>;
}

export class TranslationEngine {
  private dictionary: TranslationEntry[] = [];
  private phrases: Array<{ ar: string; en: string; fr: string }> = [];

  constructor(data?: { dictionary: TranslationEntry[]; phrases: Array<{ ar: string; en: string; fr: string }> }) {
    if (data) {
      this.dictionary = data.dictionary;
      this.phrases = data.phrases;
    }
  }

  /**
   * Translates text offline between AR, EN, and FR.
   */
  public translate(text: string, targetLang: 'ar' | 'en' | 'fr' = 'en'): TranslationResult {
    const clean = text.trim();
    if (!clean) {
      return {
        translatedText: '',
        sourceLang: 'unknown',
        targetLang,
        confidence: 0,
        source: 'قاموس زهرة اللغوي المدمج',
      };
    }

    // 1. Check exact phrase table
    for (const phrase of this.phrases) {
      if (phrase.ar.includes(clean) || phrase.en.toLowerCase() === clean.toLowerCase() || phrase.fr.toLowerCase() === clean.toLowerCase()) {
        const out = targetLang === 'ar' ? phrase.ar : targetLang === 'fr' ? phrase.fr : phrase.en;
        return {
          translatedText: out,
          sourceLang: targetLang === 'ar' ? 'en' : 'ar',
          targetLang,
          confidence: 0.98,
          source: 'جدول العبارات الاصطلاحية المدمج (Phrase Table)',
        };
      }
    }

    // 2. Word-by-word with light morphological stemming
    const words = clean.split(/\s+/);
    const translatedWords: string[] = [];
    const breakdown: Array<{ original: string; translated: string; pos?: string }> = [];

    for (const w of words) {
      const match = this.lookupWord(w, targetLang);
      if (match) {
        translatedWords.push(match.translated);
        breakdown.push({ original: w, translated: match.translated, pos: match.pos });
      } else {
        translatedWords.push(`[${w}]`);
        breakdown.push({ original: w, translated: w, pos: 'unknown' });
      }
    }

    const matchedCount = breakdown.filter((b) => b.pos !== 'unknown').length;
    const confidence = words.length > 0 ? (matchedCount / words.length) * 0.85 : 0.2;

    return {
      translatedText: translatedWords.join(' '),
      sourceLang: 'auto',
      targetLang,
      confidence: Math.round(confidence * 100) / 100,
      source: 'معجم زهرة الثلاثي المدمج (عربي - إنجليزي - فرنسي)',
      breakdown,
    };
  }

  private lookupWord(word: string, targetLang: 'ar' | 'en' | 'fr'): { translated: string; pos?: string } | null {
    const wLower = word.toLowerCase();

    // Direct match
    for (const entry of this.dictionary) {
      if (entry.ar === word || entry.en.toLowerCase() === wLower || entry.fr.toLowerCase() === wLower) {
        return {
          translated: targetLang === 'ar' ? entry.ar : targetLang === 'fr' ? entry.fr : entry.en,
          pos: entry.pos,
        };
      }
    }

    // Light Arabic stemming (strip prefixes: ال، و، ف، ب)
    if (/^[وفب]?ال/.test(word)) {
      const stripped = word.replace(/^[وفب]?ال/, '');
      for (const entry of this.dictionary) {
        if (entry.ar === stripped) {
          const trans = targetLang === 'ar' ? entry.ar : targetLang === 'fr' ? entry.fr : entry.en;
          return { translated: targetLang === 'en' ? `the ${trans}` : trans, pos: entry.pos };
        }
      }
    }

    return null;
  }
}
