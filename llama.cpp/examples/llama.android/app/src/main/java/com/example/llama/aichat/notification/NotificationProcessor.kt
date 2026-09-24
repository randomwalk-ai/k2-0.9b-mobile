package com.example.llama.aichat.notification

import android.content.Context
import android.util.Log
import com.example.llama.aichat.ai.K2InferenceManager
import com.example.llama.aichat.data.NotificationRecord
import com.example.llama.aichat.data.NotificationRepository
import com.example.llama.aichat.data.NotificationRule
import com.example.llama.aichat.data.NotificationRuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationProcessor(
    private val context: Context,
    private val inferenceManager: K2InferenceManager,
    private val notificationRepository: NotificationRepository,
    private val ruleRepository: NotificationRuleRepository,
    private val summaryManager: NotificationSummaryManager,
    private val alertManager: AIAlertManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val queue = Channel<NotificationData>(Channel.UNLIMITED)

    init {
        // Sequential FIFO queue worker: processes one notification at a time in order
        scope.launch {
            for (data in queue) {
                processSingle(data)
            }
        }

        // Watch for model ready state to process any backlog
        scope.launch {
            inferenceManager.state.collectLatest { state ->
                if (state == K2InferenceManager.State.READY) {
                    reprocessUnprocessed()
                }
            }
        }
    }

    private suspend fun reprocessUnprocessed() {
        val unprocessed = notificationRepository.getUnprocessedNotifications()
        if (unprocessed.isEmpty()) return

        Log.i("NotificationProcessor", "Reprocessing ${unprocessed.size} unprocessed notifications")
        for (record in unprocessed) {
            val data = NotificationData(
                packageName = record.packageName,
                appName = record.appName,
                title = record.title,
                text = record.text,
                subText = null,
                sender = record.sender,
                category = record.category,
                notificationKey = record.notificationKey,
                timestamp = record.timestamp
            )
            queue.send(data)
        }
    }

    fun process(data: NotificationData) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true)) {
            Log.d("NotificationProcessor", "Processing disabled by user switch")
            return
        }
        queue.trySend(data)
    }

    private fun matchesPersonName(ruleTarget: String, senderName: String): Boolean {
        val target = ruleTarget.lowercase().trim()
        val targetClean = target.replace(" ", "")
        val sender = senderName.lowercase().trim()
        val senderClean = sender.replace(" ", "")
        val senderWords = sender.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 }

        if (target.isEmpty() || sender.isEmpty()) return false

        // Exact or whitespace-insensitive match
        if (senderClean == targetClean) return true

        // If target is specific (e.g. "krishnavardhan"), sender must match the full specific name
        if (targetClean.length >= 7) {
            return senderClean.contains(targetClean) || senderClean.startsWith(targetClean)
        }

        // If target is root (e.g. "krishna" or "madhu"), sender words must start with or equal the target
        return senderWords.any { it == target || it.startsWith(target) } || senderClean.startsWith(targetClean)
    }

    private suspend fun processSingle(data: NotificationData) {
        val startTime = System.currentTimeMillis()
        try {
            Log.d("NotificationProcessor", "Processing notification from ${data.packageName}: ${data.title}")

            // 1. Deduplication: Skip if exact identical notification already exists within 60 seconds
            val recentDuplicate = notificationRepository.findRecentDuplicate(
                packageName = data.packageName,
                key = data.notificationKey,
                title = data.title,
                text = data.text,
                sender = data.sender,
                sinceTimestamp = data.timestamp - 60_000L
            )
            if (recentDuplicate != null) {
                Log.d("NotificationProcessor", "Skipping duplicate notification event from ${data.packageName} (matched ID ${recentDuplicate.id})")
                return
            }

            val defaultCleanSummary = when {
                !data.sender.isNullOrBlank() && !data.text.isNullOrBlank() && data.sender != data.appName -> "${data.sender}: ${data.text}"
                !data.text.isNullOrBlank() -> "${data.appName}: ${data.text}"
                !data.title.isNullOrBlank() -> data.title
                else -> "${data.appName} notification"
            }

            val senderLower = data.sender?.lowercase()?.trim() ?: ""
            val titleLower = data.title?.lowercase()?.trim() ?: ""
            val textLower = data.text?.lowercase()?.trim() ?: ""
            val appLower = data.appName.lowercase().trim()
            val packageLower = data.packageName.lowercase().trim()
            val contentLower = "$senderLower $titleLower $textLower $appLower"

            // 2. Identify System & Ongoing Indicators
            val isMedia = data.category == "transport" ||
                    packageLower.contains("spotify") ||
                    packageLower.contains("music") ||
                    packageLower.contains("audio") ||
                    packageLower.contains("podcast")
            val isNavigation = data.category == "navigation" ||
                    packageLower.contains("maps") ||
                    packageLower.contains("waze")
            val isSystemMeter = (packageLower == "com.android.systemui" || packageLower == "android") &&
                    (titleLower.contains("charging") || textLower.contains("charging") ||
                     titleLower.contains("battery") || textLower.contains("battery") ||
                     titleLower.contains("usb") || textLower.contains("usb"))
            val isScreenshot = packageLower.contains("screencapture") || packageLower.contains("screenshot") ||
                    titleLower.contains("screenshot") || textLower.contains("screenshot")
            val isSyncPlaceholder = textLower.contains("checking for new messages") || textLower.contains("searching for new messages")

            // Social likes/reactions on shared posts (Not direct messages)
            val isSocialReaction = textLower.contains("liked your") || textLower.contains("liked a") ||
                    textLower.contains("liked _") || textLower.contains("liked ") ||
                    textLower.contains("reacted") || textLower.contains("started following") ||
                    textLower.contains("commented on") || textLower.contains("shared a reel") ||
                    titleLower.contains("liked your") || titleLower.contains("liked _")

            var isImportant = false
            var shouldAlert = false
            var decisionReason = "General notification; no matching rule"
            var finalSummary = defaultCleanSummary
            var aiCategory = "other"

            if (isSystemMeter || isScreenshot) {
                decisionReason = if (isScreenshot) "Screenshot captured" else "System status update"
                finalSummary = if (isScreenshot) "Screenshot saved" else defaultCleanSummary
                aiCategory = "system"
            } else if (isSyncPlaceholder) {
                decisionReason = "Conversation sync placeholder"
                aiCategory = "sync"
            } else {
                val rules = ruleRepository.getEnabledRules()
                if (rules.isEmpty()) {
                    decisionReason = "No active user rules"
                    aiCategory = "other"
                } else {
                    val stopWords = setOf(
                        "messages", "message", "from", "any", "all", "every", "is", "are",
                        "important", "alert", "priority", "urgent", "on", "in", "notification",
                        "notifications", "about", "to", "the", "and", "with", "for", "msg", "msgs",
                        "sent", "by", "if", "its", "it's", "it", "someone", "anyone", "everyone",
                        "related", "relating", "please", "be"
                    )

                    var matchedRule: NotificationRule? = null
                    var matchExplanation: String? = null

                    for (rule in rules) {
                        val ruleLower = rule.text.lowercase().trim()
                        val isPersonRule = ruleLower.contains("from ") || ruleLower.contains("msg from") ||
                                ruleLower.contains("message from") || ruleLower.contains("messages from")

                        if (isPersonRule) {
                            // Extract person target name from the rule
                            val ruleTokens = ruleLower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 && it !in stopWords }
                            val targetName = ruleTokens.firstOrNull() ?: ""

                            if (targetName.isNotEmpty()) {
                                val isDirectSenderMatch = !data.sender.isNullOrBlank() && matchesPersonName(targetName, data.sender)
                                val isTitleSenderMatch = !data.title.isNullOrBlank() && !isMedia && !isNavigation && matchesPersonName(targetName, data.title)

                                if (!isMedia && !isNavigation && !isSocialReaction && (isDirectSenderMatch || isTitleSenderMatch)) {
                                    matchedRule = rule
                                    matchExplanation = "Message from ${data.sender ?: targetName} matching rule: ${rule.text}"
                                    aiCategory = "messages"
                                    break
                                }
                            }
                        } else {
                            // Dynamic semantic rule: Extract meaningful keyword tokens from the rule
                            val ruleTokens = ruleLower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 3 && it !in stopWords }
                            if (ruleTokens.isNotEmpty()) {
                                val allTokensMatch = ruleTokens.all { token ->
                                    contentLower.split(Regex("[^a-zA-Z0-9_]+")).any { word -> word == token || word.startsWith(token) }
                                }
                                if (allTokensMatch) {
                                    matchedRule = rule
                                    matchExplanation = "Matches user rule: ${rule.text}"
                                    aiCategory = "important"
                                    break
                                }
                            }
                        }
                    }

                    if (matchedRule != null) {
                        isImportant = true
                        shouldAlert = true
                        decisionReason = matchExplanation ?: "Matches user rule: ${matchedRule.text}"
                    } else {
                        isImportant = false
                        shouldAlert = false
                        decisionReason = "General notification; no matching rule"
                        aiCategory = "other"
                    }

                    // Respect explicit 'do not alert' rules
                    val isExplicitDoNotAlert = rules.any { rule ->
                        val rLower = rule.text.lowercase()
                        (rLower.contains("do not alert") || rLower.contains("dont alert") || rLower.contains("no alert") || rLower.contains("silent")) &&
                        ((senderLower.isNotEmpty() && rLower.contains(senderLower)) || (titleLower.isNotEmpty() && rLower.contains(titleLower)) || (appLower.isNotEmpty() && rLower.contains(appLower)))
                    }
                    if (isExplicitDoNotAlert) {
                        shouldAlert = false
                    }
                }
            }

            if (finalSummary.isBlank() || finalSummary.startsWith("Summary of", ignoreCase = true)) {
                finalSummary = defaultCleanSummary
            }

            val latestByKey = notificationRepository.getLatestByKey(data.notificationKey)
            val recordId = if ((isSystemMeter || isScreenshot) && latestByKey != null) latestByKey.id else 0L

            val record = NotificationRecord(
                id = recordId,
                notificationKey = data.notificationKey,
                packageName = data.packageName,
                appName = data.appName,
                title = data.title,
                text = data.text,
                sender = data.sender,
                category = data.category,
                timestamp = data.timestamp,
                important = isImportant,
                alert = shouldAlert,
                summary = finalSummary,
                reason = decisionReason,
                aiCategory = aiCategory,
                processed = true
            )

            notificationRepository.insert(record)
            val totalLatency = System.currentTimeMillis() - startTime
            Log.d("NotificationProcessor", "Analysis Complete in ${totalLatency}ms: Important=$isImportant, Alert=$shouldAlert, Sender=${data.sender ?: data.appName}, Reason=$decisionReason")

            // Auto retention cleanup
            try {
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val retentionName = prefs.getString("retention_period", "HOURS_24")
                val retentionDuration = when (retentionName) {
                    "DAYS_2" -> 2 * 24 * 60 * 60 * 1000L
                    "DAYS_3" -> 3 * 24 * 60 * 60 * 1000L
                    "DAYS_4" -> 4 * 24 * 60 * 60 * 1000L
                    "DAYS_5" -> 5 * 24 * 60 * 60 * 1000L
                    "DAYS_6" -> 6 * 24 * 60 * 60 * 1000L
                    "DAYS_7" -> 7 * 24 * 60 * 60 * 1000L
                    else -> 24 * 60 * 60 * 1000L
                }
                val cutoff = System.currentTimeMillis() - retentionDuration
                notificationRepository.deleteOlderThan(cutoff)
            } catch (e: Exception) {
                Log.w("NotificationProcessor", "Retention cleanup error: ${e.message}")
            }

            if (record.important && record.alert) {
                Log.d("NotificationProcessor", "Firing AI alert chime for ${data.sender ?: data.appName}")
                alertManager.triggerAlert(record)
            }

            try {
                val importantOnes = notificationRepository.getImportantNotificationsSync()
                summaryManager.updateSummary(importantOnes)
            } catch (e: Exception) {
                // Ignore summary update errors
            }

        } catch (e: Exception) {
            Log.e("NotificationProcessor", "Error processing notification from ${data.packageName}", e)
        }
    }
}
