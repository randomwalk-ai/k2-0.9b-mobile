import os
import subprocess
import time

EDGE_PATH = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
OUTPUT_DIR = r"c:\Projects\k2-horizon\static\benchmark_images"
SCRATCH_DIR = r"c:\Projects\k2-horizon\scratch_visuals"
os.makedirs(OUTPUT_DIR, exist_ok=True)
os.makedirs(SCRATCH_DIR, exist_ok=True)

# Common HTML template with Tailwind CSS & Google Fonts
def wrap_card(content_html, width=900, height=480):
    return f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;600;700&display=swap" rel="stylesheet">
  <style>
    body {{
      font-family: 'Inter', sans-serif;
      margin: 0;
      padding: 0;
      background: #0f172a;
      display: flex;
      align-items: center;
      justify-content: center;
      width: {width}px;
      height: {height}px;
      box-sizing: border-box;
    }}
    .mono {{ font-family: 'JetBrains Mono', monospace; }}
  </style>
</head>
<body class="p-6">
  {content_html}
</body>
</html>"""

# ----------------------------------------------------
# 1. Card 1: Passive-Aggressive Nuance Comparison
# ----------------------------------------------------
card1_html = wrap_card("""
<div class="w-full bg-slate-900 border border-slate-700/80 rounded-2xl p-6 shadow-2xl space-y-4">
  <!-- Header -->
  <div class="flex items-center justify-between border-b border-slate-800 pb-3">
    <div class="flex items-center gap-2.5">
      <span class="px-2.5 py-1 rounded-md bg-amber-500/20 text-amber-300 text-xs font-bold mono border border-amber-500/30">STRESS CASE #1</span>
      <h3 class="text-white text-base font-bold m-0">Passive-Aggressive Frustration Detection</h3>
    </div>
    <span class="text-xs text-slate-400 mono">Rule: Mute Rahul when frustrated / angry</span>
  </div>

  <!-- Notification Banner -->
  <div class="bg-slate-950 border border-slate-800 rounded-xl p-3.5 flex items-center justify-between">
    <div class="flex items-center gap-3">
      <div class="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-xs">WA</div>
      <div>
        <div class="text-xs text-slate-400">Incoming Notification • WhatsApp from <strong>Rahul</strong></div>
        <div class="text-slate-100 text-sm italic font-medium">"Sure, do whatever you want, clearly my time is not valuable."</div>
      </div>
    </div>
    <span class="px-3 py-1 rounded bg-slate-800 text-slate-300 text-xs font-semibold mono">Expected: MUTE</span>
  </div>

  <!-- Model Comparison Grid -->
  <div class="grid grid-cols-2 gap-4 pt-1">
    <!-- K2 Box -->
    <div class="bg-emerald-950/30 border border-emerald-500/50 rounded-xl p-4 space-y-2.5">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="text-base">🤖</span>
          <span class="text-white font-bold text-sm">K2 Horizon 0.9B</span>
        </div>
        <span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 text-xs font-bold mono border border-emerald-500/40">✅ PASSED</span>
      </div>
      <div class="space-y-1.5 text-xs text-slate-300">
        <div class="flex justify-between"><span class="text-slate-400">Detected Tone:</span> <span class="text-emerald-300 font-semibold mono">Passive-Aggressive Frustration</span></div>
        <div class="flex justify-between"><span class="text-slate-400">Action:</span> <span class="text-emerald-400 font-bold mono">🔕 MUTE (Silenced)</span></div>
        <div class="flex justify-between"><span class="text-slate-400">Reasoning:</span> <span class="text-slate-300 italic">"Clearly my time is not valuable" = dismissal</span></div>
      </div>
    </div>

    <!-- Llama Box -->
    <div class="bg-rose-950/20 border border-rose-500/40 rounded-xl p-4 space-y-2.5">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="text-base">🦙</span>
          <span class="text-white font-bold text-sm">Llama 3.2 1B</span>
        </div>
        <span class="px-2 py-0.5 rounded bg-rose-500/20 text-rose-300 text-xs font-bold mono border border-rose-500/40">❌ MISSED TONE</span>
      </div>
      <div class="space-y-1.5 text-xs text-slate-300">
        <div class="flex justify-between"><span class="text-slate-400">Detected Tone:</span> <span class="text-rose-300 font-semibold mono">Neutral / Agreement ("Sure")</span></div>
        <div class="flex justify-between"><span class="text-slate-400">Action:</span> <span class="text-rose-400 font-bold mono">🚨 ALERT (False Alarm)</span></div>
        <div class="flex justify-between"><span class="text-slate-400">Reasoning:</span> <span class="text-slate-400 italic">Parsed "do whatever you want" as neutral permission</span></div>
      </div>
    </div>
  </div>
