package com.example.llama.aichat.ai

import com.example.llama.aichat.data.JsonListHelper
import com.example.llama.aichat.data.NotificationRule

enum class RuleIntent {
    SIMPLE_CONTACT,      // Pure contact name match (e.g. "Madhu", "Arjun")
    SIMPLE_BLOCK,        // Pure contact block (e.g. "Block Bob", "Ignore Spammer")
    CONDITIONAL_CONTACT, // Person rule with topic/negation (e.g. "Arjun not movies", "Arjun only games", "Madhu not reels")
    TOPIC_FILTER,        // Domain / Topic rule (e.g. "Bank transactions and OTP", "Job interviews")
    APP_FILTER           // App-specific rule (e.g. "Slack P0 alerts", "Uber cab arrival")
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
    val semanticDepth: String = "AOT_FAST"
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
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}

object RuleClassifier {

    private val FUNCTIONAL_STOP_WORDS = setOf(
        "whatever", "messages", "message", "from", "any", "all", "every", "is", "are",
        "important", "alert", "priority", "urgent", "on", "in", "notification",
        "notifications", "to", "the", "and", "with", "for", "msg", "msgs",
        "sent", "by", "its", "it's", "it", "someone", "anyone", "everyone",
        "please", "be", "never", "not", "dont", "do", "ignore", "block", "blocked",
        "calls", "call", "text", "texts", "about", "related", "relating", "regarding",
        "if", "only", "when", "then", "which", "that", "this", "there", "their",
        "should", "would", "could", "must", "of", "an", "a", "or", "as", "me", "my",
        "tell", "notify", "update", "updates", "get", "give", "send", "sends", "share", "shares",
        "he", "she", "they", "him", "her",
        "app", "apps", "application", "applications", "channel", "channels", "group", "groups", "chat", "chats", "dm", "dms"
    )

