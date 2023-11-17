package dev.gmillz.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutionException

class CamConfig(private val context: Context) {
    val imageCapture: ImageCapture =
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()

    var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var preview: Preview? = null

    private lateinit var cameraSelector: CameraSelector

    fun initializeCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        if (cameraProvider != null) {
            startCamera(lifecycleOwner, surfaceProvider)
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(fun() {
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
                return
            }

            val extensionManagerFuture = ExtensionsManager.getInstanceAsync(context, cameraProvider!!)

            extensionManagerFuture.addListener({
                try {
                    extensionsManager = extensionManagerFuture.get()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                }
                startCamera(lifecycleOwner, surfaceProvider)
            }, ContextCompat.getMainExecutor(context))

        }, ContextCompat.getMainExecutor(context))
    }

    private fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        if (camera != null || cameraProvider == null) return

        cameraSelector = CameraSelector.Builder().apply {
            requireLensFacing(CameraSelector.LENS_FACING_BACK)
        }.build()

        val useCaseGroupBuilder = UseCaseGroup.Builder()

        useCaseGroupBuilder.addUseCase(imageCapture)

        val previewBuilder = Preview.Builder()

        preview = previewBuilder.build().also {
            useCaseGroupBuilder.addUseCase(it)
            it.setSurfaceProvider(surfaceProvider)
        }

        try {
            camera = cameraProvider!!.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroupBuilder.build())
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
}