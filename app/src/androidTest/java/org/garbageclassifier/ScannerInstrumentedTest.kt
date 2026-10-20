package org.garbageclassifier

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.widget.Button
import android.widget.RadioButton
import androidx.camera.view.PreviewView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ScannerInstrumentedTest {
    @Before fun allowCamera() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand("pm grant org.garbageclassifier android.permission.CAMERA")
        ).use { it.readBytes() }
    }

    @Test fun cameraStreamsAndBinSelectionSurvivesRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitPreview(scenario)
            scenario.onActivity { activity ->
                activity.findViewById<RadioButton>(R.id.inspectionButton).performClick()
                activity.findViewById<RadioButton>(R.id.dryButton).performClick()
            }
            awaitPreview(scenario)
            scenario.recreate()
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<RadioButton>(R.id.inspectionButton).isChecked)
                assertTrue(activity.findViewById<RadioButton>(R.id.dryButton).isChecked)
            }
            awaitPreview(scenario)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            awaitPreview(scenario)
        }
    }

    @Test fun licensesSurviveRecreationAndCloseResumesPreview() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitPreview(scenario)
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.licensesButton).performClick()
            }
            awaitPreview(scenario, PreviewView.StreamState.IDLE)
            scenario.recreate()
            awaitPreview(scenario, PreviewView.StreamState.IDLE)
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val deadline = SystemClock.elapsedRealtime() + 30000
            var closed = false
            while (!closed && SystemClock.elapsedRealtime() < deadline) {
                val root = automation.rootInActiveWindow
                val close = root?.findAccessibilityNodeInfosByText("Close")?.firstOrNull { it.className == "android.widget.Button" &&
                    it.text?.toString().equals("Close", ignoreCase = true) && it.isClickable }
                if (close != null) {
                    assertEquals("Close", close.text.toString())
                    closed = close.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                }
                if (!closed) SystemClock.sleep(100)
            }
            assertTrue("License dialog must survive recreation and close", closed)
            awaitPreview(scenario)
        }
    }

    private fun awaitPreview(
        scenario: ActivityScenario<MainActivity>,
        expected: PreviewView.StreamState = PreviewView.StreamState.STREAMING
    ) {
        val streaming = AtomicBoolean(false)
        val deadline = SystemClock.elapsedRealtime() + 30000
        while (!streaming.get() && SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity { activity ->
                streaming.set(activity.findViewById<PreviewView>(R.id.preview).previewStreamState.value ==
                    expected)
            }
            if (!streaming.get()) SystemClock.sleep(100)
        }
        assertTrue("Rear camera preview did not reach $expected", streaming.get())
    }
}
