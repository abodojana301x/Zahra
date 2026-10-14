/**
 * Zahra AI Engine — Layer 1: Language & Script Detector
 * Identifies Arabic, English, French, or mixed scripts using character frequency and n-gram profiles.
 */

export type DetectedLanguage = 'ar' | 'en' | 'fr' | 'mixed' | 'unknown';

export interface LanguageDetectionResult {
  language: DetectedLanguage;
  script: 'Arabic' | 'Latin' | 'Mixed' | 'NumericOrSymbol';
  confidence: number;
}

const ARABIC_RANGE_REGEX = /[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]/g;
const LATIN_RANGE_REGEX = /[a-zA-Z\u00C0-\u00FF]/g;
const FRENCH_ACCENT_REGEX = /[éèêëàâçîïôûùüœæ]/gi;

/**
 * Detects the dominant language and script of a text offline.
 * @param text Normalized or raw input text
 * @returns Detection result with language code, script type, and confidence score
 */
export function detectLanguage(text: string): LanguageDetectionResult {
  if (!text || text.trim().length === 0) {
    return { language: 'unknown', script: 'NumericOrSymbol', confidence: 0 };
  }

  const clean = text.trim();
  const arabicMatches = clean.match(ARABIC_RANGE_REGEX) ?? [];
  const latinMatches = clean.match(LATIN_RANGE_REGEX) ?? [];
  const totalLetters = arabicMatches.length + latinMatches.length;

  if (totalLetters === 0) {
    return { language: 'unknown', script: 'NumericOrSymbol', confidence: 0.5 };
  }

  const arRatio = arabicMatches.length / totalLetters;
  const latinRatio = latinMatches.length / totalLetters;

  if (arRatio > 0.75) {
    return { language: 'ar', script: 'Arabic', confidence: Math.min(1.0, 0.7 + arRatio * 0.3) };
  }

  if (latinRatio > 0.75) {
    const hasFrenchDiacritics = FRENCH_ACCENT_REGEX.test(clean);
    const frenchStopwords = /\b(le|la|les|un|une|des|du|de|et|est|en|que|qui|dans)\b/i;
    const isFrench = hasFrenchDiacritics || frenchStopwords.test(clean);

    return {
      language: isFrench ? 'fr' : 'en',
      script: 'Latin',
      confidence: Math.min(1.0, 0.7 + latinRatio * 0.3),
    };
  }

  return { language: 'mixed', script: 'Mixed', confidence: 0.6 };
}
