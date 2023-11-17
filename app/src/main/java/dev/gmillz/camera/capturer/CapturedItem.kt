package dev.gmillz.camera.capturer

import android.net.Uri

typealias ItemType = Int
const val ITEM_TYPE_IMAGE: ItemType = 0
const val ITEM_TYPE_VIDEO: ItemType = 1

data class CapturedItem(
    val type: ItemType,
    val dateString: String,
    val uri: Uri
)