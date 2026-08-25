package com.mobdysseus.app.mcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

/**
 * Minimal Model Context Protocol client speaking JSON-RPC 2.0 over HTTP.
 * Supports two transports: Streamable HTTP (plain JSON POST) and SSE
 * (Server-Sent Events) for streamed tool results. Talks to [McpServerConfig.url],
 * the base MCP server endpoint.
 */
class McpClient(private val config: McpServerConfig) {

    private val nextId = AtomicLong(1L)

    /**
     * Performs the MCP handshake (initialize) then tools/list, returning the server's tools.
     */
    suspend fun listTools(): List<McpTool> = withContext(Dispatchers.IO) {
        initialize()
        val body = post(McpCodec.request(nextId.getAndIncrement(), "tools/list", JSONObject()))
        McpCodec.parseTools(body)
    }

    /**
     * Calls a tool, streaming result text. When the server replies with
     * `text/event-stream`, emits the text chunk of each `data:` payload; otherwise
     * parses the whole JSON body once and emits its content text.
     */
    fun callTool(name: String, arguments: JSONObject): Flow<String> = flow {
        val conn = openConnection()
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json, text/event-stream")
        conn.connectTimeout = 20000
        conn.readTimeout = 0
        conn.doOutput = true

        val params = JSONObject()
            .put("name", name)
            .put("arguments", arguments)
        val body = McpCodec.request(nextId.getAndIncrement(), "tools/call", params)

        conn.outputStream.use { os ->
            os.write(body.toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            throw McpError("MCP error ($code): $err")
        }

        val contentType = conn.contentType ?: ""
        if (contentType.contains("text/event-stream", ignoreCase = true)) {
            conn.inputStream.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.startsWith("data:")) {
                        val data = line.substring(5).trim()
                        if (data.isNotEmpty() && data != "[DONE]") {
                            val text = try {
                                McpCodec.parseToolCallResult(data)
                            } catch (_: McpError) {
                                ""
                            }
                            if (text.isNotEmpty()) emit(text)
                        }
                    }
                    line = reader.readLine()
                }
            }
        } else {
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val text = McpCodec.parseToolCallResult(raw)
            if (text.isNotEmpty()) emit(text)
        }
    }.flowOn(Dispatchers.IO)

    private fun initialize() {
        val params = JSONObject()
            .put("protocolVersion", "2024-11-05")
            .put("capabilities", JSONObject())
            .put("clientInfo", JSONObject().put("name", "mobdysseus").put("version", "1.0.0"))
        val body = post(McpCodec.request(nextId.getAndIncrement(), "initialize", params))
        McpCodec.parseResult(body) // throws McpError on a JSON-RPC error response
    }

    private fun openConnection(): HttpURLConnection =
        URL(config.url).openConnection() as HttpURLConnection

    private fun post(body: String): String {
        val conn = openConnection()
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json, text/event-stream")
        conn.connectTimeout = 20000
        conn.readTimeout = 0
        conn.doOutput = true

        conn.outputStream.use { os ->
            os.write(body.toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            throw McpError("MCP error ($code): $err")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }
}
