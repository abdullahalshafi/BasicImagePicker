package com.shafi.basic_image_picker.model

import java.io.Serializable

data class ImageUtilConfig(
    var isCamera: Boolean = false,
    var isGallery: Boolean = false,
    var isOnlyVideo: Boolean = false,
    var videoSizeLimit: Int? = null,
    var saveIntoGallery: Boolean = false,
    var galleryDirectory: String = "",
    var isMulti: Boolean = false,
    var maxImage: Int? = null,
    // Auto-derived from the calling activity's theme so the esafirm picker chrome matches.
    var toolbarColor: Int? = null,
    var statusBarColor: Int? = null,
    var toolbarOnColor: Int? = null
): Serializable
