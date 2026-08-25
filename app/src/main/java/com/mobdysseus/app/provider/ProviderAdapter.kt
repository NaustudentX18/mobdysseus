package com.mobdysseus.app.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val role: String, val content: String)

/**
 * Minimal OpenAI-compatible chat client with Server-Sent-Events streaming.
 * Talks to any endpoint that speaks the /v1/chat/completions contract
 * (OpenAI, DeepSeek, Ollama, MiniMax, local servers, ...).
 */
class ProviderAdapter(private val config: ProviderConfig) {

    fun stream(messages: List<ChatMessage>): Flow<String> = flow {
        val endpoint = config.baseUrl.trimEnd('/') + "/chat/completions"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "text/event-stream")
        if (config.apiKey.isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + config.apiKey)
        }
        conn.connectTimeout = 30000
        conn.readTimeout = 0
        conn.doOutput = true

        val body = JSONObject().apply {
            put("model", config.model)
            put("stream", true)
            put("messages", JSONArray().apply {
                for (m in messages) {
                    put(JSONObject().put("role", m.role).put("content", m.content))
                }
            })
        }

        conn.outputStream.use { os ->
            os.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            throw IllegalStateException("Provider error ($code): $err")
        }

        conn.inputStream.bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
                if (line.startsWith("data:")) {
                    val data = line.substring(5).trim()
                    if (data.isNotEmpty() && data != "[DONE]") {
                        try {
                            val obj = JSONObject(data)
                            val choices = obj.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                val content = delta?.optString("content") ?: ""
                                if (content.isNotEmpty()) emit(content)
                            }
                        } catch (_: Exception) {
                            // skip malformed chunk
                        }
                    }
                }
                line = reader.readLine()
            }
        }
    }.flowOn(Dispatchers.IO)
}
