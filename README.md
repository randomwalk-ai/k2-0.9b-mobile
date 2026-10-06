# K2 Horizon: On-Device Intelligent Edge Notification Assistant

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Model](https://img.shields.io/badge/Model-IFM%2FK2--Horizon--0.9B-purple.svg)](https://huggingface.co/IFM/K2-Horizon-0.9B)
[![Runtime](https://img.shields.io/badge/Runtime-llama.cpp_Mobile-orange.svg)](https://github.com/ggerganov/llama.cpp)

An ultra-efficient, privacy-first on-device AI notification filtering and alert engine for Android powered by **K2 Horizon (0.9B)** and **llama.cpp**.

---

## The Problem & The Solution

Every day, phones are flooded with notification noise. When busy at work, in meetings, or sleeping at night, muting notifications is the easiest fix—but you risk missing critical alerts that actually matter.

**K2 Horizon Notification Assistant** allows users to define priority rules in natural plain English. When an incoming notification matches your criteria, the app triggers a **high-priority media sound chime and custom vibration—even if your phone's notification volume is muted or set to zero**. 

Everything runs **100% locally on-device**: zero cloud API calls, zero telemetry, and zero privacy compromise. Furthermore, sensitive notification history stored locally is excluded from Android Auto Backup / cloud backup and device-to-device transfer through explicit app backup rules.

---

## Architecture & System Design

```
 ┌────────────────────────────────────┐
 │ 1. Add a Rule                      │
 │                                    │
 │  📝 Write rule in plain English   │
 │              │                     │
 │              ▼                     │
 │  🧠 K2 Horizon AI reads it once    │
 │              │                     │
 │              ▼                     │
 │  💾 Saves as Simple / Complex Rule │
 └──────────────────┬─────────────────┘
                    │
                    │ (Uses saved rules)
                    ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │ ⚙️ AI Rule Engine                                                      │
 │                                                                        │
 │                      🔍 Which type of rule matches? ◄── [ 2. Notification ]
 │                                    │                    [    Arrives      ]
 │                  ┌─────────────────┴─────────────────┐                 │
 │                  ▼                                   ▼                 │
 │  ⚡ Simple Rule (Apps, Names, Keywords) 🧠 Complex Rule (Tone, Emotion)│
 │  ┌─────────────────────────────────┐   ┌─────────────────────────────┐ │
 │  │      Fast Classifier (<0.2ms)   │   │       K2 Horizon AI         │ │
 │  │    • Zero heat                  │   │   • Runs on-demand for      │ │
 │  │    • Zero battery drain         │   │     deep reasoning          │ │
 │  └────────────────┬────────────────┘   └──────────────┬──────────────┘ │
 │                   │                                   │                │
 │                   └─────────────────┬─────────────────┘                │
 │                                     │                                  │
 │                                     ▼                                  │
 │                            ❗️ Important?                              │
 └─────────────────────────────────────┬──────────────────────────────────┘
                                       │
                    ┌──────────────────┴──────────────────┐
               [ YES ]                                 [ NO ]
                    │                                     │
                    ▼                                     ▼
 ┌────────────────────────────────────┐ ┌─────────────────────────────────┐
 │ 3. Result & Action                 │ │ 3. Result & Action              │
 │                                    │ │                                 │
 │ 🔊 High-Priority Alert             │ │ 🔕 Silence Quietly              │
 │    (Sound Chime & Vibrate)         │ │    (Muted)                      │
 └────────────────────────────────────┘ └─────────────────────────────────┘
```

### Ahead-of-Time (AOT) Rule Compilation Architecture

Running model inference on every incoming notification was fundamentally the wrong design for an always-on mobile service. To eliminate battery drain and device heating, K2 Horizon acts as an **Ahead-of-Time (AOT) Rule Compiler**. The language model primarily runs when you create or edit a rule, translating plain-English intent into deterministic matching logic, with on-demand inference for rules that require semantic reasoning:

1. **1. Add a Rule (AOT Compilation)**:
   - When you write a rule in plain English (e.g., *"if any msg from madhu it is important, if she sends reels it is not important"*), K2 Horizon reads it **once**.
   - It compiles the natural language intent into a structured JSON schema and saves it as either a **Simple Rule** or a **Complex Rule**.
2. **2. Notification Arrives (AI Rule Engine)**:
   - **Simple Rules (Apps, Names, Keywords, Exclusions)**: Evaluated instantly by the **Fast Classifier (`<0.2 ms`)** in native memory with zero heat and zero battery drain.
   - **Complex Rules (Tone, Emotion, Context)**: If and only if a rule requires sentiment or emotional nuance (e.g., *"any message from pranav when he his angry it is not important"*), the notification is routed to **K2 Horizon AI** on-demand for deep reasoning.
3. **3. Result & Action**:
   - **High-Priority Alert**: Triggers a sound chime and custom vibration even if phone notification volume is muted or set to zero.
   - **Silence Quietly**: Silences non-priority notifications without interruption.

---

## Model Setup & Installation

The mobile app runs the 4-bit quantized **K2 Horizon 0.9B (`Q4_K_M`, ~635 MB)** model via the embedded `llama.cpp` C++ engine.

### Step 1: Download the Model

Download the pre-converted `k2-horizon-0.9b-q4_k_m.gguf` file:

```bash
# Download using Hugging Face Hub CLI
huggingface-cli download IFM/K2-Horizon-0.9B-GGUF k2-horizon-0.9b-q4_k_m.gguf --local-dir ./
```

> **Manual Conversion from BF16 (Optional):**  
> If starting from the base [IFM/K2-Horizon-0.9B](https://huggingface.co/IFM/K2-Horizon-0.9B) repository:
> ```bash
> # 1. Convert Hugging Face weights to GGUF
> python convert_hf_to_gguf.py path/to/K2-Horizon-0.9B --outfile k2-horizon-0.9b-f16.gguf
>
> # 2. Quantize to 4-bit (Q4_K_M)
> ./llama-quantize k2-horizon-0.9b-f16.gguf k2-horizon-0.9b-q4_k_m.gguf Q4_K_M
> ```

---

### Step 2: Push Model to Your Android Device

Place the `.gguf` model file on your Android device using either method:

#### Method A: Direct ADB Push (Recommended)
```bash
# Push directly into the device's Download folder (auto-discovered by app on launch)
adb push k2-horizon-0.9b-q4_k_m.gguf /sdcard/Download/
```

#### Method B: In-App File Importer
1. Transfer `k2-horizon-0.9b-q4_k_m.gguf` to any folder on your device.
2. In the app settings screen, tap **"Import GGUF Model"** and select the file using the Android system file picker.

---

## Installation & Quick Start

### 1. Build & Install APK

Run directly from your terminal:

```bash
# Clone the repository
git clone https://github.com/randomwalk-ai/k2-0.9b-mobile.git
cd k2-0.9b-mobile/llama.cpp/examples/llama.android

# Build the APK
./gradlew assembleDebug

# Install on your connected Android device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

*(Alternatively, open the project in Android Studio and click **Run ▶**)*

### Initial Device Permissions
1. Open **K2 Horizon** on your Android device.
2. Grant **Notification Listener Permission** when prompted (`Settings > Apps > Special App Access > Notification Access`).
3. The app will auto-discover the model and show `Model Ready`.

---

## Benchmarks & Performance

### 1. 500-Test-Case Stress Benchmark (K2 Horizon 0.9B vs. Llama 3.2 1B)

Evaluated across 500 complex test cases covering emotional tones (sarcasm, passive aggressive frustration, anger), urgent escalations, conditional exclusions, and app-specific rules. The reported accuracy reflects end-to-end performance of our notification-analysis pipeline using each model’s compiled rules, deterministic matching, and semantic escalation where applicable.

| Metric | K2 Horizon 0.9B | Llama 3.2 1B | Margin |
| :--- | :---: | :---: | :---: |
| **Overall Triage Accuracy** | **66.2%** (331/500) | 41.0% (205/500) | **+25.2%** |
| **AOT Rule Compilation (15 Complex Rules)** | **86.7%** (13/15) | 26.7% (4/15) | **+60.0%** |
| **Deep AI Emotional Nuance (250 Tone Cases)** | **58.4%** | 48.4% | **+10.0%** |

*Note: These results come from our internal notification-analysis benchmark and should not be interpreted as a general ranking of K2 Horizon 0.9B versus Llama 3.2 1B across language-model tasks. In accordance with system semantics, suppression actions (IGNORE ≡ MUTE) are normalized during evaluation.*

- **Key Finding**: Between K2 Horizon 0.9B and Llama 3.2 1B, K2 proved to be the better fit for our notification-analysis workload. As a dedicated reasoning model, its reasoning-oriented design and strict instruction following allow it to reliably compile complex natural language rules into structured schemas (86.7%) and decipher subtle human emotions like passive aggressive frustration, whereas Llama 3.2 1B frequently produced malformed schemas or misclassified dismissiveness in our evaluation.

#### Reproducing the 500-Case Benchmark

The repository includes the exact Phase 1 compiled-rule artifact ([`benchmarks/compiled_rules_phase1.json`](benchmarks/compiled_rules_phase1.json)) used by the Phase 2 runtime benchmark.

To reproduce the published 500-case evaluation results from a clean checkout, run the following commands from the repository root:

```bash
# 1. Install benchmark dependencies
pip install torch transformers llama-cpp-python

# 2. Download the Llama 3.2 1B Instruct GGUF model into the repository root (K2 Horizon downloads automatically from Hugging Face)
huggingface-cli download lmstudio-community/Llama-3.2-1B-Instruct-GGUF Llama-3.2-1B-Instruct-bf16.gguf --local-dir ./

# 3. Run the Phase 2 benchmark runner from the repository root
python benchmarks/run_phase2_benchmark_500.py
```

---

### 2. 5-Day Daily-Driver Testing

Tested on a physical **8GB RAM Android device** under regular daily use:

- **Test Duration**: 5 Continuous Days with 7 active natural language rules.
- **Notification Volume**: ~500 notifications/day (~2,500 total processed from WhatsApp, Microsoft Teams, Instagram, and phone calls).
- **Observations**: We observed only 2–3 minor edge cases across approximately 2,500 notifications.
- **Thermals & Battery**: No measurable additional standby battery drain and no noticeable thermal buildup during our 5-day test.
- **Latency**: `< 0.2 ms` for deterministic rules; `~150–300 ms` for on-demand deep reasoning.

---

## Supported Rule Types

| Rule Intent | Engine | Natural Language Example |
| :--- | :--- | :--- |
| **`Fast Contact`** | Fast Classifier (`<0.2ms`) | *"if pranav calls me it is important"* |
| **`AOT Topic Filter`** | Fast Classifier (`<0.2ms`) | *"anyone msges about playing cricket it is important"* |
| **`AOT Conditional`** | Fast Classifier (`<0.2ms`) | *"if any msg from madhu it is important, if she sends reels it is not important"* |
| **`Deep AI Emotion`** | K2 Horizon AI (`On-Demand`) | *"any message from pranav when he his angry it is not important"*, *"Alert if boss sounds furious"* |

---

## Project Structure

```
k2-0.9b-mobile/
├── README.md
├── LICENSE
├── blog/                              # Web blog static deployment bundle
│   ├── index.html                     # Case study article & interactive post
│   ├── post.html                      # Standalone article post
│   └── static/images/                 # System architecture and benchmark visual assets
│
├── benchmarks/                        # 500-sample benchmark evaluation suite
│   ├── run_phase2_benchmark_500.py    # 500-sample stress benchmark runner
│   ├── benchmark_engine.py            # Automated benchmark evaluation harness
│   ├── compiled_rules_phase1.json     # Phase 1 compiled rule schemas for K2 & Llama
│   ├── test_cases_500.json            # 500-sample stress benchmark dataset
│   └── phase2_benchmark_500_results.json # Raw evaluation benchmark results
│
└── llama.cpp/examples/llama.android/  # Android Application & Native Engine
    ├── app/src/main/java/com/example/llama/aichat/
    │   ├── ai/
    │   │   ├── K2InferenceManager.kt  # On-device llama.cpp loader & lifecycle
    │   │   ├── K2PromptBuilder.kt     # System prompts & few-shot compiler prompts
    │   │   ├── K2ResponseParser.kt    # Structured JSON response parser
    │   │   └── RuleClassifier.kt      # Edge AI rule classification schemas
    │   ├── notification/
    │   │   ├── NotificationListener.kt  # Android NotificationListenerService
    │   │   └── NotificationProcessor.kt # Hybrid dual-tier routing processor
    │   ├── data/                      # Room Database (Rules & Notification Logs)
    │   └── ui/                        # Jetpack Compose UI Screens
    └── lib/                           # Native C++ llama.cpp bindings & CMake config
```

---

## License & Contribution Policy

This repository is maintained as a standalone open-source project showcasing on-device AI system design. External pull requests and direct external contributions are not currently accepted.

Licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for details.
