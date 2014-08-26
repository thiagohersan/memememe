package me.memememe.selfie;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

public class FdActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "MEMEMEME::SELFIE";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    private Mat mRgba;
    private Mat mGray;
    private Mat  mTempRgba;
    private File mCascadeFile;

    private float mRelativeDetectSize = 0.2f;
    private int mAbsoluteDetectSize = 0;

    private DetectionBasedTracker mNativeDetector;
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if(status == LoaderCallbackInterface.SUCCESS){
                Log.i(TAG, "OpenCV loaded successfully");

                // Load native library after(!) OpenCV initialization
                System.loadLibrary("detection_based_tracker");

                try {
                    // load cascade file from application resources
                    InputStream is = getResources().openRawResource(R.raw.haarcascade_nexus);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    mCascadeFile = new File(cascadeDir, "cascade.xml");
                    FileOutputStream os = new FileOutputStream(mCascadeFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    is.close();
                    os.close();

                    mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                    cascadeDir.delete();
                } 
                catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                }

                mOpenCvCameraView.enableView();
            }
            else{
                super.onManagerConnected(status);
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.memememe_selfie_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // find front-facing camera
        for(int i=0; i<Camera.getNumberOfCameras(); i++){
            CameraInfo mInfo = new CameraInfo();
            Camera.getCameraInfo(i, mInfo);
            if(mInfo.facing == CameraInfo.CAMERA_FACING_FRONT){
                mOpenCvCameraView.setCameraIndex(i);
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if(mNativeDetector != null)
            mNativeDetector.release();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mTempRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mNativeDetector.stop();
        mAbsoluteDetectSize = 0;
        mGray.release();
        mRgba.release();
        mTempRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mTempRgba = inputFrame.rgba();
        mGray = inputFrame.gray().t();

        // only need to do this once
        if (mAbsoluteDetectSize == 0) {
            int height = mGray.cols();
            if (Math.round(height*mRelativeDetectSize) > 0) {
                mAbsoluteDetectSize = Math.round(height*mRelativeDetectSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteDetectSize);
            mNativeDetector.start();
        }

        MatOfRect detectedRects = new MatOfRect();

        if (mNativeDetector != null) {
            mNativeDetector.detect(mGray, detectedRects);
        }
        else {
            Log.e(TAG, "Native Detection method is NULL");
        }

        Rect[] detectedArray = detectedRects.toArray();
        for (int i = 0; i < detectedArray.length; i++)
            Core.rectangle(mTempRgba,
                    new Point(detectedArray[i].tl().y, detectedArray[i].tl().x),
                    new Point(detectedArray[i].br().y, detectedArray[i].br().x),
                    FACE_RECT_COLOR, 3);

        Core.flip(mTempRgba, mRgba, 1);
        detectedRects.release();

        return mRgba;
    }
}
