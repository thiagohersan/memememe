package memememe.me.selfiememememe;

import android.app.Application;
import android.content.Context;

public class MemememeApp extends Application {

    public static MemememeApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public static MemememeApp getInstance() {
        return instance;
    }
}