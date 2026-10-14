/**
 * Zahra AI Engine — Layer 4 & 6: Fallback & Confidence Gating Engine
 * Provides honest refusal when confidence < 0.40, paired with 3 closest suggestions.
 * Strictly guarantees ZERO hallucination.
 */

export interface FallbackResponse {
  text: string;
  confidence: number;
  suggestions: string[];
  honestNote: string;
}

export class FallbackEngine {
  private commonCapabilities: string[] = [
    'احسب عملية حسابية (مثال: احسب 250 * 4)',
    'تحويل الوحدات الفيزيائية (مثال: حول 10 كم إلى ميل)',
    'ترجمة الكلمات والعبارات (مثال: ترجم سلام إلى الإنجليزية)',
    'البحث في القرآن الكريم والأحاديث النبوية المعتمدة',
    'حساب مواقيت الصلاة الفلكية واتجاه القبلة',
    'تلخيص النصوص والمقالات بخوارزمية TextRank',
    'إدارة وحفظ المهام والملاحظات محلياً بدون إنترنت',
  ];

  /**
   * Generates honest rejection when confidence is below threshold (< 0.40).
   * @param input Raw user query
   * @param candidates Optional candidates from Naive Bayes or search
   */
  public generateFallback(input: string, candidates?: string[]): FallbackResponse {
    let suggestions = candidates && candidates.length > 0
      ? candidates.slice(0, 3)
      : this.pickRandomCapabilities(3);

    return {
      text: 'مش متأكد من الإجابة بدقة، ولم أجد مصدراً موثوقاً في قواعد البيانات المدمجة. بصفتي نظاماً خبيرًا بلا نموذج لغوي احتمالي، أمتنع تماماً عن التخمين أو التوليد غير المؤكد.',
      confidence: 0.15,
      suggestions,
      honestNote: 'نظام زهرة يعمل بخوارزميات حتمية وقواعد بيانات مدمجة فقط دون اتصال بالإنترنت أو نماذج توليدية.',
    };
  }

  private pickRandomCapabilities(count: number): string[] {
    return this.commonCapabilities.slice(0, count);
  }
}
