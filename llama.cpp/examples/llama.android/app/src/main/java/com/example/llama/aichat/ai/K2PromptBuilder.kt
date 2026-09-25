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
               "You are an on-device personal notification assistant. Analyze the incoming notification against the User's Active Rules.\n\n" +
               "Active User Rules:\n" +
               "$rulesFormatted\n\n" +
               "Evaluation Guidelines:\n" +
               "1. \"important\": true if the content or sender genuinely matches a user rule (e.g. specific person, critical topic, job updates, OTP/banking, urgent messages).\n" +
               "2. \"important\": false if a negative rule applies (e.g. movie messages from someone are not important), or for marketing promotions, spam, automated digests, social media likes/reactions, and unrelated app content.\n" +
               "3. If rules intersect (e.g. \"movies from Arjun not important\" vs \"job messages important\"), determine the actual subject of the message.\n" +
               "4. \"alert\": true ONLY when the user must be alerted immediately with sound/vibration.\n\n" +
               "Output ONLY a single JSON object in the exact format:\n" +
               "{\"important\": true/false, \"alert\": true/false, \"reason\": \"short concise explanation\", \"category\": \"messages/job/financial/alert/other\"}\n" +
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
