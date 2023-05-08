package com.shafi.basic_image_picker.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.esafirm.imagepicker.features.ImagePicker
import com.shafi.basic_image_picker.R
import com.shafi.basic_image_picker.model.BasicImageData
import com.shafi.basic_image_picker.model.ImageUtilConfig
import com.shafi.basic_image_picker.util.BasicProgressDialog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Shafi on 07/05/2023.
 */
class MultiImageUtilActivity : AppCompatActivity() {

    private lateinit var config: ImageUtilConfig

    private var selectedImages: MutableList<BasicImageData> = mutableListOf()

    //private var isImageCopied: AtomicBoolean = AtomicBoolean(false)
    //private var progressDialog: BasicProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config =
            intent.getSerializableExtra(ImageUtilConfig::class.java.simpleName) as ImageUtilConfig

        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
            createAndLaunchMultiPicker()
        } else {
            ImagePicker.create(this)
                .folderMode(true)
                .toolbarFolderTitle(getString(R.string.select_photo_album)) // folder selection title
                .toolbarImageTitle(getString(R.string.tap_to_select)) // image selection title
                .toolbarArrowColor(Color.WHITE) // Toolbar 'up' arrow color
                .includeVideo(false) // Show video on image picker
                .multi() // multi mode (default mode)
                .limit(config.maxImage)
                .showCamera(false) // show camera or not (true by default)
                .enableLog(false) // disabling log
                .start()
        }
    }

//    private fun startProgressBar(uriSize: Int) {
//
//        Thread(Runnable {
//
//            while (true) {
//
//                Handler(Looper.getMainLooper()).post {
//
//                    progressDialog = BasicProgressDialog(this)
//                    progressDialog?.show()
//                }
//
//                try {
//                    Thread.sleep(5000)
//                } catch (e: InterruptedException) {
//                    return@Runnable
//                }
//
//                if (isImageCopied.get()) {
//                    stopProgressBarAndSendResult(uriSize)
//                    return@Runnable
//                }
//            }
//        }).start()
//    }
//
//    private fun stopProgressBarAndSendResult(uriSize: Int) {
//        Handler(Looper.getMainLooper()).post {
//            progressDialog?.dismiss()
//            if (selectedImages.size == uriSize) {
//                sendResultOkAndFinish()
//            } else {
//                sendResultCanceledAndFinish(true)
//            }
//        }
//    }

    private fun createAndLaunchMultiPicker() {
        val multiImageLauncher =
            registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(config.maxImage)) { uris ->
                if (uris.isNullOrEmpty().not()) {

                    //startProgressBar(uris.size)
                    for (uri in uris) {
                        copyGalleryImageFileToInternalStorage(uri)
                    }

                    //isImageCopied.set(true)

                    if (selectedImages.size == uris.size) {
                        sendResultOkAndFinish()
                    } else {
                        sendResultCanceledAndFinish(true)
                    }
                }
            }

        multiImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun copyGalleryImageFileToInternalStorage(imageUri: Uri) {
        try {
            val imageName = "${UUID.randomUUID()}.jpg"
            val file = File(cacheDir, imageName)

            if (!file.exists()) {
                file.createNewFile().also { status ->
                    if (status) {
                        try {
                            FileOutputStream(file).use { outputStream ->
                                contentResolver.openInputStream(imageUri)?.use { input ->
                                    input.copyTo(outputStream)
                                }
                            }
                            val imagePath = file.absolutePath
                            selectedImages.add(
                                BasicImageData(
                                    imageName,
                                    imagePath,
                                    imageUri.toString()
                                )
                            )

                        } catch (ex: IOException) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {

            val images = ImagePicker.getImages(data)

            for (image in images) {
                selectedImages.add(BasicImageData(image.name, image.path, image.uri.toString()))
            }
            sendResultOkAndFinish()
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
}