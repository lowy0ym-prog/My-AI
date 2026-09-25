package com.myai.assistant.agent.tools

import com.myai.assistant.agent.Tool
import com.myai.assistant.agent.ToolResult
import java.io.File

/** Resolves [relativePath] under [root], refusing to escape the workspace via '..'. */
internal fun resolveInWorkspace(root: File, relativePath: String): File? {
    val candidate = File(root, relativePath).canonicalFile
    val rootCanonical = root.canonicalFile
    return if (candidate.path.startsWith(rootCanonical.path)) candidate else null
}

/** Reads a file inside the current project workspace directory. */
class ReadFileTool(private val workspaceRoot: File) : Tool {
    override val name = "read_file"
    override val description = "Read the contents of a file in the current project workspace."
    override val parameters = mapOf("path" to "relative path within the project workspace")

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val path = arguments["path"] ?: return ToolResult(false, "", "missing 'path' argument")
        val file = resolveInWorkspace(workspaceRoot, path) ?: return ToolResult(false, "", "path escapes workspace")
        return if (file.exists()) {
            ToolResult(true, file.readText())
        } else {
            ToolResult(false, "", "file not found: $path")
        }
    }
}

/** Writes/overwrites a file inside the current project workspace directory. */
class WriteFileTool(private val workspaceRoot: File) : Tool {
    override val name = "write_file"
    override val description = "Create or overwrite a file in the current project workspace."
    override val parameters = mapOf(
        "path" to "relative path within the project workspace",
        "content" to "full file content to write"
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val path = arguments["path"] ?: return ToolResult(false, "", "missing 'path' argument")
        val content = arguments["content"] ?: return ToolResult(false, "", "missing 'content' argument")
        val file = resolveInWorkspace(workspaceRoot, path) ?: return ToolResult(false, "", "path escapes workspace")
        file.parentFile?.mkdirs()
        file.writeText(content)
        return ToolResult(true, "wrote ${content.length} chars to $path")
    }
}

/** Lists files inside the current project workspace directory. */
class ListFilesTool(private val workspaceRoot: File) : Tool {
    override val name = "list_files"
    override val description = "List files and folders in the project workspace, optionally under a subpath."
    override val parameters = mapOf("path" to "relative subpath, or empty for the workspace root")

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val path = arguments["path"].orEmpty()
        val dir = resolveInWorkspace(workspaceRoot, path) ?: return ToolResult(false, "", "path escapes workspace")
        if (!dir.exists() || !dir.isDirectory) return ToolResult(false, "", "not a directory: $path")
        val listing = dir.listFiles()?.joinToString("\n") { (if (it.isDirectory) "${it.name}/" else it.name) }.orEmpty()
        return ToolResult(true, listing)
    }
}
