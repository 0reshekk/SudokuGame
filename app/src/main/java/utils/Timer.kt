package utils

import android.os.Handler
import android.os.Looper

class GameTimer(private val onTick: (Long) -> Unit) {
    private var startTime: Long = 0L
    private var accumulatedSeconds: Long = 0L
    private var isRunning: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            onTick(elapsedSeconds())
            if (isRunning) handler.postDelayed(this, 1000)
        }
    }

    fun start(resetSeconds: Long = 0L) {
        accumulatedSeconds = resetSeconds
        startTime = System.currentTimeMillis()
        isRunning = true
        handler.post(runnable)
    }

    fun pause() {
        if (!isRunning) return
        accumulatedSeconds = elapsedSeconds()
        isRunning = false
        handler.removeCallbacks(runnable)
    }

    fun stop() {
        accumulatedSeconds = elapsedSeconds()
        isRunning = false
        handler.removeCallbacks(runnable)
    }

    fun elapsedSeconds(): Long {
        val now = System.currentTimeMillis()
        return if (isRunning) {
            accumulatedSeconds + (now - startTime) / 1000
        } else {
            accumulatedSeconds
        }
    }

    fun isActive(): Boolean = isRunning
}