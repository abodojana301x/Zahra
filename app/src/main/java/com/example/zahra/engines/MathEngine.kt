package com.example.zahra.engines

import com.example.zahra.model.OutcomeState
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class MathResult(
  val formattedText: String,
  val source: String,
  val confidence: Double,
  val outcomeState: OutcomeState = OutcomeState.SUPPORTED,
  val isSuccess: Boolean = true,
  val errorMessage: String? = null,
  val numericResult: Double? = null
)

object MathEngine {

  private val DECIMAL_FORMAT = DecimalFormat("#.######", DecimalFormatSymbols(Locale.US)).apply {
    isGroupingUsed = false
  }

  fun evaluate(rawExpression: String): MathResult {
    val trimmed = rawExpression.trim()
    if (trimmed.isBlank()) {
      return MathResult(
        formattedText = "يرجى كتابة تعبير رياضي صالح للحساب (مثال: 25 * 4 + 10 أو جذر 144).",
        source = "محرك الحساب الرياضي الدقيق (Zahra Math Engine)",
        confidence = 0.5,
        outcomeState = OutcomeState.AMBIGUOUS,
        isSuccess = false,
        errorMessage = "التعبير الرياضي فارغ"
      )
    }

    // 1. Normalize Arabic numerals and math phrases
    val normalized = normalizeArabicMath(trimmed)

    return try {
      val (result, cleanExpr) = parseAndEvaluate(normalized)
      if (result.isNaN() || result.isInfinite()) {
        MathResult(
          formattedText = "خطأ رياضي: تجاوز نطاق الأعداد المسموح به أو نتيجة غير معرفة.",
          source = "محرك الحساب الرياضي الدقيق (Zahra Math Engine)",
          confidence = 1.0,
          outcomeState = OutcomeState.EXECUTION_ERROR,
          isSuccess = false,
          errorMessage = "قيمة غير معرفة أو طفحان رقمي"
        )
      } else {
        val formattedNum = formatNumber(result)
        MathResult(
          formattedText = "الناتج: $trimmed = $formattedNum",
          source = "محرك الحساب الرياضي الدقيق (Zahra Math Engine)",
          confidence = 1.0,
          outcomeState = OutcomeState.SUPPORTED,
          isSuccess = true,
          numericResult = result
        )
      }
    } catch (e: ArithmeticException) {
      MathResult(
        formattedText = e.message ?: "خطأ رياضي: القسمة على صفر غير معرفة.",
        source = "محرك الحساب الرياضي الدقيق (Zahra Math Engine)",
        confidence = 1.0,
        outcomeState = OutcomeState.EXECUTION_ERROR,
        isSuccess = false,
        errorMessage = e.message
      )
    } catch (e: IllegalArgumentException) {
      MathResult(
        formattedText = "تعبير رياضي غير صالح: ${e.message}",
        source = "محرك الحساب الرياضي الدقيق (Zahra Math Engine)",
        confidence = 0.8,
        outcomeState = OutcomeState.AMBIGUOUS,
        isSuccess = false,
        errorMessage = e.message
      )
    } catch (e: Exception) {
      MathResult(
        formattedText = "تعذر إجراء العملية الحسابية: تعبير غير مكتمل أو غير مدعوم.",
        source = "محرك الحساب الرياضي الدقيق (Zahra Math Engine)",
        confidence = 0.4,
        outcomeState = OutcomeState.EXECUTION_ERROR,
        isSuccess = false,
        errorMessage = e.message
      )
    }
  }

