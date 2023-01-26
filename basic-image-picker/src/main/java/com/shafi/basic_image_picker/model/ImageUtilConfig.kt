package com.shafi.basic_image_picker.model

import java.io.Serializable

data class ImageUtilConfig(
    var isCamera: Boolean = false,
    var isGallery: Boolean = false,
    var isOnlyVideo: Boolean = false,
    var videoSizeLimit: Int? = null,
    var saveIntoGallery: Boolean = false,
    var galleryDirectory: String = ""
): Serializable
