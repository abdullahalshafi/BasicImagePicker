package com.shafi.basic_image_picker.util

import android.app.ActionBar
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import com.shafi.basic_image_picker.R

class BasicProgressDialog(private val activity: Activity) {

    private lateinit var dialog: Dialog

    init {
        val display = activity.windowManager.defaultDisplay
        display.getSize(Point())
        dialog = Dialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.basic_progress_dialog, null)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.window!!.setDimAmount(0.3F)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setLayout(
            ActionBar.LayoutParams.WRAP_CONTENT,
            ActionBar.LayoutParams.WRAP_CONTENT
        )
    }

    fun show() {
        if (!activity.isFinishing && this::dialog.isInitialized && !dialog.isShowing) {
            dialog.show()
        }
    }

    fun dismiss() {
        if (!activity.isFinishing && this::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun isShowing(): Boolean {
        if (!activity.isFinishing && this::dialog.isInitialized) {
            return dialog.isShowing
        }
        return false
    }
}