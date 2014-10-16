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
import android.widget.ToggleButton;

public class SoundActivity extends Activity {
	// TAG is used to debug in Android logcat console
	private static final String TAG = "!!!SOUND!!! ";

	private AudioTrack mAudioTrack = null;
	private AudioRecord mAudioRecord = null;
	private ToggleButton soundButton = null;
	private Thread audioWriteThread = null;
	private Thread audioReadThread = null;
	private Random mRandom = null;

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
	        canvas.drawColor(Color.BLACK);
	        paint.setColor(Color.WHITE);
	        for(int x=0; x<mData.length; x++){
	            canvas.drawLine(x+20, 700, x+20, 700-mData[x], paint);
	        }
	    }
	}
	DrawView mDrawView;
	/**/

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mRandom = new Random();
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 8192, AudioTrack.MODE_STREAM);
		mAudioRecord = new AudioRecord(AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 8192);

		// GUI
		LinearLayout mainLayout = (LinearLayout) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.main, null);
        /**/
		mDrawView = new DrawView(this);
        mDrawView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mainLayout.addView(mDrawView);
		/**/
		setContentView(mainLayout);

        soundButton = (ToggleButton) findViewById(R.id.soundButton);

        // play audio
		audioWriteThread = new Thread(new Runnable(){
            @Override
            public void run() {
                boolean bRun = true;
                byte[] mData = new byte[512];
                long runningSample = 0L;
                long lastChangeMillis = System.currentTimeMillis();
                float cFreq = 1009.0f;
                float lowFreqK = (float)(2.0*Math.PI*cFreq/44100.0);
                float highFreqK = (float)(2.0*Math.PI*(cFreq+503.0)/44100.0);

                while(bRun){
                    try{
                        if(soundButton.isChecked()){
                            if(System.currentTimeMillis()-lastChangeMillis > 100){
                                cFreq = mRandom.nextFloat()*1499.0f+1009.0f;
                                lowFreqK = (float)(2.0*Math.PI*cFreq/44100.0);
                                highFreqK = (float)(2.0*Math.PI*(cFreq+503.0)/44100.0);
                                lastChangeMillis = System.currentTimeMillis();
                            }

                            for(int i=0; i<mData.length/2; i++,runningSample++){
                                double sinSum = Math.sin(lowFreqK*runningSample) + Math.sin(highFreqK*runningSample);
                                mData[2*i] = (byte)(63.5*sinSum);
                                mData[2*i+1] = (byte)(63.5*sinSum);
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
		    private int[] getTwoHighestIndexes(float[] f){
		        float[] maxVals = {0.0f,0.0f};
		        int[] maxIndex = {0,0};
		        for(int i=1; i<f.length/4; i++){
		            float magSq = f[2*i]*f[2*i] + f[2*i+1]*f[2*i+1];
		            if(magSq > maxVals[0]){
		                maxVals[1] = maxVals[0];
		                maxIndex[1] = maxIndex[0];
		                maxVals[0] = magSq;
		                maxIndex[0] = i;
		            }
		            else if(magSq > maxVals[1]){
                        maxVals[1] = magSq;
                        maxIndex[1] = i;
		            }
		        }
		        return maxIndex;
		    }
            @Override
            public void run() {
                boolean bRun = true;
                byte[] mData = new byte[1024];
                float[] mDataFloat = new float[mData.length];
                FloatFFT_1D fft = new FloatFFT_1D(mDataFloat.length);
                int[] diffs = new int[8];
                int currentDiffIndex = 0;
                int sumDiffs = 0;
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

                        int[] maxFreqIndexes = getTwoHighestIndexes(mDataFloat);
                        int newDiff = 44100*Math.abs(maxFreqIndexes[0]-maxFreqIndexes[1])/512;
                        sumDiffs -= diffs[currentDiffIndex];
                        diffs[currentDiffIndex] = newDiff;
                        sumDiffs += diffs[currentDiffIndex];
                        currentDiffIndex = (currentDiffIndex+1)%diffs.length;
                        
                        Log.d(TAG, "Freq Diff = "+sumDiffs/diffs.length+" Hz");

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

	public void playSound(View v){
	    if(soundButton.isChecked()){}
	    else{}
	}

	public void quitActivity(View v){
		finish();
	}
}
