package com.example.administrator.wifidemo.remoteservice;

import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.administrator.wifidemo.WifiDemoApplication;
import com.example.administrator.wifidemo.wifi.WifiActivity;
import com.example.administrator.wifidemo.wifi.source.WifiApManager;

/**
 * RemoteControlService
 *
 * @author 贾博瑄
 */

public class RemoteControlService extends Service {

    private static final String TAG = "liwei";

    /**
     * Member object for the chat services
     */
    private UdpService mUdpService = null;

    private TcpService mTcpService = null;

    private Instrumentation instrumentation;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, RemoteControlService.class);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, RemoteControlService.class);
        context.stopService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RemoteControlService onCreate: ");
        instrumentation = new Instrumentation();
        if (!WifiApManager.getInstance().isWifiConnect()) {
            Intent intent = new Intent(this, WifiActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        //initTcpSocket();
    }

    private void initTcpSocket() {

        if (mTcpService == null) {
            // Initialize the BluetoothChatService to perform bluetooth connections
            mTcpService = new TcpService(this);
        }

        if (mTcpService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mTcpService.getState() == mTcpService.STATE_NONE) {
                // Start the Bluetooth chat services
                mTcpService.setReceiveMessageListener(message -> {
                    if (instrumentation == null) {
                        instrumentation = new Instrumentation();
                    }
                    //通过KeyCode响应相应操作
                    Log.d(TAG, "instrumentation.sendKeyDownUpSync: " + message);
                    instrumentation.sendKeyDownUpSync(Integer.parseInt(message));
                });
                mTcpService.start();
            }
        }
        registerWifiReceiver();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "RemoteControlService onDestroy: ");
        if (mTcpService != null) {
            mTcpService.stop();
        }
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(WifiDemoApplication.START_REMOTE_SERVICE_ACTION));
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWifiReceiver);
    }

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //监听wifi的连接状态即是否连接的一个有效的无线路由
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())){
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (parcelableExtra != null){
                    // 获取联网状态的NetWorkInfo对象
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    //获取的State对象则代表着连接成功与否等状态
                    NetworkInfo.State state = networkInfo.getState();
                    //判断网络是否已经连接
                    Log.d(TAG, "onReceive: wifi state = " + state);
                    if (state != NetworkInfo.State.CONNECTED) {
                        WifiActivity.startActivity(RemoteControlService.this);
                    } else {

                    }
                }
            }
        }
    };

    private void registerWifiReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mWifiReceiver, intentFilter);
    }
}
