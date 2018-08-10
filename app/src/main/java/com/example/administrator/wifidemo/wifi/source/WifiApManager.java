package com.example.administrator.wifidemo.wifi.source;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import com.example.administrator.wifidemo.WifiDemoApplication;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class WifiApManager {
    private static final String TAG = "liwei";

    public static final int NO_PASS = 0;
    public static final int WEP_PSK = 1;
    public static final int WPA_PSK = 2;
    public static final int WPA2_PSK = 3;

    private static WifiApManager INSTANCE;

    private WifiManager wifiManager;
    private WifiConfiguration apConfig;
    private Context mContext;

    public static synchronized WifiApManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WifiApManager();
        }
        return INSTANCE;
    }

    @SuppressLint("WifiManagerPotentialLeak")
    private WifiApManager() {
        mContext = WifiDemoApplication.getInstance().getApplicationContext();
        wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        mWifiInfo = wifiManager.getConnectionInfo();
    }

    //开启热点
    public boolean openwifiap(String name, String password, int type) {
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);//如果WiFi是开启的就关闭WiFi。
        }
        apConfig = new WifiConfiguration();
        apConfig.SSID = name;//设置WiFi名字

        //热点相关设置
        switch (type) {
            case NO_PASS:
                apConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                apConfig.wepKeys[0] = "";
                apConfig.wepTxKeyIndex = 0;
                break;
            case WPA_PSK:
                apConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                apConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                apConfig.preSharedKey = password;
                break;
            case WPA2_PSK:
                //由于wpa2是不能直接访问的，但是KeyMgmt中却有。所以我们这样写
                for (int i = 0; i < WifiConfiguration.KeyMgmt.strings.length; i++) {
                    if ("WPA2_PSK".equals(WifiConfiguration.KeyMgmt.strings[i])) {
                        apConfig.allowedKeyManagement.set(i);//直接给它赋索引的值
                        Log.e("wpa2索引", String.valueOf(i));//不同手机索引不同
                    }
                }
                apConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                apConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                apConfig.preSharedKey = password;
                break;
        }

        try {
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return (boolean) method.invoke(wifiManager, apConfig, true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    //关闭热点
    public void closewifiap() {
        try {
//            Method method1=wifiManager.getClass().getMethod("getWifiapConfiguration");
//            method1.setAccessible(true);
//            WifiConfiguration nowconfig= (WifiConfiguration) method1.invoke(wifiManager);//获取当前热点
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiManager, apConfig, false);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    //获取连接列表
    public StringBuffer getconnectlist() {
        /*
        连接设备的信息都存在一个文件里面，读这个文件获取信息
        读取文件后每为这样的格式，每连接一个设备增加一行，没有连接时只有一行
        IP address       HW type     Flags       HW address            Mask     Device
        192.168.43.115   0x1         0x2         c4:0b:cb:8a:4c:f1     *        ap0
        192.168.43.115   0x1         0x2         c4:0b:cb:8a:4c:f1     *        ap0
         */
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                Log.e("连接列表", line);
                sb.append(line + "\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb;
    }

    public String getServerIp() {
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        int ip = dhcpInfo.serverAddress;
        //整形转化为IP地址
        return ipIntToString(ip);
    }

    // 得到IP地址
    public String getIPAddress() {
        return ipIntToString((mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress());
    }

    //整形转化为IP地址
    private String ipIntToString(int ip) {
        String sip = (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." + ((ip >> 16) & 0xff) + "." + ((ip >> 24) & 0xff);
        Log.e("服务器IP", sip);
        return sip;
    }

    /**
     *
     */

    // 定义WifiInfo对象    
    private WifiInfo mWifiInfo;
    // 扫描出的网络连接列表    
    private List<ScanResult> mWifiList;
    // 网络连接列表    
    private List<WifiConfiguration> mWifiConfiguration;
    // 定义一个WifiLock    
    WifiManager.WifiLock mWifiLock;

/*    // 打开WIFI
    public void openWifi() {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }*/

    // 关闭WIFI    
    public void closeWifi() {
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
    }

    //当前WiFi是否开启
    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    // 检查当前WIFI状态    
    public int checkState() {
        return wifiManager.getWifiState();
    }

    // 锁定WifiLock    
    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    // 解锁WifiLock    
    public void releaseWifiLock() {
        // 判断时候锁定    
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    // 创建一个WifiLock    
    public void creatWifiLock() {
        mWifiLock = wifiManager.createWifiLock("Test");
    }

    // 得到配置好的网络    
    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfiguration;
    }

    // 指定配置好的网络进行连接    
    public void connectConfiguration(int index) {
        // 索引大于配置好的网络索引返回    
        if (index > mWifiConfiguration.size()) {
            return;
        }
        // 连接配置好的指定ID的网络    
        wifiManager.enableNetwork(mWifiConfiguration.get(index).networkId,
                true);
    }

    public void startScan() {
        wifiManager.startScan();
        // 得到扫描结果    
        mWifiList = wifiManager.getScanResults();
        // 得到配置好的网络连接    
        mWifiConfiguration = wifiManager.getConfiguredNetworks();
    }

    // 得到网络列表    
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    // 查看扫描结果    
    public StringBuilder lookUpScan() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mWifiList.size(); i++) {
            stringBuilder
                    .append("Index_" + new Integer(i + 1).toString() + ":");
            // 将ScanResult信息转换成一个字符串包    
            // 其中把包括：BSSID、SSID、capabilities、frequency、level    
            stringBuilder.append((mWifiList.get(i)).toString());
            stringBuilder.append("/n");
        }
        return stringBuilder;
    }

    // 得到MAC地址    
    public String getMacAddress() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
    }

    // 得到接入点的BSSID    
    public String getBSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
    }

    // 得到连接的ID    
    public int getNetworkId() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }

    // 得到WifiInfo的所有信息包    
    public String getWifiInfo() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
    }

    // 添加一个网络并连接    
    public void addNetwork(WifiConfiguration wcg) {
        int wcgID = wifiManager.addNetwork(wcg);
        boolean b = wifiManager.enableNetwork(wcgID, true);
        System.out.println("a--" + wcgID);
        System.out.println("b--" + b);
        boolean connected = wifiManager.reconnect();
    }

    // 断开指定ID的网络    
    public void disconnectWifi(int netId) {
        wifiManager.disableNetwork(netId);
        wifiManager.disconnect();
    }

