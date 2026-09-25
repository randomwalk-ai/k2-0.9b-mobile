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
               "You are an on-device personal notification filter. Your ONLY job is to check if an incoming notification matches the user's explicit rules.\n\n" +
               "Active User Rules:\n" +
               "$rulesFormatted\n\n" +
               "CRITICAL RULES:\n" +
               "1. DEFAULT IS FALSE: If the sender or message content does NOT match any of the Active User Rules above, you MUST return \"important\": false, \"alert\": false, \"reason\": \"No matching rule\".\n" +
               "2. DO NOT mark a message important just because it is a personal chat, direct message, or emotional text. It MUST explicitly match a user rule above.\n" +
               "3. If a negative rule matches (e.g. \"movies from Arjun not important\"), you MUST return \"important\": false, \"alert\": false.\n" +
               "4. Set \"important\": true and \"alert\": true ONLY when the notification directly satisfies a positive active rule.\n\n" +
               "Output ONLY a single JSON object in the exact format:\n" +
               "{\"important\": true/false, \"alert\": true/false, \"reason\": \"concise reason\", \"category\": \"messages/job/financial/other\"}\n" +
               "<|im_end|>\n" +
               "<|im_start|>user\n" +
               "App: $safeApp ($packageName)\n" +
               "Sender: $safeSender\n" +
               "Title: $safeTitle\n" +
               "Content: $safeText\n\n" +
               "JSON:\n" +
               "<|im_end|>\n" +
               "<|im_start|>assistant\n"
    }
}
