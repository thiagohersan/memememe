package memememe.me.selfiememememe;

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
    private final float SAMPLE_RATE = 44100.0f;

    boolean bRun = true;

    AudioRecord mAudioRecord = null;
    byte[] mData = new byte[1024];
    float[] mDataFloat = new float[mData.length];
    FloatFFT_1D fft = new FloatFFT_1D(mDataFloat.length);

    // for finding max frequencies
    int maxFreq = 0;
    int maxMag = 0;
    int lastFreq = 0;
    int[] lastFreqs = {0,0,0,0};
    int deltaFreq;
    long lastReflectMillis, lastPictureMillis;

    public synchronized boolean isHearingReflect(){
        return (System.currentTimeMillis()-lastReflectMillis < 200);
    }
    public synchronized boolean isHearingPicture(){
        return (System.currentTimeMillis()-lastPictureMillis < 200);
    }

    private synchronized void processFFTResults(){
        float maxMagF = 0.0f;
        maxFreq = 0;
        maxMag = 0;

        for(int i=mDataFloat.length/8; i<mDataFloat.length/6; i++){
            float magSq = mDataFloat[2*i]*mDataFloat[2*i] + mDataFloat[2*i+1]*mDataFloat[2*i+1];
            if(magSq > maxMagF){
                maxMagF = magSq;
                maxFreq = i;
            }
        }
        maxMag = ((int)Math.sqrt(maxMagF))/1000;
    }

    @Override
    public void run(){
        mAudioRecord = new AudioRecord(AudioSource.MIC,
                (int)SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize((int)SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
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
                deltaFreq = 0;
                for(int i=1; i<lastFreqs.length; i++){
                    if(lastFreqs[i] > lastFreqs[i-1]) deltaFreq++;
                    else if(lastFreqs[i] < lastFreqs[i-1]) deltaFreq--;
                }

                if(deltaFreq > lastFreqs.length-2){
                    lastReflectMillis = System.currentTimeMillis();
                }
                if(-deltaFreq > lastFreqs.length-2){
                    lastPictureMillis = System.currentTimeMillis();
                }

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
    private float[] mTones = {0,0,0,0,0};
    private int toneIndex = 0;

    AudioTrack mAudioTrack = null;
    byte[] mData = new byte[1024];

    long runningSample = 0L;
    long lastChangeMillis = System.currentTimeMillis();
    float freqK = 0.0f;

    public synchronized void makeReflectNoise(){
        for(int i=0; i<mTones.length; i++){
            mTones[i] = FREQUENCY_MIN + i*FREQUENCY_TONE_DELTA + mRandom.nextFloat()*FREQUENCY_TONE_RANDOM;
        }
        toneIndex = 0;
        bMakeSomeNoise = true;
    }
    public synchronized void makePictureNoise(){
        for(int i=0; i<mTones.length; i++){
            mTones[i] = FREQUENCY_MAX - i*FREQUENCY_TONE_DELTA - mRandom.nextFloat()*FREQUENCY_TONE_RANDOM;
        }
        toneIndex = 0;
        bMakeSomeNoise = true;
    }
    public synchronized void stopNoise(){
        bMakeSomeNoise = false;
    }

    @Override
    public void run(){
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                (int)SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize((int)SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
                AudioTrack.MODE_STREAM);
        bRun = true;
        bMakeSomeNoise = false;
        mAudioTrack.play();

        while(bRun){
            try{
                if(bMakeSomeNoise){
                    if(System.currentTimeMillis()-lastChangeMillis > 25){
                        freqK = (float)(2.0*Math.PI*mTones[toneIndex%mTones.length]/SAMPLE_RATE);
                        lastChangeMillis = System.currentTimeMillis();
                        toneIndex++;
                        if(toneIndex > 10*mTones.length){
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
