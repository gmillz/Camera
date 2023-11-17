package dev.gmillz.camera.ui

import androidx.camera.core.Preview.SurfaceProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gmillz.camera.CamConfig
import dev.gmillz.camera.capturer.ImageCapturer
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val imageCapturer: ImageCapturer,
    private val camConfig: CamConfig
) : ViewModel() {

    fun captureImage() {
        imageCapturer.takePicture()
    }

    fun initializeCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: SurfaceProvider) {
        camConfig.initializeCamera(lifecycleOwner, surfaceProvider)
    }
}