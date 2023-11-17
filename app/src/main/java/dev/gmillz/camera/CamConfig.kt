package dev.gmillz.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.UseCaseGroup
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutionException

class CamConfig(private val context: Context) {

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    var imageCapture: ImageCapture? = null
    private var preview: Preview? = null

    private var lifecycleOwner: LifecycleOwner? = null
    private var surfaceProvider: SurfaceProvider? = null

    private val cameraManager = context.getSystemService(CameraManager::class.java)

    private lateinit var cameraSelector: CameraSelector

    fun toggleCameraSelector() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        if (isLensFacingSupported(lensFacing)) {
            if (lifecycleOwner != null && surfaceProvider != null) {
                startCamera(lifecycleOwner!!, surfaceProvider!!, true)
            }
        } else {
            // TODO display error
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
        }
    }

    private fun isLensFacingSupported(lensFacing: Int): Boolean {
        for (cameraId in cameraManager.cameraIdList) {
            if (cameraManager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.LENS_FACING) == lensFacing) {
                return true
            }
        }
        return false
    }

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

    private fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: SurfaceProvider, forced: Boolean = false) {
        if ((!forced && camera != null) || cameraProvider == null) return

        cameraProvider?.unbindAll()

        cameraSelector = CameraSelector.Builder().apply {
            requireLensFacing(lensFacing)
        }.build()

        val useCaseGroupBuilder = UseCaseGroup.Builder()

        val previewBuilder = Preview.Builder()

        preview = previewBuilder.build().also {
            useCaseGroupBuilder.addUseCase(it)
            it.setSurfaceProvider(surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build().also {
                useCaseGroupBuilder.addUseCase(it)
            }

        try {
            camera = cameraProvider!!.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroupBuilder.build())
            this.lifecycleOwner = lifecycleOwner
            this.surfaceProvider = surfaceProvider
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
}