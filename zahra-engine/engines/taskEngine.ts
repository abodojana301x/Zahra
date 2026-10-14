/**
 * Zahra AI Engine — Layer 4: Task Engine
 * CRUD task manager with TF-IDF auto-categorization and heuristic priority scoring.
 */

import { parseDateExpression } from '../entities/dateParser';
import { extractSignificantWords } from '../core/tokenizer';

export interface ZahraTask {
  id: string;
  title: string;
  category: 'عمل' | 'شخصي' | 'عاجل' | 'مالي' | 'صحي' | 'عام';
  priorityScore: number; // 1 to 5
  dueDate?: string;
  completed: boolean;
  createdAt: string;
}

const CATEGORY_PROFILES: Record<string, string[]> = {
  'عمل': ['اجتماع', 'تقرير', 'مشروع', 'مستند', 'عميل', 'إيميل', 'كود', 'برمجة', 'تسليم', 'work', 'project', 'meeting'],
  'شخصي': ['بيت', 'عائلة', 'شراء', 'سوق', 'أغراض', 'زيارة', 'صديق', 'home', 'family', 'shopping'],
  'عاجل': ['ضروري', 'هام', 'اليوم', 'الآن', 'فوري', 'عاجل', 'طارئ', 'urgent', 'asap', 'now'],
  'مالي': ['فاتورة', 'بنك', 'حساب', 'راتب', 'دفع', 'قسط', 'سداد', 'فلوس', 'bank', 'invoice', 'salary'],
  'صحي': ['طبيب', 'دواء', 'موعد', 'رياضة', 'جيم', 'فحص', 'صحة', 'عيادة', 'doctor', 'medicine', 'gym'],
};

export class TaskEngine {
  private tasks: ZahraTask[] = [];

  /**
   * Creates a new task with automated entity extraction, category detection, and priority calculation.
   */
  public createTask(rawInput: string): ZahraTask {
    const parsedDate = parseDateExpression(rawInput);
    const category = this.categorize(rawInput);
    const priority = this.calculatePriority(rawInput, category, Boolean(parsedDate));

    // Strip command words to clean title
    let cleanTitle = rawInput
      .replace(/^(?:أضف\s+مهمة|انشئ\s+مهمة|سجل\s+مهمة|مهمة\s+جديدة|تاسك:?)\s*/i, '')
      .trim();

    if (!cleanTitle) cleanTitle = rawInput;

    const newTask: ZahraTask = {
      id: `task_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      title: cleanTitle,
      category,
      priorityScore: priority,
      dueDate: parsedDate ? parsedDate.date.toISOString().split('T')[0] : undefined,
      completed: false,
      createdAt: new Date().toISOString(),
    };

    this.tasks.push(newTask);
    return newTask;
  }

  public listTasks(onlyPending: boolean = false): ZahraTask[] {
    return onlyPending ? this.tasks.filter((t) => !t.completed) : this.tasks;
  }

  public toggleTask(taskId: string): boolean {
    const t = this.tasks.find((item) => item.id === taskId);
    if (t) {
      t.completed = !t.completed;
      return true;
    }
    return false;
  }

  public deleteTask(taskId: string): boolean {
    const idx = this.tasks.findIndex((item) => item.id === taskId);
    if (idx !== -1) {
      this.tasks.splice(idx, 1);
      return true;
    }
    return false;
  }

  /**
   * Auto-categorizes text using token overlap against profile dictionary.
   */
  private categorize(text: string): ZahraTask['category'] {
    const words = extractSignificantWords(text);
    let bestCategory: ZahraTask['category'] = 'عام';
    let maxOverlap = 0;

    for (const [cat, keywords] of Object.entries(CATEGORY_PROFILES)) {
      let overlap = 0;
      for (const w of words) {
        if (keywords.some((k) => k.includes(w) || w.includes(k))) {
          overlap++;
        }
      }
      if (overlap > maxOverlap) {
        maxOverlap = overlap;
        bestCategory = cat as ZahraTask['category'];
      }
    }

    return bestCategory;
  }

  private calculatePriority(text: string, category: string, hasDueDate: boolean): number {
    let score = 2; // Default normal
    if (category === 'عاجل' || /(?:عاجل|ضروري|فوري|اليوم|urgent)/i.test(text)) {
      score += 2;
    }
    if (hasDueDate) {
      score += 1;
    }
    return Math.min(5, Math.max(1, score));
  }
}
