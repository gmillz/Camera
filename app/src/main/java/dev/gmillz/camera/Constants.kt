package dev.gmillz.camera

import android.net.Uri
import android.provider.MediaStore

const val IMAGE_NAME_PREFIX = "IMG_"

val IMAGE_COLLECTION_URI: Uri = MediaStore.Images.Media.getContentUri(
    MediaStore.VOLUME_EXTERNAL_PRIMARY
)!!

const val SAF_URI_HOST_EXTERNAL_STORAGE = "com.android.externalstorage.documents"


const val DEFAULT_MEDIA_STORE_CAPTURE_PATH = "DCIM/Camera"