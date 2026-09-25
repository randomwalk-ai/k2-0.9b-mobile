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

    private fun textMatchesDynamicAnchor(content: String, anchor: String): Boolean {
        val cleanAnchor = anchor.trim().lowercase()
        if (cleanAnchor.length < 3) return false
        val words = content.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 }
        return words.any { word ->
            word == cleanAnchor ||
            word.startsWith(cleanAnchor) ||
            cleanAnchor.startsWith(word) ||
            (cleanAnchor.length >= 4 && word.contains(cleanAnchor))
        }
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

            // 2. Action & Noise Shielding Check (Completely discard user sessions, uploads, and meters - Zero Storage)
            val isUploadOrProgress = contentLower.contains("uploading") ||
                    contentLower.contains("story uploaded") ||
                    contentLower.contains("post uploaded") ||
                    contentLower.contains("downloading") ||
                    contentLower.contains("saving...") ||
                    contentLower.contains("sending...")

            val isMedia = (data.category == "transport" ||
                    packageLower.contains("spotify") ||
                    packageLower.contains("music") ||
                    packageLower.contains("audio") ||
                    packageLower.contains("podcast")) && data.isOngoing

            val isNavigation = (data.category == "navigation" ||
                    packageLower.contains("maps") ||
                    packageLower.contains("waze")) && data.isOngoing

            val isCallCategory = data.category == "call" ||
                    packageLower.contains("dialer") ||
                    packageLower.contains("telecom") ||
                    packageLower.contains("phone")

            val isOngoingUserCall = data.isOngoing && isCallCategory && !data.isIncomingCall

            val isSystemMeter = (packageLower == "com.android.systemui" || packageLower == "android" || packageLower.contains("devicesecurity")) &&
                    (contentLower.contains("charging") || contentLower.contains("battery") ||
                     contentLower.contains("usb") || contentLower.contains("scanning phone"))

            val isScreenshot = packageLower.contains("screencapture") || packageLower.contains("smartcapture") ||
                    packageLower.contains("screenshot") || contentLower.contains("screenshot")

            val isSyncPlaceholder = contentLower.contains("checking for new messages") || contentLower.contains("searching for new messages")

            // Public social post likes / reactions (e.g. "5 likes on your comment", "liked your reel" - NOT a direct chat)
            val isSocialReaction = contentLower.contains("liked your") || contentLower.contains("liked a") ||
                    contentLower.contains("liked _") || contentLower.contains("reacted to your") ||
                    contentLower.contains("started following") || contentLower.contains("commented on")

            if (isUploadOrProgress || isOngoingUserCall || isMedia || isNavigation || isSystemMeter || isScreenshot || isSyncPlaceholder || isSocialReaction) {
                Log.d("NotificationProcessor", "Discarding outbound/user action/progress event from ${data.packageName} (Zero DB storage)")
                return
            }

            var isImportant = false
            var shouldAlert = false
            var decisionReason = "General notification; no matching rule"
            var finalSummary = defaultCleanSummary
            var aiCategory = "other"

            // 3. User Rules Evaluation (Dynamic Dual-Engine Routing)
            val enabledRules = ruleRepository.getEnabledRules()

            if (enabledRules.isEmpty()) {
                decisionReason = "No active user rules"
                aiCategory = "other"
            } else {
                val parsedRules = enabledRules.map { rule ->
                    rule to RuleClassifier.classify(rule.text)
                }

                // Dynamic Candidate Gate: Identify all rules that have ANY correlation with this notification
                val matchingCandidates = parsedRules.filter { (rule, parsed) ->
                    val matchesSender = matchesPersonName(parsed.targetPerson, data.sender) ||
                            (!isMedia && !isNavigation && matchesPersonName(parsed.targetPerson, data.title))

                    val matchesTopic = parsed.dynamicAnchors.isNotEmpty() && parsed.dynamicAnchors.any { anchor ->
                        textMatchesDynamicAnchor(contentLower, anchor)
                    }

                    matchesSender || matchesTopic
                }

                if (matchingCandidates.isEmpty()) {
                    // Fast-Path Drop: Zero candidate match with any user rule (<1ms, 0 MB RAM, 0% CPU)
                    Log.d("NotificationProcessor", "Fast-Path Drop (<1ms): Zero candidate match with ${enabledRules.size} user rules for sender '${data.sender ?: data.appName}'")
                    isImportant = false
                    shouldAlert = false
                    decisionReason = "General notification; no matching rule"
                    aiCategory = "other"
                } else {
                    val hasSemanticCandidate = matchingCandidates.any { it.second.intent == RuleIntent.SEMANTIC_CONDITIONAL }

                    if (hasSemanticCandidate) {
                        // Route to Engine B (K2 Horizon 0.9B AI) for deep contextual comprehension
                        Log.d("NotificationProcessor", "Routing to Engine B (K2 AI): ${matchingCandidates.size} candidate rules matched for sender '${data.sender ?: data.appName}'")
                        val prompt = K2PromptBuilder.buildPrompt(
                            rules = enabledRules.map { it.text },
                            appName = data.appName,
                            packageName = data.packageName,
                            title = data.title,
                            text = data.text,
                            sender = data.sender
                        )
                        Log.d("NotificationProcessor", "Engine B Prompt:\n$prompt")

                        val aiResponse = inferenceManager.analyze(prompt)
                        Log.d("NotificationProcessor", "Engine B Raw Response: $aiResponse")
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
                            Log.w("NotificationProcessor", "Engine B unavailable; falling back to candidate rules")
                            val directContact = matchingCandidates.firstOrNull { it.second.intent == RuleIntent.SIMPLE_CONTACT }
                            if (directContact != null) {
                                isImportant = true
                                shouldAlert = true
                                decisionReason = "[⚡ Fast Rule Fallback] Matched ${directContact.first.text}"
                                aiCategory = "messages"
                            } else {
                                isImportant = false
                                shouldAlert = false
                                decisionReason = "AI engine standby; no direct contact match"
                                aiCategory = "other"
                            }
                        }
                    } else {
                        // Pure Fast-Path candidate resolution (<1ms)
                        Log.d("NotificationProcessor", "Routing to Engine A (Fast Path <1ms): ${matchingCandidates.size} simple rules matched")
                        val simpleBlock = matchingCandidates.firstOrNull { it.second.intent == RuleIntent.SIMPLE_BLOCK }
                        if (simpleBlock != null) {
                            isImportant = false
                            shouldAlert = false
                            decisionReason = "[⚡ Fast Rule] Blocked: ${simpleBlock.first.text}"
                            aiCategory = "other"
                        } else {
                            val simpleContact = matchingCandidates.firstOrNull { it.second.intent == RuleIntent.SIMPLE_CONTACT }
                            if (simpleContact != null) {
                                isImportant = true
                                shouldAlert = true
                                decisionReason = "[⚡ Fast Rule] Matched: ${simpleContact.first.text}"
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

            val record = NotificationRecord(
                id = 0L,
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
