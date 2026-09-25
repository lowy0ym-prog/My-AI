package com.myai.assistant.agent

class ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        tools[tool.name] = tool
    }

    fun get(name: String): Tool? = tools[name]

    fun all(): List<Tool> = tools.values.toList()

    fun describeForPrompt(): String = tools.values.joinToString("\n") { tool ->
        "- ${tool.name}: ${tool.description} (args: ${tool.parameters})"
    }
}
