package com.mobdysseus.app.cookbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRankerTest {

    private val hw = DeviceHardware(
        socModel = "Snapdragon 8 Elite",
        model = "SM-S931B",
        totalRamGb = 12f,
        usableRamGb = 8.5f,
        hasNpu = true,
        npuTops = 45f,
        freeStorageGb = 200f,
    )

    @Test
    fun curatedModelsAllPresent() {
        assertTrue(Catalog.curated.isNotEmpty())
    }

    @Test
    fun threeBModelFitsAndSevenBRanksLower() {
        val threeB = CandidateModel("x/3b", "3B", 3.0f, "Q4_K_M", "")
        val sevenB = CandidateModel("x/7b", "7B", 7.0f, "Q4_K_M", "")
        val ranked = ModelRanker.rank(listOf(threeB, sevenB), hw)
        assertEquals("x/3b", ranked.first().model.repoId)
        assertTrue(ranked.first().fits)
    }

    @Test
    fun memoryEstimateGrowsWithParams() {
        val small = ModelRanker.estimateRamGb(3f, "Q4_K_M")
        val big = ModelRanker.estimateRamGb(7f, "Q4_K_M")
        assertTrue(big > small)
    }

    @Test
    fun higherQuantIsLowerQuality() {
        val q4 = ModelRanker.rank(listOf(CandidateModel("a", "a", 3f, "Q4_K_M", "")), hw).first()
        val q8 = ModelRanker.rank(listOf(CandidateModel("b", "b", 3f, "Q8_0", "")), hw).first()
        assertTrue(q8.score > q4.score)
    }
}
