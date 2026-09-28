package com.example.llama.aichat

import com.example.llama.aichat.ai.K2PromptBuilder
import com.example.llama.aichat.ai.K2ResponseParser
import com.example.llama.aichat.ai.RuleClassifier
import com.example.llama.aichat.ai.RuleIntent
import com.example.llama.aichat.notification.NotificationData
import org.junit.Assert.*
import org.junit.Test

class K2AiTest {

    @Test
    fun testRuleClassifierSimpleContact() {
        val rule = RuleClassifier.classify("any message from Madhu is important")
        assertEquals(RuleIntent.SIMPLE_CONTACT, rule.intent)
        assertEquals("madhu", rule.targetPerson)
        assertTrue(rule.positiveTopics.isEmpty())
        assertFalse(rule.isNegative)
    }

    @Test
    fun testRuleClassifierSimpleBlock() {
        val rule = RuleClassifier.classify("ignore messages from Bob")
        assertEquals(RuleIntent.SIMPLE_BLOCK, rule.intent)
        assertEquals("bob", rule.targetPerson)
        assertTrue(rule.isNegative)
    }

    @Test
    fun testRuleClassifierConditionalPerson() {
        val rule = RuleClassifier.classify("whatever message from arjun related to movies, it is never important")
        assertEquals(RuleIntent.CONDITIONAL_CONTACT, rule.intent)
        assertEquals("arjun", rule.targetPerson)
        assertTrue(rule.excludedTopics.contains("movies") || rule.excludedTopics.contains("movie"))
        assertTrue(rule.isNegative)
    }

    @Test
    fun testRuleClassifierExceptionClause() {
        val rule = RuleClassifier.classify("Message from arjun not related to movies, is important")
        assertEquals(RuleIntent.CONDITIONAL_CONTACT, rule.intent)
        assertEquals("arjun", rule.targetPerson)
        assertTrue(rule.excludedTopics.contains("movies") || rule.excludedTopics.contains("movie"))
        assertFalse(rule.isNegative)
    }

    @Test
    fun testRuleClassifierJobTopic() {
        val rule = RuleClassifier.classify("if someone messages about job related it is important")
        assertEquals(RuleIntent.TOPIC_FILTER, rule.intent)
        assertNull(rule.targetPerson)
        assertTrue(rule.positiveTopics.contains("job") || rule.positiveTopics.contains("interview"))
        assertFalse(rule.isNegative)
    }

    @Test
    fun testRuleClassifierBusinessInvoicingRule() {
        val rule = RuleClassifier.classify("client payment confirmation or invoice is urgent")
        assertEquals(RuleIntent.TOPIC_FILTER, rule.intent)
        assertTrue(rule.positiveTopics.contains("payment") || rule.positiveTopics.contains("invoice") || rule.positiveTopics.contains("client"))
    }

    @Test
    fun testRuleClassifierTechDowntimeRule() {
        val rule = RuleClassifier.classify("server downtime alerts from pagerduty")
        assertEquals(RuleIntent.APP_FILTER, rule.intent)
        assertTrue(rule.positiveTopics.contains("server") || rule.positiveTopics.contains("downtime") || rule.positiveTopics.contains("outage"))
    }

    @Test
    fun testValidJsonResponseParsing() {
        val json = """
            {
              "important": true,
              "alert": true,
              "summary": "Rahul: Let's discuss the job opportunity",
              "reason": "Matches user's job-related rule",
              "category": "job"
            }
        """.trimIndent()

        val analysis = K2ResponseParser.parse(json)
        assertTrue(analysis.important)
        assertTrue(analysis.alert)
        assertEquals("Rahul: Let's discuss the job opportunity", analysis.summary)
        assertEquals("Matches user's job-related rule", analysis.reason)
        assertEquals("job", analysis.category)
    }

    @Test
    fun testUnimportantJsonResponseParsing() {
        val json = """
            {
              "important": false,
              "alert": false,
              "summary": "Arjun: Have you seen the new movie trailer?",
              "reason": "Filtered out by movie negative rule for Arjun",
              "category": "other"
            }
        """.trimIndent()

        val analysis = K2ResponseParser.parse(json)
        assertFalse(analysis.important)
        assertFalse(analysis.alert)
        assertEquals("Arjun: Have you seen the new movie trailer?", analysis.summary)
        assertEquals("other", analysis.category)
    }

    @Test
    fun testPromptBuilderFormatting() {
        val rules = listOf(
            "whatever message from arjun related to movies, it is never important",
            "if someone messages about job related it is important"
        )

        val prompt = K2PromptBuilder.buildPrompt(
            rules = rules,
            appName = "WhatsApp",
            packageName = "com.whatsapp",
            title = "Arjun",
            text = "Hey check out this new job opening at Google!",
            sender = "Arjun"
        )

        assertTrue(prompt.contains("WhatsApp"))
        assertTrue(prompt.contains("Arjun"))
        assertTrue(prompt.contains("job opening"))
        assertTrue(prompt.contains("whatever message from arjun related to movies, it is never important"))
        assertTrue(prompt.contains("if someone messages about job related it is important"))
        assertTrue(prompt.endsWith("<|im_start|>assistant\n{"))
    }

    @Test
    fun testCallFromPersonRuleClassification() {
        val rule1 = RuleClassifier.classify("call from Siddharth is important")
        assertEquals(RuleIntent.SIMPLE_CONTACT, rule1.intent)
        assertEquals("siddharth", rule1.targetPerson)
        assertFalse(rule1.isNegative)

        val rule2 = RuleClassifier.classify("any messages from Siddharth is important")
        assertEquals(RuleIntent.SIMPLE_CONTACT, rule2.intent)
        assertEquals("siddharth", rule2.targetPerson)
        assertFalse(rule2.isNegative)
    }

