package me.memememe.selfie;

import java.util.Arrays;
import java.util.Random;

import org.jtransforms.fft.FloatFFT_1D;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

class NoiseReader implements Runnable{
    private static final String TAG = "MEMEMEME::NOISEREADER";

    boolean bRun = true;

    AudioRecord mAudioRecord = null;
    byte[] mData = new byte[1024];
    float[] mDataFloat = new float[mData.length];
    FloatFFT_1D fft = new FloatFFT_1D(mDataFloat.length);

    // for finding max frequencies
    float[] maxFrequencyVals = {0.0f,0.0f};
    int[] maxFrequencyIndex = {0,0};

    // for running average
    int[] frequencyDiffs = new int[8];
    int currentDiffIndex = 0;
    int sumDiffs = 0;

    public synchronized int getFrequencyDifference(){
        return sumDiffs/frequencyDiffs.length;
    }

    private synchronized void processFFTResults(){
        Arrays.fill(maxFrequencyVals,0.0f);
        Arrays.fill(maxFrequencyIndex, 0);

        for(int i=1; i<mDataFloat.length/4; i++){
            float magSq = mDataFloat[2*i]*mDataFloat[2*i] + mDataFloat[2*i+1]*mDataFloat[2*i+1];
            if(magSq > maxFrequencyVals[0]){
                maxFrequencyVals[1] = maxFrequencyVals[0];
                maxFrequencyIndex[1] = maxFrequencyIndex[0];
                maxFrequencyVals[0] = magSq;
                maxFrequencyIndex[0] = i;
            }
            else if(magSq > maxFrequencyVals[1]){
                maxFrequencyVals[1] = magSq;
                maxFrequencyIndex[1] = i;
            }
        }
    }

    @Override
    public void run(){
        mAudioRecord = new AudioRecord(AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 8192);
        bRun = true;
        mAudioRecord.startRecording();

        while(bRun){
            try{
                mAudioRecord.read(mData, 0, mData.length);
                for(int i=0; i<mDataFloat.length; i++){
                    mDataFloat[i] = (float)mData[i];
                }
                fft.realForward(mDataFloat);
                processFFTResults();

                int newDiff = 44100*Math.abs(maxFrequencyIndex[0]-maxFrequencyIndex[1])/512;
                // update running average
                sumDiffs -= frequencyDiffs[currentDiffIndex];
                frequencyDiffs[currentDiffIndex] = newDiff;
                sumDiffs += frequencyDiffs[currentDiffIndex];
                currentDiffIndex = (currentDiffIndex+1)%frequencyDiffs.length;

                bRun = !(Thread.currentThread().isInterrupted());
                Thread.sleep(0, 100);
            }
            catch(InterruptedException e){
                Log.e(TAG, "Thread Interrupted ");
            }
        }
    }
}

class NoiseWriter implements Runnable{
    private static final String TAG = "MEMEMEME::NOISEWRITER";
    private final float FREQUENCY_MAX = 13000.0f;
    private final float FREQUENCY_MIN = 11000.0f;
    private final float FREQUENCY_TONE_DELTA = 300.0f;
    private final float FREQUENCY_TONE_RANDOM = 100.0f;
    private final float SAMPLE_RATE = 44100.0f;

    Random mRandom = new Random();

    boolean bRun = true;
    boolean bMakeSomeNoise = false;
    private float[] mTones = {0,0,0,0};
    private int toneIndex = 0;

    AudioTrack mAudioTrack = null;
    byte[] mData = new byte[1024];

    long runningSample = 0L;
    long lastChangeMillis = System.currentTimeMillis();
    float freqK = 0.0f;

    public synchronized void makeYesNoise(){
        for(int i=0; i<mTones.length; i++){
            mTones[i] = FREQUENCY_MIN + i*FREQUENCY_TONE_DELTA + mRandom.nextFloat()*FREQUENCY_TONE_RANDOM;
        }
        toneIndex = 0;
        bMakeSomeNoise = true;
    }
    public synchronized void makeNoNoise(){
        for(int i=0; i<mTones.length; i++){
            mTones[i] = FREQUENCY_MAX - i*FREQUENCY_TONE_DELTA - mRandom.nextFloat()*FREQUENCY_TONE_RANDOM;
        }
        toneIndex = 0;
        bMakeSomeNoise = true;
    }
    public synchronized void stopNoise(){
        bMakeSomeNoise = false;
    }
    public synchronized boolean isMakingYesNoise(){
        return (mTones[1] > mTones[0]);
    }

    @Override
    public void run(){
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, (int)SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 8192, AudioTrack.MODE_STREAM);
        bRun = true;
        bMakeSomeNoise = false;
        mAudioTrack.play();

        while(bRun){
            try{
                if(bMakeSomeNoise){
                    if(System.currentTimeMillis()-lastChangeMillis > 50){
                        freqK = (float)(2.0*Math.PI*mTones[toneIndex%mTones.length]/SAMPLE_RATE);
                        lastChangeMillis = System.currentTimeMillis();
                        toneIndex++;
                        if(toneIndex > 4*mTones.length){
                            bMakeSomeNoise = false;
                        }
                    }

                    for(int i=0; i<mData.length/2; i++,runningSample++){
                        double sinVal = Math.sin(freqK*runningSample);
                        mData[2*i] = (byte)(63.5*sinVal);
                        mData[2*i+1] = (byte)(63.5*sinVal);
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
                Log.e(TAG, "Thread Interrupted ");
            }
        }        
    }
}
