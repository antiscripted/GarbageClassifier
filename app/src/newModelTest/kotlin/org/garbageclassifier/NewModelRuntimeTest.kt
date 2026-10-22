package org.garbageclassifier

import android.os.Debug
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.garbageclassifier.camera.MaterialClassifier
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class NewModelRuntimeTest {
    @Test fun bundledProductionClassifierMatchesReference() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val started = SystemClock.elapsedRealtime()
        val report = JSONObject().put("model_sha256", MaterialClassifier.SHA256)
        val progress = File(context.filesDir, "new-model-production.json")
        fun stage(value: String) {
            report.put("stage", value).put("elapsed_ms", SystemClock.elapsedRealtime() - started)
            progress.writeText(report.toString(2))
        }
        stage("loading_bundled_model")
        MaterialClassifier(context).use { classifier ->
            stage("production_session_ready")
            val prediction = classifier.classify(IntArray(224 * 224) { 0xff808080.toInt() }, 224, 0)
            report.put("class_id", prediction.classId).put("confidence", prediction.confidence.toDouble())
                .put("margin", prediction.margin.toDouble()).put("inference_ms", prediction.inferenceMs)
            stage("inference_returned")
            assertEquals(0, prediction.classId)
            assertEquals(0.445864737f, prediction.confidence, 0.02f)
            assertEquals(0.155505836f, prediction.margin, 0.04f)
            val memory = Debug.MemoryInfo()
            Debug.getMemoryInfo(memory)
            report.put("pss_kib", memory.totalPss)
            stage("passed")
        }
    }
}
