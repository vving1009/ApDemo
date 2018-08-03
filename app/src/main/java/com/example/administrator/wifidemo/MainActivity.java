package com.example.administrator.wifidemo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int SERVERPORT = 8191;

    @BindView(R.id.button)
    Button wifiApBtn;
    @BindView(R.id.button2)
    Button connWifiBtn;
    @BindView(R.id.button3)
    Button sendMessageBtn;
    @BindView(R.id.edit)
    EditText edit;

    private OutputStream out;

    private WifiApManager mWifiApManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mWifiApManager = WifiApManager.getInstance();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                String msg = "1234567890";
                /*try {
                    //发送消息
                    out.write(msg.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                WifiManager wifiManager = (WifiManager) MainActivity.this.getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                Log.d("wifiInfo", wifiInfo.toString());
                Log.d("SSID", wifiInfo.getSSID());
            }
        });
        IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(apReceiver, intentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.button)
    public void onWifiApBtnClicked() {
        if (mWifiApManager.openwifiap("abcd", null, WifiApManager.NO_PASS)) {
            Toast.makeText(this, "server 端启动", Toast.LENGTH_LONG).show();
            new ServerThread().start();
        } else {
            Toast.makeText(this, "server 端失败，请重试", Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.button3)
    public void onViewClicked() {
        //BufferedWriter br = new BufferedWriter(new OutputStreamWriter(clientOutputStream));
        String msg = edit.getText().toString();
        try {
            //br.write(msg, 0, msg.length());
            clientOutputStream.write(msg.getBytes("utf-8"));
            clientOutputStream.flush();
            Log.d(TAG, "send msg: " + msg);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "client send msg error: " + e.getMessage());
        }
    }

    private class ServerThread extends Thread {
        private ServerSocket mserverSocket;
        private Socket socket;
        private boolean isrun = true;
        private BufferedReader in;
        private PrintWriter out;

        @Override
        public void run() {
            super.run();
            try {
                Log.d(TAG, "server client start.");
                mserverSocket = new ServerSocket(SERVERPORT);
                while (isrun) {
                    Log.d(TAG, "server client run.");
                    socket = mserverSocket.accept();
                    String remoteIP = socket.getInetAddress().getHostAddress();
                    int remotePort = socket.getLocalPort();
                    Log.d(TAG, "A client connected. IP:" + remoteIP + ", Port: " + remotePort);
                    System.out.println("server: receiving.............");
                    // 获得 client 端的输入输出流，为进行交互做准备
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
                    out = new PrintWriter(socket.getOutputStream(), false);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                    }
                    String receive = sb.toString();
                    Log.d(TAG, "socket received: " + receive);
                    out.println("Your message has been received successfully！.");
// 关闭各个流
                    out.close();
                    in.close();
/*                    Message message = hander.obtainMessage();
                    message.obj=tmp;
                    hander.sendMessage(message);*/
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                    /*new Thread(new Runnable() {
                        @Override
                        public void run() {
                            byte[] buffer = new byte[1024];
                            int bytes;
                            InputStream mmInStream = null;

                            try {
                                mmInStream = socket.getInputStream();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            System.out.println("server");

                            try {
                                InputStream in = socket.getInputStream();
                                OutputStream os = socket.getOutputStream();

                                byte[] data = new byte[1024];
                                while (in.available() <= 0)
                                    ;// 同步

                                int len = in.read(data);

                                String[] str = new String(data, 0, len, "utf-8")
                                        .split(";");

                                String path = Environment.getExternalStorageDirectory()
                                        .getAbsolutePath() + "/CHFS/000000000000" + "/";
                                if (len != -1) {
                                    path += "socket_" + str[0];// str[0]是文件名加类型
                                }
                                //handler.obtainMessage(10, (Object) str[0]).sendToTarget();
                                System.out.println(path);
                                os.write("start".getBytes());
                                os.flush();

                                File file = new File(path);
                                DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
                                System.out.println("开始接收.....");
                                int countSize = 0;
                                while ((len = in.read(data)) != -1) {
                                    out.write(data, 0, len);
                                    countSize += len;
                                }
                                os.close();
                                out.flush();
                                out.close();
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    socket.close();
                                    System.out.println("关闭....");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                //handler.obtainMessage(10, (Object) "接受 完成").sendToTarget();
                            }
                        }
                    }).start();
                }
                if (mserverSocket != null) {
                    try {
                        mserverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }*/
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "server exception: " + e.getMessage());
            } finally {
                Log.d(TAG, "server socket close.");
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //handler.obtainMessage(10, (Object) "接受 完成").sendToTarget();
            }
        }
    }

    @OnClick(R.id.button2)
    public void onConnWifiBtnClicked() {
        //thread();
        if (mWifiApManager.checkState() == WifiManager.WIFI_STATE_ENABLED) {
            connectWifi();
        } else {
            mWifiApManager.openWifi();
        }
    }

    private void connectWifi() {
        mWifiApManager.connect("abcd", "", WifiApManager.WifiCipherType.WIFICIPHER_NOPASS);
        //thread();
        new Handler().postDelayed(() -> new ClientThread().start(), 3000);
    }

    private OutputStream clientOutputStream;


    private class ClientThread extends Thread {
        private Socket socket;
        private boolean isrun = true;
        private OutputStream os;
        //private InputStream in;
        private BufferedReader in;
        private PrintStream out;

        @Override
        public void run() {
            try {
                Log.d(TAG, "server ip= " + mWifiApManager.getServerIp());
                socket = new Socket(mWifiApManager.getServerIp(), SERVERPORT);
                clientOutputStream = socket.getOutputStream();
                out = new PrintStream(socket.getOutputStream(), true, "gbk");
                while (isrun) {
                    byte[] data = receiveData();
                    if (data.length > 1) {
                        System.out.println(new String(data));
                    }
                }
                /*new Thread(new Runnable() {

                    @Override
                    public void run() {
                        if (socket == null) {
                            return;
                        }
                        System.out.println("client connect");

                        try {
                            String path = Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/CHFS/000000000000";
                            if (android.os.Build.MODEL.contains("8812")) {
                                path += "/camera/" + "camera_temp_name.jpg";
                            } else {
//                path += "/camera/" + "camera_temp_name.mp4";
                                path += "/ARChon-v1.1-x86_64.zip";
                            }
                            DataInputStream read = new DataInputStream(new FileInputStream(new File(path)));
                            System.out.println(read.available());
                            String fileName = path.substring(path.lastIndexOf("/") + 1);// 获得文件名加类型

                            System.out.println(fileName);

                            os = socket.getOutputStream();
                            in = socket.getInputStream();
                            os.write((fileName + ";" + read.available()).getBytes("utf-8"));// 将文件名和文件大小传给接收端
                            os.flush();
                            byte[] data = new byte[1024];
                            int len = in.read(data);
                            String start = new String(data, 0, len);
                            int sendCountLen = 0;
                            if (start.equals("start")) {
                                while ((len = read.read(data)) != -1) {
                                    os.write(data, 0, len);
                                    sendCountLen += len;
                                }
                                os.flush();
                                os.close();
                                read.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();*/
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "client socket IOException " + e.getMessage());
            } finally {
                Log.d(TAG, "socket close");
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        BufferedInputStream bufferedInputStream;

        public byte[] receiveData() {

            byte[] data = null;
            if (socket.isConnected()) {
                try {
                    bufferedInputStream = new BufferedInputStream(socket.getInputStream());
                    data = new byte[bufferedInputStream.available()];
                    bufferedInputStream.read(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                data = new byte[1];
            }
            return data;
        }
    }

    private BroadcastReceiver apReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
                //"android.net.wifi.WIFI_AP_STATE_CHANGED"这个是热点状态改变的广播
                int state = intent.getIntExtra("wifi_state", 0);
                Log.d("热点状态", String.valueOf(state));
            /* state:
            12：正在开启热点
            13：已开启热点
            10：正在关闭热点
            11：已关闭热点
             */
            }
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                Log.d(TAG, "onReceive: WifiManager.WIFI_STATE_CHANGED_ACTION: " + state);
                if (state == WifiManager.WIFI_STATE_ENABLED) {
                    //mWifiApManager.addNetwork(mWifiApManager.CreateWifiInfo("abcd", null, WifiApManager.NO_PASS));
                    //connectWifi();
                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(apReceiver);
    }


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void thread() {
        final String ip = mWifiApManager.getServerIp();
        final int port = 1234;
        new Thread() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(ip, port);
                    Log.e("wifisocket", "建立连接");
                    InputStream in = socket.getInputStream();
                    out = socket.getOutputStream();

                    //接收消息
                    while (true) {
                        byte[] buffer = new byte[1024];
                        int len = 0;
                        if ((len = in.read(buffer)) != -1) {
                            byte[] data = new byte[len];
                            for (int i = 0; i < data.length; i++)
                                data[i] = buffer[i];
                            String msg = new String(data);
                            Log.e("收到消息", msg);

                            Message message = new Message();
                            message.what = 1;
                            message.obj = msg;
                            handler.sendMessage(message);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }
}
