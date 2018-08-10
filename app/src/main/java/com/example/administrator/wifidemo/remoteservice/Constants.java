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

public interface Constants {

    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_DEVICE_NAME = 2;
    int MESSAGE_TOAST = 3;
    int MESSAGE_TV_IP = 4;

    // Key names received from the BluetoothChatService Handler
    String DEVICE_NAME = "device_name";
    String TOAST = "toast";

    // Return Intent extra
    String EXTRA_DEVICE_IP = "ip";

    // UDP广播寻找tv盒子
    String MULTICAST_ADDR = "255.255.255.255";
    int PORT = 8191;
    int TTL_TIME = 3;
    String UDP_BROADCAST_MESSAGE = "satcatche_mile_remote:";
    String UDP_BROADCAST_TV_REPLY = "satcatche_mile_tv";
    String UDP_CHANGE_TV_TCP = "satcatche_mile_tcp";
}
