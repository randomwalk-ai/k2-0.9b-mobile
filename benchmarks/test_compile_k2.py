import time
import json
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer, StoppingCriteria, StoppingCriteriaList

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

tok = AutoTokenizer.from_pretrained('IFM/K2-Horizon-0.9B', trust_remote_code=True)
m = AutoModelForCausalLM.from_pretrained(
    'IFM/K2-Horizon-0.9B', 
    dtype=torch.float32, 
    low_cpu_mem_usage=True, 
    trust_remote_code=True
)
m.eval()

rule = "any message from rahul when he sounds frustrated or passive aggressive is not important"

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
user_content = f"Compile this rule:\n\"{rule}\""

prompt = (
    f"<|ifm|im_start|>system\n{system_content}<|ifm|im_end|>\n"
    f"<|ifm|im_start|>user\n{user_content}<|ifm|im_end|>\n"
    f"<|ifm|im_start|>assistant\n"
)

inputs = tok(prompt, return_tensors="pt")
prompt_len = inputs.input_ids.shape[1]
stop_criteria = StoppingCriteriaList([JsonStopCriteria(tok, prompt_len)])

print("Running K2 generation with JsonStopCriteria...")
t0 = time.time()
with torch.inference_mode():
    outputs = m.generate(
        **inputs,
        max_new_tokens=150,
        temperature=0.1,
        do_sample=False,
        stopping_criteria=stop_criteria,
        pad_token_id=tok.eos_token_id
    )

elapsed = time.time() - t0
gen_tokens = outputs[0][prompt_len:]
gen_text = tok.decode(gen_tokens, skip_special_tokens=False)

print(f"Generated {len(gen_tokens)} tokens in {elapsed:.2f}s ({len(gen_tokens)/elapsed:.1f} tok/s):")
print("-" * 50)
print(gen_text)
print("-" * 50)
