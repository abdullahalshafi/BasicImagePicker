package com.shafi.basicimagepicker

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.shafi.basic_image_picker.model.BasicImageData
import com.shafi.basic_image_picker.util.ImageUtilHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //camera
        findViewById<Button>(R.id.camera_btn).setOnClickListener {
            ImageUtilHelper.create(this, cameraLauncher) {
                isCamera(true)
                saveIntoGallery(true)
                galleryDirectoryName("MyDirectory")
                start()
            }
        }

        //gallery
        findViewById<Button>(R.id.gallery_btn).setOnClickListener {
            ImageUtilHelper.create(this, galleryLauncher) {
                isGallery(true)
                start()
            }
        }

        //video
        findViewById<Button>(R.id.video_btn).setOnClickListener {
            ImageUtilHelper.create(this, galleryVideoLauncher) {
                isGallery(true)
                isOnlyVideo(true)
                setVideoSizeLimitInMB(10)
                start()
            }
        }
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {

                if (it.resultCode == Activity.RESULT_OK) {

                    val basicImageData: BasicImageData =
                        it.data!!.getSerializableExtra(BasicImageData::class.java.simpleName) as BasicImageData

                    //do stuffs with the image object
                    Glide.with(this)
                        .load(basicImageData.path)
                        .into(findViewById(R.id.image_view))
                } else if (it.resultCode == Activity.RESULT_CANCELED) {
                    //handle your own situation
                }
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (it.resultCode == Activity.RESULT_OK) {

                    val basicImageData: BasicImageData =
                        it.data!!.getSerializableExtra(BasicImageData::class.java.simpleName) as BasicImageData

                    //do stuffs with the image object
                    Glide.with(this)
                        .load(basicImageData.path)
                        .into(findViewById(R.id.image_view))
                } else if (it.resultCode == Activity.RESULT_CANCELED) {
                    //handle your own situation
                }
            }
        }

    private val galleryVideoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (it.resultCode == Activity.RESULT_OK) {

                    val basicImageData: BasicImageData =
                        it.data!!.getSerializableExtra(BasicImageData::class.java.simpleName) as BasicImageData

                    Log.d("VIDEO_DATA", "name: ${basicImageData.name} path: ${basicImageData.path}")

                } else if (it.resultCode == Activity.RESULT_CANCELED) {
                    //handle your own situation
                }
            }
        }
}