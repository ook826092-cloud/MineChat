package cn.mine.minestars.data.ai.tools

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import cn.mine.ai.core.InputSchema
import cn.mine.ai.core.Tool
import cn.mine.ai.ui.UIMessagePart
import cn.mine.minestars.core.workspace.WorkspaceManager
import cn.mine.minestars.core.workspace.WorkspaceStorageArea
import cn.mine.minestars.data.files.FilesManager
import cn.mine.minestars.data.repository.WorkspaceRepository
import org.koin.java.KoinJavaComponent.getKoin
import java.io.ByteArrayOutputStream

private const val SHELL_TIMEOUT_MAX_SECONDS = 600L

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg")

private fun String.isImagePath(): Boolean =
    substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS

private fun String.requireSafePath() {
    require(!contains("..") && !startsWith("/")) { "Path must be within the workspace directory" }
}

private fun JsonElement?.primitiveContent(): String? = when (this) {
    null -> null
    is JsonNull -> null
    is JsonPrimitive -> content
    else -> null
}

val WorkspaceToolDefaultApprovals: Map<String, Boolean> = mapOf(
    "workspace_list_files" to false,
    "workspace_read_file" to false,
    "workspace_write_file" to false,
    "workspace_edit_file" to false,
    "workspace_delete_file" to true,
    "workspace_move_file" to true,
    "workspace_shell" to true,
)

fun resolveWorkspaceToolApproval(name: String, overrides: Map<String, Boolean>): Boolean {
    return overrides[name] ?: WorkspaceToolDefaultApprovals[name] ?: false
}

suspend fun createWorkspaceTools(
    workspaceId: String?,
    workspaceRepository: WorkspaceRepository,
): List<Tool> {
    if (workspaceId.isNullOrBlank()) return emptyList()
    val approvalOverrides = runCatching {
        workspaceRepository.getToolApprovalOverrides(workspaceId)
    }.getOrDefault(emptyMap())
    fun needsApproval(name: String) = resolveWorkspaceToolApproval(name, approvalOverrides)

    return listOf(
        createListFilesTool(workspaceId, ::needsApproval, workspaceRepository),
        createReadFileTool(workspaceId, ::needsApproval, workspaceRepository),
        createWriteFileTool(workspaceId, ::needsApproval, workspaceRepository),
        createEditFileTool(workspaceId, ::needsApproval, workspaceRepository),
        createDeleteFileTool(workspaceId, ::needsApproval, workspaceRepository),
        createMoveFileTool(workspaceId, ::needsApproval, workspaceRepository),
        createShellTool(workspaceId, ::needsApproval, workspaceRepository),
    )
}

private fun createListFilesTool(
    workspaceId: String,
    needsApproval: (String) -> Boolean,
    workspaceRepository: WorkspaceRepository,
) = Tool(
    name = "workspace_list_files",
    description = """
        List files in the assistant's bound workspace. Use area "files" for the working directory and "linux" for the installed Rootfs.
        Response format: entries[].path, name, isDirectory, sizeBytes, updatedAt.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("path", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional path relative to the workspace root. Defaults to root.")
                })
                put("area", buildJsonObject {
                    put("type", "string")
                    put("enum", buildJsonArray { add(JsonPrimitive("files")); add(JsonPrimitive("linux")) })
                    put("description", "Storage area to access. Defaults to files.")
                })
            }
        )
    },
    needsApproval = needsApproval("workspace_list_files"),
    execute = {
        val params = it.jsonObject
        val path = params["path"].primitiveContent() ?: ""
        val area = when (params["area"].primitiveContent()?.lowercase()) {
            null, "", "files" -> WorkspaceStorageArea.FILES
            "linux", "rootfs" -> WorkspaceStorageArea.LINUX
            else -> error("area must be one of: files, linux")
        }
        val entries = workspaceRepository.listFiles(workspaceId, area, path)
        listOf(
            UIMessagePart.Text(
                buildJsonObject {
                    put("entries", buildJsonArray {
                        entries.forEach { entry ->
                            add(buildJsonObject {
                                put("path", entry.path)
                                put("name", entry.name)
                                put("isDirectory", entry.isDirectory)
                                put("sizeBytes", entry.sizeBytes)
                                put("updatedAt", entry.updatedAt)
                            })
                        }
                    })
                }.toString()
            )
        )
    },
)

private fun createReadFileTool(
    workspaceId: String,
    needsApproval: (String) -> Boolean,
    workspaceRepository: WorkspaceRepository,
) = Tool(
    name = "workspace_read_file",
    description = """
        Read a UTF-8 text file from the assistant's bound workspace files area. Paths are relative to the workspace files root.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("path", buildJsonObject {
                    put("type", "string")
                    put("description", "Path relative to the workspace root")
                })
            },
            required = listOf("path"),
        )
    },
    needsApproval = needsApproval("workspace_read_file"),
    execute = {
        val path = it.jsonObject["path"]?.jsonPrimitive?.content ?: error("path is required")
        if (path.isImagePath()) {
            val buffer = ByteArrayOutputStream()
            workspaceRepository.exportFile(workspaceId, WorkspaceStorageArea.FILES, path, buffer)
            val bytes = buffer.toByteArray()
            val filesManager = getKoin().get<FilesManager>()
            val uris = filesManager.createChatFilesByByteArrays(listOf(bytes))
            listOf(
                UIMessagePart.Image(url = uris.first().toString()),
                UIMessagePart.Text(
                    buildJsonObject {
                        put("path", path)
                        put("description", "Image file read successfully")
                    }.toString()
                ),
            )
        } else {
            val text = workspaceRepository.readFile(workspaceId, path)
            listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("path", path)
                        put("text", text)
                    }.toString()
                )
            )
        }
    },
)

