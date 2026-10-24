# Project: SiteVoice Form Setup & Google Doc Export Upgrade

## Architecture
SiteVoice is an Android application (Kotlin, Jetpack Compose, Room, WorkManager, Coroutines) designed for construction site voice logging and automated daily report generation.
This project enhances:
1. Automated Google Doc Form Setup:
   - Scanning headings, consecutive dot lines, inline dots, and table cells.
   - 2-Step Human-in-the-loop (HITL) flow: Step 1 anchor tag review; Step 2 AI unified prompt review and temperature slider.
   - Dynamic prompt generation describing anchors, anti-hallucination rules, and 1-1 target JSON schema.
   - Bilingual schemaJson compatibility (Vietnamese & English keys) preventing silent fallback bugs.
2. Dynamic Report Synthesis:
   - Deriving target JSON structure from active `TemplateSchema` summary fields.
   - Configurable `llmTemperature` (0.0 - 0.8, default 0.2) in `SecurePreferences` passed to `LlmClient`.
3. Two-Pass Clean Google Doc Export:
   - Pass 1: Replace mapped data placeholders in newly copied Google Doc.
   - Pass 2: Read output document, extract lingering `\{\{[^{}]+\}\}` tags, and wipe them to `""` via `batchUpdate replaceAllText`.
4. UI & Preferences:
   - 2-step stepper in `FormSetupDialog` adhering to `design-taste-frontend` App Screens rules.
   - Temperature slider in `FormSetupDialog` and `SettingsDialog`.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Design Documentation Sync | Update docs/10-thiet-lap-form.md, docs/05-llm.md, docs/06-google-doc-mau.md, WORK_LOG.md, SUMMARY.md | M1 | ORIGINAL_REQUEST §R1 |
| 2 | Heading Regex & Table Cell Scan | Support Roman numerals, letters, subsections, colons; transfer labels from left cell to right cell in tables | M2 | ORIGINAL_REQUEST §R2 |
| 3 | Consecutive Dot Line Merging | Merge multi-line dots/underscores under heading into single `danh_sach` field; preserve inline dots | M2 | ORIGINAL_REQUEST §R2 |
| 4 | Anchor Code Deduplication | Eliminate duplicate `ma` placeholders across candidates | M2 | ORIGINAL_REQUEST §R2 |
| 5 | Unified Prompt Generation | Auto-generate unified prompt for Phase 2 with smooth rewriting rules, field descriptions, and 1-1 JSON schema | M2 | ORIGINAL_REQUEST §R2 |
| 6 | Bilingual Schema Compatibility | Support both Vietnamese (`ten`, `truong_tong_hop`) and English (`name`, `summary_fields`) keys in `TemplateSchema.fromJson` | M2 | ORIGINAL_REQUEST §R2 |
| 7 | Temperature Storage in SecurePrefs | Store `llmTemperature` (0.0..0.8, default 0.2f) in `SecurePreferences` with secondary testable constructor | M3 | ORIGINAL_REQUEST §R3 |
| 8 | Dynamic Template Synthesis | Dynamically construct target JSON structure from active template summary fields in `ReportSynthesizer` | M3 | ORIGINAL_REQUEST §R3 |
| 9 | Temperature Pass-Through to LLM | Pass configured `temperature` from `SecurePreferences` to `ReportSynthesizer` and `LlmClient` | M3 | ORIGINAL_REQUEST §R3 |
| 10 | PlaceholderMapper Default Value | Populate all template fields in `schemaJson` with default empty string `""` in `PlaceholderMapper` | M4 | ORIGINAL_REQUEST §R4 |
| 11 | Two-Pass Clean Google Doc Export | Read copied doc, detect lingering `\{\{[^{}]+\}\}` tags, and execute Pass 2 `replaceAllText -> ""` | M4 | ORIGINAL_REQUEST §R4 |
| 12 | 2-Step FormSetupDialog UI | Step 1 anchor review -> Step 2 unified prompt review with temperature slider, anti-slop design | M5 | ORIGINAL_REQUEST §R5 |
| 13 | Temperature Slider in Settings | Material 3 slider with live badge and helper text in `SettingsDialog` | M5 | ORIGINAL_REQUEST §R5 |
| 14 | Vietnamese UI Strings | Add all new UI strings to `strings.xml` | M5 | ORIGINAL_REQUEST §R5 |
| 15 | Full Verification & Git Deployment | Run `testDebugUnitTest` and `assembleDebug`, verify 0 failures, commit and push to `origin/main` | M6 | ORIGINAL_REQUEST §Verification |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Architecture & Design Documentation Sync | Synchronize docs/10-thiet-lap-form.md, docs/05-llm.md, docs/06-google-doc-mau.md, docs/SUMMARY.md | none | COMPLETED |
| M2 | Scanner & Prompt Generator Upgrade | FormCandidateScanner, FormLlmDescriber, FormSetupService, TemplateSchema | M1 | DONE |
| M3 | Dynamic Synthesis & Temperature Config | SecurePreferences, ReportSynthesizer, MainViewModel | M2 | DONE |
| M4 | Two-Pass Clean Google Doc Export | PlaceholderMapper, GoogleDocsExporter | M2 | DONE |
| M5 | 2-Step FormSetupDialog UI & Settings | FormSetupDialog, SettingsDialog, MainViewModel, strings.xml | M2, M3 | DONE |
| M6 | Full Verification & Git Deployment | testDebugUnitTest, assembleDebug, WORK_LOG.md, SUMMARY.md, Git push | M1, M2, M3, M4, M5 | DONE |

