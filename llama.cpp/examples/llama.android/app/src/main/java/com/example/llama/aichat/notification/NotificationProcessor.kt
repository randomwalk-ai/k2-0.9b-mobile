package com.example.llama.aichat.notification

import android.content.Context
import android.util.Log
import com.example.llama.aichat.ai.K2InferenceManager
import com.example.llama.aichat.ai.K2PromptBuilder
import com.example.llama.aichat.ai.K2ResponseParser
import com.example.llama.aichat.ai.RuleClassifier
import com.example.llama.aichat.ai.RuleIntent
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

    private fun matchesPersonName(ruleTarget: String?, senderName: String?): Boolean {
        if (ruleTarget.isNullOrBlank() || senderName.isNullOrBlank()) return false
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

        // If target is root (e.g. "krishna", "madhu", "arjun"), sender words or clean sender must start with target
        return senderWords.any { it == target || it.startsWith(target) } || senderClean.startsWith(targetClean) || senderClean.contains(targetClean)
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

            // 2. Action & Noise Shielding Check (Never alert for ongoing user sessions or status meters)
            val isMedia = data.category == "transport" ||
                    packageLower.contains("spotify") ||
                    packageLower.contains("music") ||
                    packageLower.contains("audio") ||
                    packageLower.contains("podcast")

            val isNavigation = data.category == "navigation" ||
                    packageLower.contains("maps") ||
                    packageLower.contains("waze")

            val isCallCategory = data.category == "call" ||
                    packageLower.contains("dialer") ||
                    packageLower.contains("telecom") ||
                    packageLower.contains("phone")

            val isOngoingUserCall = data.isOngoing && isCallCategory && !data.isIncomingCall

            val isSystemMeter = (packageLower == "com.android.systemui" || packageLower == "android") &&
                    (titleLower.contains("charging") || textLower.contains("charging") ||
                     titleLower.contains("battery") || textLower.contains("battery") ||
                     titleLower.contains("usb") || textLower.contains("usb"))

            val isScreenshot = packageLower.contains("screencapture") || packageLower.contains("screenshot") ||
                    titleLower.contains("screenshot") || textLower.contains("screenshot")

            val isSyncPlaceholder = textLower.contains("checking for new messages") || textLower.contains("searching for new messages")

            val isSocialReaction = textLower.contains("liked your") || textLower.contains("liked a") ||
                    textLower.contains("liked _") || textLower.contains("reacted to your") ||
                    textLower.contains("started following") || textLower.contains("commented on") ||
                    titleLower.contains("liked your") || titleLower.contains("liked _")

            var isImportant = false
            var shouldAlert = false
            var decisionReason = "General notification; no matching rule"
            var finalSummary = defaultCleanSummary
            var aiCategory = "other"

            if (isOngoingUserCall) {
                decisionReason = "Outgoing or active in-call session (Silent)"
                aiCategory = "call"
            } else if (isMedia) {
                decisionReason = "Active media playback (Silent)"
                aiCategory = "media"
            } else if (isNavigation) {
                decisionReason = "Live navigation active (Silent)"
                aiCategory = "navigation"
            } else if (isSystemMeter || isScreenshot) {
                decisionReason = if (isScreenshot) "Screenshot captured" else "System status update"
                finalSummary = if (isScreenshot) "Screenshot saved" else defaultCleanSummary
                aiCategory = "system"
            } else if (isSyncPlaceholder) {
                decisionReason = "Conversation sync placeholder"
                aiCategory = "sync"
            } else if (isSocialReaction) {
                decisionReason = "Social activity update (Non-direct message)"
                aiCategory = "social"
            } else {
                // 3. User Rules Evaluation (Dual-Engine Routing)
                val enabledRules = ruleRepository.getEnabledRules()

                if (enabledRules.isEmpty()) {
                    decisionReason = "No active user rules"
                    aiCategory = "other"
                } else {
                    val parsedRules = enabledRules.map { rule ->
                        rule to RuleClassifier.classify(rule.text)
                    }

                    val semanticRules = parsedRules.filter { it.second.intent == RuleIntent.SEMANTIC_CONDITIONAL }
                    val simpleBlocks = parsedRules.filter { it.second.intent == RuleIntent.SIMPLE_BLOCK }
                    val simpleContacts = parsedRules.filter { it.second.intent == RuleIntent.SIMPLE_CONTACT }

                    // Route to Engine B (K2 Semantic AI) if any active rule is semantic / conditional
                    if (semanticRules.isNotEmpty()) {
                        Log.d("NotificationProcessor", "Routing to Engine B (K2 Horizon 0.9B AI) for ${enabledRules.size} active rules")
                        val prompt = K2PromptBuilder.buildPrompt(
                            rules = enabledRules.map { it.text },
                            appName = data.appName,
                            packageName = data.packageName,
                            title = data.title,
                            text = data.text,
                            sender = data.sender
                        )

                        val aiResponse = inferenceManager.analyze(prompt)
                        val analysis = K2ResponseParser.parse(aiResponse, defaultCleanSummary)

                        if (aiResponse != null) {
                            isImportant = analysis.important
                            shouldAlert = analysis.alert
                            decisionReason = "[🧠 K2 AI] ${analysis.reason}"
                            aiCategory = analysis.category
                            if (analysis.summary.isNotBlank() && !analysis.summary.startsWith("Summary of", ignoreCase = true)) {
                                finalSummary = analysis.summary
                            }
                        } else {
                            // Safe fallback if model is unavailable
                            Log.w("NotificationProcessor", "Engine B unavailable; falling back to Fast-Path checks")
                            val matchedContact = simpleContacts.firstOrNull {
                                matchesPersonName(it.second.targetPerson, data.sender) ||
                                (!isMedia && !isNavigation && matchesPersonName(it.second.targetPerson, data.title))
                            }
                            if (matchedContact != null) {
                                isImportant = true
                                shouldAlert = true
                                decisionReason = "[⚡ Fast Rule Fallback] Matched ${matchedContact.first.text}"
                                aiCategory = "messages"
                            } else {
                                isImportant = false
                                shouldAlert = false
                                decisionReason = "AI engine standby; no direct contact match"
                                aiCategory = "other"
                            }
                        }
                    } else {
                        // Route to Engine A (Fast-Path <1ms)
                        Log.d("NotificationProcessor", "Routing to Engine A (Fast Path <1ms) - Simple rules only")

                        // 1. Priority check for simple block rules
                        val matchedBlock = simpleBlocks.firstOrNull {
                            matchesPersonName(it.second.targetPerson, data.sender) ||
                            matchesPersonName(it.second.targetPerson, data.title)
                        }

                        if (matchedBlock != null) {
                            isImportant = false
                            shouldAlert = false
                            decisionReason = "[⚡ Fast Rule] Blocked: ${matchedBlock.first.text}"
                            aiCategory = "other"
                        } else {
                            // 2. Check for simple contact match
                            val matchedContact = simpleContacts.firstOrNull {
                                matchesPersonName(it.second.targetPerson, data.sender) ||
                                (!isMedia && !isNavigation && matchesPersonName(it.second.targetPerson, data.title))
                            }

                            if (matchedContact != null) {
                                isImportant = true
                                shouldAlert = true
                                decisionReason = "[⚡ Fast Rule] Matched: ${matchedContact.first.text}"
                                aiCategory = "messages"
                            } else {
                                isImportant = false
                                shouldAlert = false
                                decisionReason = "General notification; no matching rule"
                                aiCategory = "other"
                            }
                        }
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
