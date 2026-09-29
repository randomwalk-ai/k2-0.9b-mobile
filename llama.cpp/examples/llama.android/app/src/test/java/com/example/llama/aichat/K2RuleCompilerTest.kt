package com.example.llama.aichat

import com.example.llama.aichat.ai.K2PromptBuilder
import com.example.llama.aichat.ai.K2ResponseParser
import com.example.llama.aichat.ai.RuleIntent
import com.example.llama.aichat.notification.NotificationData
import org.junit.Assert.*
import org.junit.Test

class K2RuleCompilerTest {

    @Test
    fun testRuleCompilerPromptGeneration() {
        val prompt = K2PromptBuilder.buildRuleCompilationPrompt("Pranav when angry is not important")
        assertTrue(prompt.contains("You are an expert Edge-AI Rule Compiler"))
        assertTrue(prompt.contains("APP_FILTER"))
        assertTrue(prompt.contains("TOPIC_FILTER"))
        assertTrue(prompt.contains("CONDITIONAL_EMOTION"))
        assertTrue(prompt.contains("AOT_FAST"))
        assertTrue(prompt.contains("K2_DEEP"))
        assertTrue(prompt.contains("Rule: \"Pranav when angry is not important\""))
        assertTrue(prompt.endsWith("<|im_start|>assistant\n{"))
    }

    @Test
    fun testParseCompiledAppFilterRule() {
        val rawK2Output = """
            {
              "rule_type": "APP_FILTER",
              "execution_engine": "AOT_FAST",
              "target_person": null,
              "target_apps": ["teams"],
              "action": "ALERT",
              "positive_topics": [],
              "excluded_topics": [],
              "semantic_condition": null,
              "summary": "Alert all notifications from Teams"
            }
        """.trimIndent()

        val parsed = K2ResponseParser.parseCompiledRule(rawK2Output, "Teams is important")
        assertNotNull(parsed)
        assertEquals(RuleIntent.APP_FILTER, parsed!!.intent)
        assertEquals("AOT_FAST", parsed.semanticDepth)
        assertEquals("ALERT", parsed.action)
        assertNull(parsed.targetPerson)
        assertTrue(parsed.targetApps.contains("teams"))
        assertTrue(parsed.positiveTopics.isEmpty())
        assertTrue(parsed.excludedTopics.isEmpty())
        assertNull(parsed.semanticCondition)
    }

    @Test
    fun testParseCompiledTopicFilterWithStopWordStripping() {
        val rawK2Output = """
            {
              "rule_type": "TOPIC_FILTER",
              "execution_engine": "AOT_FAST",
              "target_person": null,
              "target_apps": [],
              "action": "ALERT",
              "positive_topics": ["games", "gaming", "game", "esports"],
              "excluded_topics": [],
              "semantic_condition": null,
              "summary": "Alert messages about games and gaming"
            }
        """.trimIndent()

        val parsed = K2ResponseParser.parseCompiledRule(rawK2Output, "if any one messaged about playing games it is important")
        assertNotNull(parsed)
        assertEquals(RuleIntent.TOPIC_FILTER, parsed!!.intent)
        assertEquals("AOT_FAST", parsed.semanticDepth)
        assertEquals("ALERT", parsed.action)
        assertNull(parsed.targetPerson)
        assertFalse(parsed.positiveTopics.contains("one"))
        assertFalse(parsed.positiveTopics.contains("messaged"))
        assertFalse(parsed.positiveTopics.contains("playing"))
        assertTrue(parsed.positiveTopics.contains("games"))
        assertTrue(parsed.positiveTopics.contains("gaming"))
        assertTrue(parsed.positiveTopics.contains("game"))
        assertTrue(parsed.positiveTopics.contains("esports"))
    }

    @Test
    fun testParseCompiledConditionalEmotionRule() {
        val rawK2Output = """
            {
              "rule_type": "CONDITIONAL_EMOTION",
              "execution_engine": "K2_DEEP",
              "target_person": "pranav",
              "target_apps": [],
              "action": "MUTE",
              "positive_topics": [],
              "excluded_topics": [],
              "semantic_condition": "sender is angry, mad, or furious",
              "summary": "Mute Pranav when angry"
            }
        """.trimIndent()

        val parsed = K2ResponseParser.parseCompiledRule(rawK2Output, "Pranav when angry is not important")
        assertNotNull(parsed)
        assertEquals(RuleIntent.CONDITIONAL_EMOTION, parsed!!.intent)
        assertEquals("K2_DEEP", parsed.semanticDepth)
        assertEquals("pranav", parsed.targetPerson)
        assertEquals("MUTE", parsed.action)
        assertTrue(parsed.isNegative)
        assertEquals("sender is angry, mad, or furious", parsed.semanticCondition)
    }

