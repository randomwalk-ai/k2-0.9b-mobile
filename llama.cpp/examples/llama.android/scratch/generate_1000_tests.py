import os

def generate():
    lines = []
    lines.append("package com.example.llama.aichat")
    lines.append("")
    lines.append("import com.example.llama.aichat.ai.RuleClassifier")
    lines.append("import com.example.llama.aichat.ai.RuleIntent")
    lines.append("import com.example.llama.aichat.data.NotificationRule")
    lines.append("import com.example.llama.aichat.notification.NotificationData")
    lines.append("import org.junit.Assert.*")
    lines.append("import org.junit.Test")
    lines.append("")
    lines.append("class AotRuleEngine1000Test {")
    lines.append("")
    lines.append("    data class EvaluationResult(")
    lines.append("        val isImportant: Boolean,")
    lines.append("        val shouldAlert: Boolean,")
    lines.append("        val category: String,")
    lines.append("        val reason: String")
    lines.append("    )")
    lines.append("")
    lines.append("    private fun evaluate(rules: List<NotificationRule>, data: NotificationData): EvaluationResult {")
    lines.append("        if (rules.isEmpty()) {")
    lines.append("            return EvaluationResult(false, false, \"other\", \"No active user rules\")")
    lines.append("        }")
    lines.append("        val senderLower = data.sender?.lowercase()?.trim() ?: \"\"")
    lines.append("        val titleLower = data.title?.lowercase()?.trim() ?: \"\"")
    lines.append("        val textLower = data.text?.lowercase()?.trim() ?: \"\"")
    lines.append("        val appLower = data.appName.lowercase().trim()")
    lines.append("        val packageLower = data.packageName.lowercase().trim()")
    lines.append("        val contentLower = \"$senderLower $titleLower $textLower $appLower $packageLower\"")
    lines.append("")
    lines.append("        var hasPositiveMatch = false")
    lines.append("        var hasExplicitExclusion = false")
    lines.append("        var exclusionReason = \"\"")
    lines.append("        var positiveReason = \"\"")
    lines.append("")
    lines.append("        for (rule in rules) {")
    lines.append("            val targetPerson = rule.targetPerson")
    lines.append("            val action = rule.action")
    lines.append("            val positiveTopics = rule.getPositiveTopics()")
    lines.append("            val excludedTopics = rule.getExcludedTopics()")
    lines.append("            val targetApps = rule.getTargetApps()")
    lines.append("")
    lines.append("            val isPersonRule = !targetPerson.isNullOrBlank()")
    lines.append("            val isAppRule = targetApps.isNotEmpty()")
    lines.append("")
    lines.append("            val appMatch = if (isAppRule) {")
    lines.append("                targetApps.any { targetApp ->")
    lines.append("                    packageLower.contains(targetApp.lowercase()) || appLower.contains(targetApp.lowercase())")
    lines.append("                }")
    lines.append("            } else false")
    lines.append("")
    lines.append("            if (isAppRule && !appMatch) {")
    lines.append("                val hasTopicMatch = positiveTopics.any { textMatchesDynamicAnchor(contentLower, it) }")
    lines.append("                if (!hasTopicMatch) continue")
    lines.append("            }")
    lines.append("")
    lines.append("            val senderMatches = isPersonRule && isSenderMatch(targetPerson, data)")
    lines.append("            if (isPersonRule && !senderMatches) continue")
    lines.append("")
    lines.append("            val matchesExclusion = excludedTopics.isNotEmpty() && excludedTopics.any { anchor ->")
    lines.append("                textMatchesDynamicAnchor(contentLower, anchor)")
    lines.append("            }")
    lines.append("            if (matchesExclusion) {")
    lines.append("                hasExplicitExclusion = true")
    lines.append("                exclusionReason = \"Filtered exclusion\"")
    lines.append("                break")
    lines.append("            }")
    lines.append("")
    lines.append("            if (action.equals(\"MUTE\", ignoreCase = true) || rule.ruleIntent == \"SIMPLE_BLOCK\") {")
    lines.append("                if (isPersonRule && senderMatches) {")
    lines.append("                    hasExplicitExclusion = true")
    lines.append("                    exclusionReason = \"Muted sender\"")
    lines.append("                    break")
    lines.append("                } else if (isAppRule && !isPersonRule && appMatch) {")
    lines.append("                    hasExplicitExclusion = true")
    lines.append("                    exclusionReason = \"Muted app\"")
    lines.append("                    break")
    lines.append("                }")
    lines.append("            }")
    lines.append("")
    lines.append("            val matchesPositiveTopic = positiveTopics.isNotEmpty() && positiveTopics.any { anchor ->")
    lines.append("                textMatchesDynamicAnchor(contentLower, anchor)")
    lines.append("            }")
    lines.append("            val isPureContact = isPersonRule && positiveTopics.isEmpty() && excludedTopics.isEmpty()")
    lines.append("            val isPureApp = isAppRule && !isPersonRule && positiveTopics.isEmpty() && excludedTopics.isEmpty()")
    lines.append("")
    lines.append("            if (isPureContact && senderMatches) {")
    lines.append("                hasPositiveMatch = true")
    lines.append("                positiveReason = \"Matched contact\"")
    lines.append("            } else if (isPureApp && appMatch) {")
    lines.append("                hasPositiveMatch = true")
    lines.append("                positiveReason = \"Matched app\"")
    lines.append("            } else if (matchesPositiveTopic) {")
    lines.append("                if (!isAppRule || appMatch) {")
    lines.append("                    hasPositiveMatch = true")
    lines.append("                    positiveReason = \"Matched topic\"")
    lines.append("                }")
    lines.append("            } else if (isPersonRule && senderMatches && excludedTopics.isNotEmpty() && !matchesExclusion) {")
    lines.append("                hasPositiveMatch = true")
    lines.append("                positiveReason = \"Matched contact with no exclusion\"")
    lines.append("            } else if (isAppRule && !isPersonRule && appMatch && excludedTopics.isNotEmpty() && !matchesExclusion) {")
    lines.append("                hasPositiveMatch = true")
    lines.append("                positiveReason = \"Matched app with no exclusion\"")
    lines.append("            }")
    lines.append("        }")
    lines.append("        val finalImportant = !hasExplicitExclusion && hasPositiveMatch")
    lines.append("        return EvaluationResult(finalImportant, finalImportant, if (finalImportant) \"important\" else \"other\", if (hasExplicitExclusion) exclusionReason else if (hasPositiveMatch) positiveReason else \"No match\")")
    lines.append("    }")
    lines.append("")
    lines.append("    private fun isSenderMatch(ruleTarget: String?, data: NotificationData): Boolean {")
    lines.append("        if (ruleTarget.isNullOrBlank()) return false")
    lines.append("        if (!data.sender.isNullOrBlank() && !data.sender.equals(data.appName, ignoreCase = true)) {")
    lines.append("            if (matchesPersonName(ruleTarget, data.sender)) return true")
    lines.append("        }")
    lines.append("        if (!data.title.isNullOrBlank() && !data.title.equals(data.appName, ignoreCase = true)) {")
    lines.append("            if (matchesPersonName(ruleTarget, data.title)) return true")
    lines.append("        }")
    lines.append("        val text = data.text?.trim()")
    lines.append("        if (!text.isNullOrBlank() && text.contains(':')) {")
    lines.append("            val possiblePrefix = text.substringBefore(':').trim()")
    lines.append("            if (possiblePrefix.length in 2..30 && !possiblePrefix.contains('\\n') && !possiblePrefix.contains('.')) {")
    lines.append("                if (matchesPersonName(ruleTarget, possiblePrefix)) return true")
    lines.append("            }")
    lines.append("        }")
    lines.append("        return false")
    lines.append("    }")
    lines.append("")
    lines.append("    private fun matchesPersonName(ruleTarget: String?, candidateName: String?): Boolean {")
    lines.append("        if (ruleTarget.isNullOrBlank() || candidateName.isNullOrBlank()) return false")
    lines.append("        val target = ruleTarget.lowercase().trim()")
    lines.append("        val targetClean = target.replace(\" \", \"\")")
    lines.append("        val candidate = candidateName.lowercase().trim()")
    lines.append("        val candidateClean = candidate.replace(\" \", \"\")")
    lines.append("        val candidateWords = candidate.split(Regex(\"[^a-zA-Z0-9_]+\")).filter { it.length >= 2 }")
    lines.append("        if (target.isEmpty() || candidate.isEmpty()) return false")
    lines.append("        if (candidateClean == targetClean) return true")
    lines.append("        for (word in candidateWords) {")
    lines.append("            val wordClean = word.replace(\"_\", \"\")")
    lines.append("            if (wordClean == targetClean || wordClean.startsWith(targetClean)) return true")
    lines.append("        }")
    lines.append("        val candidateNoUnderscore = candidateClean.replace(\"_\", \"\")")
    lines.append("        if (candidateNoUnderscore.startsWith(targetClean)) return true")
    lines.append("        if (targetClean.length >= 6 && candidateClean.contains(targetClean)) return true")
    lines.append("        return false")
    lines.append("    }")
    lines.append("")
    lines.append("    private fun textMatchesDynamicAnchor(content: String, anchor: String): Boolean {")
    lines.append("        val cleanAnchor = anchor.trim().lowercase()")
    lines.append("        if (cleanAnchor.length < 3) return false")
    lines.append("        val words = content.split(Regex(\"[^a-zA-Z0-9_]+\")).filter { it.length >= 2 }")
    lines.append("        return words.any { word ->")
    lines.append("            word == cleanAnchor ||")
    lines.append("            (word.length >= 4 && word.startsWith(cleanAnchor)) ||")
    lines.append("            (cleanAnchor.contains(\" \") && content.contains(cleanAnchor))")
    lines.append("        }")
    lines.append("    }")
    lines.append("")

    domains = [
        ("Dimension1_BankingAndFinance", "testBankingFinanceOtp125Cases", "banking"),
        ("Dimension2_FoodAndQuickCommerce", "testFoodDelivery125Cases", "delivery"),
        ("Dimension3_ECommerceAndCouriers", "testECommerceCouriers125Cases", "ecommerce"),
        ("Dimension4_WorkTeamsDevOpsJobs", "testWorkTeamsDevOps125Cases", "work"),
        ("Dimension5_TravelFlightsTransit", "testTravelFlightsTransit125Cases", "travel"),
        ("Dimension6_SocialGamingReels", "testSocialGamingReels125Cases", "social"),
        ("Dimension7_EmotionToneUrgency", "testEmotionToneDepth125Cases", "emotion"),
        ("Dimension8_UtilitiesBillsGovtNoise", "testUtilitiesGovtSpamNoise125Cases", "utilities")
    ]

    total_count = 0

    for domain_name, method_name, domain_type in domains:
        lines.append(f"    @Test")
        lines.append(f"    fun {method_name}() {{")
        lines.append(f"        // Domain: {domain_name} (125 Distinct Test Cases)")
        
        for i in range(1, 126):
            total_count += 1
            case_id = total_count
            
            if domain_type == "banking":
                rule_text = "bank transactions and otp are important"
                is_alert = (i % 5 != 0)
                if is_alert:
                    pkg = "com.phonepe.app" if i % 2 == 0 else "com.google.android.apps.nbu.paisa.user"
                    app = "PhonePe" if i % 2 == 0 else "GPay"
                    title = f"HDFC Bank OTP {i}" if i % 3 == 0 else f"Acct Debited Rs.{i * 150}"
                    text = f"Your secret OTP is {100000 + i}. Valid for 5 mins." if i % 3 == 0 else f"Rs.{i * 150} paid to Merchant_{i} via UPI."
                else:
                    pkg = "com.phonepe.app"
                    app = "PhonePe"
                    title = f"Flat 50% Cashback Deal {i}"
                    text = f"Claim your scratch card offer voucher today before it expires!"
                    rule_text = "bank transactions and otp are important, promotional discount not important"

            elif domain_type == "delivery":
                rule_text = "food delivery and grocery orders are important"
                is_alert = (i % 5 != 0)
                if is_alert:
                    pkg = "in.swiggy.android" if i % 2 == 0 else "com.grofers.customerapp"
                    app = "Swiggy" if i % 2 == 0 else "Blinkit"
                    title = f"Order #{10000 + i} Status"
                    text = f"Rider #{i} is out for delivery reaching your doorstep in 8 mins."
                else:
                    pkg = "in.swiggy.android"
                    app = "Swiggy"
                    title = f"Craving Pizza? 60% OFF"
                    text = f"Special sale discount on restaurant kitchen orders today only."
                    rule_text = "food delivery and grocery orders are important, discount sale not important"

            elif domain_type == "ecommerce":
                rule_text = "package delivery and courier shipment are important"
                is_alert = (i % 5 != 0)
                if is_alert:
                    pkg = "in.amazon.mShop.android.shopping" if i % 2 == 0 else "com.flipkart.android"
                    app = "Amazon" if i % 2 == 0 else "Flipkart"
                    title = f"Shipment #{20000 + i}"
                    text = f"Your parcel is arriving today with courier delivery rider."
                else:
                    pkg = "in.amazon.mShop.android.shopping"
                    app = "Amazon"
                    title = f"Mega Clearance Sale Day {i}"
                    text = f"Save big with lightning deal flat 70% off promo."
                    rule_text = "package delivery and courier shipment are important, clearance sale not important"

            elif domain_type == "work":
                is_alert = (i % 6 != 0)
                if is_alert:
                    pkg = "com.microsoft.teams" if i % 2 == 0 else "com.Slack"
                    app = "Teams" if i % 2 == 0 else "Slack"
                    title = f"Lead_Engineer_{i}"
                    text = f"P0 critical incident outage on production cluster node {i}."
                    rule_text = "any msg from teams app is important" if i % 2 == 0 else "slack p0 incidents are important"
                else:
                    pkg = "com.linkedin.android"
                    app = "LinkedIn"
                    title = f"Weekly Network Digest {i}"
                    text = f"Someone viewed your profile and appeared in 12 searches this week."
                    rule_text = "recruiter interview is important, marketing spam not important"

            elif domain_type == "travel":
                rule_text = "cab arrival and flight train bookings are important"
                is_alert = (i % 5 != 0)
                if is_alert:
                    pkg = "com.ubercab" if i % 2 == 0 else "cris.org.in.prs.ima"
                    app = "Uber" if i % 2 == 0 else "IRCTC"
                    title = f"Ride #{30000 + i}" if i % 2 == 0 else f"Train Status PNR_{i}"
                    text = f"Driver has arrived at pickup. OTP is {2000 + i}." if i % 2 == 0 else f"Platform 4 confirmed. Coach B{i%5+1} Berth {i%60+1}."
                else:
                    pkg = "com.makemytrip"
                    app = "MakeMyTrip"
                    title = f"Holiday Package Deal {i}"
                    text = f"Exclusive holiday discount coupon voucher on flight hotels."
                    rule_text = "cab arrival and flight train bookings are important, holiday deals not important"

            elif domain_type == "social":
                if i % 3 == 0:
                    rule_text = "if any message from Madhu it is important, if she sends reels it is not important"
                    pkg = "com.instagram.android"
                    app = "Instagram"
                    title = "Madhu"
                    if i % 6 == 0:
                        text = f"Check this out instagram.com/reel/{i}998"
                        is_alert = False
                    else:
                        text = f"Hey are you attending the meetup today #{i}?"
                        is_alert = True
                elif i % 3 == 1:
                    rule_text = "notify me if Arjun msg only about games"
                    pkg = "com.whatsapp"
                    app = "WhatsApp"
                    title = "Arjun_Vasireddy"
                    if i % 2 == 0:
                        text = f"Brother lets play cricket match tournament #{i} today"
                        is_alert = True
                    else:
                        text = f"Let us go to movie theatre screen #{i}"
                        is_alert = False
                else:
                    rule_text = "any msg from teams app is important"
                    pkg = "com.microsoft.teams"
                    app = "Teams"
                    title = f"Colleague_{i}"
                    text = f"Can we sync on the design document v{i}?"
                    is_alert = True

            elif domain_type == "emotion":
                rule_text = "any msg from Pranav when he is angry is not important"
                pkg = "com.microsoft.teams"
                app = "Teams"
                title = "Pranav Dhamodaran"
                text = f"Great sprint deliverable test #{i}"
                is_alert = True

            elif domain_type == "utilities":
                rule_text = "electricity bills and traffic challan are important"
                is_alert = (i % 5 != 0)
                if is_alert:
                    pkg = "com.phonepe.app"
                    app = "PhonePe"
                    title = f"BESCOM Electricity Bill #{i}"
                    text = f"Due date approaching for power bill payment Rs.{500 + i}."
                else:
                    pkg = "com.dailybhakti.app"
                    app = "DailyBhakti"
                    title = f"Daily Horoscope and Quote #{i}"
                    text = f"Receive divine blessings and good horoscope for the day."
                    rule_text = "electricity bills are important, horoscope blessings not important"

            lines.append(f"        // Case {case_id}")
            lines.append(f"        run {{")
            lines.append(f"            val rule = RuleClassifier.classify(\"{rule_text}\").toNotificationRule(id = {case_id}L)")
            if domain_type == "emotion":
                lines.append(f"            assertEquals(\"K2_DEEP\", rule.semanticDepth)")
                lines.append(f"            assertEquals(\"pranav\", rule.targetPerson)")
                lines.append(f"            assertFalse(rule.getExcludedTopics().contains(\"pranav\"))")
            lines.append(f"            val notif = NotificationData(")
            lines.append(f"                packageName = \"{pkg}\",")
            lines.append(f"                appName = \"{app}\",")
            lines.append(f"                title = \"{title}\",")
            lines.append(f"                text = \"{text}\",")
            lines.append(f"                subText = null,")
            lines.append(f"                sender = \"{title}\",")
            lines.append(f"                category = \"messages\",")
            lines.append(f"                notificationKey = \"key_{case_id}\",")
            lines.append(f"                timestamp = System.currentTimeMillis()")
            lines.append(f"            )")
            lines.append(f"            val res = evaluate(listOf(rule), notif)")
            if is_alert:
                lines.append(f"            assertTrue(\"Case {case_id} expected ALERT\", res.isImportant)")
            else:
                lines.append(f"            assertFalse(\"Case {case_id} expected MUTE\", res.isImportant)")
            lines.append(f"        }}")
            lines.append("")

        lines.append("    }")
        lines.append("")

    lines.append("}")
    lines.append("")

    target_path = "app/src/test/java/com/example/llama/aichat/AotRuleEngine1000Test.kt"
    with open(target_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print(f"Generated {total_count} test cases in {target_path}")

if __name__ == "__main__":
    generate()
