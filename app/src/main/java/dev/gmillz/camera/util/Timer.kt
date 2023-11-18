package dev.gmillz.camera.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.TimeUnit

class Timer {

    var elapsedTime = mutableStateOf("")

    private var elapsed = 0

    private val handler = Handler(Looper.getMainLooper())

    private val runnable = Runnable {
        elapsed++
        elapsedTime.value = getString()
        start()
    }

    fun start() {
        handler.postDelayed(runnable, TimeUnit.SECONDS.toMillis(1))
    }

    fun stop() {
        handler.removeCallbacks(runnable)
        elapsed = 0
        elapsedTime.value = ""
    }

    private fun getString(): String {
        val secs = padTo2(elapsed % 60)
        val mins = padTo2(elapsed / 60 % 60)
        val hours = padTo2(elapsed / 3600)
        return if (hours == "00") {
            "$mins:$secs"
        } else {
            "$hours:$mins:$secs"
        }
    }

    private fun padTo2(time: Int): String {
        return String.format("%1$" + 2 + "s", time).replace(' ', '0')
    }
}
