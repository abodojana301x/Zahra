/**
 * Zahra AI Engine — Layer 3: Number & Ordinal Parser
 * Converts Arabic-Indic and Latin digits, written Arabic words (واحد، اثنان، مئة، ألف) into numbers.
 */

const ARABIC_WORD_NUMBERS: Record<string, number> = {
  'صفر': 0, 'واحد': 1, 'واحدة': 1, 'اثنين': 2, 'اثنان': 2, 'ثلاثة': 3, 'اربعة': 4, 'أربعة': 4,
  'خمسة': 5, 'ستة': 6, 'سبعة': 7, 'ثمانية': 8, 'تسعة': 9, 'عشرة': 10,
  'مئة': 100, 'مائة': 100, 'مئتان': 200, 'الف': 1000, 'ألف': 1000, 'الفين': 2000, 'مليون': 1000000,
  'اول': 1, 'أول': 1, 'ثاني': 2, 'ثالث': 3, 'رابع': 4, 'خامس': 5,
};

/**
 * Extracts all numbers from text (both digits and Arabic word numbers).
 * @param text Input text
 * @returns Array of parsed numbers
 */
export function extractNumbers(text: string): number[] {
  const numbers: number[] = [];
  
  // Standard digits (positive, negative, decimal)
  const digitMatches = text.match(/-?\d+(?:\.\d+)?/g);
  if (digitMatches) {
    for (const d of digitMatches) {
      numbers.push(parseFloat(d));
    }
  }

  // Word-based numbers
  const tokens = text.split(/\s+/);
  for (const token of tokens) {
    const clean = token.replace(/[^\u0621-\u064A]/g, '');
    if (ARABIC_WORD_NUMBERS[clean] !== undefined) {
      numbers.push(ARABIC_WORD_NUMBERS[clean]!);
    }
  }

  return numbers;
}
