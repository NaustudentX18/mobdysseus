package com.mobdysseus.app.research

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String,
)

class SearchClient(
    val endpoint: String = "https://api.duckduckgo.com/",
) {

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val url = buildUrl(query)
        val json = fetchJson(url)
        val results = parseResponse(json)
        if (results.isEmpty()) {
            throw IllegalStateException("No results found for query: $query")
        }
        results
    }

    private fun buildUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return "${endpoint}?q=$encoded&format=json&no_html=1&skip_disambig=1&t=mobdysseus"
    }

    private fun fetchJson(urlString: String): JSONObject {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("Search request failed with HTTP $code")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body)
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("Search request failed: ${e.message}", e)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        internal fun parseResponse(json: JSONObject): List<SearchResult> {
            val results = mutableListOf<SearchResult>()

            val abstractText = json.optString("AbstractText")
            val abstractUrl = json.optString("AbstractURL")
            if (abstractText.isNotBlank()) {
                results.add(SearchResult(title = abstractText, url = abstractUrl, snippet = abstractText))
            }

            val related = json.optJSONArray("RelatedTopics")
            if (related != null) {
                for (i in 0 until related.length()) {
                    val item = related.optJSONObject(i) ?: continue
                    val topics = item.optJSONArray("Topics")
                    if (topics != null) {
                        for (j in 0 until topics.length()) {
                            val leaf = topics.optJSONObject(j) ?: continue
                            addLeaf(results, leaf)
                        }
                    } else {
                        addLeaf(results, item)
                    }
                }
            }

            return results
        }

        private fun addLeaf(results: MutableList<SearchResult>, leaf: JSONObject) {
            val url = leaf.optString("FirstURL")
            val text = leaf.optString("Text")
            if (text.isBlank() && url.isBlank()) return
            results.add(SearchResult(title = text, url = url, snippet = text))
        }
    }
}
