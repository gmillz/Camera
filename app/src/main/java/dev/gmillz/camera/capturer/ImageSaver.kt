package dev.gmillz.camera.capturer


import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.system.Os
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.internal.utils.ImageUtil
import dev.gmillz.camera.DEFAULT_MEDIA_STORE_CAPTURE_PATH
import dev.gmillz.camera.IMAGE_COLLECTION_URI
import dev.gmillz.camera.IMAGE_NAME_PREFIX
import dev.gmillz.camera.SAF_URI_HOST_EXTERNAL_STORAGE
import dev.gmillz.camera.removePendingFlagFromUri
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.jvm.Throws

class ImageSaver(
    private val imageCapturer: ImageCapturer,
    private val context: Context
): ImageCapture.OnImageCapturedCallback() {

    private val imageWriteExecutor = Executors.newSingleThreadExecutor()
    private val contentResolver = context.contentResolver

    override fun onCaptureSuccess(image: ImageProxy) {
        context.mainExecutor.execute(imageCapturer::onCaptureSuccess)

        val jpegBytes = try {
            extractJpegBytes(image)
        } catch (e: Exception) {
            // handle error
            return
        }

        imageWriteExecutor.execute { saveImage(jpegBytes) }
    }

    override fun onError(exception: ImageCaptureException) {
        Log.d("TEST", "onError", exception)
    }

    @SuppressLint("RestrictedApi")
    @Throws(ImageUtil.CodecFailedException::class)
    private fun extractJpegBytes(imageProxy: ImageProxy): ByteArray {
        imageProxy.use { image ->

            return when (val imageFormat = image.format) {
                ImageFormat.JPEG -> {
                    ImageUtil.jpegImageToJpegByteArray(image)
                }
                ImageFormat.YUV_420_888 -> {
                    ImageUtil.yuvImageToJpegByteArray(image,
                        null, 100, image.imageInfo.rotationDegrees)
                }
                else ->
                    throw IllegalStateException("unknown imageFormat $imageFormat")
            }
        }
    }

    private fun saveImage(jpegBytes: ByteArray) {
        try {
            val cropJpegByteArray = ImageUtil::class.java.getDeclaredMethod(
                "cropJpegByteArray",
                ByteArray::class.java, Rect::class.java, Int::class.javaPrimitiveType)
            cropJpegByteArray.isAccessible = true

            val processedJpegBytes  = processExif(jpegBytes)

            val uri = try {
                obtainOutputUri()
            } catch (e: Exception) {
                return
            }

            val shouldFsync = when (uri.host) {
                MediaStore.AUTHORITY,
                SAF_URI_HOST_EXTERNAL_STORAGE -> true
                else -> false
            }

            try {
                contentResolver.openAssetFileDescriptor(uri, "w")!!.use {
                    val fd = it.fileDescriptor
                    var off = 0
                    val len = processedJpegBytes.size
                    do {
                        off += Os.write(fd, processedJpegBytes, off, len - off)
                    } while (off != len)

                    if (shouldFsync) {
                        Os.fsync(fd)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            removePendingFlagFromUri(contentResolver, uri)

            //val capturedItem = CapturedItem(ITEM_TYPE_IMAGE, dateString(), uri)
            //context.mainExecutor.execute { imageCapturer.onImageSaverSuccess(capturedItem) }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @SuppressLint("RestrictedApi")
    private fun processExif(jpegBytes: ByteArray): ByteArray {
        // TODO process exif data
        return jpegBytes
    }

    private fun obtainOutputUri(): Uri {
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename())
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType())
            put(MediaStore.MediaColumns.RELATIVE_PATH, DEFAULT_MEDIA_STORE_CAPTURE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        return contentResolver.insert(IMAGE_COLLECTION_URI, cv)!!
    }

    private fun filename(): String = IMAGE_NAME_PREFIX + dateString() + ".jpg"

    private fun dateString() =
        SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())

    private fun mimeType() =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(".jpg")?: "image/*"

    companion object {
        val imageCaptureCallbackExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    }
}