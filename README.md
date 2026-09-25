# My AI

A local, offline-first AI assistant for Android. Chat runs against an
open-weight model via [llama.cpp](https://github.com/ggerganov/llama.cpp) on
the device itself — no OpenAI or Claude API required for normal conversation.

## Status (first commit) — what's real vs. what's scaffolded

This is a genuine, compiling-in-intent Android project, not a mockup — but
building an offline local-LLM assistant with an agent framework and GitHub
integration is a multi-week effort in total. Here's the honest breakdown of
what's in this commit:

**Fully implemented and wired together:**
- Gradle/Kotlin/Compose project structure (min SDK 26, target/compile SDK 34)
- Room-backed chat persistence: conversations + messages, with search,
  rename, delete, and per-conversation history
- Chat UI: new chat, streaming display, stop generation, regenerate,
  message list — built on Compose + Material3, custom (non-ChatGPT/Claude)
  branding and color scheme
- Model manager UI: catalog, download with progress, import, delete, "use"
- Agent architecture: `Tool` / `ToolRegistry` interfaces, file
  read/write/list tools scoped to a workspace directory, and a minimal
  ReAct-style `AgentExecutor` loop (plan → tool call → observation → repeat)
- GitHub Actions: a debug-build workflow (`android-build.yml`) and a
  tag-triggered release workflow (`android-release.yml`) that clone
  llama.cpp, build with Gradle, and upload the APK as an artifact

**Real but unverified (no Android device, emulator, or network access was
available while building this — treat as a first debugging pass, not a
guarantee):**
- `app/src/main/cpp/llama_bridge.cpp` — a real JNI bridge written against
  llama.cpp's public C API (model load, tokenize, decode loop, greedy
  sampling, streaming callback). llama.cpp's API shifts between versions; if
  the commit CI fetches has renamed a symbol used here, this file will need
  a small fix. There's a `llama_bridge_stub.cpp` fallback so the app still
  compiles/runs (with a placeholder response) if llama.cpp sources aren't
  present at build time.
- The Hugging Face model URLs in `ModelCatalog` — verify these resolve
  before relying on them; I couldn't check them from this environment.
- The GitHub Actions workflows themselves — I have no tool to watch a run to
  completion or read live logs, so I can't confirm a green build yet. Check
  the **Actions** tab after this push; if a run fails, paste me the log and
  I'll fix it.

**Deliberately minimal / not yet built:**
- Model loading isn't wired from the Model Manager screen into the chat
  engine yet (`setActive()` just records the choice) — that plumbing is the
  next step once native inference is confirmed working
- Sampling is greedy-only (no temperature/top-p/top-k yet)
- No GitHub-integration screen in the app itself (repo browsing, commits,
  PRs from the phone) — the brief's CI/build side is done; in-app GitHub
  access is unbuilt
- No code-signing for release APKs (release workflow produces an **unsigned**
  APK — add a keystore + GitHub Secrets before distributing beyond
  `adb install` on your own device)
- No system-instructions/personality editor UI (settings screen is a stub)

## Project layout

```
app/src/main/java/com/myai/assistant/
  data/          Room entities, DAOs, ChatRepository
  inference/      ModelCatalog, ModelDownloader, LlamaEngine (JNI bridge)
  agent/          Tool, ToolRegistry, AgentExecutor, agent/tools/*
  ui/             chat/, sidebar/, models/, settings/, navigation/, theme/
app/src/main/cpp/  CMakeLists.txt, llama_bridge.cpp, llama_bridge_stub.cpp
.github/workflows/ android-build.yml, android-release.yml
```

## Building

CI does this for you (see below), but to build locally:
1. Open in Android Studio (Koala+ recommended) with NDK + CMake components
   installed.
2. Clone llama.cpp into `app/src/main/cpp/llama.cpp` (the workflow does this
   automatically in CI; for local builds you need to do it yourself once):
   `git clone https://github.com/ggerganov/llama.cpp app/src/main/cpp/llama.cpp`
3. Build/run as usual. No `gradlew` is committed — this repo relies on CI's
   pinned Gradle install; open in Android Studio and let it manage Gradle,
   or run `gradle wrapper` locally once to generate one.

## Getting the APK from GitHub Actions

1. Push to `main` (or trigger manually) — check the **Actions** tab.
2. On success, open the `Android Build` run → **Artifacts** → download
   `MyAI-debug.apk`.
3. Transfer to your Samsung phone and install (enable "install from unknown
   sources" for the app you use to open it).
4. Open My AI → Models → download or import a GGUF model → return to chat.

## Local model

The starter catalog (`ModelCatalog.kt`) lists Qwen2.5 3B Instruct, Llama 3.2
3B Instruct, and Phi-3.5 Mini — all in the 2-4GB Q4_K_M GGUF range, chosen as
a practical fit for phones with 6-8GB+ RAM. None of these will match the
newest cloud models' capability; they're the strongest practical fit for
on-device use today. You can also import any GGUF file you already have.

## Privacy

Conversations are stored locally (Room/SQLite on-device) and are not sent
anywhere by this app. Internet is used only for: downloading models,
optional web search / GitHub features (not yet built), and app updates via
GitHub Releases.
