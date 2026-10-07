"""
Model Engine for Side-by-Side Model Comparison at /test
Models:
1. K2-Horizon-0.9B FP32 (CPU) (Local PyTorch / Transformers FP32)
2. Llama-3.2-1B-Instruct-BF16 (Local llama.cpp / GGUF)

Pure model runner: Takes rule and notification, formats prompt, and runs local models.
"""

import os
import sys
import time
import json
import re
import asyncio
import warnings
from typing import AsyncGenerator, Dict, List, Optional, Any, Tuple
from threading import Thread

os.environ["TRANSFORMERS_VERBOSITY"] = "error"
warnings.filterwarnings("ignore")

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer, TextIteratorStreamer, StoppingCriteria, StoppingCriteriaList, logging

class JsonStopCriteria(StoppingCriteria):
    def __init__(self, tokenizer, prompt_len):
        self.tokenizer = tokenizer
        self.prompt_len = prompt_len

    def __call__(self, input_ids: torch.LongTensor, scores: torch.FloatTensor, **kwargs) -> bool:
        new_tokens = input_ids[0, self.prompt_len:]
        if len(new_tokens) < 3:
            return False
        text = self.tokenizer.decode(new_tokens, skip_special_tokens=False)
        if '<|ifm|im_end|>' in text or '<|im_end|>' in text:
            return True
        if '{' in text and text.count('{') == text.count('}'):
            if text.strip().endswith('}'):
                return True
        return False

logging.set_verbosity_error()

num_cores = os.cpu_count() or 8
torch.set_num_threads(num_cores)

try:
    import llama_cpp
    HAS_LLAMA_CPP = True
except Exception:
    HAS_LLAMA_CPP = False

K2_MODEL_ID = "IFM/K2-Horizon-0.9B"
K2_REVISION = "fa7f5ded0883803c8d8f2bdbfddcdba2e462e698"
LLAMA_GGUF_PATH = "Llama-3.2-1B-Instruct-bf16.gguf"
if not os.path.exists(LLAMA_GGUF_PATH):
    candidate_root = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "Llama-3.2-1B-Instruct-bf16.gguf")
    candidate_bench = os.path.join(os.path.dirname(os.path.abspath(__file__)), "Llama-3.2-1B-Instruct-bf16.gguf")
    if os.path.exists(candidate_root):
        LLAMA_GGUF_PATH = candidate_root
    elif os.path.exists(candidate_bench):
        LLAMA_GGUF_PATH = candidate_bench

# The 15 stress-testing rule strings provided by the user
PRELOADED_RULES = [
    "any message from rahul when he sounds frustrated or passive aggressive is not important",
    "if boss messages with genuine panic or urgent crisis it is important",
    "any message from sarah when she is thanking or appreciating is important",
    "if any email from manager sounds demanding or angry it is important",
    "any message from vikram expressing disappointment or regret is not important",
    "if anyone sends an urgent emergency asking for help it is important",
    "if any msg from priya it is important,if she sends reels or memes it is not important",
    "any msg from rohit related to interview or salary is important",
    "any notification from slack about deployment or bug fix is important",
    "any message from teams is important",
    "all notifications from swiggy or uber are important",
    "if sneha calls me it is important",
    "any msg from ananya it is important",
    "anyone messages about playing badminton or football it is important",
    "ignore all messages from marketing or spam"
]

def extract_json_from_text(raw_text: str) -> Tuple[Optional[Dict[str, Any]], bool]:
    """
    Attempts to parse any JSON emitted by the model.
    """
    cleaned = raw_text.strip()
    
    # 1. Codeblock ```json ... ```
    json_block = re.search(r'```(?:json)?\s*(\{.*?\})\s*```', cleaned, re.DOTALL)
    if json_block:
        candidate = json_block.group(1).strip()
        try:
            return json.loads(candidate), True
        except Exception:
            pass

    # 2. Outer { ... }
    first_brace = cleaned.find('{')
    if first_brace != -1:
        brace_count = 0
        end_brace = -1
        for i in range(first_brace, len(cleaned)):
            if cleaned[i] == '{':
                brace_count += 1
            elif cleaned[i] == '}':
                brace_count -= 1
                if brace_count == 0:
                    end_brace = i
                    break
        
        if end_brace != -1:
            candidate = cleaned[first_brace:end_brace+1]
            try:
                return json.loads(candidate), True
            except Exception:
                pass
            try:
                fixed = candidate.replace("'", '"')
                fixed = re.sub(r',\s*}', '}', fixed)
                return json.loads(fixed), True
            except Exception:
                pass
        else:
            # Try auto-closing unclosed JSON
            candidate = cleaned[first_brace:]
            quote_count = candidate.count('"') - candidate.count(r'\"')
            if quote_count % 2 != 0:
                candidate += '"'
            candidate += '\n}'
            try:
                fixed = re.sub(r',\s*}', '}', candidate)
                return json.loads(fixed), True
            except Exception:
                pass

    return None, False

