# Basic Image Picker
A Simple Android Library to capture and pick image from gallery.

### Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```Kotlin
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2. Add the dependency
```Kotlin
dependencies {
    implementation 'com.github.abdullahalshafi:BasicImagePicker:1.0.4'
}
```

### Usage
#### Camera
```kotlin
 ImageUtilHelper.create(this, cameraLauncher){
    isCamera(true)
    saveIntoGallery(true)
    galleryDirectoryName("MyDirectory")
    start()
 }
```

#### Gallery
```kotlin
 ImageUtilHelper.create(this, galleryLauncher){
    isGallery(true)
    start()
 }
```

#### Camera Result
```kotlin
private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
  if(it.resultCode == Activity.RESULT_OK){

    if (it.resultCode == Activity.RESULT_OK) {

       val image: Image =
          it.data!!.getSerializableExtra(Image::class.java.simpleName) as Image

       //do stuffs with the image object
     }else if(it.resultCode == Activity.RESULT_CANCELED) {
        //handle your own situation
      }
     }
 }
```

#### Gallery Result
```kotlin
private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
     if(it.resultCode == Activity.RESULT_OK){
        if (it.resultCode == Activity.RESULT_OK) {

         val image: Image =
                   it.data!!.getSerializableExtra(Image::class.java.simpleName) as Image

          //do stuffs with the image object
         }else if(it.resultCode == Activity.RESULT_CANCELED) {
           //handle your own situation
         }
        }
   }
```

#### Compress Image using zetbaitsu Compressor
```kotlin
val file = ImageUtilHelper.compressImage(this@MainActivity, file, 50, 300,300)
```


