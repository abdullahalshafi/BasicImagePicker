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
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.shafi.basic_image_picker.model.BasicImageData
import com.shafi.basic_image_picker.model.ImageUtilConfig
import com.shafi.basic_image_picker.R
import com.shafi.basic_image_picker.util.ImageUtilHelper.Companion.PACKAGE_NAME
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class ImageUtilActivity : AppCompatActivity() {

    private lateinit var config: ImageUtilConfig

    private var imageName: String? = null
    private var imagePath: String? = null
    private var imageUri: Uri? = null

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
            galleryLauncher.launch("image/*")
        }
    }

    //send result to activity
    private fun sendResultOkAndFinish() {
        if (imageName != null && imagePath != null && imageUri != null) {
            val basicImageData = BasicImageData(imageName!!, imagePath!!, imageUri.toString())
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
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                copyGalleryFileToInternalStorage()
                sendResultOkAndFinish()
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
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun copyGalleryFileToInternalStorage() {

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
                        } catch (ex: IOException) {
                            ex.printStackTrace()
                            sendResultCanceledAndFinish(true)
                        }

                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
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
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}