// K2 Horizon vs Llama 3.2 1B Test Site Logic & Benchmark Dashboard

let rawRules = [];
let allTestCases = [];
let filteredCases = [];
let benchmarkResults = null;
let isEvaluating = false;
let isBenchmarking = false;
let isCompilingRules = false;

// Filter states
let activeGroupFilter = 'all';
let activeRuleFilter = '';
let activeActionFilter = '';
let searchDebounceTimer = null;

// Chart Instances
let chartAccuracy = null;
let chartCategories = null;
let chartErrorMatrix = null;
let chartRouting = null;

document.addEventListener('DOMContentLoaded', async () => {
  await loadModelStatus();
  await loadRules();
  await loadTestCases();
  await loadPrecomputedBenchmarkResults();

  // Single Arena Run Listeners
  document.getElementById('btnCompareBoth').addEventListener('click', () => runSingleEvaluation('both'));
  document.getElementById('btnRunK2').addEventListener('click', () => runSingleEvaluation('k2'));
  document.getElementById('btnRunLlama').addEventListener('click', () => runSingleEvaluation('llama'));
});

// Tab Switching
function switchTab(tabId) {
  document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

  if (tabId === 'arena') {
    document.getElementById('tabBtnArena').classList.add('active');
    document.getElementById('tabArena').classList.add('active');
  } else if (tabId === 'compiler') {
    document.getElementById('tabBtnCompiler').classList.add('active');
    document.getElementById('tabCompiler').classList.add('active');
  } else if (tabId === 'matrix') {
    document.getElementById('tabBtnMatrix').classList.add('active');
    document.getElementById('tabMatrix').classList.add('active');
  } else if (tabId === 'analytics') {
    document.getElementById('tabBtnAnalytics').classList.add('active');
    document.getElementById('tabAnalytics').classList.add('active');
    renderAnalyticsCharts();
  }
}

// ------------------------------------------------------------------------------
// Model & Rules Loading
// ------------------------------------------------------------------------------
async function loadModelStatus() {
  try {
    const res = await fetch('/api/test/models');
    const data = await res.json();
    if (data.k2_0_9b) {
      document.getElementById('k2StatusBadge').title = data.k2_0_9b.backend;
    }
    if (data.llama_3_2_1b) {
      document.getElementById('llamaStatusBadge').title = data.llama_3_2_1b.backend;
    }
  } catch (e) {
    console.error('Failed to load model status:', e);
  }
}

async function loadRules() {
  try {
    const res = await fetch('/api/test/rules');
    const data = await res.json();
    rawRules = data.rules || [];
    renderRulesList();
    renderCompilerRulesList();

    if (data.compiled && Array.isArray(data.compiled)) {
      data.compiled.forEach(item => {
        renderRuleCompilationResult(item);
      });
    }

    if (rawRules.length > 0) {
      selectRule(0);
    }
  } catch (e) {
    console.error('Failed to load rules:', e);
  }
}

function renderRulesList() {
  const container = document.getElementById('rulesList');
  container.innerHTML = '';

  rawRules.forEach((ruleStr, idx) => {
    const item = document.createElement('div');
    item.className = 'rule-item';
    item.innerHTML = `
      <div class="rule-header">
        <span class="rule-num">Rule #${idx + 1}</span>
      </div>
      <div class="rule-text-preview">${escapeHtml(ruleStr)}</div>
    `;
    item.addEventListener('click', () => selectRule(idx));
    container.appendChild(item);
  });
}

function selectRule(index) {
  const ruleStr = rawRules[index];
  if (!ruleStr) return;

  document.querySelectorAll('.rule-item').forEach((el, i) => {
    el.classList.toggle('selected', i === index);
  });

  document.getElementById('ruleInput').value = ruleStr;
}

