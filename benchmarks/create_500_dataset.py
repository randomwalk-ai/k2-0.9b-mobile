import json

with open("test_cases_1000.json", "r", encoding="utf-8") as f:
    all_cases = json.load(f)

# Group selection quotas to reach exactly 500 balanced cases
quotas = {
    "Group 1: K2 Deep AI": 250,
    "Group 2: AOT Conditional": 120,
    "Group 3: App Filter": 50,
    "Group 4: Fast Contact & Call": 40,
    "Group 5: AOT Topic Filter": 20,
    "Group 6: Simple Block & Spam": 20
}

selected = []
group_collected = {k: 0 for k in quotas}

for c in all_cases:
    g = c.get("group", "")
    if g in quotas and group_collected[g] < quotas[g]:
        group_collected[g] += 1
        c_copy = dict(c)
        c_copy["id"] = len(selected) + 1
        selected.append(c_copy)

print(f"Total curated cases: {len(selected)}")
for g, cnt in group_collected.items():
    print(f"  {g}: {cnt}/{quotas[g]}")

with open("test_cases_500.json", "w", encoding="utf-8") as f:
    json.dump(selected, f, indent=2, ensure_ascii=False)

print("Saved successfully to test_cases_500.json")
