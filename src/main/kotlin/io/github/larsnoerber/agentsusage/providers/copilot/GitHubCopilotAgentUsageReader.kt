package io.github.larsnoerber.agentsusage.providers.copilot

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.Disposable
import io.github.larsnoerber.agentsusage.core.agents.AcpAgentInstallation
import com.intellij.util.concurrency.AppExecutorUtil
import java.io.BufferedInputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Ask the installed ACP package's own language server; it owns authentication and quota requests. */
internal class GitHubCopilotAgentUsageReader : Disposable {
    @Volatile private var disposed = false
    private val activeProcess = AtomicReference<Process?>()

    fun read(): GitHubCopilotUsage {
        if (disposed) return GitHubCopilotUsage(error = "GitHub Copilot usage reader is disposed.")
        val executable = findExecutable()
            ?: return GitHubCopilotUsage(error = "GitHub Copilot agent package is not installed.")
        val process = ProcessBuilder(executable.toString(), "--stdio")
            .redirectError(ProcessBuilder.Redirect.DISCARD).start()
        activeProcess.set(process)
        val timedOut = AtomicBoolean()
        val timeout = AppExecutorUtil.getAppScheduledExecutorService().schedule({
            timedOut.set(true)
            stopProcess(process)
        }, 30, TimeUnit.SECONDS)
        try {
            if (disposed) return GitHubCopilotUsage(error = "GitHub Copilot usage reader is disposed.")
            process.inputStream.buffered().use { input ->
                process.outputStream.use { output ->
                    request(input, output, 1, "initialize", JsonObject().apply {
                        addProperty("processId", ProcessHandle.current().pid())
                        add("rootUri", JsonNull.INSTANCE)
                        add("capabilities", JsonObject())
                        add("initializationOptions", JsonObject().apply {
                            add("editorInfo", JsonObject().apply {
                                addProperty("name", "Rider")
                                addProperty("version", "2026.2")
                            })
                            add("editorPluginInfo", JsonObject().apply {
                                addProperty("name", "Agents Usage")
                                addProperty("version", "1.0.19")
                            })
                        })
                    })
                    send(output, message("initialized", JsonObject()))
                    send(output, message("workspace/didChangeConfiguration", JsonObject().apply {
                        add("settings", JsonObject().apply {
                            add("telemetry", JsonObject().apply { addProperty("telemetryLevel", "off") })
                        })
                    }))
                    val quota = request(input, output, 2, "checkQuota", JsonObject())
                    return GitHubCopilotAgentUsageParser.parse(quota)
                }
            }
        } catch (failure: CopilotQuotaException) {
            return GitHubCopilotUsage(error = failure.message)
        } catch (_: Exception) {
            return GitHubCopilotUsage(error = if (timedOut.get()) "GitHub Copilot quota request timed out."
                else "Unable to read usage from the GitHub Copilot agent.")
        } finally {
            timeout.cancel(false)
            activeProcess.compareAndSet(process, null)
            stopProcess(process)
        }
    }

    private fun request(input: BufferedInputStream, output: OutputStream, id: Int, method: String, params: JsonObject): JsonObject {
        send(output, message(method, params).apply { addProperty("id", id) })
        while (!disposed) {
            val response = receive(input)
            if (response.has("method") && response.has("id")) {
                // Only answer infrastructure requests; this connection never starts an agent session or prompt.
                val reply = JsonObject().apply {
                    addProperty("jsonrpc", "2.0")
                    add("id", response.get("id"))
                }
                when (response.get("method").asString) {
                    "workspace/configuration" -> reply.add("result", JsonArray().apply {
                        response.getAsJsonObject("params")?.getAsJsonArray("items")?.forEach { add(JsonNull.INSTANCE) }
                    })
                    "client/registerCapability", "window/workDoneProgress/create" -> reply.add("result", JsonNull.INSTANCE)
                    else -> reply.add("error", JsonObject().apply {
                        addProperty("code", -32601)
                        addProperty("message", "Method not supported by usage reader")
                    })
                }
                send(output, reply)
            } else if (response.get("id")?.takeUnless { it.isJsonNull }?.asString == id.toString()) {
                response.getAsJsonObject("error")?.let { error ->
                    // Classify errors without exposing native response text or any credential data.
                    val reason = error.get("message")?.asString.orEmpty()
                    throw CopilotQuotaException(when {
                        reason.contains("Not signed in", ignoreCase = true) -> "Sign in to the GitHub Copilot ACP agent in Rider to load usage."
                        error.get("code")?.asInt == -32601 -> "Update the GitHub Copilot ACP agent to a version supporting quota queries."
                        else -> "The GitHub Copilot agent could not fetch account quota."
                    })
                }
                return response.getAsJsonObject("result") ?: throw CopilotQuotaException("GitHub Copilot returned no quota data.")
            }
        }
        throw CopilotQuotaException("GitHub Copilot usage reader is disposed.")
    }

    private fun message(method: String, params: JsonObject) = JsonObject().apply {
        addProperty("jsonrpc", "2.0")
        addProperty("method", method)
        add("params", params)
    }

    private fun send(output: OutputStream, value: JsonObject) {
        val body = value.toString().toByteArray(Charsets.UTF_8)
        output.write("Content-Length: ${body.size}\r\n\r\n".toByteArray(Charsets.US_ASCII))
        output.write(body)
        output.flush()
    }

    private fun receive(input: BufferedInputStream): JsonObject {
        var length: Int? = null
        while (true) {
            val header = StringBuilder()
            while (true) {
                val next = input.read()
                check(next >= 0) { "Copilot process ended" }
                if (next == '\n'.code) break
                if (next != '\r'.code) header.append(next.toChar())
                check(header.length <= 8192) { "Invalid Copilot header" }
            }
            if (header.isEmpty()) break
            if (header.startsWith("Content-Length:", ignoreCase = true)) length = header.toString().substringAfter(':').trim().toInt()
        }
        val count = length?.takeIf { it in 1..2_097_152 } ?: error("Invalid Copilot frame length")
        val body = input.readNBytes(count)
        check(body.size == count) { "Incomplete Copilot frame" }
        return JsonParser.parseString(String(body, Charsets.UTF_8)).asJsonObject
    }

    override fun dispose() {
        disposed = true
        activeProcess.getAndSet(null)?.let(::stopProcess)
    }

    private fun stopProcess(process: Process) {
        runCatching { process.descendants().use { children -> children.forEach { it.destroyForcibly() } } }
        process.destroyForcibly()
    }

    companion object {
        fun isAvailable(): Boolean = findExecutable() != null

        private fun findExecutable(): Path? {
            val os = System.getProperty("os.name").lowercase()
            val platform = when {
                os.contains("win") -> "win32"
                os.contains("mac") -> "darwin"
                else -> "linux"
            }
            val arch = if (System.getProperty("os.arch") in listOf("aarch64", "arm64")) "arm64" else "x64"
            val binary = if (platform == "win32") "copilot-language-server.exe" else "copilot-language-server"
            return AcpAgentInstallation.installedFile("github-copilot",
                "node_modules/@github/copilot-language-server-$platform-$arch/$binary")
        }
    }
}

private class CopilotQuotaException(message: String) : Exception(message)
