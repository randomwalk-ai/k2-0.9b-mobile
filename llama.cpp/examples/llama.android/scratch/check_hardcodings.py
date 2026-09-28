import os

target_dir = "app/src/main"
hardcoded_terms = ["lola", "potta", "pranav", "madhu", "siddharth", "arjun", "dei"]

found = False
for root, dirs, files in os.walk(target_dir):
    for f in files:
        if f.endswith(".kt"):
            p = os.path.join(root, f)
            with open(p, "r", encoding="utf-8") as file:
                lines = file.readlines()
            for i, line in enumerate(lines):
                for term in hardcoded_terms:
                    if term in line.lower():
                        print(f"[{term}] {p}:{i+1} -> {line.strip()}")
                        found = True

if not found:
    print("Zero hardcoded terms found in app/src/main!")
