package imagepicker.features;



import java.util.ArrayList;

import imagepicker.features.cameraonly.CameraOnlyConfig;

public class ImagePickerConfigFactory {

    public static CameraOnlyConfig createCameraDefault() {
        CameraOnlyConfig config = new CameraOnlyConfig();
        config.setSavePath(ImagePickerSavePath.DEFAULT);
        config.setReturnMode(ReturnMode.ALL);
        config.setSaveImage(true);
        return config;
    }

    public static ImagePickerConfig createDefault() {
        ImagePickerConfig config = new ImagePickerConfig();
        config.setMode(IpCons.MODE_MULTIPLE);
        config.setLimit(IpCons.MAX_LIMIT);
        config.setShowCamera(true);
        config.setFolderMode(false);
        config.setSelectedImages(new ArrayList<>());
        config.setSavePath(ImagePickerSavePath.DEFAULT);
        config.setReturnMode(ReturnMode.NONE);
        config.setSaveImage(true);
        return config;
    }
}
