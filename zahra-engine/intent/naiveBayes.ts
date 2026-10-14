/**
 * Zahra AI Engine — Layer 2: Multinomial Naive Bayes Classifier
 * Pure statistical NLP intent classification in log-space with Laplace smoothing.
 */

import { extractSignificantWords } from '../core/tokenizer';
import { normalizeText } from '../core/normalizer';

export interface ClassificationResult {
  intent: string;
  confidence: number;
  allProbabilities: Array<{ intent: string; probability: number }>;
}

export class NaiveBayesClassifier {
  private classDocCounts: Map<string, number> = new Map();
  private classWordCounts: Map<string, Map<string, number>> = new Map();
  private classTotalWords: Map<string, number> = new Map();
  private vocabulary: Set<string> = new Set();
  private totalDocs: number = 0;

  constructor(corpus?: Record<string, string[]>) {
    if (corpus) {
      this.train(corpus);
    }
  }

  /**
   * Trains the model using a corpus of labelled phrases.
   * @param corpus Map of intent name to list of sample utterances
   */
  public train(corpus: Record<string, string[]>): void {
    for (const [intent, samples] of Object.entries(corpus)) {
      if (!this.classWordCounts.has(intent)) {
        this.classWordCounts.set(intent, new Map());
        this.classTotalWords.set(intent, 0);
        this.classDocCounts.set(intent, 0);
      }

      const wordMap = this.classWordCounts.get(intent)!;
      for (const sample of samples) {
        this.totalDocs++;
        this.classDocCounts.set(intent, (this.classDocCounts.get(intent) ?? 0) + 1);

        const normalized = normalizeText(sample);
        const words = extractSignificantWords(normalized);

        for (const w of words) {
          this.vocabulary.add(w);
          wordMap.set(w, (wordMap.get(w) ?? 0) + 1);
          this.classTotalWords.set(intent, (this.classTotalWords.get(intent) ?? 0) + 1);
        }
      }
    }
  }

  /**
   * Classifies an input text by computing log-prior + sum(log-likelihood) with Laplace smoothing.
   * @param text Raw or normalized user input
   * @returns Classification result with winning intent and calibrated confidence
   */
  public classify(text: string): ClassificationResult {
    const normalized = normalizeText(text);
    const words = extractSignificantWords(normalized);
    const vocabSize = Math.max(1, this.vocabulary.size);

    if (words.length === 0 || this.totalDocs === 0) {
      return {
        intent: 'unknown',
        confidence: 0.1,
        allProbabilities: [],
      };
    }

    const logScores: Array<{ intent: string; logScore: number }> = [];

    for (const [intent, docCount] of this.classDocCounts.entries()) {
      // Log prior P(C)
      let logScore = Math.log(docCount / this.totalDocs);
      const wordMap = this.classWordCounts.get(intent)!;
      const totalWords = this.classTotalWords.get(intent)!;

      // Log likelihood sum(log P(w | C)) with Laplace smoothing (+1 / +vocabSize)
      for (const w of words) {
        const count = wordMap.get(w) ?? 0;
        const prob = (count + 1) / (totalWords + vocabSize);
        logScore += Math.log(prob);
      }

      logScores.push({ intent, logScore });
    }

    // Sort by highest score
    logScores.sort((a, b) => b.logScore - a.logScore);

    // Convert logScores to softmax normalized probabilities for confidence gate
    const maxLog = logScores[0]?.logScore ?? 0;
    const expScores = logScores.map((s) => ({
      intent: s.intent,
      expVal: Math.exp(s.logScore - maxLog),
    }));
    const sumExp = expScores.reduce((acc, curr) => acc + curr.expVal, 0);

    const probabilities = expScores.map((s) => ({
      intent: s.intent,
      probability: s.expVal / sumExp,
    }));

    const top = probabilities[0];
    return {
      intent: top?.intent ?? 'unknown',
      confidence: top ? Math.round(top.probability * 100) / 100 : 0,
      allProbabilities: probabilities.slice(0, 5),
    };
  }
}
