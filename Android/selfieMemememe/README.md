### Selfie memememe

Android app for detecting cell phones with OpenCV.

### Notes

Project updated for Android Studio, to compile create a symbolik link outside the project folder **selfieMemememe** with the name `OpenCV-android-sdk` pointing the OpenCV sdk folder, example:

```
OpenCV-android-sdk -> /Users/tgh/Dev/OpenCV-android-sdk
```

### Tumblr Keys
To make the phones post to tumblr, get your keys from dev tumblr and replace the 

```
/app/src/main/res/values/.keys.xml.REPLACE
```
to
```
/app/src/main/res/values/keys.xml
```

with the right keys.

### Dependencies:  

- JavaOSC ([github](https://github.com/hoijui/JavaOSC), [docs](http://www.illposed.com/software/javaoscdoc/))  
- OpenCV 3 ([site](http://opencv.org/platforms/android.html))  
- JTransforms ([site](https://sites.google.com/site/piotrwendykier/software/jtransforms), [docs](http://nclab.kaist.ac.kr/~twpark/JTransforms/doc/index.html))  
- Jumblr ([github](https://github.com/tumblr/jumblr))  
