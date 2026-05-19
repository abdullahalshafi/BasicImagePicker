package com.shafi.basic_image_picker.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.shafi.basic_image_picker.R
import com.shafi.basic_image_picker.model.BasicImageData
import com.shafi.basic_image_picker.model.ImageUtilConfig
import imagepicker.features.ImagePicker
import imagepicker.features.IpCons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID


/**
 * Created by Shafi on 07/05/2023.
 */
class MultiImageUtilActivity : AppCompatActivity() {

    private lateinit var config: ImageUtilConfig

    private var selectedImages: MutableList<BasicImageData> = mutableListOf()

    /** Caps concurrent copies to avoid OOM when the user picks many large images. */
    private val copySemaphore = Semaphore(MAX_PARALLEL_COPIES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = readConfig()

        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
            createAndLaunchMultiPicker()
        } else {
            val imagePicker = ImagePicker.create(this)
                .folderMode(true)
                .toolbarFolderTitle(getString(R.string.select_photo_album))
                .toolbarImageTitle(getString(R.string.tap_to_select))
                .includeVideo(false)
                .multi()

            if (config.maxImage != null) {
                imagePicker.limit(config.maxImage!!)
            }
            imagePicker.showCamera(false)
            imagePicker.enableLog(false)

            config.toolbarColor?.let { imagePicker.toolbarColor(it) }
            config.statusBarColor?.let { imagePicker.statusBarColor(it) }
            if (config.toolbarOnColor != null) {
                imagePicker.toolbarTextColor(config.toolbarOnColor!!)
            } else {
                imagePicker.toolbarArrowColor(
                    ContextCompat.getColor(this, R.color.basic_image_picker_toolbar_icon_color)
                )
            }

            imagePicker.start()
        }
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun readConfig(): ImageUtilConfig {
        val key = ImageUtilConfig::class.java.simpleName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(key, ImageUtilConfig::class.java)!!
        } else {
            intent.getSerializableExtra(key) as ImageUtilConfig
        }
    }

    private fun createAndLaunchMultiPicker() {

        if (config.maxImage != null && config.maxImage!! == 1) {
            singleImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            val imageRequest = if (config.maxImage != null) {
                ActivityResultContracts.PickMultipleVisualMedia(config.maxImage!!)
            } else {
                ActivityResultContracts.PickMultipleVisualMedia()
            }
            val multiImageLauncher =
                registerForActivityResult(imageRequest) { uris ->
                    if (uris.isNullOrEmpty().not()) {
                        copyAllAndReturn(uris)
                    } else {
                        sendResultCanceledAndFinish(false)
                    }
                }

            multiImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private val singleImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                copyAllAndReturn(listOf(uri))
            } else {
                sendResultCanceledAndFinish(false)
            }
        }

    /**
     * Copy every picked URI into cacheDir in parallel (off the main thread) and deliver the
     * result. Preserves the user's selection order. If any single copy fails the whole pick is
     * cancelled to match the prior behavior.
     */
    private fun copyAllAndReturn(uris: List<Uri>) {
        lifecycleScope.launch {
            val results: List<BasicImageData?> = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    async { copySemaphore.withPermit { copyUriToCache(uri) } }
                }.awaitAll()
            }

            if (results.any { it == null }) {
                sendResultCanceledAndFinish(true)
                return@launch
            }
            selectedImages.clear()
            selectedImages.addAll(results.filterNotNull())
            sendResultOkAndFinish()
        }
    }

    private fun copyUriToCache(imageUri: Uri): BasicImageData? {
        return try {
            val imageName = "${UUID.randomUUID()}.jpg"
            val file = File(cacheDir, imageName)
            FileOutputStream(file).use { outputStream ->
                val input = contentResolver.openInputStream(imageUri)
                    ?: return null
                input.use { it.copyTo(outputStream) }
            }
            BasicImageData(imageName, file.absolutePath, imageUri.toString(), null)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {

            val images = ImagePicker.getImages(data)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                copyAllAndReturn(images.map { it.uri })

            } else {

                for (image in images) {
                    selectedImages.add(BasicImageData(image.name, image.path, image.uri.toString(), null))
                }
                sendResultOkAndFinish()
            }
        } else if (requestCode == IpCons.RC_IMAGE_PICKER && resultCode != Activity.RESULT_OK) {
            sendResultCanceledAndFinish(false)
        }
    }

    //send result to activity for image
    private fun sendResultOkAndFinish() {
        if (selectedImages.isNotEmpty()) {

            val intent = Intent()
            setResult(
                Activity.RESULT_OK,
                intent.putExtra(
                    BasicImageData::class.java.simpleName,
                    selectedImages as java.io.Serializable
                )
            )
            finish()
        } else {
            sendResultCanceledAndFinish(true)
        }
    }

    //some error occurred
    private fun sendResultCanceledAndFinish(
        showToast: Boolean,
        message: String = getString(R.string.something_went_wrong_please_try_again)
    ) {
        if (showToast) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT)
                .show()
        }
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private companion object {
        const val MAX_PARALLEL_COPIES = 4
    }
}
