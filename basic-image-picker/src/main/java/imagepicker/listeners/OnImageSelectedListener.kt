package imagepicker.listeners

import imagepicker.model.Image

interface OnImageSelectedListener {
    fun onSelectionUpdate(selectedImage: List<Image?>?)
}