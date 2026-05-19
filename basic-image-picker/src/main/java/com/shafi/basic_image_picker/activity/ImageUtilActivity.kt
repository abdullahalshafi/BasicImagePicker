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
import androidx.lifecycle.lifecycleScope
import com.shafi.basic_image_picker.model.BasicImageData
import com.shafi.basic_image_picker.model.ImageUtilConfig
import com.shafi.basic_image_picker.R
import com.shafi.basic_image_picker.util.ImageUtilHelper.Companion.PACKAGE_NAME
import imagepicker.features.ImagePicker
import imagepicker.features.IpCons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

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
        config = readConfig()

        if (config.isCamera) {

            if (config.saveIntoGallery) {
                if (checkWritePermission()) {
                    launchCamera()
                    return
                }
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
                val picker = ImagePicker.create(this)
                    .folderMode(true)
                    .toolbarFolderTitle(getString(R.string.select_photo_album))
                    .toolbarImageTitle(getString(R.string.tap_to_select))
                    .includeVideo(false)
                    .single()
                    .showCamera(false)
                    .enableLog(false)
                config.toolbarColor?.let { picker.toolbarColor(it) }
                config.statusBarColor?.let { picker.statusBarColor(it) }
                if (config.toolbarOnColor != null) {
                    picker.toolbarTextColor(config.toolbarOnColor!!)
                } else {
                    // Caller's theme didn't provide a color; keep the pre-existing arrow tint.
                    picker.toolbarArrowColor(
                        ContextCompat.getColor(this, R.color.basic_image_picker_toolbar_icon_color)
                    )
                }
                picker.start()
            }
        } else {
            throw IllegalArgumentException(getString(R.string.you_must_specify_camera_or_gallery))
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
                    lifecycleScope.launch {
                        saveImageToGallery()
                        sendResultOkAndFinish()
                    }
                } else {
                    sendResultOkAndFinish()
                }
            } else {
                sendResultCanceledAndFinish(false)
            }
        }

    //gallery intent result
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                imageUri = uri
                lifecycleScope.launch {
                    imageName = "${UUID.randomUUID()}.jpg"
                    val path = copyUriToCache(uri, imageName!!)
                    if (path != null) {
                        imagePath = path
                        sendResultOkAndFinish()
                    } else {
                        sendResultCanceledAndFinish(true)
                    }
                }
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
                lifecycleScope.launch {
                    imageName = "${UUID.randomUUID()}.jpg"
                    val path = copyUriToCache(image.uri, imageName!!)
                    if (path != null) {
                        imagePath = path
                        sendResultOkAndFinish()
                    } else {
                        sendResultCanceledAndFinish(true)
                    }
                }

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
                videoUri = uri
                lifecycleScope.launch {
                    val videoFileName = checkVideoFileSizeAndGetFileName(uri)

                    //if size limit exceeded
                    if (videoFileName == "") {
                        return@launch
                    }

                    //if something went wrong
                    if (videoFileName == null) {
                        sendResultCanceledAndFinish(true)
                        return@launch
                    }

                    videoName = videoFileName
                    val path = copyUriToCache(uri, videoFileName)
                    if (path != null) {
                        videoPath = path
                        sendVideoResultOkAndFinish()
                    } else {
                        sendResultCanceledAndFinish(true)
                    }
                }
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

    /**
     * Copy the cache file produced by the camera into the gallery. On Q+ this inserts into
     * MediaStore via ContentResolver (the canonical scoped-storage pattern); pre-Q writes
     * directly into DCIM/<galleryDirectory>.
     */
    private suspend fun saveImageToGallery(): Unit = withContext(Dispatchers.IO) {
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
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } else {
            try {
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        .toString() + subDirectory
                )
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, imageName!!)
                if (!file.exists()) {
                    FileOutputStream(file).use { outputStream ->
                        contentResolver.openInputStream(imageUri!!)?.use { input ->
                            input.copyTo(outputStream)
                        }
                    }
                    galleryUri = file.toUri()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /** Copy a content URI into cacheDir so callers receive a durable File path. */
    private suspend fun copyUriToCache(uri: Uri, fileName: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    val input = contentResolver.openInputStream(uri)
                        ?: return@withContext null
                    input.use { it.copyTo(outputStream) }
                }
                file.absolutePath
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }

    private fun createEmptyFileAndGetUri(): Uri? {
        return try {
            imageName = "${UUID.randomUUID()}.jpg"
            val file = File(cacheDir, imageName!!)
            imagePath = file.absolutePath
            FileProvider.getUriForFile(
                this,
                "${callingPackageName}.basicimagepicker.fileprovider",
                file
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    /**
     * Returns the generated file name, "" if the user picked a video over the size limit
     * (picker is re-launched), or null on error.
     */
    private suspend fun checkVideoFileSizeAndGetFileName(videoFileUri: Uri): String? {
        data class VideoMeta(val extension: String, val size: Long)

        val meta = withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.query(videoFileUri, null, null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@runCatching null
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex < 0 || sizeIndex < 0) return@runCatching null
                    val ext = cursor.getString(nameIndex)?.substringAfterLast('.', "")
                        ?: return@runCatching null
                    VideoMeta(ext, cursor.getLong(sizeIndex))
                }
            }.getOrNull()
        } ?: return null

        config.videoSizeLimit?.let { limitMb ->
            val limitBytes = limitMb.toLong() * 1024L * 1024L
            if (meta.size > limitBytes) {
                Toast.makeText(
                    this,
                    "Video size should not exceed $limitMb MB",
                    Toast.LENGTH_LONG
                ).show()
                galleryVideoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
                return ""
            }
        }
        return "${UUID.randomUUID()}.${meta.extension}"
    }
}
