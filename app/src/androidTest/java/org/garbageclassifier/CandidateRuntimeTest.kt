package org.garbageclassifier

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.os.Debug
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
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
        val runs = arguments.getString("candidateRuns", "4").toInt()
        require(runs in 1..10)
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
        val report = JSONObject().put("model", name).put("sha256", hash)
        val progress = File(context.filesDir, "$name.progress.json")
        val startedAt = SystemClock.elapsedRealtime()
        fun stage(name: String) {
            report.put("stage", name).put("elapsed_ms", SystemClock.elapsedRealtime() - startedAt)
            progress.writeText(report.toString(2))
        }
        stage("loading_runtime")
        val environment = OrtEnvironment.getEnvironment()
        stage("runtime_loaded")
        val memory = Debug.MemoryInfo()
        Debug.getMemoryInfo(memory)
        val baselinePss = memory.totalPss
        val start = SystemClock.elapsedRealtime()
        OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(arguments.getString("candidateThreads", "2").toInt())
            options.setInterOpNumThreads(1)
            options.addConfigEntry("session.intra_op.allow_spinning", "0")
            options.addConfigEntry("session.inter_op.allow_spinning", "0")
            stage("creating_session")
            environment.createSession(model.absolutePath, options).use { session ->
                val loadMs = SystemClock.elapsedRealtime() - start
                report.put("load_ms", loadMs)
                stage("session_ready")
                assertTrue(session.inputNames == setOf("pixels"))
                val shape = longArrayOf(1, 3, size.toLong(), size.toLong())
                assertTrue((session.inputInfo["pixels"]!!.info as TensorInfo).shape.contentEquals(shape))
                OnnxTensor.createTensor(environment, FloatBuffer.wrap(FloatArray(3 * size * size)), shape).use { input ->
                    val times = JSONArray()
                    val outputs = JSONObject()
                    repeat(runs) { iteration ->
                        stage("inference_${iteration + 1}_started")
                        val before = SystemClock.elapsedRealtimeNanos()
                        session.run(mapOf("pixels" to input)).use { result ->
                            val elapsed = (SystemClock.elapsedRealtimeNanos() - before) / 1_000_000.0
                            if (iteration > 0 || runs == 1) times.put(elapsed)
                            if (iteration == runs - 1) {
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
                    report.put("model", name).put("sha256", hash)
                        .put("runtime", environment.version).put("load_ms", loadMs)
                        .put("warmup_calls", if (runs > 1) 1 else 0)
                        .put("inference_ms", times).put("outputs", outputs)
                        .put("baseline_pss_kib", baselinePss).put("loaded_pss_kib", memory.totalPss)
                        .put("native_heap_allocated_bytes", Debug.getNativeHeapAllocatedSize())
                        .put("threads", arguments.getString("candidateThreads", "2").toInt()).put("physical_phone", false)
                    stage("complete")
                    File(context.filesDir, "$name.android.json").writeText(report.toString(2))
                }
            }
        }
    }
}
