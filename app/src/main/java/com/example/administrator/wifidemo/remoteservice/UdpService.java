/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.administrator.wifidemo.remoteservice;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.administrator.wifidemo.WifiDemoApplication;
import com.example.administrator.wifidemo.utils.IpUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class UdpService {
    // Debugging
    private static final String TAG = "liwei";

    // indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections

    // Member fields
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private SendThread mSendThread;
    private MultiAcceptThread mMultiAcceptThread;
    private MultiSendThread mMultiSendThread;
    private int mState;
    private int mNewState;
    private ExecutorService mThreadPool;
    private ReceiveMessageListener mReceiveMessageListener;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public UdpService(Context context, Handler handler) {
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
        mThreadPool = Executors.newCachedThreadPool();
    }

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     */
    public UdpService(Context context) {
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = new Handler();
        mThreadPool = Executors.newCachedThreadPool();
    }

    public void setReceiveMessageListener(ReceiveMessageListener receiveMessageListener) {
        mReceiveMessageListener = receiveMessageListener;
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "UdpService updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */

    public synchronized void start() {
        Log.d(TAG, "UdpService start");

        if (mSendThread != null) {
            mSendThread.cancel();
            mSendThread = null;
        }

        if (mMultiSendThread != null) {
            mMultiSendThread.cancel();
            mMultiSendThread = null;
        }

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mThreadPool.execute(mAcceptThread);
        }

        if (mMultiAcceptThread != null) {
            mMultiAcceptThread.cancel();
            mMultiAcceptThread = null;
        }
        // Update UI title
        updateUserInterfaceTitle();
    }

    public synchronized void multiStart() {
        Log.d(TAG, "UdpService start multi");

        if (mSendThread != null) {
            mSendThread.cancel();
            mSendThread = null;
        }

        if (mMultiSendThread != null) {
            mMultiSendThread.cancel();
            mMultiSendThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        if (mMultiAcceptThread == null) {
            mMultiAcceptThread = new MultiAcceptThread();
            mThreadPool.execute(mMultiAcceptThread);
        }
        // Update UI title
        updateUserInterfaceTitle();
        multiWrite((Constants.UDP_BROADCAST_MESSAGE + IpUtils.getIPAddress(WifiDemoApplication.getInstance().getApplicationContext())).getBytes());
    }

    public synchronized void connect(String ip, int port) {
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "UdpService stop");

        if (mSendThread != null) {
            mSendThread.cancel();
            mSendThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        if (mMultiSendThread != null) {
            mMultiSendThread.cancel();
            mMultiSendThread = null;
        }
        if (mMultiAcceptThread != null) {
            mMultiAcceptThread.cancel();
            mMultiAcceptThread = null;
        }

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see SendThread#setMessage(byte[])
     */
    public void write(byte[] out, String ip) {
        synchronized (this) {
            if (mSendThread == null) {
                mSendThread = new SendThread();
            }
            mThreadPool.execute(mSendThread.setMessage(out).setIp(ip));
        }
    }

    public void multiWrite(byte[] out) {
        synchronized (this) {
            if (mMultiSendThread == null) {
                mMultiSendThread = new MultiSendThread();
            }
            mThreadPool.execute(mMultiSendThread.setMessage(out));
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        UdpService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread implements Runnable {
        // The local server socket
        DatagramSocket datagramSocket;

        @Override
        public void run() {
            Log.d(TAG, "UdpService BEGIN mAcceptThread" + this);
            //setName("AcceptThread");

            Log.d(TAG, "UdpService server client start.");

            try {
                datagramSocket = new DatagramSocket(Constants.PORT);
                Log.d(TAG, "UdpService server client run.");
                int remotePort = datagramSocket.getLocalPort();
                Log.d(TAG, "UdpService receiving............." + ", Port: " + remotePort);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread failed", e);
                e.printStackTrace();
            }
            byte[] data = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
            mState = STATE_LISTEN;
            // Listen to the server socket if we're not connected
            while (mState == STATE_LISTEN) {
                // If a connection was accepted
                if (datagramSocket != null) {
                    try {
                        Log.d(TAG, "UdpService run: datagramSocket.receive(datagramPacket)");
                        datagramSocket.receive(datagramPacket);
                        String msg = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                        //mReceiveMessageListener.onReceived(msg);
                        Log.d("UDP Demo", datagramPacket.getAddress().getHostAddress() + ":" + msg);
                        if (msg.equals(Constants.UDP_BROADCAST_TV_REPLY + "\n")) {
                            Log.d(TAG, "UdpService mHandler.obtainMessage(Constants.MESSAGE_TV_IP, datagramPacket.getAddress().getHostAddress()).sendToTarget();");
                            mHandler.obtainMessage(Constants.MESSAGE_TV_IP, datagramPacket.getAddress().getHostAddress()).sendToTarget();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "datagramSocket.receive(datagramPacket)", e);
                    }
                }
            }
            Log.i(TAG, "END AcceptThread");
        }

        void cancel() {
            Log.d(TAG, "UdpService AcceptThread cancel: " + Thread.currentThread());
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class SendThread implements Runnable {

        private DatagramSocket datagramSocket;
        private byte[] message;
        private String ip;

        SendThread setMessage(byte[] msg) {
            this.message = msg;
            return this;
        }

        SendThread setIp(String ip) {
            this.ip = ip;
            return this;
        }

        @Override
        public void run() {
            Log.i(TAG, "SEND mSendThread");
            //setName("SendThread");

            try {
                datagramSocket = new DatagramSocket();
            } catch (IOException e) {
                Log.e(TAG, "SendThread create() failed", e);
                try {
                    datagramSocket.close();
                } catch (Exception e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }
            InetAddress addr = null;
            try {
                Log.d(TAG, "UdpService ip = " + ip);
                addr = InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(TAG, "InetAddress.getByName(ip);", e);
            }
            if (message != null && message.length > 0) {
                Log.d(TAG, "UdpService send msg: " + new String(message) + ", ip: " + addr + ", port: " + Constants.PORT);
                DatagramPacket datagramPacket = new DatagramPacket(message, message.length, addr, Constants.PORT);
                try {
                    datagramSocket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "datagramSocket.send(datagramPacket);", e);
                }
            }
        }

        void cancel() {
            try {
                datagramSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class MultiAcceptThread implements Runnable {
        // The local server socket
        MulticastSocket datagramSocket;

        @Override
        public void run() {
            Log.d(TAG, "UdpService BEGIN mAcceptThread" + this);
            //setName("AcceptThread");
            
            try {
                datagramSocket = new MulticastSocket(Constants.PORT);
                datagramSocket.setTimeToLive(Constants.TTL_TIME);
                datagramSocket.joinGroup(InetAddress.getByName(Constants.MULTICAST_ADDR));
                Log.d(TAG, "UdpService server client run.");
                int remotePort = datagramSocket.getLocalPort();
                Log.d(TAG, "UdpService receiving............." + ", Port: " + remotePort);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread failed", e);
                e.printStackTrace();
            }
            byte[] data = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
            mState = STATE_LISTEN;
            // Listen to the server socket if we're not connected
            while (mState == STATE_LISTEN) {
                // If a connection was accepted
                if (datagramSocket != null) {
                    try {
                        Log.d(TAG, "UdpService run: datagramSocket.receive(datagramPacket)");
                        datagramSocket.receive(datagramPacket);
                        String msg = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                        //mReceiveMessageListener.onReceived(msg);
                        Log.d("UDP Demo", datagramPacket.getAddress().getHostAddress() + ":" + msg);
                        if (msg.equals(Constants.UDP_BROADCAST_TV_REPLY)) {
                            mHandler.obtainMessage(Constants.MESSAGE_TV_IP, datagramPacket.getAddress().getHostAddress()).sendToTarget();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "datagramSocket.receive(datagramPacket)", e);
                    }
                }
            }
            Log.i(TAG, "END AcceptThread");
        }

        void cancel() {
            Log.d(TAG, "UdpService AcceptThread cancel: " + Thread.currentThread());
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }

    private class MultiSendThread implements Runnable {

        private MulticastSocket datagramSocket;
        private byte[] message;
        private String ip;

        MultiSendThread setMessage(byte[] msg) {
            this.message = msg;
            return this;
        }

        MultiSendThread setIp(String ip) {
            this.ip = ip;
            return this;
        }

        @Override
        public void run() {
            Log.i(TAG, "SEND mSendThread");
            //setName("SendThread");

            try {
                datagramSocket = new MulticastSocket();
                datagramSocket.setTimeToLive(Constants.TTL_TIME);
            } catch (IOException e) {
                Log.e(TAG, "SendThread create() failed", e);
                try {
                    datagramSocket.close();
                } catch (Exception e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }
            if (message != null && message.length > 0) {
                Log.d(TAG, "UdpService send msg: " + new String(message) + ", ip: " + Constants.MULTICAST_ADDR + ", port: " + Constants.PORT);
                try {
                    DatagramPacket datagramPacket = new DatagramPacket(message, message.length, InetAddress.getByName(Constants.MULTICAST_ADDR), Constants.PORT);
                    datagramSocket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "datagramSocket.send(datagramPacket);", e);
                }
            }
        }

        void cancel() {
            try {
                datagramSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