  fun convertUnit(value: Double, from: String, to: String): MathResult {
    var result = 0.0
    var text = ""

    val normalizedFrom = from.trim().lowercase(Locale.ROOT)
    val normalizedTo = to.trim().lowercase(Locale.ROOT)

    when {
      normalizedFrom == "kg" && normalizedTo == "lb" -> {
        result = value * 2.20462
        text = "$value كيلوغرام = ${"%.2f".format(Locale.US, result)} رطل (باوند)"
      }
      normalizedFrom == "lb" && normalizedTo == "kg" -> {
        result = value / 2.20462
        text = "$value رطل = ${"%.2f".format(Locale.US, result)} كجم"
      }
      normalizedFrom == "km" && normalizedTo == "mi" -> {
        result = value * 0.621371
        text = "$value كم = ${"%.2f".format(Locale.US, result)} ميل"
      }
      normalizedFrom == "mi" && normalizedTo == "km" -> {
        result = value / 0.621371
        text = "$value ميل = ${"%.2f".format(Locale.US, result)} كم"
      }
      normalizedFrom == "c" && normalizedTo == "f" -> {
        result = (value * 9.0) / 5.0 + 32.0
        text = "$value °C = ${"%.1f".format(Locale.US, result)} °F"
      }
      normalizedFrom == "f" && normalizedTo == "c" -> {
        result = ((value - 32.0) * 5.0) / 9.0
        text = "$value °F = ${"%.1f".format(Locale.US, result)} °C"
      }
      normalizedFrom == "m" && normalizedTo == "ft" -> {
        result = value * 3.28084
        text = "$value متر = ${"%.2f".format(Locale.US, result)} قدم"
      }
      normalizedFrom == "ft" && normalizedTo == "m" -> {
        result = value / 3.28084
        text = "$value قدم = ${"%.2f".format(Locale.US, result)} متر"
      }
      normalizedFrom == "l" && normalizedTo == "gal" -> {
        result = value * 0.264172
        text = "$value لتر = ${"%.2f".format(Locale.US, result)} جالون"
      }
      normalizedFrom == "gal" && normalizedTo == "l" -> {
        result = value / 0.264172
        text = "$value جالون = ${"%.2f".format(Locale.US, result)} لتر"
      }
      else -> {
        return MathResult(
          formattedText = "التحويل بين $from و $to غير مدعوم حالياً في القواعد المدمجة.",
          source = "محول الوحدات الفيزيائية",
          confidence = 0.3,
          outcomeState = OutcomeState.UNSUPPORTED,
          isSuccess = false,
          errorMessage = "وحدات غير مدعومة"
        )
      }
    }

    return MathResult(
      formattedText = text,
      source = "المكتب الدولي للأوزان والمقاييس (BIPM)",
      confidence = 1.0,
      outcomeState = OutcomeState.SUPPORTED,
      isSuccess = true,
      numericResult = result
    )
  }

  private fun normalizeArabicMath(raw: String): String {
    var s = raw
    // Convert Eastern Arabic digits
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    for (i in 0..9) {
      s = s.replace(arabicDigits[i], ('0' + i))
    }

    // Common Arabic words to math operators
    s = s.replace("×", "*")
      .replace("÷", "/")
      .replace("**", "^")
      .replace("√", "sqrt ")
      .replace("جذر", "sqrt ")
      .replace("زائد", "+")
      .replace("ناقص", "-")
      .replace("ضرب", "*")
      .replace("في", "*")
      .replace("على", "/")
      .replace("تقسيم", "/")
      .replace("أس", "^")
      .replace("قوة", "^")
      .replace("مرفوع للقوة", "^")
      .replace("مرفوع للاس", "^")
      .replace("مرفوع للأس", "^")

    return s
  }

  private fun parseAndEvaluate(expr: String): Pair<Double, String> {
    val tokens = tokenize(expr)
    if (tokens.isEmpty()) {
      throw IllegalArgumentException("لا توجد حدود رياضية للمعالجة")
    }

    val rpn = toRpn(tokens)
    val result = evaluateRpn(rpn)
    return Pair(result, expr)
  }

  private fun tokenize(expr: String): List<String> {
    val tokens = mutableListOf<String>()
    var i = 0
    val len = expr.length

    while (i < len) {
      val c = expr[i]

      if (c.isWhitespace()) {
        i++
        continue
      }

      // Check for named function "sqrt"
      if (expr.startsWith("sqrt", i, ignoreCase = true)) {
        tokens.add("sqrt")
        i += 4
        continue
      }

      // Numbers (with optional decimal dot)
      if (c.isDigit() || (c == '.' && i + 1 < len && expr[i + 1].isDigit())) {
        val start = i
        var hasDot = (c == '.')
        i++
        while (i < len) {
          val ch = expr[i]
          if (ch.isDigit()) {
            i++
          } else if (ch == '.' && !hasDot) {
            hasDot = true
            i++
          } else {
            break
          }
        }
        tokens.add(expr.substring(start, i))
        continue
      }

      // Operators and parentheses
      if (c in listOf('+', '-', '*', '/', '^', '%', '(', ')')) {
        // Distinguish unary minus:
        // A '-' is unary if:
        // - It is the first token
        // - Or the previous token is an operator ('+', '-', '*', '/', '^', '%', "NEG", "sqrt") or '('
        if (c == '-') {
          val prevToken = tokens.lastOrNull()
          val isUnary = prevToken == null || prevToken in listOf("+", "-", "*", "/", "^", "%", "NEG", "sqrt", "(")
          if (isUnary) {
            tokens.add("NEG")
            i++
            continue
          }
        } else if (c == '+') {
          // Unary plus is a no-op
          val prevToken = tokens.lastOrNull()
          val isUnary = prevToken == null || prevToken in listOf("+", "-", "*", "/", "^", "%", "NEG", "sqrt", "(")
          if (isUnary) {
            i++
            continue
          }
        }
        tokens.add(c.toString())
        i++
        continue
      }

      // Unknown character
      throw IllegalArgumentException("رمز غير مدعوم: '$c'")
    }

    return tokens
  }

