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

data class LocalModelPreset(
    val id: String,
    val label: String,
    val repoId: String,
    val filename: String,
)

object LocalModelPresets {
    val all = listOf(
        LocalModelPreset("qwen3-0.6b", "Qwen3 0.6B (fast · ~450 MB)", "unsloth/Qwen3-0.6B-GGUF", "Qwen3-0.6B-Q4_K_M.gguf"),
        LocalModelPreset("qwen2.5-1.5b", "Qwen2.5 1.5B (~1 GB)", "Qwen/Qwen2.5-1.5B-Instruct-GGUF", "qwen2.5-1.5b-instruct-q4_k_m.gguf"),
        LocalModelPreset("qwen2.5-3b", "Qwen2.5 3B (recommended · ~2 GB)", "Qwen/Qwen2.5-3B-Instruct-GGUF", "qwen2.5-3b-instruct-q4_k_m.gguf"),
        LocalModelPreset("llama-3.2-3b", "Llama 3.2 3B (~2 GB)", "bartowski/Llama-3.2-3B-Instruct-GGUF", "Llama-3.2-3B-Instruct-Q4_K_M.gguf"),
    )
}

data class ProviderConfig(
    val baseUrl: String,
    val model: String,
    val apiKey: String,
    val useLocal: Boolean = false,
    val localRepoId: String = "Qwen/Qwen2.5-3B-Instruct-GGUF",
    val localFile: String = "qwen2.5-3b-instruct-q4_k_m.gguf",
)

class ProviderStore(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("provider", Context.MODE_PRIVATE)

    fun load(): ProviderConfig = ProviderConfig(
        baseUrl = prefs.getString("baseUrl", "") ?: "",
        model = prefs.getString("model", "") ?: "",
        apiKey = prefs.getString("apiKey", "") ?: "",
        useLocal = prefs.getBoolean("useLocal", false),
        localRepoId = prefs.getString("localRepoId", "Qwen/Qwen2.5-3B-Instruct-GGUF") ?: "Qwen/Qwen2.5-3B-Instruct-GGUF",
        localFile = prefs.getString("localFile", "qwen2.5-3b-instruct-q4_k_m.gguf") ?: "qwen2.5-3b-instruct-q4_k_m.gguf",
    )

    fun save(cfg: ProviderConfig) {
        prefs.edit()
            .putString("baseUrl", cfg.baseUrl)
            .putString("model", cfg.model)
            .putString("apiKey", cfg.apiKey)
            .putBoolean("useLocal", cfg.useLocal)
            .putString("localRepoId", cfg.localRepoId)
            .putString("localFile", cfg.localFile)
            .apply()
    }
}
