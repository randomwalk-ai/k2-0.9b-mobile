package com.example.llama.aichat.ai

import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.flow.takeWhile
import java.lang.StringBuilder

class K2LocalLLM(private val engine: InferenceEngine) : LocalLLM {

    override suspend fun generate(prompt: String, maxTokens: Int): String {
        val result = StringBuilder()
        if (prompt.trimEnd().endsWith("{")) {
            result.append("{")
        }
        var hasStartedJson = result.contains("{")
        var hasEndedJson = false

        engine.sendUserPrompt(prompt, maxTokens)
            .takeWhile { !hasEndedJson }
            .collect { token ->
                result.append(token)
                if (result.contains("{")) {
                    hasStartedJson = true
                }
                if (hasStartedJson && (token.contains("}") || token.contains("<|im_end|>") || token.contains("<|endoftext|>"))) {
                    hasEndedJson = true
                }
            }
        return result.toString()
    }

    override suspend fun setSystemPrompt(prompt: String) {
        engine.setSystemPrompt(prompt)
    }
}
