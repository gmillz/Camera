package dev.gmillz.camera.capturer

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.compose.runtime.mutableStateOf
import dev.gmillz.camera.CamConfig
import dev.gmillz.camera.DEFAULT_MEDIA_STORE_CAPTURE_PATH
import dev.gmillz.camera.VIDEO_NAME_PREFIX
import dev.gmillz.camera.removePendingFlagFromUri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoCapturer(private val context: Context, private val camConfig: CamConfig) {

    val isRecording = mutableStateOf(false)

    private val videoFileFormat = ".mp4"

    private var recording: Recording? = null

    fun startRecording() {
        Log.d("TEST", "startRecording")
        if (camConfig.camera == null) {
            return
        }

        val recorder = camConfig.videoCapture?.output?: return

        val dateString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = VIDEO_NAME_PREFIX + dateString + videoFileFormat

        var includeAudio = false
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED) {
            includeAudio = true
        }

        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(videoFileFormat)?: "video/mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, DEFAULT_MEDIA_STORE_CAPTURE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
            ?: return

        var pendingRecording: PendingRecording? = null
        var fD: ParcelFileDescriptor? = null
        context.contentResolver.openFileDescriptor(uri, "w")?.let {
            fD = it
            val outputOptions = FileDescriptorOutputOptions.Builder(it)
                .setLocation(null)
                .build()
            pendingRecording = recorder.prepareRecording(context, outputOptions)
        }

        if (pendingRecording == null) {
            return
        }

        if (includeAudio) {
            pendingRecording?.withAudioEnabled()
        }

        //keepScreenOn
        isRecording.value = true
        // TODO add shutter sounds
        //startTimer()
        recording = pendingRecording?.start(context.mainExecutor) { event ->
            if (event is VideoRecordEvent.Finalize) {
                // stop keepScreenOn
                isRecording.value = false
                // TODO add shutter sounds

                if (event.hasError()) {
                    when (event.error) {
                        VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA -> {
                            // TODO show toast
                            return@start
                        }
                        VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED,
                            VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR,
                            VideoRecordEvent.Finalize.ERROR_UNKNOWN -> {

                                // TODO show message
                                return@start
                            }
                        else -> {
                            // TODO
                        }
                    }
                }

                try {
                    removePendingFlagFromUri(context.contentResolver, uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val item = CapturedItem(ITEM_TYPE_VIDEO, dateString, uri)
                camConfig.updateLastCapturedItem(item)

            }
        }

        try {
            fD?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        recording?.stop()
        recording?.close()
        recording = null
    }
}