package com.example.llama.aichat.notification

import android.content.Context
import android.util.Log
import com.example.llama.aichat.ai.K2InferenceManager
import com.example.llama.aichat.ai.K2PromptBuilder
import com.example.llama.aichat.ai.K2ResponseParser
import com.example.llama.aichat.data.NotificationRecord
import com.example.llama.aichat.data.NotificationRepository
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

    private suspend fun processSingle(data: NotificationData) {
        val startTime = System.currentTimeMillis()
        try {
            Log.d("NotificationProcessor", "Processing notification from ${data.packageName}: ${data.title}")

            // 1. Deduplication: Skip if exact identical notification already exists within 60 seconds (OS re-post / audio progress update)
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

            // 2. Fast System Noise Filter (0ms compute)
            val isSystemMeter = (data.packageName == "com.android.systemui" || data.packageName == "android") &&
                    (titleLower.contains("charging") || textLower.contains("charging") ||
                     titleLower.contains("battery") || textLower.contains("battery") ||
                     titleLower.contains("usb") || textLower.contains("usb"))
            val isScreenshot = data.packageName.contains("screencapture") || data.packageName.contains("screenshot") ||
                    titleLower.contains("screenshot") || textLower.contains("screenshot")
            val isSyncPlaceholder = textLower.contains("checking for new messages") || textLower.contains("searching for new messages")

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
                    val ruleTexts = rules.map { it.text.trim() }

                    // Invoke on-device K2 Horizon 0.9B LLM
                    val prompt = K2PromptBuilder.buildPrompt(
                        rules = ruleTexts,
                        appName = data.appName,
                        packageName = data.packageName,
                        title = data.title,
                        text = data.text,
                        sender = data.sender
                    )
                    Log.d("NotificationProcessor", "Invoking K2 AI inference with prompt:\n$prompt")
                    val aiResponse = inferenceManager.analyze(prompt)
                    Log.d("NotificationProcessor", "K2 AI Raw Response:\n$aiResponse")

                    val analysis = K2ResponseParser.parse(aiResponse, defaultCleanSummary)
                    isImportant = analysis.important
                    shouldAlert = analysis.alert
                    decisionReason = analysis.reason
                    finalSummary = analysis.summary.ifBlank { defaultCleanSummary }
                    aiCategory = analysis.category

                    // Respect explicit 'do not alert' / 'dont alert' rules
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
