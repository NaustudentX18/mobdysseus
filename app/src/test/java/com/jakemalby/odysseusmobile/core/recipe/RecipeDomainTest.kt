package com.jakemalby.odysseusmobile.core.recipe

import com.jakemalby.odysseusmobile.capability.CapabilityId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeDomainTest {
    @Test
    fun allBuiltInsAreValidAndHaveStableVersions() {
        assertEquals(
            listOf("quick-chat@1", "deep-work@1", "document-companion@1", "voice-capture@1"),
            BuiltInRecipes.all.map { it.versionedId },
        )
        BuiltInRecipes.all.forEach { recipe ->
            assertTrue(recipe.modelRequirement.capabilities.contains(RecipeModelCapability.TEXT))
            assertTrue(RecipeValidator.validate(recipe).isEmpty())
        }
    }

    @Test
    fun validatorRejectsUnknownInputsDuplicateKeysAndProhibitedCapabilities() {
        val invalid = RecipeDefinition(
            id = "invalid",
            version = 1,
            displayName = "Invalid",
            promptTemplate = "{{unknown}}",
            inputSchema = listOf(
                RecipeInputField("message", "Message", RecipeInputKind.TEXT),
                RecipeInputField("message", "Message again", RecipeInputKind.TEXT),
            ),
            modelRequirement = RecipeModelRequirement(setOf(RecipeModelCapability.TEXT)),
            capabilityAllowlist = setOf(CapabilityId.SUBPROCESS),
        )

        assertEquals(
            setOf(
                RecipeValidationCode.DUPLICATE_INPUT_KEY,
                RecipeValidationCode.UNKNOWN_TEMPLATE_INPUT,
                RecipeValidationCode.MISSING_TEMPLATE_INPUT,
                RecipeValidationCode.PROHIBITED_CAPABILITY,
            ),
            RecipeValidator.validate(invalid).map { it.code }.toSet(),
        )
    }
}
