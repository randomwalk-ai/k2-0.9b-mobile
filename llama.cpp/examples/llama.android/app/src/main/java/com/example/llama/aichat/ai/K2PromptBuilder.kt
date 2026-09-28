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
               "1. NEGATION & EXCLUSION RULES (HIGHEST PRIORITY): If a rule states that messages from a person/app when angry, sending reels, or under certain conditions are NOT important / muted, and the incoming notification meets that negative condition, you MUST output \"important\": false, \"alert\": false.\n" +
               "2. TONE, SLANG & IDIOM AWARENESS: Distinguish playful banter, food cravings, and idioms (e.g. 'I could slap you for Lola's pizza', 'kill for a coffee', 'dei potta') from genuine hostility or serious anger. Food cravings and friendly jokes are NOT angry.\n" +
               "3. POSITIVE RULES: If the notification matches a positive rule (and violates no exclusions), set \"important\": true, \"alert\": true.\n" +
               "4. DEFAULT: If no rule matches, set \"important\": false, \"alert\": false, \"reason\": \"No matching rule\".\n\n" +
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
}
