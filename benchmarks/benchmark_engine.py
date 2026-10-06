import os
import sys
import time
import json
import re
import asyncio
from typing import Dict, Any, List, Optional, Tuple

import model_engine

# Load Phase 1 compiled rules
BENCHMARK_DIR = os.path.dirname(os.path.abspath(__file__))
COMPILED_RULES_FILE = os.path.join(BENCHMARK_DIR, "compiled_rules_phase1.json")
COMPILED_RULES = []
if os.path.exists(COMPILED_RULES_FILE):
    try:
        with open(COMPILED_RULES_FILE, "r", encoding="utf-8-sig") as f:
            COMPILED_RULES = json.load(f)
    except Exception as e:
        print(f"[BenchmarkEngine] Error loading compiled rules: {e}")

def get_model_compiled_schemas(model_name: str) -> List[Dict[str, Any]]:
    """Returns the compiled rule schemas for 'k2' or 'llama'."""
    key = "k2" if "k2" in model_name.lower() else "llama"
    schemas = []
    for entry in COMPILED_RULES:
        rule_data = entry.get(key, {})
        schema = rule_data.get("schema")
        if schema:
            schemas.append({
                "rule_id": entry.get("rule_id"),
                "rule_index": entry.get("rule_index"),
                "rule_text": entry.get("rule_text"),
                "schema": schema,
                "rule_type": schema.get("rule_type", "SIMPLE_RULE"),
                "filter_category": schema.get("filter_category", "FAST_CONTACT"),
                "target_person": (schema.get("target_person") or "").strip().lower() if schema.get("target_person") else None,
                "target_app": (schema.get("target_app") or "").strip().lower() if schema.get("target_app") else None,
                "include_topics": [t.strip().lower() for t in schema.get("include_topics", []) if t],
                "exclude_keywords": [k.strip().lower() for k in schema.get("exclude_keywords", []) if k],
                "tone_requirement": schema.get("tone_requirement"),
                "is_call_only": schema.get("is_call_only", False),
                "action": (schema.get("action") or "ALERT").upper(),
                "reasoning": schema.get("reasoning", "")
            })
    return schemas

def run_aot_classifier(
    schemas: List[Dict[str, Any]], 
    app_name: str, 
    sender: str, 
    text: str, 
    is_call: bool = False
) -> Tuple[Optional[str], Optional[Dict[str, Any]], bool]:
    """
    Simulates the on-device <0.2ms AOT rule matcher based on compiled schemas.
    Returns: (action, matched_schema, needs_deep_ai)
    """
    app_lower = (app_name or "").lower().strip()
    sender_lower = (sender or "").lower().strip()
    text_lower = (text or "").lower().strip()

    # Iterate through model's compiled schemas in priority order
    for r in schemas:
        rule_type = r.get("rule_type", "SIMPLE_RULE")
        cat = r.get("filter_category", "FAST_CONTACT")
        target_p = r.get("target_person")
        target_a = r.get("target_app")
        topics = r.get("include_topics", [])
        excludes = r.get("exclude_keywords", [])
        action = r.get("action", "ALERT")
        if action == "IGNORE":
            action = "MUTE"
        is_call_only = r.get("is_call_only", False)
        tone_req = r.get("tone_requirement")

        # 1. Complex / Deep AI Rule Check
        if rule_type == "COMPLEX_RULE" or cat == "DEEP_AI_TONE" or (tone_req and str(tone_req).lower() not in ["none", "null", ""]):
            # Check if notification matches the candidate scope (e.g., target person or general)
            matches_scope = False
            if target_p:
                if target_p in sender_lower or sender_lower in target_p:
                    matches_scope = True
            elif target_a:
                if target_a in app_lower or app_lower in target_a:
                    matches_scope = True
            else:
                matches_scope = True
            
            if matches_scope:
                return None, r, True # Route to Deep AI LLM

        # 2. Fast Call check
        if is_call_only:
            if not is_call:
                continue
            if target_p and (target_p in sender_lower or sender_lower in target_p):
                return action, r, False

        # 3. Fast Contact check
        if cat == "FAST_CONTACT" or (target_p and not target_a and not topics):
            if target_p and (target_p in sender_lower or sender_lower in target_p):
                # Check exclusions
                if any(ex in text_lower for ex in excludes if ex):
                    return ("MUTE" if action == "ALERT" else "ALERT"), r, False
                return action, r, False

        # 4. App Filter check
        if cat == "APP_FILTER" or target_a:
            if target_a and (target_a in app_lower or app_lower in target_a or target_a in sender_lower):
                # If topics required
                if topics:
                    if any(t in text_lower or t in sender_lower for t in topics):
                        return action, r, False
                    # Has target app, but topic didn't match -> continue checking other rules
                else:
                    return action, r, False

        # 5. AOT Conditional (Contact + Topics + Exclusions)
        if cat == "AOT_CONDITIONAL" or (target_p and topics):
            if target_p and (target_p in sender_lower or sender_lower in target_p):
                # Check exclusions first
                if any(ex in text_lower for ex in excludes if ex):
                    return ("MUTE" if action == "ALERT" else "ALERT"), r, False
                # Check topics
                if any(t in text_lower for t in topics):
                    return action, r, False
                # Default for this contact if not excluded and no specific topic
                if not topics and not excludes:
                    return action, r, False

        # 6. AOT Topic Filter (Anyone messaging about topic)
        if cat == "AOT_TOPIC" or (topics and not target_p and not target_a):
            if any(t in text_lower for t in topics):
                return action, r, False

        # 7. Simple Block
        if cat == "SIMPLE_BLOCK":
            if target_p and (target_p in sender_lower or sender_lower in target_p):
                return action, r, False
            if any(t in text_lower or t in sender_lower for t in topics):
                return action, r, False

    # Default Ambient Silence (MUTE)
    return "MUTE", None, False

