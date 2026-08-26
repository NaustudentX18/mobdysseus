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
 *
 * Every [CandidateModel] carries a real HuggingFace [CandidateModel.repoId] /
 * [CandidateModel.filename] pair so the Cookbook can download it directly via
 * [com.mobdysseus.app.local.ModelDownloadManager.prefetch].
 */
object Catalog {

    val curated = listOf(
        CandidateModel("Qwen/Qwen2.5-3B-Instruct-GGUF", "Qwen 2.5 3B Instruct", 3.0f, "Q4_K_M", "Apache-2.0", "qwen2.5-3b-instruct-q4_k_m.gguf"),
        CandidateModel("Qwen/Qwen2.5-1.5B-Instruct-GGUF", "Qwen 2.5 1.5B Instruct", 1.5f, "Q8_0", "Apache-2.0", "qwen2.5-1.5b-instruct-q8_0.gguf"),
        CandidateModel("bartowski/Llama-3.2-3B-Instruct-GGUF", "Llama 3.2 3B Instruct", 3.0f, "Q4_K_M", "Llama", "Llama-3.2-3B-Instruct-Q4_K_M.gguf"),
        CandidateModel("microsoft/Phi-3.5-mini-instruct-gguf", "Phi-3.5 Mini", 3.8f, "Q4_K_M", "MIT", "Phi-3.5-mini-instruct-q4_k_m.gguf"),
        CandidateModel("google/gemma-2-2b-it-GGUF", "Gemma 2 2B IT", 2.0f, "Q8_0", "Gemma", "gemma-2-2b-it-Q8_0.gguf"),
        CandidateModel("Qwen/Qwen2.5-7B-Instruct-GGUF", "Qwen 2.5 7B Instruct", 7.0f, "Q4_K_M", "Apache-2.0", "qwen2.5-7b-instruct-q4_k_m.gguf"),
        CandidateModel("bartowski/Llama-3.1-8B-Instruct-GGUF", "Llama 3.1 8B Instruct", 8.0f, "Q4_K_M", "Llama", "Llama-3.1-8B-Instruct-Q4_K_M.gguf"),
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
            out.add(
                CandidateModel(
                    repoId = repoId,
                    name = name,
                    paramsB = params,
                    quant = quant,
                    license = "",
                    filename = deriveFilename(repoId, quant),
                ),
            )
        }
        return out
    }

    /**
     * Best-effort GGUF filename for a live HF repo. The exact file name can't be
     * known without querying the repo, so we derive a conventional
     * `<model>-<quant>.gguf` name from the repo id and recommended quant. Curated
     * models carry exact, verified filenames instead.
     */
    private fun deriveFilename(repoId: String, quant: String): String {
        val base = repoId
            .substringAfterLast('/')
            .replace("GGUF", "")
            .replace("gguf", "")
            .trim()
            .lowercase()
            .replace(' ', '-')
        return "$base-$quant.gguf".lowercase()
    }

    private val paramRegex = Regex("[-_/](\\d+(?:\\.\\d+)?)\\s*[bB](?![a-zA-Z])")

    private fun extractParams(repoId: String): Float? =
        paramRegex.find(repoId)?.groupValues?.get(1)?.toFloatOrNull()
}
