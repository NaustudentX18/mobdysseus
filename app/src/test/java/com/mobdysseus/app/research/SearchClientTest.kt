package com.mobdysseus.app.research

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchClientTest {

    @Test
    fun parseLeafTopic() {
        val json = JSONObject()
            .put(
                "RelatedTopics",
                JSONArray().put(
                    JSONObject()
                        .put("Text", "DuckDuckGo is a search engine")
                        .put("FirstURL", "https://duckduckgo.com/")
                )
            )
        val results = SearchClient.parseResponse(json)
        assertEquals(1, results.size)
        assertEquals("DuckDuckGo is a search engine", results[0].title)
        assertEquals("https://duckduckgo.com/", results[0].url)
        assertEquals("DuckDuckGo is a search engine", results[0].snippet)
    }

    @Test
    fun parseCategoryTopic() {
        val json = JSONObject()
            .put(
                "RelatedTopics",
                JSONArray().put(
                    JSONObject()
                        .put("Name", "Search engines")
                        .put(
                            "Topics",
                            JSONArray()
                                .put(
                                    JSONObject()
                                        .put("Text", "Google")
                                        .put("FirstURL", "https://google.com/")
                                )
                                .put(
                                    JSONObject()
                                        .put("Text", "Bing")
                                        .put("FirstURL", "https://bing.com/")
                                )
                        )
                )
            )
        val results = SearchClient.parseResponse(json)
        assertEquals(2, results.size)
        assertEquals("Google", results[0].title)
        assertEquals("https://google.com/", results[0].url)
        assertEquals("Bing", results[1].title)
        assertEquals("https://bing.com/", results[1].url)
    }

    @Test
    fun parseAbstract() {
        val json = JSONObject()
            .put("AbstractText", "DuckDuckGo is an internet search engine.")
            .put("AbstractURL", "https://en.wikipedia.org/wiki/DuckDuckGo")
        val results = SearchClient.parseResponse(json)
        assertEquals(1, results.size)
        assertEquals("DuckDuckGo is an internet search engine.", results[0].title)
        assertEquals("https://en.wikipedia.org/wiki/DuckDuckGo", results[0].url)
    }

    @Test
    fun parseEmptyResponseDoesNotCrash() {
        val results = SearchClient.parseResponse(JSONObject())
        assertTrue(results.isEmpty())
    }

    @Test
    fun parseMalformedFieldsDoesNotCrash() {
        val json = JSONObject()
            .put(
                "RelatedTopics",
                JSONArray()
                    .put(JSONObject().put("Text", "No URL here"))
                    .put("not an object")
                    .put(JSONObject().put("FirstURL", "https://example.com/"))
            )
        val results = SearchClient.parseResponse(json)
        assertEquals(2, results.size)
        assertEquals("No URL here", results[0].title)
        assertEquals("", results[0].url)
        assertEquals("https://example.com/", results[1].url)
    }
}
