/**
 * Zahra AI Engine — Layer 5: Offline Search Engine
 * Fuzzy string search (Levenshtein distance), Inverted Index, and Cosine Similarity over TF-IDF.
 */

import { extractSignificantWords } from '../core/tokenizer';

export interface SearchHit<T> {
  item: T;
  score: number;
  matchType: 'exact' | 'fuzzy' | 'tfidf';
}

/**
 * Computes Levenshtein edit distance between two strings.
 */
export function levenshteinDistance(a: string, b: string): number {
  const m = a.length;
  const n = b.length;
  const dp: number[][] = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));

  for (let i = 0; i <= m; i++) dp[i]![0] = i;
  for (let j = 0; j <= n; j++) dp[0]![j] = j;

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      dp[i]![j] = Math.min(
        dp[i - 1]![j]! + 1, // deletion
        dp[i]![j - 1]! + 1, // insertion
        dp[i - 1]![j - 1]! + cost // substitution
      );
    }
  }

  return dp[m]![n]!;
}

/**
 * Calculates normalized similarity ratio (0 to 1).
 */
export function similarityRatio(a: string, b: string): number {
  const maxLen = Math.max(a.length, b.length);
  if (maxLen === 0) return 1.0;
  const dist = levenshteinDistance(a, b);
  return 1.0 - dist / maxLen;
}

/**
 * Computes Cosine Similarity between two term-frequency vector maps.
 */
export function cosineSimilarity(vecA: Map<string, number>, vecB: Map<string, number>): number {
  let dotProduct = 0;
  let normA = 0;
  let normB = 0;

  for (const [term, countA] of vecA.entries()) {
    normA += countA * countA;
    const countB = vecB.get(term) ?? 0;
    dotProduct += countA * countB;
  }

  for (const countB of vecB.values()) {
    normB += countB * countB;
  }

  if (normA === 0 || normB === 0) return 0;
  return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}

/**
 * Performs fuzzy search on a list of items.
 */
export function fuzzySearch<T>(
  items: T[],
  query: string,
  keyExtractor: (item: T) => string,
  threshold: number = 0.6
): Array<SearchHit<T>> {
  const qClean = query.toLowerCase().trim();
  const hits: Array<SearchHit<T>> = [];

  for (const item of items) {
    const text = keyExtractor(item).toLowerCase();
    
    // 1. Exact substring match
    if (text.includes(qClean)) {
      hits.push({ item, score: 1.0, matchType: 'exact' });
      continue;
    }

    // 2. Fuzzy match
    const ratio = similarityRatio(qClean, text);
    if (ratio >= threshold) {
      hits.push({ item, score: ratio, matchType: 'fuzzy' });
    }
  }

  return hits.sort((a, b) => b.score - a.score);
}
