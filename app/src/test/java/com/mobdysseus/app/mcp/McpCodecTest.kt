package com.mobdysseus.app.mcp

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class McpCodecTest {

    @Test
    fun requestEncodesJsonRpc() {
        val params = JSONObject().put("name", "echo").put("arguments", JSONObject().put("x", 1))
        val json = McpCodec.request(7L, "tools/call", params)
        val obj = JSONObject(json)
        assertEquals("2.0", obj.getString("jsonrpc"))
        assertEquals(7L, obj.getLong("id"))
        assertEquals("tools/call", obj.getString("method"))
        assertEquals("echo", obj.getJSONObject("params").getString("name"))
    }

    @Test
    fun parseResultReturnsResultObject() {
        val json = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("result", JSONObject().put("ok", true))
            .toString()
        val result = McpCodec.parseResult(json)
        assertTrue(result != null)
        assertTrue(result!!.optBoolean("ok"))
    }

    @Test
    fun parseResultNullWhenAbsent() {
        val json = JSONObject().put("jsonrpc", "2.0").put("id", 2).toString()
        assertNull(McpCodec.parseResult(json))
    }

    @Test
    fun parseResultThrowsMcpError() {
        val json = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 3)
            .put("error", JSONObject().put("code", -32600).put("message", "invalid request"))
            .toString()
        try {
            McpCodec.parseResult(json)
            fail("expected McpError")
        } catch (e: McpError) {
            assertEquals("invalid request", e.message)
        }
    }

    @Test
    fun parseToolsParsesList() {
        val tools = JSONArray()
            .put(
                JSONObject()
                    .put("name", "echo")
                    .put("description", "echo back")
                    .put("inputSchema", JSONObject().put("type", "object"))
            )
            .put(
                JSONObject()
                    .put("name", "add")
                    .put("description", "add numbers")
                    .put("inputSchema", JSONObject())
            )
        val json = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 4)
            .put("result", JSONObject().put("tools", tools))
            .toString()
        val parsed = McpCodec.parseTools(json)
        assertEquals(2, parsed.size)
        assertEquals("echo", parsed[0].name)
        assertEquals("echo back", parsed[0].description)
        assertTrue(parsed[0].inputSchema.contains("\"type\""))
        assertEquals("add", parsed[1].name)
    }

    @Test
    fun parseToolCallResultReturnsText() {
        val content = JSONArray().put(JSONObject().put("type", "text").put("text", "hello world"))
        val json = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 5)
            .put("result", JSONObject().put("content", content))
            .toString()
        assertEquals("hello world", McpCodec.parseToolCallResult(json))
    }

    @Test
    fun parseToolCallResultEmptyWhenNoContent() {
        val json = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 6)
            .put("result", JSONObject())
            .toString()
        assertEquals("", McpCodec.parseToolCallResult(json))
    }
}
