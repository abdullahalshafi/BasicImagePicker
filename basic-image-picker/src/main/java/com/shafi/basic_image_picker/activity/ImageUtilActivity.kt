package com.shafi.basic_image_picker.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.shafi.basic_image_picker.model.BasicImageData
import com.shafi.basic_image_picker.model.ImageUtilConfig
import com.shafi.basic_image_picker.R
import com.shafi.basic_image_picker.util.ImageUtilHelper.Companion.PACKAGE_NAME
import imagepicker.features.ImagePicker
import imagepicker.features.IpCons
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class ImageUtilActivity : AppCompatActivity() {

    private lateinit var config: ImageUtilConfig

    private var imageName: String? = null
    private var imagePath: String? = null
    private var imageUri: Uri? = null
    private var galleryUri: Uri? = null

    private var videoName: String? = null
    private var videoPath: String? = null
    private var videoUri: Uri? = null

    private var callingPackageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        callingPackageName = intent.getStringExtra(PACKAGE_NAME)
        config =
            intent.getSerializableExtra(ImageUtilConfig::class.java.simpleName) as ImageUtilConfig

        if (config.isCamera) {

            if (config.saveIntoGallery) {
                if (checkWritePermission()) {
                    launchCamera()
                    return
                }
                if (!checkWritePermission()) {
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                return
            }
            launchCamera()

        } else if (config.isGallery) {
            if (config.isOnlyVideo) {
                galleryVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                return
            }
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                ImagePicker.create(this)
                    .folderMode(true)
                    .toolbarFolderTitle(getString(R.string.select_photo_album)) // folder selection title
                    .toolbarImageTitle(getString(R.string.tap_to_select)) // image selection title
                    .toolbarArrowColor(
                        ContextCompat.getColor(
                            this,
                            R.color.basic_image_picker_toolbar_icon_color
                        )
                    ) // Toolbar 'up' arrow color
                    .includeVideo(false) // Show video on image picker
                    .single()
                    .showCamera(false) // show camera or not (true by default)
                    .enableLog(false) // disabling log
                    .start()
            }
        } else {
            throw IllegalArgumentException(getString(R.string.you_must_specify_camera_or_gallery))
        }
    }

    //send result to activity for image
    private fun sendResultOkAndFinish() {
        if (imageName != null && imagePath != null && imageUri != null) {
            val basicImageData = BasicImageData(
                imageName!!,
                imagePath!!,
                imageUri.toString(),
                galleryUri?.toString()
            )
            val intent = Intent()
            setResult(
                Activity.RESULT_OK,
                intent.putExtra(BasicImageData::class.java.simpleName, basicImageData)
            )
            finish()
        } else {
            sendResultCanceledAndFinish(true)
        }
    }

    //send result to activity for video
    private fun sendVideoResultOkAndFinish() {
        if (videoName != null && videoPath != null && videoUri != null) {
            val basicImageData = BasicImageData(videoName!!, videoPath!!, videoUri.toString(), null)
            val intent = Intent()
            setResult(
                Activity.RESULT_OK,
                intent.putExtra(BasicImageData::class.java.simpleName, basicImageData)
            )
            finish()
        } else {
            sendResultCanceledAndFinish(false)
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

    //create empty image file and launch the camera
    private fun launchCamera() {
        if (!checkCameraPermission()) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        imageUri = createEmptyFileAndGetUri()
        if (imageUri != null) {
            cameraLauncher.launch(imageUri)
        } else {
            sendResultCanceledAndFinish(true)
        }
    }

    //camera intent result
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->

            if (success && imageUri != null) {
                if (config.saveIntoGallery) {
                    saveImageToGallery()
                }
                sendResultOkAndFinish()
            } else {
                sendResultCanceledAndFinish(false)
            }
        }

    //gallery intent result
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                imageUri = uri
                copyGalleryImageFileToInternalStorage()
            } else {
                sendResultCanceledAndFinish(false)
            }
        }

    //gallery intent result esa firm
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {

            val image = ImagePicker.getFirstImageOrNull(data)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                imageUri = image.uri
                copyGalleryImageFileToInternalStorage()

            } else {

                imageUri = image.uri
                imageName = image.name
                imagePath = image.path
                sendResultOkAndFinish()
            }

        } else if (requestCode == IpCons.RC_IMAGE_PICKER && resultCode != Activity.RESULT_OK) {
            sendResultCanceledAndFinish(false)
        }
    }

    //gallery intent result for video
    private val galleryVideoLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {

                val videoFileName = checkVideoFileSizeAndGetFileName(uri)

                //if size limit exceeded
                if (videoFileName?.isEmpty() == true) {
                    return@registerForActivityResult
                }

                //if something went wrong
                if (videoFileName == null) {
                    sendResultCanceledAndFinish(true)
                    return@registerForActivityResult
                }

                videoUri = uri
                videoName = videoFileName
                copyGalleryVideoFileToInternalStorage()
            } else {
                sendResultCanceledAndFinish(false)
            }
        }

    private fun checkWritePermission(): Boolean {
        return if (!config.saveIntoGallery) {
            true
        } else {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                true
            } else {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun checkCameraPermission(): Boolean {

        //check whether camera permission exists in manifest
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val permissions = packageInfo.requestedPermissions

        if (permissions.isNullOrEmpty() || !permissions.contains(Manifest.permission.CAMERA)) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { writePermissionGranted: Boolean? ->

            if (config.isCamera && config.saveIntoGallery) {
                if (writePermissionGranted == true) {
                    launchCamera()
                    return@registerForActivityResult
                }
                requestWriteStoragePermission()
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { cameraPermissionGranted: Boolean? ->
            if (cameraPermissionGranted == true) {
                launchCamera()
                return@registerForActivityResult
            }
            requestCameraPermission()
        }

    private fun requestWriteStoragePermission() {
        when {
            checkWritePermission() -> {
                launchCamera()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                showPermissionAlert(
                    getString(R.string.storage_permission),
                    getString(R.string.you_can_not_capture_image_unless_you_give_storage_permission),
                    getString(R.string.please_give_storage_permission)
                ) { storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
            }

            else -> {

                showPermissionAlert(
                    getString(R.string.storage_permission),
                    getString(R.string.you_can_not_capture_image_unless_you_give_storage_permission),
                    getString(R.string.please_give_storage_permission)
                ) {
                    finish()
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        callingPackageName, null
                    )
                    intent.data = uri
                    //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }

            }
        }
    }

    private fun requestCameraPermission() {
        when {
            checkCameraPermission() -> {
                launchCamera()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                showPermissionAlert(
                    getString(R.string.camera_permission),
                    getString(R.string.you_can_not_capture_image_unless_you_give_camera_permission),
                    getString(R.string.please_give_camera_permission)
                ) { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            }

            else -> {

                showPermissionAlert(
                    getString(R.string.camera_permission),
                    getString(R.string.you_can_not_capture_image_unless_you_give_camera_permission),
                    getString(R.string.please_give_camera_permission)
                ) {

                    finish()
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        callingPackageName, null
                    )
                    intent.data = uri
                    //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }

            }
        }
    }

    private fun showPermissionAlert(
        title: String,
        message: String,
        cancelMessage: String,
        function: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { dialogInterface, _ ->
                function.invoke()
                dialogInterface.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ ->
                dialogInterface.dismiss()
                sendResultCanceledAndFinish(true, cancelMessage)
            }
            .setCancelable(false)
            .show()
    }

    private fun saveImageToGallery() {

        val subDirectory = if (config.galleryDirectory.isNotEmpty()) {
            File.separator + config.galleryDirectory
        } else {
            ""
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            try {
                val collection =
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_DCIM + subDirectory
                    )
                }

                contentResolver.insert(collection, contentValues)?.also { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        contentResolver.openInputStream(imageUri!!)?.use { input ->
                            input.copyTo(outputStream)
                        }
                        galleryUri = uri
                    }
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        } else {
            try {
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        .toString() + subDirectory
                )
                if (!directory.exists()) {
                    directory.mkdir()
                }
                val file = File(directory, imageName!!)
                if (!file.exists()) {
                    file.createNewFile().also { status ->
                        if (status) {
                            FileOutputStream(file).use { outputStream ->
                                contentResolver.openInputStream(imageUri!!)
                                    ?.use { input -> input.copyTo(outputStream) }
                            }
                            galleryUri = file.toUri()
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun copyGalleryImageFileToInternalStorage() {

        try {
            imageName = "${UUID.randomUUID()}.jpg"
            val file = File(cacheDir, imageName!!)

            if (!file.exists()) {
                file.createNewFile().also { status ->
                    if (status) {
                        try {
                            FileOutputStream(file).use { outputStream ->
                                contentResolver.openInputStream(imageUri!!)?.use { input ->
                                    input.copyTo(outputStream)
                                }
                            }
                            imagePath = file.absolutePath
                            sendResultOkAndFinish()
                        } catch (ex: IOException) {
                            ex.printStackTrace()
                            sendResultCanceledAndFinish(true)
                        }
                    } else {
                        sendResultCanceledAndFinish(true)
                    }
                }
            } else {
                sendResultCanceledAndFinish(true)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            sendResultCanceledAndFinish(true)
        }
    }

    private fun createEmptyFileAndGetUri(): Uri? {
        try {
            imageName = "${UUID.randomUUID()}.jpg"
            val file = File(cacheDir, imageName!!)

            if (!file.exists()) {
                file.createNewFile().also {
                    if (it) {
                        imagePath = file.absolutePath
                        return FileProvider.getUriForFile(
                            this,
                            "${callingPackageName}.basicimagepicker.fileprovider",
                            file
                        )
                    } else {
                        sendResultCanceledAndFinish(true)
                    }
                }
            } else {
                sendResultCanceledAndFinish(true)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    private fun copyGalleryVideoFileToInternalStorage() {

        try {
            val file = File(cacheDir, videoName!!)
            if (!file.exists()) {
                file.createNewFile().also { status ->
                    if (status) {
                        try {
                            FileOutputStream(file).use { outputStream ->
                                contentResolver.openInputStream(videoUri!!)?.use { input ->
                                    input.copyTo(outputStream)
                                }
                            }
                            videoPath = file.absolutePath
                            sendVideoResultOkAndFinish()
                        } catch (ex: IOException) {
                            ex.printStackTrace()
                            sendResultCanceledAndFinish(true)
                        }
                    } else {
                        sendResultCanceledAndFinish(true)
                    }
                }
            } else {
                sendResultCanceledAndFinish(true)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            sendResultCanceledAndFinish(true)
        }
    }

    private fun checkVideoFileSizeAndGetFileName(videoFileUri: Uri): String? {

        val cursorUri = contentResolver.query(videoFileUri, null, null, null, null)

        cursorUri?.let { cursor ->

            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            var fileNameExtension = cursor.getString(nameIndex).substringAfterLast('.')
            val fileSize = cursor.getLong(sizeIndex)
            cursor.close()

            //if video size is required
            if (config.videoSizeLimit != null) {

                val sizeLimitInLong: Long = (config.videoSizeLimit!! * 1024 * 1024).toLong()
                if (fileSize > sizeLimitInLong) {
                    Toast.makeText(
                        this,
                        "Video size should not exceed ${config.videoSizeLimit} MB",
                        Toast.LENGTH_LONG
                    ).show()
                    galleryVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    return ""
                }
            }
            fileNameExtension = "${UUID.randomUUID()}.$fileNameExtension"
            return fileNameExtension
        }

        return null
    }
}