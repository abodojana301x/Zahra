# Zahra (زهرة) 2.0 Professional — Offline AI Assistant

A high-performance, zero-model, zero-network, privacy-first mobile AI assistant that delivers real conversational and utilitarian intelligence through deterministic algorithms, statistical NLP (Naive Bayes, TF-IDF, TextRank), fuzzy retrieval, and curated knowledge bases—all operating 100% offline in airplane mode with an ultra-sleek OLED Liquid Glass UI.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Deterministic Zero-Model Guarantee**: Zahra operates without any LLM weights, downloaded `.onnx`/`.tflite` model files, or network telemetry. All reasoning stems from deterministic finite-state machines, regex command parsing, log-space Multinomial Naive Bayes classification with Laplace smoothing, TextRank graph summarization, Porter & Arabic light stemming, astronomical prayer time calculations, and indexed local SQLite/JSON knowledge bases.

- **Dual-Layer Architecture**: A production-ready Android Jetpack Compose application featuring the full 7-screen Liquid Glass design system, backed by an offline computational linguistics engine that implements all 6 layers of the Zahra specification.
- **Strict Citation & Confidence Gating**:
  - Confidence $\ge 0.75$: Direct authoritative response with citations (`[المصدر: ...]`).
  - Confidence $0.40 - 0.74$: Qualified response with `(ثقة متوسطة)` badge.
  - Confidence $< 0.40$: Explicit refusal (`"مش متأكد من الإجابة بدقة"`) paired with 3 intelligent closest suggestions. Zero hallucinations.
- **Local Data Protection**: All user notes, tasks, and conversation turns are stored locally in SQLite with AES-256 encryption.

---

## 1. Overview & Core Concept

- **What It Does**: Zahra is an offline executive assistant and computational intelligence engine for Arabic and multilingual users. It handles arithmetic & symbolic math, bilingual translation (Arabic ↔ English ↔ French) with morphological stemming, Quran/Hadith/Fiqh lookups with strict source attribution, extractive TextRank summarization, intelligent task management with automatic TF-IDF categorization, and smart note-taking with RAKE keyword extraction.
- **Target Persona**: Privacy-conscious professionals, students, travelers, and researchers who require reliable, instant, and private assistance without internet access, battery drain, or subscription lock-in.
- **Key Value**: 100% reliable offline utility under 50 MB, sub-200ms response latency, zero hallucinations, zero cloud dependency, and permanent privacy.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme
- **Theme**: "Liquid Glass" (iOS 26 aesthetic) on deep OLED black.
- **Color Palette**:
  - Background: `#0A0A0F` (near-black, battery-conserving OLED background)
  - Primary / Gradient Start: `#6B21A8` (deep royal violet)
  - Secondary / Gradient End: `#06B6D4` (vibrant cyber cyan)
  - Surface Glass: `rgba(255, 255, 255, 0.08)` with subtle `rgba(255, 255, 255, 0.15)` border strokes
  - Accent / Highlights: `#F472B6` (soft luminous pink for tags and badges)
  - Text: High contrast `#F8FAFC` for headers, `#94A3B8` for secondary metadata
- **Typography & Layout**:
  - Full RTL (Right-to-Left) native support for Arabic with LTR auto-detection for code/math/Latin.
  - Generous 16dp / 24dp padding grid, 24dp glass rounded card corners, and floating navigation bar with fluid spring transitions.

