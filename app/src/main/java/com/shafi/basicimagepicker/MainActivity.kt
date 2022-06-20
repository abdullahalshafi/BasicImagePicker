package com.shafi.basicimagepicker

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.shafi.basic_image_picker.model.Image
import com.shafi.basic_image_picker.util.ImageUtilHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //camera
        findViewById<Button>(R.id.camera_btn).setOnClickListener {
            ImageUtilHelper.create(this, cameraLauncher){
                isCamera(true)
                saveIntoGallery(true)
                galleryDirectoryName("MyDirectory")
                start()
            }
        }

        //gallery
        findViewById<Button>(R.id.gallery_btn).setOnClickListener {
            ImageUtilHelper.create(this, galleryLauncher){
                isGallery(true)
                start()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){

            if (it.resultCode == Activity.RESULT_OK) {

                val image: Image =
                    it.data!!.getSerializableExtra(Image::class.java.simpleName) as Image

                //do stuffs with the image object
                Glide.with(this)
                    .load(image.path)
                    .into(findViewById(R.id.image_view))
            }else if(it.resultCode == Activity.RESULT_CANCELED) {
                //handle your own situation
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){
            if (it.resultCode == Activity.RESULT_OK) {

                val image: Image =
                    it.data!!.getSerializableExtra(Image::class.java.simpleName) as Image

                //do stuffs with the image object
                Glide.with(this)
                    .load(image.path)
                    .into(findViewById(R.id.image_view))
            }else if(it.resultCode == Activity.RESULT_CANCELED) {
                //handle your own situation
            }
        }
    }
}