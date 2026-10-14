/**
 * Zahra AI Engine — Layer 4: Deterministic Math & Symbolic Unit Engine
 * Evaluates arithmetic, percentages, roots, powers, and unit conversions without eval().
 */

export interface MathResponse {
  result: number | string;
  formattedText: string;
  source: string;
  confidence: number;
}

/**
 * Tokenizes and evaluates arithmetic mathematical expressions deterministically.
 * Supports +, -, *, /, ^, %, sqrt, brackets.
 */
export function evaluateMath(expression: string): MathResponse {
  try {
    let clean = expression
      .replace(/×/g, '*')
      .replace(/÷/g, '/')
      .replace(/جذر\s*(\d+(?:\.\d+)?)/g, 'sqrt($1)')
      .replace(/(\d+)%/g, '($1/100)')
      .trim();

    // Check for square root
    const sqrtMatch = clean.match(/sqrt\((\d+(?:\.\d+)?)\)/);
    if (sqrtMatch) {
      const val = parseFloat(sqrtMatch[1]!);
      const res = Math.sqrt(val);
      return {
        result: res,
        formattedText: `الناتج هو: √${val} = ${res}`,
        source: 'محرك الحساب الرياضي الدقيق (Zahra Math Engine)',
        confidence: 1.0,
      };
    }

    // Safe parser: shunting-yard / standard arithmetic tokens
    const tokens = clean.match(/(\d+(?:\.\d+)?|[+\-*/^()])/g);
    if (!tokens || tokens.length === 0) {
      return {
        result: 'خطأ',
        formattedText: 'تعذر استخراج عملية حسابية صالحة.',
        source: 'Zahra Math Engine',
        confidence: 0.3,
      };
    }

    // Evaluate simple expressions safely via binary operator parsing
    const res = safeCompute(tokens);
    return {
      result: res,
      formattedText: `ناتج العملية: ${expression} = ${res}`,
      source: 'محرك الحساب الرياضي الدقيق (Zahra Math Engine)',
      confidence: 1.0,
    };
  } catch (err) {
    return {
      result: 'خطأ في الحساب',
      formattedText: 'حدث خطأ أثناء إجراء العملية الحسابية.',
      source: 'Zahra Math Engine',
      confidence: 0.4,
    };
  }
}

function safeCompute(tokens: string[]): number {
  const outputQueue: string[] = [];
  const opStack: string[] = [];
  const precedence: Record<string, number> = { '+': 1, '-': 1, '*': 2, '/': 2, '^': 3 };

  for (const token of tokens) {
    if (!isNaN(parseFloat(token))) {
      outputQueue.push(token);
    } else if (token === '(') {
      opStack.push(token);
    } else if (token === ')') {
      while (opStack.length > 0 && opStack[opStack.length - 1] !== '(') {
        outputQueue.push(opStack.pop()!);
      }
      opStack.pop();
    } else if (precedence[token]) {
      while (
        opStack.length > 0 &&
        opStack[opStack.length - 1] !== '(' &&
        precedence[opStack[opStack.length - 1]!]! >= precedence[token]!
      ) {
        outputQueue.push(opStack.pop()!);
      }
      opStack.push(token);
    }
  }

  while (opStack.length > 0) {
    outputQueue.push(opStack.pop()!);
  }

  const evalStack: number[] = [];
  for (const item of outputQueue) {
    if (!isNaN(parseFloat(item))) {
      evalStack.push(parseFloat(item));
    } else {
      const b = evalStack.pop() ?? 0;
      const a = evalStack.pop() ?? 0;
      switch (item) {
        case '+': evalStack.push(a + b); break;
        case '-': evalStack.push(a - b); break;
        case '*': evalStack.push(a * b); break;
        case '/': evalStack.push(b !== 0 ? a / b : 0); break;
        case '^': evalStack.push(Math.pow(a, b)); break;
      }
    }
  }

  return evalStack.pop() ?? 0;
}

/**
 * Converts units deterministically.
 */
export function convertPhysicalUnit(val: number, from: string, to: string): MathResponse {
  let result = 0;
  let formatted = '';

  if (from === 'kg' && to === 'lb') {
    result = val * 2.20462;
    formatted = `${val} كيلوغرام = ${result.toFixed(2)} رطل (باوند)`;
  } else if (from === 'lb' && to === 'kg') {
    result = val / 2.20462;
    formatted = `${val} رطل = ${result.toFixed(2)} كجم`;
  } else if (from === 'km' && to === 'mi') {
    result = val * 0.621371;
    formatted = `${val} كم = ${result.toFixed(2)} ميل`;
  } else if (from === 'mi' && to === 'km') {
    result = val / 0.621371;
    formatted = `${val} ميل = ${result.toFixed(2)} كم`;
  } else if (from === 'C' && to === 'F') {
    result = (val * 9) / 5 + 32;
    formatted = `${val} °C = ${result.toFixed(1)} °F`;
  } else if (from === 'F' && to === 'C') {
    result = ((val - 32) * 5) / 9;
    formatted = `${val} °F = ${result.toFixed(1)} °C`;
  } else {
    return {
      result: 'غير معروف',
      formattedText: `التحويل بين ${from} و ${to} غير مدعوم حالياً.`,
      source: 'محول الوحدات الفيزيائية',
      confidence: 0.3,
    };
  }

  return {
    result,
    formattedText: formatted,
    source: 'المكتب الدولي للأوزان والمقاييس (BIPM)',
    confidence: 1.0,
  };
}
