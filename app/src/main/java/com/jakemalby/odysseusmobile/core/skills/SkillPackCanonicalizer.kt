package com.jakemalby.odysseusmobile.core.skills

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/** Stable, length-prefixed binary encoding used exclusively for signing. */
object SkillPackCanonicalizer {
    private const val MAGIC = "MOBDYSSEUS-SKILL-PACK"

    fun bytes(manifest: SkillPackManifest): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { sink ->
            sink.writeUtf8(MAGIC)
            sink.writeInt(manifest.formatVersion)
            sink.writeUtf8(manifest.id)
            sink.writeUtf8(manifest.version)
            sink.writeUtf8(manifest.displayName)
            sink.writeUtf8(manifest.description)
            sink.writeUtf8(manifest.publisherKeyId)
            sink.writeStringList(manifest.allowedCapabilities.map { it.name }.sorted())
            val recipes = manifest.recipes.sortedBy { it.id }
            sink.writeInt(recipes.size)
            recipes.forEach { recipe ->
                sink.writeUtf8(recipe.id)
                sink.writeUtf8(recipe.displayName)
                sink.writeUtf8(recipe.prompt)
                val inputs = recipe.inputs.sortedBy { it.id }
                sink.writeInt(inputs.size)
                inputs.forEach { input ->
                    sink.writeUtf8(input.id)
                    sink.writeUtf8(input.label)
                    sink.writeUtf8(input.type.name)
                    sink.writeBoolean(input.required)
                    sink.writeInt(input.maxLength ?: -1)
                    sink.writeStringList(input.choices.sorted())
                }
                val actions = recipe.actions.sortedBy { it.id }
                sink.writeInt(actions.size)
                actions.forEach { action ->
                    sink.writeUtf8(action.id)
                    sink.writeUtf8(action.capability.name)
                    sink.writeUtf8(action.rationale)
                }
            }
        }
        return output.toByteArray()
    }

    private fun DataOutputStream.writeStringList(values: List<String>) {
        writeInt(values.size)
        values.forEach { writeUtf8(it) }
    }

    private fun DataOutputStream.writeUtf8(value: String) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        writeInt(encoded.size)
        write(encoded)
    }
}
