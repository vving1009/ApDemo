package com.example.administrator.wifidemo;

import android.app.Application;

public class WifiDemoApplication extends Application {

    private static WifiDemoApplication INSTANCE;

    public static WifiDemoApplication getInstance() {
        return INSTANCE;
    }

    @Override
    public void onCreate() {
        INSTANCE = this;
        super.onCreate();
    }
}
