package com.yalin.u2fclient.u2f.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.u2fclient.log.StatLog;
import com.yalin.u2fclient.u2f.ChannelIo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class BluetoothChannelIo implements ChannelIo, BluetoothReceiver.BluetoothStatCallback {

    private static final String TAG = BluetoothChannelIo.class.getSimpleName();

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private Context mContext;
    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    protected MessageCallback messageCallback;
    protected ChannelStateCallback stateCallback;

    protected BluetoothSocket mSocket;

    private BluetoothReceiver receiver = new BluetoothReceiver(this);

    private Timer mTimer;
    private String mAddress;

    public BluetoothChannelIo(Context context, String address) {
        if (context == null || TextUtils.isEmpty(address)) {
            throw new IllegalArgumentException();
        }
        mContext = context;
        mAddress = address;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    private synchronized void setState(int state) {
//        StatLog.printLog(TAG, "bluetooth io state changed " + state);
        mState = state;
    }

    public synchronized int getState() {
        return mState;
    }

    @Override
    public synchronized void setIoDataCallback(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    @Override
    public synchronized void reset() {
//        StatLog.printLog(TAG, "bluetooth io reset");
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {

            }
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);
    }

    private void bluetoothStart() {
        if (mAdapter == null) {
            connectionFailed(U2FErrorCode.ERROR_BLUETOOTH_NOT_AVAILABLE);
            return;
        }
        if (stateCallback != null) {
            stateCallback.onConnectStart();
        }
        openBluetooth();
    }

    private void openBluetooth() {
        if (!mAdapter.isEnabled()) {
            StatLog.printLog(TAG, "bluetooth not open request open");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(enableIntent);
        } else {
            StatLog.printLog(TAG, "bluetooth has open connect device");
            connectDevice();
        }
    }

    private void connectDevice() {
        BluetoothDevice device = mAdapter.getRemoteDevice(mAddress);
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device, true);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    @Override
    public synchronized void connect() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(receiver, filter);
//        StatLog.printLog(TAG, "register MonitorBleReceiver");
        bluetoothStart();
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
//        StatLog.printLog(TAG, "bluetooth connected");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        onConnect(device.getName());
        setState(STATE_CONNECTED);
    }

    @Override
    public synchronized void stop() {
//        StatLog.printLog(TAG, "release bluetooth io");
        try {
            cancelTimeoutTimer();
//            StatLog.printLog(TAG, "unregister MonitorBleReceiver");
            mContext.unregisterReceiver(receiver);
        } catch (Exception e) {
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    @Override
    public void setChannelStatCallback(ChannelStateCallback callback) {
        this.stateCallback = callback;
    }

    @Override
    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
        setTimeoutTimer();
    }

    private void onConnect(String deviceName) {
//        StatLog.printLog(TAG, "bluetooth device connect " + deviceName);
        if (stateCallback != null) {
            stateCallback.onConnect(deviceName);
        }
    }

    private void onMessage(int len, byte[] data) {
        cancelTimeoutTimer();
        if (messageCallback != null) {
            messageCallback.onMessage(len, data);
        }
    }

    private void connectionFailed(int error) {
        if (stateCallback != null) {
            stateCallback.connectFailed(error);
        }
//        StatLog.printLog(TAG, "bluetooth connection failed error code : " + error);
        reset();
    }

    private void connectionLost() {
//        StatLog.printLog(TAG, "bluetooth connection lost");
        if (stateCallback != null) {
            stateCallback.onConnectLost();
        }
        reset();
    }

    private void cancelTimeoutTimer() {
//        StatLog.printLog(TAG, "ble io cancel timeout timer");
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void setTimeoutTimer() {
//        StatLog.printLog(TAG, "ble io set timeout timer");
        cancelTimeoutTimer();
        mTimer = new Timer();
        mTimer.schedule(new TimeoutTask(), TIMEOUT);
    }

    @Override
    public void onBluetoothOpen() {
        connectDevice();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            setName("ConnectThread" + mSocketType);

            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                }
                connectionFailed(U2FErrorCode.ERROR_BLUETOOTH_CONNECT_FAILED);
                return;
            }

            synchronized (BluetoothChannelIo.this) {
                mConnectThread = null;
            }

            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
//            StatLog.printLog(TAG, "start bluetooth io stream");
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int byteLen;

            while (true) {
                try {
                    byteLen = mmInStream.read(buffer);
//                    StatLog.printLog(TAG, "bluetooth io read data len :" + byteLen + " data : " + new String(Hex.encodeHex(buffer)));
                    onMessage(byteLen, buffer);
                } catch (IOException e) {
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
//                StatLog.printLog(TAG, "bluetooth io write data :" + new String(Hex.encodeHex(buffer)));
            } catch (IOException e) {
                connectionLost();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    class TimeoutTask extends TimerTask {
        @Override
        public void run() {
//            StatLog.printLog(TAG, "bluetooth io read data timeout release connection");
            connectionFailed(U2FErrorCode.ERROR_DATA_TIMEOUT);
            stop();
        }
    }
}
