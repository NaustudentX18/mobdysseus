package com.mobdysseus.app.provider

import android.content.Context
import android.content.SharedPreferences

data class ProviderPreset(
    val id: String,
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val needsKey: Boolean,
)

object ProviderPresets {
    val all = listOf(
        ProviderPreset("ollama", "Ollama (local)", "http://127.0.0.1:11434/v1", "qwen3:8b", false),
        ProviderPreset("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", true),
        ProviderPreset("openai", "OpenAI", "https://api.openai.com/v1", "gpt-4.1", true),
        ProviderPreset("custom", "Custom", "", "", true),
    )
}

data class ProviderConfig(
    val baseUrl: String,
    val model: String,
    val apiKey: String,
)

class ProviderStore(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("provider", Context.MODE_PRIVATE)

    fun load(): ProviderConfig = ProviderConfig(
        baseUrl = prefs.getString("baseUrl", "") ?: "",
        model = prefs.getString("model", "") ?: "",
        apiKey = prefs.getString("apiKey", "") ?: "",
    )

    fun save(cfg: ProviderConfig) {
        prefs.edit()
            .putString("baseUrl", cfg.baseUrl)
            .putString("model", cfg.model)
            .putString("apiKey", cfg.apiKey)
            .apply()
    }
}