    // Comprehensive Domain Semantic Knowledge Graph for Ahead-Of-Time (AOT) Synonym Expansion
    private val DOMAIN_SYNONYMS = mapOf(
        // Social Media, Reels & Videos
        "reel" to listOf("reel", "reels", "video", "videos", "clip", "clips", "instagram.com/reel", "shared a reel", "sent a reel", "watch reel"),
        "reels" to listOf("reel", "reels", "video", "videos", "clip", "clips", "instagram.com/reel", "shared a reel", "sent a reel", "watch reel"),
        "video" to listOf("video", "videos", "reel", "reels", "clip", "clips", "media", "youtube"),
        "videos" to listOf("video", "videos", "reel", "reels", "clip", "clips", "media", "youtube"),

        // Gaming & Sports
        "game" to listOf("game", "games", "gaming", "gamer", "cricket", "football", "soccer", "bgmi", "pubg", "cod", "valorant", "fifa", "chess", "playstation", "xbox", "steam", "nintendo", "esports", "tournament", "match", "raid", "scrims", "discord"),
        "games" to listOf("game", "games", "gaming", "gamer", "cricket", "football", "soccer", "bgmi", "pubg", "cod", "valorant", "fifa", "chess", "playstation", "xbox", "steam", "nintendo", "esports", "tournament", "match", "raid", "scrims", "discord"),
        "gaming" to listOf("game", "games", "gaming", "gamer", "cricket", "football", "soccer", "bgmi", "pubg", "cod", "valorant", "fifa", "chess", "steam", "playstation", "xbox", "raid", "match"),
        "cricket" to listOf("cricket", "ipl", "wicket", "batsman", "bowler", "pitch", "overs"),
        "football" to listOf("football", "soccer", "fifa", "goal", "penalty", "premier league", "champions league"),

        // Movies & Entertainment
        "movie" to listOf("movie", "movies", "cinema", "film", "films", "trailer", "theatre", "theater", "showtime", "screen", "actor", "actress", "netflix", "hotstar", "prime video", "ott", "blockbuster", "series", "episode", "season", "box office", "cinemas", "teaser", "premiere"),
        "movies" to listOf("movie", "movies", "cinema", "film", "films", "trailer", "theatre", "theater", "showtime", "screen", "actor", "actress", "netflix", "hotstar", "prime video", "ott", "blockbuster", "series", "episode", "season", "box office", "cinemas", "teaser", "premiere"),
        "cinema" to listOf("movie", "movies", "cinema", "film", "films", "theatre", "theater", "showtime", "screen", "tickets"),
        "film" to listOf("movie", "movies", "cinema", "film", "films", "trailer", "theatre", "theater", "ott", "netflix"),
        "trailer" to listOf("trailer", "teaser", "preview", "promo"),

        // Banking, Money, UPI & Finance
        "bank" to listOf("bank", "banking", "otp", "debit", "debited", "credit", "credited", "refund", "refunded", "upi", "transfer", "transferred", "payment", "paid", "amount", "balance", "rs", "inr", "account", "acct", "atm", "withdrawal", "withdrawn", "salary", "txn", "transaction", "statement", "emi", "interest", "neft", "rtgs", "imps", "cred", "gpay", "phonepe", "paytm", "hdfc", "sbi", "icici", "axis", "kotak"),
        "banking" to listOf("bank", "banking", "otp", "debit", "debited", "credit", "credited", "refund", "upi", "transfer", "payment", "paid", "balance", "rs", "inr", "account", "txn", "transaction"),
        "transaction" to listOf("transaction", "txn", "debited", "credited", "transfer", "transferred", "payment", "paid", "amount", "rs", "inr", "account", "upi", "refund", "withdrawal", "spent", "received"),
        "transactions" to listOf("transaction", "txn", "debited", "credited", "transfer", "transferred", "payment", "paid", "amount", "rs", "inr", "account", "upi", "refund", "withdrawal", "spent", "received"),
        "money" to listOf("money", "amount", "rs", "inr", "rupees", "debited", "credited", "transfer", "transferred", "received", "sent", "paid", "payment", "balance", "refund"),
        "otp" to listOf("otp", "verification code", "one time password", "security code", "secret code", "valid for", "do not share", "auth code", "login code"),
        "salary" to listOf("salary", "credited", "payroll", "stipend", "wages", "bonus", "earnings"),
        "refund" to listOf("refund", "refunded", "reversal", "credited back", "cashback credited", "returned to account"),

        // Food Delivery & Quick Commerce
        "food" to listOf("food", "swiggy", "zomato", "blinkit", "zepto", "instamart", "bigbasket", "delivery", "delivered", "delivering", "order", "ordered", "rider", "driver", "courier", "doorstep", "picked up", "reaching", "out for delivery", "dispatched", "track your order", "restaurant", "kitchen", "meal", "biryani", "pizza", "groceries"),
        "delivery" to listOf("delivery", "delivered", "delivering", "order", "ordered", "out for delivery", "arriving", "arrived", "doorstep", "rider", "driver", "courier", "picked up", "reaching", "dispatched", "shipment", "package", "parcel"),
        "order" to listOf("order", "orders", "ordered", "delivery", "delivered", "arriving", "out for delivery", "picked up", "rider", "dispatched", "tracking", "order confirmed"),
        "orders" to listOf("order", "orders", "ordered", "delivery", "delivered", "arriving", "out for delivery", "picked up", "rider", "dispatched", "tracking", "order confirmed"),
        "grocery" to listOf("grocery", "groceries", "blinkit", "zepto", "instamart", "bigbasket", "order", "delivery", "delivered", "doorstep"),

        // E-Commerce & Couriers
        "package" to listOf("package", "parcel", "shipment", "shipped", "out for delivery", "delivered", "courier", "delhivery", "bluedart", "amazon", "flipkart", "tracking", "arriving today"),
        "parcel" to listOf("parcel", "package", "shipment", "shipped", "out for delivery", "delivered", "courier", "delhivery", "bluedart", "tracking"),
        "courier" to listOf("courier", "parcel", "package", "shipment", "shipped", "out for delivery", "delivered", "tracking", "awb"),

        // Jobs, Recruiting & Careers
        "job" to listOf("job", "jobs", "interview", "interviews", "recruiter", "recruitment", "hiring", "hr", "offer", "offer letter", "shortlisted", "assessment", "resume", "cv", "application", "career", "vacancy", "referral", "technical round", "salary discussion", "onboarding", "interview call", "hired"),
        "jobs" to listOf("job", "jobs", "interview", "interviews", "recruiter", "recruitment", "hiring", "hr", "offer", "offer letter", "shortlisted", "assessment", "resume", "cv", "application", "career", "vacancy", "referral"),
        "interview" to listOf("interview", "interviews", "recruiter", "hiring", "hr", "offer", "shortlisted", "assessment", "technical round", "managerial round", "coding test", "interview call", "schedule interview"),
        "interviews" to listOf("interview", "interviews", "recruiter", "hiring", "hr", "offer", "shortlisted", "assessment", "technical round", "interview call"),
        "recruiter" to listOf("recruiter", "recruitment", "talent acquisition", "hr", "hiring", "job opportunity", "interview", "offer"),

        // Travel, Rides, Flights, Trains
        "cab" to listOf("cab", "cabs", "uber", "ola", "rapido", "ride", "driver", "arrived", "reaching", "otp", "pin", "pickup", "drop"),
        "ride" to listOf("ride", "uber", "ola", "rapido", "driver", "arrived", "reaching", "otp", "pin", "pickup"),
        "flight" to listOf("flight", "airline", "boarding", "gate", "delayed", "departure", "arrival", "indigo", "air india", "vistara", "akasa", "terminal", "boarding pass", "pnr"),
        "train" to listOf("train", "irctc", "pnr", "coach", "berth", "seat", "platform", "departure", "chart prepared", "train status"),

        // Work, Incidents, DevOps & Tech
        "incident" to listOf("incident", "outage", "downtime", "p0", "p1", "p2", "sev-1", "sev-2", "server down", "crash", "alert", "pagerduty", "datadog", "sentry", "alertmanager", "production", "latency spike"),
        "outage" to listOf("outage", "downtime", "incident", "p0", "p1", "server down", "production down", "crash"),
        "downtime" to listOf("downtime", "outage", "incident", "server down", "production", "crash", "p0", "p1"),
        "p0" to listOf("p0", "sev-1", "critical incident", "production down", "outage", "downtime"),
        "p1" to listOf("p1", "sev-2", "high priority incident", "major outage"),
        "pr" to listOf("pr", "pull request", "pr review", "code review", "github", "gitlab", "pipeline failed"),

        // Bills & Utilities
        "bill" to listOf("bill", "bills", "due date", "overdue", "electricity", "power", "bescom", "water bill", "gas cylinder", "challan", "fine", "penalty", "tax due"),
        "bills" to listOf("bill", "bills", "due date", "overdue", "electricity", "water", "gas", "challan", "penalty", "tax"),
        "electricity" to listOf("electricity", "power", "power cut", "bescom", "tneb", "tssspdcl", "bill", "due date", "disconnection"),
        "challan" to listOf("challan", "traffic fine", "penalty", "fine", "mparivahan", "echallan", "violation"),

        // Promotions, Marketing, Spam (For Filtering / Exclusions)
        "promotions" to listOf("promotions", "promotional", "pre-approved", "scratch card", "deals", "advertisement", "newsletter", "save big", "special deal", "hurry up", "limited time offer", "discount", "discounts", "coupon", "cashback voucher", "claim offer", "viewed your profile", "appeared in searches", "congratulate"),
        "promotional" to listOf("promotions", "promotional", "pre-approved", "scratch card", "deals", "advertisement", "newsletter", "save big", "discount", "discounts", "coupon", "cashback voucher", "claim offer"),
        "offers" to listOf("offers", "offer", "discount", "discounts", "sale", "coupon", "cashback", "flat 50%", "deals", "scratch card", "pre-approved", "promo code"),
        "offer" to listOf("offer", "offers", "discount", "discounts", "sale", "coupon", "cashback", "scratch card", "pre-approved", "promo code"),
        "discount" to listOf("discount", "discounts", "flat 50%", "flat 60%", "flat 40%", "flat 70%", "coupon", "cashback", "promo code", "save up to"),
        "discounts" to listOf("discount", "discounts", "flat 50%", "flat 60%", "flat 40%", "flat 70%", "coupon", "cashback", "promo code", "save up to"),
        "sales" to listOf("sale", "clearance sale", "mega sale", "flash sale", "lightning deal", "discount", "coupon", "special offer"),
        "sale" to listOf("sale", "clearance sale", "mega sale", "flash sale", "lightning deal", "discount", "coupon", "special offer"),
        "marketing" to listOf("marketing", "promotional", "advertisement", "deals", "spam", "newsletter"),
        "spam" to listOf("spam", "promotional", "viewed your profile", "appeared in searches", "daily quote", "blessings", "horoscope", "astrology", "reels you may like", "suggested for you", "congratulate")
    )

