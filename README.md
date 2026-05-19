# Basic Image Picker

A small Android library for taking photos with the camera or picking images / videos from
the device. Uses the system photo picker on Android 11+ (and any device with the Play
Picker module) and falls back to the bundled
[esafirm/android-image-picker](https://github.com/esafirm/android-image-picker) UI on older
devices. Supports single, multi-select, and video picks.

- Min SDK 21, target SDK 35
- Kotlin + Java 17
- Returns a `BasicImageData` with `name`, durable `path`, content `uri`, and (camera-only)
  gallery `uri`

---

## Install

`settings.gradle` (or root `build.gradle`):

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Module `build.gradle`:

```kotlin
dependencies {
    implementation("com.github.abdullahalshafi:BasicImagePicker:1.1.7")
}
```

If you launch the camera flow, add `android.permission.CAMERA` to your `AndroidManifest.xml`
(the library handles the runtime prompt). No other permissions required — the system photo
picker and scoped MediaStore handle the rest.

---

## Quick start

Every flow follows the same shape: register an `ActivityResultLauncher`, configure
`ImageUtilHelper`, and read `BasicImageData` from the result.

### Camera

```kotlin
private val cameraLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val image = result.data!!.getSerializableExtra(
            BasicImageData::class.java.simpleName
        ) as BasicImageData
        // image.path → local File path, image.uri → content URI,
        // image.galleryUri → MediaStore URI when saveIntoGallery(true)
    }
}

ImageUtilHelper.create(this, cameraLauncher) {
    isCamera(true)
    saveIntoGallery(true)            // optional: also write a copy into DCIM
    galleryDirectoryName("MyApp")    // optional: DCIM/MyApp/<name>.jpg
    start()
}
```

### Gallery (single image)

```kotlin
ImageUtilHelper.create(this, galleryLauncher) {
    isGallery(true)
    start()
}
```

### Gallery (multiple images)

```kotlin
ImageUtilHelper.create(this, multiImageLauncher) {
    multi()
    maxImage(5)   // optional cap; omit for system max
    start()
}
```

Reading multi-pick results:

```kotlin
@Suppress("UNCHECKED_CAST")
val images = result.data!!.getSerializableExtra(
    BasicImageData::class.java.simpleName
) as List<BasicImageData>
```

### Video

```kotlin
ImageUtilHelper.create(this, galleryVideoLauncher) {
    isGallery(true)
    isOnlyVideo(true)
    setVideoSizeLimitInMB(10)   // optional; picker re-launches if over limit
    start()
}
```

Result shape is identical to a single image (`BasicImageData`); `path` points at a real
file copy in your app's cache so you can upload or play it.

---

## Customizing colors

The library applies colors in the following precedence (highest wins):

1. **`colors.xml` override** in your app.
2. **Calling activity's theme** (`colorPrimary`) — auto-derived.
3. **Bundled defaults** (indigo).

### 1. Override via `colors.xml`

Define any of these in your app's `res/values/colors.xml`. Each is independent — set only
what you want to change.

```xml
<resources>
    <!-- Toolbar background -->
    <color name="basic_image_picker_colorPrimary">#1976D2</color>
    <!-- Status bar -->
    <color name="basic_image_picker_colorPrimaryDark">#0D47A1</color>
    <!-- Toolbar title text -->
    <color name="basic_image_picker_colorTextPrimary">#FFFFFF</color>
    <!-- Back-arrow tint -->
    <color name="basic_image_picker_toolbar_icon_color">#FFFFFF</color>
    <!-- Selection / checkbox accent -->
    <color name="basic_image_picker_colorAccent">#FF4081</color>
</resources>
```

Any color you don't define falls through to step 2.

### 2. Auto-derive from your theme

If you don't override anything, the library reads `colorPrimary` from your calling
activity's theme and derives the rest:

- toolbar background ← `colorPrimary`
- status bar ← darkened `colorPrimary`
- toolbar text / arrow ← black or white depending on `colorPrimary`'s luminance

No setup required — just use a standard `Theme.MaterialComponents.*` / `Theme.AppCompat.*`
host with `colorPrimary` set.

### 3. Bundled fallback

If your theme has no `colorPrimary` and you haven't overridden anything, the picker uses
its bundled indigo defaults (`#3F51B5` / `#303F9F`).

---

## API reference

`ImageUtilHelper.create(context, launcher) { … }` builder DSL:

| Method | Description |
| --- | --- |
| `isCamera(true)` | Capture a new photo with the camera. |
| `isGallery(true)` | Pick from the gallery. |
| `isOnlyVideo(true)` | Restrict gallery picker to videos. |
| `setVideoSizeLimitInMB(n)` | Reject videos larger than `n` MB and re-launch the picker. |
| `saveIntoGallery(true)` | After camera capture, also save a copy into the public gallery. |
| `galleryDirectoryName("Name")` | Subdirectory under DCIM when `saveIntoGallery(true)`. |
| `multi()` | Multi-image pick mode. |
| `maxImage(n)` | Cap on number of images in multi mode. |
| `start()` | Launch the picker. |

`BasicImageData` (Serializable):

| Field | Notes |
| --- | --- |
| `name: String` | Generated filename, e.g. `4f7a-….jpg`. |
| `path: String` | Absolute path inside app cache. Always populated. |
| `uri: String` | Content URI of the cache copy (FileProvider). |
| `galleryUri: String?` | MediaStore URI of the gallery copy (camera + `saveIntoGallery` only). |

---

## License

MIT. Bundles a fork of [esafirm/android-image-picker](https://github.com/esafirm/android-image-picker).
