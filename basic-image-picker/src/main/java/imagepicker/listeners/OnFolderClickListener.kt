package imagepicker.listeners

import imagepicker.model.Folder

interface OnFolderClickListener {
    fun onFolderClick(bucket: Folder)
}