    private val KNOWN_APP_KEYWORDS = mapOf(
        "whatsapp" to listOf("whatsapp", "com.whatsapp", "com.whatsapp.w4b"),
        "instagram" to listOf("instagram", "com.instagram.android"),
        "telegram" to listOf("telegram", "org.telegram.messenger", "org.telegram.plus"),
        "slack" to listOf("slack", "com.Slack"),
        "teams" to listOf("teams", "microsoft teams", "com.microsoft.teams"),
        "pagerduty" to listOf("pagerduty", "com.pagerduty.android"),
        "datadog" to listOf("datadog", "com.datadog.android"),
        "github" to listOf("github", "com.github.android"),
        "jira" to listOf("jira", "com.atlassian.jira.mobile"),
        "discord" to listOf("discord", "com.discord"),
        "outlook" to listOf("outlook", "com.microsoft.office.outlook"),
        "linkedin" to listOf("linkedin", "com.linkedin.android"),
        "gmail" to listOf("gmail", "google mail", "com.google.android.gm"),
        "phonepe" to listOf("phonepe", "com.phonepe.app"),
        "gpay" to listOf("gpay", "google pay", "com.google.android.apps.nbu.paisa.user"),
        "paytm" to listOf("paytm", "net.one97.paytm"),
        "cred" to listOf("cred", "com.dreamplug.androidapp"),
        "swiggy" to listOf("swiggy", "in.swiggy.android"),
        "zomato" to listOf("zomato", "com.application.zomato"),
        "blinkit" to listOf("blinkit", "com.grofers.customerapp"),
        "zepto" to listOf("zepto", "com.zepto.app"),
        "instamart" to listOf("instamart", "in.swiggy.android"),
        "bigbasket" to listOf("bigbasket", "com.bigbasket.mobileapp"),
        "amazon" to listOf("amazon", "in.amazon.mShop.android.shopping"),
        "flipkart" to listOf("flipkart", "com.flipkart.android"),
        "myntra" to listOf("myntra", "com.myntra.android"),
        "uber" to listOf("uber", "com.ubercab"),
        "ola" to listOf("ola", "com.olacabs.customer"),
        "rapido" to listOf("rapido", "com.rapido.passenger"),
        "irctc" to listOf("irctc", "cris.org.in.prs.ima"),
        "makemytrip" to listOf("makemytrip", "com.makemytrip"),
        "goibibo" to listOf("goibibo", "com.goibibo"),
        "bookmyshow" to listOf("bookmyshow", "com.bt.bms"),
        "hotstar" to listOf("hotstar", "disney+ hotstar", "in.startv.hotstar"),
        "netflix" to listOf("netflix", "com.netflix.mediaclient"),
        "youtube" to listOf("youtube", "com.google.android.youtube"),
        "spotify" to listOf("spotify", "com.spotify.music")
    )

