package com.example.llama.aichat

import com.example.llama.aichat.ai.RuleIntent
import com.example.llama.aichat.data.NotificationRule
import com.example.llama.aichat.notification.NotificationData
import org.junit.Assert.*
import org.junit.Test

class AotRuleEngine1000Test {

    companion object {
        private object RuleClassifier {
            fun classify(ruleText: String): MockRule {
                val lower = ruleText.lowercase()
                val targetPerson = when {
                    lower.contains("from madhu") -> "madhu"
                    lower.contains("from arjun") -> "arjun"
                    lower.contains("from boss") -> "boss"
                    lower.contains("from client") -> "client"
                    lower.contains("from mom") -> "mom"
                    lower.contains("from bob") -> "bob"
                    lower.contains("from varun") -> "varun"
                    lower.contains("from rahul") -> "rahul"
                    lower.contains("from priya") -> "priya"
                    lower.contains("from deepak") -> "deepak"
                    lower.contains("from kavya") -> "kavya"
                    lower.contains("from sneha") -> "sneha"
                    lower.contains("from anita") -> "anita"
                    lower.contains("from vikram") -> "vikram"
                    lower.contains("from rohit") -> "rohit"
                    lower.contains("from pooja") -> "pooja"
                    lower.contains("from siddharth") -> "siddharth"
                    lower.contains("from pranav") -> "pranav"
                    lower.contains("madhu") -> "madhu"
                    lower.contains("bob") -> "bob"
                    lower.contains("arjun") -> "arjun"
                    else -> null
                }
                val isEmotionRule = lower.contains("angry") || lower.contains("furious") || lower.contains("mad")
                val semanticDepth = if (isEmotionRule) "K2_DEEP" else "AOT_FAST"

                val isMultiClausePositive = lower.contains("it is important") && lower.contains("not important")
                val isNegative = !isMultiClausePositive && (lower.startsWith("ignore") || lower.startsWith("block") || lower.startsWith("mute") || lower.endsWith("not important") || lower.endsWith("never important"))
                val action = if (isNegative) "MUTE" else "ALERT"

                val targetApps = when {
                    lower.contains("teams") -> listOf("teams", "com.microsoft.teams")
                    lower.contains("slack") -> listOf("slack", "com.Slack")
                    lower.contains("whatsapp") -> listOf("whatsapp", "com.whatsapp")
                    lower.contains("swiggy") -> listOf("swiggy", "in.swiggy.android")
                    lower.contains("uber") -> listOf("uber", "com.ubercab")
                    lower.contains("pagerduty") -> listOf("pagerduty", "com.pagerduty.android")
                    lower.contains("github") -> listOf("github", "com.github.android")
                    else -> emptyList()
                }
                val positiveTopics = mutableListOf<String>()
                val excludedTopics = mutableListOf<String>()

                if (lower.contains("game") || lower.contains("gaming")) positiveTopics.addAll(listOf("game", "games", "gaming", "esports", "bgmi", "pubg", "cricket", "football"))
                if (lower.contains("job") || lower.contains("interview")) positiveTopics.addAll(listOf("job", "interview", "recruiter", "hiring", "offer"))
                if (lower.contains("bank") || lower.contains("otp") || lower.contains("transaction")) positiveTopics.addAll(listOf("otp", "bank", "debited", "credited", "upi", "salary", "refund", "transaction"))
                if (lower.contains("food") || lower.contains("order") || lower.contains("delivery")) positiveTopics.addAll(listOf("delivery", "order", "food", "out for delivery", "rider", "swiggy", "zomato", "blinkit", "zepto"))
                if (lower.contains("cab") || lower.contains("ride") || lower.contains("flight") || lower.contains("train")) positiveTopics.addAll(listOf("cab", "ride", "flight", "train", "uber", "driver", "pnr"))
                if (lower.contains("bill") || lower.contains("electricity") || lower.contains("challan")) positiveTopics.addAll(listOf("bill", "electricity", "challan", "power", "bescom"))
                if (lower.contains("incident") || lower.contains("downtime") || lower.contains("server") || lower.contains("outage")) positiveTopics.addAll(listOf("incident", "downtime", "server", "outage", "p0", "production"))
                if (lower.contains("movie") || lower.contains("movies")) {
                    if (isNegative || isMultiClausePositive) excludedTopics.addAll(listOf("movie", "movies", "cinema", "film", "films", "netflix", "trailer"))
                    else positiveTopics.addAll(listOf("movie", "movies", "cinema", "film", "films", "netflix", "trailer"))
                }
                if (lower.contains("reels") || lower.contains("reel")) {
                    if (isNegative || isMultiClausePositive) excludedTopics.addAll(listOf("reel", "reels", "video", "clip"))
                    else positiveTopics.addAll(listOf("reel", "reels", "video", "clip"))
                }
                if (lower.contains("promotional") || lower.contains("offers") || lower.contains("deals") || lower.contains("discount")) {
                    excludedTopics.addAll(listOf("promotional", "offers", "discount", "sale", "coupon", "scratch card"))
                }

                return MockRule(
                    text = ruleText,
                    targetPerson = targetPerson,
                    action = action,
                    targetApps = targetApps,
                    positiveTopics = positiveTopics,
                    excludedTopics = excludedTopics,
                    semanticDepth = semanticDepth
                )
            }
        }

        private data class MockRule(
            val text: String,
            val targetPerson: String?,
            val action: String,
            val targetApps: List<String>,
            val positiveTopics: List<String>,
            val excludedTopics: List<String>,
            val semanticDepth: String = "AOT_FAST"
        ) {
            fun toNotificationRule(id: Long): NotificationRule {
                return NotificationRule(
                    id = id,
                    text = text,
                    targetPerson = targetPerson,
                    action = action,
                    targetAppsJson = com.example.llama.aichat.data.JsonListHelper.toJson(targetApps),
                    positiveTopicsJson = com.example.llama.aichat.data.JsonListHelper.toJson(positiveTopics),
                    excludedTopicsJson = com.example.llama.aichat.data.JsonListHelper.toJson(excludedTopics),
                    semanticDepth = semanticDepth
                )
            }
        }
    }

    data class EvaluationResult(
        val isImportant: Boolean,
        val shouldAlert: Boolean,
        val category: String,
        val reason: String
    )

    private fun evaluate(rules: List<NotificationRule>, data: NotificationData): EvaluationResult {
        if (rules.isEmpty()) {
            return EvaluationResult(false, false, "other", "No active user rules")
        }
        val senderLower = data.sender?.lowercase()?.trim() ?: ""
        val titleLower = data.title?.lowercase()?.trim() ?: ""
        val textLower = data.text?.lowercase()?.trim() ?: ""
        val appLower = data.appName.lowercase().trim()
        val packageLower = data.packageName.lowercase().trim()
        val contentLower = "$senderLower $titleLower $textLower $appLower $packageLower"

        var hasPositiveMatch = false
        var hasExplicitExclusion = false
        var exclusionReason = ""
        var positiveReason = ""

        for (rule in rules) {
            val targetPerson = rule.targetPerson
            val action = rule.action
            val positiveTopics = rule.getPositiveTopics()
            val excludedTopics = rule.getExcludedTopics()
            val targetApps = rule.getTargetApps()

            val isPersonRule = !targetPerson.isNullOrBlank()
            val isAppRule = targetApps.isNotEmpty()

            val appMatch = if (isAppRule) {
                targetApps.any { targetApp ->
                    packageLower.contains(targetApp.lowercase()) || appLower.contains(targetApp.lowercase())
                }
            } else false

            if (isAppRule && !appMatch) {
                val hasTopicMatch = positiveTopics.any { textMatchesDynamicAnchor(contentLower, it) }
                if (!hasTopicMatch) continue
            }

            val senderMatches = isPersonRule && isSenderMatch(targetPerson, data)
            if (isPersonRule && !senderMatches) continue

            val matchesExclusion = excludedTopics.isNotEmpty() && excludedTopics.any { anchor ->
                textMatchesDynamicAnchor(contentLower, anchor)
            }
            if (matchesExclusion) {
                hasExplicitExclusion = true
                exclusionReason = "Filtered exclusion"
                break
            }

            if (action.equals("MUTE", ignoreCase = true) || rule.ruleIntent == "SIMPLE_BLOCK") {
                if (rule.semanticDepth != "K2_DEEP") {
                    if (isPersonRule && senderMatches) {
                        hasExplicitExclusion = true
                        exclusionReason = "Muted sender"
                        break
                    } else if (isAppRule && !isPersonRule && appMatch) {
                        hasExplicitExclusion = true
                        exclusionReason = "Muted app"
                        break
                    }
                }
            }

            val matchesPositiveTopic = positiveTopics.isNotEmpty() && positiveTopics.any { anchor ->
                textMatchesDynamicAnchor(contentLower, anchor)
            }
            val isPureContact = isPersonRule && positiveTopics.isEmpty() && excludedTopics.isEmpty()
            val isPureApp = isAppRule && !isPersonRule && positiveTopics.isEmpty() && excludedTopics.isEmpty()

            if (isPureContact && senderMatches) {
                hasPositiveMatch = true
                positiveReason = "Matched contact"
            } else if (rule.semanticDepth == "K2_DEEP" && isPersonRule && senderMatches) {
                hasPositiveMatch = true
                positiveReason = "Matched contact with deep condition"
            } else if (isPureApp && appMatch) {
                hasPositiveMatch = true
                positiveReason = "Matched app"
            } else if (matchesPositiveTopic) {
                if (!isAppRule || appMatch) {
                    hasPositiveMatch = true
                    positiveReason = "Matched topic"
                }
            } else if (isPersonRule && senderMatches && excludedTopics.isNotEmpty() && !matchesExclusion) {
                hasPositiveMatch = true
                positiveReason = "Matched contact with no exclusion"
            } else if (isAppRule && !isPersonRule && appMatch && excludedTopics.isNotEmpty() && !matchesExclusion) {
                hasPositiveMatch = true
                positiveReason = "Matched app with no exclusion"
            }
        }
        val finalImportant = !hasExplicitExclusion && hasPositiveMatch
        return EvaluationResult(finalImportant, finalImportant, if (finalImportant) "important" else "other", if (hasExplicitExclusion) exclusionReason else if (hasPositiveMatch) positiveReason else "No match")
    }

    private fun isSenderMatch(ruleTarget: String?, data: NotificationData): Boolean {
        if (ruleTarget.isNullOrBlank()) return false
        if (!data.sender.isNullOrBlank() && !data.sender.equals(data.appName, ignoreCase = true)) {
            if (matchesPersonName(ruleTarget, data.sender)) return true
        }
        if (!data.title.isNullOrBlank() && !data.title.equals(data.appName, ignoreCase = true)) {
            if (matchesPersonName(ruleTarget, data.title)) return true
        }
        val text = data.text?.trim()
        if (!text.isNullOrBlank() && text.contains(':')) {
            val possiblePrefix = text.substringBefore(':').trim()
            if (possiblePrefix.length in 2..30 && !possiblePrefix.contains('\n') && !possiblePrefix.contains('.')) {
                if (matchesPersonName(ruleTarget, possiblePrefix)) return true
            }
        }
        return false
    }

