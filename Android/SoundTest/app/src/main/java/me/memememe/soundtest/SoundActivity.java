package me.memememe.soundtest;

import java.lang.Math;
import java.util.Random;

import org.jtransforms.fft.FloatFFT_1D;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

public class SoundActivity extends Activity {
    // TAG is used to debug in Android logcat console
    private static final String TAG = "!!!SOUND!!! ";

    private AudioTrack mAudioTrack = null;
    private AudioRecord mAudioRecord = null;
    private Thread audioWriteThread = null;
    private Thread audioReadThread = null;
    private Random mRandom = null;
    private float[] mTones = {0,0,0,0};
    private int toneIndex = 0;
    private boolean playSound = false;
    private int currentBackgroundColor = Color.BLACK;
    private long lastBackgroundColorChangeMillis = System.currentTimeMillis();
    private long lastToneChangeMillis = System.currentTimeMillis();

    /**/
    public class DrawView extends View {
        Paint paint = new Paint();
        float[] mData;

        public DrawView(Context context) {
            super(context);
        }

        public synchronized void setData(float[] d){
            mData = new float[d.length/2];
            for(int i=0; i<d.length/2; i++){
                mData[i] = (float)Math.sqrt(d[2*i]*d[2*i] + d[2*i+1]*d[2*i+1])*0.05f;
            }
        }

        @Override
        public synchronized void onDraw(Canvas canvas) {
            canvas.drawColor(currentBackgroundColor);
            paint.setColor(Color.WHITE);
            for(int x=0; x<mData.length; x++){
                canvas.drawLine(x+20, 700, x+20, 700-mData[x], paint);
            }
        }
    }
    DrawView mDrawView;
	/**/

    public void pressedYes(View v){
        for(int i=0; i<mTones.length; i++){
            mTones[i] = 11000.0f + i*300.0f + mRandom.nextFloat()*100.0f;
        }
        toneIndex = 0;
        playSound = true;
    }
    public void pressedNo(View v){
        for(int i=0; i<mTones.length; i++){
            mTones[i] = 13000.0f - i*300.0f - mRandom.nextFloat()*100.0f;
        }
        toneIndex = 0;
        playSound = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRandom = new Random();
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 8192, AudioTrack.MODE_STREAM);
        mAudioRecord = new AudioRecord(AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 8192);

        // GUI
        LinearLayout mainLayout = (LinearLayout) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.activity_sound, null);
        /**/
        mDrawView = new DrawView(this);
        mDrawView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mainLayout.addView(mDrawView);
		/**/
        setContentView(mainLayout);

        // play audio
        audioWriteThread = new Thread(new Runnable(){
            @Override
            public void run() {
                boolean bRun = true;
                byte[] mData = new byte[512];
                long runningSample = 0L;
                float freqK = 0.0f;

                while(bRun){
                    try{
                        if(playSound){
                            if(System.currentTimeMillis()-lastToneChangeMillis > 25){
                                freqK = (float)(2.0*Math.PI*mTones[toneIndex%mTones.length]/44100.0);
                                lastToneChangeMillis = System.currentTimeMillis();
                                toneIndex++;
                                if(toneIndex > 8*mTones.length){
                                    playSound = false;
                                }
                            }

                            for(int i=0; i<mData.length/2; i++,runningSample++){
                                double sinSum = Math.sin(freqK*runningSample);
                                mData[2*i] = (byte)(127.0*sinSum);
                                mData[2*i+1] = (byte)(127.0*sinSum);
                            }
                            if(runningSample > (Long.MAX_VALUE-2)){
                                runningSample = 0L;
                            }
                        }
                        else{
                            for(int i=0; i<mData.length; i++){
                                mData[i] = (short)0;
                            }
                        }

                        mAudioTrack.write(mData, 0, mData.length);
                        bRun = !(Thread.currentThread().isInterrupted());
                        Thread.sleep(0,100);
                    }
                    catch(InterruptedException e){
                        Log.e(TAG, "AudioWriteThread Interrupted ");
                    }
                    /* catch(IOException e){} */
                }
            }
        });
        audioWriteThread.start();
        mAudioTrack.play();

        // read audio
        audioReadThread = new Thread(new Runnable(){
            int maxFreq = 0;
            int maxMag = 0;
            private void setHighestIndexAndMagnitude(float[] f){
                float maxVal = 0.0f;
                maxFreq = 0;
                maxMag = 0;
                for(int i=f.length/8; i<f.length/6; i++){
                    float magSq = f[2*i]*f[2*i] + f[2*i+1]*f[2*i+1];
                    if(magSq > maxVal){
                        maxVal = magSq;
                        maxFreq = i;
                    }
                }
                maxMag = ((int)Math.sqrt(maxVal))/1000;
            }
            @Override
            public void run() {
                boolean bRun = true;
                byte[] mData = new byte[1024];
                float[] mDataFloat = new float[mData.length];
                FloatFFT_1D fft = new FloatFFT_1D(mDataFloat.length);
                int lastFreq = 0;
                int[] lastFreqs = {0,0,0,0};
                while(bRun){
                    try{
                        mAudioRecord.read(mData, 0, mData.length);
                        for(int i=0; i<mDataFloat.length; i++){
                            mDataFloat[i] = (float)mData[i];
                        }
                        fft.realForward(mDataFloat);

                        /**/
                        mDrawView.setData(mDataFloat);
                        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mDrawView.invalidate();
                            }
                        });
                        /**/

                        setHighestIndexAndMagnitude(mDataFloat);
                        // ignore and clear buffer
                        if(maxMag < 6){
                            for(int i=0; i<lastFreqs.length; i++){
                                lastFreqs[i] = 0;
                            }
                        }
                        // read into buffer
                        else if(maxFreq != lastFreq){
                            lastFreq = maxFreq;
                            for(int i=1; i<lastFreqs.length; i++){
                                lastFreqs[i-1] = lastFreqs[i];
                            }
                            lastFreqs[lastFreqs.length-1] = maxFreq;
                        }

                        // check what's up
                        int deltaFreq = 0;
                        for(int i=1; i<lastFreqs.length; i++){
                            if(lastFreqs[i] > lastFreqs[i-1]) deltaFreq++;
                            else if(lastFreqs[i] < lastFreqs[i-1]) deltaFreq--;
                        }

                        if(deltaFreq > lastFreqs.length-2){
                            Log.d(TAG, "YESSS");
                            if(System.currentTimeMillis()-lastToneChangeMillis > 500){
                                currentBackgroundColor = Color.rgb(12,116,12);
                                lastBackgroundColorChangeMillis = System.currentTimeMillis();
                            }
                        }
                        else if(-deltaFreq > lastFreqs.length-2){
                            Log.d(TAG, "NOOOO");
                            if(System.currentTimeMillis()-lastToneChangeMillis > 500){
                                currentBackgroundColor = Color.rgb(116,12,12);
                                lastBackgroundColorChangeMillis = System.currentTimeMillis();
                            }
                        }
                        else if(System.currentTimeMillis()-lastBackgroundColorChangeMillis > 800){
                            currentBackgroundColor = Color.BLACK;
                            lastBackgroundColorChangeMillis = System.currentTimeMillis();
                        }

                        bRun = !(Thread.currentThread().isInterrupted());
                        Thread.sleep(0, 100);
                    }
                    catch(InterruptedException e){
                        Log.e(TAG, "AudioReadThread Interrupted ");
                    }
                    /* catch(IOException e){} */
                }
            }
        });
        audioReadThread.start();
        mAudioRecord.startRecording();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void quitActivity(View v){
        finish();
    }
}
