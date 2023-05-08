package imagepicker.features.imageloader;

import android.widget.ImageView;

import imagepicker.model.Image;

public interface ImageLoader {
    void loadImage(Image image, ImageView imageView, ImageType imageType);
}
