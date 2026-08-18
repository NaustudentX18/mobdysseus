package com.jakemalby.odysseusmobile.core.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageEditRecipeTest {
    @Test fun fourRotationsReturnToOriginalOrientation() {
        val original = ImageEditRecipe()
        val rotated = (1..4).fold(original) { recipe, _ -> recipe.rotateClockwise() }
        assertEquals(original, rotated)
    }

    @Test fun outputNameIsStableAndSafe() {
        val recipe = ImageEditRecipe(quarterTurnsClockwise = 1, brightness = .2f)
        assertEquals(recipe.deterministicOutputName("My photo!.JPG"), recipe.deterministicOutputName("My photo!.JPG"))
        assertEquals(true, recipe.deterministicOutputName("My photo!.JPG").matches(Regex("My-photo-edit-[0-9a-f]{12}\\.jpg")))
        assertNotEquals(recipe.deterministicOutputName("photo.jpg"), recipe.rotateClockwise().deterministicOutputName("photo.jpg"))
        assertNotEquals(
            recipe.deterministicOutputName("photo.jpg", sourceIdentity = "first"),
            recipe.deterministicOutputName("photo.jpg", sourceIdentity = "second"),
        )
    }

    @Test fun cropRejectsOutOfRangeAndEmptyBounds() {
        assertThrows(IllegalArgumentException::class.java) { NormalizedCrop(-.1f, 0f, 1f, 1f) }
        assertThrows(IllegalArgumentException::class.java) { NormalizedCrop(.5f, 0f, .5f, 1f) }
    }

    @Test fun tonalEditsAreBounded() {
        assertThrows(IllegalArgumentException::class.java) { ImageEditRecipe(brightness = 1.1f) }
        assertThrows(IllegalArgumentException::class.java) { ImageEditRecipe(contrast = 2.1f) }
    }
}
