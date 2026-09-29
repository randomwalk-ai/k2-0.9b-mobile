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

    fun parseToneResult(rawResponse: String?): ToneAnalysisResult {
        if (rawResponse.isNullOrBlank()) {
            return ToneAnalysisResult(toneMatched = false, reason = "Model unavailable")
        }
        val text = rawResponse.trim()
        val toneRegex = Regex("\"tone_matched\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val condRegex = Regex("\"condition_matched\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val isAngryRegex = Regex("\"is_angry\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val matchedRegex = Regex("\"matched\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val reasonRegex = Regex("\"reason\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)

        val matched = toneRegex.find(text)?.groupValues?.get(1)?.toBooleanStrictOrNull()
            ?: condRegex.find(text)?.groupValues?.get(1)?.toBooleanStrictOrNull()
            ?: isAngryRegex.find(text)?.groupValues?.get(1)?.toBooleanStrictOrNull()
            ?: matchedRegex.find(text)?.groupValues?.get(1)?.toBooleanStrictOrNull()
            ?: when {
                text.startsWith("true", ignoreCase = true) -> true
                text.startsWith("false", ignoreCase = true) -> false
                else -> false
            }

        val reason = reasonRegex.find(text)?.groupValues?.get(1)?.ifBlank { null }
            ?: if (matched) "Target emotional tone detected" else "Target emotional tone not detected"

        return ToneAnalysisResult(
            toneMatched = matched,
            reason = reason
        )
    }

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

    fun parseCompiledRule(rawResponse: String?, rawRuleText: String): ParsedRule {
        val text = rawResponse?.trim() ?: ""
        val jsonCandidate = if (text.startsWith("{")) text else if (text.isNotEmpty()) "{$text" else ""

        return try {
            // 1. Extract rule_type
            val ruleTypeRegex = Regex("\"rule_type\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
            val rawRuleType = if (jsonCandidate.isNotEmpty()) ruleTypeRegex.find(jsonCandidate)?.groupValues?.get(1)?.uppercase()?.trim() else null

            // 2. Extract execution_engine
            val engineRegex = Regex("\"execution_engine\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
            val rawEngine = if (jsonCandidate.isNotEmpty()) engineRegex.find(jsonCandidate)?.groupValues?.get(1)?.uppercase()?.trim() else null

            // 3. Extract target_person
            val personRegex = Regex("\"target_person\"\\s*:\\s*(\"([^\"]*)\"|null)", RegexOption.IGNORE_CASE)
            val personMatch = if (jsonCandidate.isNotEmpty()) personRegex.find(jsonCandidate) else null
            val rawPerson = personMatch?.groupValues?.get(2)?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

            // 4. Extract action
            val actionRegex = Regex("\"action\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
            val rawAction = if (jsonCandidate.isNotEmpty()) actionRegex.find(jsonCandidate)?.groupValues?.get(1)?.uppercase()?.trim() ?: "ALERT" else {
                val lower = rawRuleText.lowercase()
                if ((lower.startsWith("ignore") || lower.startsWith("block") || lower.startsWith("mute") || lower.endsWith("not important") || lower.contains("is not important")) && !lower.contains("otherwise important")) "MUTE" else "ALERT"
            }

            // 5. Extract semantic_condition
            val conditionRegex = Regex("\"semantic_condition\"\\s*:\\s*(\"([^\"]*)\"|null)", RegexOption.IGNORE_CASE)
            val conditionMatch = if (jsonCandidate.isNotEmpty()) conditionRegex.find(jsonCandidate) else null
            val rawCondition = conditionMatch?.groupValues?.get(2)?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

            // 6. Extract Arrays: target_apps, positive_topics, excluded_topics
            val targetAppsList = if (jsonCandidate.isNotEmpty()) extractJsonStringArray(jsonCandidate, "target_apps") else emptyList()
            val positiveTopicsList = if (jsonCandidate.isNotEmpty()) extractJsonStringArray(jsonCandidate, "positive_topics") else emptyList()
            val excludedTopicsList = if (jsonCandidate.isNotEmpty()) extractJsonStringArray(jsonCandidate, "excluded_topics") else emptyList()

            // Determine intent
            val intent = when (rawRuleType) {
                "APP_FILTER" -> RuleIntent.APP_FILTER
                "TOPIC_FILTER" -> RuleIntent.TOPIC_FILTER
                "SIMPLE_CONTACT" -> RuleIntent.SIMPLE_CONTACT
                "SIMPLE_BLOCK" -> RuleIntent.SIMPLE_BLOCK
                "CONDITIONAL_CONTACT" -> RuleIntent.CONDITIONAL_CONTACT
                "CONDITIONAL_EMOTION" -> RuleIntent.CONDITIONAL_EMOTION
                else -> when {
                    rawEngine == "K2_DEEP" || rawCondition != null -> RuleIntent.CONDITIONAL_EMOTION
                    rawPerson != null && rawAction == "MUTE" && positiveTopicsList.isEmpty() && excludedTopicsList.isEmpty() -> RuleIntent.SIMPLE_BLOCK
                    rawPerson != null && (positiveTopicsList.isNotEmpty() || excludedTopicsList.isNotEmpty()) -> RuleIntent.CONDITIONAL_CONTACT
                    rawPerson != null -> RuleIntent.SIMPLE_CONTACT
                    targetAppsList.isNotEmpty() -> RuleIntent.APP_FILTER
                    else -> RuleIntent.TOPIC_FILTER
                }
            }

            val noiseTokens = setOf(
                "msg", "msgs", "message", "messages", "notification", "notifications",
                "text", "texts", "send", "sends", "sent", "sending", "talking", "messaged",
                "someone", "anyone", "any", "one", "everything", "something",
                "important", "alert", "alerts", "mute", "muted", "related", "about"
            )

            // Target Apps with fallback
            val targetApps = (targetAppsList.map { it.lowercase().trim() }.filter { it.isNotEmpty() }.toSet()).toMutableSet()
            if (targetApps.isEmpty()) {
                val lower = rawRuleText.lowercase()
                if (lower.contains("teams")) targetApps.add("teams")
                if (lower.contains("slack")) targetApps.add("slack")
                if (lower.contains("whatsapp")) targetApps.add("whatsapp")
                if (lower.contains("instagram")) targetApps.add("instagram")
                if (lower.contains("swiggy")) targetApps.add("swiggy")
                if (lower.contains("uber")) targetApps.add("uber")
            }

            // Target Person with fallback
            val cleanPerson = rawPerson?.lowercase()?.trim() ?: run {
                val fromMatch = Regex("from\\s+([a-zA-Z0-9_]+)", RegexOption.IGNORE_CASE).find(rawRuleText)
                val candidate = fromMatch?.groupValues?.get(1)?.lowercase()?.trim()
                val nonPersonWords = setOf("teams", "slack", "whatsapp", "swiggy", "uber", "instagram", "any", "anyone", "someone")
                if (candidate != null && candidate !in nonPersonWords && candidate !in targetApps) candidate else null
            }

            // Topics from K2
            val cleanPositiveTopics = (positiveTopicsList
                .map { it.lowercase().trim() }
                .filter { it.length >= 2 && it != cleanPerson && it !in targetApps && it !in noiseTokens }
                .toSet()).toMutableSet()

            if (cleanPositiveTopics.isEmpty()) {
                val matchRelated = Regex("(?:related to|about|playing)\\s+([a-zA-Z0-9_]+)", RegexOption.IGNORE_CASE).find(rawRuleText)
                val topic = matchRelated?.groupValues?.get(1)?.lowercase()?.trim()
                if (topic != null && topic.length >= 2 && topic !in noiseTokens && topic != cleanPerson && topic !in targetApps) {
                    cleanPositiveTopics.add(topic)
                }
            }

            val cleanExcludedTopics = (excludedTopicsList
                .map { it.lowercase().trim() }
                .filter { it.length >= 2 && it != cleanPerson && it !in targetApps && it !in noiseTokens }
                .toSet()).toMutableSet()

            if (cleanExcludedTopics.isEmpty()) {
                val lower = rawRuleText.lowercase()
                if (lower.contains("reels") || lower.contains("reel")) cleanExcludedTopics.addAll(listOf("reels", "reel", "video"))
                if (lower.contains("memes") || lower.contains("meme")) cleanExcludedTopics.addAll(listOf("memes", "meme", "jokes"))
            }

            val isInferredEmotion = rawEngine == "K2_DEEP" || intent == RuleIntent.CONDITIONAL_EMOTION ||
                    Regex("\\b(angry|furious|mad|rage|upset|hostile)\\b", RegexOption.IGNORE_CASE).containsMatchIn(rawRuleText)
            val finalSemanticDepth = if (isInferredEmotion) "K2_DEEP" else "AOT_FAST"
            val finalSemanticCondition = rawCondition ?: if (isInferredEmotion) "sender is angry, mad, or furious" else null

            val finalIntent = when {
                isInferredEmotion -> RuleIntent.CONDITIONAL_EMOTION
                rawRuleType == "SIMPLE_CONTACT" || (cleanPerson != null && cleanPositiveTopics.isEmpty() && cleanExcludedTopics.isEmpty() && rawAction != "MUTE") -> RuleIntent.SIMPLE_CONTACT
                rawRuleType == "SIMPLE_BLOCK" || (cleanPerson != null && cleanPositiveTopics.isEmpty() && cleanExcludedTopics.isEmpty() && rawAction == "MUTE") -> RuleIntent.SIMPLE_BLOCK
                rawRuleType == "CONDITIONAL_CONTACT" || (cleanPerson != null && (cleanPositiveTopics.isNotEmpty() || cleanExcludedTopics.isNotEmpty())) -> RuleIntent.CONDITIONAL_CONTACT
                rawRuleType == "APP_FILTER" || targetApps.isNotEmpty() -> RuleIntent.APP_FILTER
                cleanPerson != null -> RuleIntent.SIMPLE_CONTACT
                else -> RuleIntent.TOPIC_FILTER
            }

            ParsedRule(
                rawText = rawRuleText,
                intent = finalIntent,
                targetPerson = cleanPerson,
                action = rawAction,
                targetApps = targetApps,
                positiveTopics = cleanPositiveTopics,
                excludedTopics = cleanExcludedTopics,
                isNegative = rawAction == "MUTE",
                semanticDepth = finalSemanticDepth,
                semanticCondition = finalSemanticCondition
            )
        } catch (e: Exception) {
            val lower = rawRuleText.lowercase()
            val isNeg = (lower.startsWith("ignore") || lower.startsWith("block") || lower.startsWith("mute") || lower.endsWith("not important") || lower.contains("is not important")) && !lower.contains("otherwise important")
            ParsedRule(
                rawText = rawRuleText,
                intent = RuleIntent.TOPIC_FILTER,
                action = if (isNeg) "MUTE" else "ALERT",
                isNegative = isNeg
            )
        }
    }

    private fun extractJsonStringArray(json: String, arrayKey: String): List<String> {
        val arrayRegex = Regex("\"$arrayKey\"\\s*:\\s*\\[([^\\]]*)\\]", RegexOption.IGNORE_CASE)
        val match = arrayRegex.find(json)?.groupValues?.get(1)?.trim() ?: return emptyList()
        if (match.isEmpty()) return emptyList()
        return match.split(",").mapNotNull { token ->
            val clean = token.trim().removeSurrounding("\"").removeSurrounding("'").trim()
            if (clean.isNotEmpty()) clean else null
        }
    }
}

data class ToneAnalysisResult(
    val toneMatched: Boolean,
    val reason: String
)

data class SemanticConditionResult(
    val conditionMatched: Boolean,
    val reason: String,
    val category: String = "other"
)
