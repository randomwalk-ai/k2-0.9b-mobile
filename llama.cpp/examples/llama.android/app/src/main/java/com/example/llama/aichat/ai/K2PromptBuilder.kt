package com.example.llama.aichat.ai

object K2PromptBuilder {

    fun buildPrompt(
        rules: List<String>,
        appName: String,
        packageName: String,
        title: String?,
        text: String?,
        sender: String?
    ): String {
        val safeApp = appName.ifBlank { "App" }
        val safeSender = if (!sender.isNullOrBlank() && sender != safeApp) sender else (title?.ifBlank { "N/A" } ?: "N/A")
        val safeText = text?.ifBlank { title ?: "" } ?: ""

        val rulesFormatted = if (rules.isEmpty()) {
            "- No active rules"
        } else {
            rules.mapIndexed { idx, r -> "${idx + 1}. ${r.trim()}" }.joinToString("\n")
        }

        return "<|im_start|>system\n" +
               "You are an on-device personal notification assistant. Your job is to classify incoming notifications strictly according to the active user rules.\n\n" +
               "Active User Rules:\n" +
               "$rulesFormatted\n\n" +
               "Classification Guidelines:\n" +
               "1. NEGATIVE / EXCLUSION RULES (HIGHEST PRIORITY): If a rule specifies conditions where notifications are not important or should be muted, and the notification matches those conditions, output \"important\": false, \"alert\": false.\n" +
               "2. TONE & EMOTION: Distinguish true hostility, conflict, or anger from friendly banter, humor, exaggeration, and informal remarks. Non-hostile interactions must not be classified as angry.\n" +
               "3. POSITIVE RULES: If the notification satisfies an active positive rule without triggering any exclusion condition, output \"important\": true, \"alert\": true.\n" +
               "4. DEFAULT: If no rule matches, output \"important\": false, \"alert\": false, \"reason\": \"No matching rule\".\n\n" +
               "Output ONLY a single JSON object in the exact format:\n" +
               "{\"important\": true/false, \"alert\": true/false, \"reason\": \"brief explanation\", \"category\": \"messages/work/banking/delivery/other\"}\n" +
               "<|im_end|>\n" +
               "<|im_start|>user\n" +
               "Incoming Notification:\n" +
               "- App: $safeApp ($packageName)\n" +
               "- Sender: $safeSender\n" +
               "- Message: $safeText\n\n" +
               "Classify this notification in JSON:\n" +
               "<|im_end|>\n" +
               "<|im_start|>assistant\n" +
               "{"
    }

    fun buildTonePrompt(
        ruleText: String,
        targetTones: List<String>,
        appName: String,
        packageName: String,
        title: String?,
        text: String?,
        sender: String?
    ): String {
        val safeApp = appName.ifBlank { "App" }
        val safeSender = if (!sender.isNullOrBlank() && sender != safeApp) sender else (title?.ifBlank { "N/A" } ?: "N/A")
        val safeText = text?.ifBlank { title ?: "" } ?: ""
        val toneDescription = if (targetTones.isNotEmpty()) targetTones.joinToString(", ") else "emotion / tone condition in the rule"

        return "<|im_start|>system\n" +
               "You are an on-device emotion and tone analyzer. Determine if the incoming message genuinely exhibits the specific emotional tone ($toneDescription).\n\n" +
               "Rule Context: ${ruleText.trim()}\n" +
               "Target Emotional Tone: $toneDescription\n\n" +
               "Guidelines:\n" +
               "1. Set \"tone_matched\": true ONLY if the sender is genuinely expressing $toneDescription (e.g. true hostility, conflict, or stated emotion).\n" +
               "2. Set \"tone_matched\": false if the sender is calm, positive, neutral, discussing normal work/tasks, or engaging in friendly casual banter.\n" +
               "3. Distinguish actual emotional hostility from harmless remarks and friendly hyperbole.\n\n" +
               "Output ONLY JSON in the exact format:\n" +
               "{\"tone_matched\": true/false, \"reason\": \"brief explanation\"}\n" +
               "<|im_end|>\n" +
               "<|im_start|>user\n" +
               "Sender: $safeSender ($safeApp)\n" +
               "Message: $safeText\n\n" +
               "Analyze tone in JSON:\n" +
               "<|im_end|>\n" +
               "<|im_start|>assistant\n" +
               "{"
    }

