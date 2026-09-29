package com.example.llama.aichat.ai

import com.example.llama.aichat.data.JsonListHelper
import com.example.llama.aichat.data.NotificationRule

enum class RuleIntent {
    SIMPLE_CONTACT,      // Contact name match
    SIMPLE_BLOCK,        // Direct block / mute
    CONDITIONAL_CONTACT, // Contact rule with topic/negation
    CONDITIONAL_EMOTION, // Rule requiring emotion / tone evaluation
    TOPIC_FILTER,        // Domain / Topic rule
    APP_FILTER           // App-specific rule
}

data class ParsedRule(
    val rawText: String,
    val intent: RuleIntent,
    val targetPerson: String? = null,
    val action: String = "ALERT", // "ALERT" or "MUTE"
    val targetApps: Set<String> = emptySet(),
    val positiveTopics: Set<String> = emptySet(),
    val excludedTopics: Set<String> = emptySet(),
    val isNegative: Boolean = false,
    val semanticDepth: String = "AOT_FAST",
    val semanticCondition: String? = null
) {
    fun toNotificationRule(id: Long = 0L, enabled: Boolean = true): NotificationRule {
        return NotificationRule(
            id = id,
            text = rawText,
            enabled = enabled,
            targetPerson = targetPerson,
            action = action,
            positiveTopicsJson = JsonListHelper.toJson(positiveTopics),
            excludedTopicsJson = JsonListHelper.toJson(excludedTopics),
            targetAppsJson = JsonListHelper.toJson(targetApps),
            ruleIntent = intent.name,
            semanticDepth = semanticDepth,
            semanticCondition = semanticCondition,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
