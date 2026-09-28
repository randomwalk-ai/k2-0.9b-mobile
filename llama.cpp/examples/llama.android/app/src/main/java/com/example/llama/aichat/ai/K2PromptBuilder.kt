package com.example.llama.aichat.ai

object K2PromptBuilder {

    fun buildPrompt(
        rules: List<String>,
        appName: String,
        packageName: String,
        title: String?,
        text: String?,
        sender: String?
    ): String {
        val safeApp = appName.ifBlank { "App" }
        val safeSender = if (!sender.isNullOrBlank() && sender != safeApp) sender else (title?.ifBlank { "N/A" } ?: "N/A")
        val safeText = text?.ifBlank { title ?: "" } ?: ""

        val rulesFormatted = if (rules.isEmpty()) {
            "- No active rules"
        } else {
            rules.mapIndexed { idx, r -> "${idx + 1}. ${r.trim()}" }.joinToString("\n")
        }

        return "<|im_start|>system\n" +
               "You are an on-device personal notification assistant. Your job is to classify incoming notifications strictly according to the active user rules.\n\n" +
               "Active User Rules:\n" +
               "$rulesFormatted\n\n" +
               "Classification Guidelines:\n" +
               "1. NEGATIVE / EXCLUSION RULES (HIGHEST PRIORITY): If a rule specifies conditions where notifications are not important or should be muted, and the notification matches those conditions, output \"important\": false, \"alert\": false.\n" +
               "2. TONE & EMOTION: Distinguish true hostility, conflict, or anger from friendly banter, humor, exaggeration, and informal remarks. Non-hostile interactions must not be classified as angry.\n" +
               "3. POSITIVE RULES: If the notification satisfies an active positive rule without triggering any exclusion condition, output \"important\": true, \"alert\": true.\n" +
               "4. DEFAULT: If no rule matches, output \"important\": false, \"alert\": false, \"reason\": \"No matching rule\".\n\n" +
               "Output ONLY a single JSON object in the exact format:\n" +
               "{\"important\": true/false, \"alert\": true/false, \"reason\": \"brief explanation\", \"category\": \"messages/work/banking/delivery/other\"}\n" +
               "<|im_end|>\n" +
               "<|im_start|>user\n" +
               "Incoming Notification:\n" +
               "- App: $safeApp ($packageName)\n" +
               "- Sender: $safeSender\n" +
               "- Message: $safeText\n\n" +
               "Classify this notification in JSON:\n" +
               "<|im_end|>\n" +
               "<|im_start|>assistant\n" +
               "{"
    }

    fun buildTonePrompt(
        ruleText: String,
        targetTones: List<String>,
        appName: String,
        packageName: String,
        title: String?,
        text: String?,
        sender: String?
    ): String {
        val safeApp = appName.ifBlank { "App" }
        val safeSender = if (!sender.isNullOrBlank() && sender != safeApp) sender else (title?.ifBlank { "N/A" } ?: "N/A")
        val safeText = text?.ifBlank { title ?: "" } ?: ""
        val toneDescription = if (targetTones.isNotEmpty()) targetTones.joinToString(", ") else "emotion / tone condition in the rule"

        return "<|im_start|>system\n" +
               "You are an on-device emotion and tone analyzer. Determine if the incoming message genuinely exhibits the specific emotional tone ($toneDescription).\n\n" +
               "Rule Context: ${ruleText.trim()}\n" +
               "Target Emotional Tone: $toneDescription\n\n" +
               "Guidelines:\n" +
               "1. Set \"tone_matched\": true ONLY if the sender is genuinely expressing $toneDescription (e.g. true hostility, conflict, or stated emotion).\n" +
               "2. Set \"tone_matched\": false if the sender is calm, positive, neutral, discussing normal work/tasks, or engaging in friendly casual banter.\n" +
               "3. Distinguish actual emotional hostility from harmless remarks and friendly hyperbole.\n\n" +
               "Output ONLY JSON in the exact format:\n" +
               "{\"tone_matched\": true/false, \"reason\": \"brief explanation\"}\n" +
               "<|im_end|>\n" +
               "<|im_start|>user\n" +
               "Sender: $safeSender ($safeApp)\n" +
               "Message: $safeText\n\n" +
               "Analyze tone in JSON:\n" +
               "<|im_end|>\n" +
               "<|im_start|>assistant\n" +
               "{"
    }
}