// ------------------------------------------------------------------------------
// TAB 2: AOT Rule Compilation Runner & Rendering
// ------------------------------------------------------------------------------
function renderCompilerRulesList() {
  const grid = document.getElementById('compilerRulesGrid');
  grid.innerHTML = '';

  rawRules.forEach((ruleStr, idx) => {
    const card = document.createElement('div');
    card.className = 'panel';
    card.id = `rule-card-${idx + 1}`;
    card.innerHTML = `
      <div class="panel-header" style="background: rgba(30, 41, 59, 0.5);">
        <div style="font-weight: 700; font-size: 0.9rem; color: #fff;">
          <span style="color: var(--accent-k2);">Rule #${idx + 1}:</span> "${escapeHtml(ruleStr)}"
        </div>
        <button class="btn-test-action" onclick="compileSingleRule(${idx})">⚡ Re-Compile</button>
      </div>

      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; padding: 1.25rem;">
        <!-- K2 Column -->
        <div style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 8px; padding: 1rem;" id="k2-compile-${idx + 1}">
          <div style="font-size: 0.8rem; font-weight: 700; color: #c4b5fd; margin-bottom: 0.5rem; display: flex; justify-content: space-between;">
            <span>🤖 K2 Horizon 0.9B Classification</span>
            <span class="json-badge" id="k2-tag-${idx + 1}" style="display:none;"></span>
          </div>
          <pre class="json-viewer" id="k2-schema-${idx + 1}" style="max-height: 180px;">// Awaiting AOT compilation...</pre>
        </div>

        <!-- Llama Column -->
        <div style="background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 8px; padding: 1rem;" id="llama-compile-${idx + 1}">
          <div style="font-size: 0.8rem; font-weight: 700; color: #93c5fd; margin-bottom: 0.5rem; display: flex; justify-content: space-between;">
            <span>🦙 Llama 3.2 1B Classification</span>
            <span class="json-badge" id="llama-tag-${idx + 1}" style="display:none;"></span>
          </div>
          <pre class="json-viewer" id="llama-schema-${idx + 1}" style="max-height: 180px;">// Awaiting AOT compilation...</pre>
        </div>
      </div>
    `;
    grid.appendChild(card);
  });
}

function renderRuleCompilationResult(data) {
  const idx = data.rule_index;

  // Render K2
  if (data.k2) {
    const k2Viewer = document.getElementById(`k2-schema-${idx}`);
    const k2Tag = document.getElementById(`k2-tag-${idx}`);
    if (k2Viewer) {
      if (data.k2.schema) {
        k2Viewer.innerText = JSON.stringify(data.k2.schema, null, 2);
        k2Tag.style.display = 'inline-block';
        k2Tag.className = data.k2.is_valid_json ? 'json-badge valid' : 'json-badge invalid';
        const isComplex = (data.k2.schema.rule_type === 'COMPLEX_RULE');
        k2Tag.style.background = isComplex ? 'rgba(139, 92, 246, 0.3)' : 'rgba(16, 185, 129, 0.3)';
        k2Tag.style.color = isComplex ? '#c4b5fd' : '#34d399';
        k2Tag.innerText = isComplex ? '🧠 COMPLEX (DEEP AI)' : '⚡ SIMPLE (AOT)';
      } else {
        k2Viewer.innerText = data.k2.raw_output || '// No valid JSON generated';
        k2Tag.style.display = 'inline-block';
        k2Tag.className = 'json-badge invalid';
        k2Tag.innerText = 'RAW OUTPUT';
      }
    }
  }

  // Render Llama
  if (data.llama) {
    const llamaViewer = document.getElementById(`llama-schema-${idx}`);
    const llamaTag = document.getElementById(`llama-tag-${idx}`);
    if (llamaViewer) {
      if (data.llama.schema) {
        llamaViewer.innerText = JSON.stringify(data.llama.schema, null, 2);
        llamaTag.style.display = 'inline-block';
        llamaTag.className = data.llama.is_valid_json ? 'json-badge valid' : 'json-badge invalid';
        const isComplex = (data.llama.schema.rule_type === 'COMPLEX_RULE');
        llamaTag.style.background = isComplex ? 'rgba(139, 92, 246, 0.3)' : 'rgba(59, 130, 246, 0.3)';
        llamaTag.style.color = isComplex ? '#c4b5fd' : '#93c5fd';
        llamaTag.innerText = isComplex ? '🧠 COMPLEX (DEEP AI)' : '⚡ SIMPLE (AOT)';
      } else {
        llamaViewer.innerText = data.llama.raw_output || '// No valid JSON generated';
        llamaTag.style.display = 'inline-block';
        llamaTag.className = 'json-badge invalid';
        llamaTag.innerText = 'RAW OUTPUT';
      }
    }
  }
}

