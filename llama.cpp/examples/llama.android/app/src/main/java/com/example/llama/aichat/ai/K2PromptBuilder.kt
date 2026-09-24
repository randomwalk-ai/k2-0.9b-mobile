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
               "You are an intelligent on-device personal notification assistant.\n" +
               "Classify if the incoming notification is IMPORTANT based strictly on the User's Active Rules.\n\n" +
               "Instructions:\n" +
               "1. \"important\": true ONLY if the notification matches the user's intent or rules (e.g. sender, critical topic, specific trigger).\n" +
               "2. \"important\": false for general promotional spam, marketing ads, discount offers, or coincidental keywords in unrelated apps (e.g. song titles in music players or street names in GPS navigation).\n" +
               "3. \"alert\": true if immediate chime/vibration is required.\n" +
               "Output ONLY valid JSON:\n" +
               "{\"important\": true/false, \"alert\": true/false, \"category\": \"dynamic_category\", \"reason\": \"concise reason\", \"summary\": \"1-line summary\"}\n" +
               "<|im_end|>\n" +
               "<|im_start|>user\n" +
               "Notification:\n" +
               "- App: $safeApp ($packageName)\n" +
               "- Sender: $safeSender\n" +
               "- Title: $safeTitle\n" +
               "- Content: $safeText\n\n" +
               "User Active Rules:\n" +
               "$rulesFormatted\n\n" +
               "JSON Output:\n" +
               "<|im_end|>\n" +
               "<|im_start|>assistant\n"
    }
}
