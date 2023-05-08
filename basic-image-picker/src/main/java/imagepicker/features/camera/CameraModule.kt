package imagepicker.features.camera

import android.content.Context
import android.content.Intent
import imagepicker.features.common.BaseConfig


interface CameraModule {
    fun getCameraIntent(context: Context, config: BaseConfig): Intent?
    fun getImage(context: Context, intent: Intent?, imageReadyListener: OnImageReadyListener?)
    fun removeImage()
}