// ------------------------------------------------------------------------------
// TAB 3: Test Cases Data Table & Filtering
// ------------------------------------------------------------------------------
async function loadTestCases() {
  try {
    const res = await fetch('/api/test/cases?limit=1050');
    const data = await res.json();
    allTestCases = data.cases || [];
    document.getElementById('totalCasesBadge').innerText = data.total || allTestCases.length;
    applyFilters();
  } catch (e) {
    console.error('Failed to load test cases:', e);
  }
}

async function loadPrecomputedBenchmarkResults() {
  try {
    const res = await fetch('/api/test/benchmark-results');
    const data = await res.json();
    if (data && data.phase2_summary) {
      benchmarkResults = data;
      
      // Update KPI banner in Matrix tab
      const p2 = data.phase2_summary;
      if (p2.k2_horizon_0_9b && p2.llama_3_2_1b) {
        document.getElementById('k2ScoreAccuracy').innerText = `${p2.k2_horizon_0_9b.accuracy_percent}%`;
        document.getElementById('k2ScoreDetail').innerText = `${p2.k2_horizon_0_9b.correct_count} / ${p2.k2_horizon_0_9b.total_count} correct (AOT: ${p2.k2_horizon_0_9b.aot_routed_count}, Deep AI: ${p2.k2_horizon_0_9b.deep_ai_routed_count})`;

        document.getElementById('llamaScoreAccuracy').innerText = `${p2.llama_3_2_1b.accuracy_percent}%`;
        document.getElementById('llamaScoreDetail').innerText = `${p2.llama_3_2_1b.correct_count} / ${p2.llama_3_2_1b.total_count} correct (AOT: ${p2.llama_3_2_1b.aot_routed_count}, Deep AI: ${p2.llama_3_2_1b.deep_ai_routed_count})`;

        document.getElementById('scoreEvaluated').innerText = `${data.total_test_cases} / ${data.total_test_cases}`;
        document.getElementById('scoreLatency').innerText = `${p2.k2_horizon_0_9b.avg_latency_ms}ms / ${p2.llama_3_2_1b.avg_latency_ms}ms`;

        // Update Analytics KPI cards
        document.getElementById('analyticsP2K2').innerText = `${p2.k2_horizon_0_9b.accuracy_percent}%`;
        document.getElementById('analyticsP2K2Sub').innerText = `${p2.k2_horizon_0_9b.correct_count} / ${p2.k2_horizon_0_9b.total_count} Correct (Missed: ${p2.k2_horizon_0_9b.missed_emergencies})`;

        document.getElementById('analyticsP2Llama').innerText = `${p2.llama_3_2_1b.accuracy_percent}%`;
        document.getElementById('analyticsP2LlamaSub').innerText = `${p2.llama_3_2_1b.correct_count} / ${p2.llama_3_2_1b.total_count} Correct (Missed: ${p2.llama_3_2_1b.missed_emergencies})`;
      }

      // Merge results into allTestCases
      if (data.cases && Array.isArray(data.cases)) {
        const resultMap = {};
        data.cases.forEach(c => { resultMap[c.id] = c; });
        allTestCases.forEach(tc => {
          if (resultMap[tc.id]) {
            tc.k2_result = resultMap[tc.id].k2;
            tc.llama_result = resultMap[tc.id].llama;
          }
        });
        applyFilters();
      }
    }
  } catch (e) {
    console.error('Failed to load precomputed benchmark:', e);
  }
}

