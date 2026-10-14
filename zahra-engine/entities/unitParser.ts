/**
 * Zahra AI Engine — Layer 3: Unit & Currency Parser
 * Handles physical conversions (metric/imperial, temperature, volume) and currency tags.
 */

export interface ParsedUnit {
  value: number;
  fromUnit: string;
  toUnit?: string;
  category: 'length' | 'mass' | 'temperature' | 'volume' | 'currency' | 'unknown';
}

const UNIT_ALIASES: Record<string, { canonical: string; category: ParsedUnit['category'] }> = {
  // Length
  'كيلومتر': { canonical: 'km', category: 'length' },
  'كم': { canonical: 'km', category: 'length' },
  'km': { canonical: 'km', category: 'length' },
  'متر': { canonical: 'm', category: 'length' },
  'm': { canonical: 'm', category: 'length' },
  'ميل': { canonical: 'mi', category: 'length' },
  'mi': { canonical: 'mi', category: 'length' },
  'قدم': { canonical: 'ft', category: 'length' },
  'ft': { canonical: 'ft', category: 'length' },
  // Mass
  'كيلو': { canonical: 'kg', category: 'mass' },
  'كيلوغرام': { canonical: 'kg', category: 'mass' },
  'كجم': { canonical: 'kg', category: 'mass' },
  'kg': { canonical: 'kg', category: 'mass' },
  'باوند': { canonical: 'lb', category: 'mass' },
  'رطل': { canonical: 'lb', category: 'mass' },
  'lb': { canonical: 'lb', category: 'mass' },
  'جرام': { canonical: 'g', category: 'mass' },
  'غرام': { canonical: 'g', category: 'mass' },
  'g': { canonical: 'g', category: 'mass' },
  // Temperature
  'مئوية': { canonical: 'C', category: 'temperature' },
  'سيليزيوس': { canonical: 'C', category: 'temperature' },
  'c': { canonical: 'C', category: 'temperature' },
  'فهرنهايت': { canonical: 'F', category: 'temperature' },
  'f': { canonical: 'F', category: 'temperature' },
  // Volume
  'لتر': { canonical: 'L', category: 'volume' },
  'l': { canonical: 'L', category: 'volume' },
  'جالون': { canonical: 'gal', category: 'volume' },
  'gal': { canonical: 'gal', category: 'volume' },
  // Currency
  'دولار': { canonical: 'USD', category: 'currency' },
  'usd': { canonical: 'USD', category: 'currency' },
  'ريال': { canonical: 'SAR', category: 'currency' },
  'sar': { canonical: 'SAR', category: 'currency' },
  'جنيه': { canonical: 'EGP', category: 'currency' },
  'egp': { canonical: 'EGP', category: 'currency' },
  'درهم': { canonical: 'AED', category: 'currency' },
  'aed': { canonical: 'AED', category: 'currency' },
  'دينار': { canonical: 'KWD', category: 'currency' },
  'kwd': { canonical: 'KWD', category: 'currency' },
  'يورو': { canonical: 'EUR', category: 'currency' },
  'eur': { canonical: 'EUR', category: 'currency' },
};

/**
 * Extracts unit conversion requests from a sentence.
 * e.g., "حول 5 كيلو إلى باوند" or "convert 10 km to mi"
 * @param text Input text
 * @returns Parsed unit details or null
 */
export function parseUnitRequest(text: string): ParsedUnit | null {
  const match = text.match(/(?:حول|تحويل|convert)?\s*(\d+(?:\.\d+)?)\s*([a-zA-Z\u0621-\u064A]+)(?:\s*(?:إلى|الى|to)\s*([a-zA-Z\u0621-\u064A]+))?/i);
  if (!match) return null;

  const val = parseFloat(match[1]!);
  const rawFrom = match[2]!.toLowerCase();
  const rawTo = match[3] ? match[3].toLowerCase() : undefined;

  const fromInfo = UNIT_ALIASES[rawFrom];
  if (!fromInfo) return null;

  const toInfo = rawTo ? UNIT_ALIASES[rawTo] : undefined;

  return {
    value: val,
    fromUnit: fromInfo.canonical,
    toUnit: toInfo ? toInfo.canonical : undefined,
    category: fromInfo.category,
  };
}
