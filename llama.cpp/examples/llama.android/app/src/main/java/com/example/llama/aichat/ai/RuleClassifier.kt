package com.example.llama.aichat.ai

enum class RuleIntent {
    SIMPLE_CONTACT,      // Pure contact name match (Fast-Path <1ms)
    SIMPLE_BLOCK,        // Pure contact block (Fast-Path <1ms)
    SEMANTIC_CONDITIONAL // Complex / Topic / Conditional rule (K2 Horizon 0.9B AI)
}

data class ParsedRule(
    val rawText: String,
    val intent: RuleIntent,
    val targetPerson: String? = null,
    val isNegative: Boolean = false
)

object RuleClassifier {

    private val STOP_WORDS = setOf(
        "whatever", "messages", "message", "from", "any", "all", "every", "is", "are",
        "important", "alert", "priority", "urgent", "on", "in", "notification",
        "notifications", "to", "the", "and", "with", "for", "msg", "msgs",
        "sent", "by", "its", "it's", "it", "someone", "anyone", "everyone",
        "please", "be", "never", "not", "dont", "do", "ignore", "block", "blocked",
        "calls", "call", "text", "texts"
    )

    private val CONDITIONAL_INDICATORS = setOf(
        "about", "related", "relating", "regarding", "if", "only", "when",
        "emergency", "asking", "discussing", "contains", "mentioning",
        "job", "jobs", "movie", "movies", "money", "flight", "crypto", "interview",
        "debit", "credit", "otp", "order", "delivery", "arrival", "shortlist",
        "discount", "deal", "offer", "reel", "post"
    )

    fun classify(ruleText: String): ParsedRule {
        val lower = ruleText.lowercase().trim()
        val isNegative = lower.contains("never") || lower.contains("not important") ||
                lower.contains("never alert") || lower.contains("do not alert") ||
                lower.contains("dont alert") || lower.contains("no alert") ||
                lower.contains("ignore") || lower.contains("block")

        val hasCondition = CONDITIONAL_INDICATORS.any { lower.contains(it) }
        val isPersonRule = lower.contains("from ") || lower.contains("msg from") || lower.contains("message from")

        val targetName = if (isPersonRule) extractTargetPerson(lower) else null

        return when {
            // Case 1: Person rule with a specific topic condition (e.g. "Arjun related to movies...") -> SEMANTIC
            isPersonRule && hasCondition -> {
                ParsedRule(
                    rawText = ruleText,
                    intent = RuleIntent.SEMANTIC_CONDITIONAL,
                    targetPerson = targetName,
                    isNegative = isNegative
                )
            }
            // Case 2: General topic / intent rule -> SEMANTIC
            !isPersonRule && hasCondition -> {
                ParsedRule(
                    rawText = ruleText,
                    intent = RuleIntent.SEMANTIC_CONDITIONAL,
                    targetPerson = null,
                    isNegative = isNegative
                )
            }
            // Case 3: Pure negative block with no topic condition -> SIMPLE BLOCK
            isPersonRule && isNegative && !targetName.isNullOrBlank() -> {
                ParsedRule(
                    rawText = ruleText,
                    intent = RuleIntent.SIMPLE_BLOCK,
                    targetPerson = targetName,
                    isNegative = true
                )
            }
            // Case 4: Pure positive contact rule -> SIMPLE CONTACT
            isPersonRule && !targetName.isNullOrBlank() -> {
                ParsedRule(
                    rawText = ruleText,
                    intent = RuleIntent.SIMPLE_CONTACT,
                    targetPerson = targetName,
                    isNegative = false
                )
            }
            // Default -> SEMANTIC (handled by K2 LLM)
            else -> {
                ParsedRule(
                    rawText = ruleText,
                    intent = RuleIntent.SEMANTIC_CONDITIONAL,
                    targetPerson = targetName,
                    isNegative = isNegative
                )
            }
        }
    }

    private fun extractTargetPerson(lowerText: String): String? {
        val afterFrom = if (lowerText.contains("from ")) {
            lowerText.substringAfter("from ")
        } else if (lowerText.contains("msg from")) {
            lowerText.substringAfter("msg from")
        } else {
            lowerText
        }
        val tokens = afterFrom.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 && it !in STOP_WORDS }
        return tokens.firstOrNull { it !in CONDITIONAL_INDICATORS }
    }
}
