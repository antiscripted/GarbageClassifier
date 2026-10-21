package org.garbageclassifier

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Debug
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.garbageclassifier.camera.TrashDetector
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.FloatBuffer
import java.security.MessageDigest

class CandidateRuntimeTest {
    @Test fun candidateRunsOnAndroidCpu() {
        val arguments = InstrumentationRegistry.getArguments()
        val name = arguments.getString("candidateModel")
        assumeTrue("Explicit candidate model is required", name != null)
        val size = arguments.getString("candidateSize")!!.toInt()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val model = File(context.filesDir, checkNotNull(name))
        val digest = MessageDigest.getInstance("SHA-256")
        model.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        val environment = OrtEnvironment.getEnvironment()
        val originalTimes = JSONArray()
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        try {
            TrashDetector(context).use { original ->
                repeat(4) { iteration ->
                    val before = SystemClock.elapsedRealtimeNanos()
                    original.detect(bitmap, 0)
                    if (iteration > 0) originalTimes.put((SystemClock.elapsedRealtimeNanos() - before) / 1_000_000.0)
                }
            }
        } finally {
            bitmap.recycle()
        }
        val memory = Debug.MemoryInfo()
        Debug.getMemoryInfo(memory)
        val baselinePss = memory.totalPss
        val start = SystemClock.elapsedRealtime()
        OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(2)
            options.setInterOpNumThreads(1)
            environment.createSession(model.absolutePath, options).use { session ->
                val loadMs = SystemClock.elapsedRealtime() - start
                assertTrue(session.inputNames == setOf("pixels"))
                val shape = longArrayOf(1, 3, size.toLong(), size.toLong())
                assertTrue((session.inputInfo["pixels"]!!.info as TensorInfo).shape.contentEquals(shape))
                OnnxTensor.createTensor(environment, FloatBuffer.wrap(FloatArray(3 * size * size)), shape).use { input ->
                    val times = JSONArray()
                    val outputs = JSONObject()
                    repeat(4) { iteration ->
                        val before = SystemClock.elapsedRealtimeNanos()
                        session.run(mapOf("pixels" to input)).use { result ->
                            val elapsed = (SystemClock.elapsedRealtimeNanos() - before) / 1_000_000.0
                            if (iteration > 0) times.put(elapsed)
                            if (iteration == 3) {
                                for ((outputName, value) in result) {
                                    val tensor = value as OnnxTensor
                                    val buffer = tensor.floatBuffer
                                    val values = JSONArray()
                                    while (buffer.hasRemaining()) {
                                        val number = buffer.get()
                                        assertTrue(number.isFinite())
                                        values.put(number.toDouble())
                                    }
                                    outputs.put(outputName, JSONObject()
                                        .put("shape", JSONArray(tensor.info.shape.toList()))
                                        .put("values", values))
                                }
                            }
                        }
                    }
                    Debug.getMemoryInfo(memory)
                    val report = JSONObject().put("model", name).put("sha256", hash)
                        .put("runtime", environment.version).put("load_ms", loadMs)
                        .put("original_detect_ms", originalTimes)
                        .put("inference_ms", times).put("outputs", outputs)
                        .put("baseline_pss_kib", baselinePss).put("loaded_pss_kib", memory.totalPss)
                        .put("native_heap_allocated_bytes", Debug.getNativeHeapAllocatedSize())
                        .put("threads", 2).put("physical_phone", false)
                    File(context.filesDir, "$name.android.json").writeText(report.toString(2))
                }
            }
        }
    }
}