//然后是一个实际应用方法，只验证过没有密码的情况：  

    public WifiConfiguration createWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            wifiManager.removeNetwork(tempConfig.networkId);
        }

        if (Type == NO_PASS) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            //以下需注释掉，否则连不上
            //config.wepKeys[0] = "";
            //config.wepTxKeyIndex = 0;
        }
        if (Type == WEP_PSK) {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WPA_PSK) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);    
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    /**
     *
     */

    // 定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }

    // 提供一个外部接口，传入要连接的无线网
    public void connect(String ssid, String password, WifiCipherType type) {
        //WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
        WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);

//
        if (wifiConfig == null) {
            Log.d(TAG, "WifiApManager wifiConfig is null!");
            return;
        }

        WifiConfiguration tempConfig = isExsits(ssid);

        if (tempConfig != null) {
            wifiManager.removeNetwork(tempConfig.networkId);
        }

        int netID = wifiManager.addNetwork(wifiConfig);
        Log.d(TAG, "WifiApManager connect: netID = " + netID);
        boolean enabled = wifiManager.enableNetwork(netID, true);
        Log.d(TAG, "WifiApManager enableNetwork status enable=" + enabled);
        boolean connected = wifiManager.reconnect();
        Log.d(TAG, "WifiApManager enableNetwork connected=" + connected);
    }

    // 查看以前是否也配置过这个网络
    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs != null && existingConfigs.size() > 0) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;
                }
            }
        }
        return null;
    }

    private WifiConfiguration createWifiInfo(String SSID, String Password, WifiCipherType Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
// nopass
        if (Type == WifiCipherType.WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
// wep
        if (Type == WifiCipherType.WIFICIPHER_WEP) {
            if (!TextUtils.isEmpty(Password)) {
                if (isHexWepKey(Password)) {
                    config.wepKeys[0] = Password;
                } else {
                    config.wepKeys[0] = "\"" + Password + "\"";
                }
            }
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
// wpa
        if (Type == WifiCipherType.WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
// 此处需要修改否则不能自动重联
// config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    // 打开wifi功能
    public boolean openWifi() {
        boolean bRet = true;
        if (!wifiManager.isWifiEnabled()) {
            bRet = wifiManager.setWifiEnabled(true);
        }
        return bRet;
    }

    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();

// WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }

        return isHex(wepKey);
    }

    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查wifi是否处开连接状态
     *
     * @return
     */
    public boolean isWifiConnect() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo mWifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            Log.d(TAG, "WifiApManager isWifiConnect: " + mWifiInfo.isConnected());
            return mWifiInfo.isConnected();
        }
        Log.d(TAG, "WifiApManager isWifiConnect: false");
        return false;
    }

    /**
     * 检查wifi热点是否打开
     *
     * @return
     */
    @SuppressLint("WifiManagerPotentialLeak")
    public boolean isApEnabled() {
        int state;
         WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            state = (Integer) method.invoke(wifiManager);
        } catch (Exception e) {
            state = 11;   //WIFI_AP_STATE_DISABLED
        }
        return 12 == state || 13 == state;  //WIFI_AP_STATE_ENABLING, WIFI_AP_STATE_ENABLED
    }
}