async def evaluate_single_notification(
    model_name: str,
    app_name: str,
    sender: str,
    text: str,
    is_call: bool = False,
    rule_text: str = ""
) -> Dict[str, Any]:
    """
    Executes real runtime notification evaluation:
    1. Runs model's compiled AOT Classifier in <0.2ms.
    2. If routed to Deep AI, invokes the model's LLM on-demand.
    """
    t0 = time.perf_counter()
    schemas = get_model_compiled_schemas(model_name)
    
    aot_action, matched_schema, needs_deep_ai = run_aot_classifier(
        schemas, app_name, sender, text, is_call
    )

    # Path A: Resolved in <0.2ms via AOT Engine
    if not needs_deep_ai:
        elapsed_ms = round((time.perf_counter() - t0) * 1000, 2)
        action = aot_action or "MUTE"
        if action == "IGNORE":
            action = "MUTE"
        reason = f"Resolved via ⚡ AOT Filter ({matched_schema.get('filter_category', 'AMBIENT') if matched_schema else 'Ambient Noise Mute'}) in {elapsed_ms}ms"
        return {
            "model": model_name,
            "engine_used": "AOT_FILTER",
            "action": action,
            "important": (action == "ALERT"),
            "category": "ALERT" if action == "ALERT" else "NOISE",
            "reason": reason,
            "metrics": {
                "total_ms": elapsed_ms,
                "ttft_ms": elapsed_ms,
                "tok_per_sec": 0,
                "tokens_generated": 0
            }
        }

    # Path B: Routed to 🧠 Deep AI (On-Demand LLM)
    deep_ai_rule = matched_schema.get("rule_text") if matched_schema else rule_text
    prompt_rules = deep_ai_rule or "Evaluate if notification is important based on user priorities."

    if "k2" in model_name.lower():
        prompt = model_engine.model_engine.build_prompt_chatml(
            rules=prompt_rules,
            app_name=app_name,
            sender=sender,
            text=text,
            is_call=is_call
        )
        final_chunk = None
        async for chunk in model_engine.model_engine.stream_k2_inference(prompt):
            if chunk["type"] == "final":
                final_chunk = chunk
    else:
        prompt = model_engine.model_engine.build_prompt_llama(
            rules=prompt_rules,
            app_name=app_name,
            sender=sender,
            text=text,
            is_call=is_call
        )
        final_chunk = None
        async for chunk in model_engine.model_engine.stream_llama_inference(prompt):
            if chunk["type"] == "final":
                final_chunk = chunk

    if final_chunk and final_chunk.get("parsed_json"):
        pj = final_chunk["parsed_json"]
        is_alert = bool(pj.get("alert", pj.get("important", False)))
        action = "ALERT" if is_alert else "MUTE"
        return {
            "model": model_name,
            "engine_used": "DEEP_AI_LLM",
            "action": action,
            "important": is_alert,
            "category": pj.get("category", "WORK" if is_alert else "NOISE"),
            "reason": pj.get("reason", "Deep AI LLM evaluation"),
            "summary": pj.get("summary", ""),
            "raw_output": final_chunk.get("raw_output", ""),
            "metrics": final_chunk.get("metrics", {})
        }
    else:
        # Fallback if invalid JSON emitted
        elapsed_ms = round((time.perf_counter() - t0) * 1000, 2)
        return {
            "model": model_name,
            "engine_used": "DEEP_AI_LLM_ERROR",
            "action": "MUTE",
            "important": False,
            "category": "NOISE",
            "reason": "Failed to parse JSON response from LLM",
            "raw_output": final_chunk.get("raw_output", "") if final_chunk else "",
            "metrics": final_chunk.get("metrics", {}) if final_chunk else {"total_ms": elapsed_ms}
        }
