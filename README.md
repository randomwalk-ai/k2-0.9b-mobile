# K2 Horizon: On-Device Intelligent Edge Notification Assistant

[![Blog Case Study](https://img.shields.io/badge/Read_Case_Study-randomwalk.ai-10b981.svg)](https://k2-blog-randomwalk.vercel.app/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Model](https://img.shields.io/badge/Model-IFM%2FK2--Horizon--0.9B-purple.svg)](https://huggingface.co/IFM/K2-Horizon-0.9B)
[![Runtime](https://img.shields.io/badge/Runtime-llama.cpp_Mobile-orange.svg)](https://github.com/ggerganov/llama.cpp)

An ultra-efficient, privacy-first on-device AI notification filtering and alert engine for Android powered by **K2 Horizon (0.9B)** and **llama.cpp**.

Read the full engineering case study and system design on [randomwalk.ai](https://k2-blog-randomwalk.vercel.app/).

---

## The Problem & The Solution

Every day, phones are flooded with notification noise. When busy at work, in meetings, or sleeping at night, muting notifications is the easiest fix—but you risk missing critical alerts that actually matter.

**K2 Horizon Notification Assistant** allows users to define priority rules in natural plain English. When an incoming notification matches your criteria, the app triggers a **high-priority media sound chime and custom vibration—even if your phone's notification volume is muted or set to zero**. 

Everything runs **100% locally on-device**: zero cloud API calls, zero telemetry, and zero privacy compromise.

---

## Architecture & System Design

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

Running full LLM inference on every single incoming notification introduces severe mobile battery drain and thermal throttling. To solve this, K2 Horizon uses an **Ahead-of-Time (AOT) Rule Compiler architecture**:

1. **At Rule Creation Time (AOT Compilation)**:
   - When you enter a natural rule (e.g., *"if any msg from Sarah it is important, if she sends reels it is not important"*), K2 Horizon runs **once**.
   - It compiles the natural language intent into a structured JSON schema containing target apps, contacts, positive topic anchors, and negative exclusion rules.
2. **Tier 1: Fast Classifier Runtime Path (`< 0.2 ms`)**:
   - Incoming notifications are evaluated instantly against cached schemas in native memory.
   - App filters, contact lookups, and keyword exclusions execute with zero battery drain and zero CPU spikes.
3. **Tier 2: K2 Horizon Deep AI On-Demand (`~150–300 ms`)**:
   - If and only if a rule requires subjective emotional evaluation or tone reasoning (e.g., *"Charlie when angry is not important"* or *"Alert if manager sounds furious"*), the notification is routed to K2 Horizon for deep semantic reasoning.

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
git clone -b k2-arch https://github.com/ritvikrw/k2-0.9b-mobile.git
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

*Source: [randomwalk.ai Engineering Case Study](https://k2-blog-randomwalk.vercel.app/)*

### 1. 500-Test-Case Stress Benchmark (K2 Horizon 0.9B vs. Llama 3.2 1B)

Evaluated across 500 complex test cases covering emotional tones (sarcasm, passive aggressive frustration, hostility), urgent escalations, conditional exclusions, and multi-app rules:

| Metric | K2 Horizon 0.9B | Llama 3.2 1B | Margin |
| :--- | :---: | :---: | :---: |
| **Overall Triage Accuracy** | **65.8%** (329/500) | 41.0% (205/500) | **+24.8%** |
| **AOT Rule Compilation (15 Complex Rules)** | **86.7%** (13/15) | 26.7% (4/15) | **+60.0%** |
| **Deep AI Emotional Nuance (250 Tone Cases)** | **58.4%** | 48.4% | **+10.0%** |
| **App & Topic Filter Schema Routing** | **90.0%** | 0.0% | **+90.0%** |

- **Key Finding**: K2 Horizon 0.9B reliably generates structured JSON schemas for multi-clause rules and accurately detects subtle conversational cues like passive-aggressive frustration to silence unneeded alerts, whereas standard sub-1B models frequently produce malformed schemas or misclassify dismissiveness.

---

### 2. Live 5-Day Daily-Driver Benchmark

Tested on a physical **8GB RAM Android device** under regular daily use:

- **Test Duration**: 5 Continuous Days with 7 active natural language rules.
- **Notification Volume**: ~500 notifications/day (~2,500 total processed from WhatsApp, Microsoft Teams, Slack, Instagram, and phone calls).
- **Edge Cases Missed**: Only 2–3 minor edge cases across all ~2,500 notifications.
- **Thermals & Battery**: Zero standby battery drain and zero thermal buildup due to native AOT execution.
- **Latency**: `< 0.2 ms` for deterministic rules; `~150–300 ms` for on-demand deep reasoning.

---

## Supported Rule Types

| Rule Intent | Engine | Natural Language Example |
| :--- | :--- | :--- |
| **`APP_FILTER`** | `AOT_FAST` (`<0.2ms`) | *"Any message from Teams or Slack is important"* |
| **`TOPIC_FILTER`** | `AOT_FAST` (`<0.2ms`) | *"Alert any message about OTP, server outage, or delivery"* |
| **`SIMPLE_CONTACT`** | `AOT_FAST` (`<0.2ms`) | *"If Alice calls or messages me it is important"* |
| **`SIMPLE_BLOCK`** | `AOT_FAST` (`<0.2ms`) | *"Mute all promotional offers from Swiggy"* |
| **`CONDITIONAL_CONTACT`** | `AOT_FAST` (`<0.2ms`) | *"Alice is important, but if she sends reels or memes mute it"* |
| **`CONDITIONAL_EMOTION`** | `K2_DEEP` (`On-Demand`) | *"Charlie when angry is not important"*, *"Alert if boss sounds furious"* |
| **`MULTI_CONDITION`** | Hybrid | *"Arjun related to job is important, otherwise ignore"* |

---

## Project Structure

```
k2-0.9b-mobile/
├── README.md
├── LICENSE
├── benchmarks/                        # Python evaluation datasets and benchmarks
│   ├── benchmark_engine.py            # Automated benchmark evaluation harness
│   ├── test_cases_500.json            # 500-sample stress benchmark dataset
│   └── test_cases_1000.json           # Extended test dataset
│
└── llama.cpp/examples/llama.android/  # Android Application & Native Engine
    ├── app/src/main/java/com/example/llama/aichat/
    │   ├── ai/
    │   │   ├── K2InferenceManager.kt  # On-device llama.cpp loader & lifecycle
    │   │   ├── K2PromptBuilder.kt     # System prompts & few-shot compiler prompts
    │   │   ├── K2ResponseParser.kt    # Structured JSON response parser
    │   │   └── RuleCompiler.kt        # Edge AI rule compilation engine
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
