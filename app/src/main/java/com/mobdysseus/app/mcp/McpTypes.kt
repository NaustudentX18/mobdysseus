package com.mobdysseus.app.mcp

import org.json.JSONObject

data class McpServerConfig(
    val id: String,
    val name: String,
    val url: String,
)

data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: String,
)

/** Error raised by the MCP layer (JSON-RPC errors, HTTP errors, ...). */
class McpError(message: String) : Exception(message)

/**
 * Pure JSON-RPC 2.0 encode/decode for the Model Context Protocol.
 * No Android or network dependencies: everything is string/JSONObject in and out.
 */
object McpCodec {

    /** Encodes a JSON-RPC 2.0 request: {"jsonrpc":"2.0","id":id,"method":...,"params":...}. */
    fun request(id: Long, method: String, params: JSONObject): String =
        JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id)
            .put("method", method)
            .put("params", params)
            .toString()

    /**
     * Parses a JSON-RPC response and returns its "result" object.
     * Returns null when the response has no result field. Throws [McpError] when the
     * response carries an "error" member.
     */
    fun parseResult(json: String): JSONObject? {
        val obj = JSONObject(json)
        if (obj.has("error") && !obj.isNull("error")) {
            throw McpError(extractErrorMessage(obj))
        }
        if (!obj.has("result") || obj.isNull("result")) return null
        return obj.getJSONObject("result")
    }

    /**
     * Parses a tools/list response into a list of [McpTool].
     * Accepts either the full JSON-RPC response ({"result":{"tools":[...]}}) or a bare
     * result object ({"tools":[...]}). Throws [McpError] on a JSON-RPC error.
     */
    fun parseTools(json: String): List<McpTool> {
        val result = unwrapResult(JSONObject(json))
        val tools = result.optJSONArray("tools") ?: return emptyList()
        val out = ArrayList<McpTool>(tools.length())
        for (i in 0 until tools.length()) {
            val t = tools.getJSONObject(i)
            out.add(
                McpTool(
                    name = t.optString("name"),
                    description = t.optString("description"),
                    inputSchema = t.optJSONObject("inputSchema")?.toString() ?: "",
                )
            )
        }
        return out
    }

    /**
     * Parses a tools/call response and returns content[0].text, or "" when absent.
     * Accepts the full JSON-RPC response or a bare result object. Throws [McpError] on error.
     */
    fun parseToolCallResult(json: String): String {
        val result = unwrapResult(JSONObject(json))
        val content = result.optJSONArray("content") ?: return ""
        if (content.length() == 0) return ""
        val first = content.optJSONObject(0) ?: return ""
        return first.optString("text")
    }

    private fun unwrapResult(obj: JSONObject): JSONObject {
        if (obj.has("error") && !obj.isNull("error")) {
            throw McpError(extractErrorMessage(obj))
        }
        if (obj.has("result") && !obj.isNull("result")) {
            return obj.getJSONObject("result")
        }
        return obj
    }

    private fun extractErrorMessage(obj: JSONObject): String {
        val err = obj.optJSONObject("error") ?: return "JSON-RPC error"
        return err.optString("message", "JSON-RPC error")
    }
}
