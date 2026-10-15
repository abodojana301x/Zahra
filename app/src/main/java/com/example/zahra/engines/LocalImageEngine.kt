package com.example.zahra.engines

import com.example.zahra.model.ZahraToolPayload.ImageGenerationPayload
import kotlin.random.Random

class LocalImageEngine {

  private val quotePresets = listOf(
    Pair("وَقُل رَّبِّ زِدْنِي عِلْمًا", "القرآن الكريم - سورة طه"),
    Pair("الخيال أهم من المعرفة، فالمعرفة محدودة بينما الخيال يحيط بالعالم", "ألبرت أينشتاين"),
    Pair("اصبر تنل، فما خاب من استعان بالصبر والعمل الصالح", "حكمة عربية"),
    Pair("العلم نور يُضيء دروب الحيرة والظلمات", "أثر عربي مأثور"),
    Pair("لا تحسبن المجد تمراً أنت آكله، لن تبلغ المجد حتى تلعق الصبرا", "أبو تمام"),
    Pair("التفكير هو حوار الروح مع ذاتها للوصول إلى الحقيقة", "أفلاطون"),
    Pair("وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ", "القرآن الكريم - سورة الطلاق"),
    Pair("البدايات دائماً صعبة، ولكن الإصرار يصنع المعجزات", "رواد الابتكار")
  )

  fun generateImage(
    userPrompt: String,
    forcedMode: String? = null,
    forcedStyle: String? = null,
    seed: Long = System.currentTimeMillis()
  ): ImageGenerationPayload {
    val random = Random(seed)
    val lower = userPrompt.lowercase()

    // Determine Mode: "vector", "quote_card", or "pixel_art"
    val mode = forcedMode ?: when {
      lower.contains("بكسل") || lower.contains("pixel") || lower.contains("8bit") || lower.contains("ألعاب") -> "pixel_art"
      lower.contains("اقتباس") || lower.contains("بطاقة") || lower.contains("حكمة") || lower.contains("خط") || lower.contains("quote") || lower.contains("نص") -> "quote_card"
      else -> "vector"
    }

    // Determine Style: "galaxy", "mandala", "nature", "geometric", "cyber"
    val shapeStyle = forcedStyle ?: when {
      lower.contains("فضاء") || lower.contains("نجوم") || lower.contains("مجرة") || lower.contains("كوكب") || lower.contains("galaxy") -> "galaxy"
      lower.contains("زخرف") || lower.contains("ماندالا") || lower.contains("إسلام") || lower.contains("mandala") -> "mandala"
      lower.contains("طبيع") || lower.contains("شجر") || lower.contains("جبل") || lower.contains("شروق") || lower.contains("بحر") -> "nature"
      lower.contains("سايبر") || lower.contains("نيون") || lower.contains("تقني") || lower.contains("cyber") || lower.contains("ذكاء") -> "cyber"
      else -> listOf("galaxy", "mandala", "nature", "geometric", "cyber")[random.nextInt(5)]
    }

    // Color palettes
    val (primaryColor, secondaryColor) = when (shapeStyle) {
      "galaxy" -> Pair(0xFF6366F1, 0xFFA855F7) // Indigo to Purple
      "mandala" -> Pair(0xFFF59E0B, 0xFF10B981) // Amber to Emerald
      "nature" -> Pair(0xFF10B981, 0xFF06B6D4) // Emerald to Cyan
      "cyber" -> Pair(0xFF06B6D4, 0xFFEC4899) // Cyan to Pink Neon
      else -> Pair(0xFF3B82F6, 0xFF8B5CF6) // Blue to Violet
    }

    // Pick Quote
    val quotePair = quotePresets[random.nextInt(quotePresets.size)]

    // Generate 16x16 Pixel Grid if pixel_art mode
    val pixelGrid = if (mode == "pixel_art") {
      generatePixelMatrix(shapeStyle, seed)
    } else {
      emptyList()
    }

    val palette = listOf(
      0xFF0F172A, // Slate 900
      primaryColor,
      secondaryColor,
      0xFFF8FAFC, // White
      0xFFE2E8F0  // Light slate
    )

    val title = when (mode) {
      "vector" -> "لوحة فنية إجرائية: ${getArabicStyleName(shapeStyle)}"
      "quote_card" -> "بطاقة اقتباس رقمية فاخرة"
      "pixel_art" -> "رسمة بكسل رقمية 16×16: ${getArabicStyleName(shapeStyle)}"
      else -> "عمل فني إبداعي"
    }

    return ImageGenerationPayload(
      prompt = userPrompt.ifBlank { "لوحة تعبيرية إبداعية" },
      title = title,
      mode = mode,
      shapeStyle = shapeStyle,
      primaryColorHex = primaryColor,
      secondaryColorHex = secondaryColor,
      quoteArabic = quotePair.first,
      quoteAuthor = quotePair.second,
      pixelGrid = pixelGrid,
      palette = palette,
      seed = seed
    )
  }

  private fun getArabicStyleName(style: String): String {
    return when (style) {
      "galaxy" -> "أعماق المجرة والفضاء الكوني"
      "mandala" -> "زخرفة ماندالا هندسية"
      "nature" -> "شروق الطبيعة والآفاق"
      "cyber" -> "الشبكة السيبرانية والذكاء الاصطناعي"
      else -> "تجريد هندسي حديث"
    }
  }

  private fun generatePixelMatrix(style: String, seed: Long): List<List<Int>> {
    val random = Random(seed)
    val size = 16
    val grid = Array(size) { IntArray(size) { 0 } }

    // Symmetrical generation (mirror horizontally)
    val halfWidth = size / 2

    when (style) {
      "mandala", "geometric" -> {
        // Concentric geometric diamond/star
        for (y in 0 until size) {
          for (x in 0 until halfWidth) {
            val dist = Math.abs(x - halfWidth + 0.5) + Math.abs(y - halfWidth + 0.5)
            val colorIdx = when {
              dist < 3.0 -> 3
              dist < 5.5 -> 1
              dist < 7.5 -> 2
              dist < 9.0 -> if ((x + y) % 2 == 0) 1 else 0
              else -> 0
            }
            grid[y][x] = colorIdx
            grid[y][size - 1 - x] = colorIdx
          }
        }
      }
      "cyber" -> {
        // Space invader / avatar robot
        for (y in 2 until size - 2) {
          for (x in 2 until halfWidth) {
            val fill = random.nextFloat() > 0.45f
            val colorIdx = if (fill) (if (random.nextBoolean()) 1 else 2) else 0
            grid[y][x] = colorIdx
            grid[y][size - 1 - x] = colorIdx
          }
        }
        // Center core
        for (y in 6..9) {
          grid[y][7] = 3
          grid[y][8] = 3
        }
      }
      else -> {
        // Landscape / cosmic pixel art
        for (y in 0 until size) {
          for (x in 0 until size) {
            val isSky = y < 9
            if (isSky) {
              // Stars
              grid[y][x] = if (random.nextInt(15) == 0) 3 else 0
            } else {
              // Mountains
              val mHeight = 16 - (Math.sin(x * 0.4) * 3 + 4).toInt()
              grid[y][x] = if (y >= mHeight) {
                if ((x + y) % 2 == 0) 1 else 2
              } else 0
            }
          }
        }
        // Celestial body at (4, 4)
        grid[3][4] = 3; grid[3][5] = 3
        grid[4][3] = 3; grid[4][4] = 3; grid[4][5] = 3; grid[4][6] = 3
        grid[5][4] = 3; grid[5][5] = 3
      }
    }

    return grid.map { it.toList() }
  }
}
