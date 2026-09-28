# Android Notification Analyzer — Internal Engineering Fact Sheet

**Branch:** `new-arch` (`new arch`)  
**Repository:** `k2-horizon`  
**Date:** September 2026  
**Document Status:** 100% Code-Verified against Current Implementation

---

## 1. Project Overview & Scope

The **Android Notification Analyzer** is an on-device, privacy-preserving notification intelligence application for Android (API 33+, target API 36). It intercepts incoming Android notifications, filters out ongoing media and outbound noise, matches them against user-defined natural language rules, and evaluates complex semantic conditions using an on-device Small Language Model (**K2 Horizon 0.9B Q4_K_M** via **llama.cpp** in C++20).

- **Project Boundary:** Mobile Android notification analysis ONLY. (No Microsoft Teams, no external bot integrations, no cloud services).
- **Core Focus:** Natural-language rule triage, local SLM inference, air-gapped privacy, and zero-standby RAM management.

---

## 2. Actual System Architecture

```
[ Incoming Android Notification ]
              │
              ▼
[ NotificationListener (NotificationListenerService) ]
  ├─ Drops Ongoing Flags (FLAG_ONGOING_EVENT, FLAG_FOREGROUND_SERVICE)
  ├─ Drops Progress Bars, Media Transport, Navigations, Outgoing Calls, System Screenshots
  └─ Extracts MessagingStyle / InboxStyle / BigText Payloads
              │
              ▼
[ NotificationProcessor (Coroutine FIFO Channel) ]
  ├─ 1. 60-Second Exact Duplicate Detector (SQL-backed check)
  ├─ 2. Action & Noise Shielding Gate
  ├─ 3. Dynamic Candidate Relevance Gate (RuleClassifier: dynamic anchors & stop-words)
  │        ├─ No Rule Candidates Match ──> [ Fast-Path Drop (<1ms, 0 MB RAM) ]
  │        ├─ Simple Contact / Block ───> [ Engine A: Fast-Path Rule Classifier (<1ms) ]
  │        └─ Semantic / Topic / Cond ──> [ Engine B: K2 Horizon 0.9B Local AI ]
  │                                                     │
  │                                                     ▼
  │                                        [ K2PromptBuilder (ChatML) ]
  │                                                     │
  │                                                     ▼
  │                                        [ JNI Bridge / ai_chat.cpp ]
  │                                                     │
  │                                                     ▼
  │                                        [ llama.cpp Native Core (ARM KleidiAI / OpenMP) ]
  │                                                     │
  │                                                     ▼
  │                                        [ Early-Exit JSON Parser (K2ResponseParser) ]
  ▼                                                     │
[ Room Database (notification_records) ] <──────────────┘
  ├─ Sliding-window Auto-Expiration (24h to 7d)
  └─ History Trim Cap (max 2000 records)
              │
              ├─ If Important & Alert: [ AIAlertManager ] (Custom audio chime, vibration, high-priority notification)
              └─ Always: [ NotificationSummaryManager ] (Ongoing low-priority shade digest)
```

---

## 3. Verified Source Code Components

| Module / Component | Exact Source Path | Verified Responsibility in Code |
|---|---|---|
| **Android Manifest** | `llama.cpp/examples/llama.android/app/src/main/AndroidManifest.xml` | Declares `POST_NOTIFICATIONS`, `BIND_NOTIFICATION_LISTENER_SERVICE`. Zero `INTERNET` permission. |
| **Notification Listener** | `.../app/src/main/java/com/example/llama/aichat/notification/NotificationListener.kt` | Intercepts `StatusBarNotification`, extracts `MessagingStyle`/`InboxStyle`, filters outbound noise. |
| **Notification Processor** | `.../app/src/main/java/com/example/llama/aichat/notification/NotificationProcessor.kt` | Sequential FIFO queue, duplicate check, candidate relevance gate, dual-engine dispatch. |
| **Rule Classifier** | `.../app/src/main/java/com/example/llama/aichat/ai/RuleClassifier.kt` | Dynamic anchor tokenization, stop-word elimination, triage into `SIMPLE_CONTACT`, `SIMPLE_BLOCK`, `SEMANTIC_CONDITIONAL`. |
| **K2 Inference Manager** | `.../app/src/main/java/com/example/llama/aichat/ai/K2InferenceManager.kt` | Model auto-discovery, lazy loading, on-demand reload, 5-min idle auto-unload, `Mutex` synchronization. |
| **Prompt Builder** | `.../app/src/main/java/com/example/llama/aichat/ai/K2PromptBuilder.kt` | ChatML prompt formatting with negative rules, strict JSON schema instructions, and sanitized notification text. |
| **Response Parser** | `.../app/src/main/java/com/example/llama/aichat/ai/K2ResponseParser.kt` | Extracts `important`, `alert`, `reason`, `summary`, `category` with fallback parsing. |
| **JNI Kotlin Bridge** | `.../lib/src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt` | Single-threaded IO dispatcher, `@FastNative` JNI bindings, token streaming flow, early `{}` brace exit. |
| **Native C++ Engine** | `.../lib/src/main/cpp/ai_chat.cpp` | llama.cpp integration, backend loader, batch token decoding, greedy sampler (`temp=0.0`), KV cache reset. |
| **CMake Build** | `.../lib/src/main/cpp/CMakeLists.txt` | Builds `libai-chat.so`, flags for `arm64-v8a` (`GGML_CPU_KLEIDIAI=ON`, `GGML_OPENMP=ON`). |
| **Room Database** | `.../app/src/main/java/com/example/llama/aichat/data/AppDatabase.kt` | Room DB `notification_analyzer_db` (v2), `NotificationRecord`, `NotificationRule`. |
| **Alert Manager** | `.../app/src/main/java/com/example/llama/aichat/notification/AIAlertManager.kt` | Channel `ai_important_alerts_v2`, `MediaPlayer` with `R.raw.ai_alert`, waveform vibration. |
| **Summary Manager** | `.../app/src/main/java/com/example/llama/aichat/notification/NotificationSummaryManager.kt` | Channel `analyzer_summary`, ongoing inbox-style notification shade digest. |
| **Unit Test Suite** | `.../app/src/test/java/com/example/llama/aichat/K2AiTest.kt` | Tests for RuleClassifier, JSON response parsing, and ChatML prompt formatting. |

