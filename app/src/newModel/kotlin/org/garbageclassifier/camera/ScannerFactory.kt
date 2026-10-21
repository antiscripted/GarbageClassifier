package org.garbageclassifier.camera

import android.content.Context

object ScannerFactory {
    fun create(context: Context): FrameProcessor = CenterScanner(context)
}