## Interface Contracts
### FormCandidateScanner ↔ FormLlmDescriber
- `FormCandidateScanner.scanDocument(doc: Document)`: Returns `List<FormCandidate>` with merged consecutive dots into `tinHieu = "dong_cham_lien_tiep"` or `loaiGoiY = "danh_sach"`, table cells having left label as `nhanGoiY`, and inline dots separated.
- `FormLlmDescriber.describeCandidates(candidates: List<FormCandidate>, ...)`: Returns candidates with deduplicated `ma` tags.
- `FormLlmDescriber.generateUnifiedPrompt(candidates: List<FormCandidate>, templateName: String)`: Returns complete `String` system prompt for Phase 2 synthesis.

### FormSetupService ↔ TemplateSchema
- `FormSetupService.generateSchemaJson(name, fields, ...)`: Generates JSON compatible with `TemplateSchema.fromJson(jsonString)`. Supports both `"ten"` / `"name"` and `"truong_tong_hop"` / `"summary_fields"`.

### SecurePreferences ↔ ReportSynthesizer ↔ LlmClient
- `SecurePreferences.llmTemperature`: Float property in `0.0f..0.8f`, default `0.2f`.
- `ReportSynthesizer(llmClient, classifier, noteRepository, temperature = 0.2f)`: Injects `temperature` and passes it to `llmClient.chat(messages, temperature, responseJson = true)`.
- Dynamic JSON prompt derived from `schema.summaryFields`.

### PlaceholderMapper ↔ GoogleDocsExporter
- `PlaceholderMapper.mapToPlaceholders(log, template)`: Contains entries for all template fields defaulting to `""`.
- `GoogleDocsExporter.exportDailyReport(templateDocId, placeholders, ...)`:
  - Pass 1: batchUpdate with `placeholders`.
  - Pass 2: GET doc, find remaining `\{\{[^{}]+\}\}`, batchUpdate replaceAllText -> `""`.

## Code Layout
- `app/src/main/java/com/sitevoice/app/`
  - `formsetup/`: `FormCandidateScanner.kt`, `FormLlmDescriber.kt`, `FormSetupService.kt`, `DocPlaceholderInjector.kt`
  - `template/`: `TemplateSchema.kt`, `TemplateRepository.kt`, `TemplateEntity.kt`
  - `settings/`: `SecurePreferences.kt`
  - `llm/`: `ReportSynthesizer.kt`, `LlmClient.kt`, `VilaoGeminiLlmClient.kt`, `GroqLlmClient.kt`
  - `export/`: `PlaceholderMapper.kt`, `GoogleDocsExporter.kt`
  - `ui/`: `MainViewModel.kt`, `template/FormSetupDialog.kt`, `settings/SettingsDialog.kt`
- `app/src/test/java/com/sitevoice/app/`
  - `FormCandidateScannerTest.kt`, `FormLlmDescriberTest.kt`, `TemplateSchemaTest.kt`, `SecurePreferencesTest.kt`, `ReportSynthesizerTest.kt`, `PlaceholderMapperTest.kt`, `GoogleDocsExporterTest.kt`
- `docs/`
  - `10-thiet-lap-form.md`, `05-llm.md`, `06-google-doc-mau.md`, `SUMMARY.md`, `WORK_LOG.md`
