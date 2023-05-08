package com.esafirm.imagepicker.features.common;

import android.os.Parcel;
import android.os.Parcelable;

import com.esafirm.imagepicker.features.ImagePickerSavePath;
import com.esafirm.imagepicker.features.ReturnMode;

public class BaseConfig implements Parcelable {

    private ImagePickerSavePath savePath;
    private ReturnMode returnMode;
    private boolean saveImage = true;

    public ReturnMode getReturnMode() {
        return returnMode;
    }

    public ImagePickerSavePath getImageDirectory() {
        return savePath;
    }

    public void setSavePath(ImagePickerSavePath savePath) {
        this.savePath = savePath;
    }

    public void setImageDirectory(String dirName) {
        savePath = new ImagePickerSavePath(dirName, false);
    }

    public void setImageFullDirectory(String path) {
        savePath = new ImagePickerSavePath(path, true);
    }

    public void setReturnMode(ReturnMode returnMode) {
        this.returnMode = returnMode;
    }

    public boolean isSaveImage() {
        return saveImage;
    }

    public void setSaveImage(boolean saveImage) {
        this.saveImage = saveImage;
    }

    public BaseConfig() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.savePath, flags);
        dest.writeInt(this.returnMode == null ? -1 : this.returnMode.ordinal());
        dest.writeByte(this.saveImage ? (byte) 1 : (byte) 0);
    }

    protected BaseConfig(Parcel in) {
        this.savePath = in.readParcelable(ImagePickerSavePath.class.getClassLoader());
        int tmpReturnMode = in.readInt();
        this.returnMode = tmpReturnMode == -1 ? null : ReturnMode.values()[tmpReturnMode];
        this.saveImage = in.readByte() != 0;
    }

    public static final Creator<BaseConfig> CREATOR = new Creator<BaseConfig>() {
        @Override
        public BaseConfig createFromParcel(Parcel source) {
            return new BaseConfig(source);
        }

        @Override
        public BaseConfig[] newArray(int size) {
            return new BaseConfig[size];
        }
    };
}
