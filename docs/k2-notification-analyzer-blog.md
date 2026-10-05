# K2 Horizon Model Beats Llama 3.2 by 25%: Here’s How We Did It

**Author:** randomwalk.ai Engineering Team  
**Test Hardware:** 8GB RAM Android Device  
**Model:** IFM/K2-Horizon-0.9B (4-bit Q4_K_M GGUF via llama.cpp)  
**Branch:** `k2-arch`

---

We originally set out to test the new **K2 Horizon** model family locally. We first benchmarked both the **0.9B** and **7B** models on a 32GB RAM laptop by building a local chatbot testbed. While the 7B model delivered solid answers, its latency and memory load were noticeably heavier compared to the nimble 0.9B variant. For mobile deployment on Android, the 0.9B model was the clear contender. Given K2's strong reasoning capabilities even at smaller parameter sizes, we wanted to build a practical real-world use case, which led us to create an on-device **Notification Analyzer**.

## 1. The Everyday Problem: The Important Notification You Missed Because You Were Busy

Every day, our phones get flooded with notification noise. When busy at work, in meetings, or sleeping at night, muting notifications is the easiest fix, but it comes at a cost: **you risk missing the alerts that actually matter**.

We built an on-device Android notification analyzer where you define priority rules in plain English. When an alert matches your rules, **the app alerts you through a high-priority media sound chime and custom vibration, even if your phone's notification volume is muted or set to zero**. It is 100% private because everything runs locally using <strong>K2 Horizon 0.9B</strong>, a compact open-weights Small Language Model (SLM).

---

## 2. Development & Architecture: Tackling Heat with Ahead-of-Time Compilation

Deploying a language model directly on a mobile device introduces strict thermal and power constraints. Unlike a server with dedicated cooling, mobile processors quickly throttle under continuous load. Here is how our early experiments unfolded:

| Attempt | Setup | What happened |
|---|---|---|
| **1** | Full precision 0.9B (~2.1 GB), inference on every notification | Loaded fine into RAM, but heavy continuous CPU compute on each alert caused immediate phone heating and battery drain. |
| **2** | RAM residency & lazy loading (permanent vs. idle timeout) | Continuous RAM residency created constant memory pressure, while lazy unloading triggered repeated cold-starts and CPU spikes during notification bursts. |

Running model inference on every incoming notification was fundamentally the wrong design for an always-on background service. That led to rethinking the entire architecture from first principles:

> *"For simple rules, why should an SLM run at runtime to decide if an alert is important? We can delegate easy tasks to a fast classifier."*

Instead of treating K2 Horizon as a runtime filter that evaluates every message, we convert it into an **Ahead-of-Time (AOT) Rule Compiler**. The language model only runs when you create or edit a rule, translating plain-English intent into deterministic matching logic.

![System Architecture: AI Rule Engine](/static/images/architecture_diagram.jpg)

Here is how the dual-engine pipeline operates in practice:

1. **At Rule Creation Time (AOT Compilation):** When you type a rule like *"if any msg from madhu it is important, if she sends reels it is not important"*, K2 processes it once. It extracts the contact name, required topic triggers, and excluded keywords into a structured JSON schema.
2. **Fast Classifier Runtime Path (<0.2 ms):** When a notification arrives, the on-device classifier evaluates it in native memory against the compiled schema. App filters, contact lookups, and keyword exclusions are processed in less than 0.2 milliseconds with zero CPU spikes and zero thermal buildup.
3. **Complex Semantic Rules (K2 On-Demand):** If a rule explicitly requires sentiment analysis or emotional tone (e.g. *"any message from Pranav when he is angry it is not important"*), the fast classifier routes only those specific matching messages to K2 Horizon for deep semantic reasoning.

---

## 3. Stress Testing & Benchmarks: K2 Horizon 0.9B vs. Llama 3.2 1B

Before daily-driver deployment, we conducted rigorous stress tests across a benchmark dataset of **500 diverse test cases** covering emotions (sarcasm, passive aggressive frustration, anger), urgent escalation, conditional exclusions, and app rules.

### Key Benchmark Metrics (500 Test Cases):
- **Overall Triage Accuracy:** K2 Horizon 0.9B scored **65.8% (329/500)** vs. Llama 3.2 1B at **41.0% (205/500)** (+24.8% Higher Overall Accuracy).
- **AOT Rule Compilation (15 Natural Rules):** K2 Horizon 0.9B achieved **86.7% (13/15)** vs. Llama 3.2 1B at **26.7% (4/15)**.
- **Deep AI Emotional Nuance (250 cases):** K2 Horizon 0.9B scored **58.4%** vs. Llama 3.2 1B at **48.4%**.
- **App & Topic Filter Routing:** K2 Horizon scored **90.0%** vs. Llama at **0.0%** (Llama failed to generate valid JSON structures for app constraints).

### Sample Stress Test Cases:
| Case #1: Passive Aggressive Detection | Case #2: Critical Outage Escalation |
|---|---|
| ![Stress Case 1](/static/images/stress_case_1_passive_aggressive.png) | ![Stress Case 2](/static/images/stress_case_2_urgent_escalation.png) |

**Case Breakdown:** While both models reliably catch explicit emergency keywords like "Urgent" (Case #2), K2 Horizon 0.9B successfully identifies nuanced conversational cues like passive aggressive frustration (Case #1) to silence unneeded alerts, where other sub-1B models misclassify dismissiveness as neutral agreement.

Overall, K2 Horizon 0.9B proves to be the best choice for our on-device notification analyzer. As a dedicated reasoning model, its compact reasoning density and strict instruction following allow it to reliably compile complex natural language rules into structured schemas and decipher subtle human emotions where generalist sub-1B models fall short.

---

## 4. Real-World Testing on Q4 Version of K2-0.9B

### Deploying with 4-Bit Quantization (Q4_K_M):
For everyday mobile deployment on Android, a **4-bit quantized version of K2 Horizon (Q4_K_M, ~635 MB)** perfectly serves all our regular needs. It cuts memory by 70%, lowers latency, eliminates thermal buildup, and delivers optimal battery efficiency.

### Live Notification Feed Testing:
- **Madhu: "Sent you a reel"** ➔ **🔕 Silenced (Reel Excluded)**
- **Madhu: "Hi"** ➔ **🚨 Alerted (Important Contact)**
- **Arjun: "I found a job opening on LinkedIn"** ➔ **🚨 Alerted (Topic: 'job')**
- **Arjun: "Lets go to a trip" / "Watched movie?"** ➔ **🔕 Silenced (No Match)**

### Live 5-Day Benchmark Data:
- **Test Duration:** 5 continuous days as a daily driver (7 active rules).
- **Notification Volume:** ~500 notifications/day (~2,500 total processed across WhatsApp, Teams, Instagram, Phone, etc.).
- **Accuracy:** Only 2–3 minor edge cases across all 5 days.
- **Efficiency:** Zero standby battery drain and zero phone heating.

---

## 5. Key Takeaways

By pairing K2 Horizon with a fast on-device classifier, we use the model where it excels: compiling complex human intent into structured rules ahead of time, while native code handles instant runtime execution. This architecture delivers deep semantic flexibility without sacrificing the battery life or thermal stability of the host device.

Small language models do not need to process every single incoming data stream at runtime. When building on-device AI applications, the winning formula is not making the model compute faster on every event, but using compact SLMs as intelligent compilers and letting deterministic code handle the high-throughput path.

