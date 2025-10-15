package com.jmas.supervisor;

import android.app.Application;
import android.content.Context;

public class SupervisorApp extends Application {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return appContext;
    }
}
