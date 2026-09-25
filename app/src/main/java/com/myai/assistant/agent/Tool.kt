package com.myai.assistant.agent

/**
 * A single capability the agent can invoke. Tools are the extension point
 * referenced in the project brief (multi-step agent / project operations):
 * new capabilities are added by implementing this interface and registering
 * with a [ToolRegistry].
 */
interface Tool {
    val name: String
    val description: String
    /** Human-readable description of expected arguments, for prompting the model. */
    val parameters: Map<String, String>

    suspend fun execute(arguments: Map<String, String>): ToolResult
}

data class ToolResult(
    val success: Boolean,
    val output: String,
    val error: String? = null
)
