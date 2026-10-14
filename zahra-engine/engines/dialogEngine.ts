/**
 * Zahra AI Engine — Layer 4: Finite State Machine Dialog Engine
 * Manages multi-turn conversation states (e.g. creating tasks step-by-step, clarifying conversions).
 */

export type DialogState = 'IDLE' | 'AWAITING_TASK_DEADLINE' | 'AWAITING_TRANSLATION_TARGET' | 'AWAITING_CONFIRMATION';

export interface DialogContext {
  state: DialogState;
  pendingIntent?: string;
  pendingData?: Record<string, any>;
  turnsInCurrentState: number;
}

export interface DialogTransitionResult {
  handled: boolean;
  replyText?: string;
  nextState: DialogState;
  finalPayload?: any;
}

export class DialogEngine {
  private context: DialogContext = {
    state: 'IDLE',
    turnsInCurrentState: 0,
  };

  public getContext(): DialogContext {
    return { ...this.context };
  }

  public reset(): void {
    this.context = { state: 'IDLE', turnsInCurrentState: 0 };
  }

  /**
   * Evaluates turn against current FSM state.
   */
  public handleTurn(input: string): DialogTransitionResult {
    const clean = input.trim().toLowerCase();

    switch (this.context.state) {
      case 'AWAITING_TASK_DEADLINE': {
        this.context.state = 'IDLE';
        const taskTitle = this.context.pendingData?.title ?? 'مهمة';
        return {
          handled: true,
          replyText: `تم تحديد موعد للمهمة "${taskTitle}": ${clean}. تم حفظها في جدول مهامك.`,
          nextState: 'IDLE',
          finalPayload: { action: 'TASK_SAVED', deadline: clean },
        };
      }

      case 'AWAITING_TRANSLATION_TARGET': {
        this.context.state = 'IDLE';
        const word = this.context.pendingData?.word ?? '';
        return {
          handled: true,
          replyText: `جاري ترجمة "${word}" إلى ${clean}...`,
          nextState: 'IDLE',
          finalPayload: { action: 'TRIGGER_TRANSLATE', targetLang: clean },
        };
      }

      case 'AWAITING_CONFIRMATION': {
        const isYes = /(?:نعم|أجل|تمام|موافق|yes|ok)/i.test(clean);
        this.context.state = 'IDLE';
        return {
          handled: true,
          replyText: isYes ? 'تم تأكيد الإجراء بنجاح.' : 'تم إلغاء الإجراء بناءً على طلبك.',
          nextState: 'IDLE',
          finalPayload: { confirmed: isYes },
        };
      }

      case 'IDLE':
      default:
        return { handled: false, nextState: 'IDLE' };
    }
  }

  public triggerState(state: DialogState, data?: Record<string, any>): void {
    this.context.state = state;
    this.context.pendingData = data;
    this.context.turnsInCurrentState = 0;
  }
}
