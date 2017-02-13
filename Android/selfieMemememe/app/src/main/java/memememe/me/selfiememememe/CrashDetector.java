package memememe.me.selfiememememe;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class CrashDetector implements Thread.UncaughtExceptionHandler {

    private Activity activity;
    private static final String OSC_OUT_ADDRESS = "10.10.0.1";
    private static final int OSC_OUT_PORT = 8888;
    private OSCPortOut mOscOut;
    private static final String TAG = "MEMEMEME::CRASHED";

    public CrashDetector(Activity a) {
        activity = a;

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
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Intent intent = new Intent(activity, MemememeActivity.class);
        intent.putExtra("crash", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(MemememeApp.getInstance().getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

        AlarmManager mgr = (AlarmManager) MemememeApp.getInstance().getBaseContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 180000, pendingIntent);


        final String theCommand = "/memememe/stop";
        Thread oscthread = new Thread(new Runnable(){
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
        activity.finish();
        System.exit(2);
    }
}