    val EMOTION_AND_TONE_KEYWORDS = setOf(
        "angry", "furious", "mad", "frustrated", "annoyed", "sarcastic", "sarcasm", "joke", "jokes",
        "funny", "humor", "serious", "sad", "crying", "depressed", "happy", "excited", "rude",
        "aggressive", "urgent tone", "urgency", "emergency", "crisis", "bad news", "good news",
        "scam", "suspicious", "phishing", "fight", "quarrel", "abusive", "abuse", "mood", "feeling"
    )

    fun getEmotionTriggers(ruleText: String): List<String> {
        val lower = ruleText.lowercase()
        val ruleTokens = lower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.isNotBlank() }.toSet()
        return EMOTION_AND_TONE_KEYWORDS.filter { trigger ->
            if (trigger.contains(" ")) lower.contains(trigger) else ruleTokens.contains(trigger)
        }
    }

    fun classify(ruleText: String): ParsedRule {
        val lower = ruleText.lowercase().trim()
        val ruleTokens = lower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.isNotBlank() }.toSet()

        // Detect if rule requires on-device LLM deep reasoning for emotion/tone/subjectivity (word boundary match)
        val isDeepReasoningRequired = getEmotionTriggers(lower).isNotEmpty()
        val semanticDepth = if (isDeepReasoningRequired) "K2_DEEP" else "AOT_FAST"

        // Detect if rule has positive intent (e.g. "from madhu is important", "alert for arjun")
        val hasPositiveClause = lower.contains("is important") || lower.contains("are important") ||
                lower.contains("it is important") || lower.contains("alert") ||
                lower.contains("priority") || lower.contains("urgent") ||
                lower.contains("notify me") || lower.contains("tell me")

        val isExplicitNegative = lower.startsWith("block ") || lower.startsWith("ignore ") ||
            lower.startsWith("mute ") || lower.startsWith("never alert") ||
            lower.startsWith("do not alert") || lower.startsWith("dont alert") ||
            lower.startsWith("no alert") || lower.endsWith("not important") ||
            lower.endsWith("never important") || lower.startsWith("ignore all ") ||
            lower.startsWith("mute all ")

        val isPureNegative = !hasPositiveClause && !isDeepReasoningRequired && isExplicitNegative

        val action = if (isPureNegative) "MUTE" else "ALERT"

        // 1. Extract Target Apps
        val targetApps = mutableSetOf<String>()
        for ((appName, identifiers) in KNOWN_APP_KEYWORDS) {
            if (lower.contains(appName)) {
                targetApps.addAll(identifiers)
            }
        }

        // 2. Extract Target Person
        val explicitTarget = extractTargetPerson(lower)
        val targetName = if (explicitTarget != null) {
            explicitTarget
        } else {
            // Check if user entered strictly a contact name (e.g. "Madhu", "Arjun_Vasireddy", "ignore Madhu", "block Bob")
            val nonStopTokens = lower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 && it !in FUNCTIONAL_STOP_WORDS && it !in KNOWN_APP_KEYWORDS.keys }
            val totalTokens = lower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 }
            if (nonStopTokens.size in 1..2 && totalTokens.size <= 3 && !hasGeneralTopicKeywords(nonStopTokens)) {
                nonStopTokens.joinToString(" ")
            } else {
                null
            }
        }

        val isPersonRule = targetName != null

        // 3. Extract Raw Excluded Anchors (Exceptions / Negations)
        val rawExcludedAnchors = extractExcludedAnchors(lower).toMutableSet()

        // Guarantee person name and apps are never treated as excluded topics
        if (targetName != null) {
            rawExcludedAnchors.remove(targetName.lowercase())
            val targetParts = targetName.lowercase().split(Regex("[^a-zA-Z0-9_]+"))
            rawExcludedAnchors.removeAll(targetParts.toSet())
        }
        rawExcludedAnchors.removeAll(KNOWN_APP_KEYWORDS.keys)

        // 4. Extract Positive Topic Anchors
        val allTokens = lower.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 3 }
        val targetTokens = targetName?.split(Regex("[^a-zA-Z0-9_]+"))?.toSet() ?: emptySet()
        val appTokens = targetApps.flatMap { it.split(Regex("[^a-zA-Z0-9_]+")) }.toSet()

        val rawPositiveAnchors = allTokens.filter { token ->
            token !in FUNCTIONAL_STOP_WORDS &&
            token !in targetTokens &&
            token !in rawExcludedAnchors &&
            token !in appTokens
        }.toSet()

        // If rule is purely negative (e.g. "whatever from arjun related to movies, never important"),
        // any topic anchor is an exclusion anchor!
        if (isPureNegative && rawPositiveAnchors.isNotEmpty()) {
            rawExcludedAnchors.addAll(rawPositiveAnchors)
        }

        val expandedExcludedTopics = expandTopicSet(rawExcludedAnchors)
        val finalPositiveAnchors = if (isPureNegative) emptySet() else rawPositiveAnchors
        val expandedPositiveTopics = expandTopicSet(finalPositiveAnchors)

        val hasCondition = expandedPositiveTopics.isNotEmpty() || expandedExcludedTopics.isNotEmpty()

        val intent = when {
            // Case 1: Pure block with no condition -> SIMPLE_BLOCK
            isPersonRule && isPureNegative && !hasCondition -> RuleIntent.SIMPLE_BLOCK

            // Case 2: Pure positive contact -> SIMPLE_CONTACT
            isPersonRule && !isPureNegative && !hasCondition -> RuleIntent.SIMPLE_CONTACT

            // Case 3: Person rule with condition/exceptions -> CONDITIONAL_CONTACT
            isPersonRule && hasCondition -> RuleIntent.CONDITIONAL_CONTACT

            // Case 4: App-specific rule with topics -> APP_FILTER
            targetApps.isNotEmpty() && !isPersonRule -> RuleIntent.APP_FILTER

            // Case 5: General topic rule -> TOPIC_FILTER
            else -> RuleIntent.TOPIC_FILTER
        }

        return ParsedRule(
            rawText = ruleText,
            intent = intent,
            targetPerson = targetName,
            action = action,
            targetApps = targetApps,
            positiveTopics = expandedPositiveTopics,
            excludedTopics = expandedExcludedTopics,
            isNegative = isPureNegative,
            semanticDepth = semanticDepth
        )
    }

    private fun hasGeneralTopicKeywords(tokens: List<String>): Boolean {
        val topicRoots = setOf(
            "bank", "otp", "debit", "credit", "money", "food", "order", "delivery",
            "job", "interview", "ride", "cab", "flight", "train", "bill", "p0", "pr",
            "game", "games", "movie", "movies", "slack", "teams", "swiggy", "uber", "reel", "reels"
        )
        return tokens.any { it in topicRoots }
    }

    private fun expandTopicSet(rawTopics: Set<String>): Set<String> {
        val expanded = mutableSetOf<String>()
        for (topic in rawTopics) {
            val clean = topic.lowercase().trim()
            expanded.add(clean)
            val synonyms = DOMAIN_SYNONYMS[clean]
                ?: DOMAIN_SYNONYMS[clean.removeSuffix("s")]
                ?: DOMAIN_SYNONYMS[clean + "s"]
            if (synonyms != null) {
                expanded.addAll(synonyms)
            }
        }
        return expanded
    }

    private fun extractTargetPerson(lowerText: String): String? {
        val afterFrom = when {
            lowerText.contains("from ") -> lowerText.substringAfter("from ")
            lowerText.contains("msg from ") -> lowerText.substringAfter("msg from ")
            lowerText.contains("message from ") -> lowerText.substringAfter("message from ")
            lowerText.contains("sent by ") -> lowerText.substringAfter("sent by ")
            lowerText.contains("by ") -> lowerText.substringAfter("by ")
            lowerText.contains("calls from ") -> lowerText.substringAfter("calls from ")
            lowerText.contains("call from ") -> lowerText.substringAfter("call from ")
            lowerText.contains("if ") && lowerText.contains(" msg") -> {
                val candidate = lowerText.substringAfter("if ").substringBefore(" msg").trim()
                if (candidate.length in 2..25 && candidate !in FUNCTIONAL_STOP_WORDS) candidate else null
            }
            lowerText.contains("if ") && lowerText.contains(" sends") -> {
                val candidate = lowerText.substringAfter("if ").substringBefore(" sends").trim()
                if (candidate.length in 2..25 && candidate !in FUNCTIONAL_STOP_WORDS) candidate else null
            }
            lowerText.contains("if ") && lowerText.contains(" messages") -> {
                val candidate = lowerText.substringAfter("if ").substringBefore(" messages").trim()
                if (candidate.length in 2..25 && candidate !in FUNCTIONAL_STOP_WORDS) candidate else null
            }
            else -> null
        } ?: return null

        val rawAfterTokens = afterFrom.split(Regex("[^a-zA-Z0-9_]+")).filter { it.isNotBlank() }
        if (rawAfterTokens.isNotEmpty()) {
            val firstToken = rawAfterTokens.first()
            if (firstToken in KNOWN_APP_KEYWORDS.keys || firstToken in FUNCTIONAL_STOP_WORDS) {
                return null
            }
        }

        val tokens = afterFrom.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 && it !in FUNCTIONAL_STOP_WORDS && it !in KNOWN_APP_KEYWORDS.keys }
        val candidate = tokens.firstOrNull()
        return if (candidate != null && !hasGeneralTopicKeywords(listOf(candidate))) candidate else null
    }

    private fun extractExcludedAnchors(lowerText: String): Set<String> {
        // If it's a simple block like "ignore Bob" or "block Arjun", do not treat the person as an excluded topic anchor
        if (lowerText.startsWith("ignore ") || lowerText.startsWith("block ") || lowerText.startsWith("mute ")) {
            val nonStopTokens = lowerText.split(Regex("[^a-zA-Z0-9_]+")).filter { it.length >= 2 && it !in FUNCTIONAL_STOP_WORDS }
            if (nonStopTokens.size <= 2 && !hasGeneralTopicKeywords(nonStopTokens)) {
                return emptySet()
            }
        }

        val patterns = listOf(
            "not related to ", "not about ", "except about ", "except for ", "except ",
            "excluding ", "unless about ", "unless it is ", "unless ", "other than ",
            "ignore ", "mute ", "filter out ", "without "
        )

        val result = mutableSetOf<String>()

        for (p in patterns) {
            if (lowerText.contains(p)) {
                val after = lowerText.substringAfter(p)
                val cleanClause = after.substringBefore(",").substringBefore(".").substringBefore(" is ").substringBefore(" but ").trim()

                // Preserve compound phrases
                if (cleanClause.contains("credit card")) {
                    result.add("credit card")
                    result.add("card offer")
                }
                if (cleanClause.contains("pre-approved") || cleanClause.contains("pre approved")) {
                    result.add("pre-approved")
                }

                val tokens = cleanClause.split(Regex("[^a-zA-Z0-9_]+")).filter { 
                    it.length >= 3 && it !in FUNCTIONAL_STOP_WORDS && it != "credit" && it != "card"
                }
                result.addAll(tokens)
                if (result.isNotEmpty()) {
                    return result
                }
            }
        }

        // Secondary negation clauses: "... if he/she sends reels it is not important" or "... reels is not important"
        if (lowerText.contains("not important") || lowerText.contains("never important") || lowerText.contains("no alert")) {
            val negationPrefix = when {
                lowerText.contains("not important") -> lowerText.substringBefore("not important")
                lowerText.contains("never important") -> lowerText.substringBefore("never important")
                lowerText.contains("no alert") -> lowerText.substringBefore("no alert")
                else -> ""
            }
            val lastSegment = negationPrefix.substringAfterLast(",").substringAfterLast(" if ").substringAfterLast(" but ").trim()
            val tokens = lastSegment.split(Regex("[^a-zA-Z0-9_]+")).filter {
                it.length >= 3 && it !in FUNCTIONAL_STOP_WORDS
            }
            result.addAll(tokens)
        }

        return result
    }
}
