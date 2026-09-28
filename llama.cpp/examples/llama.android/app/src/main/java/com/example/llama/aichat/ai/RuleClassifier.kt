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
    val dynamicAnchors: Set<String> = emptySet(),
    val excludedAnchors: Set<String> = emptySet(),
    val isNegative: Boolean = false
)

object RuleClassifier {

    private val FUNCTIONAL_STOP_WORDS = setOf(
        "whatever", "messages", "message", "from", "any", "all", "every", "is", "are",
        "important", "alert", "priority", "urgent", "on", "in", "notification",
        "notifications", "to", "the", "and", "with", "for", "msg", "msgs",
        "sent", "by", "its", "it's", "it", "someone", "anyone", "everyone",
        "please", "be", "never", "not", "dont", "do", "ignore", "block", "blocked",
        "calls", "call", "text", "texts", "about", "related", "relating", "regarding",
        "if", "only", "when", "then", "which", "that", "this", "there", "their",
        "should", "would", "could", "must", "of", "an", "a", "or", "as"
    )

    fun classify(ruleText: String): ParsedRule {
        val lower = ruleText.lowercase().trim()
        val isExplicitNegative = lower.contains("never") || lower.contains("not important") ||
                lower.contains("never alert") || lower.contains("do not alert") ||
                lower.contains("dont alert") || lower.contains("no alert") ||
                lower.contains("ignore") || lower.contains("block")

        val explicitTarget = extractTargetPerson(lower)

        val targetName = if (explicitTarget != null) {
            explicitTarget
        } else {
            // Check if user entered strictly a contact name (e.g. "Madhu", "Arjun_Vasireddy", "ignore Madhu", "block Bob")
            val nonStopTokens = lower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 && it !in FUNCTIONAL_STOP_WORDS }
            val totalTokens = lower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 }
            if (nonStopTokens.size in 1..2 && totalTokens.size <= 3) {
                nonStopTokens.joinToString(" ")
            } else {
                null
            }
        }

        val isPersonRule = targetName != null
        val excludedAnchors = extractExcludedAnchors(lower)

        // Dynamically extract positive topic anchors from user's rule text (excluding target and exclusions)
        val allTokens = lower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 3 }
        val targetTokens = targetName?.split(Regex("[^a-zA-Z0-9_]+"))?.toSet() ?: emptySet()
        val topicAnchors = allTokens.filter { token ->
            token !in FUNCTIONAL_STOP_WORDS && token !in targetTokens && token !in excludedAnchors
        }.toSet()

        val hasCondition = topicAnchors.isNotEmpty() || excludedAnchors.isNotEmpty()

        return when {
            // Case 1: Person rule with dynamic topic condition or exception (e.g. "Arjun not related to movies...") -> SEMANTIC
            isPersonRule && hasCondition -> {
                ParsedRule(
                    rawText = ruleText,
                    intent = RuleIntent.SEMANTIC_CONDITIONAL,
                    targetPerson = targetName,
                    dynamicAnchors = topicAnchors,
                    excludedAnchors = excludedAnchors,
                    isNegative = isExplicitNegative
                )
            }
            // Case 2: General topic rule (e.g. "client invoices", "jobs", "server downtime") -> SEMANTIC
            !isPersonRule && hasCondition -> {
                ParsedRule(
                    rawText = ruleText,
                    intent = RuleIntent.SEMANTIC_CONDITIONAL,
                    targetPerson = null,
                    dynamicAnchors = topicAnchors,
                    excludedAnchors = excludedAnchors,
                    isNegative = isExplicitNegative
                )
            }
            // Case 3: Pure negative block with no topic condition -> SIMPLE BLOCK
            isPersonRule && isExplicitNegative && !targetName.isNullOrBlank() -> {
                ParsedRule(
                    rawText = ruleText,
                    intent = RuleIntent.SIMPLE_BLOCK,
                    targetPerson = targetName,
                    dynamicAnchors = emptySet(),
                    excludedAnchors = emptySet(),
                    isNegative = true
                )
            }
            // Case 4: Pure positive contact rule -> SIMPLE CONTACT
            isPersonRule && !targetName.isNullOrBlank() -> {
                ParsedRule(
                    rawText = ruleText,
                    intent = RuleIntent.SIMPLE_CONTACT,
                    targetPerson = targetName,
                    dynamicAnchors = emptySet(),
                    excludedAnchors = emptySet(),
                    isNegative = false
                )
            }
            // Default -> SEMANTIC (handled by K2 LLM)
            else -> {
                ParsedRule(
                    rawText = ruleText,
                    intent = RuleIntent.SEMANTIC_CONDITIONAL,
                    targetPerson = targetName,
                    dynamicAnchors = topicAnchors,
                    excludedAnchors = excludedAnchors,
                    isNegative = isExplicitNegative
                )
            }
        }
    }

    private fun extractTargetPerson(lowerText: String): String? {
        val afterFrom = when {
            lowerText.contains("from ") -> lowerText.substringAfter("from ")
            lowerText.contains("msg from ") -> lowerText.substringAfter("msg from ")
            lowerText.contains("message from ") -> lowerText.substringAfter("message from ")
            lowerText.contains("sent by ") -> lowerText.substringAfter("sent by ")
            lowerText.contains("by ") -> lowerText.substringAfter("by ")
            lowerText.contains("calls from ") -> lowerText.substringAfter("calls from ")
            lowerText.contains("call from ") -> lowerText.substringAfter("call from ")
            else -> null
        } ?: return null

        val tokens = afterFrom.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 && it !in FUNCTIONAL_STOP_WORDS }
        return tokens.firstOrNull()
    }

    private fun extractExcludedAnchors(lowerText: String): Set<String> {
        val pattern = when {
            lowerText.contains("not related to ") -> lowerText.substringAfter("not related to ")
            lowerText.contains("not about ") -> lowerText.substringAfter("not about ")
            lowerText.contains("except about ") -> lowerText.substringAfter("except about ")
            lowerText.contains("except ") -> lowerText.substringAfter("except ")
            lowerText.contains("excluding ") -> lowerText.substringAfter("excluding ")
            lowerText.contains("unless about ") -> lowerText.substringAfter("unless about ")
            lowerText.contains("unless ") -> lowerText.substringAfter("unless ")
            lowerText.contains("other than ") -> lowerText.substringAfter("other than ")
            else -> return emptySet()
        }

        val cleanClause = pattern.substringBefore(",").substringBefore(".").substringBefore(" is ").trim()
        val tokens = cleanClause.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 3 && it !in FUNCTIONAL_STOP_WORDS }
        return tokens.toSet()
    }
}
