package dev.gmillz.camera.ui

import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gmillz.camera.CamConfig
import dev.gmillz.camera.CameraMode
import dev.gmillz.camera.capturer.ImageCapturer
import dev.gmillz.camera.capturer.VideoCapturer
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val imageCapturer: ImageCapturer,
    private val videoCapturer: VideoCapturer,
    private val camConfig: CamConfig
) : ViewModel() {

    private val _lastCapturedItemState = mutableStateOf(camConfig.lastCapturedItem.value)
    val lastCapturedItemState = _lastCapturedItemState

    private val _settingsOpen = mutableStateOf(false)
    val settingsOpen: State<Boolean> = _settingsOpen

    val flashMode: State<Int> = camConfig.flashModeState
    val maximizeQuality: State<Boolean> = camConfig.maximizeQualityState
    val currentMode: State<CameraMode> = camConfig.currentModeState
    val isRecording: State<Boolean> = videoCapturer.isRecording
    val elapsedTime: State<String> = videoCapturer.timer.elapsedTime

    init {
        camConfig.lastCapturedItem.observeForever {
            _lastCapturedItemState.value = it
        }
    }

    fun setCameraMode(mode: CameraMode) {
        camConfig.switchCameraMode(mode)
    }

    fun startFocusAndMetering(x: Float, y: Float, width: Float, height: Float) {
        val factory = SurfaceOrientedMeteringPointFactory(
            width, height
        )
        camConfig.camera?.cameraControl?.startFocusAndMetering(
            FocusMeteringAction.Builder(
                factory.createPoint(x, y)
            ).disableAutoCancel()
                .build()
        )
    }

    fun toggleSettingsOpen() {
        _settingsOpen.value = !settingsOpen.value
    }

    fun captureImage() {
        imageCapturer.takePicture()
    }

    fun toggleRecord() {
        if (camConfig.currentMode != CameraMode.VIDEO) {
            return
        }
        if (videoCapturer.isRecording.value) {
            videoCapturer.stopRecording()
        } else {
            videoCapturer.startRecording()
        }
    }

    fun toggleCamera() {
        camConfig.toggleCameraSelector()
    }

    fun toggleFlashMode() {
        camConfig.toggleFlashMode()
    }

    fun toggleMaximizeQuality(maximizeQuality: Boolean) {
        camConfig.toggleMaximizeQuality(maximizeQuality)
    }

    fun getAvailableCameraModes(): List<CameraMode> {
        return camConfig.availableModes()
    }

    fun initializeCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: SurfaceProvider) {
        camConfig.initializeCamera(lifecycleOwner, surfaceProvider)
    }
}