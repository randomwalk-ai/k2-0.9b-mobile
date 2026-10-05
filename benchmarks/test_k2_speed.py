import time
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer

t0 = time.time()
tok = AutoTokenizer.from_pretrained('IFM/K2-Horizon-0.9B', trust_remote_code=True)
m = AutoModelForCausalLM.from_pretrained(
    'IFM/K2-Horizon-0.9B', 
    torch_dtype=torch.float32, 
    low_cpu_mem_usage=True, 
    trust_remote_code=True
)
m.eval()
print(f"FP32 loaded in: {time.time()-t0:.2f}s")

inp = tok('<|ifm|im_start|>user\nIs this urgent: "Accident on highway 4"? Reply JSON: {"urgent": true/false}<|ifm|im_end|>\n<|ifm|im_start|>assistant\n', return_tensors='pt')
t1 = time.time()
with torch.inference_mode():
    out = m.generate(**inp, max_new_tokens=30, do_sample=False)
print(f"FP32 gen in {time.time()-t1:.2f}s: {tok.decode(out[0])}")
