package com.example.llama.aichat

import com.example.llama.aichat.ai.K2PromptBuilder
import com.example.llama.aichat.ai.K2ResponseParser
import com.example.llama.aichat.ai.RuleClassifier
import com.example.llama.aichat.ai.RuleIntent
import org.junit.Assert.*
import org.junit.Test

class K2AiTest {

    @Test
    fun testRuleClassifierSimpleContact() {
        val rule = RuleClassifier.classify("any message from Madhu is important")
        assertEquals(RuleIntent.SIMPLE_CONTACT, rule.intent)
        assertEquals("madhu", rule.targetPerson)
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
    fun testRuleClassifierSemanticConditionalPerson() {
        val rule = RuleClassifier.classify("whatever message from arjun related to movies, it is never important")
        assertEquals(RuleIntent.SEMANTIC_CONDITIONAL, rule.intent)
        assertEquals("arjun", rule.targetPerson)
        assertTrue(rule.isNegative)
    }

    @Test
    fun testRuleClassifierSemanticGeneralTopic() {
        val rule = RuleClassifier.classify("if someone messages about job related it is important")
        assertEquals(RuleIntent.SEMANTIC_CONDITIONAL, rule.intent)
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
        assertTrue(prompt.endsWith("<|im_start|>assistant\n"))
    }
}