function setGroupFilter(group) {
  activeGroupFilter = group;
  document.querySelectorAll('.filter-chip').forEach(chip => {
    chip.classList.toggle('active', chip.getAttribute('data-filter-group') === group);
  });
  applyFilters();
}

function onRuleFilterChanged() {
  activeRuleFilter = document.getElementById('filterRuleSelect').value;
  applyFilters();
}

function onActionFilterChanged() {
  activeActionFilter = document.getElementById('filterActionSelect').value;
  applyFilters();
}

function onSearchChanged() {
  clearTimeout(searchDebounceTimer);
  searchDebounceTimer = setTimeout(() => {
    applyFilters();
  }, 250);
}

function applyFilters() {
  const searchQuery = document.getElementById('testCaseSearch').value.trim().toLowerCase();

  filteredCases = allTestCases.filter(tc => {
    if (activeGroupFilter !== 'all') {
      if (!tc.group.toLowerCase().includes(activeGroupFilter.toLowerCase())) {
        return false;
      }
    }

    if (activeRuleFilter && tc.rule_id !== activeRuleFilter) {
      return false;
    }

    if (activeActionFilter && tc.ground_truth_action !== activeActionFilter) {
      return false;
    }

    if (searchQuery) {
      const matchText = (tc.text || '').toLowerCase().includes(searchQuery);
      const matchSender = (tc.sender || '').toLowerCase().includes(searchQuery);
      const matchApp = (tc.app || '').toLowerCase().includes(searchQuery);
      const matchReason = (tc.ground_truth_reason || '').toLowerCase().includes(searchQuery);
      if (!matchText && !matchSender && !matchApp && !matchReason) {
        return false;
      }
    }

    return true;
  });

  renderTestCasesTable();
}

