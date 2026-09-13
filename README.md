# 🎙️ Myvoice — your personal AI thinking assistant

**Say it. Think it through. Keep it.**

Myvoice is an end-to-end Android app that turns your voice into a thinking partner.
Tap the mic, speak your mind, and get a thoughtful, structured reflection back — optionally
grounded in live web search. Every thought is saved to a searchable local history you own,
with optional encrypted-at-rest-style cloud sync to your own Appwrite database.

[![Android CI](https://github.com/sameerswain2026-code/Myvoice/actions/workflows/android-ci.yml/badge.svg)](https://github.com/sameerswain2026-code/Myvoice/actions/workflows/android-ci.yml)

---

## ✨ Features

| | |
|---|---|
| 🎤 **Tap-to-talk** | One big button. Records 16 kHz mono audio or uses on-device recognition. |
| 🗣️ **3 speech-to-text engines** | **Device** (free, offline), **Deepgram** (English, `nova-2`), **Sarvam AI** (Indic languages — Hindi, Odia, Tamil, Telugu, Bengali… via `saaras:v3`, with translate-to-English mode). |
| 🧠 **Gemini thinking partner** | A Socratic, no-flattery reflection engine with a structured reply format ("What I hear" → insights → next smallest step → questions to sit with). Model configurable (default `gemini-2.0-flash`). |
| 🌐 **Web-grounded answers** | The LLM decides when the web helps; **Tavily** fetches results; replies cite sources `[1] [2]`. |
| 🗂️ **Thought history** | Everything saved with auto title + tags (LLM-generated, offline keyword fallback). Full-text search, favorites, detail view. |
| 🔊 **Read aloud** | Built-in Android TTS with auto-play option. |
| ☁️ **Own your data** | Local-first Room database + optional two-way sync with your own Appwrite project. Markdown export & one-tap clear. |
| 🎨 **Material 3** | Dynamic color, dark mode, edge-to-edge, min SDK 26 (Android 8.0+). |

## 🏗️ Architecture

```
┌─────────────────────────── app (Kotlin, Compose, single module) ───────────────────────────┐
│  ui/            Compose screens (Talk, History, Detail, Settings) + ViewModels            │
│  domain/        ThinkingEngine (LLM + web-search loop), metadata generator, keyword tagger │
│  audio/         AudioRecorder (AudioRecord PCM) + WavCodec (RIFF encoder)                  │
│  speech/        DeviceSpeechRecognizer (SpeechRecognizer), TtsManager                      │
│  data/remote/   GeminiClient · DeepgramClient · SarvamClient · TavilyClient · AppwriteSync │
│  data/db/       Room (Thought entity, search, favorites)                                   │
│  data/prefs/    DataStore settings (providers, keys, persona, sync config)                 │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

**The thinking loop:** `mic → PCM → WAV → STT → ThinkingEngine → Gemini reply → auto title/tags → Room → optional Appwrite sync`. If web search is enabled, the engine first asks Gemini *"would searching help?"* (strict JSON), runs Tavily for the query, and feeds the results into the final reply with inline citations.

All networking is plain OkHttp + kotlinx.serialization over the official REST APIs — no heavy SDKs, fully inspectable.

## 🔑 API keys (all optional, all yours)

Everything degrades gracefully: with **zero keys** you still get device speech recognition, typed thoughts, local history and offline tagging. Add keys in **Settings** inside the app:

| Capability | Provider | Get a key | Where in Settings |
|---|---|---|---|
| Thinking replies | Google **Gemini** | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) | Thinking model |
| STT (English) | **Deepgram** | [console.deepgram.com](https://console.deepgram.com) | Speech-to-text |
| STT (Indic: hi, od, ta, te, bn…) | **Sarvam AI** | [dashboard.sarvam.ai](https://dashboard.sarvam.ai) | Speech-to-text |
| Web search | **Tavily** | [app.tavily.com](https://app.tavily.com) | Thinking model |
| Cloud sync | **Appwrite** | [cloud.appwrite.io](https://cloud.appwrite.io) | Cloud sync |

Keys are stored only in the app's private DataStore and sent only to the provider you configured.

### One-time Appwrite setup (optional)

1. Create a project → **Databases** → create a database → create a collection (e.g. `thoughts`).
2. Add string attributes: `title` (255), `transcript` (50000), `reply` (50000), `sttProvider` (32), `llmModel` (64), `source` (16), `createdMs` (32), `updatedMs` (32), `durationMs` (32); a **string array** attribute `tags`; a **boolean** `favorite`.
3. Create an API key (scopes: `documents.read`, `documents.write`) and paste endpoint / project ID / database ID / collection ID / key into the app, then hit **Save & sync now**.

## 🚀 Getting the app

### Option A — download a ready APK (no tooling needed)

1. Open the repo's **Actions → Android CI** tab.
2. Open the latest green run → **Artifacts → `MyVoice-APKs`**.
3. Unzip, sideload `app-debug.apk` (or the R8-optimized `app-release.apk`) onto your phone.
4. Tag a release (`v1.0.0`) and CI additionally publishes a GitHub Release with the APK attached.

### Option B — build locally

```bash
git clone https://github.com/sameerswain2026-code/Myvoice.git
cd Myvoice
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests
```

Or open the folder in **Android Studio** (Hedgehog+) and press Run. Requires JDK 17.

> The release build is signed with the debug keystore for easy sideloading. For Play Store publishing, add a real `signingConfig` in `app/build.gradle.kts`.

## 🧪 Tests

Pure-JVM unit tests cover the WAV encoder, offline keyword tagger, the LLM search-decision parser, context building, and Markdown export — run via `./gradlew testDebugUnitTest` (CI runs them on every push).

## 🔒 Privacy

- Audio and text go **only** to the providers you configure (Gemini/Deepgram/Sarvam/Tavily).
- Thoughts live on-device; cloud sync is opt-in and goes to **your own** Appwrite project.
- No analytics, no trackers, no third-party data collection.

## 🗺️ Roadmap

- Streaming STT (Deepgram websocket) with live partials for cloud mode
- Sarvam Bulbul TTS voices (Indic languages) alongside Android TTS
- Weekly reflection digests ("what you were thinking about this time last month")
- Widgets & Wear OS quick capture
- Real user auth via Appwrite (multi-device sync)

## 📄 License

MIT — see [LICENSE](LICENSE). See [SPEC.md](SPEC.md) for the full product specification.
