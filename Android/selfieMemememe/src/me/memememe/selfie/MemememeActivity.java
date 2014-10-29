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
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.exceptions.JumblrException;
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
    private static enum State {WAITING, SEARCHING, LOOKING, REFLECTING, FLASHING, POSTING,
                               MAKING_NOISE_LOOKING, MAKING_NOISE_REFLECTING, SCANNING_LOOKING, SCANNING_REFLECTING};

    private static final String TAG = "MEMEMEME::SELFIE";
    private static final String SELFIE_FILE_NAME = "selfie.jpg";
    private static final String[] TEXTS = {"me", "meme", "mememe", "memememe", "#selfie"};
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private static final Scalar SCREEN_COLOR_FLASH = new Scalar(160, 160, 160, 255);
    private static final Scalar SCREEN_COLOR_BLACK = new Scalar(0, 0, 0, 255);
    private static final String OSC_OUT_ADDRESS = Build.SERIAL.equals("04d8d9b29715d6ef")?"10.10.0.110":"10.10.0.111";
    private static final int OSC_OUT_PORT = 8888;

    private static final int TIMEOUT_SCANNING = 10000;
    private static final int TIMEOUT_REFLECTING = 5000;
    private static final int TIMEOUT_MAKING_NOISE_LOOKING = 30000;
    private static final int PERIOD_MAKING_NOISE_LOOKING = 7000;
    private static final int TIMEOUT_MAKING_NOISE_REFLECTING = 5000;
    private static final int TIMEOUT_FLASHING = 4000;
    private static final int DELAY_FLASHING = 2000;
    private static final int TIMEOUT_WAITING = 2000;

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
    private String mCurrentFlashText;
    private Random mRandomGenerator;
    private int mImageCounter;

    private JumblrClient mTumblrClient;

    private NoiseReader mNoiseReader = null;
    private NoiseWriter mNoiseWriter = null;
    private Thread noiseReaderThread = null;
    private Thread noiseWriterThread = null;

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
        mImageCounter = 0;
        mTumblrClient = new JumblrClient(
                "16svfFXx0K9IMsV8TCCjDhTMrIiKpJLlTTlCOfVJjNREaHjgNm",
                "tuitRq41Y1QO9shzegw6YkAuYNCqMH6FDvKVQX7d3yLN5ydVS9",
                "Zhf9R1oEEAn39Q2OAHiEcB4XasHjcJBw2Y5MwbFbAZFQ7R9Icr",
                "zP50LmZLOsEAVyyMuoF5QPRU7I1tnzTJASHp4na3oZOFEYfqfp");

        mNoiseReader = new NoiseReader();
        mNoiseWriter = new NoiseWriter();
        noiseReaderThread = new Thread(mNoiseReader);
        noiseWriterThread = new Thread(mNoiseWriter);
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

        try{
            noiseReaderThread.interrupt();
            noiseWriterThread.interrupt();
            noiseReaderThread.join(500);
            noiseWriterThread.join(500);
        }
        catch(InterruptedException e){}
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

        // start sound threads
        noiseReaderThread.start();
        noiseWriterThread.start();

        // initialize state machine
        mCurrentState = State.SEARCHING;
        Log.d(TAG, "state := SEARCHING");
        // send first message to motors
        sendCommandToPlatform("search").start();

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

    private Thread sendCommandToPlatform(String cmd){
        final String theCommand = "/memememe/"+cmd;
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try{
                    mOscOut.send(new OSCMessage(theCommand));
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
        Core.flip(inputFrame.gray().t(), mGray, 0);

        // save images for video...
        /*
        Core.flip(mTempRgba.t(), mRgba, 0);
        String dateFilename = SELFIE_FILE_NAME.replace(".jpg", "")+String.format("%04d", mImageCounter++)+".jpg";
        final File movFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), dateFilename);
        Highgui.imwrite(movFile.toString(), mRgba);
        */

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
            mTempRgba.setTo(SCREEN_COLOR_BLACK);

            // keep from finding too often
            if((System.currentTimeMillis()-mLastStateChangeMillis > 5000) && (detectedArray.length > 0)){
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
                        mNoiseWriter.makeYesNoise();
                        mCurrentState = State.MAKING_NOISE_LOOKING;
                        Log.d(TAG, "state := MAKING_NOISE_LOOKING");
                    }
                });
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.WAITING;
                Log.d(TAG, "state := WAITING");
                thread.start();
            }

            // no phone detected, check for noises
            else if(mNoiseReader.isHearingYes() || mNoiseReader.isHearingNo()){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SCANNING_REFLECTING;
                Log.d(TAG, "state := SCANNING_REFLECTING");
                sendCommandToPlatform("scan").start();
            }
        }
        else if(mCurrentState == State.SCANNING_LOOKING){
            mTempRgba.setTo(SCREEN_COLOR_BLACK);

            if(detectedArray.length > 0){
                Log.d(TAG, "found something while SCANNING/LOOKING");
                sendCommandToPlatform("stop").start();

                // in mTempRgba coordinates!!!
                mCurrentFlashPosition = new Point(
                        mTempRgba.width()-detectedArray[0].tl().y-detectedArray[0].height/2,
                        detectedArray[0].br().x-detectedArray[0].width/2);

                mCurrentFlashColor = SCREEN_COLOR_FLASH;
                mCurrentFlashText = TEXTS[mRandomGenerator.nextInt(TEXTS.length)];
                mTempRgba.setTo(mCurrentFlashColor);
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.FLASHING;
                Log.d(TAG, "state := FLASHING");
            }

            // if scanning for a while, go back to searching
            if(System.currentTimeMillis()-mLastStateChangeMillis > TIMEOUT_SCANNING){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SEARCHING;
                Log.d(TAG, "state := SEARCHING");
                sendCommandToPlatform("search").start();
            }
        }
        else if(mCurrentState == State.SCANNING_REFLECTING){
            mTempRgba.setTo(SCREEN_COLOR_BLACK);

            if(detectedArray.length > 0){
                Log.d(TAG, "found something while SCANNING/REFLECTING");
                sendCommandToPlatform("stop").start();

                mLastStateChangeMillis = System.currentTimeMillis();
                mNoiseWriter.makeYesNoise();
                mCurrentState = State.MAKING_NOISE_REFLECTING;
                Log.d(TAG, "state := MAKING_NOISE_REFLECTING");
            }

            // if scanning for a while, go back to searching, but let LOOKER know
            if(System.currentTimeMillis()-mLastStateChangeMillis > TIMEOUT_SCANNING){
                mLastStateChangeMillis = System.currentTimeMillis();
                mNoiseWriter.makeNoNoise();
                mCurrentState = State.MAKING_NOISE_REFLECTING;
                Log.d(TAG, "state := MAKING_NOISE_REFLECTING");
            }
        }
        else if(mCurrentState == State.REFLECTING){
            // do nothing to image

            // if reflecting for a while, go back to searching
            if(System.currentTimeMillis()-mLastStateChangeMillis > TIMEOUT_REFLECTING){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SEARCHING;
                Log.d(TAG, "state := SEARCHING");
                sendCommandToPlatform("search").start();
            }
        }
        else if(mCurrentState == State.MAKING_NOISE_LOOKING){
            mTempRgba.setTo(SCREEN_COLOR_FLASH);

            if((System.currentTimeMillis()-mLastStateChangeMillis) > PERIOD_MAKING_NOISE_LOOKING){
                mNoiseWriter.stopNoise();
                mTempRgba.setTo(SCREEN_COLOR_BLACK);

                if(mNoiseReader.isHearingYes()){
                    mLastStateChangeMillis = System.currentTimeMillis();
                    mCurrentState = State.SCANNING_LOOKING;
                    Log.d(TAG, "state := SCANNING_LOOKING");
                    sendCommandToPlatform("scan").start();
                }
                else if(mNoiseReader.isHearingNo()){
                    mLastStateChangeMillis = System.currentTimeMillis();
                    mCurrentState = State.SEARCHING;
                    Log.d(TAG, "state := SEARCHING");
                    sendCommandToPlatform("search").start();
                }
            }

            // if waiting for a while, go back to searching
            if(System.currentTimeMillis()-mLastStateChangeMillis > TIMEOUT_MAKING_NOISE_LOOKING){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SEARCHING;
                Log.d(TAG, "state := SEARCHING");
                sendCommandToPlatform("search").start();
            }
        }
        else if(mCurrentState == State.MAKING_NOISE_REFLECTING){
            mTempRgba.setTo(SCREEN_COLOR_BLACK);

            // if making noise for a while, move on
            if(System.currentTimeMillis()-mLastStateChangeMillis > TIMEOUT_MAKING_NOISE_REFLECTING){
                mNoiseWriter.stopNoise();
                if(mNoiseWriter.isMakingYesNoise()){
                    mLastStateChangeMillis = System.currentTimeMillis();
                    mCurrentState = State.REFLECTING;
                    Log.d(TAG, "state := REFLECTING");
                }
                else{
                    mLastStateChangeMillis = System.currentTimeMillis();
                    mCurrentState = State.SEARCHING;
                    Log.d(TAG, "state := SEARCHING");
                    sendCommandToPlatform("search").start();
                }
            }
        }
        else if(mCurrentState == State.LOOKING){
            mTempRgba.setTo(SCREEN_COLOR_BLACK);

            if((System.currentTimeMillis()-mLastStateChangeMillis > 100) && (detectedArray.length > 0)){
                Log.d(TAG, "found something while LOOKING");

                // in mTempRgba coordinates!!!
                mCurrentFlashPosition = new Point(
                        mTempRgba.width()-detectedArray[0].tl().y-detectedArray[0].height/2,
                        detectedArray[0].br().x-detectedArray[0].width/2);

                mCurrentFlashColor = SCREEN_COLOR_FLASH;
                mCurrentFlashText = TEXTS[mRandomGenerator.nextInt(TEXTS.length)];
                mTempRgba.setTo(mCurrentFlashColor);
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.FLASHING;
                Log.d(TAG, "state := FLASHING");
                mNoiseWriter.stopNoise();
            }

            // if looking for more than 5 seconds, go back to searching
            if(System.currentTimeMillis()-mLastStateChangeMillis > 5000){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SEARCHING;
                Log.d(TAG, "state := SEARCHING");
                mNoiseWriter.stopNoise();
                sendCommandToPlatform("search").start();
            }
        }
        else if(mCurrentState == State.FLASHING){
            byte detectedColor[] = new byte[4];
            mTempRgba.get((int)mCurrentFlashPosition.y,(int)mCurrentFlashPosition.x,detectedColor);

            // checking these 2 values seem to be enough
            if(System.currentTimeMillis()-mLastStateChangeMillis > DELAY_FLASHING){
                if((detectedColor[1]&0xff)>200 && (detectedColor[2]&0xff)>200){
                    mLastStateChangeMillis = System.currentTimeMillis();
                    mCurrentState = State.POSTING;
                    Log.d(TAG, "state := POSTING");
                    mTempRgba.setTo(new Scalar(160, 160, 160, 255));
                }
            }
            else{
                mTempRgba.setTo(mCurrentFlashColor);
            }

            // if flashing for a while, go back to searching
            if(System.currentTimeMillis()-mLastStateChangeMillis > TIMEOUT_FLASHING){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SEARCHING;
                Log.d(TAG, "state := SEARCHING");
                sendCommandToPlatform("search").start();
            }

            Core.flip(mTempRgba.t(), mRgba, 1);
            for(int i=0; i<mRandomGenerator.nextInt(5)+2; i++){
                mCurrentFlashText = TEXTS[mRandomGenerator.nextInt(TEXTS.length)];
                Size mTextSize = Core.getTextSize(mCurrentFlashText, Core.FONT_HERSHEY_PLAIN, 1, 16, null);
                float mWidthScale = (float)(mRgba.width()/mTextSize.width);
                mTextSize = Core.getTextSize(mCurrentFlashText, Core.FONT_HERSHEY_PLAIN, mWidthScale, 16, null);
                Point mTextOrigin = new Point(
                        mRandomGenerator.nextInt((int)(mRgba.width() - mTextSize.width)),
                        mRandomGenerator.nextInt((int)(mRgba.height() - mTextSize.height)));
                Core.putText(mRgba, mCurrentFlashText, mTextOrigin, Core.FONT_HERSHEY_PLAIN, mWidthScale, SCREEN_COLOR_BLACK, 16);
            }
            Core.flip(mRgba.t(), mTempRgba, 1);
        }
        else if(mCurrentState == State.WAITING){
            mTempRgba.setTo(SCREEN_COLOR_BLACK);
            // if waiting for a while, go back to searching
            if(System.currentTimeMillis()-mLastStateChangeMillis > TIMEOUT_WAITING){
                mLastStateChangeMillis = System.currentTimeMillis();
                mCurrentState = State.SEARCHING;
                Log.d(TAG, "state := SEARCHING");
                sendCommandToPlatform("search").start();
            }
        }
        else if(mCurrentState == State.POSTING){
            Core.flip(mTempRgba.t(), mRgba, 0);
            Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_BGR2RGB);

            final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SELFIE_FILE_NAME);
            Highgui.imwrite(file.toString(), mRgba);

            Thread tumblrThread = new Thread(new Runnable(){
                @Override
                public void run() {
                    try{
                        PhotoPost mPP = mTumblrClient.newPost("memememeselfie.tumblr.com", PhotoPost.class);
                        mPP.setData(file);
                        mPP.save();
                    }
                    catch(IOException e){
                        Log.e(TAG, "IO Exception!!: while sending picture to tumblr.");
                    }
                    catch(JumblrException e){
                        Log.e(TAG, "Jumblr Exception!!: while sending picture to tumblr.");
                        Log.e(TAG, "Response Code: "+e.getResponseCode());
                        Log.e(TAG, e.toString());
                    }
                    catch(Exception e){
                        Log.e(TAG, "some Exception!!: while sending picture to tumblr.");
                        Log.e(TAG, e.toString());
                    }
                }
            });
            tumblrThread.start();

            mLastStateChangeMillis = System.currentTimeMillis();
            mCurrentState = State.SEARCHING;
            Log.d(TAG, "state := SEARCHING");
            sendCommandToPlatform("search").start();
            mTempRgba.setTo(SCREEN_COLOR_BLACK);
        }

        Core.flip(mTempRgba, mRgba, 1);
        return mRgba;
    }
}