function renderTestCasesTable() {
  const tbody = document.getElementById('testCasesTableBody');
  tbody.innerHTML = '';

  if (filteredCases.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">
          No matching test cases found for current filters.
        </td>
      </tr>
    `;
    return;
  }

  const displaySlice = filteredCases.slice(0, 200);

  displaySlice.forEach(tc => {
    const tr = document.createElement('tr');
    tr.id = `row-case-${tc.id}`;

    const isAlert = (tc.ground_truth_action === 'ALERT');

    // K2 Cell
    let k2Cell = `<span style="color: var(--text-muted); font-size: 0.8rem;">--</span>`;
    if (tc.k2_result) {
      const k2Ok = tc.k2_result.is_correct;
      const k2Act = tc.k2_result.action;
      const k2Eng = tc.k2_result.engine_used === 'AOT_FILTER' ? '⚡ AOT' : '🧠 LLM';
      k2Cell = `
        <div style="display: flex; align-items: center; gap: 0.4rem;">
          <span style="font-size: 0.9rem;">${k2Ok ? '✅' : '❌'}</span>
          <span class="badge-action ${k2Act === 'ALERT' ? 'alert' : 'mute'}" style="font-size: 0.72rem; padding: 0.15rem 0.4rem;">
            ${k2Act}
          </span>
          <span style="font-size: 0.68rem; color: #a78bfa;">${k2Eng}</span>
        </div>
      `;
    }

    // Llama Cell
    let llamaCell = `<span style="color: var(--text-muted); font-size: 0.8rem;">--</span>`;
    if (tc.llama_result) {
      const llamaOk = tc.llama_result.is_correct;
      const llamaAct = tc.llama_result.action;
      const llamaEng = tc.llama_result.engine_used === 'AOT_FILTER' ? '⚡ AOT' : '🧠 LLM';
      llamaCell = `
        <div style="display: flex; align-items: center; gap: 0.4rem;">
          <span style="font-size: 0.9rem;">${llamaOk ? '✅' : '❌'}</span>
          <span class="badge-action ${llamaAct === 'ALERT' ? 'alert' : 'mute'}" style="font-size: 0.72rem; padding: 0.15rem 0.4rem;">
            ${llamaAct}
          </span>
          <span style="font-size: 0.68rem; color: #93c5fd;">${llamaEng}</span>
        </div>
      `;
    }

    tr.innerHTML = `
      <td style="font-family: var(--font-mono); color: var(--text-muted); font-size: 0.8rem;">#${tc.id}</td>
      <td>
        <div style="font-weight: 600; color: #cbd5e1; font-size: 0.8rem;">${escapeHtml(tc.rule_name)}</div>
        <div style="font-size: 0.7rem; color: var(--text-muted);">${escapeHtml(tc.group.split(':')[0])}</div>
      </td>
      <td style="font-weight: 600;">${escapeHtml(tc.sender)}</td>
      <td><span style="font-size: 0.78rem; background: var(--bg-secondary); padding: 0.15rem 0.45rem; border-radius: 4px; border: 1px solid var(--border-color);">${escapeHtml(tc.app)}</span></td>
      <td>
        <div style="max-width: 380px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #e2e8f0;" title="${escapeHtml(tc.text)}">
          "${escapeHtml(tc.text)}"
        </div>
        <div style="font-size: 0.72rem; color: var(--text-muted); margin-top: 2px;">
          💡 ${escapeHtml(tc.ground_truth_reason)}
        </div>
      </td>
      <td>
        <span class="badge-action ${isAlert ? 'alert' : 'mute'}">
          ${isAlert ? '🚨 ALERT' : '🔕 MUTE'}
        </span>
      </td>
      <td>${k2Cell}</td>
      <td>${llamaCell}</td>
      <td style="text-align: right;">
        <button class="btn-test-action" onclick="loadCaseIntoArena(${tc.id})">⚡ Test</button>
      </td>
    `;
    tbody.appendChild(tr);
  });

  if (filteredCases.length > 200) {
    const trMore = document.createElement('tr');
    trMore.innerHTML = `
      <td colspan="9" style="text-align: center; color: var(--text-muted); padding: 1rem; font-size: 0.8rem;">
        Showing first 200 of ${filteredCases.length} filtered cases. (Use Search or Filters to narrow down).
      </td>
    `;
    tbody.appendChild(trMore);
  }
}

function loadCaseIntoArena(caseId) {
  const tc = allTestCases.find(c => c.id === caseId);
  if (!tc) return;

  let rIdx = 0;
  if (tc.rule_id && tc.rule_id.startsWith('rule_')) {
    rIdx = parseInt(tc.rule_id.split('_')[1]) - 1;
  }
  const ruleText = rawRules[rIdx] || rawRules[0];

  document.getElementById('ruleInput').value = ruleText;
  document.getElementById('appSelect').value = tc.app;
  document.getElementById('senderInput').value = tc.sender;
  document.getElementById('messageInput').value = tc.text;
  document.getElementById('isCallCheckbox').checked = Boolean(tc.is_call);

  switchTab('arena');
  runSingleEvaluation('both');
}

// ------------------------------------------------------------------------------
// Single Arena Evaluation Runner
// ------------------------------------------------------------------------------
async function runSingleEvaluation(modelChoice = 'both') {
  if (isEvaluating) return;
  isEvaluating = true;

  const ruleText = document.getElementById('ruleInput').value.trim();
  const appName = document.getElementById('appSelect').value;
  const senderName = document.getElementById('senderInput').value.trim();
  const messageText = document.getElementById('messageInput').value.trim();
  const isCall = document.getElementById('isCallCheckbox').checked;

  if (!ruleText || !messageText) {
    alert('Please enter a rule and notification message text.');
    isEvaluating = false;
    return;
  }

  setButtonsDisabled(true);

  // Clear previous displays
  if (modelChoice === 'both' || modelChoice === 'k2') {
    document.getElementById('k2RawOutput').innerText = 'Initializing K2 stream...';
    document.getElementById('k2JsonBadge').style.display = 'none';
    document.getElementById('k2JsonContainer').innerText = '// JSON will appear on completion...';
    document.getElementById('k2ReasonText').innerText = 'Awaiting model inference...';
    document.getElementById('k2Ttft').innerText = '--';
    document.getElementById('k2Tps').innerText = '--';
    document.getElementById('k2TotalTime').innerText = '--';
  }

  if (modelChoice === 'both' || modelChoice === 'llama') {
    document.getElementById('llamaRawOutput').innerText = 'Initializing Llama stream...';
    document.getElementById('llamaJsonBadge').style.display = 'none';
    document.getElementById('llamaJsonContainer').innerText = '// JSON will appear on completion...';
    document.getElementById('llamaReasonText').innerText = 'Awaiting model inference...';
    document.getElementById('llamaTtft').innerText = '--';
    document.getElementById('llamaTps').innerText = '--';
    document.getElementById('llamaTotalTime').innerText = '--';
  }

  try {
    const response = await fetch('/api/test/evaluate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: modelChoice,
        rules: ruleText,
        app: appName,
        sender: senderName,
        text: messageText,
        is_call: isCall
      })
    });

    if (!response.ok) {
      throw new Error(`Server returned ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop();

      let currentEvent = 'message';

      for (let line of lines) {
        line = line.trim();
        if (!line) continue;

        if (line.startsWith('event: ')) {
          currentEvent = line.substring(7);
        } else if (line.startsWith('data: ')) {
          const dataStr = line.substring(6);
          try {
            const data = JSON.parse(dataStr);
            handleStreamEvent(currentEvent, data);
          } catch (e) {
            console.error('Error parsing SSE data:', e, dataStr);
          }
        }
      }
    }

  } catch (err) {
    console.error('Evaluation stream error:', err);
    alert('Inference stream error: ' + err.message);
  } finally {
    isEvaluating = false;
    setButtonsDisabled(false);
  }
}

