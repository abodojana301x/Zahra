/**
 * Zahra AI Engine — Layer 4: TextRank Extractive Summarizer
 * Graph-based sentence ranking using token similarity and PageRank power iteration.
 * Strictly non-generative: extracts existing representative sentences with 0% hallucination.
 */

import { extractSignificantWords } from '../core/tokenizer';

export interface SummaryResult {
  summary: string;
  keySentences: string[];
  compressionRatio: number;
  confidence: number;
  source: string;
}

export class SummarizeEngine {
  /**
   * Summarizes long text by ranking sentences with TextRank algorithm.
   * @param text Original article or paragraph
   * @param maxSentences Number of top sentences to return (default 3)
   */
  public summarize(text: string, maxSentences: number = 3): SummaryResult {
    if (!text || text.trim().length === 0) {
      return {
        summary: '',
        keySentences: [],
        compressionRatio: 0,
        confidence: 0,
        source: 'خوارزمية TextRank الاستخلاصية (Mihalcea & Tarau)',
      };
    }

    // Split text into sentences by punctuation (. ! ? \n ؛ .)
    const rawSentences = text
      .split(/(?<=[.!?؟\n؛])\s+/)
      .map((s) => s.trim())
      .filter((s) => s.length > 15);

    if (rawSentences.length <= maxSentences) {
      return {
        summary: text,
        keySentences: rawSentences,
        compressionRatio: 1.0,
        confidence: 0.9,
        source: 'خوارزمية TextRank الاستخلاصية (Mihalcea & Tarau)',
      };
    }

    // Build sentence token sets
    const sentenceTokens = rawSentences.map((s) => new Set(extractSignificantWords(s)));
    const n = rawSentences.length;

    // Build similarity matrix (Jaccard similarity / log length normalization)
    const weights: number[][] = Array.from({ length: n }, () => Array(n).fill(0));

    for (let i = 0; i < n; i++) {
      for (let j = i + 1; j < n; j++) {
        const tokensA = sentenceTokens[i]!;
        const tokensB = sentenceTokens[j]!;

        let overlap = 0;
        for (const token of tokensA) {
          if (tokensB.has(token)) overlap++;
        }

        if (overlap > 0) {
          // Similarity normalized by sum of log lengths
          const sim = overlap / (Math.log(Math.max(2, tokensA.size)) + Math.log(Math.max(2, tokensB.size)));
          weights[i]![j] = sim;
          weights[j]![i] = sim;
        }
      }
    }

    // PageRank power iteration
    const d = 0.85; // Damping factor
    let scores = Array(n).fill(1.0);
    const maxIterations = 30;
    const threshold = 0.0001;

    for (let iter = 0; iter < maxIterations; iter++) {
      const nextScores = Array(n).fill(1 - d);

      for (let i = 0; i < n; i++) {
        let sum = 0;
        for (let j = 0; j < n; j++) {
          if (i === j) continue;
          const w = weights[j]![i]!;
          if (w > 0) {
            const outWeight = weights[j]!.reduce((acc, curr) => acc + curr, 0);
            if (outWeight > 0) {
              sum += (w / outWeight) * scores[j]!;
            }
          }
        }
        nextScores[i] += d * sum;
      }

      // Check convergence
      let diff = 0;
      for (let i = 0; i < n; i++) {
        diff += Math.abs(nextScores[i]! - scores[i]!);
      }
      scores = nextScores;
      if (diff < threshold) break;
    }

    // Rank sentences by score
    const rankedIndices = scores
      .map((score, index) => ({ score, index }))
      .sort((a, b) => b.score - a.score)
      .slice(0, maxSentences)
      .map((item) => item.index)
      .sort((a, b) => a - b); // preserve original chronological flow

    const chosenSentences = rankedIndices.map((idx) => rawSentences[idx]!);
    const summaryText = chosenSentences.join('\n• ');
    const originalLength = text.length;
    const summaryLength = summaryText.length;

    return {
      summary: `• ${summaryText}`,
      keySentences: chosenSentences,
      compressionRatio: originalLength > 0 ? Math.round((summaryLength / originalLength) * 100) / 100 : 0,
      confidence: 0.94,
      source: 'خوارزمية TextRank الاستخلاصية (Mihalcea & Tarau)',
    };
  }
}
