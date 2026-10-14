/**
 * Zahra AI Engine — Layer 1: Unicode Tokenizer
 * Breaks sentences and words into tokens with locale boundary segmenting and stopword filtering.
 */

export interface Token {
  text: string;
  index: number;
  isWord: boolean;
  length: number;
}

const ARABIC_STOPWORDS = new Set([
  'في', 'من', 'على', 'إلى', 'عن', 'مع', 'هذا', 'هذه', 'ذلك', 'تلك', 'التي', 'الذي',
  'هو', 'هي', 'هم', 'هن', 'نحن', 'أنا', 'أنت', 'كان', 'كانت', 'يكون', 'أن', 'إن',
  'ما', 'لا', 'لم', 'لن', 'هل', 'قد', 'ثم', 'أو', 'بل', 'لكن', 'كل', 'بعض'
]);

const ENGLISH_STOPWORDS = new Set([
  'the', 'is', 'at', 'which', 'on', 'in', 'a', 'an', 'and', 'or', 'for', 'to',
  'with', 'it', 'this', 'that', 'from', 'by', 'as', 'be', 'are', 'was', 'were'
]);

/**
 * Tokenizes text into words and punctuation using Intl.Segmenter or regex fallback.
 * @param text Clean input text
 * @param locale Optional locale hint (e.g., 'ar', 'en')
 * @returns Array of structured tokens
 */
export function tokenize(text: string, locale: string = 'ar'): Token[] {
  if (!text) return [];

  if (typeof Intl !== 'undefined' && 'Segmenter' in Intl) {
    // Standard Intl.Segmenter
    const segmenter = new (Intl as any).Segmenter(locale, { granularity: 'word' });
    const segments = Array.from(segmenter.segment(text)) as Array<{ segment: string; index: number; isWordLike?: boolean }>;
    return segments.map((s) => ({
      text: s.segment,
      index: s.index,
      isWord: Boolean(s.isWordLike),
      length: s.segment.length,
    }));
  }

  // Regex fallback
  const regex = /([\p{L}\p{N}]+|[^\s\p{L}\p{N}]+)/gu;
  const tokens: Token[] = [];
  let match: RegExpExecArray | null;
  while ((match = regex.exec(text)) !== null) {
    const raw = match[0];
    tokens.push({
      text: raw,
      index: match.index,
      isWord: /^[\p{L}\p{N}]+$/u.test(raw),
      length: raw.length,
    });
  }
  return tokens;
}

/**
 * Extracts normalized word tokens and filters out common stopwords.
 * @param text Clean text
 * @param lang Language identifier ('ar' or 'en')
 * @returns Filtered array of word strings
 */
export function extractSignificantWords(text: string, lang: 'ar' | 'en' | string = 'ar'): string[] {
  const tokens = tokenize(text, lang);
  const stopSet = lang === 'en' ? ENGLISH_STOPWORDS : ARABIC_STOPWORDS;

  return tokens
    .filter((t) => t.isWord && t.text.length > 1)
    .map((t) => t.text.toLowerCase())
    .filter((word) => !stopSet.has(word));
}