  private fun toRpn(tokens: List<String>): List<String> {
    val output = mutableListOf<String>()
    val stack = mutableListOf<String>()

    // Operator precedence
    // ^ is right-associative (precedence 4)
    // NEG is right-associative unary prefix (precedence 3)
    // sqrt is right-associative prefix (precedence 3)
    // *, /, % are left-associative (precedence 2)
    // +, - are left-associative (precedence 1)

    fun precedence(op: String): Int = when (op) {
      "^" -> 4
      "NEG", "sqrt" -> 3
      "*", "/", "%" -> 2
      "+", "-" -> 1
      else -> 0
    }

    fun isRightAssociative(op: String): Boolean = (op == "^" || op == "NEG" || op == "sqrt")

    var expectedOperand = true

    for (token in tokens) {
      val isNumber = token.toDoubleOrNull() != null
      if (isNumber) {
        output.add(token)
        expectedOperand = false
      } else if (token == "(") {
        stack.add(token)
        expectedOperand = true
      } else if (token == ")") {
        var foundOpen = false
        while (stack.isNotEmpty()) {
          val top = stack.removeAt(stack.size - 1)
          if (top == "(") {
            foundOpen = true
            break
          }
          output.add(top)
        }
        if (!foundOpen) {
          throw IllegalArgumentException("أقواس غير متطابقة (قوس إغلاق إضافي)")
        }
        expectedOperand = false
      } else if (token == "NEG" || token == "sqrt") {
        stack.add(token)
        expectedOperand = true
      } else {
        // Binary operator (+, -, *, /, ^, %)
        if (expectedOperand) {
          throw IllegalArgumentException("عامل تشغيل بدون معامل سابق: '$token'")
        }
        while (stack.isNotEmpty()) {
          val top = stack.last()
          if (top == "(") break
          val pTop = precedence(top)
          val pToken = precedence(token)
          if (pTop > pToken || (pTop == pToken && !isRightAssociative(token))) {
            output.add(stack.removeAt(stack.size - 1))
          } else {
            break
          }
        }
        stack.add(token)
        expectedOperand = true
      }
    }

    if (expectedOperand && stack.lastOrNull() !in listOf(")")) {
      throw IllegalArgumentException("التعبير ينتهي بعامل تشغيل دون معامل أخير")
    }

    while (stack.isNotEmpty()) {
      val top = stack.removeAt(stack.size - 1)
      if (top == "(" || top == ")") {
        throw IllegalArgumentException("أقواس غير متطابقة (قوس مفتوح غير مغلق)")
      }
      output.add(top)
    }

    return output
  }

  private fun evaluateRpn(rpn: List<String>): Double {
    val stack = mutableListOf<Double>()

    for (token in rpn) {
      val num = token.toDoubleOrNull()
      if (num != null) {
        stack.add(num)
      } else if (token == "NEG") {
        if (stack.isEmpty()) throw IllegalArgumentException("معامل غير مكتمل للإشارة السالبة")
        val a = stack.removeAt(stack.size - 1)
        stack.add(-a)
      } else if (token == "sqrt") {
        if (stack.isEmpty()) throw IllegalArgumentException("معامل غير مكتمل للجذر التربيعي")
        val a = stack.removeAt(stack.size - 1)
        if (a < 0.0) throw ArithmeticException("خطأ رياضي: لا يمكن حساب الجذر التربيعي لعدد سالب ($a).")
        stack.add(sqrt(a))
      } else {
        if (stack.size < 2) {
          throw IllegalArgumentException("حدود غير كافية لإجراء العملية: '$token'")
        }
        val b = stack.removeAt(stack.size - 1)
        val a = stack.removeAt(stack.size - 1)

        when (token) {
          "+" -> stack.add(a + b)
          "-" -> stack.add(a - b)
          "*" -> stack.add(a * b)
          "/" -> {
            if (abs(b) < 1e-15) {
              throw ArithmeticException("خطأ رياضي: القسمة على صفر غير معرفة (Division by zero)")
            }
            stack.add(a / b)
          }
          "%" -> {
            if (abs(b) < 1e-15) {
              throw ArithmeticException("خطأ رياضي: حساب باقي القسمة على صفر غير معرف")
            }
            stack.add(a % b)
          }
          "^" -> {
            val res = a.pow(b)
            stack.add(res)
          }
          else -> throw IllegalArgumentException("عملية غير معروفة: '$token'")
        }
      }
    }

    if (stack.size != 1) {
      throw IllegalArgumentException("تعبير غير متوازن رياضياً")
    }

    return stack[0]
  }

  private fun formatNumber(value: Double): String {
    return if (value == value.toLong().toDouble()) {
      value.toLong().toString()
    } else {
      DECIMAL_FORMAT.format(value)
    }
  }
}
