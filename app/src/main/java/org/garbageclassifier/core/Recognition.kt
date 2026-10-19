package org.garbageclassifier.core

data class Detection(val classId: Int, val confidence: Float, val margin: Float, val box: Box)

enum class BinType { WET, DRY }
enum class SortingStatus { CORRECT, WRONG, UNCERTAIN }

data class Material(val id: Int, val label: String, val bin: BinType?)
data class Recognition(val detection: Detection, val material: Material?, val confirmed: Boolean) {
    val bin: BinType? get() = if (confirmed) material?.bin else null
}

object WasteRules {
    fun inspect(recognition: Recognition, selectedBin: BinType): SortingStatus =
        when (recognition.bin) {
            null -> SortingStatus.UNCERTAIN
            selectedBin -> SortingStatus.CORRECT
            else -> SortingStatus.WRONG
        }
}

class RecognitionFilter(private val materials: List<Material>) {
    private var previous = emptyList<Detection>()
    private var previousTime = Long.MIN_VALUE

    fun update(detections: List<Detection>, timeMs: Long): List<Recognition> {
        val recent = previousTime != Long.MIN_VALUE && timeMs - previousTime in 0..700
        val available = if (recent) previous.toMutableList() else mutableListOf()
        val candidates = detections.filter { detection ->
            detection.confidence >= CONFIDENCE && detection.margin >= MARGIN &&
                detection.box.isValid && materials.getOrNull(detection.classId)?.bin != null &&
                detections.none { other ->
                    other.classId != detection.classId && other.confidence >= CONFIDENCE &&
                        other.box.intersectionOverUnion(detection.box) >= 0.60f
                }
        }
        val results = detections.map { detection ->
            val material = materials.getOrNull(detection.classId)
            val eligible = detection in candidates
            val match = if (eligible) available.indexOfFirst {
                it.classId == detection.classId && it.box.intersectionOverUnion(detection.box) >= 0.35f
            } else -1
            if (match >= 0) available.removeAt(match)
            Recognition(detection, material, eligible && match >= 0)
        }
        previous = candidates
        previousTime = timeMs
        return results
    }

    fun reset() {
        previous = emptyList()
        previousTime = Long.MIN_VALUE
    }

    companion object {
        const val CONFIDENCE = 0.60f
        const val MARGIN = 0.15f
    }
}

object Suppression {
    fun apply(detections: List<Detection>, threshold: Float = 0.45f, limit: Int = 20): List<Detection> {
        require(threshold in 0f..1f && limit > 0)
        val sorted = detections.filter { it.box.isValid && it.confidence.isFinite() }
            .sortedByDescending { it.confidence }
        val kept = mutableListOf<Detection>()
        for (candidate in sorted) {
            if (kept.none {
                it.classId == candidate.classId && it.box.intersectionOverUnion(candidate.box) > threshold
            }) {
                kept += candidate
                if (kept.size == limit) break
            }
        }
        return kept
    }
}
