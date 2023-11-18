package dev.gmillz.camera

import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import dev.gmillz.camera.capturer.CapturedItem
import java.util.concurrent.ExecutionException

enum class CameraMode(val extensionMode: Int, val title: Int) {
    CAMERA(ExtensionMode.NONE, R.string.camera),
    VIDEO(ExtensionMode.NONE, R.string.video),
}

class CamConfig(private val context: Context) {

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    var camera: Camera? = null
    var lastCapturedItem: MutableLiveData<CapturedItem?> = MutableLiveData()
    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    var imageCapture: ImageCapture? = null
    var videoCapture: VideoCapture<Recorder>? = null
    private var preview: Preview? = null

    var videoQuality: Quality = SettingValues.Default.VIDEO_QUALITY

    private var lifecycleOwner: LifecycleOwner? = null
    private var surfaceProvider: SurfaceProvider? = null

    val sharedPrefs: SharedPreferences = context.getSharedPreferences("camera", 0)
    private val cameraManager = context.getSystemService(CameraManager::class.java)

    private lateinit var cameraSelector: CameraSelector

    private var _currentMode: CameraMode = CameraMode.CAMERA
    val currentModeState = mutableStateOf(_currentMode)
    var currentMode: CameraMode
        get() {
            return _currentMode
        }
        set(value) {
            currentModeState.value = value
            _currentMode = value
        }

    private val isFlashAvailable: Boolean
        get() {
            return camera?.cameraInfo?.hasFlashUnit()?: false
        }

    val flashModeState = mutableIntStateOf(flashMode)
    private var flashMode: Int
        get() {
            return if (imageCapture != null)
                imageCapture!!.flashMode
            else
                SettingValues.Default.FLASH_MODE
        }
        set(flashMode) {
            sharedPrefs.edit {
                putInt(SettingValues.Key.FLASH_MODE, flashMode)
            }
            imageCapture?.flashMode = flashMode
            flashModeState.intValue = flashMode
        }

    val maximizeQualityState = mutableStateOf(maximizeQuality)
    private var maximizeQuality: Boolean
        get() {
            return sharedPrefs.getBoolean(
                SettingValues.Key.MAXIMIZE_QUALITY,
                SettingValues.Default.MAXIMIZE_QUALITY
            )
        }
        set(value) {
            sharedPrefs.edit {
                putBoolean(SettingValues.Key.MAXIMIZE_QUALITY, value)
            }
            maximizeQualityState.value = value
        }

    var aspectRatio: Int
        get() {
            return when (currentMode) {
                CameraMode.VIDEO -> AspectRatio.RATIO_16_9
                else -> sharedPrefs.getInt(SettingValues.Key.ASPECT_RATIO,
                    SettingValues.Default.ASPECT_RATIO)
            }
        }
        set(value) {
            sharedPrefs.edit {
                putInt(SettingValues.Key.ASPECT_RATIO, value)
            }
            aspectRatioState.value = value
        }
    val aspectRatioState = mutableIntStateOf(aspectRatio)

    init {
        loadLastCapturedItem()
    }

    fun availableModes(): List<CameraMode> {
        return CameraMode.values().filter {
            when (it) {
                CameraMode.CAMERA, CameraMode.VIDEO -> true
                else -> {
                    check(it.extensionMode != ExtensionMode.NONE)
                    extensionsManager?.isExtensionAvailable(cameraSelector, it.extensionMode)?: false
                }
            }
        }
    }

    fun switchCameraMode(mode: CameraMode) {
        if (currentMode == mode) {
            return
        }
        currentMode = mode
        Log.d("TEST", "mode - ${currentMode.name}")
        if (lifecycleOwner != null && surfaceProvider != null) {
            startCamera(lifecycleOwner!!, surfaceProvider!!, true)
        }
    }

    private fun loadLastCapturedItem() {
        val type = sharedPrefs.getInt(SettingValues.Key.LAST_CAPTURED_ITEM_TYPE, -1)
        val dateStr = sharedPrefs.getString(SettingValues.Key.LAST_CAPTURED_ITEM_DATE_STRING, null)
        val uri = sharedPrefs.getString(SettingValues.Key.LAST_CAPTURED_ITEM_URI, null)

        var item: CapturedItem? = null
        if (dateStr != null && uri != null) {
            item = CapturedItem(type, dateStr, Uri.parse(uri))
        }
        lastCapturedItem.postValue(item)
    }

    fun updateLastCapturedItem(item: CapturedItem) {
        sharedPrefs.edit {
            putInt(SettingValues.Key.LAST_CAPTURED_ITEM_TYPE, item.type)
            putString(SettingValues.Key.LAST_CAPTURED_ITEM_DATE_STRING, item.dateString)
            putString(SettingValues.Key.LAST_CAPTURED_ITEM_URI, item.uri.toString())
        }
        lastCapturedItem.postValue(item)
    }

    fun toggleAspectRatio() {
        aspectRatio = if (aspectRatio == AspectRatio.RATIO_16_9) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

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

    fun toggleFlashMode() {
        if (isFlashAvailable) {
            flashMode = when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }
        }
    }

    fun toggleMaximizeQuality(maximizeQuality: Boolean) {
        this.maximizeQuality = maximizeQuality
        if (lifecycleOwner != null && surfaceProvider != null) {
            startCamera(lifecycleOwner!!, surfaceProvider!!, true)
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
        val aspectRatioStrategy = AspectRatioStrategy(
            aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO
        )

        if (currentMode == CameraMode.VIDEO) {
            videoCapture = VideoCapture.withOutput(
                Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(videoQuality))
                    .build()
            )
            useCaseGroupBuilder.addUseCase(videoCapture!!)
        }

        val previewBuilder = Preview.Builder()

        preview = previewBuilder.build().also {
            useCaseGroupBuilder.addUseCase(it)
            it.setSurfaceProvider(surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(
                if (maximizeQuality) {
                    ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                } else {
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                }
            )
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(aspectRatioStrategy)
                    .build()
            )
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