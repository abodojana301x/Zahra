package com.example

import com.example.zahra.engines.MathEngine
import com.example.zahra.model.OutcomeState
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class WorkstreamAMathTest {

  // --- A1: Division by Zero ---

  @Test
  fun testDivisionByZero_simple() {
    val res = MathEngine.evaluate("5 / 0")
    assertFalse(res.isSuccess)
    assertEquals(OutcomeState.EXECUTION_ERROR, res.outcomeState)
    assertTrue(res.formattedText.contains("صفر") || res.formattedText.contains("غير معرفة"))
  }

  @Test
  fun testDivisionByZero_zeroDividedByZero() {
    val res = MathEngine.evaluate("0 / 0")
    assertFalse(res.isSuccess)
    assertEquals(OutcomeState.EXECUTION_ERROR, res.outcomeState)
    assertTrue(res.formattedText.contains("صفر") || res.formattedText.contains("غير معرفة"))
  }

  @Test
  fun testDivisionByZero_insideSubexpression() {
    val res = MathEngine.evaluate("10 / (3 - 3)")
    assertFalse(res.isSuccess)
    assertEquals(OutcomeState.EXECUTION_ERROR, res.outcomeState)
    assertTrue(res.formattedText.contains("صفر") || res.formattedText.contains("غير معرفة"))
  }

  @Test
  fun testDivisionByZero_complexSubexpression() {
    val res = MathEngine.evaluate("50 + (100 / (10 - 2 * 5))")
    assertFalse(res.isSuccess)
    assertEquals(OutcomeState.EXECUTION_ERROR, res.outcomeState)
  }

  @Test
  fun testModuloByZero() {
    val res = MathEngine.evaluate("15 % 0")
    assertFalse(res.isSuccess)
    assertEquals(OutcomeState.EXECUTION_ERROR, res.outcomeState)
  }

  // --- A2: Unary Minus and Negative Numbers ---

  @Test
  fun testUnaryMinus_atBeginning() {
    val res = MathEngine.evaluate("-5 + 3")
    assertTrue(res.isSuccess)
    assertEquals(-2.0, res.numericResult ?: 0.0, 0.0001)
    assertTrue(res.formattedText.contains("-2"))
  }

  @Test
  fun testUnaryMinus_afterMultiplication() {
    val res = MathEngine.evaluate("5 * -2")
    assertTrue(res.isSuccess)
    assertEquals(-10.0, res.numericResult ?: 0.0, 0.0001)
    assertTrue(res.formattedText.contains("-10"))
  }

  @Test
  fun testUnaryMinus_parenthesizedExpression() {
    val res = MathEngine.evaluate("-(3 + 2)")
    assertTrue(res.isSuccess)
    assertEquals(-5.0, res.numericResult ?: 0.0, 0.0001)
  }

  @Test
  fun testUnaryMinus_multiplicationOfNegatives() {
    val res = MathEngine.evaluate("(-4) * (-3)")
    assertTrue(res.isSuccess)
    assertEquals(12.0, res.numericResult ?: 0.0, 0.0001)
    assertTrue(res.formattedText.contains("12"))
  }

  @Test
  fun testUnaryMinus_subtractionAndUnaryMinus() {
    val res = MathEngine.evaluate("5 - -3")
    assertTrue(res.isSuccess)
    assertEquals(8.0, res.numericResult ?: 0.0, 0.0001)
  }

  // --- A3: Operator Precedence and Associativity ---

  @Test
  fun testPrecedence_multiplicationBeforeAddition() {
    val res1 = MathEngine.evaluate("2 + 3 * 4")
    assertTrue(res1.isSuccess)
    assertEquals(14.0, res1.numericResult ?: 0.0, 0.0001)

    val res2 = MathEngine.evaluate("(2 + 3) * 4")
    assertTrue(res2.isSuccess)
    assertEquals(20.0, res2.numericResult ?: 0.0, 0.0001)
  }

  @Test
  fun testAssociativity_exponentiationIsRightAssociative() {
    // 2 ^ 3 ^ 2 should be 2 ^ (3 ^ 2) = 2 ^ 9 = 512, NOT (2^3)^2 = 64
    val resCaret = MathEngine.evaluate("2 ^ 3 ^ 2")
    assertTrue(resCaret.isSuccess)
    assertEquals(512.0, resCaret.numericResult ?: 0.0, 0.0001)

    val resAsterisk = MathEngine.evaluate("2 ** 3 ** 2")
    assertTrue(resAsterisk.isSuccess)
    assertEquals(512.0, resAsterisk.numericResult ?: 0.0, 0.0001)
  }

  @Test
  fun testPrecedence_negativeExponentiation() {
    // Standard mathematical semantics: exponentiation binds tighter than unary minus: -2 ^ 2 = -(2^2) = -4
    val res1 = MathEngine.evaluate("-2 ^ 2")
    assertTrue(res1.isSuccess)
    assertEquals(-4.0, res1.numericResult ?: 0.0, 0.0001)

    // With explicit parenthesis: (-2) ^ 2 = 4
    val res2 = MathEngine.evaluate("(-2) ^ 2")
    assertTrue(res2.isSuccess)
    assertEquals(4.0, res2.numericResult ?: 0.0, 0.0001)
  }

  // --- A4: Invalid and Incomplete Expressions ---

  @Test
  fun testInvalidExpression_empty() {
    val res = MathEngine.evaluate("")
    assertFalse(res.isSuccess)
    assertEquals(OutcomeState.AMBIGUOUS, res.outcomeState)
  }

  @Test
  fun testInvalidExpression_unmatchedParentheses() {
    val resOpen = MathEngine.evaluate("(2 + 3")
    assertFalse(resOpen.isSuccess)
    assertEquals(OutcomeState.AMBIGUOUS, resOpen.outcomeState)

    val resClose = MathEngine.evaluate("2 + 3)")
    assertFalse(resClose.isSuccess)
    assertEquals(OutcomeState.AMBIGUOUS, resClose.outcomeState)
  }

  @Test
  fun testInvalidExpression_missingOperands() {
    val resTrailing = MathEngine.evaluate("2 +")
    assertFalse(resTrailing.isSuccess)
    assertEquals(OutcomeState.AMBIGUOUS, resTrailing.outcomeState)

    val resLeading = MathEngine.evaluate("* 5")
    assertFalse(resLeading.isSuccess)
    assertEquals(OutcomeState.AMBIGUOUS, resLeading.outcomeState)
  }

  @Test
  fun testInvalidExpression_repeatedInvalidOperators() {
    val res = MathEngine.evaluate("2 * / 3")
    assertFalse(res.isSuccess)
    assertEquals(OutcomeState.AMBIGUOUS, res.outcomeState)
  }

  @Test
  fun testInvalidExpression_unsupportedCharacters() {
    val res = MathEngine.evaluate("2 + abc * 3")
    assertFalse(res.isSuccess)
    assertEquals(OutcomeState.AMBIGUOUS, res.outcomeState)
  }

  // --- A5: Decimals, Square Roots, and Arabic Numerals ---

  @Test
  fun testSquareRoot_valid() {
    val res = MathEngine.evaluate("جذر 144")
    assertTrue(res.isSuccess)
    assertEquals(12.0, res.numericResult ?: 0.0, 0.0001)
  }

  @Test
  fun testSquareRoot_insideExpression() {
    val res = MathEngine.evaluate("2 * sqrt 25 + 4")
    assertTrue(res.isSuccess)
    assertEquals(14.0, res.numericResult ?: 0.0, 0.0001)
  }

  @Test
  fun testSquareRoot_negativeThrowsError() {
    val res = MathEngine.evaluate("sqrt(-16)")
    assertFalse(res.isSuccess)
    assertEquals(OutcomeState.EXECUTION_ERROR, res.outcomeState)
  }

  @Test
  fun testArabicWordsAndIndicNumerals() {
    val res = MathEngine.evaluate("٢٥ ضرب ٤ زائد ١٠")
    assertTrue(res.isSuccess)
    assertEquals(110.0, res.numericResult ?: 0.0, 0.0001)
  }

  // --- Benchmark: 1,000 Generated Deterministic Math Cases ---

  @Test
  fun testBenchmark_1000GeneratedMathCases() {
    val rng = Random(42) // Fixed seed for 100% reproducible testing
    var passedCount = 0
    val totalCases = 1000

    for (i in 0 until totalCases) {
      val caseType = i % 5
      when (caseType) {
        0 -> {
          // Valid addition/subtraction
          val a = rng.nextInt(-1000, 1000)
          val b = rng.nextInt(-1000, 1000)
          val op = if (rng.nextBoolean()) "+" else "-"
          val expr = "$a $op $b"
          val expected = if (op == "+") (a + b).toDouble() else (a - b).toDouble()
          val res = MathEngine.evaluate(expr)
          assertTrue("Failed on valid case: $expr", res.isSuccess)
          assertEquals("Wrong result on $expr", expected, res.numericResult ?: 0.0, 0.0001)
          passedCount++
        }
        1 -> {
          // Valid multiplication & precedence: a + b * c
          val a = rng.nextInt(-100, 100)
          val b = rng.nextInt(-50, 50)
          val c = rng.nextInt(-50, 50)
          val expr = "$a + $b * $c"
          val expected = (a + b * c).toDouble()
          val res = MathEngine.evaluate(expr)
          assertTrue("Failed on: $expr", res.isSuccess)
          assertEquals("Wrong result on $expr", expected, res.numericResult ?: 0.0, 0.0001)
          passedCount++
        }
        2 -> {
          // Division by zero cases (MUST fail safely without crash)
          val a = rng.nextInt(1, 100)
          val expr = "$a / 0"
          val res = MathEngine.evaluate(expr)
          assertFalse("Division by zero must not succeed: $expr", res.isSuccess)
          assertEquals(OutcomeState.EXECUTION_ERROR, res.outcomeState)
          passedCount++
        }
        3 -> {
          // Exponentiation: a ^ b
          val a = rng.nextInt(2, 6)
          val b = rng.nextInt(1, 5)
          val expr = "$a ^ $b"
          val expected = Math.pow(a.toDouble(), b.toDouble())
          val res = MathEngine.evaluate(expr)
          assertTrue("Failed on: $expr", res.isSuccess)
          assertEquals("Wrong result on $expr", expected, res.numericResult ?: 0.0, 0.0001)
          passedCount++
        }
        4 -> {
          // Invalid syntax / unmatched parentheses
          val expr = "(${rng.nextInt(1, 50)} + ${rng.nextInt(1, 50)}"
          val res = MathEngine.evaluate(expr)
          assertFalse("Unmatched parens must fail: $expr", res.isSuccess)
          assertEquals(OutcomeState.AMBIGUOUS, res.outcomeState)
          passedCount++
        }
      }
    }

    assertEquals(totalCases, passedCount)
  }
}