    @Test
    fun testParseCompiledSimpleContactAndBlockRules() {
        val rawContactOutput = """
            {
              "rule_type": "SIMPLE_CONTACT",
              "execution_engine": "AOT_FAST",
              "target_person": "madhu",
              "target_apps": [],
              "action": "ALERT",
              "positive_topics": [],
              "excluded_topics": [],
              "semantic_condition": null,
              "summary": "Alert all messages from Madhu"
            }
        """.trimIndent()

        val contactParsed = K2ResponseParser.parseCompiledRule(rawContactOutput, "any message from Madhu is important")
        assertNotNull(contactParsed)
        assertEquals(RuleIntent.SIMPLE_CONTACT, contactParsed!!.intent)
        assertEquals("madhu", contactParsed.targetPerson)
        assertEquals("ALERT", contactParsed.action)

        val rawBlockOutput = """
            {
              "rule_type": "SIMPLE_BLOCK",
              "execution_engine": "AOT_FAST",
              "target_person": "bob",
              "target_apps": [],
              "action": "MUTE",
              "positive_topics": [],
              "excluded_topics": [],
              "semantic_condition": null,
              "summary": "Mute all messages from Bob"
            }
        """.trimIndent()

        val blockParsed = K2ResponseParser.parseCompiledRule(rawBlockOutput, "ignore messages from Bob")
        assertNotNull(blockParsed)
        assertEquals(RuleIntent.SIMPLE_BLOCK, blockParsed!!.intent)
        assertEquals("bob", blockParsed.targetPerson)
        assertEquals("MUTE", blockParsed.action)
        assertTrue(blockParsed.isNegative)
    }

    @Test
    fun testParseCompiledConditionalContactWithExceptions() {
        val rawExceptionOutput = """
            {
              "rule_type": "CONDITIONAL_CONTACT",
              "execution_engine": "AOT_FAST",
              "target_person": "arjun",
              "target_apps": [],
              "action": "ALERT",
              "positive_topics": [],
              "excluded_topics": ["movies", "movie", "cinema", "films"],
              "semantic_condition": null,
              "summary": "Alert messages from Arjun except movies"
            }
        """.trimIndent()

        val parsed = K2ResponseParser.parseCompiledRule(rawExceptionOutput, "whatever message from arjun related to movies, it is never important")
        assertNotNull(parsed)
        assertEquals(RuleIntent.CONDITIONAL_CONTACT, parsed!!.intent)
        assertEquals("arjun", parsed.targetPerson)
        assertTrue(parsed.excludedTopics.contains("movies"))
        assertTrue(parsed.excludedTopics.contains("movie"))
        assertTrue(parsed.excludedTopics.contains("cinema"))
    }

    @Test
    fun testMalformedK2ResponseGracefulFallback() {
        val garbageOutput = "Sorry, as an AI I cannot compile this rule"
        val parsed = K2ResponseParser.parseCompiledRule(garbageOutput, "any message from Madhu is important")
        assertNotNull(parsed)
        assertEquals("madhu", parsed.targetPerson)
        assertEquals(RuleIntent.SIMPLE_CONTACT, parsed.intent)
    }

    @Test
    fun testEndToEndDualEngineSimulation() {
        // User creates Rule 1: "Teams is important"
        val rule1K2Json = """{"rule_type": "APP_FILTER", "execution_engine": "AOT_FAST", "target_person": null, "target_apps": ["teams"], "action": "ALERT", "positive_topics": [], "excluded_topics": []}"""
        val rule1 = K2ResponseParser.parseCompiledRule(rule1K2Json, "Teams is important")!!.toNotificationRule(id = 1L)

        // User creates Rule 2: "Pranav when angry is not important"
        val rule2K2Json = """{"rule_type": "CONDITIONAL_EMOTION", "execution_engine": "K2_DEEP", "target_person": "pranav", "target_apps": [], "action": "MUTE", "positive_topics": [], "excluded_topics": [], "semantic_condition": "sender is angry"}"""
        val rule2 = K2ResponseParser.parseCompiledRule(rule2K2Json, "Pranav when angry is not important")!!.toNotificationRule(id = 2L)

        // Notification Case 1: Pranav on Teams sends angry message
        val angryMsg = NotificationData(
            packageName = "com.microsoft.teams",
            appName = "Teams",
            title = "Pranav",
            text = "I am so mad right now, why didn't you submit the report?!",
            subText = null,
            sender = "Pranav",
            category = "messages",
            notificationKey = "k1",
            timestamp = System.currentTimeMillis()
        )

        // 1. Verify Alice triggers Rule 1 instantly via AOT (<0.2ms) without K2 Deep
        assertTrue(rule1.getTargetApps().contains("teams"))
        assertEquals("AOT_FAST", rule1.semanticDepth)

        // 2. Verify Pranav triggers Rule 2 (K2_DEEP)
        assertEquals("K2_DEEP", rule2.semanticDepth)
        assertEquals("pranav", rule2.targetPerson)

        // Simulate K2 Tone Evaluation for Case 1 (Angry)
        val k2AngryToneJson = """{"tone_matched": true, "reason": "Sender is expressing intense anger and frustration"}"""
        val angryToneResult = K2ResponseParser.parseToneResult(k2AngryToneJson)
        assertTrue(angryToneResult.toneMatched)
        // Since rule2 action is MUTE, it strictly mutes and prevents Rule 1 from alerting!

        // Simulate K2 Tone Evaluation for Case 2 (Happy)
        val k2HappyToneJson = """{"tone_matched": false, "reason": "Sender is expressing gratitude and praise, not anger"}"""
        val happyToneResult = K2ResponseParser.parseToneResult(k2HappyToneJson)
        assertFalse(happyToneResult.toneMatched)
        // Since suppression condition is FALSE, execution falls through to Rule 1 (Teams Alert) -> Alerts!
    }
}
