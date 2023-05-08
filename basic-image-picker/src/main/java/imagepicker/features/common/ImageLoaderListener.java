package imagepicker.features.common;

import imagepicker.model.Folder;
import imagepicker.model.Image;

import java.util.List;

public interface ImageLoaderListener {
    void onImageLoaded(List<Image> images, List<Folder> folders);
    void onFailed(Throwable throwable);
}
