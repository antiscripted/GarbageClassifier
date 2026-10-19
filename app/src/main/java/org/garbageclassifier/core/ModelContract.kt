package org.garbageclassifier.core

object ModelContract {
    const val INPUT_SIZE = 320
    const val CANDIDATES = 2100
    const val CHANNELS = 14
    const val SHA256 = "9c258ac8a0344cd07deb28ddcedb096e5bd30c092b807ff7e442f40279d8f54d"
    const val ASSET = "trash_detector.tflite"

    val materials = listOf(
        Material(0, "Not Recyclable", null),
        Material(1, "Food waste", BinType.WET),
        Material(2, "Glass", BinType.DRY),
        Material(3, "Textile", null),
        Material(4, "Metal", BinType.DRY),
        Material(5, "Wooden", null),
        Material(6, "Leather", null),
        Material(7, "Plastic", BinType.DRY),
        Material(8, "Ceramic", null),
        Material(9, "Paper", BinType.DRY)
    )
}

class YoloDecoder {
    fun decode(output: FloatArray, letterbox: Letterbox): List<Detection> {
        require(output.size == ModelContract.CHANNELS * ModelContract.CANDIDATES)
        require(letterbox.inputSize == ModelContract.INPUT_SIZE)
        val detections = ArrayList<Detection>()
        val count = ModelContract.CANDIDATES
        for (index in 0 until count) {
            var best = 0f
            var second = 0f
            var classId = -1
            var validScores = true
            for (label in ModelContract.materials.indices) {
                val score = output[(label + 4) * count + index]
                if (!score.isFinite() || score !in 0f..1f) {
                    validScores = false
                    break
                }
                if (score > best) {
                    second = best
                    best = score
                    classId = label
                } else if (score > second) {
                    second = score
                }
            }
            if (!validScores || best < 0.25f || classId < 0) continue
            val x = output[index] * ModelContract.INPUT_SIZE
            val y = output[count + index] * ModelContract.INPUT_SIZE
            val width = output[2 * count + index] * ModelContract.INPUT_SIZE
            val height = output[3 * count + index] * ModelContract.INPUT_SIZE
            val modelBox = Box(x - width / 2, y - height / 2, x + width / 2, y + height / 2)
            if (!modelBox.isValid) continue
            val box = letterbox.toSource(modelBox)
            if (box.width < 2f || box.height < 2f) continue
            detections += Detection(classId, best, best - second, box)
        }
        return Suppression.apply(detections)
    }
}