### The 7 Core Screens
1. **Chat Screen**: Conversational interface with fast streaming simulation, typing pulse indicator, intent & confidence tag badges, citation links, quick suggestion chips, and instant action buttons.
2. **Tasks Screen**: Auto-categorized todo list with TF-IDF classification (Work, Personal, Urgent, Finance), due date extraction from Arabic text ("بكرة", "الأحد الجاي"), priority score calculation, and filter tabs.
3. **Notes Screen**: Rich note management with automatic RAKE keyword extraction, tag cloud, full-text inverted search, and instant note creation from chat.
4. **Translate Screen**: AR ↔ EN ↔ FR dictionary and phrasebook with morphological light stemmer, grammar POS tags, and offline search.
5. **Religion & Guidance Screen**: Authentic Quran ayah search with tafsir summaries, Hadith verification with isnad/book references, Fiqh Q&A with madhhab tags, astronomical prayer time calculator with countdown to next prayer, and Qibla direction formula.
6. **Memory Archive Screen**: Historical timeline of conversations, memory inspection, RAKE topics filter, and entity graph viewer.
7. **Settings & Honest Limitations**: Clear transparency screen detailing rule-based limitations, local encryption status, airplane mode test tool, database backup export, and system performance benchmarks.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale | Alternatives Considered |
|---|---|---|---|
| **AI Subsystem** | Deterministic Expert System + Statistical NLP (Naive Bayes + TF-IDF + TextRank) | Guaranteed zero network, zero heavy weight models, runs under 50MB with instant startup | Local LLMs (GGUF/ONNX) rejected due to 1-4GB model weight size, battery drain, and hallucination risk |
| **Summarization** | Extractive TextRank Graph Centrality | Picks the most representative existing sentences mathematically; cannot fabricate facts | Generative summarization rejected because it requires an LLM |
| **Arabic NLP** | Custom Unicode Normalizer & Light Stemmer (stripping diacritics, unifying Alef/Ya/Ta-marbuta, affix stripping) | Instant performance on mobile CPU without external dependencies | PyArabic / CoreNLP rejected due to runtime weight and complex bindings |
| **Storage & Memory** | Structured SQLite (Room) + In-Memory Ring Buffer + Inverted Index | Fast full-text search, persistent privacy, atomic transactions | Unstructured JSON files rejected due to query latency on large datasets |

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ZAHRA 2.0 MOBILE CLIENT                         │
├────────────────────────────────────────────────────────────────────────┤
│  JETPACK COMPOSE LIQUID GLASS UI (Chat, Tasks, Notes, Translate, etc.) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ User Query / Voice / Text
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     LAYER 1: INPUT NORMALIZATION                       │
│  • Unicode Tokenizer (Word boundary segmentation)                      │
│  • Arabic Normalizer (Diacritics removal, Alef/Ya/Ta unification)      │
│  • Script & Language Detector (Arabic / Latin / Mixed N-grams)         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Clean Tokens & Script Profile
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     LAYER 2: INTENT CLASSIFICATION                     │
│  [1] Regex Matcher ──▶ [2] Naive Bayes ──▶ [3] Fuzzy ──▶ [4] FSM Dialog│
│  (Exact commands)     (Log-space NB)     (Levenshtein)  (Multi-turn)   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Intent + Confidence Score
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     LAYER 3: ENTITY EXTRACTION                         │
│  • Numbers & Ordinals (Arabic-Indic: ٠-٩ & Latin: 0-9)                │
│  • Relative & Absolute Dates (بكرة, الأحد الجاي, بعد أسبوع, 12 مارس)   │
│  • Time & Prayer Expressions (٣ العصر, قبل الفجر, 14:30)               │
│  • Currencies & Physical Units (SAR, EGP, USD, kg, km, °C)             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Structured Intent + Extracted Slots
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     LAYER 4: REASONING ROUTER                          │
│  ┌──────────────┬──────────────┬──────────────┬─────────────────────┐  │
│  │ Math Engine  │ Religion KB  │ Translate KB │ TextRank Summarizer │  │
│  ├──────────────┼──────────────┼──────────────┼─────────────────────┤  │
│  │ Task Engine  │ Note Engine  │ Dialog FSM   │ Fallback Engine     │  │
│  └──────────────┴──────────────┴──────────────┴─────────────────────┘  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Raw Engine Output + Citations
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     LAYER 5 & 6: MEMORY & RESPONSE                     │
│  • Short-term: 10-turn RAM Ring Buffer                                 │
│  • Long-term: SQLite Inverted Index + RAKE Keywords                    │
│  • Confidence Gate: Direct (≥0.75) | Qualified (0.40-0.74) | Safe (<0.4│
│  • Citation Formatter: [المصدر: ...]                                   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Implementation Roadmap & Deliverables

1. **Android Resource & Branding Setup**:
   - Set app name to "Zahra" in `res/values/strings.xml`, `settings.gradle.kts`, and `metadata.json`.
   - Create custom vector neural-flower adaptive icon in `res/drawable`.
2. **Knowledge Base Assets**:
   - Curated JSON databases bundled in assets:
     - `religion_kb.json` (Quran, Hadith, Fiqh, Prayer formula)
     - `translation_kb.json` (AR-EN-FR glossaries, phrases, morphological roots)
     - `general_kb.json` (Science, history, tech, geography facts with citations)
     - `intent_corpus.json` (Training sets for 25 intents)
3. **Core AI Engine Implementation**:
   - `ZahraNormalizer` & `LanguageDetector`
   - `ZahraTokenizer` & `ArabicStemmer`
   - `NaiveBayesClassifier` (log-space Multinomial with Laplace smoothing)
   - `EntityExtractor` (Arabic dates, times, currencies, units, numbers)
   - Specialized engines: `MathEngine`, `TextRankSummarizer`, `ReligionEngine`, `TranslationEngine`, `TaskEngine`, `NoteEngine`, `DialogEngine`, `FallbackEngine`
4. **Local Database & Storage (Room / SQLite)**:
   - Entities for Messages, Tasks, Notes, and Memory Index.
   - DAOs with search, filtering, and CRUD operations.
5. **Liquid Glass UI & Screens**:
   - Theme definition with OLED `#0A0A0F`, `#6B21A8`, `#06B6D4`, and glass modifier components.
   - Interactive 7-tab navigation: Chat, Tasks, Notes, Translate, Religion, Archive, and Settings.
6. **Verification & Testing**:
   - Compile applet verification.
   - Unit tests for Normalizer, Naive Bayes, TextRank, and Math parser.
   - Offline validation in Airplane mode.