private fun createWriteFileTool(
    workspaceId: String,
    needsApproval: (String) -> Boolean,
    workspaceRepository: WorkspaceRepository,
) = Tool(
    name = "workspace_write_file",
    description = """
        Write a UTF-8 text file to the assistant's bound workspace files area. Paths are relative to the workspace files root.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("path", buildJsonObject {
                    put("type", "string")
                    put("description", "Path relative to the workspace root")
                })
                put("text", buildJsonObject {
                    put("type", "string")
                    put("description", "UTF-8 text content to write")
                })
                put("overwrite", buildJsonObject {
                    put("type", "boolean")
                    put("description", "Whether to overwrite an existing file. Defaults to true.")
                })
            },
            required = listOf("path", "text"),
        )
    },
    needsApproval = needsApproval("workspace_write_file"),
    execute = {
        val params = it.jsonObject
        val path = params["path"]?.jsonPrimitive?.content ?: error("path is required")
        path.requireSafePath()
        val text = params["text"]?.jsonPrimitive?.content ?: error("text is required")
        val overwrite = params["overwrite"].primitiveContent()?.toBooleanStrictOrNull() ?: true
        val entry = workspaceRepository.writeFile(workspaceId, path, text, overwrite)
        listOf(UIMessagePart.Text(entry.toJson().toString()))
    },
)

private fun createEditFileTool(
    workspaceId: String,
    needsApproval: (String) -> Boolean,
    workspaceRepository: WorkspaceRepository,
) = Tool(
    name = "workspace_edit_file",
    description = """
        Edit a UTF-8 text file in the assistant's bound workspace files area by replacing exact text.
        Provide old_text and new_text. By default old_text must occur exactly once; set replace_all=true to replace every occurrence.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("path", buildJsonObject {
                    put("type", "string")
                    put("description", "Path relative to the workspace root")
                })
                put("old_text", buildJsonObject {
                    put("type", "string")
                    put("description", "Exact text to replace")
                })
                put("new_text", buildJsonObject {
                    put("type", "string")
                    put("description", "Replacement text")
                })
                put("replace_all", buildJsonObject {
                    put("type", "boolean")
                    put("description", "Whether to replace every occurrence. Defaults to false.")
                })
            },
            required = listOf("path", "old_text", "new_text"),
        )
    },
    needsApproval = needsApproval("workspace_edit_file"),
    execute = {
        val params = it.jsonObject
        val path = params["path"]?.jsonPrimitive?.content ?: error("path is required")
        path.requireSafePath()
        val oldText = params["old_text"]?.jsonPrimitive?.content ?: error("old_text is required")
        val newText = params["new_text"]?.jsonPrimitive?.content ?: error("new_text is required")
        val replaceAll = params["replace_all"].primitiveContent()?.toBooleanStrictOrNull() ?: false
        require(oldText.isNotEmpty()) { "old_text must not be empty" }

        val original = workspaceRepository.readFile(workspaceId, path)
        val occurrences = original.windowed(oldText.length).count { window -> window == oldText }
        require(occurrences > 0) { "old_text was not found in $path" }
        if (!replaceAll) {
            require(occurrences == 1) {
                "old_text occurs $occurrences times in $path; set replace_all=true to replace all occurrences"
            }
        }

        val updated = if (replaceAll) original.replace(oldText, newText) else original.replaceFirst(oldText, newText)
        val entry = workspaceRepository.writeFile(workspaceId, path, updated, overwrite = true)
        listOf(
            UIMessagePart.Text(
                text = buildJsonObject {
                    put("path", entry.path)
                    put("replacements", if (replaceAll) occurrences else 1)
                    put("sizeBytes", entry.sizeBytes)
                    put("updatedAt", entry.updatedAt)
                }.toString(),
            )
        )
    },
)

