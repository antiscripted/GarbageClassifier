package org.garbageclassifier

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.os.Build
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.garbageclassifier.camera.MaterialClassifier
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.abs

class Int4CompatibilityTest {
    @Test fun packedKernelAndProductionSessionWorkOffline() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        val expected = JSONObject(assets.open("int4/expected.json").bufferedReader().use { it.readText() })
        val pixels = expected.getJSONArray("input")
        val reference = expected.getJSONArray("expected")
        val input = FloatArray(pixels.length()) { pixels.getDouble(it).toFloat() }
        val report = JSONObject().put("model_sha256", MaterialClassifier.SHA256)
            .put("abis", JSONArray(Build.SUPPORTED_ABIS.toList())).put("full_model_inference", false)
        val output = File(instrumentation.targetContext.filesDir, "int4-compatibility.json")
        report.put("stage", "kernel_started")
        output.writeText(report.toString(2))
        val environment = OrtEnvironment.getEnvironment()
        OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(2)
            options.setInterOpNumThreads(1)
            options.addConfigEntry("session.intra_op.allow_spinning", "0")
            val model = assets.open("int4/kernel.onnx").use { it.readBytes() }
            environment.createSession(model, options).use { session ->
                OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), longArrayOf(1, 4, 32)).use { tensor ->
                    val started = SystemClock.elapsedRealtime()
                    session.run(mapOf("pixels" to tensor)).use { result ->
                        report.put("kernel_pair_ms", SystemClock.elapsedRealtime() - started)
                        for ((name, value) in result) {
                            val values = FloatArray(reference.length())
                            (value as OnnxTensor).floatBuffer.get(values)
                            val error = values.indices.maxOf { abs(values[it] - reference.getDouble(it)) }
                            assertTrue(error <= expected.getDouble("${name}_tolerance"))
                            report.put("${name}_maximum_absolute_error", error)
                        }
                    }
                }
            }
        }
        report.put("stage", "kernel_passed_loading_production_session")
        output.writeText(report.toString(2))
        val started = SystemClock.elapsedRealtime()
        MaterialClassifier(instrumentation.targetContext).use {
            report.put("production_session_ms", SystemClock.elapsedRealtime() - started)
        }
        report.put("stage", "passed")
        output.writeText(report.toString(2))
    }
}
