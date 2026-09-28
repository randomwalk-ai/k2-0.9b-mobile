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
}