private fun createDeleteFileTool(
    workspaceId: String,
    needsApproval: (String) -> Boolean,
    workspaceRepository: WorkspaceRepository,
) = Tool(
    name = "workspace_delete_file",
    description = """
        Delete a file or directory in the assistant's bound workspace. Use recursive=true for directories.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("path", buildJsonObject {
                    put("type", "string")
                    put("description", "Path relative to the workspace root")
                })
                put("area", buildJsonObject {
                    put("type", "string")
                    put("enum", buildJsonArray { add(JsonPrimitive("files")); add(JsonPrimitive("linux")) })
                    put("description", "Storage area to access. Defaults to files.")
                })
                put("recursive", buildJsonObject {
                    put("type", "boolean")
                    put("description", "Required when deleting a directory. Defaults to false.")
                })
            },
            required = listOf("path"),
        )
    },
    needsApproval = needsApproval("workspace_delete_file"),
    execute = {
        val params = it.jsonObject
        val path = params["path"]?.jsonPrimitive?.content ?: error("path is required")
        val area = when (params["area"].primitiveContent()?.lowercase()) {
            null, "", "files" -> WorkspaceStorageArea.FILES
            "linux", "rootfs" -> WorkspaceStorageArea.LINUX
            else -> error("area must be one of: files, linux")
        }
        val recursive = params["recursive"].primitiveContent()?.toBooleanStrictOrNull() ?: false
        val deleted = workspaceRepository.deleteFile(workspaceId, path, recursive)
        listOf(
            UIMessagePart.Text(
                buildJsonObject {
                    put("success", deleted)
                    put("path", path)
                }.toString()
            )
        )
    },
)

private fun createMoveFileTool(
    workspaceId: String,
    needsApproval: (String) -> Boolean,
    workspaceRepository: WorkspaceRepository,
) = Tool(
    name = "workspace_move_file",
    description = """
        Move or rename a file or directory in the assistant's bound workspace files area.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("source", buildJsonObject {
                    put("type", "string")
                    put("description", "Source path relative to the workspace files root")
                })
                put("target", buildJsonObject {
                    put("type", "string")
                    put("description", "Target path relative to the workspace files root")
                })
                put("overwrite", buildJsonObject {
                    put("type", "boolean")
                    put("description", "Whether to overwrite the target if it exists. Defaults to false.")
                })
            },
            required = listOf("source", "target"),
        )
    },
    needsApproval = needsApproval("workspace_move_file"),
    execute = {
        val params = it.jsonObject
        val source = params["source"]?.jsonPrimitive?.content ?: error("source is required")
        val target = params["target"]?.jsonPrimitive?.content ?: error("target is required")
        val overwrite = params["overwrite"].primitiveContent()?.toBooleanStrictOrNull() ?: false
        val entry = workspaceRepository.moveFile(workspaceId, source, target, overwrite)
        listOf(UIMessagePart.Text(entry.toJson().toString()))
    },
)

private fun createShellTool(
    workspaceId: String,
    needsApproval: (String) -> Boolean,
    workspaceRepository: WorkspaceRepository,
) = Tool(
    name = "workspace_shell",
    description = """
        Run a shell command in the assistant's bound workspace Rootfs. The workspace files area is mounted at /workspace.
        Use cwd for a path relative to the workspace files root. Requires Rootfs to be installed and ready.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("command", buildJsonObject {
                    put("type", "string")
                    put("description", "Shell command to run")
                })
                put("cwd", buildJsonObject {
                    put("type", "string")
                    put("description", "Working directory relative to the workspace files root. Defaults to root.")
                })
                put("timeout", buildJsonObject {
                    put("type", "integer")
                    put("description", "Command timeout in seconds. Defaults to 30, max $SHELL_TIMEOUT_MAX_SECONDS.")
                })
            },
            required = listOf("command"),
        )
    },
    needsApproval = needsApproval("workspace_shell"),
    execute = {
        val params = it.jsonObject
        val command = params["command"]?.jsonPrimitive?.content ?: error("command is required")
        val cwd = params["cwd"].primitiveContent() ?: ""
        val timeoutMillis = params["timeout"].primitiveContent()?.toLongOrNull()
            ?.coerceIn(1L, SHELL_TIMEOUT_MAX_SECONDS)
            ?.times(1_000L)
            ?: WorkspaceManager.DEFAULT_COMMAND_TIMEOUT_MS
        val result = workspaceRepository.executeCommand(workspaceId, command, cwd, timeoutMillis)
        listOf(
            UIMessagePart.Text(
                buildJsonObject {
                    put("exitCode", result.exitCode)
                    put("stdout", result.stdout)
                    put("stderr", result.stderr)
                    put("timedOut", result.timedOut)
                    if (result.truncated) put("truncated", true)
                }.toString()
            )
        )
    },
)

private fun cn.mine.minestars.core.workspace.WorkspaceFileEntry.toJson() = buildJsonObject {
    put("path", path)
    put("name", name)
    put("isDirectory", isDirectory)
    put("sizeBytes", sizeBytes)
    put("updatedAt", updatedAt)
}
