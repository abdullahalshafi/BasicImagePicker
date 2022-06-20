package com.shafi.basic_image_picker.model

import java.io.Serializable

data class ImageUtilConfig(
    var isCamera: Boolean = false,
    var isGallery: Boolean = false,
    var saveIntoGallery: Boolean = false,
    var galleryDirectory: String = "",
    var savePickedImageToCache: Boolean = true
): Serializable