</div>
""", width=860, height=440)

# ----------------------------------------------------
# 2. Card 2: High-Urgency Work Escalation
# ----------------------------------------------------
card2_html = wrap_card("""
<div class="w-full bg-slate-900 border border-slate-700/80 rounded-2xl p-6 shadow-2xl space-y-4">
  <!-- Header -->
  <div class="flex items-center justify-between border-b border-slate-800 pb-3">
    <div class="flex items-center gap-2.5">
      <span class="px-2.5 py-1 rounded-md bg-rose-500/20 text-rose-300 text-xs font-bold mono border border-rose-500/30">STRESS CASE #2</span>
      <h3 class="text-white text-base font-bold m-0">Critical Production Escalation</h3>
    </div>
    <span class="text-xs text-slate-400 mono">Rule: High priority alert on outages & emergencies</span>
  </div>

  <!-- Notification Banner -->
  <div class="bg-slate-950 border border-slate-800 rounded-xl p-3.5 flex items-center justify-between">
    <div class="flex items-center gap-3">
      <div class="w-8 h-8 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center font-bold text-xs">SLACK</div>
      <div>
        <div class="text-xs text-slate-400">Incoming Notification • Slack from <strong>DevOps Lead</strong></div>
        <div class="text-slate-100 text-sm italic font-medium">"Urgent! Production server is down, join the war room immediately."</div>
      </div>
    </div>
    <span class="px-3 py-1 rounded bg-rose-500/20 text-rose-300 text-xs font-bold mono border border-rose-500/30">Expected: ALERT</span>
  </div>

  <!-- Model Comparison Grid -->
  <div class="grid grid-cols-2 gap-4 pt-1">
    <!-- K2 Box -->
    <div class="bg-emerald-950/30 border border-emerald-500/50 rounded-xl p-4 space-y-2.5">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="text-base">🤖</span>
          <span class="text-white font-bold text-sm">K2 Horizon 0.9B</span>
        </div>
        <span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 text-xs font-bold mono border border-emerald-500/40">✅ 100% RELIABLE</span>
      </div>
      <div class="space-y-1.5 text-xs text-slate-300">
        <div class="flex justify-between"><span class="text-slate-400">Severity Match:</span> <span class="text-emerald-300 font-semibold mono">CRITICAL (Outage)</span></div>
        <div class="flex justify-between"><span class="text-slate-400">Action:</span> <span class="text-emerald-400 font-bold mono">🚨 HIGH-PRIORITY CHIME & VIBRATE</span></div>
        <div class="flex justify-between"><span class="text-slate-400">Execution:</span> <span class="text-emerald-300 font-semibold mono">⚡ Fast Path (&lt;0.2ms native)</span></div>
      </div>
    </div>

    <!-- Llama Box -->
    <div class="bg-indigo-950/30 border border-indigo-500/50 rounded-xl p-4 space-y-2.5">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="text-base">🦙</span>
          <span class="text-white font-bold text-sm">Llama 3.2 1B</span>
        </div>
        <span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 text-xs font-bold mono border border-emerald-500/40">✅ PASSED</span>
      </div>
      <div class="space-y-1.5 text-xs text-slate-300">
        <div class="flex justify-between"><span class="text-slate-400">Severity Match:</span> <span class="text-indigo-300 font-semibold mono">CRITICAL (Keyword 'Urgent')</span></div>
        <div class="flex justify-between"><span class="text-slate-400">Action:</span> <span class="text-indigo-400 font-bold mono">🚨 HIGH-PRIORITY ALERT</span></div>
        <div class="flex justify-between"><span class="text-slate-400">Execution:</span> <span class="text-indigo-300 font-semibold mono">⚡ Matched compiled rule</span></div>
      </div>
    </div>
  </div>
