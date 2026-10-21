package org.garbageclassifier.camera

import android.content.Context
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object CenterModelRunner {
    private val worker = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean()
    private val ownership = Any()
    private var users = 0
    private var classifier: MaterialClassifier? = null
    val available: Boolean get() = !busy.get()

    fun retain() = synchronized(ownership) { users++ }

    fun release() {
        synchronized(ownership) { users-- }
        worker.execute {
            synchronized(ownership) {
                if (users == 0) {
                    classifier?.close()
                    classifier = null
                }
            }
        }
    }

    fun submit(context: Context, pixels: IntArray, side: Int, rotation: Int,
               completion: (Result<MaterialPrediction>) -> Unit): Boolean {
        if (!busy.compareAndSet(false, true)) return false
        worker.execute {
            try {
                val result = runCatching {
                    val engine = classifier ?: MaterialClassifier(context.applicationContext).also { classifier = it }
                    engine.classify(pixels, side, rotation)
                }
                completion(result)
            } finally {
                busy.set(false)
            }
        }
        return true
    }
}
