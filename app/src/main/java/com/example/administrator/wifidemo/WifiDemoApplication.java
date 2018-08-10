package com.example.administrator.wifidemo;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.example.administrator.wifidemo.remoteservice.RemoteControlService;

public class WifiDemoApplication extends Application {

    private static WifiDemoApplication INSTANCE;

    /**
     * 广播ACTION
     */
    public static final String START_REMOTE_SERVICE_ACTION = "com.satcatche.satv.REMOTE_CONTROL";

    public static WifiDemoApplication getInstance() {
        return INSTANCE;
    }

    @Override
    public void onCreate() {
        INSTANCE = this;
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(startRemoteControlServiceReceiver,
                new IntentFilter(START_REMOTE_SERVICE_ACTION));
        startRemoteControlService();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(startRemoteControlServiceReceiver);
        stopRemoteControlService();
    }

    /**
     * 启动蓝牙遥控服务
     */
    private void startRemoteControlService() {
        RemoteControlService.startService(getApplicationContext());
    }

    /**
     * 停止蓝牙遥控服务
     */
    private void stopRemoteControlService() {
        RemoteControlService.stopService(getApplicationContext());
    }

    /**
     * 蓝牙遥控服务意外停止后再启动
     */
    private BroadcastReceiver startRemoteControlServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startRemoteControlService();
        }
    };
}
