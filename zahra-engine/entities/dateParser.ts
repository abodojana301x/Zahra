/**
 * Zahra AI Engine — Layer 3: Arabic & Latin Date/Time Parser
 * Parses relative expressions ("بكرة", "الأحد الجاي", "بعد أسبوع", "3 العصر") into Date objects.
 */

export interface ParsedDateTime {
  date: Date;
  rawExpression: string;
  hasTime: boolean;
  confidence: number;
}

const DAYS_OF_WEEK: Record<string, number> = {
  'الاحد': 0, 'الأحد': 0, 'sunday': 0,
  'الاثنين': 1, 'الإثنين': 1, 'monday': 1,
  'الثلاثاء': 2, 'tuesday': 2,
  'الاربعاء': 3, 'الأربعاء': 3, 'wednesday': 3,
  'الخميس': 4, 'thursday': 4,
  'الجمعة': 5, 'friday': 5,
  'السبت': 6, 'saturday': 6,
};

const ARABIC_MONTHS: Record<string, number> = {
  'يناير': 0, 'فبراير': 1, 'مارس': 2, 'ابريل': 3, 'أبريل': 3, 'مايو': 4, 'يونيو': 5,
  'يوليو': 6, 'اغسطس': 7, 'أغسطس': 7, 'سبتمبر': 8, 'اكتوبر': 9, 'أكتوبر': 9, 'نوفمبر': 10, 'ديسمبر': 11,
};

/**
 * Extracts and parses relative and absolute dates and times from text.
 * @param text Input text
 * @param referenceDate Base date (defaults to now)
 * @returns Parsed date information or null
 */
export function parseDateExpression(text: string, referenceDate: Date = new Date()): ParsedDateTime | null {
  const clean = text.toLowerCase();

  // 1. "اليوم" (today)
  if (/(?:اليوم|today)/i.test(clean)) {
    return { date: new Date(referenceDate), rawExpression: 'اليوم', hasTime: false, confidence: 0.95 };
  }

  // 2. "بكرة" / "غدا" (tomorrow)
  if (/(?:بكرة|غدا|غداً|tomorrow)/i.test(clean)) {
    const d = new Date(referenceDate);
    d.setDate(d.getDate() + 1);
    return { date: d, rawExpression: 'غداً', hasTime: false, confidence: 0.98 };
  }

  // 3. "بعد بكرة" (day after tomorrow)
  if (/(?:بعد\s+بكرة|بعد\s+غد)/i.test(clean)) {
    const d = new Date(referenceDate);
    d.setDate(d.getDate() + 2);
    return { date: d, rawExpression: 'بعد غد', hasTime: false, confidence: 0.98 };
  }

  // 4. "بعد أسبوع" / "بعد اسبوع" (after one week)
  if (/(?:بعد\s+أسبوع|بعد\s+اسبوع|after\s+a?\s*week)/i.test(clean)) {
    const d = new Date(referenceDate);
    d.setDate(d.getDate() + 7);
    return { date: d, rawExpression: 'بعد أسبوع', hasTime: false, confidence: 0.95 };
  }

  // 5. Day of week e.g. "الأحد الجاي"
  for (const [dayName, dayIndex] of Object.entries(DAYS_OF_WEEK)) {
    if (clean.includes(dayName)) {
      const d = new Date(referenceDate);
      const currentDay = d.getDay();
      let diff = dayIndex - currentDay;
      if (diff <= 0) diff += 7; // next occurrence
      d.setDate(d.getDate() + diff);
      return { date: d, rawExpression: dayName, hasTime: false, confidence: 0.92 };
    }
  }

  // 6. Absolute date e.g. "12 مارس" or "15/04/2026"
  const slashMatch = clean.match(/(\d{1,2})[\/\-](\d{1,2})(?:[\/\-](\d{2,4}))?/);
  if (slashMatch) {
    const day = parseInt(slashMatch[1]!, 10);
    const month = parseInt(slashMatch[2]!, 10) - 1;
    const year = slashMatch[3] ? parseInt(slashMatch[3], 10) : referenceDate.getFullYear();
    const d = new Date(year, month, day);
    return { date: d, rawExpression: slashMatch[0], hasTime: false, confidence: 0.95 };
  }

  for (const [monthName, monthIndex] of Object.entries(ARABIC_MONTHS)) {
    const match = clean.match(new RegExp(`(\\d{1,2})\\s+${monthName}`));
    if (match) {
      const day = parseInt(match[1]!, 10);
      const d = new Date(referenceDate.getFullYear(), monthIndex, day);
      return { date: d, rawExpression: match[0], hasTime: false, confidence: 0.95 };
    }
  }

  return null;
}
