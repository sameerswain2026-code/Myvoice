# Myvoice — Product Specification (v1.0)

*Personal AI Thinking Assistant · Android*

## 1. Vision

Everyone thinks out loud. Myvoice captures that stream — by voice — and turns it into
structured thinking. Speak a half-formed idea; get back a focused reflection that helps you
sharpen it, plus a permanent, searchable memory of what you were thinking and why.
The product is local-first, private by default, and bring-your-own-key.

## 2. Target user

- People who think by talking: founders, students, writers, researchers.
- India-first: strong support for Indic languages (Odia, Hindi, Tamil, Telugu, Bengali…) via Sarvam AI, with Deepgram for English and a free on-device fallback everywhere.

## 3. Core user flows

### 3.1 Talk (primary loop)
1. User taps the mic orb. (If permission missing → runtime prompt.)
2. Recording starts: pulsing ring + timer for cloud providers; live partial transcript for device recognition.
3. Tap again to stop → WAV is built (16 kHz mono PCM).
4. Transcription: chosen provider (Device / Deepgram / Sarvam). Transcript lands in an editable field — the user can fix hearing mistakes.
5. Tap **Think with me** → ThinkingEngine runs:
   - optional web-search decision (strict-JSON LLM call) → Tavily search → cited results;
   - Gemini reply in the thinking-partner format;
   - auto title + tags (LLM JSON, offline keyword fallback);
   - thought saved to Room.
6. Reply appears in a card with **read aloud / stop / copy**. Optional auto-speak.

### 3.2 History
- Reverse-chronological cards: title, transcript snippet, tags, timestamp, voice icon.
- Instant full-text search across titles, transcripts, replies.
- One-tap favorite / delete.

### 3.3 Detail
- Full transcript + full reflection, metadata line (time, voice-note flag, model used).
- Actions: read aloud, stop, copy, share (system sheet), favorite, delete (with confirm), back.

### 3.4 Settings
- **Speech-to-text**: provider chips (Device/Deepgram/Sarvam) + provider fields (key, model, Sarvam transcribe-vs-translate mode).
- **Thinking model**: Gemini key + model name, personalization prompt, web-search toggle, Tavily key.
- **Voice output**: auto-speak toggle.
- **Cloud sync (Appwrite)**: endpoint, project, database, collection, API key, "Save & sync now" with human summary ("Pushed 3 · Updated 0 · Pulled 1").
- **Your data**: count, Markdown export (share sheet), clear-all (with confirm).
- Single **Save** action persists the whole draft.

## 4. Functional requirements

| ID | Requirement |
|---|---|
| F1 | Record voice notes; minimum recording quality 16 kHz mono 16-bit PCM. |
| F2 | Transcribe via device recognizer or Deepgram or Sarvam (user-selectable). |
| F3 | Editable transcript before thinking. |
| F4 | LLM reply via Gemini REST (`v1beta:generateContent`), model configurable. |
| F5 | Web-search tool: LLM decides (strict JSON), Tavily executes, citations inline. |
| F6 | Persist every completed thought: title, transcript, reply, tags, timestamps, source, provider, model, duration, favorite, sync flag. |
| F7 | Full-text search; favorites; delete; detail view; share; Markdown export. |
| F8 | TTS read-aloud (reply text cleaned of markdown), optional auto-play. |
| F9 | Two-way Appwrite sync: push unsynced (create-or-update by documentId = thought UUID), pull remote docs newer than local. |
| F10 | All cloud features degrade gracefully without keys; clear in-app errors on failure (auth, quota, network). |

## 5. Non-functional requirements

- **Offline-first**: device STT + typed thoughts + history work with no network; cloud strictly opt-in via keys.
- **Privacy**: keys only in app-private DataStore; data only to user-configured providers; no analytics/trackers.
- **Performance**: cold start < 2 s on mid-range devices; recording starts < 300 ms after tap.
- **Reliability**: network calls on IO dispatcher with timeouts (20 s connect / 120 s read-write); per-item sync failures don't abort the batch.
- **Compatibility**: minSdk 26 (Android 8.0), targetSdk 34, phones, portrait-first.
- **Quality**: JVM unit tests for codec, tagging, protocol parsing, export; CI on every push.

## 6. API integration contract

| Provider | Endpoint | Auth | Notes |
|---|---|---|---|
| Gemini | `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` | `x-goog-api-key` header | `systemInstruction`, `responseMimeType: application/json` for JSON modes. |
| Deepgram | `POST https://api.deepgram.com/v1/listen?model=nova-2&smart_format=true&punctuate=true` | `Authorization: Token <key>` | Body: `audio/wav`. Response: `results.channels[0].alternatives[0].transcript`. |
| Sarvam | `POST https://api.sarvam.ai/speech-to-text` | `api-subscription-key` header | multipart `file` (WAV) + `model` (`saaras:v3`) + optional `mode` (`transcribe`/`translate`). Response: `{transcript}`. |
| Tavily | `POST https://api.tavily.com/search` | `Authorization: Bearer <key>` | Body `{query, search_depth, max_results, include_answer}`. |
| Appwrite | `POST/PATCH/GET {endpoint}/databases/{db}/collections/{col}/documents` | `X-Appwrite-Project`, `X-Appwrite-Key` | Attributes: `title, transcript, reply, tags[], createdMs, updatedMs, favorite, source, sttProvider, llmModel, durationMs` (timestamps stored as strings). |

## 7. Data model (Room `thoughts`)

`id` UUID (also Appwrite documentId) · `title` · `transcript` · `reply` · `tags[]` (JSON-encoded) · `createdAt`/`updatedAt` epoch ms · `isFavorite` · `source` (VOICE/TEXT) · `sttProvider` · `llmModel` · `durationMs` · `synced`.

## 8. Release plan

- CI builds debug + R8 release APKs on every push; artifacts downloadable per run.
- Git tags `vX.Y.Z` produce GitHub Releases with the APK attached.
- v1.1+: streaming STT, Sarvam Bulbul TTS, reflection digests, auth-based multi-device sync.

## 9. Out of scope (v1)

User accounts, iOS, tablet layouts, background/lock-screen recording, end-to-end encryption of sync, on-device LLM.
