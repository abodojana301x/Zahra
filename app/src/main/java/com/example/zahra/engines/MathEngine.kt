package com.example.zahra.engines

import kotlin.math.pow
import kotlin.math.sqrt

data class MathResult(
  val formattedText: String,
  val source: String,
  val confidence: Double
)

object MathEngine {
  fun evaluate(expression: String): MathResult {
    return try {
      var clean = expression
        .replace("×", "*")
        .replace("÷", "/")
        .replace(" ", "")

      // Handle square root
      val sqrtMatch = Regex("جذر(\\d+(?:\\.\\d+)?)").find(clean)
      if (sqrtMatch != null) {
        val num = sqrtMatch.groupValues[1].toDouble()
        val res = sqrt(num)
        return MathResult("الناتج: √$num = $res", "محرك الحساب الرياضي الدقيق (Zahra Math Engine)", 1.0)
      }

      // Handle simple expressions: + - * / ^
      val res = evaluateExpression(clean)
      MathResult("ناتج العملية: $expression = $res", "محرك الحساب الرياضي الدقيق (Zahra Math Engine)", 1.0)
    } catch (e: Exception) {
      MathResult("تعذر إجراء العملية الحسابية بدقة.", "محرك الحساب الرياضي", 0.4)
    }
  }

  fun convertUnit(value: Double, from: String, to: String): MathResult {
    var result = 0.0
    var text = ""

    when {
      from == "kg" && to == "lb" -> {
        result = value * 2.20462
        text = "$value كيلوغرام = ${"%.2f".format(result)} رطل (باوند)"
      }
      from == "lb" && to == "kg" -> {
        result = value / 2.20462
        text = "$value رطل = ${"%.2f".format(result)} كجم"
      }
      from == "km" && to == "mi" -> {
        result = value * 0.621371
        text = "$value كم = ${"%.2f".format(result)} ميل"
      }
      from == "mi" && to == "km" -> {
        result = value / 0.621371
        text = "$value ميل = ${"%.2f".format(result)} كم"
      }
      from == "C" && to == "F" -> {
        result = (value * 9.0) / 5.0 + 32.0
        text = "$value °C = ${"%.1f".format(result)} °F"
      }
      from == "F" && to == "C" -> {
        result = ((value - 32.0) * 5.0) / 9.0
        text = "$value °F = ${"%.1f".format(result)} °C"
      }
      else -> {
        return MathResult("التحويل بين $from و $to غير مدعوم حالياً في القواعد المدمجة.", "محول الوحدات الفيزيائية", 0.3)
      }
    }

    return MathResult(text, "المكتب الدولي للأوزان والمقاييس (BIPM)", 1.0)
  }

  private fun evaluateExpression(expr: String): Double {
    // Basic Shunting-yard algorithm
    val tokens = mutableListOf<String>()
    var i = 0
    while (i < expr.length) {
      val c = expr[i]
      if (c.isDigit() || c == '.') {
        val start = i
        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
          i++
        }
        tokens.add(expr.substring(start, i))
        continue
      } else if (c in listOf('+', '-', '*', '/', '^', '(', ')')) {
        tokens.add(c.toString())
      }
      i++
    }

    val outputQueue = mutableListOf<String>()
    val opStack = mutableListOf<String>()
    val precedence = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "^" to 3)

    for (token in tokens) {
      val num = token.toDoubleOrNull()
      if (num != null) {
        outputQueue.add(token)
      } else if (token == "(") {
        opStack.add(token)
      } else if (token == ")") {
        while (opStack.isNotEmpty() && opStack.last() != "(") {
          outputQueue.add(opStack.removeAt(opStack.size - 1))
        }
        if (opStack.isNotEmpty()) opStack.removeAt(opStack.size - 1)
      } else if (precedence.containsKey(token)) {
        while (opStack.isNotEmpty() && opStack.last() != "(" &&
          (precedence[opStack.last()] ?: 0) >= (precedence[token] ?: 0)
        ) {
          outputQueue.add(opStack.removeAt(opStack.size - 1))
        }
        opStack.add(token)
      }
    }

    while (opStack.isNotEmpty()) {
      outputQueue.add(opStack.removeAt(opStack.size - 1))
    }

    val evalStack = mutableListOf<Double>()
    for (item in outputQueue) {
      val num = item.toDoubleOrNull()
      if (num != null) {
        evalStack.add(num)
      } else {
        val b = if (evalStack.isNotEmpty()) evalStack.removeAt(evalStack.size - 1) else 0.0
        val a = if (evalStack.isNotEmpty()) evalStack.removeAt(evalStack.size - 1) else 0.0
        when (item) {
          "+" -> evalStack.add(a + b)
          "-" -> evalStack.add(a - b)
          "*" -> evalStack.add(a * b)
          "/" -> evalStack.add(if (b != 0.0) a / b else 0.0)
          "^" -> evalStack.add(a.pow(b))
        }
      }
    }

    return evalStack.lastOrNull() ?: 0.0
  }
}
