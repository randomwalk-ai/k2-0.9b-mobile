package com.example.llama.aichat.ai

data class NotificationAnalysis(
    val important: Boolean,
    val alert: Boolean,
    val reason: String,
    val summary: String = "",
    val category: String = "other"
)

object K2ResponseParser {

    fun parse(rawResponse: String?, defaultSummary: String = "Notification received"): NotificationAnalysis {
        if (rawResponse.isNullOrBlank()) return fallback(defaultSummary)

        val text = rawResponse.trim()

        // Extract values via robust regex
        val importantRegex = Regex("\"important\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val alertRegex = Regex("\"alert\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val reasonRegex = Regex("\"reason\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        val summaryRegex = Regex("\"summary\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        val categoryRegex = Regex("\"category\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)

        val importantMatch = importantRegex.find(text)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        val alertMatch = alertRegex.find(text)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        val reasonMatch = reasonRegex.find(text)?.groupValues?.get(1)
        val summaryMatch = summaryRegex.find(text)?.groupValues?.get(1)
        val categoryMatch = categoryRegex.find(text)?.groupValues?.get(1)

        val rawImportant = importantMatch ?: when {
            text.startsWith("true", ignoreCase = true) -> true
            text.startsWith("false", ignoreCase = true) -> false
            else -> false
        }

        val reason = reasonMatch?.ifBlank { null } ?: if (rawImportant) "Matches user rules" else "General notification"
        val reasonLower = reason.lowercase()
        val isExplicitlyStatedNotImportant = reasonLower.contains("not important") ||
                reasonLower.contains("never important") ||
                reasonLower.contains("muted") ||
                reasonLower.contains("is ignored") ||
                reasonLower.contains("should be ignored")

        val isImportant = if (isExplicitlyStatedNotImportant) false else rawImportant
        val isAlert = if (isImportant) (alertMatch ?: false) else false
        val summary = summaryMatch?.ifBlank { null } ?: defaultSummary
        val category = categoryMatch?.ifBlank { null } ?: if (isImportant) "important" else "other"

        return NotificationAnalysis(
            important = isImportant,
            alert = isAlert,
            reason = reason,
            summary = summary,
            category = category
        )
    }

    private fun fallback(summary: String) = NotificationAnalysis(
        important = false,
        alert = false,
        reason = "General notification; no matching rule",
        summary = summary,
        category = "other"
    )

    fun parseConditionResult(rawResponse: String?): SemanticConditionResult {
        if (rawResponse.isNullOrBlank()) {
            return SemanticConditionResult(conditionMatched = false, reason = "Model unavailable", category = "other")
        }
        val text = rawResponse.trim()
        val conditionRegex = Regex("\"condition_matched\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val importantRegex = Regex("\"important\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val reasonRegex = Regex("\"reason\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        val categoryRegex = Regex("\"category\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)

        val condMatch = conditionRegex.find(text)?.groupValues?.get(1)?.toBooleanStrictOrNull()
            ?: importantRegex.find(text)?.groupValues?.get(1)?.toBooleanStrictOrNull()
            ?: when {
                text.startsWith("true", ignoreCase = true) -> true
                text.startsWith("false", ignoreCase = true) -> false
                else -> false
            }
        val reason = reasonRegex.find(text)?.groupValues?.get(1)?.ifBlank { null }
            ?: if (condMatch) "Condition matched" else "Condition not matched"
        val category = categoryRegex.find(text)?.groupValues?.get(1)?.ifBlank { "other" } ?: "other"

        return SemanticConditionResult(
            conditionMatched = condMatch,
            reason = reason,
            category = category
        )
    }
}

data class SemanticConditionResult(
    val conditionMatched: Boolean,
    val reason: String,
    val category: String = "other"
)
