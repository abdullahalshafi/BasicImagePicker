package com.shafi.basic_image_picker.util


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.result.ActivityResultLauncher
import com.shafi.basic_image_picker.activity.ImageUtilActivity
import com.shafi.basic_image_picker.activity.MultiImageUtilActivity
import com.shafi.basic_image_picker.model.ImageUtilConfig
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import java.io.File

class ImageUtilHelper(
    private var context: Context,
    private var intentLauncher: ActivityResultLauncher<Intent>
) {
    private var config: ImageUtilConfig = ImageUtilConfig()

    companion object {

        const val PACKAGE_NAME = "package_name"

        public fun create(
            context: Context,
            intentLauncher: ActivityResultLauncher<Intent>,
            imageUtil: ImageUtilHelper.() -> Unit
        ): ImageUtilHelper {
            return ImageUtilHelper(context, intentLauncher).apply(imageUtil)
        }

        suspend fun compressImage(
            context: Context,
            file: File,
            quality: Int = 80,
            width: Int = -1,
            height: Int = -1
        ): File {
            return Compressor.compress(context, file) {
                if (width != -1 && height != -1) {
                    resolution(width, height)
                }
                quality(quality)
                format(Bitmap.CompressFormat.JPEG)
            }
        }
    }

    public fun isCamera(isCamera: Boolean) {
        config.isCamera = isCamera
    }

    public fun isGallery(isGallery: Boolean) {
        config.isGallery = isGallery
    }

    public fun isOnlyVideo(isOnlyVideo: Boolean) {
        config.isOnlyVideo = isOnlyVideo
    }

    public fun setVideoSizeLimitInMB(sizeLimitInMb: Int) {
        config.videoSizeLimit = sizeLimitInMb
    }

    public fun saveIntoGallery(shouldSaveIntoGallery: Boolean) {
        config.saveIntoGallery = shouldSaveIntoGallery
    }

    public fun galleryDirectoryName(directoryName: String) {
        config.galleryDirectory = directoryName
    }

    public fun multi() {
        config.isMulti = true
    }

    public fun maxImage(maxLimit: Int) {
        config.maxImage = maxLimit
    }

    public fun start() {

        val intent = if (config.isMulti && config.maxImage > 0) {
            Intent(context, MultiImageUtilActivity::class.java)
        } else {
            Intent(context, ImageUtilActivity::class.java)
        }
        intent.putExtra(ImageUtilConfig::class.simpleName, config)
        intent.putExtra(PACKAGE_NAME, context.packageName)
        intentLauncher.launch(intent)
    }
}