</div>
""", width=860, height=440)

# ----------------------------------------------------
# 3. Card 3: AOT Rule Compilation Accuracy
# ----------------------------------------------------
card3_html = wrap_card("""
<div class="w-full bg-slate-900 border border-slate-700/80 rounded-2xl p-6 shadow-2xl space-y-4">
  <!-- Header -->
  <div class="flex items-center justify-between border-b border-slate-800 pb-3">
    <div class="flex items-center gap-2.5">
      <span class="px-2.5 py-1 rounded-md bg-purple-500/20 text-purple-300 text-xs font-bold mono border border-purple-500/30">PHASE 1 BENCHMARK</span>
      <h3 class="text-white text-base font-bold m-0">AOT Rule Compiler: Natural Language to JSON Schema</h3>
    </div>
    <span class="text-xs text-emerald-400 font-bold mono">+60.0% Higher Accuracy</span>
  </div>

  <!-- User Input Rule -->
  <div class="bg-slate-950 border border-slate-800 rounded-xl p-3.5">
    <div class="text-xs text-slate-400 mb-1">User Plain-English Rule (Created Once):</div>
    <div class="text-purple-200 text-xs font-mono bg-purple-950/40 p-2 rounded border border-purple-800/40">
      "if any msg from madhu it is important, if she sends reels it is not important"
    </div>
  </div>

  <!-- Schema Output Comparison -->
  <div class="grid grid-cols-2 gap-4">
    <!-- K2 Compiler -->
    <div class="bg-emerald-950/20 border border-emerald-500/50 rounded-xl p-3.5 space-y-2">
      <div class="flex items-center justify-between">
        <span class="text-white text-xs font-bold flex items-center gap-1.5">🤖 K2 Horizon (86.7% Score)</span>
        <span class="text-[10px] bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded mono font-bold">13/15 RULES</span>
      </div>
      <pre class="text-[11px] font-mono text-emerald-300 bg-slate-950/80 p-2 rounded border border-emerald-900/50 overflow-hidden leading-snug">
{
  "contact": "Madhu",
  "action": "ALERT",
  "exclude_keywords": ["reel", "reels"]
}</pre>
    </div>

    <!-- Llama Compiler -->
    <div class="bg-rose-950/20 border border-rose-500/40 rounded-xl p-3.5 space-y-2">
      <div class="flex items-center justify-between">
        <span class="text-white text-xs font-bold flex items-center gap-1.5">🦙 Llama 3.2 1B (26.7% Score)</span>
        <span class="text-[10px] bg-rose-500/20 text-rose-300 px-2 py-0.5 rounded mono font-bold">4/15 RULES</span>
      </div>
      <pre class="text-[11px] font-mono text-rose-300 bg-slate-950/80 p-2 rounded border border-rose-900/50 overflow-hidden leading-snug">
{
  "contact": "Madhu",
  "action": "ALERT"
  // ⚠️ Excluded keywords dropped
}</pre>
    </div>
  </div>
</div>
""", width=860, height=450)

# ----------------------------------------------------
# 4. Card 4: 6-Category Full Stress Benchmark Matrix
# ----------------------------------------------------
card4_html = wrap_card("""
<div class="w-full bg-slate-900 border border-slate-700/80 rounded-2xl p-6 shadow-2xl space-y-4">
  <!-- Header -->
  <div class="flex items-center justify-between border-b border-slate-800 pb-3">
    <div>
      <h3 class="text-white text-base font-bold m-0">500 Stress Cases: Category Breakdown Matrix</h3>
      <p class="text-xs text-slate-400 m-0">Win rates across nuanced sentiment, conditional logic, and app routing</p>
    </div>
    <div class="flex gap-3 text-xs mono">
      <span class="text-emerald-400 font-bold">■ K2 Horizon (329/500)</span>
      <span class="text-indigo-400 font-bold">■ Llama 3.2 (205/500)</span>
    </div>
  </div>

  <!-- Rows -->
  <div class="space-y-2.5 text-xs">
    <!-- Category 1 -->
    <div class="space-y-1">
      <div class="flex justify-between text-slate-300">
        <span>Deep AI Emotional Nuance (250 cases)</span>
        <span class="mono"><strong class="text-emerald-400">K2: 58.4%</strong> vs <span class="text-indigo-400">Llama: 48.4%</span></span>
      </div>
      <div class="h-2 w-full bg-slate-800 rounded-full overflow-hidden flex">
        <div class="bg-emerald-500 h-2" style="width: 58.4%"></div>
      </div>
    </div>

    <!-- Category 2 -->
    <div class="space-y-1">
      <div class="flex justify-between text-slate-300">
        <span>AOT Conditional Exclusions (120 cases)</span>
        <span class="mono"><strong class="text-emerald-400">K2: 64.2%</strong> vs <span class="text-indigo-400">Llama: 40.8%</span></span>
      </div>
      <div class="h-2 w-full bg-slate-800 rounded-full overflow-hidden flex">
        <div class="bg-emerald-500 h-2" style="width: 64.2%"></div>
      </div>
    </div>

    <!-- Category 3 -->
    <div class="space-y-1">
      <div class="flex justify-between text-slate-300">
        <span>App Package Routing (50 cases)</span>
        <span class="mono"><strong class="text-emerald-400">K2: 90.0%</strong> vs <span class="text-indigo-400">Llama: 0.0%</span></span>
      </div>
      <div class="h-2 w-full bg-slate-800 rounded-full overflow-hidden flex">
        <div class="bg-emerald-500 h-2" style="width: 90.0%"></div>
      </div>
    </div>

    <!-- Category 4 -->
    <div class="space-y-1">
      <div class="flex justify-between text-slate-300">
        <span>Fast Contact & Call Triggering (40 cases)</span>
        <span class="mono"><strong class="text-emerald-400">K2: 62.5%</strong> vs <span class="text-indigo-400">Llama: 37.5%</span></span>
      </div>
      <div class="h-2 w-full bg-slate-800 rounded-full overflow-hidden flex">
        <div class="bg-emerald-500 h-2" style="width: 62.5%"></div>
      </div>
    </div>
  </div>
