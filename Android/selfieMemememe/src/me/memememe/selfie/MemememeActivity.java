package me.memememe.selfie;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

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
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.PhotoPost;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;

public class MemememeActivity extends Activity implements CvCameraViewListener2 {
    private static enum Type {LOOKER, REFLECTER};
    private final Type mType = (Build.SERIAL.equals("04d8d9b29715d6ef")?Type.LOOKER:Type.REFLECTER);

    private static enum State {WAITING, SEARCHING, LOOKING, REFLECTING, FLASHING, POSTING};

    private static final String TAG = "MEMEMEME::SELFIE";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private static final Scalar BLACK_SCREEN_COLOR = new Scalar(0, 0, 0, 255);
    private static final String OSC_OUT_ADDRESS = "172.26.10.132";
    private static final int OSC_OUT_PORT = 8888;

    private Mat mRgba;
    private Mat mGray;
    private Mat  mTempRgba;
    private File mCascadeFile;

    private float mRelativeDetectSize = 0.15f;
    private int mAbsoluteDetectSize = 0;

    private DetectionBasedTracker mNativeDetector;
    private CameraBridgeViewBase mOpenCvCameraView;
    private OSCPortOut mOscOut;

    private State mCurrentState;
    private long mLastStateChangeMillis;
    private Scalar mCurrentFlashColor;
    private Point mCurrentFlashPosition;
    private Random mRandomGenerator;

