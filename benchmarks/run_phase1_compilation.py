"""
Phase 1: Ahead-of-Time (AOT) Rule Compilation Benchmark
Executes all 15 natural language rules through both local models:
1. K2-Horizon-0.9B-BF16 (PyTorch bfloat16)
2. Llama-3.2-1B-Instruct-BF16 (llama.cpp GGUF)

Stores the exact responses in compiled_rules_phase1.json.
"""

import os
import sys
import time
import json
import asyncio
import warnings

# Ensure UTF-8 stdout on Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

os.environ["TRANSFORMERS_VERBOSITY"] = "error"
warnings.filterwarnings("ignore")

import model_engine

# The 15 Rules
RULES = model_engine.PRELOADED_RULES

async def main():
    print("=" * 80)
    print(" 🚀 STARTING PHASE 1: AHEAD-OF-TIME (AOT) RULE COMPILATION BENCHMARK")
    print(f" Total Rules to Compile: {len(RULES)}")
    print(" Models:")
    print("   1. K2 Horizon 0.9B BF16 (PyTorch / Transformers)")
    print("   2. Llama 3.2 1B Instruct BF16 (llama.cpp / GGUF)")
    print("=" * 80 + "\n")

    # Load both models
    print("[1/2] Preloading K2 Horizon 0.9B BF16...")
    t0 = time.time()
    model_engine.model_engine.load_k2()
    print(f"      K2 Horizon loaded in {time.time() - t0:.2f}s\n")

    print("[2/2] Preloading Llama 3.2 1B Instruct BF16...")
    t0 = time.time()
    model_engine.model_engine.load_llama()
    print(f"      Llama 3.2 loaded in {time.time() - t0:.2f}s\n")

    compiled_results = []

    print("-" * 80)
    print(" RUNNING SIDE-BY-SIDE COMPILATION ACROSS ALL 15 RULES")
    print("-" * 80 + "\n")

    for i, rule_text in enumerate(RULES, 1):
        print(f"\n>>> [Rule {i}/15]: \"{rule_text}\"", flush=True)

        # ----------------------------------------------------------------------
        # Flow 1: K2 Horizon 0.9B BF16
        # ----------------------------------------------------------------------
        k2_prompt = model_engine.model_engine.build_compile_rule_prompt_chatml(rule_text)
        k2_final = None
        async for chunk in model_engine.model_engine.stream_k2_inference(k2_prompt):
            if chunk["type"] == "final":
                k2_final = chunk

        # ----------------------------------------------------------------------
        # Flow 2: Llama 3.2 1B Instruct BF16
        # ----------------------------------------------------------------------
        llama_prompt = model_engine.model_engine.build_compile_rule_prompt_llama(rule_text)
        llama_final = None
        async for chunk in model_engine.model_engine.stream_llama_inference(llama_prompt):
            if chunk["type"] == "final":
                llama_final = chunk

        k2_schema = k2_final["parsed_json"] if k2_final else None
        llama_schema = llama_final["parsed_json"] if llama_final else None

        k2_type = k2_schema.get("rule_type") if k2_schema else "INVALID_JSON"
        llama_type = llama_schema.get("rule_type") if llama_schema else "INVALID_JSON"

        k2_cat = k2_schema.get("filter_category") if k2_schema else "N/A"
        llama_cat = llama_schema.get("filter_category") if llama_schema else "N/A"

        print(f"  [K2 0.9B]   Rule Type: {k2_type:<14} | Category: {k2_cat:<16} | Time: {k2_final['metrics']['total_ms']}ms", flush=True)
        print(f"  [Llama 3.2] Rule Type: {llama_type:<14} | Category: {llama_cat:<16} | Time: {llama_final['metrics']['total_ms']}ms", flush=True)

        result_entry = {
            "rule_id": f"rule_{i}",
            "rule_index": i,
            "rule_text": rule_text,
            "k2": {
                "rule_type": k2_type,
                "filter_category": k2_cat,
                "schema": k2_schema,
                "is_valid_json": k2_final["is_valid_json"] if k2_final else False,
                "raw_output": k2_final["raw_output"] if k2_final else "",
                "metrics": k2_final["metrics"] if k2_final else None
            },
            "llama": {
                "rule_type": llama_type,
                "filter_category": llama_cat,
                "schema": llama_schema,
                "is_valid_json": llama_final["is_valid_json"] if llama_final else False,
                "raw_output": llama_final["raw_output"] if llama_final else "",
                "metrics": llama_final["metrics"] if llama_final else None
            }
        }
        compiled_results.append(result_entry)

    # Save to disk
    output_path = "compiled_rules_phase1.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(compiled_results, f, indent=2, ensure_ascii=False)

    print("\n" + "=" * 80)
    print(f" ✅ PHASE 1 COMPLETE! Compiled schemas saved to: {output_path}")
    print("=" * 80 + "\n")

if __name__ == "__main__":
    asyncio.run(main())
