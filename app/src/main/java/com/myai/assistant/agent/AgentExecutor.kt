package com.myai.assistant.agent

import com.myai.assistant.inference.LlamaEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A minimal ReAct-style loop: the model is prompted with the task, the
 * available tools, and prior steps; it responds with either a tool call or a
 * final answer. This is intentionally small \u2014 the goal is a real, extensible
 * seam for multi-step / project-building work, not a complete planning
 * system. Response parsing here is line-based on purpose; swap in structured
 * JSON output (with grammar-constrained decoding) once basic generation is
 * validated end-to-end.
 */
class AgentExecutor(
    private val engine: LlamaEngine,
    private val registry: ToolRegistry,
    private val maxSteps: Int = 8
) {
    data class Step(val thought: String, val toolCall: String?, val toolResult: ToolResult?)

    sealed class AgentEvent {
        data class StepCompleted(val step: Step) : AgentEvent()
        data class FinalAnswer(val text: String) : AgentEvent()
        data class Error(val message: String) : AgentEvent()
    }

    fun run(task: String): Flow<AgentEvent> = flow {
        if (!engine.isLoaded) {
            emit(AgentEvent.Error("No local model is loaded. Load a model from Settings > Models first."))
            return@flow
        }

        val transcript = StringBuilder()
        transcript.append(buildSystemPrompt())
        transcript.append("\nTask: $task\n")

        repeat(maxSteps) {
            val response = StringBuilder()
            engine.generate(transcript.toString()).collect { token -> response.append(token) }

            val parsed = parseModelResponse(response.toString())
            if (parsed.toolName == null) {
                emit(AgentEvent.FinalAnswer(parsed.finalAnswer ?: response.toString()))
                return@flow
            }

            val tool = registry.get(parsed.toolName)
            val result = tool?.execute(parsed.toolArgs)
                ?: ToolResult(false, "", "unknown tool: ${parsed.toolName}")

            val step = Step(thought = parsed.thought, toolCall = parsed.toolName, toolResult = result)
            emit(AgentEvent.StepCompleted(step))

            transcript.append("\nObservation: ${result.output.ifBlank { result.error.orEmpty() }}\n")
        }

        emit(AgentEvent.Error("Reached the step limit ($maxSteps) without a final answer."))
    }

    private fun buildSystemPrompt(): String =
        "You are My AI, a local on-device agent. Available tools:\n${registry.describeForPrompt()}\n" +
            "Respond with either a tool call (Tool: <name>\nArgs: <key=value pairs>) or " +
            "a final answer (Answer: <text>)."

    private data class ParsedResponse(
        val thought: String,
        val toolName: String?,
        val toolArgs: Map<String, String>,
        val finalAnswer: String?
    )

    private fun parseModelResponse(text: String): ParsedResponse {
        val answerLine = text.lineSequence().firstOrNull { it.startsWith("Answer:") }
        if (answerLine != null) {
            return ParsedResponse(text, null, emptyMap(), answerLine.removePrefix("Answer:").trim())
        }
        val toolLine = text.lineSequence().firstOrNull { it.startsWith("Tool:") }
        val toolName = toolLine?.removePrefix("Tool:")?.trim()
        return ParsedResponse(text, toolName, emptyMap(), null)
    }
}
