package com.example.llama.aichat

import com.example.llama.aichat.ai.K2PromptBuilder
import com.example.llama.aichat.ai.K2ResponseParser
import com.example.llama.aichat.ai.RuleIntent
import com.example.llama.aichat.notification.NotificationData
import org.junit.Assert.*
import org.junit.Test

class K2AiTest {

    @Test
    fun testCompiledSimpleContact() {
        val json = """{"rule_type": "SIMPLE_CONTACT", "execution_engine": "AOT_FAST", "target_person": "madhu", "target_apps": [], "action": "ALERT", "positive_topics": [], "excluded_topics": []}"""
        val rule = K2ResponseParser.parseCompiledRule(json, "any message from Madhu is important")!!
        assertEquals(RuleIntent.SIMPLE_CONTACT, rule.intent)
        assertEquals("madhu", rule.targetPerson)
        assertTrue(rule.positiveTopics.isEmpty())
        assertFalse(rule.isNegative)
    }

    @Test
    fun testCompiledSimpleBlock() {
        val json = """{"rule_type": "SIMPLE_BLOCK", "execution_engine": "AOT_FAST", "target_person": "bob", "target_apps": [], "action": "MUTE", "positive_topics": [], "excluded_topics": []}"""
        val rule = K2ResponseParser.parseCompiledRule(json, "ignore messages from Bob")!!
        assertEquals(RuleIntent.SIMPLE_BLOCK, rule.intent)
        assertEquals("bob", rule.targetPerson)
        assertTrue(rule.isNegative)
    }

    @Test
    fun testCompiledConditionalPerson() {
        val json = """{"rule_type": "CONDITIONAL_CONTACT", "execution_engine": "AOT_FAST", "target_person": "arjun", "target_apps": [], "action": "MUTE", "positive_topics": [], "excluded_topics": ["movies", "movie"]}"""
        val rule = K2ResponseParser.parseCompiledRule(json, "whatever message from arjun related to movies, it is never important")!!
        assertEquals(RuleIntent.CONDITIONAL_CONTACT, rule.intent)
        assertEquals("arjun", rule.targetPerson)
        assertTrue(rule.excludedTopics.contains("movies") || rule.excludedTopics.contains("movie"))
        assertTrue(rule.isNegative)
    }

    @Test
    fun testCompiledJobTopic() {
        val json = """{"rule_type": "TOPIC_FILTER", "execution_engine": "AOT_FAST", "target_person": null, "target_apps": [], "action": "ALERT", "positive_topics": ["job", "interview"], "excluded_topics": []}"""
        val rule = K2ResponseParser.parseCompiledRule(json, "if someone messages about job related it is important")!!
        assertEquals(RuleIntent.TOPIC_FILTER, rule.intent)
        assertNull(rule.targetPerson)
        assertTrue(rule.positiveTopics.contains("job") || rule.positiveTopics.contains("interview"))
        assertFalse(rule.isNegative)
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
    fun testEmotionRuleClassificationPranavAngry() {
        val json = """{"rule_type": "CONDITIONAL_EMOTION", "execution_engine": "K2_DEEP", "target_person": "pranav", "target_apps": [], "action": "MUTE", "positive_topics": [], "excluded_topics": [], "semantic_condition": "sender is angry"}"""
        val rule = K2ResponseParser.parseCompiledRule(json, "any msg from Pranav when he is angry is not important")!!
        assertEquals("K2_DEEP", rule.semanticDepth)
        assertEquals("pranav", rule.targetPerson)

        val k2AngryResponse = """{"important": false, "alert": false, "reason": "Pranav is angry and confrontational", "category": "messages"}"""
        val angryAnalysis = K2ResponseParser.parse(k2AngryResponse)
        assertFalse(angryAnalysis.important)
        assertFalse(angryAnalysis.alert)
        assertTrue(angryAnalysis.reason.contains("angry"))

        val k2HappyResponse = """{"important": true, "alert": true, "reason": "Pranav is congratulating with excitement", "category": "messages"}"""
        val happyAnalysis = K2ResponseParser.parse(k2HappyResponse)
        assertTrue(happyAnalysis.important)
        assertTrue(happyAnalysis.alert)
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

    @Test
    fun testToneAnalysisParsing() {
        val angryJson = """{"tone_matched": true, "reason": "Sender is expressing intense anger and aggression"}"""
        val angryResult = K2ResponseParser.parseToneResult(angryJson)
        assertTrue(angryResult.toneMatched)
        assertEquals("Sender is expressing intense anger and aggression", angryResult.reason)

        val calmJson = """{"tone_matched": false, "reason": "Sender is calm and expressing contentment, not anger"}"""
        val calmResult = K2ResponseParser.parseToneResult(calmJson)
        assertFalse(calmResult.toneMatched)
        assertEquals("Sender is calm and expressing contentment, not anger", calmResult.reason)
    }
}
