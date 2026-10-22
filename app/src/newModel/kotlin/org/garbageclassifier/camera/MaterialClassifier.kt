package org.garbageclassifier.camera

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.os.SystemClock
import org.garbageclassifier.core.AntialiasedResize
import org.garbageclassifier.core.BinType
import org.garbageclassifier.core.Material
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.MessageDigest
import kotlin.math.exp

data class MaterialPrediction(val classId: Int, val confidence: Float, val margin: Float, val inferenceMs: Long)

class MaterialClassifier(context: Context) : Closeable {
    private val environment = OrtEnvironment.getEnvironment()
    private val input = ByteBuffer.allocateDirect(SIZE * SIZE * 3 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val model = context.assets.openFd(ASSET).use { asset ->
        asset.createInputStream().use { stream ->
            stream.channel.map(FileChannel.MapMode.READ_ONLY, asset.startOffset, asset.declaredLength)
        }
    }
    private val session: OrtSession

    init {
        val digest = MessageDigest.getInstance("SHA-256").apply { update(model.duplicate()) }
            .digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        check(digest == SHA256) { "Unexpected material model checksum" }
        session = OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(2)
            options.setInterOpNumThreads(1)
            options.addConfigEntry("session.intra_op.allow_spinning", "0")
            options.addConfigEntry("session.inter_op.allow_spinning", "0")
            environment.createSession(model, options)
        }
        try {
            check(session.inputNames == setOf("pixels") && session.outputNames == setOf("logits"))
            check((session.inputInfo["pixels"]!!.info as TensorInfo).shape.contentEquals(SHAPE))
            check((session.outputInfo["logits"]!!.info as TensorInfo).shape.contentEquals(longArrayOf(1, 10)))
        } catch (error: Exception) {
            session.close()
            throw error
        }
    }

    fun classify(pixels: IntArray, side: Int, rotation: Int): MaterialPrediction {
        require(rotation in listOf(0, 90, 180, 270))
        val upright = if (rotation == 0) pixels else IntArray(pixels.size).also { output ->
            for (y in 0 until side) for (x in 0 until side) {
                val index = when (rotation) {
                    90 -> x * side + side - 1 - y
                    180 -> (side - 1 - y) * side + side - 1 - x
                    else -> (side - 1 - x) * side + y
                }
                output[index] = pixels[y * side + x]
            }
        }
        val resized = AntialiasedResize.square(upright, side, SIZE)
        input.rewind()
        for (shift in listOf(16, 8, 0)) for (pixel in resized) {
            input.put(((pixel shr shift) and 255) / 127.5f - 1f)
        }
        input.rewind()
        val started = SystemClock.elapsedRealtime()
        val logits = OnnxTensor.createTensor(environment, input, SHAPE).use { tensor ->
            session.run(mapOf("pixels" to tensor)).use { result ->
                FloatArray(10).also { (result[0] as OnnxTensor).floatBuffer.get(it) }
            }
        }
        check(logits.all { it.isFinite() })
        val maximum = logits.max()
        val probabilities = logits.map { exp((it - maximum).toDouble()).toFloat() }
        val total = probabilities.sum()
        val ordered = probabilities.indices.sortedByDescending { probabilities[it] }
        return MaterialPrediction(ordered[0], probabilities[ordered[0]] / total,
            (probabilities[ordered[0]] - probabilities[ordered[1]]) / total,
            SystemClock.elapsedRealtime() - started)
    }

    override fun close() = session.close()

    companion object {
        const val SIZE = 224
        const val ASSET = "material_classifier.onnx"
        const val SHA256 = "fec39a575bd1a2d2434a1f763989bba0c2ecdd3c2cbfd52a27d9f923f53abd10"
        private val SHAPE = longArrayOf(1, 3, 224, 224)
        val materials = listOf(
            Material(0, "Battery", null), Material(1, "Biological", BinType.WET),
            Material(2, "Cardboard", BinType.DRY), Material(3, "Clothes", null),
            Material(4, "Glass", BinType.DRY), Material(5, "Metal", BinType.DRY),
            Material(6, "Paper", BinType.DRY), Material(7, "Plastic", BinType.DRY),
            Material(8, "Shoes", null), Material(9, "Trash", null)
        )
    }
}
