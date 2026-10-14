/**
 * Zahra AI Engine — Layer 1: Unicode & Arabic Text Normalizer
 * Normalizes input text by removing diacritics, folding Arabic letter variations,
 * unifying Arabic-Indic digits, and sanitizing punctuation without external libraries.
 */

export interface NormalizerOptions {
  stripDiacritics?: boolean;
  unifyAlef?: boolean;
  unifyYaa?: boolean;
  unifyTaaMarbuta?: boolean;
  unifyDigits?: boolean;
}

const DEFAULT_OPTIONS: Required<NormalizerOptions> = {
  stripDiacritics: true,
  unifyAlef: true,
  unifyYaa: true,
  unifyTaaMarbuta: true,
  unifyDigits: true,
};

// Arabic Tashkeel (harakat & tatweel) regex
const TASHKEEL_REGEX = /[\u064B-\u065F\u0670\u0640]/g;

// Arabic-Indic to Latin digits map
const ARABIC_INDIC_DIGITS: Record<string, string> = {
  '٠': '0', '١': '1', '٢': '2', '٣': '3', '٤': '4',
  '٥': '5', '٦': '6', '٧': '7', '٨': '8', '٩': '9',
  '۰': '0', '۱': '1', '۲': '2', '۳': '3', '۴': '4',
  '۵': '5', '۶': '6', '۷': '7', '۸': '8', '۹': '9',
};

/**
 * Normalizes a raw string according to Arabic morphological and Unicode folding rules.
 * @param input Raw user input text
 * @param options Custom normalization switches
 * @returns Clean, normalized canonical string
 */
export function normalizeText(input: string, options?: NormalizerOptions): string {
  if (!input) return '';
  const opts = { ...DEFAULT_OPTIONS, ...options };
  let text = input.trim();

  // Strip diacritics & tatweel (kashida)
  if (opts.stripDiacritics) {
    text = text.replace(TASHKEEL_REGEX, '');
  }

  // Unify Alef variants: أ, إ, آ, ٱ -> ا
  if (opts.unifyAlef) {
    text = text.replace(/[أإآٱ]/g, 'ا');
  }

  // Unify Yaa variants: ى, ئ -> ي
  if (opts.unifyYaa) {
    text = text.replace(/[ىئ]/g, 'ي');
  }

  // Unify Taa Marbuta: ة -> ه
  if (opts.unifyTaaMarbuta) {
    text = text.replace(/ة/g, 'ه');
  }

  // Unify Arabic-Indic digits: ٠-٩ -> 0-9
  if (opts.unifyDigits) {
    text = text.replace(/[٠-٩۰-۹]/g, (char) => ARABIC_INDIC_DIGITS[char] ?? char);
  }

  // Normalize multi-whitespace to single space
  text = text.replace(/\s+/g, ' ');

  return text;
}
