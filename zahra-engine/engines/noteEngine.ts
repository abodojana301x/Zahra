/**
 * Zahra AI Engine — Layer 4: Note Engine
 * CRUD note manager with RAKE keyword extraction, automated tagging, and fuzzy search.
 */

import { extractSignificantWords } from '../core/tokenizer';

export interface ZahraNote {
  id: string;
  title: string;
  content: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export class NoteEngine {
  private notes: ZahraNote[] = [];

  /**
   * Creates a note and extracts top keywords as tags via RAKE algorithm.
   */
  public createNote(rawContent: string, customTitle?: string): ZahraNote {
    const cleanContent = rawContent
      .replace(/^(?:سجل\s+ملاحظة|اكتب\s+ملاحظة|احفظ\s+ملاحظة|تذكر\s+أن|note:?)\s*/i, '')
      .trim();

    const title = customTitle || cleanContent.split('\n')[0]?.substring(0, 40) || 'ملاحظة جديدة';
    const tags = this.extractRakeKeywords(cleanContent);

    const note: ZahraNote = {
      id: `note_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      title,
      content: cleanContent,
      tags,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    this.notes.push(note);
    return note;
  }

  public listNotes(query?: string): ZahraNote[] {
    if (!query) return this.notes;
    const q = query.toLowerCase();
    return this.notes.filter(
      (n) => n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q) || n.tags.some((t) => t.includes(q))
    );
  }

  public deleteNote(noteId: string): boolean {
    const idx = this.notes.findIndex((n) => n.id === noteId);
    if (idx !== -1) {
      this.notes.splice(idx, 1);
      return true;
    }
    return false;
  }

  /**
   * RAKE (Rapid Automatic Keyword Extraction) simplified algorithm.
   */
  public extractRakeKeywords(text: string, maxKeywords: number = 4): string[] {
    const words = extractSignificantWords(text);
    if (words.length === 0) return ['عام'];

    const wordFreq = new Map<string, number>();
    const wordDegree = new Map<string, number>();

    for (const w of words) {
      wordFreq.set(w, (wordFreq.get(w) ?? 0) + 1);
      wordDegree.set(w, (wordDegree.get(w) ?? 0) + words.length);
    }

    const scores: Array<{ word: string; score: number }> = [];
    for (const [w, freq] of wordFreq.entries()) {
      const degree = wordDegree.get(w) ?? freq;
      scores.push({ word: w, score: degree / freq });
    }

    scores.sort((a, b) => b.score - a.score);
    return scores.slice(0, maxKeywords).map((s) => s.word);
  }
}
