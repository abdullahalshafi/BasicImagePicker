package com.shafi.basic_image_picker.model

import java.io.Serializable

data class Image(
    val name: String,
    val path: String,
    val uri: String
): Serializable
