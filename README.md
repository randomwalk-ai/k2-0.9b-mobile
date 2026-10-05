# K2 Horizon: On-Device Intelligent Edge Notification Assistant

An ultra-efficient, privacy-first on-device AI notification filtering and routing engine for Android powered by **K2 Horizon (0.9B)** and **llama.cpp**. 

K2 Horizon runs entirely offline on the edge—delivering sub-millisecond deterministic filtering for high-throughput notifications, and seamless on-demand LLM semantic & emotional reasoning when deep context is required. Zero battery drain, zero cloud dependency, and total user privacy.

---

## Architecture Overview

```
                         Incoming Android Notification
                                       │
                                       ▼
                   ┌───────────────────────────────────────┐
                   │       NotificationProcessor           │
                   │   (Dynamic Extraction & Parsing)      │
                   └───────────────────┬───────────────────┘
                                       │
                                       ▼
                   ┌───────────────────────────────────────┐
                   │    Compiled Rule Execution Engine     │
                   │           (Room DB Cache)             │
                   └───────┬───────────────────────┬───────┘
                           │                       │
              [Tier 1: AOT_FAST]              [Tier 2: K2_DEEP]
                           │                       │
                           ▼                       ▼
            ┌─────────────────────────────┐ ┌─────────────────────────────┐
            │   AOT Fast Pattern Engine   │ │     K2 Horizon 0.9B LLM     │
            │  - Keyword & Contact Match  │ │   (llama.cpp 4-bit Engine)  │
            │  - App Domain Classification│ │  - Emotion / Tone Analysis  │
            │  - Exclusion Hierarchy Rules│ │  - Deep Semantic Reasoning │
            │      Latency: < 0.2ms       │ │      Latency: ~150-300ms    │
            └──────────────┬──────────────┘ └──────────────┬──────────────┘
                           │                               │
                           └───────────────┬───────────────┘
                                           │
                                           ▼
                            Final Routing Decision
                     (Alert / Priority / Mute / Suppress)
```

### Dual-Tier Hybrid Execution Pipeline

1. **Ahead-Of-Time (AOT) Fast Engine (`AOT_FAST`)**:
   - Executes in **< 0.2ms** with negligible CPU and battery footprint.
   - Evaluates deterministic criteria: Target App IDs, Contact Names, Exact & Substring Domain Anchors, Negation/Exclusion hierarchy.
2. **K2 Horizon Deep Reasoning Engine (`K2_DEEP`)**:
   - Invoked dynamically only when notifications require subjective emotional evaluation, hostility/anger detection, or nuanced situational understanding.
   - Powered by `llama.cpp` mobile runtime (ARM NEON / OpenCL accelerated).
   - Automatically reloads on-demand and idles after inactivity to optimize memory.

---

## Supported Rule Types

K2 Horizon supports 7 distinct rule intent categories compiled automatically from natural language:

| Rule Type | Intent | Engine | Example |
| :--- | :--- | :--- | :--- |
| **`APP_FILTER`** | Target specific mobile apps | `AOT_FAST` | *"Any message from Slack or Teams is important"* |
| **`TOPIC_FILTER`** | Cross-app domain & topic matching | `AOT_FAST` | *"Alert any message regarding OTP, delivery, or server outage"* |
| **`SIMPLE_CONTACT`** | All messages from a contact | `AOT_FAST` | *"Any message from Alice is important"* |
| **`SIMPLE_BLOCK`** | Silence specific contacts or channels | `AOT_FAST` | *"Block Bob"*, *"Mute promotional spam"* |
| **`CONDITIONAL_CONTACT`** | Contact matching with topic exclusions | `AOT_FAST` | *"Alice is important, but if she sends reels or memes mute it"* |
| **`CONDITIONAL_EMOTION`** | Tone, mood, and emotion triggers | `K2_DEEP` | *"Charlie when angry is not important"*, *"Alert if boss sounds furious"* |
| **`MULTI_CONDITION`** | Compound rules with layered exclusions | `AOT_FAST` / `K2_DEEP` | *"Food delivery alerts from Swiggy except promotional offers"* |

---

## Model Download & Setup

The system is optimized for **K2 Horizon 0.9B** in 4-bit quantization (`Q4_K_M`), but is compatible with any standard `.gguf` mobile model (such as Qwen 2.5, Gemma 2, LLaMA 3.2, TinyLlama).

### 1. Download Pre-Quantized Model (Recommended)

