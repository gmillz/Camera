package dev.gmillz.camera

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import java.io.IOException

@Throws(IOException::class)
fun removePendingFlagFromUri(contentResolver: ContentResolver, uri: Uri) {
    val cv = ContentValues()
    cv.put(MediaStore.MediaColumns.IS_PENDING, 0)
    if (contentResolver.update(uri, cv, null, null) != 1) {
        throw IOException("unable to remove IS_PENDING flag")
    }
}