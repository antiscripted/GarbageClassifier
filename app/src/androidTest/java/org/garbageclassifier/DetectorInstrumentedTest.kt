package org.garbageclassifier

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Size
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.OutputTransform
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.garbageclassifier.camera.TrashDetector
import org.garbageclassifier.core.Box
import org.garbageclassifier.core.FrameRotation
import org.garbageclassifier.core.ModelContract
import org.garbageclassifier.core.Point
import org.garbageclassifier.core.RecognitionFilter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(TransformExperimental::class)
class DetectorInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun bundledModelDetectsThreeRealMaterialExamples() {
        TrashDetector(instrumentation.targetContext).use { detector ->
            for ((file, expected) in listOf("plastic1.jpg" to 7, "metal1.jpg" to 4, "paper1.jpg" to 9)) {
                val bitmap = load(file)
                try {
                    val first = detector.detect(bitmap, 0).first()
                    assertEquals(file, expected, first.classId)
                    assertTrue(file, first.confidence > 0.7f)
                    assertTrue(first.box.isValid)
                    assertTrue(first.box.left >= 0f && first.box.top >= 0f)
                    assertTrue(first.box.right <= bitmap.width && first.box.bottom <= bitmap.height)
                    if (expected == 4) {
                        assertEquals(0f, first.box.left, 10f)
                        assertEquals(308f, first.box.right, 15f)
                        assertEquals(284f, first.box.bottom, 15f)
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    @Test fun emptyImageDoesNotProduceClassification() {
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        try {
            TrashDetector(instrumentation.targetContext).use { detector ->
                assertTrue(detector.detect(bitmap, 0).isEmpty())
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun uncertainRealExampleDoesNotRecommendABin() {
        val bitmap = load("glass1.jpg")
        try {
            TrashDetector(instrumentation.targetContext).use { detector ->
                val detections = detector.detect(bitmap, 0)
                val filter = RecognitionFilter(ModelContract.materials)
                filter.update(detections, 0)
                assertTrue(filter.update(detections, 200).all { it.bin == null })
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun allSensorRotationsPreserveObjectAndBoxLocation() {
        val original = load("plastic1.jpg")
        try {
            TrashDetector(instrumentation.targetContext).use { detector ->
                val reference = detector.detect(original, 0).first { it.classId == 7 }
                for (degrees in listOf(90, 180, 270)) {
                    val rotation = Matrix().apply { postRotate(-degrees.toFloat()) }
                    val buffer = Bitmap.createBitmap(original, 0, 0, original.width, original.height, rotation, false)
                    try {
                        val found = detector.detect(buffer, degrees).first { it.classId == 7 }
                        val points = listOf(Point(found.box.left, found.box.top), Point(found.box.right, found.box.top),
                            Point(found.box.right, found.box.bottom), Point(found.box.left, found.box.bottom))
                            .map { FrameRotation.upright(it, buffer.width, buffer.height, degrees) }
                        val upright = Box(points.minOf { it.x }, points.minOf { it.y },
                            points.maxOf { it.x }, points.maxOf { it.y })
                        assertTrue("rotation $degrees", reference.box.intersectionOverUnion(upright) > 0.90f)
                    } finally {
                        buffer.recycle()
                    }
                }
            }
        } finally {
            original.recycle()
        }
    }

    @Test fun cameraXMappingAccountsForCenterCrop() {
        val normalized = RectF(-1f, -1f, 1f, 1f)
        val source = Matrix().apply { setRectToRect(normalized, RectF(176f, 0f, 464f, 480f), Matrix.ScaleToFit.FILL) }
        val target = Matrix().apply { setRectToRect(normalized, RectF(0f, 0f, 360f, 600f), Matrix.ScaleToFit.FILL) }
        val transform = CoordinateTransform(OutputTransform(source, Size(288, 480)),
            OutputTransform(target, Size(288, 480)))
        val bounds = RectF(176f, 0f, 464f, 480f)
        transform.mapRect(bounds)
        assertEquals(0f, bounds.left, 0.01f)
        assertEquals(0f, bounds.top, 0.01f)
        assertEquals(360f, bounds.right, 0.01f)
        assertEquals(600f, bounds.bottom, 0.01f)
    }

    @Test fun cameraXMappingAccountsForNinetyDegreePreviewRotation() {
        val source = Matrix().apply {
            setRectToRect(RectF(-1f, -1f, 1f, 1f), RectF(0f, 0f, 640f, 480f), Matrix.ScaleToFit.FILL)
        }
        val target = Matrix().apply {
            setPolyToPoly(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f), 0,
                floatArrayOf(360f, 0f, 360f, 480f, 0f, 0f), 0, 3)
        }
        val transform = CoordinateTransform(OutputTransform(source, Size(640, 480)),
            OutputTransform(target, Size(640, 480)))
        val bounds = RectF(100f, 80f, 300f, 280f)
        transform.mapRect(bounds)
        assertEquals(150f, bounds.left, 0.01f)
        assertEquals(75f, bounds.top, 0.01f)
        assertEquals(300f, bounds.right, 0.01f)
        assertEquals(225f, bounds.bottom, 0.01f)
    }

    private fun load(file: String): Bitmap = instrumentation.context.assets.open(file).use {
        checkNotNull(BitmapFactory.decodeStream(it))
    }
}
