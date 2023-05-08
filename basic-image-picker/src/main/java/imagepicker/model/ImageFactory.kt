package imagepicker.model

import android.content.ContentUris
import android.net.Uri
import imagepicker.helper.ImagePickerUtils


object ImageFactory {
    @JvmStatic
    fun singleImage(uri: Uri, path: String): List<Image> {
        return listOf(
            Image(
            id = ContentUris.parseId(uri),
            name = ImagePickerUtils.getNameFromFilePath(path),
            path = path
        )
        )
    }
}