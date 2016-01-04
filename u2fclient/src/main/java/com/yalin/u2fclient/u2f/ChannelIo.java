package com.yalin.u2fclient.u2f;

/**
 * Created by YaLin on 2015/11/19.
 */
public interface ChannelIo {
    long TIMEOUT = 120000;

    interface ChannelStateCallback {

        void onConnectStart();

        void onConnect(String deviceName);

        void connectFailed(int error);

        void onConnectLost();
    }

    interface MessageCallback {
        void onMessage(int len, byte[] message);
    }

    int STATE_NONE = 0;
    int STATE_LISTEN = 1;
    int STATE_CONNECTING = 2;
    int STATE_CONNECTED = 3;

    void reset();

    void connect();

    void stop();

    void setChannelStatCallback(ChannelStateCallback callback);

    void setIoDataCallback(MessageCallback messageCallback);

    int getState();

    void write(byte[] data);
}
