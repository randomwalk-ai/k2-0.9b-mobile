package com.example.llama.aichat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object JsonListHelper {
    fun toJson(list: Collection<String>): String {
        return "[" + list.joinToString(",") { "\"" + it.replace("\"", "\\\"") + "\"" } + "]"
    }

    fun fromJson(json: String?): List<String> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val trimmed = json.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isEmpty()) return emptyList()
        return inner.split(",").mapNotNull { token ->
            val clean = token.trim().removeSurrounding("\"").removeSurrounding("'").trim()
            if (clean.isNotEmpty()) clean else null
        }
    }
}

@Entity(tableName = "notification_rules")
data class NotificationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val enabled: Boolean = true,
    val targetPerson: String? = null,
    val action: String = "ALERT", // "ALERT" or "MUTE"
    val positiveTopicsJson: String = "[]",
    val excludedTopicsJson: String = "[]",
    val targetAppsJson: String = "[]",
    val ruleIntent: String = "SIMPLE_CONTACT",
    val semanticDepth: String = "AOT_FAST",
    val semanticCondition: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getPositiveTopics(): List<String> = JsonListHelper.fromJson(positiveTopicsJson)

    fun getExcludedTopics(): List<String> = JsonListHelper.fromJson(excludedTopicsJson)

    fun getTargetApps(): List<String> = JsonListHelper.fromJson(targetAppsJson)
}
