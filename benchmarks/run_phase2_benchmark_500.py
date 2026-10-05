"""
Phase 2: Full Runtime Notification Triage Benchmark (500 Test Cases)
Evaluates K2 Horizon 0.9B vs Llama 3.2 1B Instruct using their Phase 1 compiled schemas.
"""

import os
import sys
import time
import json
import asyncio
import warnings

# UTF-8 encoding
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

os.environ["TRANSFORMERS_VERBOSITY"] = "error"
warnings.filterwarnings("ignore")

import model_engine
import benchmark_engine

DATASET_FILE = "test_cases_500.json"
RESULTS_FILE = "phase2_benchmark_500_results.json"

async def main():
    print("=" * 80)
    print(" 🚀 STARTING PHASE 2: RUNTIME NOTIFICATION TRIAGE BENCHMARK (500 CASES)")
    print(" Architecture: Exact Mobile On-Device Hybrid (AOT <0.2ms + Deep AI LLM)")
    print(" Models: 1. K2-Horizon-0.9B (CPU FP32) | 2. Llama-3.2-1B-Instruct (GGUF)")
    print("=" * 80 + "\n")

    if not os.path.exists(DATASET_FILE):
        print(f"[-] Dataset {DATASET_FILE} not found!")
        return

    with open(DATASET_FILE, "r", encoding="utf-8") as f:
        cases = json.load(f)

    total_cases = len(cases)
    print(f" Loaded {total_cases} curated stress-test cases.\n")

    # Load models
    print("[1/2] Loading K2 Horizon 0.9B into RAM...")
    t0 = time.time()
    model_engine.model_engine.load_k2()
    print(f"      K2 Horizon loaded in {time.time() - t0:.2f}s\n")

    print("[2/2] Loading Llama 3.2 1B Instruct into RAM...")
    t0 = time.time()
    model_engine.model_engine.load_llama()
    print(f"      Llama 3.2 loaded in {time.time() - t0:.2f}s\n")

    # Metrics trackers
    k2_correct = 0
    llama_correct = 0
    k2_aot_count = 0
    k2_deep_ai_count = 0
    llama_aot_count = 0
    llama_deep_ai_count = 0

    k2_false_alerts = 0
    k2_missed_emergencies = 0
    llama_false_alerts = 0
    llama_missed_emergencies = 0

    k2_total_time_ms = 0.0
    llama_total_time_ms = 0.0

    group_stats = {}

    evaluated_cases = []

    print("-" * 80)
    print(" EXECUTING BENCHMARK ACROSS 500 CASES")
    print("-" * 80)

    start_benchmark_time = time.time()

    for i, tc in enumerate(cases, 1):
        case_id = tc["id"]
        group = tc.get("group", "Other")
        sender = tc.get("sender", "")
        app_name = tc.get("app", "")
        text = tc.get("text", "")
        is_call = tc.get("is_call", False)
        gt_action = tc.get("ground_truth_action", "MUTE").upper()
        gt_reason = tc.get("ground_truth_reason", "")
        rule_name = tc.get("rule_name", "")

        if group not in group_stats:
            group_stats[group] = {
                "total": 0,
                "k2_correct": 0,
                "llama_correct": 0
            }
        group_stats[group]["total"] += 1

        # Evaluate K2 Horizon
        k2_eval = await benchmark_engine.evaluate_single_notification(
            model_name="K2-Horizon-0.9B",
            app_name=app_name,
            sender=sender,
            text=text,
            is_call=is_call
        )
        k2_action = k2_eval["action"]
        k2_match = (k2_action == gt_action)
        if k2_match:
            k2_correct += 1
            group_stats[group]["k2_correct"] += 1
        else:
            if gt_action == "ALERT" and k2_action == "MUTE":
                k2_missed_emergencies += 1
            elif gt_action == "MUTE" and k2_action == "ALERT":
                k2_false_alerts += 1

        if k2_eval["engine_used"] == "AOT_FILTER":
            k2_aot_count += 1
        else:
            k2_deep_ai_count += 1
        k2_total_time_ms += k2_eval.get("metrics", {}).get("total_ms", 0)

        # Evaluate Llama 3.2
        llama_eval = await benchmark_engine.evaluate_single_notification(
            model_name="Llama-3.2-1B-Instruct",
            app_name=app_name,
            sender=sender,
            text=text,
            is_call=is_call
        )
        llama_action = llama_eval["action"]
        llama_match = (llama_action == gt_action)
        if llama_match:
            llama_correct += 1
            group_stats[group]["llama_correct"] += 1
        else:
            if gt_action == "ALERT" and llama_action == "MUTE":
                llama_missed_emergencies += 1
            elif gt_action == "MUTE" and llama_action == "ALERT":
                llama_false_alerts += 1

        if llama_eval["engine_used"] == "AOT_FILTER":
            llama_aot_count += 1
        else:
            llama_deep_ai_count += 1
        llama_total_time_ms += llama_eval.get("metrics", {}).get("total_ms", 0)

        evaluated_entry = {
            "id": case_id,
            "group": group,
            "rule_name": rule_name,
            "sender": sender,
            "app": app_name,
            "text": text,
            "is_call": is_call,
            "ground_truth_action": gt_action,
            "ground_truth_reason": gt_reason,
            "k2": {
                "action": k2_action,
                "is_correct": k2_match,
                "engine_used": k2_eval["engine_used"],
                "reason": k2_eval.get("reason", ""),
                "metrics": k2_eval.get("metrics", {})
            },
            "llama": {
                "action": llama_action,
                "is_correct": llama_match,
                "engine_used": llama_eval["engine_used"],
                "reason": llama_eval.get("reason", ""),
                "metrics": llama_eval.get("metrics", {})
            }
        }
        evaluated_cases.append(evaluated_entry)

        # Log progress every 25 cases or on mismatch
        if i % 25 == 0 or i == 1 or i == total_cases:
            k2_acc = (k2_correct / i) * 100
            llama_acc = (llama_correct / i) * 100
            print(f"[{i:03d}/{total_cases}] K2 Acc: {k2_acc:.1f}% ({k2_correct}/{i}) | Llama Acc: {llama_acc:.1f}% ({llama_correct}/{i}) | Last: {sender} ({app_name}) -> GT:{gt_action} [K2:{k2_action}, Llama:{llama_action}]", flush=True)

    elapsed_total_sec = time.time() - start_benchmark_time

    # Final summary calculations
    k2_acc = round((k2_correct / total_cases) * 100, 2)
    llama_acc = round((llama_correct / total_cases) * 100, 2)

    final_payload = {
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "total_test_cases": total_cases,
        "elapsed_benchmark_seconds": round(elapsed_total_sec, 2),
        "phase1_stats": {
            "k2_compilation_accuracy": 86.7,
            "llama_compilation_accuracy": 26.7,
            "total_rules": 15
        },
        "phase2_summary": {
            "k2_horizon_0_9b": {
                "accuracy_percent": k2_acc,
                "correct_count": k2_correct,
                "total_count": total_cases,
                "aot_routed_count": k2_aot_count,
                "deep_ai_routed_count": k2_deep_ai_count,
                "false_alerts": k2_false_alerts,
                "missed_emergencies": k2_missed_emergencies,
                "avg_latency_ms": round(k2_total_time_ms / total_cases, 2),
                "total_time_ms": round(k2_total_time_ms, 2)
            },
            "llama_3_2_1b": {
                "accuracy_percent": llama_acc,
                "correct_count": llama_correct,
                "total_count": total_cases,
                "aot_routed_count": llama_aot_count,
                "deep_ai_routed_count": llama_deep_ai_count,
                "false_alerts": llama_false_alerts,
                "missed_emergencies": llama_missed_emergencies,
                "avg_latency_ms": round(llama_total_time_ms / total_cases, 2),
                "total_time_ms": round(llama_total_time_ms, 2)
            }
        },
        "group_breakdown": group_stats,
        "cases": evaluated_cases
    }

    with open(RESULTS_FILE, "w", encoding="utf-8") as f:
        json.dump(final_payload, f, indent=2, ensure_ascii=False)

    print("\n" + "=" * 80)
    print(" ✅ PHASE 2 BENCHMARK COMPLETE!")
    print(f" Total Cases: {total_cases} in {elapsed_total_sec:.1f} seconds")
    print(f" 🤖 K2 Horizon 0.9B Accuracy:       {k2_acc}% ({k2_correct}/{total_cases}) [Missed: {k2_missed_emergencies}, False Alerts: {k2_false_alerts}]")
    print(f" 🦙 Llama 3.2 1B Instruct Accuracy: {llama_acc}% ({llama_correct}/{total_cases}) [Missed: {llama_missed_emergencies}, False Alerts: {llama_false_alerts}]")
    print(f" Results written to: {RESULTS_FILE}")
    print("=" * 80 + "\n")

if __name__ == "__main__":
    asyncio.run(main())
