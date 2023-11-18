package dev.gmillz.camera

import androidx.camera.core.ImageCapture
import androidx.camera.video.Quality


object SettingValues {
    object Key {
        const val LAST_CAPTURED_ITEM_TYPE = "last_captured_item_type"
        const val LAST_CAPTURED_ITEM_DATE_STRING = "last_captured_item_date_string"
        const val LAST_CAPTURED_ITEM_URI = "last_captured_item_uri"

        const val FLASH_MODE = "flash_mode"
        const val MAXIMIZE_QUALITY = "maximize_quality"
    }

    object Default {
        const val FLASH_MODE = ImageCapture.FLASH_MODE_OFF
        const val MAXIMIZE_QUALITY = true
        val VIDEO_QUALITY: Quality = Quality.HIGHEST
    }
}