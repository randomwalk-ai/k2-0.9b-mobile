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
               "User Rules:\n" +
               "$rulesFormatted\n\n" +
               "Evaluation Rules:\n" +
               "1. POSITIVE MATCH: If the notification satisfies the sender, keyword, or topic conditions of an active user rule, set \"important\": true, \"alert\": true.\n" +
               "2. EXCEPTIONS & NEGATIONS: If an active rule specifies an exclusion or exception for a sender or topic, and the notification content falls under that exclusion, set \"important\": false, \"alert\": false.\n" +
               "3. DEFAULT TO UNIMPORTANT: If the notification does not match any active user rule, set \"important\": false, \"alert\": false, \"reason\": \"No matching rule\".\n\n" +
               "Output ONLY a single JSON object in the exact format:\n" +
               "{\"important\": true/false, \"alert\": true/false, \"reason\": \"brief explanation\", \"category\": \"messages/job/financial/other\"}\n" +
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
