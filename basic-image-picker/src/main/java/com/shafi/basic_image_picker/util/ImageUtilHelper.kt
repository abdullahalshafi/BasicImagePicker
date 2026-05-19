package com.shafi.basic_image_picker.util


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.TypedValue
import androidx.activity.result.ActivityResultLauncher
import androidx.core.graphics.ColorUtils
import com.shafi.basic_image_picker.activity.ImageUtilActivity
import com.shafi.basic_image_picker.activity.MultiImageUtilActivity
import com.shafi.basic_image_picker.model.ImageUtilConfig

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

        applyCallerThemeColors()

        val intent = if (config.isMulti) {
            Intent(context, MultiImageUtilActivity::class.java)
        } else {
            Intent(context, ImageUtilActivity::class.java)
        }
        intent.putExtra(ImageUtilConfig::class.simpleName, config)
        intent.putExtra(PACKAGE_NAME, context.packageName)
        intentLauncher.launch(intent)
    }

    /**
     * Read colorPrimary from the calling activity's theme and derive the rest. We deliberately
     * ignore the theme's colorPrimaryDark / colorOnPrimary because parent themes
     * (Theme.AppCompat.*, Theme.MaterialComponents.*) define defaults for them, and Android's
     * theme APIs offer no way to tell "explicitly set by the app" from "inherited default."
     * Trusting those attrs caused the picker to show the inherited #303030 grey on the status
     * bar whenever the host app overrode only colorPrimary.
     */
    private fun applyCallerThemeColors() {
        val primary = context.resolveAttrColor(androidx.appcompat.R.attr.colorPrimary)
            ?: return
        config.toolbarColor = primary
        config.statusBarColor = darken(primary)
        config.toolbarOnColor = contrastingColor(primary)
    }

    private fun Context.resolveAttrColor(attr: Int): Int? {
        val tv = TypedValue()
        return if (theme.resolveAttribute(attr, tv, true) && tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            tv.data
        } else {
            null
        }
    }

    private fun darken(color: Int, factor: Float = 0.8f): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] = (hsl[2] * factor).coerceIn(0f, 1f)
        return ColorUtils.HSLToColor(hsl)
    }

    private fun contrastingColor(background: Int): Int {
        return if (ColorUtils.calculateLuminance(background) < 0.5) Color.WHITE else Color.BLACK
    }
}