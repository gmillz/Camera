package dev.gmillz.camera.ui

import androidx.camera.core.Preview.SurfaceProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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

    private val _lastCapturedItemState = mutableStateOf(camConfig.lastCapturedItem.value)
    val lastCapturedItemState = _lastCapturedItemState

    private val _settingsOpen = mutableStateOf(false)
    val settingsOpen: State<Boolean> = _settingsOpen

    val flashMode: State<Int> = camConfig.flashModeState

    init {
        camConfig.lastCapturedItem.observeForever {
            _lastCapturedItemState.value = it
        }
    }

    fun toggleSettingsOpen() {
        _settingsOpen.value = !settingsOpen.value
    }

    fun captureImage() {
        imageCapturer.takePicture()
    }

    fun toggleCamera() {
        camConfig.toggleCameraSelector()
    }

    fun toggleFlashMode() {
        camConfig.toggleFlashMode()
    }

    fun initializeCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: SurfaceProvider) {
        camConfig.initializeCamera(lifecycleOwner, surfaceProvider)
    }
}