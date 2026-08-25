package com.mobdysseus.app.cookbook

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Model discovery: a curated catalog of known-good GGUF models for the
 * Snapdragon 8 Elite / 12 GB class, augmented by a live HuggingFace trending
 * query. Falls back to the curated list when offline.
 */
object Catalog {

    val curated = listOf(
        CandidateModel("Qwen/Qwen2.5-3B-Instruct-GGUF", "Qwen 2.5 3B Instruct", 3.0f, "Q4_K_M", "Apache-2.0"),
        CandidateModel("Qwen/Qwen2.5-1.5B-Instruct-GGUF", "Qwen 2.5 1.5B Instruct", 1.5f, "Q8_0", "Apache-2.0"),
        CandidateModel("bartowski/Llama-3.2-3B-Instruct-GGUF", "Llama 3.2 3B Instruct", 3.0f, "Q4_K_M", "Llama"),
        CandidateModel("microsoft/Phi-3.5-mini-instruct-gguf", "Phi-3.5 Mini", 3.8f, "Q4_K_M", "MIT"),
        CandidateModel("google/gemma-2-2b-it-GGUF", "Gemma 2 2B IT", 2.0f, "Q8_0", "Gemma"),
        CandidateModel("Qwen/Qwen2.5-7B-Instruct-GGUF", "Qwen 2.5 7B Instruct", 7.0f, "Q4_K_M", "Apache-2.0"),
        CandidateModel("bartowski/Llama-3.1-8B-Instruct-GGUF", "Llama 3.1 8B Instruct", 8.0f, "Q4_K_M", "Llama"),
    )

    suspend fun latest(): List<CandidateModel> = withContext(Dispatchers.IO) {
        try {
            val url = "https://huggingface.co/api/models" +
                "?sort=trendingScore&direction=-1&limit=60&filter=text-generation"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            val code = conn.responseCode
            if (code !in 200..299) return@withContext curated
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            (parseHf(text) + curated).distinctBy { it.repoId }
        } catch (_: Exception) {
            curated
        }
    }

    private val excluded = listOf(
        "lora", "adapter", "peft", "qlora", "embedding", "dataset",
        "reward", "rm-", "merge", "draft", "speculative",
    )

    private fun parseHf(json: String): List<CandidateModel> {
        val arr = JSONArray(json)
        val out = mutableListOf<CandidateModel>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val repoId = o.optString("modelId").ifBlank { o.optString("id") }
            if (repoId.isBlank()) continue
            val lower = repoId.lowercase()
            if (excluded.any { it in lower }) continue
            val params = extractParams(repoId) ?: continue
            if (params > 9f) continue // above the 12 GB on-device ceiling
            val quant = ModelRanker.recommendQuant(params)
            val name = repoId.substringAfterLast('/')
            out.add(CandidateModel(repoId, name, params, quant, ""))
        }
        return out
    }

    private val paramRegex = Regex("[-_/](\\d+(?:\\.\\d+)?)\\s*[bB](?![a-zA-Z])")

    private fun extractParams(repoId: String): Float? =
        paramRegex.find(repoId)?.groupValues?.get(1)?.toFloatOrNull()
}
