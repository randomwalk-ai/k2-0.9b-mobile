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
        val safeSender = if (!sender.isNullOrBlank() && sender != safeApp) sender else "N/A"
        val safeTitle = title?.ifBlank { "" } ?: ""
        val safeText = text?.ifBlank { "" } ?: ""

        val rulesFormatted = if (rules.isEmpty()) {
            "- (No active rules)"
        } else {
            rules.joinToString("\n") { "- ${it.trim()}" }
        }

        return "<|im_start|>system\n" +
               "You are an on-device personal notification filter. Evaluate if the notification is IMPORTANT based strictly on the User's Active Rules.\n" +
               "Rules:\n" +
               "$rulesFormatted\n\n" +
               "Instructions:\n" +
               "1. \"important\": true if the notification genuinely matches the user's intent or rules (e.g. sender, critical topic, urgent transaction, direct actionable message).\n" +
               "2. \"important\": false for promotional ads, discount offers, newsletter blasts, or coincidental keyword matches in unrelated apps (e.g. a song title or street name).\n" +
               "3. \"alert\": true ONLY if this notification requires immediate chime/vibration.\n" +
               "Output ONLY a single JSON object in this format:\n" +
               "{\"important\": true/false, \"alert\": true/false, \"reason\": \"concise explanation\"}\n" +
               "<|im_end|>\n" +
               "<|im_start|>user\n" +
               "App: $safeApp ($packageName)\n" +
               "Sender: $safeSender\n" +
               "Title: $safeTitle\n" +
               "Content: $safeText\n\n" +
               "JSON:\n" +
               "<|im_end|>\n" +
               "<|im_start|>assistant\n" +
               "{\"important\":"
    }
}
