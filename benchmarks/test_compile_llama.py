import time
import json
import model_engine

engine = model_engine.model_engine
engine.load_llama()

print("=" * 60)
print("COMPILING 15 RULES WITH LLAMA 3.2 1B INSTRUCT")
print("=" * 60)

for i, rule in enumerate(model_engine.PRELOADED_RULES, 1):
    prompt = engine.build_compile_rule_prompt_llama(rule)
    t0 = time.time()
    out = engine.llama_model(
        prompt,
        max_tokens=150,
        temperature=0.1,
        stop=["<|eot_id|>", "<|end_of_text|>"]
    )
    elapsed = time.time() - t0
    raw_text = out["choices"][0]["text"]
    parsed, ok = model_engine.extract_json_from_text(raw_text)
    cat = parsed.get("filter_category", "N/A") if ok else "INVALID_JSON"
    rtype = parsed.get("rule_type", "N/A") if ok else "INVALID_JSON"
    print(f"Rule {i:02d}: Type={rtype:<14} | Category={cat:<16} | Time={elapsed*1000:.1f}ms")
