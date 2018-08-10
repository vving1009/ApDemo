package com.example.administrator.wifidemo.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.administrator.wifidemo.BaseFragment;
import com.example.administrator.wifidemo.R;
import com.example.administrator.wifidemo.remoteservice.TcpService;
import com.example.administrator.wifidemo.remoteservice.UdpService;
import com.example.administrator.wifidemo.remoteservice.data.WifiSsid;
import com.example.administrator.wifidemo.wifi.source.WifiApManager;
import com.example.administrator.wifidemo.wifi.source.WifiRepository;

/**
 * WifiFragment
 *
 * @author 贾博瑄
 */

public class WifiFragment extends BaseFragment implements WifiContract.View {

    private static final String TAG = "liwei";

    private final String SSID = "MILE_TV";

    private final String PASSWORD = "SATCATCHE";

    private WifiContract.Presenter mPresenter;
    private TcpService mTcpService;
    private UdpService mUdpService;
    private WifiApManager mWifiApManager;
    private WifiSsid mWifiSsid;

    public static WifiFragment newInstance() {
        Bundle args = new Bundle();
        WifiFragment fragment = new WifiFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiApManager = WifiApManager.getInstance();
        mTcpService = new TcpService(getContext());
        mTcpService.setReceiveMessageListener(msg -> {
            //WifiSsid wifiSsid = new Gson().fromJson(msg, WifiSsid.class);
            Log.d(TAG, "TcpService ReceiveMessage: " + msg);
            mWifiSsid = new WifiSsid();
            mWifiSsid.setSsid("zeacho_work_5g");
            mWifiSsid.setPassword(msg);
            /*mTcpService.write("received wifi ssid.".getBytes());
            new Handler().postDelayed(() -> {
                mTcpService.stop();
                mWifiApManager.closewifiap();
            }, 1000);*/
            mTcpService.stop();
            mWifiApManager.closewifiap();
            /*if (mWifiApManager.checkState() == WifiManager.WIFI_STATE_ENABLED) {
                connectWifi();
            } else {
                mWifiApManager.openWifi();
            }*/
        });
        if (!WifiApManager.getInstance().isApEnabled()) {
            startWifiAp();
        }
        registerWifiReceiver();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "WifiFragment onCreateView: ");
        View root = inflater.inflate(R.layout.wifi_fragment, container, false);
        mPresenter = new WifiPresenter(this, new WifiRepository());
        mPresenter.subscribe();
        return root;
    }

    private void connectWifi() {
        if (mWifiSsid != null) {
            Log.d(TAG, "connectWifi: ssid=" + mWifiSsid.getSsid() + ", password=" + mWifiSsid.getPassword());
            mWifiApManager.connect(mWifiSsid.getSsid(), mWifiSsid.getPassword(), WifiApManager.WifiCipherType.WIFICIPHER_WPA);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.unsubscribe();
        getContext().unregisterReceiver(mWifiReceiver);
    }

    public void startWifiAp() {
        if (mWifiApManager.openwifiap(SSID, PASSWORD, WifiApManager.WPA_PSK)) {
            Log.d(TAG, "startWifiAp: 热点已启动");
            //mTcpService.start();
        } else {
            Toast.makeText(getContext(), "server 端失败，请重试", Toast.LENGTH_LONG).show();
        }
    }

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {

        boolean apDisabling = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "WifiFragment onReceive: " + intent.getAction());
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())){
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED); // WifiManager.WIFI_AP_STATE_DISABLED = 11
                switch (state) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.d(TAG, "WIFI_STATE_ENABLED");
                        connectWifi();
                        break;
                }
            }
            //监听wifi热点开启关闭状态
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())) {
                int state = intent.getIntExtra("wifi_state", 11); // WifiManager.WIFI_AP_STATE_DISABLED = 11
                Log.d(TAG, "onReceive: WIFI_AP_STATE =" + state);
                switch (state) {
                    case 10: // WifiManager.WIFI_AP_STATE_DISABLING = 10
                        Log.d(TAG, "WIFI_AP_STATE_DISABLING");
                        apDisabling = true;
                        break;
                    case 11: // WifiManager.WIFI_AP_STATE_DISABLED = 11
                        Log.d(TAG, "WIFI_AP_STATE_DISABLED");
                        if (apDisabling) {
                            if (mWifiApManager.openWifi()) {
                                connectWifi();
                            }
                        }
                        break;
                    case 13: // WifiManager.WIFI_AP_STATE_ENABLED = 13
                        Log.d(TAG, "WIFI_AP_STATE_ENABLED");
                        apDisabling = false;
                        mTcpService.start();
                        break;
                    case 14: // WifiManager.WIFI_AP_STATE_FAILED = 14
                        Log.d(TAG, "WIFI_AP_STATE_FAILED");
                        break;
                }
            }
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
                    if (state == NetworkInfo.State.CONNECTED) {
                        getActivity().finish();
                    }
                }
            }
            // 监听是否成功连上wifi
            if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(intent.getAction())){
                Log.d(TAG, "SUPPLICANT_CONNECTION_CHANGE_ACTION: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
                if (!intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    startWifiAp();
                }
            }
            // 监听是否成功连上wifi
            if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction())){
                SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                Log.d(TAG, "SUPPLICANT_STATE_CHANGED_ACTION: " + state);
            }
        }
    };

    private void registerWifiReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");

        intentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        intentFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        intentFilter.addAction("android.net.wifi.BATCHED_RESULTS");
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        getContext().registerReceiver(mWifiReceiver, intentFilter);
    }
}