    @Test
    fun testSingleContactNameRuleClassification() {
        val rule1 = RuleClassifier.classify("Madhu")
        assertEquals(RuleIntent.SIMPLE_CONTACT, rule1.intent)
        assertEquals("madhu", rule1.targetPerson)
        assertTrue(rule1.positiveTopics.isEmpty())

        val rule2 = RuleClassifier.classify("ignore Madhu")
        assertEquals(RuleIntent.SIMPLE_BLOCK, rule2.intent)
        assertEquals("madhu", rule2.targetPerson)
        assertTrue(rule2.isNegative)
    }

    @Test
    fun testSenderMatchingDirectSender() {
        val dataFromArjun = NotificationData(
            packageName = "com.instagram.android",
            appName = "Instagram",
            title = "Arjun_Vasireddy",
            text = "Hi madhu",
            subText = null,
            sender = "Arjun_Vasireddy",
            category = "messages",
            notificationKey = "key_1",
            timestamp = System.currentTimeMillis()
        )

        val dataFromMadhu = NotificationData(
            packageName = "com.instagram.android",
            appName = "Instagram",
            title = "Madhu",
            text = "Hey are you free?",
            subText = null,
            sender = "Madhu",
            category = "messages",
            notificationKey = "key_2",
            timestamp = System.currentTimeMillis()
        )

        // Rule for Madhu should NOT match notification from Arjun even if Arjun writes "Hi madhu"
        val parsedRule = RuleClassifier.classify("any message from Madhu is important")
        assertEquals(RuleIntent.SIMPLE_CONTACT, parsedRule.intent)
        assertEquals("madhu", parsedRule.targetPerson)

        // Test candidate gate simulation
        assertFalse(isSenderMatchSimulated(parsedRule.targetPerson, dataFromArjun))
        assertTrue(isSenderMatchSimulated(parsedRule.targetPerson, dataFromMadhu))
    }

    @Test
    fun testSenderMatchingGroupChatCases() {
        val parsedRule = RuleClassifier.classify("any message from Madhu is important")

        // 1. Group chat where someone else (Arjun) talks about Madhu
        val groupMsgFromArjun = NotificationData(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "College Friends",
            text = "Did anyone see Madhu today?",
            subText = null,
            sender = "Arjun",
            category = "messages",
            notificationKey = "key_group_1",
            timestamp = System.currentTimeMillis()
        )
        assertFalse(isSenderMatchSimulated(parsedRule.targetPerson, groupMsgFromArjun))

        // 2. Group chat where Madhu is the sender
        val groupMsgFromMadhu = NotificationData(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "College Friends",
            text = "I will reach at 6pm",
            subText = null,
            sender = "Madhu",
            category = "messages",
            notificationKey = "key_group_2",
            timestamp = System.currentTimeMillis()
        )
        assertTrue(isSenderMatchSimulated(parsedRule.targetPerson, groupMsgFromMadhu))

        // 3. Text prefix group format ("Madhu: Let's start")
        val groupPrefixMsg = NotificationData(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Project Group",
            text = "Madhu: Let's start the call",
            subText = null,
            sender = null,
            category = "messages",
            notificationKey = "key_group_3",
            timestamp = System.currentTimeMillis()
        )
        assertTrue(isSenderMatchSimulated(parsedRule.targetPerson, groupPrefixMsg))
    }

    @Test
    fun testHandleMatchingVariations() {
        val ruleTarget = "arjun"
        assertTrue(matchesPersonNameSimulated(ruleTarget, "Arjun"))
        assertTrue(matchesPersonNameSimulated(ruleTarget, "Arjun_Vasireddy"))
        assertTrue(matchesPersonNameSimulated(ruleTarget, "Arjun Vasireddy"))
        assertTrue(matchesPersonNameSimulated(ruleTarget, "arjun.v"))
        assertFalse(matchesPersonNameSimulated(ruleTarget, "Madhu"))
        assertFalse(matchesPersonNameSimulated(ruleTarget, "Varun"))
    }

    private fun isSenderMatchSimulated(ruleTarget: String?, data: NotificationData): Boolean {
        if (ruleTarget.isNullOrBlank()) return false

        if (!data.sender.isNullOrBlank() && !data.sender.equals(data.appName, ignoreCase = true)) {
            if (matchesPersonNameSimulated(ruleTarget, data.sender)) {
                return true
            }
        }

        if (!data.title.isNullOrBlank() && !data.title.equals(data.appName, ignoreCase = true)) {
            if (matchesPersonNameSimulated(ruleTarget, data.title)) {
                return true
            }
        }

        val text = data.text?.trim()
        if (!text.isNullOrBlank() && text.contains(":")) {
            val possiblePrefix = text.substringBefore(":").trim()
            if (possiblePrefix.length in 2..30 && !possiblePrefix.contains("\n") && !possiblePrefix.contains(".")) {
                if (matchesPersonNameSimulated(ruleTarget, possiblePrefix)) {
                    return true
                }
            }
        }

        return false
    }

    private fun matchesPersonNameSimulated(ruleTarget: String?, candidateName: String?): Boolean {
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
            if (wordClean == targetClean || wordClean.startsWith(targetClean)) {
                return true
            }
        }

        val candidateNoUnderscore = candidateClean.replace("_", "")
        if (candidateNoUnderscore.startsWith(targetClean)) {
            return true
        }

        if (targetClean.length >= 6 && candidateClean.contains(targetClean)) {
            return true
        }

        return false
    }
}
