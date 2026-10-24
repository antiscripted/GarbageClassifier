package org.garbageclassifier

import android.app.AlertDialog
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.CoordinateTransform
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import org.garbageclassifier.camera.AnalyzedFrame
import org.garbageclassifier.camera.CameraController
import org.garbageclassifier.camera.OverlayItem
import org.garbageclassifier.core.BinType
import org.garbageclassifier.core.Recognition
import org.garbageclassifier.core.SortingStatus
import org.garbageclassifier.core.WasteRules
import org.garbageclassifier.databinding.ActivityMainBinding
import org.json.JSONArray

@androidx.annotation.OptIn(TransformExperimental::class)
class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var camera: CameraController
    private val handler = Handler(Looper.getMainLooper())
    private var inspection = false
    private var selectedBin = BinType.WET
    private var lastResult = 0L
    private var primaryTrackId: Long? = null
    private var running = false
    private var licenseDialog: AlertDialog? = null
    private var selectedLicense = -1
    private data class LicenseEntry(val title: String, val files: List<String>)
    private val licenseEntries by lazy {
        val entries = JSONArray(assets.open("licenses/index.json").bufferedReader().use { it.readText() })
        List(entries.length()) { index ->
            val entry = entries.getJSONObject(index)
            val files = entry.getJSONArray("files")
            LicenseEntry(entry.getString("title"), List(files.length()) { files.getString(it) })
        }
    }
    private val permissionHistory by lazy { getPreferences(MODE_PRIVATE) }
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else showPermission()
    }
    private val expiry = object : Runnable {
        override fun run() {
            if (running && lastResult != 0L && SystemClock.elapsedRealtime() - lastResult > 900) {
                camera.resetTracking()
                clearResults(R.string.unclear)
            }
            if (running) handler.postDelayed(this, 250)
        }
    }
    private val resize = Runnable {
        if (running) {
            camera.stop()
            startCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val padding = (20 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(padding + bars.left, bars.top, padding + bars.right, padding + bars.bottom)
            insets
        }
        binding.cameraContainer.clipToOutline = true
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightNavigationBars = true
        binding.preview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        binding.preview.scaleType = PreviewView.ScaleType.FILL_CENTER
        binding.preview.keepScreenOn = true
        camera = CameraController(this, this, binding.preview, ::display,
            { error -> showError(error, R.string.camera_unavailable) },
            { error -> showError(error, R.string.unclear) })
        binding.licensesButton.setOnClickListener { showLicenses() }
        inspection = savedInstanceState?.getBoolean("inspection") ?: false
        selectedBin = if (savedInstanceState?.getBoolean("dry") == true) BinType.DRY else BinType.WET
        binding.modeGroup.check(if (inspection) R.id.inspectionButton else R.id.scannerButton)
        binding.binGroup.check(if (selectedBin == BinType.DRY) R.id.dryButton else R.id.wetButton)
        binding.binGroup.isVisible = inspection
        binding.modeGroup.setOnCheckedChangeListener { _, checked ->
            inspection = checked == R.id.inspectionButton
            binding.binGroup.isVisible = inspection
            camera.resetTracking()
            clearResults(R.string.hold_object)
        }
        binding.binGroup.setOnCheckedChangeListener { _, checked ->
            selectedBin = if (checked == R.id.dryButton) BinType.DRY else BinType.WET
            camera.resetTracking()
            clearResults(R.string.hold_object)
        }
        if (savedInstanceState?.getBoolean("licenses") == true) {
            showLicenses(savedInstanceState.getInt("selectedLicense", -1))
        }
        binding.preview.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                if (running) {
                    camera.stop()
                    clearResults(R.string.hold_object)
                }
                handler.removeCallbacks(resize)
                handler.postDelayed(resize, 100)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.preview.post {
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@post
            if (hasCameraPermission()) startCamera()
            else if (!permissionHistory.getBoolean("requested", false)) requestCameraPermission()
            else showPermission()
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        permissionHistory.edit { putBoolean("requested", true) }
        permissionRequest.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) || !hasCameraPermission() ||
            licenseDialog?.isShowing == true) return
        clearResults(R.string.hold_object)
        binding.actionButton.isVisible = false
        running = true
        lastResult = SystemClock.elapsedRealtime()
        handler.removeCallbacks(expiry)
        handler.post(expiry)
        camera.start()
    }

    private fun showLicenses(selection: Int = -1) {
        if (licenseDialog?.isShowing == true) return
        running = false
        handler.removeCallbacks(expiry)
        camera.stop()
        clearResults(R.string.hold_object)
        val entry = licenseEntries.getOrNull(selection)
        selectedLicense = if (entry == null) -1 else selection
        val builder = AlertDialog.Builder(this)
            .setPositiveButton(R.string.close, null)
        if (entry == null) {
            builder.setTitle(R.string.licenses)
                .setItems(licenseEntries.map { it.title }.toTypedArray()) { dialog, index ->
                    (dialog as AlertDialog).setOnDismissListener(null)
                    dialog.dismiss()
                    licenseDialog = null
                    showLicenses(index)
                }
        } else {
            val padding = (20 * resources.displayMetrics.density).toInt()
            val text = TextView(this).apply {
                textSize = 14f
                setTextIsSelectable(true)
                setPadding(padding, padding, padding, padding)
                text = entry.files.joinToString("\n\n") { name ->
                    assets.open("licenses/$name").bufferedReader().use { it.readText() }
                }
            }
            builder.setTitle(entry.title).setView(ScrollView(this).apply { addView(text) })
        }
        licenseDialog = builder.create().also { dialog ->
                dialog.setOnDismissListener {
                    licenseDialog = null
                    if (!isFinishing && !isDestroyed) {
                        if (entry == null) startCamera() else showLicenses()
                    }
                }
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
            }
    }

    private fun showPermission() {
        clearResults(R.string.permission_required)
        val canAsk = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
            !permissionHistory.getBoolean("requested", false)
        binding.actionButton.setText(if (canAsk) R.string.allow_camera else R.string.open_settings)
        binding.actionButton.isVisible = true
        binding.actionButton.setOnClickListener {
            if (canAsk) requestCameraPermission()
            else startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)))
        }
    }

    private fun showError(error: Exception, message: Int) {
        Log.e("GarbageClassifier", "Scanner stopped", error)
        running = false
        camera.stop()
        handler.removeCallbacks(expiry)
        clearResults(message)
        binding.actionButton.setText(R.string.try_again)
        binding.actionButton.isVisible = true
        binding.actionButton.setOnClickListener { startCamera() }
    }

    private fun display(frame: AnalyzedFrame) {
        if (!running || SystemClock.elapsedRealtime() - frame.capturedAtMs > 900) return
        val target = binding.preview.outputTransform ?: return
        val transform = CoordinateTransform(frame.transform, target)
        val visible = frame.recognitions.mapNotNull { recognition ->
            if (!recognition.visible) return@mapNotNull null
            val box = recognition.detection.box
            val bounds = RectF(box.left, box.top, box.right, box.bottom)
            transform.mapRect(bounds)
            if (!bounds.intersect(0f, 0f, binding.overlay.width.toFloat(), binding.overlay.height.toFloat()) ||
                bounds.width() < 2f || bounds.height() < 2f) null else recognition to bounds
        }
        lastResult = frame.capturedAtMs
        val overlays = visible.map { (tracked, bounds) ->
            val recognition = tracked.forOverlay()
            val status = statusOf(recognition)
            val label = if (recognition.bin == null) getString(R.string.unclear) else buildString {
                append(recognition.material?.label)
                append(" · ")
                append(binName(checkNotNull(recognition.bin)))
                if (inspection) {
                    append(" · ")
                    append(getString(if (status == SortingStatus.WRONG) R.string.wrong_bin else R.string.correct_bin))
                }
            }
            OverlayItem(bounds, label, status)
        }
        binding.overlay.show(if (frame.localized) overlays else emptyList())
        val candidates = visible.map { it.first }.ifEmpty {
            frame.recognitions.filter { !it.visible && it.bin != null }
        }
        val sorted = candidates.sortedWith(
            compareBy<Recognition> {
                if (inspection) when (statusOf(it)) {
                    SortingStatus.WRONG -> 0
                    SortingStatus.UNCERTAIN -> 1
                    SortingStatus.CORRECT -> 2
                } else if (it.bin != null) 0 else 1
            }.thenByDescending { it.detection.confidence }
        )
        val retained = candidates.firstOrNull { it.trackId == primaryTrackId && it.bin != null }
        val primary = if (retained != null && (!inspection || statusOf(retained) == SortingStatus.WRONG ||
                sorted.none { statusOf(it) == SortingStatus.WRONG })) retained else sorted.firstOrNull()
        primaryTrackId = primary?.takeIf { it.bin != null }?.trackId
        if (primary?.bin == null) {
            setResult(getString(R.string.unclear), null, null)
        } else {
            val bin = binName(checkNotNull(primary.bin))
            val status = if (!inspection) R.string.classified
                else if (statusOf(primary) == SortingStatus.WRONG) R.string.wrong_bin else R.string.correct_bin
            setResult(getString(status), "${primary.material?.label} · $bin", getString(R.string.put_in_bin, bin))
        }
        binding.resultCard.contentDescription = overlays.joinToString("\n") { it.label }
            .ifEmpty { listOf(binding.status.text, binding.material.text, binding.recommendation.text)
                .filter { it.isNotEmpty() }.joinToString("\n") }
    }

    private fun statusOf(recognition: Recognition): SortingStatus = when {
        recognition.bin == null -> SortingStatus.UNCERTAIN
        inspection -> WasteRules.inspect(recognition, selectedBin)
        else -> SortingStatus.CORRECT
    }

    private fun binName(bin: BinType) = getString(if (bin == BinType.WET) R.string.wet else R.string.dry)

    private fun clearResults(message: Int) {
        lastResult = 0
        primaryTrackId = null
        binding.overlay.show(emptyList())
        setResult(getString(message), null, null)
        binding.resultCard.contentDescription = getString(message)
    }

    private fun setResult(status: String, material: String?, recommendation: String?) {
        if (binding.status.text.toString() != status) binding.status.text = status
        binding.material.visibility = if (material != null) View.VISIBLE else View.INVISIBLE
        binding.recommendation.visibility = if (recommendation != null) View.VISIBLE else View.INVISIBLE
        if (binding.material.text.toString() != material.orEmpty()) binding.material.text = material
        if (binding.recommendation.text.toString() != recommendation.orEmpty()) binding.recommendation.text = recommendation
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("licenses", licenseDialog?.isShowing == true)
        outState.putInt("selectedLicense", selectedLicense)
        outState.putBoolean("inspection", inspection)
        outState.putBoolean("dry", selectedBin == BinType.DRY)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        running = false
        handler.removeCallbacks(expiry)
        handler.removeCallbacks(resize)
        camera.stop()
        clearResults(R.string.hold_object)
        super.onStop()
    }

    override fun onDestroy() {
        licenseDialog?.setOnDismissListener(null)
        licenseDialog?.dismiss()
        licenseDialog = null
        camera.close()
        super.onDestroy()
    }
}
