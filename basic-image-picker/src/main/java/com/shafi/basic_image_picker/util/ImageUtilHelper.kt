package com.shafi.basic_image_picker.util


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.TypedValue
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.shafi.basic_image_picker.R
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

        // Mirror of the sentinel in basic-image-picker/src/main/res/values/colors.xml.
        // ContextCompat.getColor returns this when the consumer hasn't shadowed the resource.
        private const val SENTINEL_NO_OVERRIDE = 0x01000001

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
     * Precedence per color: consumer's colors.xml override > calling activity's theme attr >
     * bundled ef_AppTheme default (applied only if all fields remain null).
     *
     * Override detection works because library defaults are a sentinel value (#01000001); if
     * ContextCompat.getColor returns anything else, the consumer shadowed the resource in their
     * own colors.xml. Android theme APIs offer no direct "was this overridden?" check.
     *
     * For statusBarColor and toolbarOnColor we still ignore the corresponding theme attrs
     * (colorPrimaryDark / colorOnPrimary) and derive them, because parent themes always supply
     * misleading inherited defaults for those two.
     */
    private fun applyCallerThemeColors() {
        val resPrimary = getResColorOverride(R.color.basic_image_picker_colorPrimary)
        val resPrimaryDark = getResColorOverride(R.color.basic_image_picker_colorPrimaryDark)
        val resTextPrimary = getResColorOverride(R.color.basic_image_picker_colorTextPrimary)
        val resArrowColor = getResColorOverride(R.color.basic_image_picker_toolbar_icon_color)

        val themePrimary = context.resolveAttrColor(androidx.appcompat.R.attr.colorPrimary)
        val primary = resPrimary ?: themePrimary ?: return

        config.toolbarColor = primary
        config.statusBarColor = resPrimaryDark ?: darken(primary)
        config.toolbarOnColor = resTextPrimary ?: contrastingColor(primary)
        config.arrowColor = resArrowColor
    }

    private fun getResColorOverride(@ColorRes id: Int): Int? {
        val c = ContextCompat.getColor(context, id)
        return if (c == SENTINEL_NO_OVERRIDE) null else c
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