    fun buildRuleCompilationPrompt(rawRule: String): String {
        return "<|im_start|>system\n" +
               "You are an expert Edge-AI Rule Compiler. Your task is to analyze user-defined notification filtering rules and compile them into structured, deterministic execution schemas for on-device execution.\n\n" +
               "Rule Intent Types:\n" +
               "- \"APP_FILTER\": Rule applies to specific mobile applications (e.g. Teams, Slack, WhatsApp, Swiggy, Uber).\n" +
               "- \"TOPIC_FILTER\": Rule applies to specific topics/keywords across all apps (e.g. OTP, gaming, delivery, job interview, server outage).\n" +
               "- \"SIMPLE_CONTACT\": Rule applies to all messages from a specific person (e.g. \"Alice\", \"Charlie\").\n" +
               "- \"SIMPLE_BLOCK\": Rule completely mutes/blocks all messages from a person or app (e.g. \"Block Bob\", \"Ignore Spammer\").\n" +
               "- \"CONDITIONAL_CONTACT\": Rule applies to a person with specific topic restrictions or exclusions (e.g. \"Alice not memes\", \"Charlie except gaming\").\n" +
               "- \"CONDITIONAL_EMOTION\": Rule depends on the sender's emotional state or tone (e.g. \"David when angry is not important\", \"Notify if boss sounds furious\").\n\n" +
               "Execution Engine Strategies:\n" +
               "- \"AOT_FAST\": Use for deterministic keyword, app, and contact matching that executes in <0.2ms with zero battery drain.\n" +
               "- \"K2_DEEP\": Use ONLY for rules requiring semantic emotion, tone, sentiment, or deep subjective context evaluation.\n\n" +
               "Field Specification:\n" +
               "1. \"rule_type\": One of [\"APP_FILTER\", \"TOPIC_FILTER\", \"SIMPLE_CONTACT\", \"SIMPLE_BLOCK\", \"CONDITIONAL_CONTACT\", \"CONDITIONAL_EMOTION\"]\n" +
               "2. \"execution_engine\": \"AOT_FAST\" or \"K2_DEEP\"\n" +
               "3. \"target_person\": Lowercase name of the targeted person, or null if none.\n" +
               "4. \"target_apps\": Array of target app names in lowercase (e.g. [\"teams\"], [\"slack\"]), or [] if none.\n" +
               "5. \"action\": \"ALERT\" (makes matching notifications important) or \"MUTE\" (suppresses matching notifications).\n" +
               "6. \"positive_topics\": Array of core keywords/topics that should trigger alerts. Strip all conversational filler words (e.g. \"playing\", \"messaged\", \"talking\", \"someone\", \"one\", \"everything\", \"any\", \"messages\").\n" +
               "7. \"excluded_topics\": Array of keywords that should be excluded/muted (e.g. [\"memes\", \"promotions\", \"casual\"]).\n" +
               "8. \"semantic_condition\": Short description of emotional/subjective condition (e.g. \"sender is angry or hostile\"), or null if none.\n" +
               "9. \"summary\": Clean one-line summary of the compiled rule.\n\n" +
               "Few-Shot Examples:\n" +
               "Rule: \"Teams is important\"\n" +
               "Output: {\"rule_type\": \"APP_FILTER\", \"execution_engine\": \"AOT_FAST\", \"target_person\": null, \"target_apps\": [\"teams\"], \"action\": \"ALERT\", \"positive_topics\": [], \"excluded_topics\": [], \"semantic_condition\": null, \"summary\": \"Alert all notifications from Teams\"}\n\n" +
               "Rule: \"if any one messaged about playing games it is important\"\n" +
               "Output: {\"rule_type\": \"TOPIC_FILTER\", \"execution_engine\": \"AOT_FAST\", \"target_person\": null, \"target_apps\": [], \"action\": \"ALERT\", \"positive_topics\": [\"games\", \"gaming\", \"game\", \"esports\"], \"excluded_topics\": [], \"semantic_condition\": null, \"summary\": \"Alert messages about games and gaming\"}\n\n" +
               "Rule: \"any message from Alice is important\"\n" +
               "Output: {\"rule_type\": \"SIMPLE_CONTACT\", \"execution_engine\": \"AOT_FAST\", \"target_person\": \"alice\", \"target_apps\": [], \"action\": \"ALERT\", \"positive_topics\": [], \"excluded_topics\": [], \"semantic_condition\": null, \"summary\": \"Alert all messages from Alice\"}\n\n" +
               "Rule: \"ignore messages from Bob\"\n" +
               "Output: {\"rule_type\": \"SIMPLE_BLOCK\", \"execution_engine\": \"AOT_FAST\", \"target_person\": \"bob\", \"target_apps\": [], \"action\": \"MUTE\", \"positive_topics\": [], \"excluded_topics\": [], \"semantic_condition\": null, \"summary\": \"Mute all messages from Bob\"}\n\n" +
               "Rule: \"David when angry is not important\"\n" +
               "Output: {\"rule_type\": \"CONDITIONAL_EMOTION\", \"execution_engine\": \"K2_DEEP\", \"target_person\": \"david\", \"target_apps\": [], \"action\": \"MUTE\", \"positive_topics\": [], \"excluded_topics\": [], \"semantic_condition\": \"sender is angry, mad, or furious\", \"summary\": \"Mute David when angry\"}\n\n" +
               "Rule: \"Charlie if he sends memes it is not important, otherwise important\"\n" +
               "Output: {\"rule_type\": \"CONDITIONAL_CONTACT\", \"execution_engine\": \"AOT_FAST\", \"target_person\": \"charlie\", \"target_apps\": [], \"action\": \"ALERT\", \"positive_topics\": [], \"excluded_topics\": [\"memes\", \"meme\", \"jokes\"], \"semantic_condition\": null, \"summary\": \"Alert messages from Charlie except memes\"}\n\n" +
               "Rule: \"Mute all promotional offers and discounts from Swiggy\"\n" +
               "Output: {\"rule_type\": \"APP_FILTER\", \"execution_engine\": \"AOT_FAST\", \"target_person\": null, \"target_apps\": [\"swiggy\"], \"action\": \"MUTE\", \"positive_topics\": [], \"excluded_topics\": [\"promotional\", \"offers\", \"discount\", \"discounts\", \"sale\", \"coupon\"], \"semantic_condition\": null, \"summary\": \"Mute promo offers from Swiggy\"}\n\n" +
               "Output ONLY a single valid JSON object.\n" +
               "<|im_end|>\n" +
               "<|im_start|>user\n" +
               "Compile this rule into JSON:\n" +
               "Rule: \"$rawRule\"\n" +
               "<|im_end|>\n" +
               "<|im_start|>assistant\n" +
               "{"
    }
}