    private JumblrClient mTumblrClient;

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
        mRandomGenerator = new Random();
        mTumblrClient = new JumblrClient(
                "16svfFXx0K9IMsV8TCCjDhTMrIiKpJLlTTlCOfVJjNREaHjgNm",
                "tuitRq41Y1QO9shzegw6YkAuYNCqMH6FDvKVQX7d3yLN5ydVS9",
                "UZMLiqrzaFQrQDd69yPxdx50D98J2UeAyEW1DTuHi3jXHqnw9X",
                "xLEtnEGrTVSOZ9AA8G27aO8m0D0EuEXB1gR4k07KbzYk2maQu9");
    }

    @Override
    public void onPause(){
        super.onPause();
        try{
            mOscOut.send(new OSCMessage("/memememe/stop"));
        }
        catch(IOException e){
            Log.e(TAG, "IO Exception!!: while sending stop osc message.");
        }
        catch(NullPointerException e){
            Log.e(TAG, "Null Pointer Exception!!: while sending stop osc message.");
        }
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        if(mNativeDetector != null) mNativeDetector.release();
        if(mOscOut != null) mOscOut.close();
    }

    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);

        // initialize OSC
        try{
            mOscOut = new OSCPortOut(InetAddress.getByName(OSC_OUT_ADDRESS), OSC_OUT_PORT);
        }
        catch(UnknownHostException e){
            Log.e(TAG, "Unknown Host Exception!!: while starting OscOut with (address, port): "+OSC_OUT_ADDRESS+", "+OSC_OUT_PORT);
        }
        catch(SocketException e){
            Log.e(TAG, "Socket Exception!!: while starting OscOut with (address, port): "+OSC_OUT_ADDRESS+", "+OSC_OUT_PORT);
        }

        // send first message to motors
        sendSearchToPlatform().start();

        // initialize state machine
        mCurrentState = State.SEARCHING;
        if(mType == Type.REFLECTER) mCurrentState = State.REFLECTING;

        mLastStateChangeMillis = System.currentTimeMillis();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        if(mNativeDetector != null) mNativeDetector.release();
        if(mOscOut != null) mOscOut.close();
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

    private Thread sendSearchToPlatform(){
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try{
                    mOscOut.send(new OSCMessage("/memememe/search"));
                }
                catch(IOException e){
                    Log.e(TAG, "IO Exception!!: while sending search osc message.");
                }
                catch(NullPointerException e){
                    Log.e(TAG, "Null Pointer Exception!!: while sending search osc message.");
                }
            }
        });
        return thread;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mTempRgba = inputFrame.rgba();
        //mGray = inputFrame.gray().t();
        Core.flip(inputFrame.gray().t(), mGray, 0);

        // only need to do this once
        if (mAbsoluteDetectSize == 0) {
            int height = mGray.cols();
            if (Math.round(height*mRelativeDetectSize) > 0) {
                mAbsoluteDetectSize = Math.round(height*mRelativeDetectSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteDetectSize);
            mNativeDetector.start();
        }

        // always detect in order to keep NativeDetector consistent with camera
        MatOfRect detectedRects = new MatOfRect();
        if (mNativeDetector != null) {
            mNativeDetector.detect(mGray, detectedRects);
        }
        else {
            Log.e(TAG, "Native Detection method is NULL");
        }
        Rect[] detectedArray = detectedRects.toArray();
        detectedRects.release();

        // states
        if(mCurrentState == State.SEARCHING){
            mTempRgba.setTo(BLACK_SCREEN_COLOR);

            // keep from finding too often
            if((System.currentTimeMillis()-mLastStateChangeMillis > 4000) && (detectedArray.length > 0)){
                Log.d(TAG, "found something while SEARCHING");
                /////////////////
                Core.rectangle(mTempRgba,
                        new Point(mTempRgba.width()-detectedArray[0].tl().y, detectedArray[0].br().x),
                        new Point(mTempRgba.width()-detectedArray[0].br().y, detectedArray[0].tl().x),
                        FACE_RECT_COLOR, 3);

                Point imgCenter = new Point(mGray.width()/2, mGray.height()/2);
                final Point lookAt = new Point(
                        ((detectedArray[0].tl().x>imgCenter.x)?-1:(detectedArray[0].br().x<imgCenter.x)?1:0),
                        ((detectedArray[0].br().y>imgCenter.y)?-1:(detectedArray[0].tl().y<imgCenter.y)?1:0));

                if(lookAt.x > 0)
                    Log.d(TAG, "need to look to my LEFT");
                if(lookAt.x < 0)
                    Log.d(TAG, "need to look to my RIGHT");
                if(lookAt.y > 0)
                    Log.d(TAG, "need to look to my UP");
                if(lookAt.y < 0)
                    Log.d(TAG, "need to look to my DOWN");

                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try{
                            mOscOut.send(new OSCMessage("/memememe/stop"));
                            // like a double take
                            Point doubleTake = new Point(
                                    ((mRandomGenerator.nextFloat()>0.5)?1:-1),
                                    ((mRandomGenerator.nextFloat()>0.5)?1:-1));
                            OSCMessage lookMessage = new OSCMessage("/memememe/look");
                            lookMessage.addArgument((int)doubleTake.x);
                            lookMessage.addArgument((int)doubleTake.y);
                            mOscOut.send(lookMessage);
                            lookMessage = new OSCMessage("/memememe/look");
                            lookMessage.addArgument((int)(-doubleTake.x));
                            lookMessage.addArgument((int)(-doubleTake.y));
                            mOscOut.send(lookMessage);
                            // the lookAt values
                            lookMessage = new OSCMessage("/memememe/look");
                            lookMessage.addArgument((int)lookAt.x);
                            lookMessage.addArgument((int)lookAt.y);
                            mOscOut.send(lookMessage);
                        }
                        catch(IOException e){
                            Log.e(TAG, "IO Exception!!: while sending look osc message.");
                        }
                        catch(NullPointerException e){
                            Log.e(TAG, "Null Pointer Exception!!: while sending look osc message.");
                        }
                        mLastStateChangeMillis = System.currentTimeMillis();
                        mCurrentState = (mRandomGenerator.nextBoolean())?State.LOOKING:State.REFLECTING;
                        if(mType == Type.LOOKER) mCurrentState = State.LOOKING;
                    }
                });
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.WAITING;
                thread.start();
            }
        }
        else if(mCurrentState == State.REFLECTING){
            Log.d(TAG, "state := REFLECTING");
            // do nothing to image

            // if reflecting for 5 seconds, go back to searching
            if(System.currentTimeMillis()-mLastStateChangeMillis > 5000){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SEARCHING;
                if(mType == Type.REFLECTER) mCurrentState = State.REFLECTING;
                sendSearchToPlatform().start();
            }
        }
        else if(mCurrentState == State.LOOKING){
            Log.d(TAG, "state := LOOKING");
            mTempRgba.setTo(BLACK_SCREEN_COLOR);

            if((System.currentTimeMillis()-mLastStateChangeMillis > 1000) && (detectedArray.length > 0)){
                Log.d(TAG, "found something while LOOKING");

                // in mTempRgba coordinates!!!
                mCurrentFlashPosition = new Point(
                        mTempRgba.width()-detectedArray[0].tl().y-detectedArray[0].height/2,
                        detectedArray[0].br().x-detectedArray[0].width/2);

                mCurrentFlashColor = new Scalar(
                        128+mRandomGenerator.nextInt(128),
                        128+mRandomGenerator.nextInt(128),
                        128+mRandomGenerator.nextInt(128), 255);
                mTempRgba.setTo(mCurrentFlashColor);
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.FLASHING;
            }

            // if looking for more than 5 seconds, go back to searching
            if(System.currentTimeMillis()-mLastStateChangeMillis > 5000){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SEARCHING;
                sendSearchToPlatform().start();
            }
        }
        else if(mCurrentState == State.FLASHING){
            Log.d(TAG, "state := FLASHING");

            byte detectedColor[] = new byte[4];
            mTempRgba.get((int)mCurrentFlashPosition.y,(int)mCurrentFlashPosition.x,detectedColor);

            // checking these 2 values seem to be enough
            if((detectedColor[1]&0xff)>200 && (detectedColor[2]&0xff)>200){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.POSTING;
            }

            // if flashing for more than 1 second, go back to searching
            if(System.currentTimeMillis()-mLastStateChangeMillis > 2000){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SEARCHING;
                sendSearchToPlatform().start();
            }
            mTempRgba.setTo(mCurrentFlashColor);
        }
        else if(mCurrentState == State.WAITING){
            Log.d(TAG, "state := WAITING");
            mTempRgba.setTo(BLACK_SCREEN_COLOR);
            // if waiting for 2 seconds, go back to searching
            if(System.currentTimeMillis()-mLastStateChangeMillis > 2000){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SEARCHING;
                sendSearchToPlatform().start();
            }
        }
        else if(mCurrentState == State.POSTING){
            Log.d(TAG, "state := POSTING");

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String filename = "selfie.jpg";
            File file = new File(path, filename);

            // TODO: there's a bug here to fix
            Core.flip(mTempRgba.t(), mRgba, 0);
            Imgproc.cvtColor(mRgba, mTempRgba, Imgproc.COLOR_BGR2RGB);
            Highgui.imwrite(file.toString(), mTempRgba);

            try{
                PhotoPost mPP = mTumblrClient.newPost("memememeselfie.tumblr.com", PhotoPost.class);
                mPP.setData(file);
                mPP.save();
            }
            catch(IOException e){
                Log.e(TAG, "IO Exception!!: while sending picture to tumblr.");
            }
            catch(Exception e){
                Log.e(TAG, "some Exception!!: while sending picture to tumblr.");
                Log.e(TAG, e.toString());
            }

            mLastStateChangeMillis = System.currentTimeMillis();
            mCurrentState = State.SEARCHING;
            sendSearchToPlatform().start();
            mTempRgba.setTo(BLACK_SCREEN_COLOR);
        }

        Core.flip(mTempRgba, mRgba, 1);
        return mRgba;
    }
}