function handleStreamEvent(event, data) {
  if (event === 'k2_status') {
    document.getElementById('k2RawOutput').innerText = `[Status] ${data.message}`;
  } else if (event === 'k2_token') {
    document.getElementById('k2RawOutput').innerText = data.full_text;
  } else if (event === 'k2_final') {
    renderFinalResult('k2', data);
  } else if (event === 'llama_status') {
    document.getElementById('llamaRawOutput').innerText = `[Status] ${data.message}`;
  } else if (event === 'llama_token') {
    document.getElementById('llamaRawOutput').innerText = data.full_text;
  } else if (event === 'llama_final') {
    renderFinalResult('llama', data);
  }
}

function renderFinalResult(modelKey, data) {
  const isK2 = (modelKey === 'k2');
  const badge = document.getElementById(isK2 ? 'k2DecisionBadge' : 'llamaDecisionBadge');
  const jsonBadge = document.getElementById(isK2 ? 'k2JsonBadge' : 'llamaJsonBadge');
  const jsonContainer = document.getElementById(isK2 ? 'k2JsonContainer' : 'llamaJsonContainer');
  const reasonText = document.getElementById(isK2 ? 'k2ReasonText' : 'llamaReasonText');

  document.getElementById(isK2 ? 'k2Ttft' : 'llamaTtft').innerText = `${data.metrics.ttft_ms} ms`;
  document.getElementById(isK2 ? 'k2Tps' : 'llamaTps').innerText = `${data.metrics.tok_per_sec} tok/s`;
  document.getElementById(isK2 ? 'k2TotalTime' : 'llamaTotalTime').innerText = `${data.metrics.total_ms} ms`;

  jsonBadge.style.display = 'inline-block';
  if (data.is_valid_json) {
    jsonBadge.className = 'json-badge valid';
    jsonBadge.innerText = 'VALID JSON';
  } else {
    jsonBadge.className = 'json-badge invalid';
    jsonBadge.innerText = 'INVALID JSON';
  }

  if (data.parsed_json) {
    jsonContainer.innerText = JSON.stringify(data.parsed_json, null, 2);
    const isAlert = data.parsed_json.alert === true || data.parsed_json.important === true;
    if (isAlert) {
      badge.className = 'decision-badge alert';
      badge.innerText = '🚨 ALERT USER';
    } else {
      badge.className = 'decision-badge mute';
      badge.innerText = '🔕 MUTE / SILENCE';
    }
    reasonText.innerText = data.parsed_json.reason || data.parsed_json.summary || 'Decision rendered by model.';
  } else {
    jsonContainer.innerText = data.raw_output || 'No output generated.';
    badge.className = 'decision-badge mute';
    badge.innerText = '⚠️ PARSE ERROR';
    reasonText.innerText = 'Model output could not be parsed into valid schema.';
  }
}

