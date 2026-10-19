package org.garbageclassifier.core

import org.junit.Assert.*
import org.junit.Test

class WasteRulesTest {
    private val box = Box(0f, 0f, 100f, 100f)
    private fun detection(id: Int = 7, confidence: Float = 0.9f, margin: Float = 0.8f) =
        Detection(id, confidence, margin, box)
    private fun filter() = RecognitionFilter(ModelContract.materials)

    @Test fun foodWasteMapsToWet() {
        assertEquals(BinType.WET, ModelContract.materials[1].bin)
    }

    @Test fun supportedDryMaterialsMapToDry() {
        for (id in listOf(2, 4, 7, 9)) assertEquals(BinType.DRY, ModelContract.materials[id].bin)
    }

    @Test fun ambiguousMaterialsHaveNoRecommendation() {
        for (id in listOf(0, 3, 5, 6, 8)) assertNull(ModelContract.materials[id].bin)
    }

    @Test fun labelOrderMatchesVerifiedModelMetadata() {
        assertEquals(listOf("Not Recyclable", "Food waste", "Glass", "Textile", "Metal", "Wooden",
            "Leather", "Plastic", "Ceramic", "Paper"), ModelContract.materials.map { it.label })
        assertEquals((0..9).toList(), ModelContract.materials.map { it.id })
    }

    @Test fun firstSightingDoesNotRecommendABin() {
        assertNull(filter().update(listOf(detection()), 0).single().bin)
    }

    @Test fun twoConsistentSightingsRecommendBin() {
        val filter = filter()
        filter.update(listOf(detection()), 0)
        assertEquals(BinType.DRY, filter.update(listOf(detection()), 200).single().bin)
    }

    @Test fun lowConfidenceNeverRecommendsBin() {
        val filter = filter()
        repeat(5) { assertNull(filter.update(listOf(detection(confidence = 0.59f)), it * 200L).single().bin) }
    }

    @Test fun conflictingClassScoresStayUncertain() {
        val filter = filter()
        filter.update(listOf(detection(margin = 0.1f)), 0)
        assertNull(filter.update(listOf(detection(margin = 0.1f)), 200).single().bin)
    }

    @Test fun highConfidenceUnsupportedClassStaysUncertain() {
        val filter = filter()
        filter.update(listOf(detection(id = 0)), 0)
        assertNull(filter.update(listOf(detection(id = 0)), 200).single().bin)
    }

    @Test fun unknownClassIdStaysUncertain() {
        val filter = filter()
        filter.update(listOf(detection(id = 55)), 0)
        assertNull(filter.update(listOf(detection(id = 55)), 200).single().bin)
    }

    @Test fun staleSightingDoesNotConfirmNewObject() {
        val filter = filter()
        filter.update(listOf(detection()), 0)
        assertNull(filter.update(listOf(detection()), 1000).single().bin)
    }

    @Test fun clockRegressionDoesNotConfirmNewObject() {
        val filter = filter()
        filter.update(listOf(detection()), 500)
        assertNull(filter.update(listOf(detection()), 300).single().bin)
    }

    @Test fun disappearingObjectIsNotHeldOnScreen() {
        val filter = filter()
        filter.update(listOf(detection()), 0)
        assertTrue(filter.update(emptyList(), 200).isEmpty())
        assertNull(filter.update(listOf(detection()), 400).single().bin)
    }

    @Test fun differentLocationsCannotConfirmEachOther() {
        val filter = filter()
        filter.update(listOf(detection()), 0)
        val elsewhere = detection().copy(box = Box(200f, 200f, 300f, 300f))
        assertNull(filter.update(listOf(elsewhere), 200).single().bin)
    }

    @Test fun changedClassCannotConfirmObject() {
        val filter = filter()
        filter.update(listOf(detection()), 0)
        assertNull(filter.update(listOf(detection(id = 1)), 200).single().bin)
    }

    @Test fun previousObjectCanOnlyConfirmOneCurrentDetection() {
        val filter = filter()
        filter.update(listOf(detection()), 0)
        assertEquals(1, filter.update(listOf(detection(), detection()), 200).count { it.confirmed })
    }

    @Test fun overlappingConfidentMaterialDisagreementsStayUncertain() {
        val filter = filter()
        val conflicting = listOf(detection(7), detection(1))
        filter.update(conflicting, 0)
        assertTrue(filter.update(conflicting, 200).all { it.bin == null })
        assertNull(filter.update(listOf(detection(7)), 400).single().bin)
    }

    @Test fun separateObjectsCanBeConfirmedTogether() {
        val filter = filter()
        val objects = listOf(detection(7), detection(1).copy(box = Box(200f, 0f, 300f, 100f)))
        filter.update(objects, 0)
        assertEquals(2, filter.update(objects, 200).count { it.confirmed })
    }

    @Test fun resetClearsConfirmation() {
        val filter = filter()
        filter.update(listOf(detection()), 0)
        filter.reset()
        assertNull(filter.update(listOf(detection()), 200).single().bin)
    }

    @Test fun wetObjectInDryBinIsWrong() {
        val item = Recognition(detection(1), ModelContract.materials[1], true)
        assertEquals(SortingStatus.WRONG, WasteRules.inspect(item, BinType.DRY))
    }

    @Test fun dryObjectInWetBinIsWrong() {
        val item = Recognition(detection(), ModelContract.materials[7], true)
        assertEquals(SortingStatus.WRONG, WasteRules.inspect(item, BinType.WET))
    }

    @Test fun matchingBinIsCorrect() {
        for (id in listOf(1, 2, 4, 7, 9)) {
            val material = ModelContract.materials[id]
            val item = Recognition(detection(id), material, true)
            assertEquals(SortingStatus.CORRECT, WasteRules.inspect(item, checkNotNull(material.bin)))
        }
    }

    @Test fun uncertainObjectIsNeverMarkedCorrectOrWrong() {
        val item = Recognition(detection(), ModelContract.materials[7], false)
        for (bin in BinType.entries) assertEquals(SortingStatus.UNCERTAIN, WasteRules.inspect(item, bin))
    }

    @Test fun unsupportedObjectIsNeverMarkedCorrectOrWrong() {
        val item = Recognition(detection(0), ModelContract.materials[0], true)
        for (bin in BinType.entries) assertEquals(SortingStatus.UNCERTAIN, WasteRules.inspect(item, bin))
    }
}