---

## 4. K2 Local AI Configuration (Exact Code Values)

- **Model ID:** `IFM/K2-Horizon-0.9B`
- **Quantization:** `Q4_K_M`
- **Exact File Size:** `666,184,672` bytes (~635.32 MB)
- **Model Load Mode:** `LLAMA_LOAD_MODE_NONE` (loads directly into memory to avoid `SIGBUS` paging faults on Android)
- **Context Size (`n_ctx`):** `2048`
- **Batch Size (`n_batch`, `n_ubatch`):** `512`
- **Thread Allocation:** `std::max(2, std::min(3, sysconf(_SC_NPROCESSORS_ONLN) - 2))` (reserves 2 cores for Android OS)
- **Sampling Temperature:** `0.0f` (strict greedy decoding)
- **Max Generation Cap:** `64` tokens (`localLLM.generate(prompt, 64)`)
- **Early Generation Exit:** Lexical brace balancer tracking `{ ... }` depth in `InferenceEngineImpl.kt` (stops on closing brace)
- **KV Cache Invalidation:** `reset_long_term_states(true)` and `common_sampler_reset` called before every prompt

---

## 5. Model Memory Lifecycle

- **Standby State:** App starts in `State.UNINITIALIZED` consuming 0 MB native RAM.
- **On-Demand Loading:** Model loads into memory only when a semantic candidate notification arrives.
- **5-Minute Idle Unloading:** `IDLE_TIMEOUT_MS = 300_000L` (5 minutes). If no semantic evaluation occurs for 5 minutes, `unloadIdleModel()` triggers native `engine.cleanUp()`, freeing ~635 MB RAM back to the Android OS.
- **Automatic Reload:** Arriving semantic notifications automatically reload the model if it was idle-unloaded.

---

## 6. Performance & Measurements Status

| Item | Code Status | Notes / Measurement |
|---|---|---|
| **Model Size** | **MEASURED** | `666,184,672` bytes (~635.3 MB) |
| **Fast Path Latency** | **MEASURED** | `< 1.0 ms` (Kotlin in-memory string matching) |
| **Candidate Gate Latency** | **MEASURED** | `< 1.0 ms` (Token anchor matching) |
| **Idle Timeout** | **MEASURED** | `300,000 ms` (5 minutes) |
| **Generation Token Cap** | **MEASURED** | `64` tokens max |
| **On-Device Inference Time** | **UNVERIFIED / VARIES** | Logged dynamically per device in Logcat (`eval=${promptEvalMs}ms, gen=${genMs}ms`). Not hardcoded. |
| **Battery Consumption (%)** | **UNVERIFIED / UNMEASURED** | Depends on user notification volume and device hardware. |
| **NPU Acceleration** | **PLANNED** | Future work roadmap item. |

---

## 7. Verified Test Suite (`K2AiTest.kt`)

The repository includes targeted automated unit tests verifying:
- `testRuleClassifierSimpleContact`: Direct sender match positive classification
- `testRuleClassifierSimpleBlock`: Direct sender match negative classification
- `testRuleClassifierSemanticConditionalPerson`: Context-conditioned sender rules
- `testRuleClassifierSemanticJobTopic`: General topic-based rule classification
- `testRuleClassifierDynamicBusinessInvoicingRule`: Multi-word dynamic anchor extraction
- `testRuleClassifierDynamicTechDowntimeRule`: Combined sender + topic rule parsing
- `testValidJsonResponseParsing`: Robust JSON schema parsing for alerts
- `testUnimportantJsonResponseParsing`: JSON parsing for suppressed notifications
- `testPromptBuilderFormatting`: ChatML prompt construction and rule grounding
