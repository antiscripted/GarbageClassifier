package org.garbageclassifier.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import org.garbageclassifier.core.Detection
import org.garbageclassifier.core.FrameRotation
import org.garbageclassifier.core.Letterbox
import org.garbageclassifier.core.ModelContract
import org.garbageclassifier.core.Point
import org.garbageclassifier.core.YoloDecoder
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

class TrashDetector(context: Context) : Closeable {
    private val interpreter: Interpreter
    private val decoder = YoloDecoder()
    private val size = ModelContract.INPUT_SIZE
    private val bitmap = createBitmap(size, size)
    private val canvas = Canvas(bitmap)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val pixels = IntArray(size * size)
    private val input = ByteBuffer.allocateDirect(size * size * 3 * 4).order(ByteOrder.nativeOrder())
    private val output = ByteBuffer.allocateDirect(ModelContract.CHANNELS * ModelContract.CANDIDATES * 4)
        .order(ByteOrder.nativeOrder())
    private val values = FloatArray(ModelContract.CHANNELS * ModelContract.CANDIDATES)

    init {
        val bytes = context.assets.open(ModelContract.ASSET).use { it.readBytes() }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        check(hash == ModelContract.SHA256) { "Unexpected model checksum" }
        val model = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        model.put(bytes).rewind()
        interpreter = Interpreter(model, Interpreter.Options().setNumThreads(2).setUseXNNPACK(true))
        try {
            check(interpreter.inputTensorCount == 1 && interpreter.outputTensorCount == 1)
            check(interpreter.getInputTensor(0).shape().contentEquals(intArrayOf(1, size, size, 3)))
            check(interpreter.getOutputTensor(0).shape().contentEquals(
                intArrayOf(1, ModelContract.CHANNELS, ModelContract.CANDIDATES)))
            check(interpreter.getInputTensor(0).dataType() == DataType.FLOAT32)
            check(interpreter.getOutputTensor(0).dataType() == DataType.FLOAT32)
        } catch (error: Exception) {
            interpreter.close()
            throw error
        }
    }

    fun detect(source: Bitmap, rotationDegrees: Int): List<Detection> {
        val rotated = rotationDegrees == 90 || rotationDegrees == 270
        val width = if (rotated) source.height else source.width
        val height = if (rotated) source.width else source.height
        val letterbox = Letterbox(width, height, size)
        val origin = FrameRotation.upright(Point(0f, 0f), source.width, source.height, rotationDegrees)
        val horizontal = FrameRotation.upright(
            Point(source.width.toFloat(), 0f), source.width, source.height, rotationDegrees)
        val vertical = FrameRotation.upright(
            Point(0f, source.height.toFloat()), source.width, source.height, rotationDegrees)
        val target = floatArrayOf(origin.x, origin.y, horizontal.x, horizontal.y, vertical.x, vertical.y)
        for (index in target.indices step 2) {
            target[index] = target[index] * letterbox.scale + letterbox.padX
            target[index + 1] = target[index + 1] * letterbox.scale + letterbox.padY
        }
        val matrix = Matrix()
        check(matrix.setPolyToPoly(floatArrayOf(0f, 0f, source.width.toFloat(), 0f,
            0f, source.height.toFloat()), 0, target, 0, 3))
        canvas.drawColor(Color.rgb(114, 114, 114))
        canvas.drawBitmap(source, matrix, paint)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        input.rewind()
        for (pixel in pixels) {
            input.putFloat(((pixel shr 16) and 255) / 255f)
            input.putFloat(((pixel shr 8) and 255) / 255f)
            input.putFloat((pixel and 255) / 255f)
        }
        input.rewind()
        output.rewind()
        interpreter.run(input, output)
        output.rewind()
        output.asFloatBuffer().get(values)
        return decoder.decode(values, letterbox).map {
            it.copy(box = FrameRotation.toBuffer(it.box, source.width, source.height, rotationDegrees))
        }
    }

    override fun close() {
        interpreter.close()
        bitmap.recycle()
    }
}