Download the pre-quantized `k2-horizon-0.9b-q4_k_m.gguf` (~635 MB):

```bash
# Example download using Hugging Face CLI or direct link
huggingface-cli download <hf-repo-id>/k2-horizon-0.9b-GGUF k2-horizon-0.9b-q4_k_m.gguf --local-dir ./
```

### 2. Manual GGUF Conversion from BF16 / FP16 (Optional)

If starting from original Hugging Face weights in BF16:

```bash
# 1. Clone llama.cpp
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp

# 2. Convert PyTorch/Safetensors to full-precision GGUF
python convert_hf_to_gguf.py /path/to/k2-horizon-0.9b/ --outfile k2-horizon-0.9b-f16.gguf

# 3. Quantize to 4-bit medium precision (Q4_K_M)
./llama-quantize k2-horizon-0.9b-f16.gguf k2-horizon-0.9b-q4_k_m.gguf Q4_K_M
```

### 3. Deploy Model to Android Device

Push the `.gguf` file to your Android device via ADB:

```bash
# Push to standard device Downloads folder (auto-discovered on launch)
adb push k2-horizon-0.9b-q4_k_m.gguf /sdcard/Download/

# Alternatively, import via the in-app file picker:
# App Settings -> "Import GGUF Model"
```

---

## Building & Running the Android App

### Prerequisites
- **Android Studio** (Hedgehog 2023.1.1+ / Ladybug or newer)
- **Android SDK** (API 34 / Android 14)
- **NDK** (Version 26.1.10909125 or higher)
- **CMake** (3.22.1+)
- **JDK 17+**

### Command-Line Build

```bash
# Navigate to the Android project root
cd llama.cpp/examples/llama.android

# Run unit and rule compilation test suites
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Initial Android Device Setup
1. Launch the **K2 Horizon** app.
2. Grant **Notification Listener Permission** when prompted (Settings > Apps > Special App Access > Notification Access).
3. The app will auto-discover the `k2-horizon-0.9b-q4_k_m.gguf` file in `/sdcard/Download/` and initialize the local runtime.
4. Input natural language rules in the Rules screen (e.g. *"Teams notifications are important"* or *"Charlie when angry is not important"*).

---

## Performance & Benchmarks

- **AOT Fast Engine Throughput**: > 5,000 notifications / second (< 0.2ms latency per notification).
- **K2 Deep Inference**: ~150ms – 300ms time-to-first-token on modern mobile ARM SoCs (Snapdragon 8 Gen 2/3, Dimensity 9300, Google Tensor G3/G4).
- **RAM Footprint**: ~680 MB during active inference; drops to zero during idle unload.
- **Accuracy**: Tested across a rigorous 1,000-case automated test suite across all 7 rule intents with 100% pass rate.

---

## Project Structure

```
k2-horizon/
├── benchmarks/                     # Python latency, accuracy & compilation test benches
│   ├── benchmark_engine.py         # Multi-model benchmarking harness
│   ├── test_cases_1000.json        # 1000-case evaluation dataset
│   ├── run_phase1_compilation.py   # Rule compiler test suite
│   └── run_phase2_benchmark_500.py # 500-sample end-to-end benchmark
│
├── llama.cpp/examples/llama.android/
│   ├── app/src/main/java/com/example/llama/aichat/
│   │   ├── ai/
│   │   │   ├── K2InferenceManager.kt  # On-device llama.cpp lifecycle & loader
│   │   │   ├── K2PromptBuilder.kt     # System prompts & few-shot compiler prompts
│   │   │   ├── K2ResponseParser.kt    # Structured JSON response parser
│   │   │   └── RuleCompiler.kt        # Edge AI rule compiler
│   │   ├── notification/
│   │   │   ├── NotificationListener.kt  # Android NotificationListenerService
│   │   │   └── NotificationProcessor.kt # Hybrid dual-tier routing processor
│   │   ├── data/                      # Room Database (Rules & Notifications)
│   │   └── ui/                        # Jetpack Compose UI Screens & Navigation
│   └── app/src/test/                  # JVM Unit Tests (1000-case test suites)
```

---

## Testing

To run the full suite of unit tests:

```bash
cd llama.cpp/examples/llama.android
./gradlew test
```

---

## License & Contribution Policy

This repository is maintained as a standalone open-source project. External pull requests and direct external contributions are not currently accepted. 

Licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for details.
