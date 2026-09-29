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

    fun isSenderMatch(ruleTarget: String?, data: NotificationData): Boolean {
        if (ruleTarget.isNullOrBlank()) return false

        // 1. Direct sender from MessagingStyle (e.g. "Arjun_Vasireddy", "Madhu")
        if (!data.sender.isNullOrBlank() && !data.sender.equals(data.appName, ignoreCase = true)) {
            if (matchesPersonName(ruleTarget, data.sender)) {
                return true
            }
        }

        // 2. Notification title for 1-on-1 chats, SMS, or Phone / Missed Calls
        if (!data.title.isNullOrBlank() && !data.title.equals(data.appName, ignoreCase = true)) {
            if (matchesPersonName(ruleTarget, data.title)) {
                return true
            }
        }

        // 3. Sender prefix in text for group messages (e.g. "Madhu: Hi everyone")
        val text = data.text?.trim()
        if (!text.isNullOrBlank() && text.contains(":")) {
            val possiblePrefix = text.substringBefore(":").trim()
            if (possiblePrefix.length in 2..30 && !possiblePrefix.contains("\n") && !possiblePrefix.contains(".")) {
                if (matchesPersonName(ruleTarget, possiblePrefix)) {
                    return true
                }
            }
        }

        return false
    }

    fun matchesPersonName(ruleTarget: String?, candidateName: String?): Boolean {
        if (ruleTarget.isNullOrBlank() || candidateName.isNullOrBlank()) return false
        val target = ruleTarget.lowercase().trim()
        val targetClean = target.replace(" ", "")
        val candidate = candidateName.lowercase().trim()
        val candidateClean = candidate.replace(" ", "")
        val candidateWords = candidate.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 }

        if (target.isEmpty() || candidate.isEmpty()) return false

        // Exact match
        if (candidateClean == targetClean) return true

        // Check word parts (e.g. "Arjun" in "Arjun_Vasireddy" or "Arjun Vasireddy")
        for (word in candidateWords) {
            val wordClean = word.replace("_", "")
            if (wordClean == targetClean || wordClean.startsWith(targetClean)) {
                return true
            }
        }

        // Prefix match (e.g. "arjun_vasireddy" starts with "arjun")
        val candidateNoUnderscore = candidateClean.replace("_", "")
        if (candidateNoUnderscore.startsWith(targetClean)) {
            return true
        }

        // Substring match for longer specific targets (e.g. "krishnavardhan")
        if (targetClean.length >= 6 && candidateClean.contains(targetClean)) {
            return true
        }

        return false
    }

    fun textMatchesDynamicAnchor(content: String, anchor: String): Boolean {
        val cleanAnchor = anchor.trim().lowercase()
        if (cleanAnchor.length < 2) return false

        // Multi-word phrase matching (e.g. "out for delivery", "offer letter", "play station")
        if (cleanAnchor.contains(" ")) {
            return content.contains(cleanAnchor)
        }

        val words = content.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 }
        return words.any { word ->
            word == cleanAnchor ||
            word == "${cleanAnchor}s" ||
            "${word}s" == cleanAnchor ||
            (cleanAnchor.length >= 5 && word.startsWith(cleanAnchor) && word.length <= cleanAnchor.length + 3)
        }
    }

    private fun inferCategoryFromContent(contentLower: String, appNameLower: String, defaultCategory: String?): String {
        val words = contentLower.split(Regex("[^a-zA-Z0-9_]+")).toSet()
        return when {
            defaultCategory == "call" || contentLower.contains("missed call") -> "calls"

            words.contains("interview") || words.contains("recruiter") || words.contains("hiring") ||
            words.contains("p0") || words.contains("incident") || words.contains("slack") ||
            words.contains("teams") || contentLower.contains("pull request") || words.contains("outage") ||
            words.contains("downtime") || words.contains("pagerduty") -> "work"

            words.contains("otp") || words.contains("debited") || words.contains("credited") ||
            words.contains("debit") || words.contains("credit") || words.contains("atm") ||
            words.contains("withdrawal") || words.contains("emi") || words.contains("account") ||
            words.contains("upi") || words.contains("bank") || words.contains("refund") ||
            words.contains("salary") || words.contains("phonepe") || words.contains("gpay") ||
            words.contains("paytm") || words.contains("cred") || words.contains("balance") -> "banking"

            words.contains("delivery") || words.contains("swiggy") || words.contains("zomato") ||
            words.contains("blinkit") || words.contains("zepto") || words.contains("parcel") ||
            words.contains("order") || words.contains("package") || words.contains("rider") ||
            words.contains("doorstep") || contentLower.contains("out for delivery") -> "delivery"

            words.contains("cab") || words.contains("uber") || words.contains("ola") ||
            words.contains("flight") || words.contains("pnr") || words.contains("train") ||
            words.contains("platform") || words.contains("boarding") -> "travel"

            words.contains("whatsapp") || words.contains("instagram") || words.contains("telegram") ||
            words.contains("message") || words.contains("chat") || defaultCategory == "msg" -> "messages"

            else -> "other"
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

            val isMissedCall = data.category == "missed_call" ||
                    data.title?.contains("missed call", ignoreCase = true) == true ||
                    data.text?.contains("missed call", ignoreCase = true) == true

            val defaultCleanSummary = when {
                isMissedCall -> {
                    val caller = data.sender ?: data.text ?: data.title ?: "Unknown"
                    "Missed call: $caller"
                }
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
            val contentLower = "$senderLower $titleLower $textLower $appLower $packageLower"

            // 2. Action & Noise Shielding Check (Zero DB storage for background transport, navigation, or progress)
            val isMedia = data.category == "transport"
            val isNavigation = data.category == "navigation" && data.isOngoing
            val isOngoingUserCall = data.category == "call" && data.isOngoing && !data.isIncomingCall
            val isSystemApp = data.packageName == "android" || data.packageName == "com.android.systemui"
            val isSystemMeter = isSystemApp && (data.category == "status" || data.category == "sys" || data.category == "service")
            val isProgress = data.category == "progress" && data.isOngoing

            if (isMedia || isNavigation || isOngoingUserCall || isSystemMeter || isProgress) {
                Log.d("NotificationProcessor", "Discarding background session/system event from ${data.packageName} (Zero DB storage)")
                return
            }

            var isImportant = false
            var shouldAlert = false
            var decisionReason = "General notification; no matching rule"
            var finalSummary = defaultCleanSummary
            var aiCategory = inferCategoryFromContent(contentLower, appLower, data.category)

            // 3. User Rules Evaluation (Hybrid Dual-Engine: AOT Fast Engine + Selective K2 Deep Reasoning)
            val enabledRules = ruleRepository.getEnabledRules()

            if (enabledRules.isEmpty()) {
                decisionReason = "No active user rules"
                aiCategory = "other"
            } else {
                // Check if any matching candidate rule requires K2 Deep AI Reasoning (emotions, subjective tone, urgency)
                val candidateDeepRule = enabledRules.firstOrNull { rule ->
                    if (rule.semanticDepth != "K2_DEEP") return@firstOrNull false
                    val targetPerson = rule.targetPerson
                    val targetApps = rule.getTargetApps()
                    val isPersonRule = !targetPerson.isNullOrBlank()
                    val isAppRule = targetApps.isNotEmpty()

                    val senderMatches = isPersonRule && isSenderMatch(targetPerson, data)
                    val appMatch = if (isAppRule) {
                        targetApps.any { targetApp ->
                            packageLower.contains(targetApp.lowercase()) || appLower.contains(targetApp.lowercase())
                        }
                    } else false

                    (isPersonRule && senderMatches) || (isAppRule && appMatch) || (!isPersonRule && !isAppRule)
                }

                var executedViaK2 = false

                if (candidateDeepRule != null) {
                    val isDeepSuppression = candidateDeepRule.action == "MUTE" ||
                            candidateDeepRule.text.lowercase().contains("not important") ||
                            candidateDeepRule.text.lowercase().contains("never important") ||
                            candidateDeepRule.text.lowercase().contains("no alert") ||
                            candidateDeepRule.text.lowercase().startsWith("ignore") ||
                            candidateDeepRule.text.lowercase().startsWith("block") ||
                            candidateDeepRule.text.lowercase().startsWith("mute")

                    // Extract target emotion / tone keywords from rule
                    val targetTones = RuleClassifier.getEmotionTriggers(candidateDeepRule.text)

                    // Build single condition evaluation prompt for K2
                    val prompt = K2PromptBuilder.buildTonePrompt(
                        ruleText = candidateDeepRule.text,
                        targetTones = targetTones,
                        appName = data.appName,
                        packageName = data.packageName,
                        title = data.title,
                        text = data.text,
                        sender = data.sender
                    )
                    val rawResponse = inferenceManager.analyze(prompt)
                    if (!rawResponse.isNullOrBlank()) {
                        val toneResult = K2ResponseParser.parseToneResult(rawResponse)

                        if (isDeepSuppression) {
                            if (toneResult.toneMatched) {
                                // Suppression condition matched (e.g. sender is angry) -> Strictly MUTE
                                isImportant = false
                                shouldAlert = false
                                val cleanReason = toneResult.reason.ifBlank { "Suppression tone condition met" }
                                decisionReason = "[🧠 K2 Deep AI] $cleanReason (Muted per rule)"
                                executedViaK2 = true
                            } else {
                                // Suppression condition NOT matched (e.g. sender is calm/positive/not angry)
                                // Do not mark executedViaK2 = true, so execution smoothly falls through to check remaining rules (e.g. Teams rule)!
                                Log.d("NotificationProcessor", "Deep suppression tone condition not met (${toneResult.reason}); evaluating remaining rules")
                            }
                        } else {
                            // Deep positive rule (e.g. "urgent crisis is important")
                            if (toneResult.toneMatched) {
                                isImportant = true
                                shouldAlert = true
                                decisionReason = "[🧠 K2 Deep AI] ${toneResult.reason}"
                                executedViaK2 = true
                            }
                        }
                    } else {
                        Log.w("NotificationProcessor", "K2 inference returned null/unavailable; falling back to AOT evaluation")
                    }
                }

                if (!executedViaK2) {
                    var hasPositiveMatch = false
                    var hasExplicitExclusion = false
                    var exclusionReason = ""
                    var positiveReason = ""
                    var matchedRuleText = ""

                    for (rule in enabledRules) {
                        if (candidateDeepRule != null && rule.id == candidateDeepRule.id) {
                            continue
                        }
                    val targetPerson = rule.targetPerson
                    val action = rule.action
                    val positiveTopics = rule.getPositiveTopics()
                    val excludedTopics = rule.getExcludedTopics()
                    val targetApps = rule.getTargetApps()

                    val isPersonRule = !targetPerson.isNullOrBlank()
                    val isAppRule = targetApps.isNotEmpty()

                    // If rule has app restrictions, verify notification app matches (or topic matches)
                    val appMatch = if (isAppRule) {
                        targetApps.any { targetApp ->
                            packageLower.contains(targetApp.lowercase()) || appLower.contains(targetApp.lowercase())
                        }
                    } else false

                    if (isAppRule && !appMatch) {
                        val hasTopicMatch = positiveTopics.any { textMatchesDynamicAnchor(contentLower, it) }
                        if (!hasTopicMatch) continue
                    }

                    // Evaluate sender match
                    val senderMatches = isPersonRule && isSenderMatch(targetPerson, data)

                    // Case A: Person rule where sender does NOT match -> Skip (Prevents body mentions like "Hi madhu" by Arjun from triggering Madhu's rule)
                    if (isPersonRule && !senderMatches) {
                        continue
                    }

                    // Check Exclusions (Exclusions ALWAYS take precedence)
                    val matchesExclusion = excludedTopics.isNotEmpty() && excludedTopics.any { anchor ->
                        textMatchesDynamicAnchor(contentLower, anchor)
                    }

                    if (matchesExclusion) {
                        val hitAnchor = excludedTopics.firstOrNull { textMatchesDynamicAnchor(contentLower, it) } ?: "excluded topic"
                        hasExplicitExclusion = true
                        exclusionReason = "[⚡ Filtered] Excluded topic '$hitAnchor' in rule: ${rule.text}"
                        break // Precedence: exclusion stops positive evaluation
                    }

                    // Check Block / Mute action
                    if (action.equals("MUTE", ignoreCase = true) || rule.ruleIntent == "SIMPLE_BLOCK") {
                        if (isPersonRule && senderMatches) {
                            hasExplicitExclusion = true
                            exclusionReason = "[⚡ Blocked] Muted sender: ${rule.text}"
                            break
                        } else if (isAppRule && !isPersonRule && appMatch) {
                            hasExplicitExclusion = true
                            exclusionReason = "[⚡ Blocked] Muted app: ${rule.text}"
                            break
                        }
                    }

                    // Check Positive Topics
                    val matchesPositiveTopic = positiveTopics.isNotEmpty() && positiveTopics.any { anchor ->
                        textMatchesDynamicAnchor(contentLower, anchor)
                    }

                    // Check Pure Contact Rule (no topics required)
                    val isPureContact = isPersonRule && positiveTopics.isEmpty() && excludedTopics.isEmpty()

                    // Check Pure App Rule (no person, no topics required - e.g. "any msg from teams app is important")
                    val isPureApp = isAppRule && !isPersonRule && positiveTopics.isEmpty() && excludedTopics.isEmpty()

                    if (isPureContact && senderMatches) {
                        hasPositiveMatch = true
                        matchedRuleText = rule.text
                        positiveReason = "[⚡ Fast Rule] Matched contact: ${rule.text}"
                    } else if (isPureApp && appMatch) {
                        hasPositiveMatch = true
                        matchedRuleText = rule.text
                        positiveReason = "[⚡ Fast Rule] Matched app: ${rule.text}"
                    } else if (matchesPositiveTopic) {
                        if (!isAppRule || appMatch) {
                            hasPositiveMatch = true
                            matchedRuleText = rule.text
                            val hitAnchor = positiveTopics.firstOrNull { textMatchesDynamicAnchor(contentLower, it) } ?: "topic"
                            positiveReason = "[⚡ Fast Rule] Matched '$hitAnchor' in rule: ${rule.text}"
                        }
                    } else if (isPersonRule && senderMatches && excludedTopics.isNotEmpty() && !matchesExclusion) {
                        // Person rule with exclusions only (e.g. "Arjun not movies") and no exclusion matched!
                        hasPositiveMatch = true
                        matchedRuleText = rule.text
                        positiveReason = "[⚡ Fast Rule] Matched: ${rule.text}"
                    } else if (isAppRule && !isPersonRule && appMatch && excludedTopics.isNotEmpty() && !matchesExclusion) {
                        // App rule with exclusions only (e.g. "Teams not memes") and no exclusion matched!
                        hasPositiveMatch = true
                        matchedRuleText = rule.text
                        positiveReason = "[⚡ Fast Rule] Matched app: ${rule.text}"
                    }
                }

                if (hasExplicitExclusion) {
                    isImportant = false
                    shouldAlert = false
                    decisionReason = exclusionReason
                    aiCategory = "other"
                } else if (hasPositiveMatch) {
                    isImportant = true
                    shouldAlert = true
                    decisionReason = positiveReason
                } else {
                    isImportant = false
                    shouldAlert = false
                    decisionReason = "General notification; no matching rule"
                    aiCategory = "other"
                }
                }
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
            Log.d("NotificationProcessor", "Pure AOT Analysis Complete in ${totalLatency}ms (<0.2ms!): Important=$isImportant, Alert=$shouldAlert, Sender=${data.sender ?: data.appName}, Reason=$decisionReason")

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
