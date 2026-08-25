package com.mobdysseus.app.cookbook

data class CandidateModel(
    val repoId: String,
    val name: String,
    val paramsB: Float,
    val quant: String,
    val license: String,
)

data class RankedModel(
    val model: CandidateModel,
    val score: Float,
    val ramGb: Float,
    val fits: Boolean,
)

/**
 * On-device port of the desktop Cookbook's fit-scoring engine
 * (upstream `services/hwfit/models.py` + `fit.py`). The scoring is pure
 * deterministic math: RAM fit + quant quality penalty + a speed/architecture
 * bonus. The desktop uses GPU VRAM; here we use usable RAM + NPU.
 */
object ModelRanker {

    private val bytesPerParam = mapOf(
        "Q8_0" to 1.05f, "Q6_K" to 0.80f, "Q5_K_M" to 0.68f,
        "Q4_K_M" to 0.58f, "Q4_0" to 0.58f, "Q3_K_M" to 0.48f, "Q2_K" to 0.37f,
    )

    private val qualityPenalty = mapOf(
        "Q8_0" to 0f, "Q6_K" to -1f, "Q5_K_M" to -2f,
        "Q4_K_M" to -5f, "Q4_0" to -5f, "Q3_K_M" to -8f, "Q2_K" to -12f,
    )

    /** Recommend a quant tier for a given parameter count. */
    fun recommendQuant(paramsB: Float): String = when {
        paramsB <= 2f -> "Q8_0"
        paramsB <= 5f -> "Q4_K_M"
        paramsB <= 9f -> "Q4_K_M"
        else -> "Q3_K_M"
    }

    /** Estimated resident RAM for weights + context/KV overhead, in GB. */
    fun estimateRamGb(paramsB: Float, quant: String): Float =
        paramsB * (bytesPerParam[quant] ?: 0.6f) + 1.2f

    fun rank(models: List<CandidateModel>, hw: DeviceHardware): List<RankedModel> =
        models
            .map { m ->
                val ramGb = estimateRamGb(m.paramsB, m.quant)
                val fits = ramGb <= hw.usableRamGb
                val quality = 10f + (qualityPenalty[m.quant] ?: 0f)
                val fitBonus = if (fits) 6f else -25f
                val speedBonus = (9f - m.paramsB).coerceIn(0f, 6f)
                RankedModel(m, quality + fitBonus + speedBonus, ramGb, fits)
            }
            .sortedByDescending { it.score }
}
