package com.jakemalby.odysseusmobile.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline-only recommendations for the Galaxy S25 family.
 *
 * This intentionally contains profiles, not unverified download links or fake
 * hashes. A future signed remote-catalog client may convert a verified artifact
 * into [ModelCatalogEntry], then apply these requirements before offering it.
 */
data class S25ModelCatalog(
    val schemaVersion: Int,
    val profiles: List<S25ModelProfile>,
) {
    init {
        require(schemaVersion == CURRENT_SCHEMA_VERSION) { "Unsupported S25 catalog schema: $schemaVersion" }
        require(profiles.map(S25ModelProfile::id).distinct().size == profiles.size) { "S25 catalog profile ids must be unique" }
    }

    fun recommendationFor(id: String): S25ModelProfile? = profiles.firstOrNull { it.id == id }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class S25ModelProfile(
    val id: String,
    val displayName: String,
    val family: String,
    val format: ModelFormat,
    val minimumApiLevel: Int,
    val minimumAvailableRamBytes: Long,
    val minimumFreeStorageBytes: Long,
    val preferredBackends: List<ModelBackend>,
    val capabilities: Set<ModelCapability>,
    val chargingRecommended: Boolean,
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9._-]*"))) { "Invalid S25 profile id" }
        require(displayName.isNotBlank() && family.isNotBlank())
        require(minimumApiLevel > 0)
        require(minimumAvailableRamBytes > 0 && minimumFreeStorageBytes > 0)
        require(preferredBackends.isNotEmpty())
        require(preferredBackends.distinct().size == preferredBackends.size)
        require(capabilities.isNotEmpty())
    }

    fun isSuitableFor(device: DeviceProfile): Boolean =
        device.apiLevel >= minimumApiLevel &&
            device.availableRamBytes >= minimumAvailableRamBytes &&
            device.availableStorageBytes >= minimumFreeStorageBytes &&
            preferredBackends.any(device.availableBackends::contains)
}

/** Strict parser for built-in or bundled JSON; it deliberately performs no I/O or network calls. */
object S25ModelCatalogParser {
    fun parse(json: String): S25ModelCatalog = try {
        val root = JSONObject(json)
        val version = root.requireInt("schemaVersion")
        val profiles = root.requireArray("profiles").mapObjects(::parseProfile)
        S25ModelCatalog(version, profiles)
    } catch (error: Exception) {
        throw IllegalArgumentException("Invalid S25 model catalog: ${error.message}", error)
    }

    private fun parseProfile(value: JSONObject): S25ModelProfile = S25ModelProfile(
        id = value.requireString("id"),
        displayName = value.requireString("displayName"),
        family = value.requireString("family"),
        format = enumValueOf<ModelFormat>(value.requireString("format")),
        minimumApiLevel = value.requireInt("minimumApiLevel"),
        minimumAvailableRamBytes = value.requireLong("minimumAvailableRamBytes"),
        minimumFreeStorageBytes = value.requireLong("minimumFreeStorageBytes"),
        preferredBackends = value.requireArray("preferredBackends").mapStrings().map { enumValueOf<ModelBackend>(it) },
        capabilities = value.requireArray("capabilities").mapStrings().map { enumValueOf<ModelCapability>(it) }.toSet(),
        chargingRecommended = value.requireBoolean("chargingRecommended"),
    )

    private fun JSONObject.requireString(name: String): String = getString(name).also {
        require(it.isNotBlank()) { "$name must not be blank" }
    }

    private fun JSONObject.requireInt(name: String): Int = getInt(name)
    private fun JSONObject.requireLong(name: String): Long = getLong(name)
    private fun JSONObject.requireBoolean(name: String): Boolean = getBoolean(name)
    private fun JSONObject.requireArray(name: String): JSONArray = getJSONArray(name)
    private fun JSONArray.mapStrings(): List<String> = List(length()) { index -> getString(index) }
    private fun <T> JSONArray.mapObjects(map: (JSONObject) -> T): List<T> = List(length()) { index ->
        map(getJSONObject(index))
    }
}

/** Versioned profiles are safe to ship: none gives an endpoint, hash, or credential. */
object BuiltinS25ModelCatalog {
    private const val JSON = """
        {
          "schemaVersion": 1,
          "profiles": [
            {
              "id": "private-quick-chat",
              "displayName": "Private quick chat",
              "family": "Gemma 3 1B-class",
              "format": "LITERT_LM",
              "minimumApiLevel": 26,
              "minimumAvailableRamBytes": 4294967296,
              "minimumFreeStorageBytes": 4294967296,
              "preferredBackends": ["NPU","GPU","CPU"],
              "capabilities": ["TEXT"],
              "chargingRecommended": false
            },
            {
              "id": "private-multimodal",
              "displayName": "Private document and vision work",
              "family": "Gemma 3n E2B-class",
              "format": "LITERT_LM",
              "minimumApiLevel": 26,
              "minimumAvailableRamBytes": 8589934592,
              "minimumFreeStorageBytes": 8589934592,
              "preferredBackends": ["NPU","GPU"],
              "capabilities": ["TEXT","VISION","AUDIO"],
              "chargingRecommended": true
            },
            {
              "id": "deep-private-work",
              "displayName": "Deep private work",
              "family": "Gemma 3n E4B-class",
              "format": "LITERT_LM",
              "minimumApiLevel": 26,
              "minimumAvailableRamBytes": 12884901888,
              "minimumFreeStorageBytes": 12884901888,
              "preferredBackends": ["NPU","GPU"],
              "capabilities": ["TEXT","VISION","AUDIO","TOOL_USE"],
              "chargingRecommended": true
            }
          ]
        }
    """

    val value: S25ModelCatalog by lazy { S25ModelCatalogParser.parse(JSON) }
}
