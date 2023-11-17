package dev.gmillz.camera.capturer

import android.content.Context
import android.util.Log
import dev.gmillz.camera.CamConfig

class ImageCapturer(private val context: Context, private val camConfig: CamConfig) {

    fun takePicture() {
        if (camConfig.camera == null) {
            Log.d("ImageCapturer", "camera is null")
            return
        }
        val imageCapture = camConfig.imageCapture

        val imageSaver = ImageSaver(
            imageCapturer = this,
            context = context
        )

        imageCapture.takePicture(ImageSaver.imageCaptureCallbackExecutor, imageSaver)
    }

    fun onCaptureSuccess() {
        // unfade

    }
}