</div>
""", width=860, height=440)

# ----------------------------------------------------
# 5. Card 5: Overall Arena Executive Summary
# ----------------------------------------------------
card5_html = wrap_card("""
<div class="w-full bg-slate-900 border border-slate-700/80 rounded-2xl p-6 shadow-2xl space-y-4">
  <!-- Header -->
  <div class="flex items-center justify-between border-b border-slate-800 pb-3">
    <div class="flex items-center gap-2">
      <span class="text-lg">🏆</span>
      <h3 class="text-white text-base font-bold m-0">On-Device Model Arena: Head-to-Head Summary</h3>
    </div>
    <span class="px-3 py-1 rounded bg-emerald-500/20 text-emerald-300 text-xs font-bold mono border border-emerald-500/40">WINNER: K2 HORIZON 0.9B</span>
  </div>

  <div class="grid grid-cols-3 gap-3 text-center">
    <div class="bg-slate-950 p-3.5 rounded-xl border border-slate-800">
      <div class="text-slate-400 text-xs">Total Stress Cases</div>
      <div class="text-white font-extrabold text-xl mt-1 mono">500</div>
      <div class="text-[11px] text-slate-500 mt-0.5">Diverse Edge Cases</div>
    </div>
    <div class="bg-emerald-950/40 p-3.5 rounded-xl border border-emerald-500/40">
      <div class="text-emerald-300 text-xs font-bold">K2 Horizon 0.9B</div>
      <div class="text-emerald-400 font-extrabold text-xl mt-1 mono">65.8% (329/500)</div>
      <div class="text-[11px] text-emerald-300/80 mt-0.5">+24.8% Higher Accuracy</div>
    </div>
    <div class="bg-indigo-950/40 p-3.5 rounded-xl border border-indigo-500/40">
      <div class="text-indigo-300 text-xs font-bold">Llama 3.2 1B</div>
      <div class="text-indigo-400 font-extrabold text-xl mt-1 mono">41.0% (205/500)</div>
      <div class="text-[11px] text-slate-400 mt-0.5">Frequent schema drops</div>
    </div>
  </div>

  <div class="p-3 bg-slate-950/60 rounded-xl border border-slate-800 text-xs text-slate-300 flex items-center gap-2.5">
    <span class="text-emerald-400 font-bold text-sm">💡</span>
    <span><strong>Core Insight:</strong> At sub-1B scale, K2 Horizon's high reasoning density enables deterministic schema parsing and subtle sentiment detection where generalist models fail.</span>
  </div>
</div>
""", width=860, height=440)

# Save HTML files and take screenshots
cards = [
    ("benchmark_card_1_passive_aggressive.html", "benchmark_card_1_passive_aggressive.png", card1_html, 860, 440),
    ("benchmark_card_2_urgent_escalation.html", "benchmark_card_2_urgent_escalation.png", card2_html, 860, 440),
    ("benchmark_card_3_aot_compiler.html", "benchmark_card_3_aot_compiler.png", card3_html, 860, 450),
    ("benchmark_card_4_category_matrix.html", "benchmark_card_4_category_matrix.png", card4_html, 860, 440),
    ("benchmark_card_5_arena_summary.html", "benchmark_card_5_arena_summary.png", card5_html, 860, 440),
]

for html_name, png_name, html_content, w, h in cards:
    html_path = os.path.join(SCRATCH_DIR, html_name)
    png_path = os.path.join(OUTPUT_DIR, png_name)
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html_content)
    
    cmd = [
        EDGE_PATH,
        "--headless",
        "--disable-gpu",
        f"--window-size={w},{h}",
        f"--screenshot={png_path}",
        html_path
    ]
    subprocess.run(cmd, check=True)
    print(f"Generated: {png_name}")

print("All 5 benchmark images generated successfully!")
