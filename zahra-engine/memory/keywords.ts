/**
 * Zahra AI Engine — Layer 5: RAKE (Rapid Automatic Keyword Extraction)
 * Extracts key domain phrases and tags from text without external models.
 */

import { extractSignificantWords, tokenize } from '../core/tokenizer';

export interface ExtractedKeyword {
  phrase: string;
  score: number;
}

/**
 * Extracts key phrases using RAKE scoring.
 */
export function extractKeywords(text: string, maxPhrases: number = 5): ExtractedKeyword[] {
  if (!text || text.trim().length === 0) return [];

  // 1. Split text into candidate phrases by sentence delimiters and stopwords
  const candidatePhrases = text
    .split(/[,.؛!؟\n\t]+/)
    .map((p) => p.trim())
    .filter((p) => p.length > 2);

  const wordFrequency = new Map<string, number>();
  const wordDegree = new Map<string, number>();

  for (const phrase of candidatePhrases) {
    const words = extractSignificantWords(phrase);
    const degree = words.length;

    for (const word of words) {
      wordFrequency.set(word, (wordFrequency.get(word) ?? 0) + 1);
      wordDegree.set(word, (wordDegree.get(word) ?? 0) + degree);
    }
  }

  // Calculate word score: degree(w) / frequency(w)
  const wordScores = new Map<string, number>();
  for (const [w, freq] of wordFrequency.entries()) {
    const deg = wordDegree.get(w) ?? freq;
    wordScores.set(w, deg / freq);
  }

  // Score candidate phrases
  const phraseScores = new Map<string, number>();
  for (const phrase of candidatePhrases) {
    const words = extractSignificantWords(phrase);
    if (words.length === 0) continue;

    let score = 0;
    for (const w of words) {
      score += wordScores.get(w) ?? 1;
    }
    phraseScores.set(phrase, score);
  }

  // Sort and return top candidates
  const results: ExtractedKeyword[] = [];
  for (const [phrase, score] of phraseScores.entries()) {
    results.push({ phrase, score });
  }

  return results.sort((a, b) => b.score - a.score).slice(0, maxPhrases);
}
