package dev.gmillz.camera

import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.UseCaseGroup
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import dev.gmillz.camera.capturer.CapturedItem
import java.util.concurrent.ExecutionException

class CamConfig(private val context: Context) {

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    var camera: Camera? = null
    var lastCapturedItem: MutableLiveData<CapturedItem?> = MutableLiveData()
    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    var imageCapture: ImageCapture? = null
    private var preview: Preview? = null

    private var lifecycleOwner: LifecycleOwner? = null
    private var surfaceProvider: SurfaceProvider? = null

    val sharedPrefs: SharedPreferences = context.getSharedPreferences("camera", 0)
    private val cameraManager = context.getSystemService(CameraManager::class.java)

    private lateinit var cameraSelector: CameraSelector

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

    init {
        loadLastCapturedItem()
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