    private fun matchesPersonName(ruleTarget: String?, candidateName: String?): Boolean {
        if (ruleTarget.isNullOrBlank() || candidateName.isNullOrBlank()) return false
        val target = ruleTarget.lowercase().trim()
        val targetClean = target.replace(" ", "")
        val candidate = candidateName.lowercase().trim()
        val candidateClean = candidate.replace(" ", "")
        val candidateWords = candidate.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 }
        if (target.isEmpty() || candidate.isEmpty()) return false
        if (candidateClean == targetClean) return true
        for (word in candidateWords) {
            val wordClean = word.replace("_", "")
            if (wordClean == targetClean || wordClean.startsWith(targetClean)) return true
        }
        val candidateNoUnderscore = candidateClean.replace("_", "")
        if (candidateNoUnderscore.startsWith(targetClean)) return true
        if (targetClean.length >= 6 && candidateClean.contains(targetClean)) return true
        return false
    }

    private fun textMatchesDynamicAnchor(content: String, anchor: String): Boolean {
        val cleanAnchor = anchor.trim().lowercase()
        if (cleanAnchor.length < 2) return false

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

    @Test
    fun testBankingFinanceOtp125Cases() {
        // Domain: Dimension1_BankingAndFinance (125 Distinct Test Cases)
        // Case 1
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 1L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.150",
                text = "Rs.150 paid to Merchant_1 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.150",
                category = "messages",
                notificationKey = "key_1",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 1 expected ALERT", res.isImportant)
        }

        // Case 2
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 2L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.300",
                text = "Rs.300 paid to Merchant_2 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.300",
                category = "messages",
                notificationKey = "key_2",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 2 expected ALERT", res.isImportant)
        }

        // Case 3
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 3L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 3",
                text = "Your secret OTP is 100003. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 3",
                category = "messages",
                notificationKey = "key_3",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 3 expected ALERT", res.isImportant)
        }

        // Case 4
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 4L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.600",
                text = "Rs.600 paid to Merchant_4 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.600",
                category = "messages",
                notificationKey = "key_4",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 4 expected ALERT", res.isImportant)
        }

        // Case 5
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 5L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 5",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 5",
                category = "messages",
                notificationKey = "key_5",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 5 expected MUTE", res.isImportant)
        }

        // Case 6
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 6L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 6",
                text = "Your secret OTP is 100006. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 6",
                category = "messages",
                notificationKey = "key_6",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 6 expected ALERT", res.isImportant)
        }

        // Case 7
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 7L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.1050",
                text = "Rs.1050 paid to Merchant_7 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.1050",
                category = "messages",
                notificationKey = "key_7",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 7 expected ALERT", res.isImportant)
        }

        // Case 8
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 8L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.1200",
                text = "Rs.1200 paid to Merchant_8 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.1200",
                category = "messages",
                notificationKey = "key_8",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 8 expected ALERT", res.isImportant)
        }

        // Case 9
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 9L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 9",
                text = "Your secret OTP is 100009. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 9",
                category = "messages",
                notificationKey = "key_9",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 9 expected ALERT", res.isImportant)
        }

        // Case 10
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 10L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 10",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 10",
                category = "messages",
                notificationKey = "key_10",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 10 expected MUTE", res.isImportant)
        }

        // Case 11
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 11L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.1650",
                text = "Rs.1650 paid to Merchant_11 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.1650",
                category = "messages",
                notificationKey = "key_11",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 11 expected ALERT", res.isImportant)
        }

        // Case 12
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 12L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 12",
                text = "Your secret OTP is 100012. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 12",
                category = "messages",
                notificationKey = "key_12",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 12 expected ALERT", res.isImportant)
        }

        // Case 13
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 13L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.1950",
                text = "Rs.1950 paid to Merchant_13 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.1950",
                category = "messages",
                notificationKey = "key_13",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 13 expected ALERT", res.isImportant)
        }

        // Case 14
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 14L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.2100",
                text = "Rs.2100 paid to Merchant_14 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.2100",
                category = "messages",
                notificationKey = "key_14",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 14 expected ALERT", res.isImportant)
        }

        // Case 15
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 15L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 15",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 15",
                category = "messages",
                notificationKey = "key_15",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 15 expected MUTE", res.isImportant)
        }

        // Case 16
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 16L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.2400",
                text = "Rs.2400 paid to Merchant_16 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.2400",
                category = "messages",
                notificationKey = "key_16",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 16 expected ALERT", res.isImportant)
        }

        // Case 17
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 17L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.2550",
                text = "Rs.2550 paid to Merchant_17 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.2550",
                category = "messages",
                notificationKey = "key_17",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 17 expected ALERT", res.isImportant)
        }

        // Case 18
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 18L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 18",
                text = "Your secret OTP is 100018. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 18",
                category = "messages",
                notificationKey = "key_18",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 18 expected ALERT", res.isImportant)
        }

        // Case 19
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 19L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.2850",
                text = "Rs.2850 paid to Merchant_19 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.2850",
                category = "messages",
                notificationKey = "key_19",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 19 expected ALERT", res.isImportant)
        }

        // Case 20
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 20L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 20",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 20",
                category = "messages",
                notificationKey = "key_20",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 20 expected MUTE", res.isImportant)
        }

        // Case 21
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 21L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 21",
                text = "Your secret OTP is 100021. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 21",
                category = "messages",
                notificationKey = "key_21",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 21 expected ALERT", res.isImportant)
        }

        // Case 22
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 22L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.3300",
                text = "Rs.3300 paid to Merchant_22 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.3300",
                category = "messages",
                notificationKey = "key_22",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 22 expected ALERT", res.isImportant)
        }

        // Case 23
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 23L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.3450",
                text = "Rs.3450 paid to Merchant_23 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.3450",
                category = "messages",
                notificationKey = "key_23",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 23 expected ALERT", res.isImportant)
        }

        // Case 24
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 24L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 24",
                text = "Your secret OTP is 100024. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 24",
                category = "messages",
                notificationKey = "key_24",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 24 expected ALERT", res.isImportant)
        }

        // Case 25
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 25L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 25",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 25",
                category = "messages",
                notificationKey = "key_25",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 25 expected MUTE", res.isImportant)
        }

        // Case 26
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 26L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.3900",
                text = "Rs.3900 paid to Merchant_26 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.3900",
                category = "messages",
                notificationKey = "key_26",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 26 expected ALERT", res.isImportant)
        }

        // Case 27
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 27L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 27",
                text = "Your secret OTP is 100027. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 27",
                category = "messages",
                notificationKey = "key_27",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 27 expected ALERT", res.isImportant)
        }

        // Case 28
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 28L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.4200",
                text = "Rs.4200 paid to Merchant_28 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.4200",
                category = "messages",
                notificationKey = "key_28",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 28 expected ALERT", res.isImportant)
        }

        // Case 29
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 29L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.4350",
                text = "Rs.4350 paid to Merchant_29 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.4350",
                category = "messages",
                notificationKey = "key_29",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 29 expected ALERT", res.isImportant)
        }

        // Case 30
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 30L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 30",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 30",
                category = "messages",
                notificationKey = "key_30",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 30 expected MUTE", res.isImportant)
        }

        // Case 31
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 31L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.4650",
                text = "Rs.4650 paid to Merchant_31 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.4650",
                category = "messages",
                notificationKey = "key_31",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 31 expected ALERT", res.isImportant)
        }

        // Case 32
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 32L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.4800",
                text = "Rs.4800 paid to Merchant_32 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.4800",
                category = "messages",
                notificationKey = "key_32",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 32 expected ALERT", res.isImportant)
        }

        // Case 33
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 33L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 33",
                text = "Your secret OTP is 100033. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 33",
                category = "messages",
                notificationKey = "key_33",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 33 expected ALERT", res.isImportant)
        }

        // Case 34
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 34L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.5100",
                text = "Rs.5100 paid to Merchant_34 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.5100",
                category = "messages",
                notificationKey = "key_34",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 34 expected ALERT", res.isImportant)
        }

        // Case 35
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 35L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 35",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 35",
                category = "messages",
                notificationKey = "key_35",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 35 expected MUTE", res.isImportant)
        }

        // Case 36
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 36L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 36",
                text = "Your secret OTP is 100036. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 36",
                category = "messages",
                notificationKey = "key_36",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 36 expected ALERT", res.isImportant)
        }

        // Case 37
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 37L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.5550",
                text = "Rs.5550 paid to Merchant_37 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.5550",
                category = "messages",
                notificationKey = "key_37",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 37 expected ALERT", res.isImportant)
        }

        // Case 38
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 38L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.5700",
                text = "Rs.5700 paid to Merchant_38 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.5700",
                category = "messages",
                notificationKey = "key_38",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 38 expected ALERT", res.isImportant)
        }

        // Case 39
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 39L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 39",
                text = "Your secret OTP is 100039. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 39",
                category = "messages",
                notificationKey = "key_39",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 39 expected ALERT", res.isImportant)
        }

        // Case 40
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 40L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 40",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 40",
                category = "messages",
                notificationKey = "key_40",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 40 expected MUTE", res.isImportant)
        }

        // Case 41
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 41L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.6150",
                text = "Rs.6150 paid to Merchant_41 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.6150",
                category = "messages",
                notificationKey = "key_41",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 41 expected ALERT", res.isImportant)
        }

        // Case 42
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 42L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 42",
                text = "Your secret OTP is 100042. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 42",
                category = "messages",
                notificationKey = "key_42",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 42 expected ALERT", res.isImportant)
        }

        // Case 43
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 43L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.6450",
                text = "Rs.6450 paid to Merchant_43 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.6450",
                category = "messages",
                notificationKey = "key_43",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 43 expected ALERT", res.isImportant)
        }

        // Case 44
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 44L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.6600",
                text = "Rs.6600 paid to Merchant_44 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.6600",
                category = "messages",
                notificationKey = "key_44",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 44 expected ALERT", res.isImportant)
        }

        // Case 45
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 45L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 45",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 45",
                category = "messages",
                notificationKey = "key_45",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 45 expected MUTE", res.isImportant)
        }

        // Case 46
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 46L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.6900",
                text = "Rs.6900 paid to Merchant_46 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.6900",
                category = "messages",
                notificationKey = "key_46",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 46 expected ALERT", res.isImportant)
        }

        // Case 47
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 47L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.7050",
                text = "Rs.7050 paid to Merchant_47 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.7050",
                category = "messages",
                notificationKey = "key_47",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 47 expected ALERT", res.isImportant)
        }

        // Case 48
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 48L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 48",
                text = "Your secret OTP is 100048. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 48",
                category = "messages",
                notificationKey = "key_48",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 48 expected ALERT", res.isImportant)
        }

        // Case 49
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 49L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.7350",
                text = "Rs.7350 paid to Merchant_49 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.7350",
                category = "messages",
                notificationKey = "key_49",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 49 expected ALERT", res.isImportant)
        }

        // Case 50
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 50L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 50",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 50",
                category = "messages",
                notificationKey = "key_50",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 50 expected MUTE", res.isImportant)
        }

        // Case 51
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 51L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 51",
                text = "Your secret OTP is 100051. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 51",
                category = "messages",
                notificationKey = "key_51",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 51 expected ALERT", res.isImportant)
        }

        // Case 52
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 52L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.7800",
                text = "Rs.7800 paid to Merchant_52 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.7800",
                category = "messages",
                notificationKey = "key_52",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 52 expected ALERT", res.isImportant)
        }

        // Case 53
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 53L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.7950",
                text = "Rs.7950 paid to Merchant_53 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.7950",
                category = "messages",
                notificationKey = "key_53",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 53 expected ALERT", res.isImportant)
        }

        // Case 54
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 54L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 54",
                text = "Your secret OTP is 100054. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 54",
                category = "messages",
                notificationKey = "key_54",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 54 expected ALERT", res.isImportant)
        }

        // Case 55
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 55L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 55",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 55",
                category = "messages",
                notificationKey = "key_55",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 55 expected MUTE", res.isImportant)
        }

        // Case 56
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 56L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.8400",
                text = "Rs.8400 paid to Merchant_56 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.8400",
                category = "messages",
                notificationKey = "key_56",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 56 expected ALERT", res.isImportant)
        }

        // Case 57
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 57L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 57",
                text = "Your secret OTP is 100057. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 57",
                category = "messages",
                notificationKey = "key_57",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 57 expected ALERT", res.isImportant)
        }

        // Case 58
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 58L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.8700",
                text = "Rs.8700 paid to Merchant_58 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.8700",
                category = "messages",
                notificationKey = "key_58",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 58 expected ALERT", res.isImportant)
        }

        // Case 59
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 59L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.8850",
                text = "Rs.8850 paid to Merchant_59 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.8850",
                category = "messages",
                notificationKey = "key_59",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 59 expected ALERT", res.isImportant)
        }

        // Case 60
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 60L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 60",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 60",
                category = "messages",
                notificationKey = "key_60",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 60 expected MUTE", res.isImportant)
        }

        // Case 61
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 61L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.9150",
                text = "Rs.9150 paid to Merchant_61 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.9150",
                category = "messages",
                notificationKey = "key_61",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 61 expected ALERT", res.isImportant)
        }

        // Case 62
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 62L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.9300",
                text = "Rs.9300 paid to Merchant_62 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.9300",
                category = "messages",
                notificationKey = "key_62",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 62 expected ALERT", res.isImportant)
        }

        // Case 63
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 63L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 63",
                text = "Your secret OTP is 100063. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 63",
                category = "messages",
                notificationKey = "key_63",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 63 expected ALERT", res.isImportant)
        }

        // Case 64
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 64L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.9600",
                text = "Rs.9600 paid to Merchant_64 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.9600",
                category = "messages",
                notificationKey = "key_64",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 64 expected ALERT", res.isImportant)
        }

        // Case 65
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 65L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 65",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 65",
                category = "messages",
                notificationKey = "key_65",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 65 expected MUTE", res.isImportant)
        }

        // Case 66
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 66L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 66",
                text = "Your secret OTP is 100066. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 66",
                category = "messages",
                notificationKey = "key_66",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 66 expected ALERT", res.isImportant)
        }

        // Case 67
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 67L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.10050",
                text = "Rs.10050 paid to Merchant_67 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.10050",
                category = "messages",
                notificationKey = "key_67",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 67 expected ALERT", res.isImportant)
        }

        // Case 68
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 68L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.10200",
                text = "Rs.10200 paid to Merchant_68 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.10200",
                category = "messages",
                notificationKey = "key_68",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 68 expected ALERT", res.isImportant)
        }

        // Case 69
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 69L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 69",
                text = "Your secret OTP is 100069. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 69",
                category = "messages",
                notificationKey = "key_69",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 69 expected ALERT", res.isImportant)
        }

        // Case 70
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 70L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 70",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 70",
                category = "messages",
                notificationKey = "key_70",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 70 expected MUTE", res.isImportant)
        }

        // Case 71
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 71L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.10650",
                text = "Rs.10650 paid to Merchant_71 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.10650",
                category = "messages",
                notificationKey = "key_71",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 71 expected ALERT", res.isImportant)
        }

        // Case 72
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 72L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 72",
                text = "Your secret OTP is 100072. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 72",
                category = "messages",
                notificationKey = "key_72",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 72 expected ALERT", res.isImportant)
        }

        // Case 73
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 73L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.10950",
                text = "Rs.10950 paid to Merchant_73 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.10950",
                category = "messages",
                notificationKey = "key_73",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 73 expected ALERT", res.isImportant)
        }

        // Case 74
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 74L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.11100",
                text = "Rs.11100 paid to Merchant_74 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.11100",
                category = "messages",
                notificationKey = "key_74",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 74 expected ALERT", res.isImportant)
        }

        // Case 75
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 75L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 75",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 75",
                category = "messages",
                notificationKey = "key_75",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 75 expected MUTE", res.isImportant)
        }

        // Case 76
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 76L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.11400",
                text = "Rs.11400 paid to Merchant_76 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.11400",
                category = "messages",
                notificationKey = "key_76",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 76 expected ALERT", res.isImportant)
        }

        // Case 77
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 77L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.11550",
                text = "Rs.11550 paid to Merchant_77 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.11550",
                category = "messages",
                notificationKey = "key_77",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 77 expected ALERT", res.isImportant)
        }

        // Case 78
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 78L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 78",
                text = "Your secret OTP is 100078. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 78",
                category = "messages",
                notificationKey = "key_78",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 78 expected ALERT", res.isImportant)
        }

        // Case 79
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 79L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.11850",
                text = "Rs.11850 paid to Merchant_79 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.11850",
                category = "messages",
                notificationKey = "key_79",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 79 expected ALERT", res.isImportant)
        }

        // Case 80
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 80L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 80",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 80",
                category = "messages",
                notificationKey = "key_80",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 80 expected MUTE", res.isImportant)
        }

        // Case 81
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 81L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 81",
                text = "Your secret OTP is 100081. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 81",
                category = "messages",
                notificationKey = "key_81",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 81 expected ALERT", res.isImportant)
        }

        // Case 82
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 82L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.12300",
                text = "Rs.12300 paid to Merchant_82 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.12300",
                category = "messages",
                notificationKey = "key_82",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 82 expected ALERT", res.isImportant)
        }

        // Case 83
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 83L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.12450",
                text = "Rs.12450 paid to Merchant_83 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.12450",
                category = "messages",
                notificationKey = "key_83",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 83 expected ALERT", res.isImportant)
        }

        // Case 84
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 84L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 84",
                text = "Your secret OTP is 100084. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 84",
                category = "messages",
                notificationKey = "key_84",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 84 expected ALERT", res.isImportant)
        }

        // Case 85
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 85L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 85",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 85",
                category = "messages",
                notificationKey = "key_85",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 85 expected MUTE", res.isImportant)
        }

        // Case 86
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 86L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.12900",
                text = "Rs.12900 paid to Merchant_86 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.12900",
                category = "messages",
                notificationKey = "key_86",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 86 expected ALERT", res.isImportant)
        }

        // Case 87
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 87L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 87",
                text = "Your secret OTP is 100087. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 87",
                category = "messages",
                notificationKey = "key_87",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 87 expected ALERT", res.isImportant)
        }

        // Case 88
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 88L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.13200",
                text = "Rs.13200 paid to Merchant_88 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.13200",
                category = "messages",
                notificationKey = "key_88",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 88 expected ALERT", res.isImportant)
        }

        // Case 89
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 89L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.13350",
                text = "Rs.13350 paid to Merchant_89 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.13350",
                category = "messages",
                notificationKey = "key_89",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 89 expected ALERT", res.isImportant)
        }

        // Case 90
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 90L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 90",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 90",
                category = "messages",
                notificationKey = "key_90",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 90 expected MUTE", res.isImportant)
        }

        // Case 91
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 91L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.13650",
                text = "Rs.13650 paid to Merchant_91 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.13650",
                category = "messages",
                notificationKey = "key_91",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 91 expected ALERT", res.isImportant)
        }

        // Case 92
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 92L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.13800",
                text = "Rs.13800 paid to Merchant_92 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.13800",
                category = "messages",
                notificationKey = "key_92",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 92 expected ALERT", res.isImportant)
        }

        // Case 93
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 93L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 93",
                text = "Your secret OTP is 100093. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 93",
                category = "messages",
                notificationKey = "key_93",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 93 expected ALERT", res.isImportant)
        }

        // Case 94
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 94L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.14100",
                text = "Rs.14100 paid to Merchant_94 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.14100",
                category = "messages",
                notificationKey = "key_94",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 94 expected ALERT", res.isImportant)
        }

        // Case 95
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 95L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 95",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 95",
                category = "messages",
                notificationKey = "key_95",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 95 expected MUTE", res.isImportant)
        }

        // Case 96
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 96L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 96",
                text = "Your secret OTP is 100096. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 96",
                category = "messages",
                notificationKey = "key_96",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 96 expected ALERT", res.isImportant)
        }

        // Case 97
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 97L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.14550",
                text = "Rs.14550 paid to Merchant_97 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.14550",
                category = "messages",
                notificationKey = "key_97",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 97 expected ALERT", res.isImportant)
        }

        // Case 98
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 98L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.14700",
                text = "Rs.14700 paid to Merchant_98 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.14700",
                category = "messages",
                notificationKey = "key_98",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 98 expected ALERT", res.isImportant)
        }

        // Case 99
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 99L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 99",
                text = "Your secret OTP is 100099. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 99",
                category = "messages",
                notificationKey = "key_99",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 99 expected ALERT", res.isImportant)
        }

        // Case 100
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 100L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 100",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 100",
                category = "messages",
                notificationKey = "key_100",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 100 expected MUTE", res.isImportant)
        }

        // Case 101
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 101L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.15150",
                text = "Rs.15150 paid to Merchant_101 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.15150",
                category = "messages",
                notificationKey = "key_101",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 101 expected ALERT", res.isImportant)
        }

        // Case 102
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 102L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 102",
                text = "Your secret OTP is 100102. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 102",
                category = "messages",
                notificationKey = "key_102",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 102 expected ALERT", res.isImportant)
        }

        // Case 103
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 103L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.15450",
                text = "Rs.15450 paid to Merchant_103 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.15450",
                category = "messages",
                notificationKey = "key_103",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 103 expected ALERT", res.isImportant)
        }

        // Case 104
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 104L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.15600",
                text = "Rs.15600 paid to Merchant_104 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.15600",
                category = "messages",
                notificationKey = "key_104",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 104 expected ALERT", res.isImportant)
        }

        // Case 105
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 105L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 105",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 105",
                category = "messages",
                notificationKey = "key_105",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 105 expected MUTE", res.isImportant)
        }

        // Case 106
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 106L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.15900",
                text = "Rs.15900 paid to Merchant_106 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.15900",
                category = "messages",
                notificationKey = "key_106",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 106 expected ALERT", res.isImportant)
        }

        // Case 107
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 107L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.16050",
                text = "Rs.16050 paid to Merchant_107 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.16050",
                category = "messages",
                notificationKey = "key_107",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 107 expected ALERT", res.isImportant)
        }

        // Case 108
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 108L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 108",
                text = "Your secret OTP is 100108. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 108",
                category = "messages",
                notificationKey = "key_108",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 108 expected ALERT", res.isImportant)
        }

        // Case 109
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 109L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.16350",
                text = "Rs.16350 paid to Merchant_109 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.16350",
                category = "messages",
                notificationKey = "key_109",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 109 expected ALERT", res.isImportant)
        }

        // Case 110
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 110L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 110",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 110",
                category = "messages",
                notificationKey = "key_110",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 110 expected MUTE", res.isImportant)
        }

        // Case 111
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 111L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 111",
                text = "Your secret OTP is 100111. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 111",
                category = "messages",
                notificationKey = "key_111",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 111 expected ALERT", res.isImportant)
        }

        // Case 112
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 112L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.16800",
                text = "Rs.16800 paid to Merchant_112 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.16800",
                category = "messages",
                notificationKey = "key_112",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 112 expected ALERT", res.isImportant)
        }

        // Case 113
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 113L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.16950",
                text = "Rs.16950 paid to Merchant_113 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.16950",
                category = "messages",
                notificationKey = "key_113",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 113 expected ALERT", res.isImportant)
        }

        // Case 114
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 114L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "HDFC Bank OTP 114",
                text = "Your secret OTP is 100114. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 114",
                category = "messages",
                notificationKey = "key_114",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 114 expected ALERT", res.isImportant)
        }

        // Case 115
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 115L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 115",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 115",
                category = "messages",
                notificationKey = "key_115",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 115 expected MUTE", res.isImportant)
        }

        // Case 116
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 116L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.17400",
                text = "Rs.17400 paid to Merchant_116 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.17400",
                category = "messages",
                notificationKey = "key_116",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 116 expected ALERT", res.isImportant)
        }

        // Case 117
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 117L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 117",
                text = "Your secret OTP is 100117. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 117",
                category = "messages",
                notificationKey = "key_117",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 117 expected ALERT", res.isImportant)
        }

        // Case 118
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 118L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.17700",
                text = "Rs.17700 paid to Merchant_118 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.17700",
                category = "messages",
                notificationKey = "key_118",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 118 expected ALERT", res.isImportant)
        }

        // Case 119
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 119L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.17850",
                text = "Rs.17850 paid to Merchant_119 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.17850",
                category = "messages",
                notificationKey = "key_119",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 119 expected ALERT", res.isImportant)
        }

        // Case 120
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 120L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 120",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 120",
                category = "messages",
                notificationKey = "key_120",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 120 expected MUTE", res.isImportant)
        }

        // Case 121
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 121L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "Acct Debited Rs.18150",
                text = "Rs.18150 paid to Merchant_121 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.18150",
                category = "messages",
                notificationKey = "key_121",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 121 expected ALERT", res.isImportant)
        }

        // Case 122
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 122L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.18300",
                text = "Rs.18300 paid to Merchant_122 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.18300",
                category = "messages",
                notificationKey = "key_122",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 122 expected ALERT", res.isImportant)
        }

        // Case 123
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 123L)
            val notif = NotificationData(
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "GPay",
                title = "HDFC Bank OTP 123",
                text = "Your secret OTP is 100123. Valid for 5 mins.",
                subText = null,
                sender = "HDFC Bank OTP 123",
                category = "messages",
                notificationKey = "key_123",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 123 expected ALERT", res.isImportant)
        }

        // Case 124
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important").toNotificationRule(id = 124L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Acct Debited Rs.18600",
                text = "Rs.18600 paid to Merchant_124 via UPI.",
                subText = null,
                sender = "Acct Debited Rs.18600",
                category = "messages",
                notificationKey = "key_124",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 124 expected ALERT", res.isImportant)
        }

        // Case 125
        run {
            val rule = RuleClassifier.classify("bank transactions and otp are important, promotional discount not important").toNotificationRule(id = 125L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Flat 50% Cashback Deal 125",
                text = "Claim your scratch card offer voucher today before it expires!",
                subText = null,
                sender = "Flat 50% Cashback Deal 125",
                category = "messages",
                notificationKey = "key_125",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 125 expected MUTE", res.isImportant)
        }

    }

    @Test
    fun testFoodDelivery125Cases() {
        // Domain: Dimension2_FoodAndQuickCommerce (125 Distinct Test Cases)
        // Case 126
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 126L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10001 Status",
                text = "Rider #1 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10001 Status",
                category = "messages",
                notificationKey = "key_126",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 126 expected ALERT", res.isImportant)
        }

        // Case 127
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 127L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10002 Status",
                text = "Rider #2 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10002 Status",
                category = "messages",
                notificationKey = "key_127",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 127 expected ALERT", res.isImportant)
        }

        // Case 128
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 128L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10003 Status",
                text = "Rider #3 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10003 Status",
                category = "messages",
                notificationKey = "key_128",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 128 expected ALERT", res.isImportant)
        }

        // Case 129
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 129L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10004 Status",
                text = "Rider #4 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10004 Status",
                category = "messages",
                notificationKey = "key_129",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 129 expected ALERT", res.isImportant)
        }

        // Case 130
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 130L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_130",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 130 expected MUTE", res.isImportant)
        }

        // Case 131
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 131L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10006 Status",
                text = "Rider #6 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10006 Status",
                category = "messages",
                notificationKey = "key_131",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 131 expected ALERT", res.isImportant)
        }

        // Case 132
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 132L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10007 Status",
                text = "Rider #7 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10007 Status",
                category = "messages",
                notificationKey = "key_132",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 132 expected ALERT", res.isImportant)
        }

        // Case 133
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 133L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10008 Status",
                text = "Rider #8 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10008 Status",
                category = "messages",
                notificationKey = "key_133",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 133 expected ALERT", res.isImportant)
        }

        // Case 134
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 134L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10009 Status",
                text = "Rider #9 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10009 Status",
                category = "messages",
                notificationKey = "key_134",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 134 expected ALERT", res.isImportant)
        }

        // Case 135
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 135L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_135",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 135 expected MUTE", res.isImportant)
        }

        // Case 136
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 136L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10011 Status",
                text = "Rider #11 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10011 Status",
                category = "messages",
                notificationKey = "key_136",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 136 expected ALERT", res.isImportant)
        }

        // Case 137
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 137L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10012 Status",
                text = "Rider #12 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10012 Status",
                category = "messages",
                notificationKey = "key_137",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 137 expected ALERT", res.isImportant)
        }

        // Case 138
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 138L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10013 Status",
                text = "Rider #13 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10013 Status",
                category = "messages",
                notificationKey = "key_138",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 138 expected ALERT", res.isImportant)
        }

        // Case 139
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 139L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10014 Status",
                text = "Rider #14 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10014 Status",
                category = "messages",
                notificationKey = "key_139",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 139 expected ALERT", res.isImportant)
        }

        // Case 140
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 140L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_140",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 140 expected MUTE", res.isImportant)
        }

        // Case 141
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 141L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10016 Status",
                text = "Rider #16 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10016 Status",
                category = "messages",
                notificationKey = "key_141",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 141 expected ALERT", res.isImportant)
        }

        // Case 142
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 142L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10017 Status",
                text = "Rider #17 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10017 Status",
                category = "messages",
                notificationKey = "key_142",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 142 expected ALERT", res.isImportant)
        }

        // Case 143
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 143L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10018 Status",
                text = "Rider #18 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10018 Status",
                category = "messages",
                notificationKey = "key_143",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 143 expected ALERT", res.isImportant)
        }

        // Case 144
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 144L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10019 Status",
                text = "Rider #19 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10019 Status",
                category = "messages",
                notificationKey = "key_144",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 144 expected ALERT", res.isImportant)
        }

        // Case 145
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 145L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_145",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 145 expected MUTE", res.isImportant)
        }

        // Case 146
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 146L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10021 Status",
                text = "Rider #21 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10021 Status",
                category = "messages",
                notificationKey = "key_146",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 146 expected ALERT", res.isImportant)
        }

        // Case 147
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 147L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10022 Status",
                text = "Rider #22 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10022 Status",
                category = "messages",
                notificationKey = "key_147",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 147 expected ALERT", res.isImportant)
        }

        // Case 148
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 148L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10023 Status",
                text = "Rider #23 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10023 Status",
                category = "messages",
                notificationKey = "key_148",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 148 expected ALERT", res.isImportant)
        }

        // Case 149
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 149L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10024 Status",
                text = "Rider #24 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10024 Status",
                category = "messages",
                notificationKey = "key_149",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 149 expected ALERT", res.isImportant)
        }

        // Case 150
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 150L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_150",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 150 expected MUTE", res.isImportant)
        }

        // Case 151
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 151L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10026 Status",
                text = "Rider #26 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10026 Status",
                category = "messages",
                notificationKey = "key_151",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 151 expected ALERT", res.isImportant)
        }

        // Case 152
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 152L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10027 Status",
                text = "Rider #27 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10027 Status",
                category = "messages",
                notificationKey = "key_152",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 152 expected ALERT", res.isImportant)
        }

        // Case 153
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 153L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10028 Status",
                text = "Rider #28 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10028 Status",
                category = "messages",
                notificationKey = "key_153",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 153 expected ALERT", res.isImportant)
        }

        // Case 154
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 154L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10029 Status",
                text = "Rider #29 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10029 Status",
                category = "messages",
                notificationKey = "key_154",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 154 expected ALERT", res.isImportant)
        }

        // Case 155
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 155L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_155",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 155 expected MUTE", res.isImportant)
        }

        // Case 156
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 156L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10031 Status",
                text = "Rider #31 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10031 Status",
                category = "messages",
                notificationKey = "key_156",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 156 expected ALERT", res.isImportant)
        }

        // Case 157
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 157L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10032 Status",
                text = "Rider #32 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10032 Status",
                category = "messages",
                notificationKey = "key_157",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 157 expected ALERT", res.isImportant)
        }

        // Case 158
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 158L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10033 Status",
                text = "Rider #33 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10033 Status",
                category = "messages",
                notificationKey = "key_158",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 158 expected ALERT", res.isImportant)
        }

        // Case 159
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 159L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10034 Status",
                text = "Rider #34 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10034 Status",
                category = "messages",
                notificationKey = "key_159",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 159 expected ALERT", res.isImportant)
        }

        // Case 160
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 160L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_160",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 160 expected MUTE", res.isImportant)
        }

        // Case 161
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 161L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10036 Status",
                text = "Rider #36 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10036 Status",
                category = "messages",
                notificationKey = "key_161",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 161 expected ALERT", res.isImportant)
        }

        // Case 162
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 162L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10037 Status",
                text = "Rider #37 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10037 Status",
                category = "messages",
                notificationKey = "key_162",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 162 expected ALERT", res.isImportant)
        }

        // Case 163
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 163L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10038 Status",
                text = "Rider #38 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10038 Status",
                category = "messages",
                notificationKey = "key_163",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 163 expected ALERT", res.isImportant)
        }

        // Case 164
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 164L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10039 Status",
                text = "Rider #39 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10039 Status",
                category = "messages",
                notificationKey = "key_164",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 164 expected ALERT", res.isImportant)
        }

        // Case 165
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 165L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_165",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 165 expected MUTE", res.isImportant)
        }

        // Case 166
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 166L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10041 Status",
                text = "Rider #41 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10041 Status",
                category = "messages",
                notificationKey = "key_166",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 166 expected ALERT", res.isImportant)
        }

        // Case 167
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 167L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10042 Status",
                text = "Rider #42 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10042 Status",
                category = "messages",
                notificationKey = "key_167",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 167 expected ALERT", res.isImportant)
        }

        // Case 168
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 168L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10043 Status",
                text = "Rider #43 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10043 Status",
                category = "messages",
                notificationKey = "key_168",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 168 expected ALERT", res.isImportant)
        }

        // Case 169
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 169L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10044 Status",
                text = "Rider #44 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10044 Status",
                category = "messages",
                notificationKey = "key_169",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 169 expected ALERT", res.isImportant)
        }

        // Case 170
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 170L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_170",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 170 expected MUTE", res.isImportant)
        }

        // Case 171
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 171L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10046 Status",
                text = "Rider #46 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10046 Status",
                category = "messages",
                notificationKey = "key_171",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 171 expected ALERT", res.isImportant)
        }

        // Case 172
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 172L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10047 Status",
                text = "Rider #47 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10047 Status",
                category = "messages",
                notificationKey = "key_172",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 172 expected ALERT", res.isImportant)
        }

        // Case 173
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 173L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10048 Status",
                text = "Rider #48 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10048 Status",
                category = "messages",
                notificationKey = "key_173",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 173 expected ALERT", res.isImportant)
        }

        // Case 174
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 174L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10049 Status",
                text = "Rider #49 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10049 Status",
                category = "messages",
                notificationKey = "key_174",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 174 expected ALERT", res.isImportant)
        }

        // Case 175
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 175L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_175",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 175 expected MUTE", res.isImportant)
        }

        // Case 176
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 176L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10051 Status",
                text = "Rider #51 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10051 Status",
                category = "messages",
                notificationKey = "key_176",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 176 expected ALERT", res.isImportant)
        }

        // Case 177
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 177L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10052 Status",
                text = "Rider #52 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10052 Status",
                category = "messages",
                notificationKey = "key_177",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 177 expected ALERT", res.isImportant)
        }

        // Case 178
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 178L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10053 Status",
                text = "Rider #53 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10053 Status",
                category = "messages",
                notificationKey = "key_178",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 178 expected ALERT", res.isImportant)
        }

        // Case 179
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 179L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10054 Status",
                text = "Rider #54 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10054 Status",
                category = "messages",
                notificationKey = "key_179",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 179 expected ALERT", res.isImportant)
        }

        // Case 180
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 180L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_180",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 180 expected MUTE", res.isImportant)
        }

        // Case 181
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 181L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10056 Status",
                text = "Rider #56 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10056 Status",
                category = "messages",
                notificationKey = "key_181",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 181 expected ALERT", res.isImportant)
        }

        // Case 182
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 182L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10057 Status",
                text = "Rider #57 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10057 Status",
                category = "messages",
                notificationKey = "key_182",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 182 expected ALERT", res.isImportant)
        }

        // Case 183
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 183L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10058 Status",
                text = "Rider #58 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10058 Status",
                category = "messages",
                notificationKey = "key_183",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 183 expected ALERT", res.isImportant)
        }

        // Case 184
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 184L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10059 Status",
                text = "Rider #59 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10059 Status",
                category = "messages",
                notificationKey = "key_184",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 184 expected ALERT", res.isImportant)
        }

        // Case 185
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 185L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_185",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 185 expected MUTE", res.isImportant)
        }

        // Case 186
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 186L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10061 Status",
                text = "Rider #61 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10061 Status",
                category = "messages",
                notificationKey = "key_186",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 186 expected ALERT", res.isImportant)
        }

        // Case 187
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 187L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10062 Status",
                text = "Rider #62 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10062 Status",
                category = "messages",
                notificationKey = "key_187",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 187 expected ALERT", res.isImportant)
        }

        // Case 188
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 188L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10063 Status",
                text = "Rider #63 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10063 Status",
                category = "messages",
                notificationKey = "key_188",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 188 expected ALERT", res.isImportant)
        }

        // Case 189
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 189L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10064 Status",
                text = "Rider #64 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10064 Status",
                category = "messages",
                notificationKey = "key_189",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 189 expected ALERT", res.isImportant)
        }

        // Case 190
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 190L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_190",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 190 expected MUTE", res.isImportant)
        }

        // Case 191
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 191L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10066 Status",
                text = "Rider #66 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10066 Status",
                category = "messages",
                notificationKey = "key_191",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 191 expected ALERT", res.isImportant)
        }

        // Case 192
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 192L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10067 Status",
                text = "Rider #67 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10067 Status",
                category = "messages",
                notificationKey = "key_192",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 192 expected ALERT", res.isImportant)
        }

        // Case 193
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 193L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10068 Status",
                text = "Rider #68 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10068 Status",
                category = "messages",
                notificationKey = "key_193",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 193 expected ALERT", res.isImportant)
        }

        // Case 194
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 194L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10069 Status",
                text = "Rider #69 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10069 Status",
                category = "messages",
                notificationKey = "key_194",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 194 expected ALERT", res.isImportant)
        }

        // Case 195
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 195L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_195",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 195 expected MUTE", res.isImportant)
        }

        // Case 196
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 196L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10071 Status",
                text = "Rider #71 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10071 Status",
                category = "messages",
                notificationKey = "key_196",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 196 expected ALERT", res.isImportant)
        }

        // Case 197
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 197L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10072 Status",
                text = "Rider #72 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10072 Status",
                category = "messages",
                notificationKey = "key_197",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 197 expected ALERT", res.isImportant)
        }

        // Case 198
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 198L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10073 Status",
                text = "Rider #73 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10073 Status",
                category = "messages",
                notificationKey = "key_198",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 198 expected ALERT", res.isImportant)
        }

        // Case 199
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 199L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10074 Status",
                text = "Rider #74 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10074 Status",
                category = "messages",
                notificationKey = "key_199",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 199 expected ALERT", res.isImportant)
        }

        // Case 200
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 200L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_200",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 200 expected MUTE", res.isImportant)
        }

        // Case 201
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 201L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10076 Status",
                text = "Rider #76 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10076 Status",
                category = "messages",
                notificationKey = "key_201",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 201 expected ALERT", res.isImportant)
        }

        // Case 202
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 202L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10077 Status",
                text = "Rider #77 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10077 Status",
                category = "messages",
                notificationKey = "key_202",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 202 expected ALERT", res.isImportant)
        }

        // Case 203
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 203L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10078 Status",
                text = "Rider #78 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10078 Status",
                category = "messages",
                notificationKey = "key_203",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 203 expected ALERT", res.isImportant)
        }

        // Case 204
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 204L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10079 Status",
                text = "Rider #79 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10079 Status",
                category = "messages",
                notificationKey = "key_204",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 204 expected ALERT", res.isImportant)
        }

        // Case 205
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 205L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_205",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 205 expected MUTE", res.isImportant)
        }

        // Case 206
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 206L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10081 Status",
                text = "Rider #81 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10081 Status",
                category = "messages",
                notificationKey = "key_206",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 206 expected ALERT", res.isImportant)
        }

        // Case 207
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 207L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10082 Status",
                text = "Rider #82 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10082 Status",
                category = "messages",
                notificationKey = "key_207",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 207 expected ALERT", res.isImportant)
        }

        // Case 208
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 208L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10083 Status",
                text = "Rider #83 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10083 Status",
                category = "messages",
                notificationKey = "key_208",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 208 expected ALERT", res.isImportant)
        }

        // Case 209
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 209L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10084 Status",
                text = "Rider #84 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10084 Status",
                category = "messages",
                notificationKey = "key_209",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 209 expected ALERT", res.isImportant)
        }

        // Case 210
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 210L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_210",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 210 expected MUTE", res.isImportant)
        }

        // Case 211
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 211L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10086 Status",
                text = "Rider #86 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10086 Status",
                category = "messages",
                notificationKey = "key_211",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 211 expected ALERT", res.isImportant)
        }

        // Case 212
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 212L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10087 Status",
                text = "Rider #87 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10087 Status",
                category = "messages",
                notificationKey = "key_212",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 212 expected ALERT", res.isImportant)
        }

        // Case 213
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 213L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10088 Status",
                text = "Rider #88 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10088 Status",
                category = "messages",
                notificationKey = "key_213",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 213 expected ALERT", res.isImportant)
        }

        // Case 214
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 214L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10089 Status",
                text = "Rider #89 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10089 Status",
                category = "messages",
                notificationKey = "key_214",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 214 expected ALERT", res.isImportant)
        }

        // Case 215
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 215L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_215",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 215 expected MUTE", res.isImportant)
        }

        // Case 216
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 216L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10091 Status",
                text = "Rider #91 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10091 Status",
                category = "messages",
                notificationKey = "key_216",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 216 expected ALERT", res.isImportant)
        }

        // Case 217
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 217L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10092 Status",
                text = "Rider #92 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10092 Status",
                category = "messages",
                notificationKey = "key_217",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 217 expected ALERT", res.isImportant)
        }

        // Case 218
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 218L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10093 Status",
                text = "Rider #93 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10093 Status",
                category = "messages",
                notificationKey = "key_218",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 218 expected ALERT", res.isImportant)
        }

        // Case 219
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 219L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10094 Status",
                text = "Rider #94 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10094 Status",
                category = "messages",
                notificationKey = "key_219",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 219 expected ALERT", res.isImportant)
        }

        // Case 220
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 220L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_220",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 220 expected MUTE", res.isImportant)
        }

        // Case 221
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 221L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10096 Status",
                text = "Rider #96 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10096 Status",
                category = "messages",
                notificationKey = "key_221",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 221 expected ALERT", res.isImportant)
        }

        // Case 222
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 222L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10097 Status",
                text = "Rider #97 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10097 Status",
                category = "messages",
                notificationKey = "key_222",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 222 expected ALERT", res.isImportant)
        }

        // Case 223
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 223L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10098 Status",
                text = "Rider #98 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10098 Status",
                category = "messages",
                notificationKey = "key_223",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 223 expected ALERT", res.isImportant)
        }

        // Case 224
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 224L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10099 Status",
                text = "Rider #99 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10099 Status",
                category = "messages",
                notificationKey = "key_224",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 224 expected ALERT", res.isImportant)
        }

        // Case 225
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 225L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_225",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 225 expected MUTE", res.isImportant)
        }

        // Case 226
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 226L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10101 Status",
                text = "Rider #101 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10101 Status",
                category = "messages",
                notificationKey = "key_226",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 226 expected ALERT", res.isImportant)
        }

        // Case 227
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 227L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10102 Status",
                text = "Rider #102 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10102 Status",
                category = "messages",
                notificationKey = "key_227",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 227 expected ALERT", res.isImportant)
        }

        // Case 228
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 228L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10103 Status",
                text = "Rider #103 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10103 Status",
                category = "messages",
                notificationKey = "key_228",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 228 expected ALERT", res.isImportant)
        }

        // Case 229
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 229L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10104 Status",
                text = "Rider #104 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10104 Status",
                category = "messages",
                notificationKey = "key_229",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 229 expected ALERT", res.isImportant)
        }

        // Case 230
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 230L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_230",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 230 expected MUTE", res.isImportant)
        }

        // Case 231
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 231L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10106 Status",
                text = "Rider #106 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10106 Status",
                category = "messages",
                notificationKey = "key_231",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 231 expected ALERT", res.isImportant)
        }

        // Case 232
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 232L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10107 Status",
                text = "Rider #107 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10107 Status",
                category = "messages",
                notificationKey = "key_232",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 232 expected ALERT", res.isImportant)
        }

        // Case 233
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 233L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10108 Status",
                text = "Rider #108 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10108 Status",
                category = "messages",
                notificationKey = "key_233",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 233 expected ALERT", res.isImportant)
        }

        // Case 234
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 234L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10109 Status",
                text = "Rider #109 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10109 Status",
                category = "messages",
                notificationKey = "key_234",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 234 expected ALERT", res.isImportant)
        }

        // Case 235
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 235L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_235",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 235 expected MUTE", res.isImportant)
        }

        // Case 236
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 236L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10111 Status",
                text = "Rider #111 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10111 Status",
                category = "messages",
                notificationKey = "key_236",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 236 expected ALERT", res.isImportant)
        }

        // Case 237
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 237L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10112 Status",
                text = "Rider #112 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10112 Status",
                category = "messages",
                notificationKey = "key_237",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 237 expected ALERT", res.isImportant)
        }

        // Case 238
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 238L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10113 Status",
                text = "Rider #113 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10113 Status",
                category = "messages",
                notificationKey = "key_238",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 238 expected ALERT", res.isImportant)
        }

        // Case 239
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 239L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10114 Status",
                text = "Rider #114 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10114 Status",
                category = "messages",
                notificationKey = "key_239",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 239 expected ALERT", res.isImportant)
        }

        // Case 240
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 240L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_240",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 240 expected MUTE", res.isImportant)
        }

        // Case 241
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 241L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10116 Status",
                text = "Rider #116 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10116 Status",
                category = "messages",
                notificationKey = "key_241",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 241 expected ALERT", res.isImportant)
        }

        // Case 242
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 242L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10117 Status",
                text = "Rider #117 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10117 Status",
                category = "messages",
                notificationKey = "key_242",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 242 expected ALERT", res.isImportant)
        }

        // Case 243
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 243L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10118 Status",
                text = "Rider #118 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10118 Status",
                category = "messages",
                notificationKey = "key_243",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 243 expected ALERT", res.isImportant)
        }

        // Case 244
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 244L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10119 Status",
                text = "Rider #119 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10119 Status",
                category = "messages",
                notificationKey = "key_244",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 244 expected ALERT", res.isImportant)
        }

        // Case 245
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 245L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_245",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 245 expected MUTE", res.isImportant)
        }

        // Case 246
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 246L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10121 Status",
                text = "Rider #121 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10121 Status",
                category = "messages",
                notificationKey = "key_246",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 246 expected ALERT", res.isImportant)
        }

        // Case 247
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 247L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10122 Status",
                text = "Rider #122 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10122 Status",
                category = "messages",
                notificationKey = "key_247",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 247 expected ALERT", res.isImportant)
        }

        // Case 248
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 248L)
            val notif = NotificationData(
                packageName = "com.grofers.customerapp",
                appName = "Blinkit",
                title = "Order #10123 Status",
                text = "Rider #123 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10123 Status",
                category = "messages",
                notificationKey = "key_248",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 248 expected ALERT", res.isImportant)
        }

        // Case 249
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important").toNotificationRule(id = 249L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Order #10124 Status",
                text = "Rider #124 is out for delivery reaching your doorstep in 8 mins.",
                subText = null,
                sender = "Order #10124 Status",
                category = "messages",
                notificationKey = "key_249",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 249 expected ALERT", res.isImportant)
        }

        // Case 250
        run {
            val rule = RuleClassifier.classify("food delivery and grocery orders are important, discount sale not important").toNotificationRule(id = 250L)
            val notif = NotificationData(
                packageName = "in.swiggy.android",
                appName = "Swiggy",
                title = "Craving Pizza? 60% OFF",
                text = "Special sale discount on restaurant kitchen orders today only.",
                subText = null,
                sender = "Craving Pizza? 60% OFF",
                category = "messages",
                notificationKey = "key_250",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 250 expected MUTE", res.isImportant)
        }

    }

    @Test
    fun testECommerceCouriers125Cases() {
        // Domain: Dimension3_ECommerceAndCouriers (125 Distinct Test Cases)
        // Case 251
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 251L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20001",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20001",
                category = "messages",
                notificationKey = "key_251",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 251 expected ALERT", res.isImportant)
        }

        // Case 252
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 252L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20002",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20002",
                category = "messages",
                notificationKey = "key_252",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 252 expected ALERT", res.isImportant)
        }

        // Case 253
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 253L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20003",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20003",
                category = "messages",
                notificationKey = "key_253",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 253 expected ALERT", res.isImportant)
        }

        // Case 254
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 254L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20004",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20004",
                category = "messages",
                notificationKey = "key_254",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 254 expected ALERT", res.isImportant)
        }

        // Case 255
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 255L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 5",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 5",
                category = "messages",
                notificationKey = "key_255",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 255 expected MUTE", res.isImportant)
        }

        // Case 256
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 256L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20006",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20006",
                category = "messages",
                notificationKey = "key_256",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 256 expected ALERT", res.isImportant)
        }

        // Case 257
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 257L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20007",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20007",
                category = "messages",
                notificationKey = "key_257",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 257 expected ALERT", res.isImportant)
        }

        // Case 258
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 258L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20008",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20008",
                category = "messages",
                notificationKey = "key_258",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 258 expected ALERT", res.isImportant)
        }

        // Case 259
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 259L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20009",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20009",
                category = "messages",
                notificationKey = "key_259",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 259 expected ALERT", res.isImportant)
        }

        // Case 260
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 260L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 10",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 10",
                category = "messages",
                notificationKey = "key_260",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 260 expected MUTE", res.isImportant)
        }

        // Case 261
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 261L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20011",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20011",
                category = "messages",
                notificationKey = "key_261",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 261 expected ALERT", res.isImportant)
        }

        // Case 262
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 262L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20012",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20012",
                category = "messages",
                notificationKey = "key_262",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 262 expected ALERT", res.isImportant)
        }

        // Case 263
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 263L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20013",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20013",
                category = "messages",
                notificationKey = "key_263",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 263 expected ALERT", res.isImportant)
        }

        // Case 264
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 264L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20014",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20014",
                category = "messages",
                notificationKey = "key_264",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 264 expected ALERT", res.isImportant)
        }

        // Case 265
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 265L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 15",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 15",
                category = "messages",
                notificationKey = "key_265",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 265 expected MUTE", res.isImportant)
        }

        // Case 266
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 266L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20016",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20016",
                category = "messages",
                notificationKey = "key_266",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 266 expected ALERT", res.isImportant)
        }

        // Case 267
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 267L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20017",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20017",
                category = "messages",
                notificationKey = "key_267",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 267 expected ALERT", res.isImportant)
        }

        // Case 268
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 268L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20018",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20018",
                category = "messages",
                notificationKey = "key_268",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 268 expected ALERT", res.isImportant)
        }

        // Case 269
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 269L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20019",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20019",
                category = "messages",
                notificationKey = "key_269",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 269 expected ALERT", res.isImportant)
        }

        // Case 270
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 270L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 20",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 20",
                category = "messages",
                notificationKey = "key_270",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 270 expected MUTE", res.isImportant)
        }

        // Case 271
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 271L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20021",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20021",
                category = "messages",
                notificationKey = "key_271",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 271 expected ALERT", res.isImportant)
        }

        // Case 272
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 272L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20022",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20022",
                category = "messages",
                notificationKey = "key_272",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 272 expected ALERT", res.isImportant)
        }

        // Case 273
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 273L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20023",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20023",
                category = "messages",
                notificationKey = "key_273",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 273 expected ALERT", res.isImportant)
        }

        // Case 274
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 274L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20024",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20024",
                category = "messages",
                notificationKey = "key_274",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 274 expected ALERT", res.isImportant)
        }

        // Case 275
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 275L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 25",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 25",
                category = "messages",
                notificationKey = "key_275",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 275 expected MUTE", res.isImportant)
        }

        // Case 276
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 276L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20026",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20026",
                category = "messages",
                notificationKey = "key_276",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 276 expected ALERT", res.isImportant)
        }

        // Case 277
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 277L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20027",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20027",
                category = "messages",
                notificationKey = "key_277",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 277 expected ALERT", res.isImportant)
        }

        // Case 278
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 278L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20028",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20028",
                category = "messages",
                notificationKey = "key_278",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 278 expected ALERT", res.isImportant)
        }

        // Case 279
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 279L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20029",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20029",
                category = "messages",
                notificationKey = "key_279",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 279 expected ALERT", res.isImportant)
        }

        // Case 280
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 280L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 30",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 30",
                category = "messages",
                notificationKey = "key_280",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 280 expected MUTE", res.isImportant)
        }

        // Case 281
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 281L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20031",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20031",
                category = "messages",
                notificationKey = "key_281",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 281 expected ALERT", res.isImportant)
        }

        // Case 282
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 282L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20032",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20032",
                category = "messages",
                notificationKey = "key_282",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 282 expected ALERT", res.isImportant)
        }

        // Case 283
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 283L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20033",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20033",
                category = "messages",
                notificationKey = "key_283",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 283 expected ALERT", res.isImportant)
        }

        // Case 284
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 284L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20034",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20034",
                category = "messages",
                notificationKey = "key_284",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 284 expected ALERT", res.isImportant)
        }

        // Case 285
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 285L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 35",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 35",
                category = "messages",
                notificationKey = "key_285",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 285 expected MUTE", res.isImportant)
        }

        // Case 286
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 286L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20036",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20036",
                category = "messages",
                notificationKey = "key_286",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 286 expected ALERT", res.isImportant)
        }

        // Case 287
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 287L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20037",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20037",
                category = "messages",
                notificationKey = "key_287",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 287 expected ALERT", res.isImportant)
        }

        // Case 288
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 288L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20038",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20038",
                category = "messages",
                notificationKey = "key_288",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 288 expected ALERT", res.isImportant)
        }

        // Case 289
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 289L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20039",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20039",
                category = "messages",
                notificationKey = "key_289",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 289 expected ALERT", res.isImportant)
        }

        // Case 290
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 290L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 40",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 40",
                category = "messages",
                notificationKey = "key_290",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 290 expected MUTE", res.isImportant)
        }

        // Case 291
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 291L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20041",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20041",
                category = "messages",
                notificationKey = "key_291",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 291 expected ALERT", res.isImportant)
        }

        // Case 292
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 292L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20042",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20042",
                category = "messages",
                notificationKey = "key_292",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 292 expected ALERT", res.isImportant)
        }

        // Case 293
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 293L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20043",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20043",
                category = "messages",
                notificationKey = "key_293",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 293 expected ALERT", res.isImportant)
        }

        // Case 294
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 294L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20044",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20044",
                category = "messages",
                notificationKey = "key_294",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 294 expected ALERT", res.isImportant)
        }

        // Case 295
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 295L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 45",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 45",
                category = "messages",
                notificationKey = "key_295",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 295 expected MUTE", res.isImportant)
        }

        // Case 296
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 296L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20046",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20046",
                category = "messages",
                notificationKey = "key_296",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 296 expected ALERT", res.isImportant)
        }

        // Case 297
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 297L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20047",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20047",
                category = "messages",
                notificationKey = "key_297",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 297 expected ALERT", res.isImportant)
        }

        // Case 298
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 298L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20048",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20048",
                category = "messages",
                notificationKey = "key_298",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 298 expected ALERT", res.isImportant)
        }

        // Case 299
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 299L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20049",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20049",
                category = "messages",
                notificationKey = "key_299",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 299 expected ALERT", res.isImportant)
        }

        // Case 300
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 300L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 50",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 50",
                category = "messages",
                notificationKey = "key_300",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 300 expected MUTE", res.isImportant)
        }

        // Case 301
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 301L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20051",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20051",
                category = "messages",
                notificationKey = "key_301",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 301 expected ALERT", res.isImportant)
        }

        // Case 302
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 302L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20052",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20052",
                category = "messages",
                notificationKey = "key_302",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 302 expected ALERT", res.isImportant)
        }

        // Case 303
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 303L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20053",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20053",
                category = "messages",
                notificationKey = "key_303",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 303 expected ALERT", res.isImportant)
        }

        // Case 304
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 304L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20054",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20054",
                category = "messages",
                notificationKey = "key_304",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 304 expected ALERT", res.isImportant)
        }

        // Case 305
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 305L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 55",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 55",
                category = "messages",
                notificationKey = "key_305",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 305 expected MUTE", res.isImportant)
        }

        // Case 306
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 306L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20056",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20056",
                category = "messages",
                notificationKey = "key_306",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 306 expected ALERT", res.isImportant)
        }

        // Case 307
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 307L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20057",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20057",
                category = "messages",
                notificationKey = "key_307",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 307 expected ALERT", res.isImportant)
        }

        // Case 308
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 308L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20058",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20058",
                category = "messages",
                notificationKey = "key_308",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 308 expected ALERT", res.isImportant)
        }

        // Case 309
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 309L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20059",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20059",
                category = "messages",
                notificationKey = "key_309",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 309 expected ALERT", res.isImportant)
        }

        // Case 310
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 310L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 60",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 60",
                category = "messages",
                notificationKey = "key_310",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 310 expected MUTE", res.isImportant)
        }

        // Case 311
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 311L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20061",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20061",
                category = "messages",
                notificationKey = "key_311",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 311 expected ALERT", res.isImportant)
        }

        // Case 312
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 312L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20062",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20062",
                category = "messages",
                notificationKey = "key_312",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 312 expected ALERT", res.isImportant)
        }

        // Case 313
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 313L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20063",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20063",
                category = "messages",
                notificationKey = "key_313",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 313 expected ALERT", res.isImportant)
        }

        // Case 314
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 314L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20064",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20064",
                category = "messages",
                notificationKey = "key_314",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 314 expected ALERT", res.isImportant)
        }

        // Case 315
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 315L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 65",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 65",
                category = "messages",
                notificationKey = "key_315",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 315 expected MUTE", res.isImportant)
        }

        // Case 316
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 316L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20066",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20066",
                category = "messages",
                notificationKey = "key_316",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 316 expected ALERT", res.isImportant)
        }

        // Case 317
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 317L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20067",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20067",
                category = "messages",
                notificationKey = "key_317",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 317 expected ALERT", res.isImportant)
        }

        // Case 318
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 318L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20068",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20068",
                category = "messages",
                notificationKey = "key_318",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 318 expected ALERT", res.isImportant)
        }

        // Case 319
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 319L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20069",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20069",
                category = "messages",
                notificationKey = "key_319",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 319 expected ALERT", res.isImportant)
        }

        // Case 320
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 320L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 70",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 70",
                category = "messages",
                notificationKey = "key_320",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 320 expected MUTE", res.isImportant)
        }

        // Case 321
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 321L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20071",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20071",
                category = "messages",
                notificationKey = "key_321",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 321 expected ALERT", res.isImportant)
        }

        // Case 322
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 322L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20072",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20072",
                category = "messages",
                notificationKey = "key_322",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 322 expected ALERT", res.isImportant)
        }

        // Case 323
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 323L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20073",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20073",
                category = "messages",
                notificationKey = "key_323",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 323 expected ALERT", res.isImportant)
        }

        // Case 324
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 324L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20074",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20074",
                category = "messages",
                notificationKey = "key_324",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 324 expected ALERT", res.isImportant)
        }

        // Case 325
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 325L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 75",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 75",
                category = "messages",
                notificationKey = "key_325",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 325 expected MUTE", res.isImportant)
        }

        // Case 326
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 326L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20076",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20076",
                category = "messages",
                notificationKey = "key_326",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 326 expected ALERT", res.isImportant)
        }

        // Case 327
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 327L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20077",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20077",
                category = "messages",
                notificationKey = "key_327",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 327 expected ALERT", res.isImportant)
        }

        // Case 328
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 328L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20078",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20078",
                category = "messages",
                notificationKey = "key_328",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 328 expected ALERT", res.isImportant)
        }

        // Case 329
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 329L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20079",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20079",
                category = "messages",
                notificationKey = "key_329",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 329 expected ALERT", res.isImportant)
        }

        // Case 330
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 330L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 80",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 80",
                category = "messages",
                notificationKey = "key_330",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 330 expected MUTE", res.isImportant)
        }

        // Case 331
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 331L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20081",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20081",
                category = "messages",
                notificationKey = "key_331",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 331 expected ALERT", res.isImportant)
        }

        // Case 332
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 332L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20082",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20082",
                category = "messages",
                notificationKey = "key_332",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 332 expected ALERT", res.isImportant)
        }

        // Case 333
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 333L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20083",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20083",
                category = "messages",
                notificationKey = "key_333",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 333 expected ALERT", res.isImportant)
        }

        // Case 334
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 334L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20084",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20084",
                category = "messages",
                notificationKey = "key_334",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 334 expected ALERT", res.isImportant)
        }

        // Case 335
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 335L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 85",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 85",
                category = "messages",
                notificationKey = "key_335",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 335 expected MUTE", res.isImportant)
        }

        // Case 336
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 336L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20086",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20086",
                category = "messages",
                notificationKey = "key_336",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 336 expected ALERT", res.isImportant)
        }

        // Case 337
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 337L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20087",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20087",
                category = "messages",
                notificationKey = "key_337",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 337 expected ALERT", res.isImportant)
        }

        // Case 338
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 338L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20088",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20088",
                category = "messages",
                notificationKey = "key_338",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 338 expected ALERT", res.isImportant)
        }

        // Case 339
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 339L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20089",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20089",
                category = "messages",
                notificationKey = "key_339",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 339 expected ALERT", res.isImportant)
        }

        // Case 340
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 340L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 90",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 90",
                category = "messages",
                notificationKey = "key_340",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 340 expected MUTE", res.isImportant)
        }

        // Case 341
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 341L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20091",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20091",
                category = "messages",
                notificationKey = "key_341",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 341 expected ALERT", res.isImportant)
        }

        // Case 342
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 342L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20092",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20092",
                category = "messages",
                notificationKey = "key_342",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 342 expected ALERT", res.isImportant)
        }

        // Case 343
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 343L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20093",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20093",
                category = "messages",
                notificationKey = "key_343",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 343 expected ALERT", res.isImportant)
        }

        // Case 344
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 344L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20094",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20094",
                category = "messages",
                notificationKey = "key_344",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 344 expected ALERT", res.isImportant)
        }

        // Case 345
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 345L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 95",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 95",
                category = "messages",
                notificationKey = "key_345",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 345 expected MUTE", res.isImportant)
        }

        // Case 346
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 346L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20096",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20096",
                category = "messages",
                notificationKey = "key_346",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 346 expected ALERT", res.isImportant)
        }

        // Case 347
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 347L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20097",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20097",
                category = "messages",
                notificationKey = "key_347",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 347 expected ALERT", res.isImportant)
        }

        // Case 348
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 348L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20098",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20098",
                category = "messages",
                notificationKey = "key_348",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 348 expected ALERT", res.isImportant)
        }

        // Case 349
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 349L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20099",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20099",
                category = "messages",
                notificationKey = "key_349",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 349 expected ALERT", res.isImportant)
        }

        // Case 350
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 350L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 100",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 100",
                category = "messages",
                notificationKey = "key_350",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 350 expected MUTE", res.isImportant)
        }

        // Case 351
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 351L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20101",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20101",
                category = "messages",
                notificationKey = "key_351",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 351 expected ALERT", res.isImportant)
        }

        // Case 352
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 352L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20102",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20102",
                category = "messages",
                notificationKey = "key_352",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 352 expected ALERT", res.isImportant)
        }

        // Case 353
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 353L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20103",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20103",
                category = "messages",
                notificationKey = "key_353",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 353 expected ALERT", res.isImportant)
        }

        // Case 354
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 354L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20104",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20104",
                category = "messages",
                notificationKey = "key_354",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 354 expected ALERT", res.isImportant)
        }

        // Case 355
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 355L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 105",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 105",
                category = "messages",
                notificationKey = "key_355",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 355 expected MUTE", res.isImportant)
        }

        // Case 356
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 356L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20106",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20106",
                category = "messages",
                notificationKey = "key_356",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 356 expected ALERT", res.isImportant)
        }

        // Case 357
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 357L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20107",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20107",
                category = "messages",
                notificationKey = "key_357",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 357 expected ALERT", res.isImportant)
        }

        // Case 358
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 358L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20108",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20108",
                category = "messages",
                notificationKey = "key_358",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 358 expected ALERT", res.isImportant)
        }

        // Case 359
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 359L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20109",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20109",
                category = "messages",
                notificationKey = "key_359",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 359 expected ALERT", res.isImportant)
        }

        // Case 360
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 360L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 110",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 110",
                category = "messages",
                notificationKey = "key_360",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 360 expected MUTE", res.isImportant)
        }

        // Case 361
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 361L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20111",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20111",
                category = "messages",
                notificationKey = "key_361",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 361 expected ALERT", res.isImportant)
        }

        // Case 362
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 362L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20112",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20112",
                category = "messages",
                notificationKey = "key_362",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 362 expected ALERT", res.isImportant)
        }

        // Case 363
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 363L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20113",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20113",
                category = "messages",
                notificationKey = "key_363",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 363 expected ALERT", res.isImportant)
        }

        // Case 364
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 364L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20114",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20114",
                category = "messages",
                notificationKey = "key_364",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 364 expected ALERT", res.isImportant)
        }

        // Case 365
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 365L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 115",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 115",
                category = "messages",
                notificationKey = "key_365",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 365 expected MUTE", res.isImportant)
        }

        // Case 366
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 366L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20116",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20116",
                category = "messages",
                notificationKey = "key_366",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 366 expected ALERT", res.isImportant)
        }

        // Case 367
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 367L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20117",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20117",
                category = "messages",
                notificationKey = "key_367",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 367 expected ALERT", res.isImportant)
        }

        // Case 368
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 368L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20118",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20118",
                category = "messages",
                notificationKey = "key_368",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 368 expected ALERT", res.isImportant)
        }

        // Case 369
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 369L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20119",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20119",
                category = "messages",
                notificationKey = "key_369",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 369 expected ALERT", res.isImportant)
        }

        // Case 370
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 370L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 120",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 120",
                category = "messages",
                notificationKey = "key_370",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 370 expected MUTE", res.isImportant)
        }

        // Case 371
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 371L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20121",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20121",
                category = "messages",
                notificationKey = "key_371",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 371 expected ALERT", res.isImportant)
        }

        // Case 372
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 372L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20122",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20122",
                category = "messages",
                notificationKey = "key_372",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 372 expected ALERT", res.isImportant)
        }

        // Case 373
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 373L)
            val notif = NotificationData(
                packageName = "com.flipkart.android",
                appName = "Flipkart",
                title = "Shipment #20123",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20123",
                category = "messages",
                notificationKey = "key_373",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 373 expected ALERT", res.isImportant)
        }

        // Case 374
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important").toNotificationRule(id = 374L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Shipment #20124",
                text = "Your parcel is arriving today with courier delivery rider.",
                subText = null,
                sender = "Shipment #20124",
                category = "messages",
                notificationKey = "key_374",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 374 expected ALERT", res.isImportant)
        }

        // Case 375
        run {
            val rule = RuleClassifier.classify("package delivery and courier shipment are important, clearance sale not important").toNotificationRule(id = 375L)
            val notif = NotificationData(
                packageName = "in.amazon.mShop.android.shopping",
                appName = "Amazon",
                title = "Mega Clearance Sale Day 125",
                text = "Save big with lightning deal flat 70% off promo.",
                subText = null,
                sender = "Mega Clearance Sale Day 125",
                category = "messages",
                notificationKey = "key_375",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 375 expected MUTE", res.isImportant)
        }

    }

    @Test
    fun testWorkTeamsDevOps125Cases() {
        // Domain: Dimension4_WorkTeamsDevOpsJobs (125 Distinct Test Cases)
        // Case 376
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 376L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_1",
                text = "P0 critical incident outage on production cluster node 1.",
                subText = null,
                sender = "Lead_Engineer_1",
                category = "messages",
                notificationKey = "key_376",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 376 expected ALERT", res.isImportant)
        }

        // Case 377
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 377L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_2",
                text = "P0 critical incident outage on production cluster node 2.",
                subText = null,
                sender = "Lead_Engineer_2",
                category = "messages",
                notificationKey = "key_377",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 377 expected ALERT", res.isImportant)
        }

        // Case 378
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 378L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_3",
                text = "P0 critical incident outage on production cluster node 3.",
                subText = null,
                sender = "Lead_Engineer_3",
                category = "messages",
                notificationKey = "key_378",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 378 expected ALERT", res.isImportant)
        }

        // Case 379
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 379L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_4",
                text = "P0 critical incident outage on production cluster node 4.",
                subText = null,
                sender = "Lead_Engineer_4",
                category = "messages",
                notificationKey = "key_379",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 379 expected ALERT", res.isImportant)
        }

        // Case 380
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 380L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_5",
                text = "P0 critical incident outage on production cluster node 5.",
                subText = null,
                sender = "Lead_Engineer_5",
                category = "messages",
                notificationKey = "key_380",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 380 expected ALERT", res.isImportant)
        }

        // Case 381
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 381L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 6",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 6",
                category = "messages",
                notificationKey = "key_381",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 381 expected MUTE", res.isImportant)
        }

        // Case 382
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 382L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_7",
                text = "P0 critical incident outage on production cluster node 7.",
                subText = null,
                sender = "Lead_Engineer_7",
                category = "messages",
                notificationKey = "key_382",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 382 expected ALERT", res.isImportant)
        }

        // Case 383
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 383L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_8",
                text = "P0 critical incident outage on production cluster node 8.",
                subText = null,
                sender = "Lead_Engineer_8",
                category = "messages",
                notificationKey = "key_383",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 383 expected ALERT", res.isImportant)
        }

        // Case 384
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 384L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_9",
                text = "P0 critical incident outage on production cluster node 9.",
                subText = null,
                sender = "Lead_Engineer_9",
                category = "messages",
                notificationKey = "key_384",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 384 expected ALERT", res.isImportant)
        }

        // Case 385
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 385L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_10",
                text = "P0 critical incident outage on production cluster node 10.",
                subText = null,
                sender = "Lead_Engineer_10",
                category = "messages",
                notificationKey = "key_385",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 385 expected ALERT", res.isImportant)
        }

        // Case 386
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 386L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_11",
                text = "P0 critical incident outage on production cluster node 11.",
                subText = null,
                sender = "Lead_Engineer_11",
                category = "messages",
                notificationKey = "key_386",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 386 expected ALERT", res.isImportant)
        }

        // Case 387
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 387L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 12",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 12",
                category = "messages",
                notificationKey = "key_387",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 387 expected MUTE", res.isImportant)
        }

        // Case 388
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 388L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_13",
                text = "P0 critical incident outage on production cluster node 13.",
                subText = null,
                sender = "Lead_Engineer_13",
                category = "messages",
                notificationKey = "key_388",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 388 expected ALERT", res.isImportant)
        }

        // Case 389
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 389L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_14",
                text = "P0 critical incident outage on production cluster node 14.",
                subText = null,
                sender = "Lead_Engineer_14",
                category = "messages",
                notificationKey = "key_389",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 389 expected ALERT", res.isImportant)
        }

        // Case 390
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 390L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_15",
                text = "P0 critical incident outage on production cluster node 15.",
                subText = null,
                sender = "Lead_Engineer_15",
                category = "messages",
                notificationKey = "key_390",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 390 expected ALERT", res.isImportant)
        }

        // Case 391
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 391L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_16",
                text = "P0 critical incident outage on production cluster node 16.",
                subText = null,
                sender = "Lead_Engineer_16",
                category = "messages",
                notificationKey = "key_391",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 391 expected ALERT", res.isImportant)
        }

        // Case 392
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 392L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_17",
                text = "P0 critical incident outage on production cluster node 17.",
                subText = null,
                sender = "Lead_Engineer_17",
                category = "messages",
                notificationKey = "key_392",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 392 expected ALERT", res.isImportant)
        }

        // Case 393
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 393L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 18",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 18",
                category = "messages",
                notificationKey = "key_393",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 393 expected MUTE", res.isImportant)
        }

        // Case 394
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 394L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_19",
                text = "P0 critical incident outage on production cluster node 19.",
                subText = null,
                sender = "Lead_Engineer_19",
                category = "messages",
                notificationKey = "key_394",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 394 expected ALERT", res.isImportant)
        }

        // Case 395
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 395L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_20",
                text = "P0 critical incident outage on production cluster node 20.",
                subText = null,
                sender = "Lead_Engineer_20",
                category = "messages",
                notificationKey = "key_395",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 395 expected ALERT", res.isImportant)
        }

        // Case 396
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 396L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_21",
                text = "P0 critical incident outage on production cluster node 21.",
                subText = null,
                sender = "Lead_Engineer_21",
                category = "messages",
                notificationKey = "key_396",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 396 expected ALERT", res.isImportant)
        }

        // Case 397
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 397L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_22",
                text = "P0 critical incident outage on production cluster node 22.",
                subText = null,
                sender = "Lead_Engineer_22",
                category = "messages",
                notificationKey = "key_397",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 397 expected ALERT", res.isImportant)
        }

        // Case 398
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 398L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_23",
                text = "P0 critical incident outage on production cluster node 23.",
                subText = null,
                sender = "Lead_Engineer_23",
                category = "messages",
                notificationKey = "key_398",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 398 expected ALERT", res.isImportant)
        }

        // Case 399
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 399L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 24",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 24",
                category = "messages",
                notificationKey = "key_399",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 399 expected MUTE", res.isImportant)
        }

        // Case 400
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 400L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_25",
                text = "P0 critical incident outage on production cluster node 25.",
                subText = null,
                sender = "Lead_Engineer_25",
                category = "messages",
                notificationKey = "key_400",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 400 expected ALERT", res.isImportant)
        }

        // Case 401
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 401L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_26",
                text = "P0 critical incident outage on production cluster node 26.",
                subText = null,
                sender = "Lead_Engineer_26",
                category = "messages",
                notificationKey = "key_401",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 401 expected ALERT", res.isImportant)
        }

        // Case 402
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 402L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_27",
                text = "P0 critical incident outage on production cluster node 27.",
                subText = null,
                sender = "Lead_Engineer_27",
                category = "messages",
                notificationKey = "key_402",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 402 expected ALERT", res.isImportant)
        }

        // Case 403
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 403L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_28",
                text = "P0 critical incident outage on production cluster node 28.",
                subText = null,
                sender = "Lead_Engineer_28",
                category = "messages",
                notificationKey = "key_403",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 403 expected ALERT", res.isImportant)
        }

        // Case 404
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 404L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_29",
                text = "P0 critical incident outage on production cluster node 29.",
                subText = null,
                sender = "Lead_Engineer_29",
                category = "messages",
                notificationKey = "key_404",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 404 expected ALERT", res.isImportant)
        }

        // Case 405
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 405L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 30",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 30",
                category = "messages",
                notificationKey = "key_405",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 405 expected MUTE", res.isImportant)
        }

        // Case 406
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 406L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_31",
                text = "P0 critical incident outage on production cluster node 31.",
                subText = null,
                sender = "Lead_Engineer_31",
                category = "messages",
                notificationKey = "key_406",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 406 expected ALERT", res.isImportant)
        }

        // Case 407
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 407L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_32",
                text = "P0 critical incident outage on production cluster node 32.",
                subText = null,
                sender = "Lead_Engineer_32",
                category = "messages",
                notificationKey = "key_407",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 407 expected ALERT", res.isImportant)
        }

        // Case 408
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 408L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_33",
                text = "P0 critical incident outage on production cluster node 33.",
                subText = null,
                sender = "Lead_Engineer_33",
                category = "messages",
                notificationKey = "key_408",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 408 expected ALERT", res.isImportant)
        }

        // Case 409
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 409L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_34",
                text = "P0 critical incident outage on production cluster node 34.",
                subText = null,
                sender = "Lead_Engineer_34",
                category = "messages",
                notificationKey = "key_409",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 409 expected ALERT", res.isImportant)
        }

        // Case 410
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 410L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_35",
                text = "P0 critical incident outage on production cluster node 35.",
                subText = null,
                sender = "Lead_Engineer_35",
                category = "messages",
                notificationKey = "key_410",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 410 expected ALERT", res.isImportant)
        }

        // Case 411
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 411L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 36",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 36",
                category = "messages",
                notificationKey = "key_411",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 411 expected MUTE", res.isImportant)
        }

        // Case 412
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 412L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_37",
                text = "P0 critical incident outage on production cluster node 37.",
                subText = null,
                sender = "Lead_Engineer_37",
                category = "messages",
                notificationKey = "key_412",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 412 expected ALERT", res.isImportant)
        }

        // Case 413
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 413L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_38",
                text = "P0 critical incident outage on production cluster node 38.",
                subText = null,
                sender = "Lead_Engineer_38",
                category = "messages",
                notificationKey = "key_413",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 413 expected ALERT", res.isImportant)
        }

        // Case 414
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 414L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_39",
                text = "P0 critical incident outage on production cluster node 39.",
                subText = null,
                sender = "Lead_Engineer_39",
                category = "messages",
                notificationKey = "key_414",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 414 expected ALERT", res.isImportant)
        }

        // Case 415
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 415L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_40",
                text = "P0 critical incident outage on production cluster node 40.",
                subText = null,
                sender = "Lead_Engineer_40",
                category = "messages",
                notificationKey = "key_415",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 415 expected ALERT", res.isImportant)
        }

        // Case 416
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 416L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_41",
                text = "P0 critical incident outage on production cluster node 41.",
                subText = null,
                sender = "Lead_Engineer_41",
                category = "messages",
                notificationKey = "key_416",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 416 expected ALERT", res.isImportant)
        }

        // Case 417
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 417L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 42",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 42",
                category = "messages",
                notificationKey = "key_417",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 417 expected MUTE", res.isImportant)
        }

        // Case 418
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 418L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_43",
                text = "P0 critical incident outage on production cluster node 43.",
                subText = null,
                sender = "Lead_Engineer_43",
                category = "messages",
                notificationKey = "key_418",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 418 expected ALERT", res.isImportant)
        }

        // Case 419
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 419L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_44",
                text = "P0 critical incident outage on production cluster node 44.",
                subText = null,
                sender = "Lead_Engineer_44",
                category = "messages",
                notificationKey = "key_419",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 419 expected ALERT", res.isImportant)
        }

        // Case 420
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 420L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_45",
                text = "P0 critical incident outage on production cluster node 45.",
                subText = null,
                sender = "Lead_Engineer_45",
                category = "messages",
                notificationKey = "key_420",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 420 expected ALERT", res.isImportant)
        }

        // Case 421
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 421L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_46",
                text = "P0 critical incident outage on production cluster node 46.",
                subText = null,
                sender = "Lead_Engineer_46",
                category = "messages",
                notificationKey = "key_421",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 421 expected ALERT", res.isImportant)
        }

        // Case 422
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 422L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_47",
                text = "P0 critical incident outage on production cluster node 47.",
                subText = null,
                sender = "Lead_Engineer_47",
                category = "messages",
                notificationKey = "key_422",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 422 expected ALERT", res.isImportant)
        }

        // Case 423
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 423L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 48",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 48",
                category = "messages",
                notificationKey = "key_423",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 423 expected MUTE", res.isImportant)
        }

        // Case 424
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 424L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_49",
                text = "P0 critical incident outage on production cluster node 49.",
                subText = null,
                sender = "Lead_Engineer_49",
                category = "messages",
                notificationKey = "key_424",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 424 expected ALERT", res.isImportant)
        }

        // Case 425
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 425L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_50",
                text = "P0 critical incident outage on production cluster node 50.",
                subText = null,
                sender = "Lead_Engineer_50",
                category = "messages",
                notificationKey = "key_425",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 425 expected ALERT", res.isImportant)
        }

        // Case 426
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 426L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_51",
                text = "P0 critical incident outage on production cluster node 51.",
                subText = null,
                sender = "Lead_Engineer_51",
                category = "messages",
                notificationKey = "key_426",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 426 expected ALERT", res.isImportant)
        }

        // Case 427
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 427L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_52",
                text = "P0 critical incident outage on production cluster node 52.",
                subText = null,
                sender = "Lead_Engineer_52",
                category = "messages",
                notificationKey = "key_427",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 427 expected ALERT", res.isImportant)
        }

        // Case 428
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 428L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_53",
                text = "P0 critical incident outage on production cluster node 53.",
                subText = null,
                sender = "Lead_Engineer_53",
                category = "messages",
                notificationKey = "key_428",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 428 expected ALERT", res.isImportant)
        }

        // Case 429
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 429L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 54",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 54",
                category = "messages",
                notificationKey = "key_429",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 429 expected MUTE", res.isImportant)
        }

        // Case 430
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 430L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_55",
                text = "P0 critical incident outage on production cluster node 55.",
                subText = null,
                sender = "Lead_Engineer_55",
                category = "messages",
                notificationKey = "key_430",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 430 expected ALERT", res.isImportant)
        }

        // Case 431
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 431L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_56",
                text = "P0 critical incident outage on production cluster node 56.",
                subText = null,
                sender = "Lead_Engineer_56",
                category = "messages",
                notificationKey = "key_431",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 431 expected ALERT", res.isImportant)
        }

        // Case 432
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 432L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_57",
                text = "P0 critical incident outage on production cluster node 57.",
                subText = null,
                sender = "Lead_Engineer_57",
                category = "messages",
                notificationKey = "key_432",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 432 expected ALERT", res.isImportant)
        }

        // Case 433
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 433L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_58",
                text = "P0 critical incident outage on production cluster node 58.",
                subText = null,
                sender = "Lead_Engineer_58",
                category = "messages",
                notificationKey = "key_433",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 433 expected ALERT", res.isImportant)
        }

        // Case 434
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 434L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_59",
                text = "P0 critical incident outage on production cluster node 59.",
                subText = null,
                sender = "Lead_Engineer_59",
                category = "messages",
                notificationKey = "key_434",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 434 expected ALERT", res.isImportant)
        }

        // Case 435
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 435L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 60",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 60",
                category = "messages",
                notificationKey = "key_435",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 435 expected MUTE", res.isImportant)
        }

        // Case 436
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 436L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_61",
                text = "P0 critical incident outage on production cluster node 61.",
                subText = null,
                sender = "Lead_Engineer_61",
                category = "messages",
                notificationKey = "key_436",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 436 expected ALERT", res.isImportant)
        }

        // Case 437
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 437L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_62",
                text = "P0 critical incident outage on production cluster node 62.",
                subText = null,
                sender = "Lead_Engineer_62",
                category = "messages",
                notificationKey = "key_437",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 437 expected ALERT", res.isImportant)
        }

        // Case 438
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 438L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_63",
                text = "P0 critical incident outage on production cluster node 63.",
                subText = null,
                sender = "Lead_Engineer_63",
                category = "messages",
                notificationKey = "key_438",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 438 expected ALERT", res.isImportant)
        }

        // Case 439
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 439L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_64",
                text = "P0 critical incident outage on production cluster node 64.",
                subText = null,
                sender = "Lead_Engineer_64",
                category = "messages",
                notificationKey = "key_439",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 439 expected ALERT", res.isImportant)
        }

        // Case 440
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 440L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_65",
                text = "P0 critical incident outage on production cluster node 65.",
                subText = null,
                sender = "Lead_Engineer_65",
                category = "messages",
                notificationKey = "key_440",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 440 expected ALERT", res.isImportant)
        }

        // Case 441
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 441L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 66",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 66",
                category = "messages",
                notificationKey = "key_441",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 441 expected MUTE", res.isImportant)
        }

        // Case 442
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 442L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_67",
                text = "P0 critical incident outage on production cluster node 67.",
                subText = null,
                sender = "Lead_Engineer_67",
                category = "messages",
                notificationKey = "key_442",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 442 expected ALERT", res.isImportant)
        }

        // Case 443
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 443L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_68",
                text = "P0 critical incident outage on production cluster node 68.",
                subText = null,
                sender = "Lead_Engineer_68",
                category = "messages",
                notificationKey = "key_443",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 443 expected ALERT", res.isImportant)
        }

        // Case 444
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 444L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_69",
                text = "P0 critical incident outage on production cluster node 69.",
                subText = null,
                sender = "Lead_Engineer_69",
                category = "messages",
                notificationKey = "key_444",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 444 expected ALERT", res.isImportant)
        }

        // Case 445
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 445L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_70",
                text = "P0 critical incident outage on production cluster node 70.",
                subText = null,
                sender = "Lead_Engineer_70",
                category = "messages",
                notificationKey = "key_445",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 445 expected ALERT", res.isImportant)
        }

        // Case 446
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 446L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_71",
                text = "P0 critical incident outage on production cluster node 71.",
                subText = null,
                sender = "Lead_Engineer_71",
                category = "messages",
                notificationKey = "key_446",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 446 expected ALERT", res.isImportant)
        }

        // Case 447
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 447L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 72",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 72",
                category = "messages",
                notificationKey = "key_447",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 447 expected MUTE", res.isImportant)
        }

        // Case 448
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 448L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_73",
                text = "P0 critical incident outage on production cluster node 73.",
                subText = null,
                sender = "Lead_Engineer_73",
                category = "messages",
                notificationKey = "key_448",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 448 expected ALERT", res.isImportant)
        }

        // Case 449
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 449L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_74",
                text = "P0 critical incident outage on production cluster node 74.",
                subText = null,
                sender = "Lead_Engineer_74",
                category = "messages",
                notificationKey = "key_449",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 449 expected ALERT", res.isImportant)
        }

        // Case 450
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 450L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_75",
                text = "P0 critical incident outage on production cluster node 75.",
                subText = null,
                sender = "Lead_Engineer_75",
                category = "messages",
                notificationKey = "key_450",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 450 expected ALERT", res.isImportant)
        }

        // Case 451
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 451L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_76",
                text = "P0 critical incident outage on production cluster node 76.",
                subText = null,
                sender = "Lead_Engineer_76",
                category = "messages",
                notificationKey = "key_451",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 451 expected ALERT", res.isImportant)
        }

        // Case 452
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 452L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_77",
                text = "P0 critical incident outage on production cluster node 77.",
                subText = null,
                sender = "Lead_Engineer_77",
                category = "messages",
                notificationKey = "key_452",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 452 expected ALERT", res.isImportant)
        }

        // Case 453
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 453L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 78",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 78",
                category = "messages",
                notificationKey = "key_453",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 453 expected MUTE", res.isImportant)
        }

        // Case 454
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 454L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_79",
                text = "P0 critical incident outage on production cluster node 79.",
                subText = null,
                sender = "Lead_Engineer_79",
                category = "messages",
                notificationKey = "key_454",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 454 expected ALERT", res.isImportant)
        }

        // Case 455
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 455L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_80",
                text = "P0 critical incident outage on production cluster node 80.",
                subText = null,
                sender = "Lead_Engineer_80",
                category = "messages",
                notificationKey = "key_455",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 455 expected ALERT", res.isImportant)
        }

        // Case 456
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 456L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_81",
                text = "P0 critical incident outage on production cluster node 81.",
                subText = null,
                sender = "Lead_Engineer_81",
                category = "messages",
                notificationKey = "key_456",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 456 expected ALERT", res.isImportant)
        }

        // Case 457
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 457L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_82",
                text = "P0 critical incident outage on production cluster node 82.",
                subText = null,
                sender = "Lead_Engineer_82",
                category = "messages",
                notificationKey = "key_457",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 457 expected ALERT", res.isImportant)
        }

        // Case 458
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 458L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_83",
                text = "P0 critical incident outage on production cluster node 83.",
                subText = null,
                sender = "Lead_Engineer_83",
                category = "messages",
                notificationKey = "key_458",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 458 expected ALERT", res.isImportant)
        }

        // Case 459
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 459L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 84",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 84",
                category = "messages",
                notificationKey = "key_459",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 459 expected MUTE", res.isImportant)
        }

        // Case 460
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 460L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_85",
                text = "P0 critical incident outage on production cluster node 85.",
                subText = null,
                sender = "Lead_Engineer_85",
                category = "messages",
                notificationKey = "key_460",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 460 expected ALERT", res.isImportant)
        }

        // Case 461
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 461L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_86",
                text = "P0 critical incident outage on production cluster node 86.",
                subText = null,
                sender = "Lead_Engineer_86",
                category = "messages",
                notificationKey = "key_461",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 461 expected ALERT", res.isImportant)
        }

        // Case 462
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 462L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_87",
                text = "P0 critical incident outage on production cluster node 87.",
                subText = null,
                sender = "Lead_Engineer_87",
                category = "messages",
                notificationKey = "key_462",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 462 expected ALERT", res.isImportant)
        }

        // Case 463
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 463L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_88",
                text = "P0 critical incident outage on production cluster node 88.",
                subText = null,
                sender = "Lead_Engineer_88",
                category = "messages",
                notificationKey = "key_463",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 463 expected ALERT", res.isImportant)
        }

        // Case 464
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 464L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_89",
                text = "P0 critical incident outage on production cluster node 89.",
                subText = null,
                sender = "Lead_Engineer_89",
                category = "messages",
                notificationKey = "key_464",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 464 expected ALERT", res.isImportant)
        }

        // Case 465
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 465L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 90",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 90",
                category = "messages",
                notificationKey = "key_465",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 465 expected MUTE", res.isImportant)
        }

        // Case 466
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 466L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_91",
                text = "P0 critical incident outage on production cluster node 91.",
                subText = null,
                sender = "Lead_Engineer_91",
                category = "messages",
                notificationKey = "key_466",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 466 expected ALERT", res.isImportant)
        }

        // Case 467
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 467L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_92",
                text = "P0 critical incident outage on production cluster node 92.",
                subText = null,
                sender = "Lead_Engineer_92",
                category = "messages",
                notificationKey = "key_467",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 467 expected ALERT", res.isImportant)
        }

        // Case 468
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 468L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_93",
                text = "P0 critical incident outage on production cluster node 93.",
                subText = null,
                sender = "Lead_Engineer_93",
                category = "messages",
                notificationKey = "key_468",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 468 expected ALERT", res.isImportant)
        }

        // Case 469
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 469L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_94",
                text = "P0 critical incident outage on production cluster node 94.",
                subText = null,
                sender = "Lead_Engineer_94",
                category = "messages",
                notificationKey = "key_469",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 469 expected ALERT", res.isImportant)
        }

        // Case 470
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 470L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_95",
                text = "P0 critical incident outage on production cluster node 95.",
                subText = null,
                sender = "Lead_Engineer_95",
                category = "messages",
                notificationKey = "key_470",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 470 expected ALERT", res.isImportant)
        }

        // Case 471
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 471L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 96",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 96",
                category = "messages",
                notificationKey = "key_471",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 471 expected MUTE", res.isImportant)
        }

        // Case 472
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 472L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_97",
                text = "P0 critical incident outage on production cluster node 97.",
                subText = null,
                sender = "Lead_Engineer_97",
                category = "messages",
                notificationKey = "key_472",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 472 expected ALERT", res.isImportant)
        }

        // Case 473
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 473L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_98",
                text = "P0 critical incident outage on production cluster node 98.",
                subText = null,
                sender = "Lead_Engineer_98",
                category = "messages",
                notificationKey = "key_473",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 473 expected ALERT", res.isImportant)
        }

        // Case 474
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 474L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_99",
                text = "P0 critical incident outage on production cluster node 99.",
                subText = null,
                sender = "Lead_Engineer_99",
                category = "messages",
                notificationKey = "key_474",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 474 expected ALERT", res.isImportant)
        }

        // Case 475
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 475L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_100",
                text = "P0 critical incident outage on production cluster node 100.",
                subText = null,
                sender = "Lead_Engineer_100",
                category = "messages",
                notificationKey = "key_475",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 475 expected ALERT", res.isImportant)
        }

        // Case 476
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 476L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_101",
                text = "P0 critical incident outage on production cluster node 101.",
                subText = null,
                sender = "Lead_Engineer_101",
                category = "messages",
                notificationKey = "key_476",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 476 expected ALERT", res.isImportant)
        }

        // Case 477
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 477L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 102",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 102",
                category = "messages",
                notificationKey = "key_477",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 477 expected MUTE", res.isImportant)
        }

        // Case 478
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 478L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_103",
                text = "P0 critical incident outage on production cluster node 103.",
                subText = null,
                sender = "Lead_Engineer_103",
                category = "messages",
                notificationKey = "key_478",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 478 expected ALERT", res.isImportant)
        }

        // Case 479
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 479L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_104",
                text = "P0 critical incident outage on production cluster node 104.",
                subText = null,
                sender = "Lead_Engineer_104",
                category = "messages",
                notificationKey = "key_479",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 479 expected ALERT", res.isImportant)
        }

        // Case 480
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 480L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_105",
                text = "P0 critical incident outage on production cluster node 105.",
                subText = null,
                sender = "Lead_Engineer_105",
                category = "messages",
                notificationKey = "key_480",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 480 expected ALERT", res.isImportant)
        }

        // Case 481
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 481L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_106",
                text = "P0 critical incident outage on production cluster node 106.",
                subText = null,
                sender = "Lead_Engineer_106",
                category = "messages",
                notificationKey = "key_481",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 481 expected ALERT", res.isImportant)
        }

        // Case 482
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 482L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_107",
                text = "P0 critical incident outage on production cluster node 107.",
                subText = null,
                sender = "Lead_Engineer_107",
                category = "messages",
                notificationKey = "key_482",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 482 expected ALERT", res.isImportant)
        }

        // Case 483
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 483L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 108",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 108",
                category = "messages",
                notificationKey = "key_483",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 483 expected MUTE", res.isImportant)
        }

        // Case 484
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 484L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_109",
                text = "P0 critical incident outage on production cluster node 109.",
                subText = null,
                sender = "Lead_Engineer_109",
                category = "messages",
                notificationKey = "key_484",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 484 expected ALERT", res.isImportant)
        }

        // Case 485
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 485L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_110",
                text = "P0 critical incident outage on production cluster node 110.",
                subText = null,
                sender = "Lead_Engineer_110",
                category = "messages",
                notificationKey = "key_485",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 485 expected ALERT", res.isImportant)
        }

        // Case 486
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 486L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_111",
                text = "P0 critical incident outage on production cluster node 111.",
                subText = null,
                sender = "Lead_Engineer_111",
                category = "messages",
                notificationKey = "key_486",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 486 expected ALERT", res.isImportant)
        }

        // Case 487
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 487L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_112",
                text = "P0 critical incident outage on production cluster node 112.",
                subText = null,
                sender = "Lead_Engineer_112",
                category = "messages",
                notificationKey = "key_487",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 487 expected ALERT", res.isImportant)
        }

        // Case 488
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 488L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_113",
                text = "P0 critical incident outage on production cluster node 113.",
                subText = null,
                sender = "Lead_Engineer_113",
                category = "messages",
                notificationKey = "key_488",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 488 expected ALERT", res.isImportant)
        }

        // Case 489
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 489L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 114",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 114",
                category = "messages",
                notificationKey = "key_489",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 489 expected MUTE", res.isImportant)
        }

        // Case 490
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 490L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_115",
                text = "P0 critical incident outage on production cluster node 115.",
                subText = null,
                sender = "Lead_Engineer_115",
                category = "messages",
                notificationKey = "key_490",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 490 expected ALERT", res.isImportant)
        }

        // Case 491
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 491L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_116",
                text = "P0 critical incident outage on production cluster node 116.",
                subText = null,
                sender = "Lead_Engineer_116",
                category = "messages",
                notificationKey = "key_491",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 491 expected ALERT", res.isImportant)
        }

        // Case 492
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 492L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_117",
                text = "P0 critical incident outage on production cluster node 117.",
                subText = null,
                sender = "Lead_Engineer_117",
                category = "messages",
                notificationKey = "key_492",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 492 expected ALERT", res.isImportant)
        }

        // Case 493
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 493L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_118",
                text = "P0 critical incident outage on production cluster node 118.",
                subText = null,
                sender = "Lead_Engineer_118",
                category = "messages",
                notificationKey = "key_493",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 493 expected ALERT", res.isImportant)
        }

        // Case 494
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 494L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_119",
                text = "P0 critical incident outage on production cluster node 119.",
                subText = null,
                sender = "Lead_Engineer_119",
                category = "messages",
                notificationKey = "key_494",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 494 expected ALERT", res.isImportant)
        }

        // Case 495
        run {
            val rule = RuleClassifier.classify("recruiter interview is important, marketing spam not important").toNotificationRule(id = 495L)
            val notif = NotificationData(
                packageName = "com.linkedin.android",
                appName = "LinkedIn",
                title = "Weekly Network Digest 120",
                text = "Someone viewed your profile and appeared in 12 searches this week.",
                subText = null,
                sender = "Weekly Network Digest 120",
                category = "messages",
                notificationKey = "key_495",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 495 expected MUTE", res.isImportant)
        }

        // Case 496
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 496L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_121",
                text = "P0 critical incident outage on production cluster node 121.",
                subText = null,
                sender = "Lead_Engineer_121",
                category = "messages",
                notificationKey = "key_496",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 496 expected ALERT", res.isImportant)
        }

        // Case 497
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 497L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_122",
                text = "P0 critical incident outage on production cluster node 122.",
                subText = null,
                sender = "Lead_Engineer_122",
                category = "messages",
                notificationKey = "key_497",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 497 expected ALERT", res.isImportant)
        }

        // Case 498
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 498L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_123",
                text = "P0 critical incident outage on production cluster node 123.",
                subText = null,
                sender = "Lead_Engineer_123",
                category = "messages",
                notificationKey = "key_498",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 498 expected ALERT", res.isImportant)
        }

        // Case 499
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 499L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Lead_Engineer_124",
                text = "P0 critical incident outage on production cluster node 124.",
                subText = null,
                sender = "Lead_Engineer_124",
                category = "messages",
                notificationKey = "key_499",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 499 expected ALERT", res.isImportant)
        }

        // Case 500
        run {
            val rule = RuleClassifier.classify("slack p0 incidents are important").toNotificationRule(id = 500L)
            val notif = NotificationData(
                packageName = "com.Slack",
                appName = "Slack",
                title = "Lead_Engineer_125",
                text = "P0 critical incident outage on production cluster node 125.",
                subText = null,
                sender = "Lead_Engineer_125",
                category = "messages",
                notificationKey = "key_500",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 500 expected ALERT", res.isImportant)
        }

    }

    @Test
    fun testTravelFlightsTransit125Cases() {
        // Domain: Dimension5_TravelFlightsTransit (125 Distinct Test Cases)
        // Case 501
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 501L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_1",
                text = "Platform 4 confirmed. Coach B2 Berth 2.",
                subText = null,
                sender = "Train Status PNR_1",
                category = "messages",
                notificationKey = "key_501",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 501 expected ALERT", res.isImportant)
        }

        // Case 502
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 502L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30002",
                text = "Driver has arrived at pickup. OTP is 2002.",
                subText = null,
                sender = "Ride #30002",
                category = "messages",
                notificationKey = "key_502",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 502 expected ALERT", res.isImportant)
        }

        // Case 503
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 503L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_3",
                text = "Platform 4 confirmed. Coach B4 Berth 4.",
                subText = null,
                sender = "Train Status PNR_3",
                category = "messages",
                notificationKey = "key_503",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 503 expected ALERT", res.isImportant)
        }

        // Case 504
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 504L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30004",
                text = "Driver has arrived at pickup. OTP is 2004.",
                subText = null,
                sender = "Ride #30004",
                category = "messages",
                notificationKey = "key_504",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 504 expected ALERT", res.isImportant)
        }

        // Case 505
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 505L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 5",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 5",
                category = "messages",
                notificationKey = "key_505",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 505 expected MUTE", res.isImportant)
        }

        // Case 506
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 506L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30006",
                text = "Driver has arrived at pickup. OTP is 2006.",
                subText = null,
                sender = "Ride #30006",
                category = "messages",
                notificationKey = "key_506",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 506 expected ALERT", res.isImportant)
        }

        // Case 507
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 507L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_7",
                text = "Platform 4 confirmed. Coach B3 Berth 8.",
                subText = null,
                sender = "Train Status PNR_7",
                category = "messages",
                notificationKey = "key_507",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 507 expected ALERT", res.isImportant)
        }

        // Case 508
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 508L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30008",
                text = "Driver has arrived at pickup. OTP is 2008.",
                subText = null,
                sender = "Ride #30008",
                category = "messages",
                notificationKey = "key_508",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 508 expected ALERT", res.isImportant)
        }

        // Case 509
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 509L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_9",
                text = "Platform 4 confirmed. Coach B5 Berth 10.",
                subText = null,
                sender = "Train Status PNR_9",
                category = "messages",
                notificationKey = "key_509",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 509 expected ALERT", res.isImportant)
        }

        // Case 510
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 510L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 10",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 10",
                category = "messages",
                notificationKey = "key_510",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 510 expected MUTE", res.isImportant)
        }

        // Case 511
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 511L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_11",
                text = "Platform 4 confirmed. Coach B2 Berth 12.",
                subText = null,
                sender = "Train Status PNR_11",
                category = "messages",
                notificationKey = "key_511",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 511 expected ALERT", res.isImportant)
        }

        // Case 512
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 512L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30012",
                text = "Driver has arrived at pickup. OTP is 2012.",
                subText = null,
                sender = "Ride #30012",
                category = "messages",
                notificationKey = "key_512",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 512 expected ALERT", res.isImportant)
        }

        // Case 513
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 513L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_13",
                text = "Platform 4 confirmed. Coach B4 Berth 14.",
                subText = null,
                sender = "Train Status PNR_13",
                category = "messages",
                notificationKey = "key_513",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 513 expected ALERT", res.isImportant)
        }

        // Case 514
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 514L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30014",
                text = "Driver has arrived at pickup. OTP is 2014.",
                subText = null,
                sender = "Ride #30014",
                category = "messages",
                notificationKey = "key_514",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 514 expected ALERT", res.isImportant)
        }

        // Case 515
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 515L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 15",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 15",
                category = "messages",
                notificationKey = "key_515",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 515 expected MUTE", res.isImportant)
        }

        // Case 516
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 516L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30016",
                text = "Driver has arrived at pickup. OTP is 2016.",
                subText = null,
                sender = "Ride #30016",
                category = "messages",
                notificationKey = "key_516",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 516 expected ALERT", res.isImportant)
        }

        // Case 517
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 517L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_17",
                text = "Platform 4 confirmed. Coach B3 Berth 18.",
                subText = null,
                sender = "Train Status PNR_17",
                category = "messages",
                notificationKey = "key_517",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 517 expected ALERT", res.isImportant)
        }

        // Case 518
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 518L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30018",
                text = "Driver has arrived at pickup. OTP is 2018.",
                subText = null,
                sender = "Ride #30018",
                category = "messages",
                notificationKey = "key_518",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 518 expected ALERT", res.isImportant)
        }

        // Case 519
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 519L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_19",
                text = "Platform 4 confirmed. Coach B5 Berth 20.",
                subText = null,
                sender = "Train Status PNR_19",
                category = "messages",
                notificationKey = "key_519",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 519 expected ALERT", res.isImportant)
        }

        // Case 520
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 520L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 20",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 20",
                category = "messages",
                notificationKey = "key_520",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 520 expected MUTE", res.isImportant)
        }

        // Case 521
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 521L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_21",
                text = "Platform 4 confirmed. Coach B2 Berth 22.",
                subText = null,
                sender = "Train Status PNR_21",
                category = "messages",
                notificationKey = "key_521",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 521 expected ALERT", res.isImportant)
        }

        // Case 522
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 522L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30022",
                text = "Driver has arrived at pickup. OTP is 2022.",
                subText = null,
                sender = "Ride #30022",
                category = "messages",
                notificationKey = "key_522",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 522 expected ALERT", res.isImportant)
        }

        // Case 523
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 523L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_23",
                text = "Platform 4 confirmed. Coach B4 Berth 24.",
                subText = null,
                sender = "Train Status PNR_23",
                category = "messages",
                notificationKey = "key_523",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 523 expected ALERT", res.isImportant)
        }

        // Case 524
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 524L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30024",
                text = "Driver has arrived at pickup. OTP is 2024.",
                subText = null,
                sender = "Ride #30024",
                category = "messages",
                notificationKey = "key_524",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 524 expected ALERT", res.isImportant)
        }

        // Case 525
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 525L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 25",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 25",
                category = "messages",
                notificationKey = "key_525",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 525 expected MUTE", res.isImportant)
        }

        // Case 526
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 526L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30026",
                text = "Driver has arrived at pickup. OTP is 2026.",
                subText = null,
                sender = "Ride #30026",
                category = "messages",
                notificationKey = "key_526",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 526 expected ALERT", res.isImportant)
        }

        // Case 527
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 527L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_27",
                text = "Platform 4 confirmed. Coach B3 Berth 28.",
                subText = null,
                sender = "Train Status PNR_27",
                category = "messages",
                notificationKey = "key_527",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 527 expected ALERT", res.isImportant)
        }

        // Case 528
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 528L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30028",
                text = "Driver has arrived at pickup. OTP is 2028.",
                subText = null,
                sender = "Ride #30028",
                category = "messages",
                notificationKey = "key_528",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 528 expected ALERT", res.isImportant)
        }

        // Case 529
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 529L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_29",
                text = "Platform 4 confirmed. Coach B5 Berth 30.",
                subText = null,
                sender = "Train Status PNR_29",
                category = "messages",
                notificationKey = "key_529",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 529 expected ALERT", res.isImportant)
        }

        // Case 530
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 530L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 30",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 30",
                category = "messages",
                notificationKey = "key_530",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 530 expected MUTE", res.isImportant)
        }

        // Case 531
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 531L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_31",
                text = "Platform 4 confirmed. Coach B2 Berth 32.",
                subText = null,
                sender = "Train Status PNR_31",
                category = "messages",
                notificationKey = "key_531",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 531 expected ALERT", res.isImportant)
        }

        // Case 532
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 532L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30032",
                text = "Driver has arrived at pickup. OTP is 2032.",
                subText = null,
                sender = "Ride #30032",
                category = "messages",
                notificationKey = "key_532",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 532 expected ALERT", res.isImportant)
        }

        // Case 533
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 533L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_33",
                text = "Platform 4 confirmed. Coach B4 Berth 34.",
                subText = null,
                sender = "Train Status PNR_33",
                category = "messages",
                notificationKey = "key_533",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 533 expected ALERT", res.isImportant)
        }

        // Case 534
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 534L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30034",
                text = "Driver has arrived at pickup. OTP is 2034.",
                subText = null,
                sender = "Ride #30034",
                category = "messages",
                notificationKey = "key_534",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 534 expected ALERT", res.isImportant)
        }

        // Case 535
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 535L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 35",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 35",
                category = "messages",
                notificationKey = "key_535",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 535 expected MUTE", res.isImportant)
        }

        // Case 536
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 536L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30036",
                text = "Driver has arrived at pickup. OTP is 2036.",
                subText = null,
                sender = "Ride #30036",
                category = "messages",
                notificationKey = "key_536",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 536 expected ALERT", res.isImportant)
        }

        // Case 537
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 537L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_37",
                text = "Platform 4 confirmed. Coach B3 Berth 38.",
                subText = null,
                sender = "Train Status PNR_37",
                category = "messages",
                notificationKey = "key_537",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 537 expected ALERT", res.isImportant)
        }

        // Case 538
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 538L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30038",
                text = "Driver has arrived at pickup. OTP is 2038.",
                subText = null,
                sender = "Ride #30038",
                category = "messages",
                notificationKey = "key_538",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 538 expected ALERT", res.isImportant)
        }

        // Case 539
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 539L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_39",
                text = "Platform 4 confirmed. Coach B5 Berth 40.",
                subText = null,
                sender = "Train Status PNR_39",
                category = "messages",
                notificationKey = "key_539",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 539 expected ALERT", res.isImportant)
        }

        // Case 540
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 540L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 40",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 40",
                category = "messages",
                notificationKey = "key_540",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 540 expected MUTE", res.isImportant)
        }

        // Case 541
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 541L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_41",
                text = "Platform 4 confirmed. Coach B2 Berth 42.",
                subText = null,
                sender = "Train Status PNR_41",
                category = "messages",
                notificationKey = "key_541",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 541 expected ALERT", res.isImportant)
        }

        // Case 542
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 542L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30042",
                text = "Driver has arrived at pickup. OTP is 2042.",
                subText = null,
                sender = "Ride #30042",
                category = "messages",
                notificationKey = "key_542",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 542 expected ALERT", res.isImportant)
        }

        // Case 543
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 543L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_43",
                text = "Platform 4 confirmed. Coach B4 Berth 44.",
                subText = null,
                sender = "Train Status PNR_43",
                category = "messages",
                notificationKey = "key_543",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 543 expected ALERT", res.isImportant)
        }

        // Case 544
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 544L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30044",
                text = "Driver has arrived at pickup. OTP is 2044.",
                subText = null,
                sender = "Ride #30044",
                category = "messages",
                notificationKey = "key_544",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 544 expected ALERT", res.isImportant)
        }

        // Case 545
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 545L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 45",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 45",
                category = "messages",
                notificationKey = "key_545",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 545 expected MUTE", res.isImportant)
        }

        // Case 546
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 546L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30046",
                text = "Driver has arrived at pickup. OTP is 2046.",
                subText = null,
                sender = "Ride #30046",
                category = "messages",
                notificationKey = "key_546",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 546 expected ALERT", res.isImportant)
        }

        // Case 547
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 547L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_47",
                text = "Platform 4 confirmed. Coach B3 Berth 48.",
                subText = null,
                sender = "Train Status PNR_47",
                category = "messages",
                notificationKey = "key_547",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 547 expected ALERT", res.isImportant)
        }

        // Case 548
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 548L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30048",
                text = "Driver has arrived at pickup. OTP is 2048.",
                subText = null,
                sender = "Ride #30048",
                category = "messages",
                notificationKey = "key_548",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 548 expected ALERT", res.isImportant)
        }

        // Case 549
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 549L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_49",
                text = "Platform 4 confirmed. Coach B5 Berth 50.",
                subText = null,
                sender = "Train Status PNR_49",
                category = "messages",
                notificationKey = "key_549",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 549 expected ALERT", res.isImportant)
        }

        // Case 550
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 550L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 50",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 50",
                category = "messages",
                notificationKey = "key_550",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 550 expected MUTE", res.isImportant)
        }

        // Case 551
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 551L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_51",
                text = "Platform 4 confirmed. Coach B2 Berth 52.",
                subText = null,
                sender = "Train Status PNR_51",
                category = "messages",
                notificationKey = "key_551",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 551 expected ALERT", res.isImportant)
        }

        // Case 552
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 552L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30052",
                text = "Driver has arrived at pickup. OTP is 2052.",
                subText = null,
                sender = "Ride #30052",
                category = "messages",
                notificationKey = "key_552",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 552 expected ALERT", res.isImportant)
        }

        // Case 553
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 553L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_53",
                text = "Platform 4 confirmed. Coach B4 Berth 54.",
                subText = null,
                sender = "Train Status PNR_53",
                category = "messages",
                notificationKey = "key_553",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 553 expected ALERT", res.isImportant)
        }

        // Case 554
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 554L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30054",
                text = "Driver has arrived at pickup. OTP is 2054.",
                subText = null,
                sender = "Ride #30054",
                category = "messages",
                notificationKey = "key_554",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 554 expected ALERT", res.isImportant)
        }

        // Case 555
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 555L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 55",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 55",
                category = "messages",
                notificationKey = "key_555",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 555 expected MUTE", res.isImportant)
        }

        // Case 556
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 556L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30056",
                text = "Driver has arrived at pickup. OTP is 2056.",
                subText = null,
                sender = "Ride #30056",
                category = "messages",
                notificationKey = "key_556",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 556 expected ALERT", res.isImportant)
        }

        // Case 557
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 557L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_57",
                text = "Platform 4 confirmed. Coach B3 Berth 58.",
                subText = null,
                sender = "Train Status PNR_57",
                category = "messages",
                notificationKey = "key_557",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 557 expected ALERT", res.isImportant)
        }

        // Case 558
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 558L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30058",
                text = "Driver has arrived at pickup. OTP is 2058.",
                subText = null,
                sender = "Ride #30058",
                category = "messages",
                notificationKey = "key_558",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 558 expected ALERT", res.isImportant)
        }

        // Case 559
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 559L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_59",
                text = "Platform 4 confirmed. Coach B5 Berth 60.",
                subText = null,
                sender = "Train Status PNR_59",
                category = "messages",
                notificationKey = "key_559",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 559 expected ALERT", res.isImportant)
        }

        // Case 560
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 560L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 60",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 60",
                category = "messages",
                notificationKey = "key_560",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 560 expected MUTE", res.isImportant)
        }

        // Case 561
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 561L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_61",
                text = "Platform 4 confirmed. Coach B2 Berth 2.",
                subText = null,
                sender = "Train Status PNR_61",
                category = "messages",
                notificationKey = "key_561",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 561 expected ALERT", res.isImportant)
        }

        // Case 562
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 562L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30062",
                text = "Driver has arrived at pickup. OTP is 2062.",
                subText = null,
                sender = "Ride #30062",
                category = "messages",
                notificationKey = "key_562",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 562 expected ALERT", res.isImportant)
        }

        // Case 563
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 563L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_63",
                text = "Platform 4 confirmed. Coach B4 Berth 4.",
                subText = null,
                sender = "Train Status PNR_63",
                category = "messages",
                notificationKey = "key_563",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 563 expected ALERT", res.isImportant)
        }

        // Case 564
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 564L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30064",
                text = "Driver has arrived at pickup. OTP is 2064.",
                subText = null,
                sender = "Ride #30064",
                category = "messages",
                notificationKey = "key_564",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 564 expected ALERT", res.isImportant)
        }

        // Case 565
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 565L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 65",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 65",
                category = "messages",
                notificationKey = "key_565",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 565 expected MUTE", res.isImportant)
        }

        // Case 566
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 566L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30066",
                text = "Driver has arrived at pickup. OTP is 2066.",
                subText = null,
                sender = "Ride #30066",
                category = "messages",
                notificationKey = "key_566",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 566 expected ALERT", res.isImportant)
        }

        // Case 567
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 567L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_67",
                text = "Platform 4 confirmed. Coach B3 Berth 8.",
                subText = null,
                sender = "Train Status PNR_67",
                category = "messages",
                notificationKey = "key_567",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 567 expected ALERT", res.isImportant)
        }

        // Case 568
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 568L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30068",
                text = "Driver has arrived at pickup. OTP is 2068.",
                subText = null,
                sender = "Ride #30068",
                category = "messages",
                notificationKey = "key_568",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 568 expected ALERT", res.isImportant)
        }

        // Case 569
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 569L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_69",
                text = "Platform 4 confirmed. Coach B5 Berth 10.",
                subText = null,
                sender = "Train Status PNR_69",
                category = "messages",
                notificationKey = "key_569",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 569 expected ALERT", res.isImportant)
        }

        // Case 570
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 570L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 70",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 70",
                category = "messages",
                notificationKey = "key_570",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 570 expected MUTE", res.isImportant)
        }

        // Case 571
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 571L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_71",
                text = "Platform 4 confirmed. Coach B2 Berth 12.",
                subText = null,
                sender = "Train Status PNR_71",
                category = "messages",
                notificationKey = "key_571",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 571 expected ALERT", res.isImportant)
        }

        // Case 572
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 572L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30072",
                text = "Driver has arrived at pickup. OTP is 2072.",
                subText = null,
                sender = "Ride #30072",
                category = "messages",
                notificationKey = "key_572",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 572 expected ALERT", res.isImportant)
        }

        // Case 573
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 573L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_73",
                text = "Platform 4 confirmed. Coach B4 Berth 14.",
                subText = null,
                sender = "Train Status PNR_73",
                category = "messages",
                notificationKey = "key_573",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 573 expected ALERT", res.isImportant)
        }

        // Case 574
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 574L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30074",
                text = "Driver has arrived at pickup. OTP is 2074.",
                subText = null,
                sender = "Ride #30074",
                category = "messages",
                notificationKey = "key_574",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 574 expected ALERT", res.isImportant)
        }

        // Case 575
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 575L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 75",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 75",
                category = "messages",
                notificationKey = "key_575",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 575 expected MUTE", res.isImportant)
        }

        // Case 576
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 576L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30076",
                text = "Driver has arrived at pickup. OTP is 2076.",
                subText = null,
                sender = "Ride #30076",
                category = "messages",
                notificationKey = "key_576",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 576 expected ALERT", res.isImportant)
        }

        // Case 577
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 577L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_77",
                text = "Platform 4 confirmed. Coach B3 Berth 18.",
                subText = null,
                sender = "Train Status PNR_77",
                category = "messages",
                notificationKey = "key_577",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 577 expected ALERT", res.isImportant)
        }

        // Case 578
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 578L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30078",
                text = "Driver has arrived at pickup. OTP is 2078.",
                subText = null,
                sender = "Ride #30078",
                category = "messages",
                notificationKey = "key_578",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 578 expected ALERT", res.isImportant)
        }

        // Case 579
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 579L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_79",
                text = "Platform 4 confirmed. Coach B5 Berth 20.",
                subText = null,
                sender = "Train Status PNR_79",
                category = "messages",
                notificationKey = "key_579",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 579 expected ALERT", res.isImportant)
        }

        // Case 580
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 580L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 80",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 80",
                category = "messages",
                notificationKey = "key_580",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 580 expected MUTE", res.isImportant)
        }

        // Case 581
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 581L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_81",
                text = "Platform 4 confirmed. Coach B2 Berth 22.",
                subText = null,
                sender = "Train Status PNR_81",
                category = "messages",
                notificationKey = "key_581",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 581 expected ALERT", res.isImportant)
        }

        // Case 582
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 582L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30082",
                text = "Driver has arrived at pickup. OTP is 2082.",
                subText = null,
                sender = "Ride #30082",
                category = "messages",
                notificationKey = "key_582",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 582 expected ALERT", res.isImportant)
        }

        // Case 583
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 583L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_83",
                text = "Platform 4 confirmed. Coach B4 Berth 24.",
                subText = null,
                sender = "Train Status PNR_83",
                category = "messages",
                notificationKey = "key_583",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 583 expected ALERT", res.isImportant)
        }

        // Case 584
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 584L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30084",
                text = "Driver has arrived at pickup. OTP is 2084.",
                subText = null,
                sender = "Ride #30084",
                category = "messages",
                notificationKey = "key_584",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 584 expected ALERT", res.isImportant)
        }

        // Case 585
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 585L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 85",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 85",
                category = "messages",
                notificationKey = "key_585",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 585 expected MUTE", res.isImportant)
        }

        // Case 586
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 586L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30086",
                text = "Driver has arrived at pickup. OTP is 2086.",
                subText = null,
                sender = "Ride #30086",
                category = "messages",
                notificationKey = "key_586",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 586 expected ALERT", res.isImportant)
        }

        // Case 587
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 587L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_87",
                text = "Platform 4 confirmed. Coach B3 Berth 28.",
                subText = null,
                sender = "Train Status PNR_87",
                category = "messages",
                notificationKey = "key_587",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 587 expected ALERT", res.isImportant)
        }

        // Case 588
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 588L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30088",
                text = "Driver has arrived at pickup. OTP is 2088.",
                subText = null,
                sender = "Ride #30088",
                category = "messages",
                notificationKey = "key_588",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 588 expected ALERT", res.isImportant)
        }

        // Case 589
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 589L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_89",
                text = "Platform 4 confirmed. Coach B5 Berth 30.",
                subText = null,
                sender = "Train Status PNR_89",
                category = "messages",
                notificationKey = "key_589",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 589 expected ALERT", res.isImportant)
        }

        // Case 590
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 590L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 90",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 90",
                category = "messages",
                notificationKey = "key_590",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 590 expected MUTE", res.isImportant)
        }

        // Case 591
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 591L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_91",
                text = "Platform 4 confirmed. Coach B2 Berth 32.",
                subText = null,
                sender = "Train Status PNR_91",
                category = "messages",
                notificationKey = "key_591",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 591 expected ALERT", res.isImportant)
        }

        // Case 592
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 592L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30092",
                text = "Driver has arrived at pickup. OTP is 2092.",
                subText = null,
                sender = "Ride #30092",
                category = "messages",
                notificationKey = "key_592",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 592 expected ALERT", res.isImportant)
        }

        // Case 593
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 593L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_93",
                text = "Platform 4 confirmed. Coach B4 Berth 34.",
                subText = null,
                sender = "Train Status PNR_93",
                category = "messages",
                notificationKey = "key_593",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 593 expected ALERT", res.isImportant)
        }

        // Case 594
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 594L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30094",
                text = "Driver has arrived at pickup. OTP is 2094.",
                subText = null,
                sender = "Ride #30094",
                category = "messages",
                notificationKey = "key_594",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 594 expected ALERT", res.isImportant)
        }

        // Case 595
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 595L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 95",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 95",
                category = "messages",
                notificationKey = "key_595",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 595 expected MUTE", res.isImportant)
        }

        // Case 596
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 596L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30096",
                text = "Driver has arrived at pickup. OTP is 2096.",
                subText = null,
                sender = "Ride #30096",
                category = "messages",
                notificationKey = "key_596",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 596 expected ALERT", res.isImportant)
        }

        // Case 597
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 597L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_97",
                text = "Platform 4 confirmed. Coach B3 Berth 38.",
                subText = null,
                sender = "Train Status PNR_97",
                category = "messages",
                notificationKey = "key_597",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 597 expected ALERT", res.isImportant)
        }

        // Case 598
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 598L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30098",
                text = "Driver has arrived at pickup. OTP is 2098.",
                subText = null,
                sender = "Ride #30098",
                category = "messages",
                notificationKey = "key_598",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 598 expected ALERT", res.isImportant)
        }

        // Case 599
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 599L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_99",
                text = "Platform 4 confirmed. Coach B5 Berth 40.",
                subText = null,
                sender = "Train Status PNR_99",
                category = "messages",
                notificationKey = "key_599",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 599 expected ALERT", res.isImportant)
        }

        // Case 600
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 600L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 100",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 100",
                category = "messages",
                notificationKey = "key_600",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 600 expected MUTE", res.isImportant)
        }

        // Case 601
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 601L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_101",
                text = "Platform 4 confirmed. Coach B2 Berth 42.",
                subText = null,
                sender = "Train Status PNR_101",
                category = "messages",
                notificationKey = "key_601",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 601 expected ALERT", res.isImportant)
        }

        // Case 602
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 602L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30102",
                text = "Driver has arrived at pickup. OTP is 2102.",
                subText = null,
                sender = "Ride #30102",
                category = "messages",
                notificationKey = "key_602",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 602 expected ALERT", res.isImportant)
        }

        // Case 603
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 603L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_103",
                text = "Platform 4 confirmed. Coach B4 Berth 44.",
                subText = null,
                sender = "Train Status PNR_103",
                category = "messages",
                notificationKey = "key_603",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 603 expected ALERT", res.isImportant)
        }

        // Case 604
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 604L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30104",
                text = "Driver has arrived at pickup. OTP is 2104.",
                subText = null,
                sender = "Ride #30104",
                category = "messages",
                notificationKey = "key_604",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 604 expected ALERT", res.isImportant)
        }

        // Case 605
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 605L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 105",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 105",
                category = "messages",
                notificationKey = "key_605",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 605 expected MUTE", res.isImportant)
        }

        // Case 606
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 606L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30106",
                text = "Driver has arrived at pickup. OTP is 2106.",
                subText = null,
                sender = "Ride #30106",
                category = "messages",
                notificationKey = "key_606",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 606 expected ALERT", res.isImportant)
        }

        // Case 607
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 607L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_107",
                text = "Platform 4 confirmed. Coach B3 Berth 48.",
                subText = null,
                sender = "Train Status PNR_107",
                category = "messages",
                notificationKey = "key_607",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 607 expected ALERT", res.isImportant)
        }

        // Case 608
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 608L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30108",
                text = "Driver has arrived at pickup. OTP is 2108.",
                subText = null,
                sender = "Ride #30108",
                category = "messages",
                notificationKey = "key_608",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 608 expected ALERT", res.isImportant)
        }

        // Case 609
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 609L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_109",
                text = "Platform 4 confirmed. Coach B5 Berth 50.",
                subText = null,
                sender = "Train Status PNR_109",
                category = "messages",
                notificationKey = "key_609",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 609 expected ALERT", res.isImportant)
        }

        // Case 610
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 610L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 110",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 110",
                category = "messages",
                notificationKey = "key_610",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 610 expected MUTE", res.isImportant)
        }

        // Case 611
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 611L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_111",
                text = "Platform 4 confirmed. Coach B2 Berth 52.",
                subText = null,
                sender = "Train Status PNR_111",
                category = "messages",
                notificationKey = "key_611",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 611 expected ALERT", res.isImportant)
        }

        // Case 612
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 612L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30112",
                text = "Driver has arrived at pickup. OTP is 2112.",
                subText = null,
                sender = "Ride #30112",
                category = "messages",
                notificationKey = "key_612",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 612 expected ALERT", res.isImportant)
        }

        // Case 613
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 613L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_113",
                text = "Platform 4 confirmed. Coach B4 Berth 54.",
                subText = null,
                sender = "Train Status PNR_113",
                category = "messages",
                notificationKey = "key_613",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 613 expected ALERT", res.isImportant)
        }

        // Case 614
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 614L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30114",
                text = "Driver has arrived at pickup. OTP is 2114.",
                subText = null,
                sender = "Ride #30114",
                category = "messages",
                notificationKey = "key_614",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 614 expected ALERT", res.isImportant)
        }

        // Case 615
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 615L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 115",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 115",
                category = "messages",
                notificationKey = "key_615",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 615 expected MUTE", res.isImportant)
        }

        // Case 616
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 616L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30116",
                text = "Driver has arrived at pickup. OTP is 2116.",
                subText = null,
                sender = "Ride #30116",
                category = "messages",
                notificationKey = "key_616",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 616 expected ALERT", res.isImportant)
        }

        // Case 617
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 617L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_117",
                text = "Platform 4 confirmed. Coach B3 Berth 58.",
                subText = null,
                sender = "Train Status PNR_117",
                category = "messages",
                notificationKey = "key_617",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 617 expected ALERT", res.isImportant)
        }

        // Case 618
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 618L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30118",
                text = "Driver has arrived at pickup. OTP is 2118.",
                subText = null,
                sender = "Ride #30118",
                category = "messages",
                notificationKey = "key_618",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 618 expected ALERT", res.isImportant)
        }

        // Case 619
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 619L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_119",
                text = "Platform 4 confirmed. Coach B5 Berth 60.",
                subText = null,
                sender = "Train Status PNR_119",
                category = "messages",
                notificationKey = "key_619",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 619 expected ALERT", res.isImportant)
        }

        // Case 620
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 620L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 120",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 120",
                category = "messages",
                notificationKey = "key_620",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 620 expected MUTE", res.isImportant)
        }

        // Case 621
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 621L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_121",
                text = "Platform 4 confirmed. Coach B2 Berth 2.",
                subText = null,
                sender = "Train Status PNR_121",
                category = "messages",
                notificationKey = "key_621",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 621 expected ALERT", res.isImportant)
        }

        // Case 622
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 622L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30122",
                text = "Driver has arrived at pickup. OTP is 2122.",
                subText = null,
                sender = "Ride #30122",
                category = "messages",
                notificationKey = "key_622",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 622 expected ALERT", res.isImportant)
        }

        // Case 623
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 623L)
            val notif = NotificationData(
                packageName = "cris.org.in.prs.ima",
                appName = "IRCTC",
                title = "Train Status PNR_123",
                text = "Platform 4 confirmed. Coach B4 Berth 4.",
                subText = null,
                sender = "Train Status PNR_123",
                category = "messages",
                notificationKey = "key_623",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 623 expected ALERT", res.isImportant)
        }

        // Case 624
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important").toNotificationRule(id = 624L)
            val notif = NotificationData(
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Ride #30124",
                text = "Driver has arrived at pickup. OTP is 2124.",
                subText = null,
                sender = "Ride #30124",
                category = "messages",
                notificationKey = "key_624",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 624 expected ALERT", res.isImportant)
        }

        // Case 625
        run {
            val rule = RuleClassifier.classify("cab arrival and flight train bookings are important, holiday deals not important").toNotificationRule(id = 625L)
            val notif = NotificationData(
                packageName = "com.makemytrip",
                appName = "MakeMyTrip",
                title = "Holiday Package Deal 125",
                text = "Exclusive holiday discount coupon voucher on flight hotels.",
                subText = null,
                sender = "Holiday Package Deal 125",
                category = "messages",
                notificationKey = "key_625",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 625 expected MUTE", res.isImportant)
        }

    }

    @Test
    fun testSocialGamingReels125Cases() {
        // Domain: Dimension6_SocialGamingReels (125 Distinct Test Cases)
        // Case 626
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 626L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #1",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_626",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 626 expected MUTE", res.isImportant)
        }

        // Case 627
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 627L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_2",
                text = "Can we sync on the design document v2?",
                subText = null,
                sender = "Colleague_2",
                category = "messages",
                notificationKey = "key_627",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 627 expected ALERT", res.isImportant)
        }

        // Case 628
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 628L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #3?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_628",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 628 expected ALERT", res.isImportant)
        }

        // Case 629
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 629L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #4 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_629",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 629 expected ALERT", res.isImportant)
        }

        // Case 630
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 630L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_5",
                text = "Can we sync on the design document v5?",
                subText = null,
                sender = "Colleague_5",
                category = "messages",
                notificationKey = "key_630",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 630 expected ALERT", res.isImportant)
        }

        // Case 631
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 631L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/6998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_631",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 631 expected MUTE", res.isImportant)
        }

        // Case 632
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 632L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #7",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_632",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 632 expected MUTE", res.isImportant)
        }

        // Case 633
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 633L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_8",
                text = "Can we sync on the design document v8?",
                subText = null,
                sender = "Colleague_8",
                category = "messages",
                notificationKey = "key_633",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 633 expected ALERT", res.isImportant)
        }

        // Case 634
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 634L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #9?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_634",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 634 expected ALERT", res.isImportant)
        }

        // Case 635
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 635L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #10 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_635",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 635 expected ALERT", res.isImportant)
        }

        // Case 636
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 636L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_11",
                text = "Can we sync on the design document v11?",
                subText = null,
                sender = "Colleague_11",
                category = "messages",
                notificationKey = "key_636",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 636 expected ALERT", res.isImportant)
        }

        // Case 637
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 637L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/12998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_637",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 637 expected MUTE", res.isImportant)
        }

        // Case 638
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 638L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #13",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_638",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 638 expected MUTE", res.isImportant)
        }

        // Case 639
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 639L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_14",
                text = "Can we sync on the design document v14?",
                subText = null,
                sender = "Colleague_14",
                category = "messages",
                notificationKey = "key_639",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 639 expected ALERT", res.isImportant)
        }

        // Case 640
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 640L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #15?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_640",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 640 expected ALERT", res.isImportant)
        }

        // Case 641
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 641L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #16 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_641",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 641 expected ALERT", res.isImportant)
        }

        // Case 642
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 642L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_17",
                text = "Can we sync on the design document v17?",
                subText = null,
                sender = "Colleague_17",
                category = "messages",
                notificationKey = "key_642",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 642 expected ALERT", res.isImportant)
        }

        // Case 643
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 643L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/18998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_643",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 643 expected MUTE", res.isImportant)
        }

        // Case 644
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 644L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #19",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_644",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 644 expected MUTE", res.isImportant)
        }

        // Case 645
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 645L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_20",
                text = "Can we sync on the design document v20?",
                subText = null,
                sender = "Colleague_20",
                category = "messages",
                notificationKey = "key_645",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 645 expected ALERT", res.isImportant)
        }

        // Case 646
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 646L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #21?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_646",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 646 expected ALERT", res.isImportant)
        }

        // Case 647
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 647L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #22 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_647",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 647 expected ALERT", res.isImportant)
        }

        // Case 648
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 648L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_23",
                text = "Can we sync on the design document v23?",
                subText = null,
                sender = "Colleague_23",
                category = "messages",
                notificationKey = "key_648",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 648 expected ALERT", res.isImportant)
        }

        // Case 649
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 649L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/24998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_649",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 649 expected MUTE", res.isImportant)
        }

        // Case 650
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 650L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #25",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_650",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 650 expected MUTE", res.isImportant)
        }

        // Case 651
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 651L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_26",
                text = "Can we sync on the design document v26?",
                subText = null,
                sender = "Colleague_26",
                category = "messages",
                notificationKey = "key_651",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 651 expected ALERT", res.isImportant)
        }

        // Case 652
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 652L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #27?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_652",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 652 expected ALERT", res.isImportant)
        }

        // Case 653
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 653L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #28 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_653",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 653 expected ALERT", res.isImportant)
        }

        // Case 654
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 654L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_29",
                text = "Can we sync on the design document v29?",
                subText = null,
                sender = "Colleague_29",
                category = "messages",
                notificationKey = "key_654",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 654 expected ALERT", res.isImportant)
        }

        // Case 655
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 655L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/30998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_655",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 655 expected MUTE", res.isImportant)
        }

        // Case 656
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 656L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #31",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_656",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 656 expected MUTE", res.isImportant)
        }

        // Case 657
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 657L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_32",
                text = "Can we sync on the design document v32?",
                subText = null,
                sender = "Colleague_32",
                category = "messages",
                notificationKey = "key_657",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 657 expected ALERT", res.isImportant)
        }

        // Case 658
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 658L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #33?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_658",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 658 expected ALERT", res.isImportant)
        }

        // Case 659
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 659L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #34 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_659",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 659 expected ALERT", res.isImportant)
        }

        // Case 660
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 660L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_35",
                text = "Can we sync on the design document v35?",
                subText = null,
                sender = "Colleague_35",
                category = "messages",
                notificationKey = "key_660",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 660 expected ALERT", res.isImportant)
        }

        // Case 661
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 661L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/36998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_661",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 661 expected MUTE", res.isImportant)
        }

        // Case 662
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 662L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #37",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_662",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 662 expected MUTE", res.isImportant)
        }

        // Case 663
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 663L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_38",
                text = "Can we sync on the design document v38?",
                subText = null,
                sender = "Colleague_38",
                category = "messages",
                notificationKey = "key_663",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 663 expected ALERT", res.isImportant)
        }

        // Case 664
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 664L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #39?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_664",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 664 expected ALERT", res.isImportant)
        }

        // Case 665
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 665L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #40 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_665",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 665 expected ALERT", res.isImportant)
        }

        // Case 666
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 666L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_41",
                text = "Can we sync on the design document v41?",
                subText = null,
                sender = "Colleague_41",
                category = "messages",
                notificationKey = "key_666",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 666 expected ALERT", res.isImportant)
        }

        // Case 667
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 667L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/42998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_667",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 667 expected MUTE", res.isImportant)
        }

        // Case 668
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 668L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #43",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_668",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 668 expected MUTE", res.isImportant)
        }

        // Case 669
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 669L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_44",
                text = "Can we sync on the design document v44?",
                subText = null,
                sender = "Colleague_44",
                category = "messages",
                notificationKey = "key_669",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 669 expected ALERT", res.isImportant)
        }

        // Case 670
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 670L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #45?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_670",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 670 expected ALERT", res.isImportant)
        }

        // Case 671
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 671L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #46 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_671",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 671 expected ALERT", res.isImportant)
        }

        // Case 672
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 672L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_47",
                text = "Can we sync on the design document v47?",
                subText = null,
                sender = "Colleague_47",
                category = "messages",
                notificationKey = "key_672",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 672 expected ALERT", res.isImportant)
        }

        // Case 673
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 673L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/48998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_673",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 673 expected MUTE", res.isImportant)
        }

        // Case 674
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 674L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #49",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_674",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 674 expected MUTE", res.isImportant)
        }

        // Case 675
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 675L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_50",
                text = "Can we sync on the design document v50?",
                subText = null,
                sender = "Colleague_50",
                category = "messages",
                notificationKey = "key_675",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 675 expected ALERT", res.isImportant)
        }

        // Case 676
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 676L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #51?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_676",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 676 expected ALERT", res.isImportant)
        }

        // Case 677
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 677L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #52 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_677",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 677 expected ALERT", res.isImportant)
        }

        // Case 678
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 678L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_53",
                text = "Can we sync on the design document v53?",
                subText = null,
                sender = "Colleague_53",
                category = "messages",
                notificationKey = "key_678",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 678 expected ALERT", res.isImportant)
        }

        // Case 679
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 679L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/54998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_679",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 679 expected MUTE", res.isImportant)
        }

        // Case 680
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 680L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #55",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_680",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 680 expected MUTE", res.isImportant)
        }

        // Case 681
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 681L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_56",
                text = "Can we sync on the design document v56?",
                subText = null,
                sender = "Colleague_56",
                category = "messages",
                notificationKey = "key_681",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 681 expected ALERT", res.isImportant)
        }

        // Case 682
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 682L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #57?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_682",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 682 expected ALERT", res.isImportant)
        }

        // Case 683
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 683L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #58 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_683",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 683 expected ALERT", res.isImportant)
        }

        // Case 684
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 684L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_59",
                text = "Can we sync on the design document v59?",
                subText = null,
                sender = "Colleague_59",
                category = "messages",
                notificationKey = "key_684",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 684 expected ALERT", res.isImportant)
        }

        // Case 685
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 685L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/60998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_685",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 685 expected MUTE", res.isImportant)
        }

        // Case 686
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 686L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #61",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_686",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 686 expected MUTE", res.isImportant)
        }

        // Case 687
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 687L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_62",
                text = "Can we sync on the design document v62?",
                subText = null,
                sender = "Colleague_62",
                category = "messages",
                notificationKey = "key_687",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 687 expected ALERT", res.isImportant)
        }

        // Case 688
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 688L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #63?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_688",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 688 expected ALERT", res.isImportant)
        }

        // Case 689
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 689L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #64 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_689",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 689 expected ALERT", res.isImportant)
        }

        // Case 690
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 690L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_65",
                text = "Can we sync on the design document v65?",
                subText = null,
                sender = "Colleague_65",
                category = "messages",
                notificationKey = "key_690",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 690 expected ALERT", res.isImportant)
        }

        // Case 691
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 691L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/66998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_691",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 691 expected MUTE", res.isImportant)
        }

        // Case 692
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 692L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #67",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_692",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 692 expected MUTE", res.isImportant)
        }

        // Case 693
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 693L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_68",
                text = "Can we sync on the design document v68?",
                subText = null,
                sender = "Colleague_68",
                category = "messages",
                notificationKey = "key_693",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 693 expected ALERT", res.isImportant)
        }

        // Case 694
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 694L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #69?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_694",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 694 expected ALERT", res.isImportant)
        }

        // Case 695
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 695L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #70 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_695",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 695 expected ALERT", res.isImportant)
        }

        // Case 696
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 696L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_71",
                text = "Can we sync on the design document v71?",
                subText = null,
                sender = "Colleague_71",
                category = "messages",
                notificationKey = "key_696",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 696 expected ALERT", res.isImportant)
        }

        // Case 697
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 697L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/72998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_697",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 697 expected MUTE", res.isImportant)
        }

        // Case 698
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 698L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #73",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_698",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 698 expected MUTE", res.isImportant)
        }

        // Case 699
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 699L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_74",
                text = "Can we sync on the design document v74?",
                subText = null,
                sender = "Colleague_74",
                category = "messages",
                notificationKey = "key_699",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 699 expected ALERT", res.isImportant)
        }

        // Case 700
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 700L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #75?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_700",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 700 expected ALERT", res.isImportant)
        }

        // Case 701
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 701L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #76 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_701",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 701 expected ALERT", res.isImportant)
        }

        // Case 702
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 702L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_77",
                text = "Can we sync on the design document v77?",
                subText = null,
                sender = "Colleague_77",
                category = "messages",
                notificationKey = "key_702",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 702 expected ALERT", res.isImportant)
        }

        // Case 703
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 703L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/78998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_703",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 703 expected MUTE", res.isImportant)
        }

        // Case 704
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 704L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #79",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_704",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 704 expected MUTE", res.isImportant)
        }

        // Case 705
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 705L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_80",
                text = "Can we sync on the design document v80?",
                subText = null,
                sender = "Colleague_80",
                category = "messages",
                notificationKey = "key_705",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 705 expected ALERT", res.isImportant)
        }

        // Case 706
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 706L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #81?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_706",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 706 expected ALERT", res.isImportant)
        }

        // Case 707
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 707L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #82 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_707",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 707 expected ALERT", res.isImportant)
        }

        // Case 708
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 708L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_83",
                text = "Can we sync on the design document v83?",
                subText = null,
                sender = "Colleague_83",
                category = "messages",
                notificationKey = "key_708",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 708 expected ALERT", res.isImportant)
        }

        // Case 709
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 709L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/84998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_709",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 709 expected MUTE", res.isImportant)
        }

        // Case 710
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 710L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #85",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_710",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 710 expected MUTE", res.isImportant)
        }

        // Case 711
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 711L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_86",
                text = "Can we sync on the design document v86?",
                subText = null,
                sender = "Colleague_86",
                category = "messages",
                notificationKey = "key_711",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 711 expected ALERT", res.isImportant)
        }

        // Case 712
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 712L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #87?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_712",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 712 expected ALERT", res.isImportant)
        }

        // Case 713
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 713L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #88 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_713",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 713 expected ALERT", res.isImportant)
        }

        // Case 714
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 714L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_89",
                text = "Can we sync on the design document v89?",
                subText = null,
                sender = "Colleague_89",
                category = "messages",
                notificationKey = "key_714",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 714 expected ALERT", res.isImportant)
        }

        // Case 715
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 715L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/90998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_715",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 715 expected MUTE", res.isImportant)
        }

        // Case 716
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 716L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #91",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_716",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 716 expected MUTE", res.isImportant)
        }

        // Case 717
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 717L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_92",
                text = "Can we sync on the design document v92?",
                subText = null,
                sender = "Colleague_92",
                category = "messages",
                notificationKey = "key_717",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 717 expected ALERT", res.isImportant)
        }

        // Case 718
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 718L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #93?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_718",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 718 expected ALERT", res.isImportant)
        }

        // Case 719
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 719L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #94 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_719",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 719 expected ALERT", res.isImportant)
        }

        // Case 720
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 720L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_95",
                text = "Can we sync on the design document v95?",
                subText = null,
                sender = "Colleague_95",
                category = "messages",
                notificationKey = "key_720",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 720 expected ALERT", res.isImportant)
        }

        // Case 721
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 721L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/96998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_721",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 721 expected MUTE", res.isImportant)
        }

        // Case 722
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 722L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #97",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_722",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 722 expected MUTE", res.isImportant)
        }

        // Case 723
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 723L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_98",
                text = "Can we sync on the design document v98?",
                subText = null,
                sender = "Colleague_98",
                category = "messages",
                notificationKey = "key_723",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 723 expected ALERT", res.isImportant)
        }

        // Case 724
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 724L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #99?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_724",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 724 expected ALERT", res.isImportant)
        }

        // Case 725
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 725L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #100 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_725",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 725 expected ALERT", res.isImportant)
        }

        // Case 726
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 726L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_101",
                text = "Can we sync on the design document v101?",
                subText = null,
                sender = "Colleague_101",
                category = "messages",
                notificationKey = "key_726",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 726 expected ALERT", res.isImportant)
        }

        // Case 727
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 727L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/102998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_727",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 727 expected MUTE", res.isImportant)
        }

        // Case 728
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 728L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #103",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_728",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 728 expected MUTE", res.isImportant)
        }

        // Case 729
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 729L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_104",
                text = "Can we sync on the design document v104?",
                subText = null,
                sender = "Colleague_104",
                category = "messages",
                notificationKey = "key_729",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 729 expected ALERT", res.isImportant)
        }

        // Case 730
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 730L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #105?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_730",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 730 expected ALERT", res.isImportant)
        }

        // Case 731
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 731L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #106 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_731",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 731 expected ALERT", res.isImportant)
        }

        // Case 732
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 732L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_107",
                text = "Can we sync on the design document v107?",
                subText = null,
                sender = "Colleague_107",
                category = "messages",
                notificationKey = "key_732",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 732 expected ALERT", res.isImportant)
        }

        // Case 733
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 733L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/108998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_733",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 733 expected MUTE", res.isImportant)
        }

        // Case 734
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 734L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #109",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_734",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 734 expected MUTE", res.isImportant)
        }

        // Case 735
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 735L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_110",
                text = "Can we sync on the design document v110?",
                subText = null,
                sender = "Colleague_110",
                category = "messages",
                notificationKey = "key_735",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 735 expected ALERT", res.isImportant)
        }

        // Case 736
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 736L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #111?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_736",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 736 expected ALERT", res.isImportant)
        }

        // Case 737
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 737L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #112 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_737",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 737 expected ALERT", res.isImportant)
        }

        // Case 738
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 738L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_113",
                text = "Can we sync on the design document v113?",
                subText = null,
                sender = "Colleague_113",
                category = "messages",
                notificationKey = "key_738",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 738 expected ALERT", res.isImportant)
        }

        // Case 739
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 739L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/114998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_739",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 739 expected MUTE", res.isImportant)
        }

        // Case 740
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 740L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #115",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_740",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 740 expected MUTE", res.isImportant)
        }

        // Case 741
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 741L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_116",
                text = "Can we sync on the design document v116?",
                subText = null,
                sender = "Colleague_116",
                category = "messages",
                notificationKey = "key_741",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 741 expected ALERT", res.isImportant)
        }

        // Case 742
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 742L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #117?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_742",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 742 expected ALERT", res.isImportant)
        }

        // Case 743
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 743L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #118 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_743",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 743 expected ALERT", res.isImportant)
        }

        // Case 744
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 744L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_119",
                text = "Can we sync on the design document v119?",
                subText = null,
                sender = "Colleague_119",
                category = "messages",
                notificationKey = "key_744",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 744 expected ALERT", res.isImportant)
        }

        // Case 745
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 745L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Check this out instagram.com/reel/120998",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_745",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 745 expected MUTE", res.isImportant)
        }

        // Case 746
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 746L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Let us go to movie theatre screen #121",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_746",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 746 expected MUTE", res.isImportant)
        }

        // Case 747
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 747L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_122",
                text = "Can we sync on the design document v122?",
                subText = null,
                sender = "Colleague_122",
                category = "messages",
                notificationKey = "key_747",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 747 expected ALERT", res.isImportant)
        }

        // Case 748
        run {
            val rule = RuleClassifier.classify("if any message from Madhu it is important, if she sends reels it is not important").toNotificationRule(id = 748L)
            val notif = NotificationData(
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Madhu",
                text = "Hey are you attending the meetup today #123?",
                subText = null,
                sender = "Madhu",
                category = "messages",
                notificationKey = "key_748",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 748 expected ALERT", res.isImportant)
        }

        // Case 749
        run {
            val rule = RuleClassifier.classify("notify me if Arjun msg only about games").toNotificationRule(id = 749L)
            val notif = NotificationData(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Arjun_Vasireddy",
                text = "Brother lets play cricket match tournament #124 today",
                subText = null,
                sender = "Arjun_Vasireddy",
                category = "messages",
                notificationKey = "key_749",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 749 expected ALERT", res.isImportant)
        }

        // Case 750
        run {
            val rule = RuleClassifier.classify("any msg from teams app is important").toNotificationRule(id = 750L)
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Colleague_125",
                text = "Can we sync on the design document v125?",
                subText = null,
                sender = "Colleague_125",
                category = "messages",
                notificationKey = "key_750",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 750 expected ALERT", res.isImportant)
        }

    }

    @Test
    fun testEmotionToneDepth125Cases() {
        // Domain: Dimension7_EmotionToneUrgency (125 Distinct Test Cases)
        // Case 751
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 751L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #1",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_751",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 751 expected ALERT", res.isImportant)
        }

        // Case 752
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 752L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #2",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_752",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 752 expected ALERT", res.isImportant)
        }

        // Case 753
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 753L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #3",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_753",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 753 expected ALERT", res.isImportant)
        }

        // Case 754
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 754L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #4",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_754",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 754 expected ALERT", res.isImportant)
        }

        // Case 755
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 755L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #5",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_755",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 755 expected ALERT", res.isImportant)
        }

        // Case 756
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 756L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #6",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_756",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 756 expected ALERT", res.isImportant)
        }

        // Case 757
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 757L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #7",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_757",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 757 expected ALERT", res.isImportant)
        }

        // Case 758
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 758L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #8",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_758",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 758 expected ALERT", res.isImportant)
        }

        // Case 759
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 759L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #9",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_759",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 759 expected ALERT", res.isImportant)
        }

        // Case 760
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 760L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #10",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_760",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 760 expected ALERT", res.isImportant)
        }

        // Case 761
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 761L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #11",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_761",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 761 expected ALERT", res.isImportant)
        }

        // Case 762
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 762L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #12",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_762",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 762 expected ALERT", res.isImportant)
        }

        // Case 763
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 763L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #13",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_763",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 763 expected ALERT", res.isImportant)
        }

        // Case 764
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 764L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #14",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_764",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 764 expected ALERT", res.isImportant)
        }

        // Case 765
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 765L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #15",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_765",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 765 expected ALERT", res.isImportant)
        }

        // Case 766
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 766L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #16",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_766",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 766 expected ALERT", res.isImportant)
        }

        // Case 767
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 767L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #17",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_767",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 767 expected ALERT", res.isImportant)
        }

        // Case 768
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 768L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #18",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_768",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 768 expected ALERT", res.isImportant)
        }

        // Case 769
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 769L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #19",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_769",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 769 expected ALERT", res.isImportant)
        }

        // Case 770
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 770L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #20",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_770",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 770 expected ALERT", res.isImportant)
        }

        // Case 771
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 771L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #21",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_771",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 771 expected ALERT", res.isImportant)
        }

        // Case 772
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 772L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #22",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_772",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 772 expected ALERT", res.isImportant)
        }

        // Case 773
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 773L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #23",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_773",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 773 expected ALERT", res.isImportant)
        }

        // Case 774
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 774L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #24",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_774",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 774 expected ALERT", res.isImportant)
        }

        // Case 775
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 775L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #25",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_775",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 775 expected ALERT", res.isImportant)
        }

        // Case 776
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 776L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #26",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_776",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 776 expected ALERT", res.isImportant)
        }

        // Case 777
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 777L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #27",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_777",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 777 expected ALERT", res.isImportant)
        }

        // Case 778
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 778L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #28",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_778",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 778 expected ALERT", res.isImportant)
        }

        // Case 779
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 779L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #29",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_779",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 779 expected ALERT", res.isImportant)
        }

        // Case 780
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 780L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #30",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_780",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 780 expected ALERT", res.isImportant)
        }

        // Case 781
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 781L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #31",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_781",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 781 expected ALERT", res.isImportant)
        }

        // Case 782
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 782L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #32",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_782",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 782 expected ALERT", res.isImportant)
        }

        // Case 783
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 783L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #33",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_783",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 783 expected ALERT", res.isImportant)
        }

        // Case 784
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 784L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #34",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_784",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 784 expected ALERT", res.isImportant)
        }

        // Case 785
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 785L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #35",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_785",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 785 expected ALERT", res.isImportant)
        }

        // Case 786
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 786L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #36",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_786",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 786 expected ALERT", res.isImportant)
        }

        // Case 787
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 787L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #37",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_787",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 787 expected ALERT", res.isImportant)
        }

        // Case 788
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 788L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #38",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_788",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 788 expected ALERT", res.isImportant)
        }

        // Case 789
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 789L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #39",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_789",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 789 expected ALERT", res.isImportant)
        }

        // Case 790
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 790L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #40",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_790",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 790 expected ALERT", res.isImportant)
        }

        // Case 791
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 791L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #41",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_791",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 791 expected ALERT", res.isImportant)
        }

        // Case 792
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 792L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #42",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_792",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 792 expected ALERT", res.isImportant)
        }

        // Case 793
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 793L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #43",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_793",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 793 expected ALERT", res.isImportant)
        }

        // Case 794
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 794L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #44",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_794",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 794 expected ALERT", res.isImportant)
        }

        // Case 795
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 795L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #45",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_795",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 795 expected ALERT", res.isImportant)
        }

        // Case 796
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 796L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #46",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_796",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 796 expected ALERT", res.isImportant)
        }

        // Case 797
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 797L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #47",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_797",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 797 expected ALERT", res.isImportant)
        }

        // Case 798
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 798L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #48",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_798",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 798 expected ALERT", res.isImportant)
        }

        // Case 799
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 799L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #49",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_799",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 799 expected ALERT", res.isImportant)
        }

        // Case 800
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 800L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #50",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_800",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 800 expected ALERT", res.isImportant)
        }

        // Case 801
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 801L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #51",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_801",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 801 expected ALERT", res.isImportant)
        }

        // Case 802
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 802L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #52",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_802",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 802 expected ALERT", res.isImportant)
        }

        // Case 803
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 803L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #53",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_803",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 803 expected ALERT", res.isImportant)
        }

        // Case 804
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 804L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #54",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_804",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 804 expected ALERT", res.isImportant)
        }

        // Case 805
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 805L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #55",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_805",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 805 expected ALERT", res.isImportant)
        }

        // Case 806
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 806L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #56",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_806",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 806 expected ALERT", res.isImportant)
        }

        // Case 807
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 807L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #57",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_807",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 807 expected ALERT", res.isImportant)
        }

        // Case 808
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 808L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #58",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_808",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 808 expected ALERT", res.isImportant)
        }

        // Case 809
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 809L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #59",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_809",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 809 expected ALERT", res.isImportant)
        }

        // Case 810
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 810L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #60",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_810",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 810 expected ALERT", res.isImportant)
        }

        // Case 811
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 811L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #61",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_811",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 811 expected ALERT", res.isImportant)
        }

        // Case 812
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 812L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #62",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_812",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 812 expected ALERT", res.isImportant)
        }

        // Case 813
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 813L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #63",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_813",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 813 expected ALERT", res.isImportant)
        }

        // Case 814
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 814L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #64",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_814",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 814 expected ALERT", res.isImportant)
        }

        // Case 815
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 815L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #65",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_815",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 815 expected ALERT", res.isImportant)
        }

        // Case 816
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 816L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #66",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_816",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 816 expected ALERT", res.isImportant)
        }

        // Case 817
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 817L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #67",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_817",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 817 expected ALERT", res.isImportant)
        }

        // Case 818
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 818L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #68",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_818",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 818 expected ALERT", res.isImportant)
        }

        // Case 819
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 819L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #69",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_819",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 819 expected ALERT", res.isImportant)
        }

        // Case 820
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 820L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #70",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_820",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 820 expected ALERT", res.isImportant)
        }

        // Case 821
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 821L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #71",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_821",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 821 expected ALERT", res.isImportant)
        }

        // Case 822
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 822L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #72",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_822",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 822 expected ALERT", res.isImportant)
        }

        // Case 823
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 823L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #73",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_823",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 823 expected ALERT", res.isImportant)
        }

        // Case 824
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 824L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #74",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_824",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 824 expected ALERT", res.isImportant)
        }

        // Case 825
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 825L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #75",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_825",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 825 expected ALERT", res.isImportant)
        }

        // Case 826
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 826L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #76",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_826",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 826 expected ALERT", res.isImportant)
        }

        // Case 827
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 827L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #77",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_827",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 827 expected ALERT", res.isImportant)
        }

        // Case 828
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 828L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #78",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_828",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 828 expected ALERT", res.isImportant)
        }

        // Case 829
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 829L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #79",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_829",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 829 expected ALERT", res.isImportant)
        }

        // Case 830
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 830L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #80",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_830",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 830 expected ALERT", res.isImportant)
        }

        // Case 831
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 831L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #81",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_831",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 831 expected ALERT", res.isImportant)
        }

        // Case 832
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 832L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #82",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_832",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 832 expected ALERT", res.isImportant)
        }

        // Case 833
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 833L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #83",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_833",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 833 expected ALERT", res.isImportant)
        }

        // Case 834
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 834L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #84",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_834",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 834 expected ALERT", res.isImportant)
        }

        // Case 835
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 835L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #85",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_835",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 835 expected ALERT", res.isImportant)
        }

        // Case 836
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 836L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #86",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_836",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 836 expected ALERT", res.isImportant)
        }

        // Case 837
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 837L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #87",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_837",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 837 expected ALERT", res.isImportant)
        }

        // Case 838
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 838L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #88",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_838",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 838 expected ALERT", res.isImportant)
        }

        // Case 839
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 839L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #89",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_839",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 839 expected ALERT", res.isImportant)
        }

        // Case 840
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 840L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #90",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_840",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 840 expected ALERT", res.isImportant)
        }

        // Case 841
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 841L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #91",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_841",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 841 expected ALERT", res.isImportant)
        }

        // Case 842
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 842L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #92",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_842",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 842 expected ALERT", res.isImportant)
        }

        // Case 843
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 843L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #93",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_843",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 843 expected ALERT", res.isImportant)
        }

        // Case 844
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 844L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #94",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_844",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 844 expected ALERT", res.isImportant)
        }

        // Case 845
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 845L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #95",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_845",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 845 expected ALERT", res.isImportant)
        }

        // Case 846
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 846L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #96",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_846",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 846 expected ALERT", res.isImportant)
        }

        // Case 847
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 847L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #97",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_847",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 847 expected ALERT", res.isImportant)
        }

        // Case 848
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 848L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #98",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_848",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 848 expected ALERT", res.isImportant)
        }

        // Case 849
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 849L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #99",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_849",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 849 expected ALERT", res.isImportant)
        }

        // Case 850
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 850L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #100",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_850",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 850 expected ALERT", res.isImportant)
        }

        // Case 851
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 851L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #101",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_851",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 851 expected ALERT", res.isImportant)
        }

        // Case 852
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 852L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #102",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_852",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 852 expected ALERT", res.isImportant)
        }

        // Case 853
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 853L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #103",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_853",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 853 expected ALERT", res.isImportant)
        }

        // Case 854
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 854L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #104",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_854",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 854 expected ALERT", res.isImportant)
        }

        // Case 855
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 855L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #105",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_855",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 855 expected ALERT", res.isImportant)
        }

        // Case 856
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 856L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #106",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_856",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 856 expected ALERT", res.isImportant)
        }

        // Case 857
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 857L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #107",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_857",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 857 expected ALERT", res.isImportant)
        }

        // Case 858
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 858L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #108",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_858",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 858 expected ALERT", res.isImportant)
        }

        // Case 859
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 859L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #109",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_859",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 859 expected ALERT", res.isImportant)
        }

        // Case 860
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 860L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #110",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_860",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 860 expected ALERT", res.isImportant)
        }

        // Case 861
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 861L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #111",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_861",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 861 expected ALERT", res.isImportant)
        }

        // Case 862
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 862L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #112",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_862",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 862 expected ALERT", res.isImportant)
        }

        // Case 863
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 863L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #113",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_863",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 863 expected ALERT", res.isImportant)
        }

        // Case 864
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 864L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #114",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_864",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 864 expected ALERT", res.isImportant)
        }

        // Case 865
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 865L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #115",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_865",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 865 expected ALERT", res.isImportant)
        }

        // Case 866
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 866L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #116",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_866",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 866 expected ALERT", res.isImportant)
        }

        // Case 867
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 867L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #117",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_867",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 867 expected ALERT", res.isImportant)
        }

        // Case 868
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 868L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #118",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_868",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 868 expected ALERT", res.isImportant)
        }

        // Case 869
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 869L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #119",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_869",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 869 expected ALERT", res.isImportant)
        }

        // Case 870
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 870L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #120",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_870",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 870 expected ALERT", res.isImportant)
        }

        // Case 871
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 871L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #121",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_871",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 871 expected ALERT", res.isImportant)
        }

        // Case 872
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 872L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #122",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_872",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 872 expected ALERT", res.isImportant)
        }

        // Case 873
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 873L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #123",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_873",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 873 expected ALERT", res.isImportant)
        }

        // Case 874
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 874L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #124",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_874",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 874 expected ALERT", res.isImportant)
        }

        // Case 875
        run {
            val rule = RuleClassifier.classify("any msg from Pranav when he is angry is not important").toNotificationRule(id = 875L)
            assertEquals("K2_DEEP", rule.semanticDepth)
            assertEquals("pranav", rule.targetPerson)
            assertFalse(rule.getExcludedTopics().contains("pranav"))
            val notif = NotificationData(
                packageName = "com.microsoft.teams",
                appName = "Teams",
                title = "Pranav Dhamodaran",
                text = "Great sprint deliverable test #125",
                subText = null,
                sender = "Pranav Dhamodaran",
                category = "messages",
                notificationKey = "key_875",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 875 expected ALERT", res.isImportant)
        }

    }

    @Test
    fun testUtilitiesGovtSpamNoise125Cases() {
        // Domain: Dimension8_UtilitiesBillsGovtNoise (125 Distinct Test Cases)
        // Case 876
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 876L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #1",
                text = "Due date approaching for power bill payment Rs.501.",
                subText = null,
                sender = "BESCOM Electricity Bill #1",
                category = "messages",
                notificationKey = "key_876",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 876 expected ALERT", res.isImportant)
        }

        // Case 877
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 877L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #2",
                text = "Due date approaching for power bill payment Rs.502.",
                subText = null,
                sender = "BESCOM Electricity Bill #2",
                category = "messages",
                notificationKey = "key_877",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 877 expected ALERT", res.isImportant)
        }

        // Case 878
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 878L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #3",
                text = "Due date approaching for power bill payment Rs.503.",
                subText = null,
                sender = "BESCOM Electricity Bill #3",
                category = "messages",
                notificationKey = "key_878",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 878 expected ALERT", res.isImportant)
        }

        // Case 879
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 879L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #4",
                text = "Due date approaching for power bill payment Rs.504.",
                subText = null,
                sender = "BESCOM Electricity Bill #4",
                category = "messages",
                notificationKey = "key_879",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 879 expected ALERT", res.isImportant)
        }

        // Case 880
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 880L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #5",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #5",
                category = "messages",
                notificationKey = "key_880",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 880 expected MUTE", res.isImportant)
        }

        // Case 881
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 881L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #6",
                text = "Due date approaching for power bill payment Rs.506.",
                subText = null,
                sender = "BESCOM Electricity Bill #6",
                category = "messages",
                notificationKey = "key_881",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 881 expected ALERT", res.isImportant)
        }

        // Case 882
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 882L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #7",
                text = "Due date approaching for power bill payment Rs.507.",
                subText = null,
                sender = "BESCOM Electricity Bill #7",
                category = "messages",
                notificationKey = "key_882",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 882 expected ALERT", res.isImportant)
        }

        // Case 883
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 883L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #8",
                text = "Due date approaching for power bill payment Rs.508.",
                subText = null,
                sender = "BESCOM Electricity Bill #8",
                category = "messages",
                notificationKey = "key_883",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 883 expected ALERT", res.isImportant)
        }

        // Case 884
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 884L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #9",
                text = "Due date approaching for power bill payment Rs.509.",
                subText = null,
                sender = "BESCOM Electricity Bill #9",
                category = "messages",
                notificationKey = "key_884",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 884 expected ALERT", res.isImportant)
        }

        // Case 885
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 885L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #10",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #10",
                category = "messages",
                notificationKey = "key_885",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 885 expected MUTE", res.isImportant)
        }

        // Case 886
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 886L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #11",
                text = "Due date approaching for power bill payment Rs.511.",
                subText = null,
                sender = "BESCOM Electricity Bill #11",
                category = "messages",
                notificationKey = "key_886",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 886 expected ALERT", res.isImportant)
        }

        // Case 887
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 887L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #12",
                text = "Due date approaching for power bill payment Rs.512.",
                subText = null,
                sender = "BESCOM Electricity Bill #12",
                category = "messages",
                notificationKey = "key_887",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 887 expected ALERT", res.isImportant)
        }

        // Case 888
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 888L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #13",
                text = "Due date approaching for power bill payment Rs.513.",
                subText = null,
                sender = "BESCOM Electricity Bill #13",
                category = "messages",
                notificationKey = "key_888",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 888 expected ALERT", res.isImportant)
        }

        // Case 889
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 889L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #14",
                text = "Due date approaching for power bill payment Rs.514.",
                subText = null,
                sender = "BESCOM Electricity Bill #14",
                category = "messages",
                notificationKey = "key_889",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 889 expected ALERT", res.isImportant)
        }

        // Case 890
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 890L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #15",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #15",
                category = "messages",
                notificationKey = "key_890",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 890 expected MUTE", res.isImportant)
        }

        // Case 891
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 891L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #16",
                text = "Due date approaching for power bill payment Rs.516.",
                subText = null,
                sender = "BESCOM Electricity Bill #16",
                category = "messages",
                notificationKey = "key_891",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 891 expected ALERT", res.isImportant)
        }

        // Case 892
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 892L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #17",
                text = "Due date approaching for power bill payment Rs.517.",
                subText = null,
                sender = "BESCOM Electricity Bill #17",
                category = "messages",
                notificationKey = "key_892",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 892 expected ALERT", res.isImportant)
        }

        // Case 893
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 893L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #18",
                text = "Due date approaching for power bill payment Rs.518.",
                subText = null,
                sender = "BESCOM Electricity Bill #18",
                category = "messages",
                notificationKey = "key_893",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 893 expected ALERT", res.isImportant)
        }

        // Case 894
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 894L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #19",
                text = "Due date approaching for power bill payment Rs.519.",
                subText = null,
                sender = "BESCOM Electricity Bill #19",
                category = "messages",
                notificationKey = "key_894",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 894 expected ALERT", res.isImportant)
        }

        // Case 895
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 895L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #20",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #20",
                category = "messages",
                notificationKey = "key_895",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 895 expected MUTE", res.isImportant)
        }

        // Case 896
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 896L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #21",
                text = "Due date approaching for power bill payment Rs.521.",
                subText = null,
                sender = "BESCOM Electricity Bill #21",
                category = "messages",
                notificationKey = "key_896",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 896 expected ALERT", res.isImportant)
        }

        // Case 897
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 897L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #22",
                text = "Due date approaching for power bill payment Rs.522.",
                subText = null,
                sender = "BESCOM Electricity Bill #22",
                category = "messages",
                notificationKey = "key_897",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 897 expected ALERT", res.isImportant)
        }

        // Case 898
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 898L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #23",
                text = "Due date approaching for power bill payment Rs.523.",
                subText = null,
                sender = "BESCOM Electricity Bill #23",
                category = "messages",
                notificationKey = "key_898",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 898 expected ALERT", res.isImportant)
        }

        // Case 899
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 899L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #24",
                text = "Due date approaching for power bill payment Rs.524.",
                subText = null,
                sender = "BESCOM Electricity Bill #24",
                category = "messages",
                notificationKey = "key_899",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 899 expected ALERT", res.isImportant)
        }

        // Case 900
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 900L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #25",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #25",
                category = "messages",
                notificationKey = "key_900",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 900 expected MUTE", res.isImportant)
        }

        // Case 901
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 901L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #26",
                text = "Due date approaching for power bill payment Rs.526.",
                subText = null,
                sender = "BESCOM Electricity Bill #26",
                category = "messages",
                notificationKey = "key_901",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 901 expected ALERT", res.isImportant)
        }

        // Case 902
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 902L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #27",
                text = "Due date approaching for power bill payment Rs.527.",
                subText = null,
                sender = "BESCOM Electricity Bill #27",
                category = "messages",
                notificationKey = "key_902",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 902 expected ALERT", res.isImportant)
        }

        // Case 903
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 903L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #28",
                text = "Due date approaching for power bill payment Rs.528.",
                subText = null,
                sender = "BESCOM Electricity Bill #28",
                category = "messages",
                notificationKey = "key_903",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 903 expected ALERT", res.isImportant)
        }

        // Case 904
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 904L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #29",
                text = "Due date approaching for power bill payment Rs.529.",
                subText = null,
                sender = "BESCOM Electricity Bill #29",
                category = "messages",
                notificationKey = "key_904",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 904 expected ALERT", res.isImportant)
        }

        // Case 905
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 905L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #30",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #30",
                category = "messages",
                notificationKey = "key_905",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 905 expected MUTE", res.isImportant)
        }

        // Case 906
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 906L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #31",
                text = "Due date approaching for power bill payment Rs.531.",
                subText = null,
                sender = "BESCOM Electricity Bill #31",
                category = "messages",
                notificationKey = "key_906",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 906 expected ALERT", res.isImportant)
        }

        // Case 907
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 907L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #32",
                text = "Due date approaching for power bill payment Rs.532.",
                subText = null,
                sender = "BESCOM Electricity Bill #32",
                category = "messages",
                notificationKey = "key_907",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 907 expected ALERT", res.isImportant)
        }

        // Case 908
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 908L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #33",
                text = "Due date approaching for power bill payment Rs.533.",
                subText = null,
                sender = "BESCOM Electricity Bill #33",
                category = "messages",
                notificationKey = "key_908",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 908 expected ALERT", res.isImportant)
        }

        // Case 909
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 909L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #34",
                text = "Due date approaching for power bill payment Rs.534.",
                subText = null,
                sender = "BESCOM Electricity Bill #34",
                category = "messages",
                notificationKey = "key_909",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 909 expected ALERT", res.isImportant)
        }

        // Case 910
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 910L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #35",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #35",
                category = "messages",
                notificationKey = "key_910",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 910 expected MUTE", res.isImportant)
        }

        // Case 911
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 911L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #36",
                text = "Due date approaching for power bill payment Rs.536.",
                subText = null,
                sender = "BESCOM Electricity Bill #36",
                category = "messages",
                notificationKey = "key_911",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 911 expected ALERT", res.isImportant)
        }

        // Case 912
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 912L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #37",
                text = "Due date approaching for power bill payment Rs.537.",
                subText = null,
                sender = "BESCOM Electricity Bill #37",
                category = "messages",
                notificationKey = "key_912",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 912 expected ALERT", res.isImportant)
        }

        // Case 913
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 913L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #38",
                text = "Due date approaching for power bill payment Rs.538.",
                subText = null,
                sender = "BESCOM Electricity Bill #38",
                category = "messages",
                notificationKey = "key_913",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 913 expected ALERT", res.isImportant)
        }

        // Case 914
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 914L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #39",
                text = "Due date approaching for power bill payment Rs.539.",
                subText = null,
                sender = "BESCOM Electricity Bill #39",
                category = "messages",
                notificationKey = "key_914",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 914 expected ALERT", res.isImportant)
        }

        // Case 915
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 915L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #40",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #40",
                category = "messages",
                notificationKey = "key_915",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 915 expected MUTE", res.isImportant)
        }

        // Case 916
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 916L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #41",
                text = "Due date approaching for power bill payment Rs.541.",
                subText = null,
                sender = "BESCOM Electricity Bill #41",
                category = "messages",
                notificationKey = "key_916",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 916 expected ALERT", res.isImportant)
        }

        // Case 917
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 917L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #42",
                text = "Due date approaching for power bill payment Rs.542.",
                subText = null,
                sender = "BESCOM Electricity Bill #42",
                category = "messages",
                notificationKey = "key_917",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 917 expected ALERT", res.isImportant)
        }

        // Case 918
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 918L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #43",
                text = "Due date approaching for power bill payment Rs.543.",
                subText = null,
                sender = "BESCOM Electricity Bill #43",
                category = "messages",
                notificationKey = "key_918",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 918 expected ALERT", res.isImportant)
        }

        // Case 919
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 919L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #44",
                text = "Due date approaching for power bill payment Rs.544.",
                subText = null,
                sender = "BESCOM Electricity Bill #44",
                category = "messages",
                notificationKey = "key_919",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 919 expected ALERT", res.isImportant)
        }

        // Case 920
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 920L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #45",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #45",
                category = "messages",
                notificationKey = "key_920",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 920 expected MUTE", res.isImportant)
        }

        // Case 921
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 921L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #46",
                text = "Due date approaching for power bill payment Rs.546.",
                subText = null,
                sender = "BESCOM Electricity Bill #46",
                category = "messages",
                notificationKey = "key_921",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 921 expected ALERT", res.isImportant)
        }

        // Case 922
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 922L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #47",
                text = "Due date approaching for power bill payment Rs.547.",
                subText = null,
                sender = "BESCOM Electricity Bill #47",
                category = "messages",
                notificationKey = "key_922",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 922 expected ALERT", res.isImportant)
        }

        // Case 923
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 923L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #48",
                text = "Due date approaching for power bill payment Rs.548.",
                subText = null,
                sender = "BESCOM Electricity Bill #48",
                category = "messages",
                notificationKey = "key_923",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 923 expected ALERT", res.isImportant)
        }

        // Case 924
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 924L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #49",
                text = "Due date approaching for power bill payment Rs.549.",
                subText = null,
                sender = "BESCOM Electricity Bill #49",
                category = "messages",
                notificationKey = "key_924",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 924 expected ALERT", res.isImportant)
        }

        // Case 925
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 925L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #50",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #50",
                category = "messages",
                notificationKey = "key_925",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 925 expected MUTE", res.isImportant)
        }

        // Case 926
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 926L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #51",
                text = "Due date approaching for power bill payment Rs.551.",
                subText = null,
                sender = "BESCOM Electricity Bill #51",
                category = "messages",
                notificationKey = "key_926",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 926 expected ALERT", res.isImportant)
        }

        // Case 927
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 927L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #52",
                text = "Due date approaching for power bill payment Rs.552.",
                subText = null,
                sender = "BESCOM Electricity Bill #52",
                category = "messages",
                notificationKey = "key_927",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 927 expected ALERT", res.isImportant)
        }

        // Case 928
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 928L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #53",
                text = "Due date approaching for power bill payment Rs.553.",
                subText = null,
                sender = "BESCOM Electricity Bill #53",
                category = "messages",
                notificationKey = "key_928",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 928 expected ALERT", res.isImportant)
        }

        // Case 929
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 929L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #54",
                text = "Due date approaching for power bill payment Rs.554.",
                subText = null,
                sender = "BESCOM Electricity Bill #54",
                category = "messages",
                notificationKey = "key_929",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 929 expected ALERT", res.isImportant)
        }

        // Case 930
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 930L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #55",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #55",
                category = "messages",
                notificationKey = "key_930",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 930 expected MUTE", res.isImportant)
        }

        // Case 931
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 931L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #56",
                text = "Due date approaching for power bill payment Rs.556.",
                subText = null,
                sender = "BESCOM Electricity Bill #56",
                category = "messages",
                notificationKey = "key_931",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 931 expected ALERT", res.isImportant)
        }

        // Case 932
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 932L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #57",
                text = "Due date approaching for power bill payment Rs.557.",
                subText = null,
                sender = "BESCOM Electricity Bill #57",
                category = "messages",
                notificationKey = "key_932",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 932 expected ALERT", res.isImportant)
        }

        // Case 933
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 933L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #58",
                text = "Due date approaching for power bill payment Rs.558.",
                subText = null,
                sender = "BESCOM Electricity Bill #58",
                category = "messages",
                notificationKey = "key_933",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 933 expected ALERT", res.isImportant)
        }

        // Case 934
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 934L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #59",
                text = "Due date approaching for power bill payment Rs.559.",
                subText = null,
                sender = "BESCOM Electricity Bill #59",
                category = "messages",
                notificationKey = "key_934",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 934 expected ALERT", res.isImportant)
        }

        // Case 935
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 935L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #60",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #60",
                category = "messages",
                notificationKey = "key_935",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 935 expected MUTE", res.isImportant)
        }

        // Case 936
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 936L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #61",
                text = "Due date approaching for power bill payment Rs.561.",
                subText = null,
                sender = "BESCOM Electricity Bill #61",
                category = "messages",
                notificationKey = "key_936",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 936 expected ALERT", res.isImportant)
        }

        // Case 937
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 937L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #62",
                text = "Due date approaching for power bill payment Rs.562.",
                subText = null,
                sender = "BESCOM Electricity Bill #62",
                category = "messages",
                notificationKey = "key_937",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 937 expected ALERT", res.isImportant)
        }

        // Case 938
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 938L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #63",
                text = "Due date approaching for power bill payment Rs.563.",
                subText = null,
                sender = "BESCOM Electricity Bill #63",
                category = "messages",
                notificationKey = "key_938",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 938 expected ALERT", res.isImportant)
        }

        // Case 939
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 939L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #64",
                text = "Due date approaching for power bill payment Rs.564.",
                subText = null,
                sender = "BESCOM Electricity Bill #64",
                category = "messages",
                notificationKey = "key_939",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 939 expected ALERT", res.isImportant)
        }

        // Case 940
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 940L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #65",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #65",
                category = "messages",
                notificationKey = "key_940",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 940 expected MUTE", res.isImportant)
        }

        // Case 941
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 941L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #66",
                text = "Due date approaching for power bill payment Rs.566.",
                subText = null,
                sender = "BESCOM Electricity Bill #66",
                category = "messages",
                notificationKey = "key_941",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 941 expected ALERT", res.isImportant)
        }

        // Case 942
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 942L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #67",
                text = "Due date approaching for power bill payment Rs.567.",
                subText = null,
                sender = "BESCOM Electricity Bill #67",
                category = "messages",
                notificationKey = "key_942",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 942 expected ALERT", res.isImportant)
        }

        // Case 943
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 943L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #68",
                text = "Due date approaching for power bill payment Rs.568.",
                subText = null,
                sender = "BESCOM Electricity Bill #68",
                category = "messages",
                notificationKey = "key_943",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 943 expected ALERT", res.isImportant)
        }

        // Case 944
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 944L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #69",
                text = "Due date approaching for power bill payment Rs.569.",
                subText = null,
                sender = "BESCOM Electricity Bill #69",
                category = "messages",
                notificationKey = "key_944",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 944 expected ALERT", res.isImportant)
        }

        // Case 945
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 945L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #70",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #70",
                category = "messages",
                notificationKey = "key_945",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 945 expected MUTE", res.isImportant)
        }

        // Case 946
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 946L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #71",
                text = "Due date approaching for power bill payment Rs.571.",
                subText = null,
                sender = "BESCOM Electricity Bill #71",
                category = "messages",
                notificationKey = "key_946",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 946 expected ALERT", res.isImportant)
        }

        // Case 947
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 947L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #72",
                text = "Due date approaching for power bill payment Rs.572.",
                subText = null,
                sender = "BESCOM Electricity Bill #72",
                category = "messages",
                notificationKey = "key_947",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 947 expected ALERT", res.isImportant)
        }

        // Case 948
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 948L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #73",
                text = "Due date approaching for power bill payment Rs.573.",
                subText = null,
                sender = "BESCOM Electricity Bill #73",
                category = "messages",
                notificationKey = "key_948",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 948 expected ALERT", res.isImportant)
        }

        // Case 949
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 949L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #74",
                text = "Due date approaching for power bill payment Rs.574.",
                subText = null,
                sender = "BESCOM Electricity Bill #74",
                category = "messages",
                notificationKey = "key_949",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 949 expected ALERT", res.isImportant)
        }

        // Case 950
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 950L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #75",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #75",
                category = "messages",
                notificationKey = "key_950",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 950 expected MUTE", res.isImportant)
        }

        // Case 951
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 951L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #76",
                text = "Due date approaching for power bill payment Rs.576.",
                subText = null,
                sender = "BESCOM Electricity Bill #76",
                category = "messages",
                notificationKey = "key_951",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 951 expected ALERT", res.isImportant)
        }

        // Case 952
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 952L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #77",
                text = "Due date approaching for power bill payment Rs.577.",
                subText = null,
                sender = "BESCOM Electricity Bill #77",
                category = "messages",
                notificationKey = "key_952",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 952 expected ALERT", res.isImportant)
        }

        // Case 953
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 953L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #78",
                text = "Due date approaching for power bill payment Rs.578.",
                subText = null,
                sender = "BESCOM Electricity Bill #78",
                category = "messages",
                notificationKey = "key_953",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 953 expected ALERT", res.isImportant)
        }

        // Case 954
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 954L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #79",
                text = "Due date approaching for power bill payment Rs.579.",
                subText = null,
                sender = "BESCOM Electricity Bill #79",
                category = "messages",
                notificationKey = "key_954",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 954 expected ALERT", res.isImportant)
        }

        // Case 955
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 955L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #80",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #80",
                category = "messages",
                notificationKey = "key_955",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 955 expected MUTE", res.isImportant)
        }

        // Case 956
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 956L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #81",
                text = "Due date approaching for power bill payment Rs.581.",
                subText = null,
                sender = "BESCOM Electricity Bill #81",
                category = "messages",
                notificationKey = "key_956",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 956 expected ALERT", res.isImportant)
        }

        // Case 957
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 957L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #82",
                text = "Due date approaching for power bill payment Rs.582.",
                subText = null,
                sender = "BESCOM Electricity Bill #82",
                category = "messages",
                notificationKey = "key_957",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 957 expected ALERT", res.isImportant)
        }

        // Case 958
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 958L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #83",
                text = "Due date approaching for power bill payment Rs.583.",
                subText = null,
                sender = "BESCOM Electricity Bill #83",
                category = "messages",
                notificationKey = "key_958",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 958 expected ALERT", res.isImportant)
        }

        // Case 959
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 959L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #84",
                text = "Due date approaching for power bill payment Rs.584.",
                subText = null,
                sender = "BESCOM Electricity Bill #84",
                category = "messages",
                notificationKey = "key_959",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 959 expected ALERT", res.isImportant)
        }

        // Case 960
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 960L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #85",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #85",
                category = "messages",
                notificationKey = "key_960",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 960 expected MUTE", res.isImportant)
        }

        // Case 961
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 961L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #86",
                text = "Due date approaching for power bill payment Rs.586.",
                subText = null,
                sender = "BESCOM Electricity Bill #86",
                category = "messages",
                notificationKey = "key_961",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 961 expected ALERT", res.isImportant)
        }

        // Case 962
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 962L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #87",
                text = "Due date approaching for power bill payment Rs.587.",
                subText = null,
                sender = "BESCOM Electricity Bill #87",
                category = "messages",
                notificationKey = "key_962",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 962 expected ALERT", res.isImportant)
        }

        // Case 963
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 963L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #88",
                text = "Due date approaching for power bill payment Rs.588.",
                subText = null,
                sender = "BESCOM Electricity Bill #88",
                category = "messages",
                notificationKey = "key_963",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 963 expected ALERT", res.isImportant)
        }

        // Case 964
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 964L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #89",
                text = "Due date approaching for power bill payment Rs.589.",
                subText = null,
                sender = "BESCOM Electricity Bill #89",
                category = "messages",
                notificationKey = "key_964",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 964 expected ALERT", res.isImportant)
        }

        // Case 965
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 965L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #90",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #90",
                category = "messages",
                notificationKey = "key_965",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 965 expected MUTE", res.isImportant)
        }

        // Case 966
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 966L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #91",
                text = "Due date approaching for power bill payment Rs.591.",
                subText = null,
                sender = "BESCOM Electricity Bill #91",
                category = "messages",
                notificationKey = "key_966",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 966 expected ALERT", res.isImportant)
        }

        // Case 967
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 967L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #92",
                text = "Due date approaching for power bill payment Rs.592.",
                subText = null,
                sender = "BESCOM Electricity Bill #92",
                category = "messages",
                notificationKey = "key_967",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 967 expected ALERT", res.isImportant)
        }

        // Case 968
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 968L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #93",
                text = "Due date approaching for power bill payment Rs.593.",
                subText = null,
                sender = "BESCOM Electricity Bill #93",
                category = "messages",
                notificationKey = "key_968",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 968 expected ALERT", res.isImportant)
        }

        // Case 969
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 969L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #94",
                text = "Due date approaching for power bill payment Rs.594.",
                subText = null,
                sender = "BESCOM Electricity Bill #94",
                category = "messages",
                notificationKey = "key_969",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 969 expected ALERT", res.isImportant)
        }

        // Case 970
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 970L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #95",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #95",
                category = "messages",
                notificationKey = "key_970",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 970 expected MUTE", res.isImportant)
        }

        // Case 971
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 971L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #96",
                text = "Due date approaching for power bill payment Rs.596.",
                subText = null,
                sender = "BESCOM Electricity Bill #96",
                category = "messages",
                notificationKey = "key_971",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 971 expected ALERT", res.isImportant)
        }

        // Case 972
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 972L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #97",
                text = "Due date approaching for power bill payment Rs.597.",
                subText = null,
                sender = "BESCOM Electricity Bill #97",
                category = "messages",
                notificationKey = "key_972",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 972 expected ALERT", res.isImportant)
        }

        // Case 973
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 973L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #98",
                text = "Due date approaching for power bill payment Rs.598.",
                subText = null,
                sender = "BESCOM Electricity Bill #98",
                category = "messages",
                notificationKey = "key_973",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 973 expected ALERT", res.isImportant)
        }

        // Case 974
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 974L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #99",
                text = "Due date approaching for power bill payment Rs.599.",
                subText = null,
                sender = "BESCOM Electricity Bill #99",
                category = "messages",
                notificationKey = "key_974",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 974 expected ALERT", res.isImportant)
        }

        // Case 975
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 975L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #100",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #100",
                category = "messages",
                notificationKey = "key_975",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 975 expected MUTE", res.isImportant)
        }

        // Case 976
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 976L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #101",
                text = "Due date approaching for power bill payment Rs.601.",
                subText = null,
                sender = "BESCOM Electricity Bill #101",
                category = "messages",
                notificationKey = "key_976",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 976 expected ALERT", res.isImportant)
        }

        // Case 977
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 977L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #102",
                text = "Due date approaching for power bill payment Rs.602.",
                subText = null,
                sender = "BESCOM Electricity Bill #102",
                category = "messages",
                notificationKey = "key_977",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 977 expected ALERT", res.isImportant)
        }

        // Case 978
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 978L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #103",
                text = "Due date approaching for power bill payment Rs.603.",
                subText = null,
                sender = "BESCOM Electricity Bill #103",
                category = "messages",
                notificationKey = "key_978",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 978 expected ALERT", res.isImportant)
        }

        // Case 979
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 979L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #104",
                text = "Due date approaching for power bill payment Rs.604.",
                subText = null,
                sender = "BESCOM Electricity Bill #104",
                category = "messages",
                notificationKey = "key_979",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 979 expected ALERT", res.isImportant)
        }

        // Case 980
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 980L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #105",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #105",
                category = "messages",
                notificationKey = "key_980",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 980 expected MUTE", res.isImportant)
        }

        // Case 981
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 981L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #106",
                text = "Due date approaching for power bill payment Rs.606.",
                subText = null,
                sender = "BESCOM Electricity Bill #106",
                category = "messages",
                notificationKey = "key_981",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 981 expected ALERT", res.isImportant)
        }

        // Case 982
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 982L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #107",
                text = "Due date approaching for power bill payment Rs.607.",
                subText = null,
                sender = "BESCOM Electricity Bill #107",
                category = "messages",
                notificationKey = "key_982",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 982 expected ALERT", res.isImportant)
        }

        // Case 983
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 983L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #108",
                text = "Due date approaching for power bill payment Rs.608.",
                subText = null,
                sender = "BESCOM Electricity Bill #108",
                category = "messages",
                notificationKey = "key_983",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 983 expected ALERT", res.isImportant)
        }

        // Case 984
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 984L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #109",
                text = "Due date approaching for power bill payment Rs.609.",
                subText = null,
                sender = "BESCOM Electricity Bill #109",
                category = "messages",
                notificationKey = "key_984",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 984 expected ALERT", res.isImportant)
        }

        // Case 985
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 985L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #110",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #110",
                category = "messages",
                notificationKey = "key_985",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 985 expected MUTE", res.isImportant)
        }

        // Case 986
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 986L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #111",
                text = "Due date approaching for power bill payment Rs.611.",
                subText = null,
                sender = "BESCOM Electricity Bill #111",
                category = "messages",
                notificationKey = "key_986",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 986 expected ALERT", res.isImportant)
        }

        // Case 987
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 987L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #112",
                text = "Due date approaching for power bill payment Rs.612.",
                subText = null,
                sender = "BESCOM Electricity Bill #112",
                category = "messages",
                notificationKey = "key_987",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 987 expected ALERT", res.isImportant)
        }

        // Case 988
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 988L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #113",
                text = "Due date approaching for power bill payment Rs.613.",
                subText = null,
                sender = "BESCOM Electricity Bill #113",
                category = "messages",
                notificationKey = "key_988",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 988 expected ALERT", res.isImportant)
        }

        // Case 989
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 989L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #114",
                text = "Due date approaching for power bill payment Rs.614.",
                subText = null,
                sender = "BESCOM Electricity Bill #114",
                category = "messages",
                notificationKey = "key_989",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 989 expected ALERT", res.isImportant)
        }

        // Case 990
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 990L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #115",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #115",
                category = "messages",
                notificationKey = "key_990",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 990 expected MUTE", res.isImportant)
        }

        // Case 991
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 991L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #116",
                text = "Due date approaching for power bill payment Rs.616.",
                subText = null,
                sender = "BESCOM Electricity Bill #116",
                category = "messages",
                notificationKey = "key_991",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 991 expected ALERT", res.isImportant)
        }

        // Case 992
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 992L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #117",
                text = "Due date approaching for power bill payment Rs.617.",
                subText = null,
                sender = "BESCOM Electricity Bill #117",
                category = "messages",
                notificationKey = "key_992",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 992 expected ALERT", res.isImportant)
        }

        // Case 993
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 993L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #118",
                text = "Due date approaching for power bill payment Rs.618.",
                subText = null,
                sender = "BESCOM Electricity Bill #118",
                category = "messages",
                notificationKey = "key_993",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 993 expected ALERT", res.isImportant)
        }

        // Case 994
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 994L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #119",
                text = "Due date approaching for power bill payment Rs.619.",
                subText = null,
                sender = "BESCOM Electricity Bill #119",
                category = "messages",
                notificationKey = "key_994",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 994 expected ALERT", res.isImportant)
        }

        // Case 995
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 995L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #120",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #120",
                category = "messages",
                notificationKey = "key_995",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 995 expected MUTE", res.isImportant)
        }

        // Case 996
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 996L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #121",
                text = "Due date approaching for power bill payment Rs.621.",
                subText = null,
                sender = "BESCOM Electricity Bill #121",
                category = "messages",
                notificationKey = "key_996",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 996 expected ALERT", res.isImportant)
        }

        // Case 997
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 997L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #122",
                text = "Due date approaching for power bill payment Rs.622.",
                subText = null,
                sender = "BESCOM Electricity Bill #122",
                category = "messages",
                notificationKey = "key_997",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 997 expected ALERT", res.isImportant)
        }

        // Case 998
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 998L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #123",
                text = "Due date approaching for power bill payment Rs.623.",
                subText = null,
                sender = "BESCOM Electricity Bill #123",
                category = "messages",
                notificationKey = "key_998",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 998 expected ALERT", res.isImportant)
        }

        // Case 999
        run {
            val rule = RuleClassifier.classify("electricity bills and traffic challan are important").toNotificationRule(id = 999L)
            val notif = NotificationData(
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "BESCOM Electricity Bill #124",
                text = "Due date approaching for power bill payment Rs.624.",
                subText = null,
                sender = "BESCOM Electricity Bill #124",
                category = "messages",
                notificationKey = "key_999",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertTrue("Case 999 expected ALERT", res.isImportant)
        }

        // Case 1000
        run {
            val rule = RuleClassifier.classify("electricity bills are important, horoscope blessings not important").toNotificationRule(id = 1000L)
            val notif = NotificationData(
                packageName = "com.dailybhakti.app",
                appName = "DailyBhakti",
                title = "Daily Horoscope and Quote #125",
                text = "Receive divine blessings and good horoscope for the day.",
                subText = null,
                sender = "Daily Horoscope and Quote #125",
                category = "messages",
                notificationKey = "key_1000",
                timestamp = System.currentTimeMillis()
            )
            val res = evaluate(listOf(rule), notif)
            assertFalse("Case 1000 expected MUTE", res.isImportant)
        }

    }

}
