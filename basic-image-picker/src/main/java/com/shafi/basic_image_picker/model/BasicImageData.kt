package com.shafi.basic_image_picker.model


import java.io.Serializable

data class BasicImageData(
    val name: String,
    val path: String,
    val uri: String,
    val galleryUri: String?
): Serializable

