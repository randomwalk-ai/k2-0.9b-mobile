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
        val jsonCandidate = if (text.startsWith("{")) text else if (text.isNotEmpty()) "{$text" else ""

        // Extract values via robust regex supporting quoted/unquoted and yes/no
        val importantRegex = Regex("\"important\"\\s*:\\s*\"?(true|false|yes|no)\"?", RegexOption.IGNORE_CASE)
        val alertRegex = Regex("\"alert\"\\s*:\\s*\"?(true|false|yes|no)\"?", RegexOption.IGNORE_CASE)
        val reasonRegex = Regex("\"reason\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        val summaryRegex = Regex("\"summary\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        val categoryRegex = Regex("\"category\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)

        val importantStr = importantRegex.find(jsonCandidate)?.groupValues?.get(1)
        val alertStr = alertRegex.find(jsonCandidate)?.groupValues?.get(1)
        val reasonMatch = reasonRegex.find(jsonCandidate)?.groupValues?.get(1)
        val summaryMatch = summaryRegex.find(jsonCandidate)?.groupValues?.get(1)
        val categoryMatch = categoryRegex.find(jsonCandidate)?.groupValues?.get(1)

        val importantMatch = when (importantStr?.lowercase()) {
            "true", "yes" -> true
            "false", "no" -> false
            else -> null
        }

        val alertMatch = when (alertStr?.lowercase()) {
            "true", "yes" -> true
            "false", "no" -> false
            else -> null
        }

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
        val jsonCandidate = if (text.startsWith("{")) text else if (text.isNotEmpty()) "{$text" else ""

        val toneRegex = Regex("\"tone_matched\"\\s*:\\s*\"?(true|false|yes|no)\"?", RegexOption.IGNORE_CASE)
        val condRegex = Regex("\"condition_matched\"\\s*:\\s*\"?(true|false|yes|no)\"?", RegexOption.IGNORE_CASE)
        val isAngryRegex = Regex("\"is_angry\"\\s*:\\s*\"?(true|false|yes|no)\"?", RegexOption.IGNORE_CASE)
        val matchedRegex = Regex("\"matched\"\\s*:\\s*\"?(true|false|yes|no)\"?", RegexOption.IGNORE_CASE)
        val reasonRegex = Regex("\"reason\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)

        val rawMatchedStr = toneRegex.find(jsonCandidate)?.groupValues?.get(1)
            ?: condRegex.find(jsonCandidate)?.groupValues?.get(1)
            ?: isAngryRegex.find(jsonCandidate)?.groupValues?.get(1)
            ?: matchedRegex.find(jsonCandidate)?.groupValues?.get(1)

        val rawReason = reasonRegex.find(jsonCandidate)?.groupValues?.get(1)?.trim()

        val parsedFromKey = when (rawMatchedStr?.lowercase()) {
            "true", "yes" -> true
            "false", "no" -> false
            else -> null
        }

        // Semantic reason fallback if the JSON boolean key was missing, malformed, or inverted
        val matched = if (parsedFromKey != null) {
            parsedFromKey
        } else if (!rawReason.isNullOrBlank()) {
            val lowerReason = rawReason.lowercase()
            val isExplicitNegation = lowerReason.contains("does not match") ||
                    lowerReason.contains("not match") ||
                    lowerReason.contains("not expressing") ||
                    lowerReason.contains("not angry") ||
                    lowerReason.contains("calm") ||
                    lowerReason.contains("positive sentiment") ||
                    lowerReason.contains("neutral") ||
                    lowerReason.contains("no anger") ||
                    lowerReason.contains("friendly")
            val isExplicitPositive = lowerReason.contains("genuine emotional tone") ||
                    lowerReason.contains("target emotional tone detected") ||
                    lowerReason.contains("matches target tone") ||
                    lowerReason.contains("hostility") ||
                    lowerReason.contains("anger") ||
                    lowerReason.contains("furious") ||
                    lowerReason.contains("mad") ||
                    lowerReason.contains("rage") ||
                    lowerReason.contains("upset") ||
                    lowerReason.contains("expresses") ||
                    lowerReason.contains("indicating")
            if (isExplicitNegation) false else isExplicitPositive
        } else {
            when {
                text.startsWith("true", ignoreCase = true) -> true
                text.startsWith("false", ignoreCase = true) -> false
                else -> false
            }
        }

        val reason = rawReason?.ifBlank { null }
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
        val jsonCandidate = if (text.startsWith("{")) text else if (text.isNotEmpty()) "{$text" else ""

        val conditionRegex = Regex("\"condition_matched\"\\s*:\\s*\"?(true|false|yes|no)\"?", RegexOption.IGNORE_CASE)
        val importantRegex = Regex("\"important\"\\s*:\\s*\"?(true|false|yes|no)\"?", RegexOption.IGNORE_CASE)
        val reasonRegex = Regex("\"reason\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        val categoryRegex = Regex("\"category\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)

        val rawCondStr = conditionRegex.find(jsonCandidate)?.groupValues?.get(1)
            ?: importantRegex.find(jsonCandidate)?.groupValues?.get(1)

        val rawReason = reasonRegex.find(jsonCandidate)?.groupValues?.get(1)?.trim()

        val parsedFromKey = when (rawCondStr?.lowercase()) {
            "true", "yes" -> true
            "false", "no" -> false
            else -> null
        }

        val condMatch = if (parsedFromKey != null) {
            parsedFromKey
        } else if (!rawReason.isNullOrBlank()) {
            val lowerReason = rawReason.lowercase()
            val isExplicitNegation = lowerReason.contains("does not match") || lowerReason.contains("not match") || lowerReason.contains("not satisfy")
            val isExplicitPositive = lowerReason.contains("matched") || lowerReason.contains("satisfied") || lowerReason.contains("detected")
            if (isExplicitNegation) false else isExplicitPositive
        } else {
            when {
                text.startsWith("true", ignoreCase = true) -> true
                text.startsWith("false", ignoreCase = true) -> false
                else -> false
            }
        }

        val reason = rawReason?.ifBlank { null }
            ?: if (condMatch) "Condition matched" else "Condition not matched"
        val category = categoryRegex.find(jsonCandidate)?.groupValues?.get(1)?.ifBlank { "other" } ?: "other"

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
            val rawActionExtracted = if (jsonCandidate.isNotEmpty()) actionRegex.find(jsonCandidate)?.groupValues?.get(1)?.uppercase()?.trim() else null

            // 5. Extract semantic_condition
            val conditionRegex = Regex("\"semantic_condition\"\\s*:\\s*(\"([^\"]*)\"|null)", RegexOption.IGNORE_CASE)
            val conditionMatch = if (jsonCandidate.isNotEmpty()) conditionRegex.find(jsonCandidate) else null
            val rawCondition = conditionMatch?.groupValues?.get(2)?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

            // 6. Extract Arrays: target_apps, positive_topics, excluded_topics
            val targetAppsList = if (jsonCandidate.isNotEmpty()) extractJsonStringArray(jsonCandidate, "target_apps") else emptyList()
            val positiveTopicsList = if (jsonCandidate.isNotEmpty()) extractJsonStringArray(jsonCandidate, "positive_topics") else emptyList()
            val excludedTopicsList = if (jsonCandidate.isNotEmpty()) extractJsonStringArray(jsonCandidate, "excluded_topics") else emptyList()

            val noiseTokens = setOf(
                "msg", "msgs", "message", "messages", "notification", "notifications",
                "text", "texts", "send", "sends", "sent", "sending", "talking", "messaged",
                "someone", "anyone", "any", "one", "everything", "something",
                "important", "alert", "alerts", "mute", "muted", "related", "about",
                "calls", "calling", "call", "me", "him", "her", "them", "us"
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

            // Target Person with dynamic NLP fallback
            val cleanPerson = rawPerson?.lowercase()?.trim() ?: run {
                val nonPersonWords = setOf(
                    "teams", "slack", "whatsapp", "swiggy", "uber", "instagram", "phone", "email", "gmail",
                    "any", "anyone", "someone", "one", "all", "everything", "nobody", "noone",
                    "if", "when", "whenever", "wherever", "whatever", "every", "a", "an", "the",
                    "calls", "call", "calling", "msg", "msgs", "message", "messages", "text", "texts",
                    "me", "my", "mine", "us", "our", "him", "her", "them", "it", "this", "that"
                )

                val personPatterns = listOf(
                    Regex("(?:from|by)\\s+([a-zA-Z0-9_]+)", RegexOption.IGNORE_CASE),
                    Regex("(?:if|when|whenever)\\s+([a-zA-Z0-9_]+)\\s+(?:calls?|texts?|messages?|msgs?|sends?|pings?|rings?)", RegexOption.IGNORE_CASE),
                    Regex("^([a-zA-Z0-9_]+)\\s+(?:calls?|texts?|messages?|msgs?|sends?|pings?|rings?)", RegexOption.IGNORE_CASE),
                    Regex("(?:when|if|whenever)\\s+([a-zA-Z0-9_]+)\\s+(?:is|his|feels|sounds)", RegexOption.IGNORE_CASE)
                )

                personPatterns.firstNotNullOfOrNull { pattern ->
                    val match = pattern.find(rawRuleText)
                    val candidate = match?.groupValues?.get(1)?.lowercase()?.trim()
                    if (candidate != null && candidate.length >= 2 && candidate !in nonPersonWords && candidate !in targetApps) {
                        candidate
                    } else null
                }
            }

            // Topics from K2 with dynamic NLP fallback
            val cleanPositiveTopics = (positiveTopicsList
                .map { it.lowercase().trim() }
                .filter { it.length >= 2 && it != cleanPerson && it !in targetApps && it !in noiseTokens }
                .toSet()).toMutableSet()

            if (cleanPositiveTopics.isEmpty()) {
                val matchRelated = Regex("(?:related to|about|for|regarding|discussing|playing)(?:\\s+(?:playing|having|doing|getting))?\\s+([a-zA-Z0-9_]+)", RegexOption.IGNORE_CASE).find(rawRuleText)
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
                val matchExcept = Regex("(?:except|unless|not|without|sends?)\\s+([a-zA-Z0-9_]+)", RegexOption.IGNORE_CASE).find(rawRuleText)
                val candidateExcluded = matchExcept?.groupValues?.get(1)?.lowercase()?.trim()
                if (candidateExcluded != null && candidateExcluded.length >= 2 && candidateExcluded !in noiseTokens && candidateExcluded != cleanPerson && candidateExcluded !in targetApps && candidateExcluded !in setOf("he", "she", "they", "his", "her", "their", "important", "alert")) {
                    cleanExcludedTopics.add(candidateExcluded)
                }
                if (lower.contains("reels") || lower.contains("reel")) cleanExcludedTopics.addAll(listOf("reels", "reel", "video"))
                if (lower.contains("memes") || lower.contains("meme")) cleanExcludedTopics.addAll(listOf("memes", "meme", "jokes"))
                if (lower.contains("spam") || lower.contains("promo")) cleanExcludedTopics.addAll(listOf("spam", "promo", "offers", "deals"))
            }

            val lowerText = rawRuleText.lowercase()
            val isInferredEmotion = rawEngine == "K2_DEEP" || rawRuleType == "CONDITIONAL_EMOTION" ||
                    Regex("\\b(angry|furious|mad|rage|upset|hostile)\\b", RegexOption.IGNORE_CASE).containsMatchIn(rawRuleText)
            val finalSemanticDepth = if (isInferredEmotion) "K2_DEEP" else "AOT_FAST"
            val finalSemanticCondition = rawCondition ?: if (isInferredEmotion) "sender is angry, mad, or furious" else null

            val hasPositiveClause = (lowerText.contains("is important") || lowerText.contains("it is important") || lowerText.contains("are important") || lowerText.contains("alert")) &&
                    !lowerText.startsWith("not important") && !lowerText.startsWith("never important")
            val isExplicitNegative = lowerText.startsWith("ignore") || lowerText.startsWith("block") || lowerText.startsWith("mute") ||
                    lowerText.contains("is not important") || lowerText.contains("never important") || lowerText.contains("not important")

            val finalAction = when {
                rawActionExtracted != null -> rawActionExtracted
                isInferredEmotion && isExplicitNegative -> "MUTE"
                cleanPerson != null && cleanExcludedTopics.isNotEmpty() && hasPositiveClause -> "ALERT"
                isExplicitNegative && !hasPositiveClause -> "MUTE"
                else -> "ALERT"
            }

            val finalIntent = when {
                isInferredEmotion -> RuleIntent.CONDITIONAL_EMOTION
                cleanPerson != null && cleanPositiveTopics.isEmpty() && cleanExcludedTopics.isEmpty() && finalAction == "MUTE" -> RuleIntent.SIMPLE_BLOCK
                cleanPerson != null && cleanPositiveTopics.isEmpty() && cleanExcludedTopics.isEmpty() -> RuleIntent.SIMPLE_CONTACT
                cleanPerson != null && (cleanPositiveTopics.isNotEmpty() || cleanExcludedTopics.isNotEmpty()) -> RuleIntent.CONDITIONAL_CONTACT
                rawRuleType == "APP_FILTER" || (targetApps.isNotEmpty() && cleanPerson == null && cleanPositiveTopics.isEmpty() && cleanExcludedTopics.isEmpty()) -> RuleIntent.APP_FILTER
                cleanPerson != null -> RuleIntent.SIMPLE_CONTACT
                else -> RuleIntent.TOPIC_FILTER
            }

            ParsedRule(
                rawText = rawRuleText,
                intent = finalIntent,
                targetPerson = cleanPerson,
                action = finalAction,
                targetApps = targetApps,
                positiveTopics = cleanPositiveTopics,
                excludedTopics = cleanExcludedTopics,
                isNegative = finalAction == "MUTE",
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