class ModelEngine:
    def __init__(self):
        self.k2_model = None
        self.k2_tokenizer = None
        self.k2_loaded = False

        self.llama_model = None
        self.llama_loaded = False

    def get_models_status(self) -> Dict[str, Any]:
        return {
            "k2_0_9b": {
                "name": "K2 Horizon 0.9B FP32 (CPU)",
                "tag": K2_MODEL_ID,
                "backend": "PyTorch / Transformers (FP32 CPU)",
                "loaded": self.k2_loaded,
                "cores": num_cores
            },
            "llama_3_2_1b": {
                "name": "Llama 3.2 1B Instruct BF16",
                "tag": LLAMA_GGUF_PATH,
                "backend": "llama.cpp (GGUF BF16)",
                "loaded": self.llama_loaded,
                "cores": num_cores
            }
        }

    def load_k2(self):
        if self.k2_loaded and self.k2_model is not None:
            return
        print(f"[ModelEngine] Loading K2 Horizon 0.9B into RAM (revision: {K2_REVISION})...")
        self.k2_tokenizer = AutoTokenizer.from_pretrained(K2_MODEL_ID, revision=K2_REVISION, trust_remote_code=True)
        self.k2_model = AutoModelForCausalLM.from_pretrained(
            K2_MODEL_ID,
            revision=K2_REVISION,
            dtype=torch.float32,
            low_cpu_mem_usage=True,
            trust_remote_code=True
        )
        self.k2_model.eval()
        self.k2_loaded = True
        print("[ModelEngine] K2 Horizon 0.9B ready!")

    def load_llama(self):
        if self.llama_loaded and self.llama_model is not None:
            return
        if not HAS_LLAMA_CPP:
            raise RuntimeError("llama_cpp is not installed.")
        print(f"[ModelEngine] Loading Llama 3.2 1B Instruct BF16 into RAM...")
        self.llama_model = llama_cpp.Llama(
            model_path=LLAMA_GGUF_PATH,
            n_ctx=2048,
            n_threads=num_cores,
            verbose=False
        )
        self.llama_loaded = True
        print("[ModelEngine] Llama 3.2 1B Instruct BF16 ready!")

    def build_compile_rule_prompt_chatml(self, rule_text: str) -> str:
        """
        Builds Ahead-of-Time (AOT) Rule Compilation prompt for K2 Horizon.
        """
        system_content = (
            "You are an Ahead-of-Time (AOT) Rule Compiler for an on-device mobile notification analyzer.\n"
            "Given a user's natural language priority rule, classify and compile it into a structured JSON schema:\n"
            "{\n"
            '  "rule_type": "SIMPLE_RULE" or "COMPLEX_RULE",\n'
            '  "filter_category": "FAST_CONTACT" | "APP_FILTER" | "AOT_TOPIC" | "AOT_CONDITIONAL" | "SIMPLE_BLOCK" | "DEEP_AI_TONE",\n'
            '  "target_person": "name or null",\n'
            '  "target_app": "app or null",\n'
            '  "include_topics": ["keywords"],\n'
            '  "exclude_keywords": ["keywords"],\n'
            '  "tone_requirement": "tone/emotion or null",\n'
            '  "is_call_only": boolean,\n'
            '  "action": "ALERT" or "MUTE",\n'
            '  "reasoning": "brief explanation"\n'
            "}\n"
            "Note: Use 'SIMPLE_RULE' for deterministic names, apps, keywords, exclusions. Use 'COMPLEX_RULE' only if the rule requires tone, emotion, or contextual sentiment."
        )
        user_content = f"Compile this rule:\n\"{rule_text}\""

        prompt = (
            f"<|ifm|im_start|>system\n{system_content}<|ifm|im_end|>\n"
            f"<|ifm|im_start|>user\n{user_content}<|ifm|im_end|>\n"
            f"<|ifm|im_start|>assistant\n"
        )
        return prompt

    def build_compile_rule_prompt_llama(self, rule_text: str) -> str:
        """
        Builds Ahead-of-Time (AOT) Rule Compilation prompt for Llama 3.2.
        """
        system_content = (
            "You are an Ahead-of-Time (AOT) Rule Compiler for an on-device mobile notification analyzer.\n"
            "Given a user's natural language priority rule, classify and compile it into a structured JSON schema:\n"
            "{\n"
            '  "rule_type": "SIMPLE_RULE" or "COMPLEX_RULE",\n'
            '  "filter_category": "FAST_CONTACT" | "APP_FILTER" | "AOT_TOPIC" | "AOT_CONDITIONAL" | "SIMPLE_BLOCK" | "DEEP_AI_TONE",\n'
            '  "target_person": "name or null",\n'
            '  "target_app": "app or null",\n'
            '  "include_topics": ["keywords"],\n'
            '  "exclude_keywords": ["keywords"],\n'
            '  "tone_requirement": "tone/emotion or null",\n'
            '  "is_call_only": boolean,\n'
            '  "action": "ALERT" or "MUTE",\n'
            '  "reasoning": "brief explanation"\n'
            "}\n"
            "Note: Use 'SIMPLE_RULE' for deterministic names, apps, keywords, exclusions. Use 'COMPLEX_RULE' only if the rule requires tone, emotion, or contextual sentiment."
        )
        user_content = f"Compile this rule:\n\"{rule_text}\""

        prompt = (
            f"<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
            f"{system_content}<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n"
            f"{user_content}<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
        )
        return prompt

    def build_prompt_chatml(self, rules: str, app_name: str, sender: str, text: str, is_call: bool = False) -> str:
        system_content = (
            "You are an on-device AI Notification Analyzer for a mobile phone.\n"
            "Evaluate the incoming notification against the user's priority rules. Output JSON:\n"
            "{\n"
            '  "important": boolean,\n'
            '  "alert": boolean,\n'
            '  "category": "WORK" | "PERSONAL" | "ALERT" | "NOISE",\n'
            '  "reason": "1-sentence reason",\n'
            '  "summary": "short summary"\n'
            "}"
        )
        call_info = " [INCOMING CALL]" if is_call else ""
        user_content = f"User Rules:\n{rules}\n\nIncoming Notification:\nApp: {app_name}{call_info}\nSender: {sender}\nText: {text}"

        prompt = (
            f"<|ifm|im_start|>system\n{system_content}<|ifm|im_end|>\n"
            f"<|ifm|im_start|>user\n{user_content}<|ifm|im_end|>\n"
            f"<|ifm|im_start|>assistant\n"
        )
        return prompt

    def build_prompt_llama(self, rules: str, app_name: str, sender: str, text: str, is_call: bool = False) -> str:
        system_content = (
            "You are an on-device AI Notification Analyzer for a mobile phone.\n"
            "Evaluate the incoming notification against the user's priority rules. Output JSON:\n"
            "{\n"
            '  "important": boolean,\n'
            '  "alert": boolean,\n'
            '  "category": "WORK" | "PERSONAL" | "ALERT" | "NOISE",\n'
            '  "reason": "1-sentence reason",\n'
            '  "summary": "short summary"\n'
            "}"
        )
        call_info = " [INCOMING CALL]" if is_call else ""
        user_content = f"User Rules:\n{rules}\n\nIncoming Notification:\nApp: {app_name}{call_info}\nSender: {sender}\nText: {text}"

        prompt = (
            f"<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
            f"{system_content}<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n"
            f"{user_content}<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
        )
        return prompt

    async def stream_k2_inference(self, prompt: str) -> AsyncGenerator[Dict[str, Any], None]:
        if not self.k2_loaded:
            yield {"type": "status", "message": "Loading K2 Horizon 0.9B into RAM..."}
            await asyncio.to_thread(self.load_k2)
            yield {"type": "status", "message": "K2 ready! Generating tokens..."}

        inputs = self.k2_tokenizer(prompt, return_tensors="pt").to("cpu")
        streamer = TextIteratorStreamer(self.k2_tokenizer, skip_prompt=True, skip_special_tokens=False)

        prompt_len = inputs.input_ids.shape[1]
        stop_criteria = StoppingCriteriaList([JsonStopCriteria(self.k2_tokenizer, prompt_len)])
        generation_kwargs = dict(
            **inputs,
            streamer=streamer,
            max_new_tokens=180,
            do_sample=False,
            stopping_criteria=stop_criteria,
            pad_token_id=self.k2_tokenizer.eos_token_id
        )

        t_start = time.perf_counter()
        first_token_time = None
        full_text = ""
        token_count = 0

        def run_gen():
            with torch.inference_mode():
                self.k2_model.generate(**generation_kwargs)

        thread = Thread(target=run_gen)
        thread.start()

        loop = asyncio.get_event_loop()
        while True:
            try:
                token = await loop.run_in_executor(None, lambda: next(streamer, None))
                if token is None:
                    break

                if first_token_time is None:
                    first_token_time = time.perf_counter() - t_start

                token_count += 1
                full_text += token
                yield {"type": "token", "token": token, "full_text": full_text}

                if full_text.count('{') > 0 and full_text.count('{') == full_text.count('}'):
                    if '}' in token:
                        break

            except StopIteration:
                break
            except Exception as e:
                yield {"type": "error", "error": str(e)}
                break

        thread.join(timeout=1.0)

        total_time = time.perf_counter() - t_start
        ttft_ms = round((first_token_time or total_time) * 1000, 2)
        tok_per_sec = round(token_count / total_time, 2) if total_time > 0 else 0
        total_ms = round(total_time * 1000, 2)

        parsed_json, is_valid_json = extract_json_from_text(full_text)

        yield {
            "type": "final",
            "model": "K2-Horizon-0.9B-FP32-CPU",
            "raw_output": full_text,
            "parsed_json": parsed_json,
            "is_valid_json": is_valid_json,
            "metrics": {
                "ttft_ms": ttft_ms,
                "tok_per_sec": tok_per_sec,
                "total_ms": total_ms,
                "tokens_generated": token_count
            }
        }

    async def stream_llama_inference(self, prompt: str) -> AsyncGenerator[Dict[str, Any], None]:
        if not self.llama_loaded:
            yield {"type": "status", "message": "Loading Llama 3.2 into RAM..."}
            await asyncio.to_thread(self.load_llama)
            yield {"type": "status", "message": "Llama 3.2 ready! Generating tokens..."}

        t_start = time.perf_counter()
        first_token_time = None
        full_text = ""
        token_count = 0

        loop = asyncio.get_event_loop()

        def generate_generator():
            return self.llama_model(
                prompt,
                max_tokens=256,
                temperature=0.2,
                stream=True,
                stop=["<|eot_id|>", "<|end_of_text|>"]
            )

        stream = await loop.run_in_executor(None, generate_generator)

        for chunk in stream:
            token = chunk["choices"][0]["text"]
            if first_token_time is None:
                first_token_time = time.perf_counter() - t_start

            token_count += 1
            full_text += token
            yield {"type": "token", "token": token, "full_text": full_text}

            if full_text.count('{') > 0 and full_text.count('{') == full_text.count('}'):
                if '}' in token:
                    break

        total_time = time.perf_counter() - t_start
        ttft_ms = round((first_token_time or total_time) * 1000, 2)
        tok_per_sec = round(token_count / total_time, 2) if total_time > 0 else 0
        total_ms = round(total_time * 1000, 2)

        parsed_json, is_valid_json = extract_json_from_text(full_text)

        yield {
            "type": "final",
            "model": "Llama-3.2-1B-Instruct-BF16",
            "raw_output": full_text,
            "parsed_json": parsed_json,
            "is_valid_json": is_valid_json,
            "metrics": {
                "ttft_ms": ttft_ms,
                "tok_per_sec": tok_per_sec,
                "total_ms": total_ms,
                "tokens_generated": token_count
            }
        }

model_engine = ModelEngine()