function setButtonsDisabled(disabled) {
  document.getElementById('btnCompareBoth').disabled = disabled;
  document.getElementById('btnRunK2').disabled = disabled;
  document.getElementById('btnRunLlama').disabled = disabled;
}

// ------------------------------------------------------------------------------
// TAB 4: EXECUTIVE ANALYTICS & CHART.JS VISUALIZATIONS
// ------------------------------------------------------------------------------
function renderAnalyticsCharts() {
  if (typeof Chart === 'undefined') {
    console.warn('Chart.js not loaded yet');
    return;
  }

  // Chart 1: Phase 1 vs Phase 2 Accuracy
  const ctxAcc = document.getElementById('accuracyComparisonChart');
  if (ctxAcc) {
    if (chartAccuracy) chartAccuracy.destroy();
    chartAccuracy = new Chart(ctxAcc, {
      type: 'bar',
      data: {
        labels: ['Phase 1: AOT Compilation', 'Phase 2: Runtime Triage (500 Cases)'],
        datasets: [
          {
            label: '🤖 K2 Horizon 0.9B',
            data: [86.7, benchmarkResults ? benchmarkResults.phase2_summary.k2_horizon_0_9b.accuracy_percent : 88.4],
            backgroundColor: 'rgba(139, 92, 246, 0.85)',
            borderColor: '#8b5cf6',
            borderWidth: 1,
            borderRadius: 6
          },
          {
            label: '🦙 Llama 3.2 1B Instruct',
            data: [26.7, benchmarkResults ? benchmarkResults.phase2_summary.llama_3_2_1b.accuracy_percent : 41.2],
            backgroundColor: 'rgba(59, 130, 246, 0.85)',
            borderColor: '#3b82f6',
            borderWidth: 1,
            borderRadius: 6
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            max: 100,
            ticks: { color: '#94a3b8', callback: val => val + '%' },
            grid: { color: 'rgba(255, 255, 255, 0.05)' }
          },
          x: {
            ticks: { color: '#cbd5e1' },
            grid: { display: false }
          }
        },
        plugins: {
          legend: { labels: { color: '#fff', font: { weight: '600' } } }
        }
      }
    });
  }

  // Chart 2: Category Breakdown Performance
  const ctxCat = document.getElementById('categoryBreakdownChart');
  if (ctxCat) {
    if (chartCategories) chartCategories.destroy();
    
    let catLabels = ['Deep AI Tone', 'AOT Conditional', 'App Filters', 'Fast Contacts', 'Spam Block'];
    let k2CatData = [58.4, 64.2, 90.0, 62.5, 80.0];
    let llamaCatData = [48.4, 40.8, 0.0, 37.5, 100.0];

    if (benchmarkResults && benchmarkResults.group_breakdown) {
      const gb = benchmarkResults.group_breakdown;
      catLabels = [];
      k2CatData = [];
      llamaCatData = [];
      for (const [gName, gStat] of Object.entries(gb)) {
        const shortName = gName.replace(/Group \d+: /, '');
        catLabels.push(shortName);
        k2CatData.push(Number(((gStat.k2_correct / gStat.total) * 100).toFixed(1)));
        llamaCatData.push(Number(((gStat.llama_correct / gStat.total) * 100).toFixed(1)));
      }
    }

    chartCategories = new Chart(ctxCat, {
      type: 'bar',
      data: {
        labels: catLabels,
        datasets: [
          {
            label: '🤖 K2 Horizon 0.9B',
            data: k2CatData,
            backgroundColor: 'rgba(167, 139, 250, 0.85)',
            borderColor: '#8b5cf6',
            borderWidth: 1,
            borderRadius: 4
          },
          {
            label: '🦙 Llama 3.2 1B',
            data: llamaCatData,
            backgroundColor: 'rgba(96, 165, 250, 0.85)',
            borderColor: '#3b82f6',
            borderWidth: 1,
            borderRadius: 4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            max: 100,
            ticks: { color: '#94a3b8', callback: val => val + '%' },
            grid: { color: 'rgba(255, 255, 255, 0.05)' }
          },
          x: {
            ticks: { color: '#cbd5e1', font: { size: 11 } },
            grid: { display: false }
          }
        },
        plugins: {
          legend: { labels: { color: '#fff' } }
        }
      }
    });
  }

  // Chart 3: Error Matrix (Missed Emergencies vs False Positives)
  const ctxErr = document.getElementById('errorMatrixChart');
  if (ctxErr) {
    if (chartErrorMatrix) chartErrorMatrix.destroy();
    const k2Missed = benchmarkResults ? benchmarkResults.phase2_summary.k2_horizon_0_9b.missed_emergencies : 117;
    const k2False = benchmarkResults ? benchmarkResults.phase2_summary.k2_horizon_0_9b.false_alerts : 52;
    const llamaMissed = benchmarkResults ? benchmarkResults.phase2_summary.llama_3_2_1b.missed_emergencies : 259;
    const llamaFalse = benchmarkResults ? benchmarkResults.phase2_summary.llama_3_2_1b.false_alerts : 36;

    chartErrorMatrix = new Chart(ctxErr, {
      type: 'bar',
      data: {
        labels: ['Missed Emergencies (🚨 Silent Drop)', 'False Interruptions (🔕 Ambient Noise)'],
        datasets: [
          {
            label: '🤖 K2 Horizon (Low Misses = Safe)',
            data: [k2Missed, k2False],
            backgroundColor: 'rgba(16, 185, 129, 0.85)',
            borderColor: '#10b981',
            borderWidth: 1,
            borderRadius: 6
          },
          {
            label: '🦙 Llama 3.2 1B (High Miss Risk)',
            data: [llamaMissed, llamaFalse],
            backgroundColor: 'rgba(239, 68, 68, 0.85)',
            borderColor: '#ef4444',
            borderWidth: 1,
            borderRadius: 6
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: { color: '#94a3b8' },
            grid: { color: 'rgba(255, 255, 255, 0.05)' }
          },
          x: {
            ticks: { color: '#cbd5e1', font: { size: 11 } },
            grid: { display: false }
          }
        },
        plugins: {
          legend: { labels: { color: '#fff' } }
        }
      }
    });
  }

  // Chart 4: Routing Distribution
  const ctxRoute = document.getElementById('routingDistributionChart');
  if (ctxRoute) {
    if (chartRouting) chartRouting.destroy();
    const aotCount = benchmarkResults ? benchmarkResults.phase2_summary.k2_horizon_0_9b.aot_routed_count : 332;
    const deepAiCount = benchmarkResults ? benchmarkResults.phase2_summary.k2_horizon_0_9b.deep_ai_routed_count : 168;

    chartRouting = new Chart(ctxRoute, {
      type: 'doughnut',
      data: {
        labels: [`⚡ AOT Engine (${aotCount} cases <0.2ms)`, `🧠 Deep AI LLM (${deepAiCount} cases)`],
        datasets: [
          {
            data: [aotCount, deepAiCount],
            backgroundColor: [
              'rgba(16, 185, 129, 0.85)',
              'rgba(139, 92, 246, 0.85)'
            ],
            borderColor: '#0f172a',
            borderWidth: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { color: '#cbd5e1', padding: 15 } }
        }
      }
    });
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
