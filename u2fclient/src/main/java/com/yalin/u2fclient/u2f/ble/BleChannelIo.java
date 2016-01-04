package com.yalin.u2fclient.u2f.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;

import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.u2fclient.log.StatLog;
import com.yalin.u2fclient.u2f.ChannelIo;

import org.apache.commons.codec.binary.Hex;

import java.util.Timer;
import java.util.TimerTask;

public class BleChannelIo implements ChannelIo, BleReceiver.BluetoothStatCallback {
    private static final String TAG = BleChannelIo.class.getSimpleName();

    private Context mContext;
    private final BluetoothAdapter mAdapter;
    private int mState;

    protected MessageCallback messageCallback;
    protected ChannelStateCallback stateCallback;

    private Timer mTimer;

    private BleTransmitService mBleTransmitService;
    private BleReceiver receiver = new BleReceiver(this);
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BleTransmitService.ACTION_GATT_CONNECTED.equals(action)) {
                StatLog.printLog(TAG, "ble io ACTION_GATT_CONNECTED received");
            } else if (BleTransmitService.ACTION_GATT_DISCONNECTED.equals(action)) {
                StatLog.printLog(TAG, "ble io ACTION_GATT_DISCONNECTED received");
                connectionLost();
            } else if (BleTransmitService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                StatLog.printLog(TAG, "ble io ACTION_GATT_SERVICES_DISCOVERED received");
            } else if (BleTransmitService.ACTION_DATA_AVAILABLE.equals(action)) {
                StatLog.printLog(TAG, "ble io ACTION_DATA_AVAILABLE received");
                String dataBase64 = intent.getStringExtra(BleTransmitService.EXTRA_DATA);
                onMessage(dataBase64);
            } else if (BleTransmitService.ACTION_GATT_CONNECT_FAILED.equals(action)) {
                StatLog.printLog(TAG, "ble io ACTION_GATT_CONNECT_FAILED received");
                connectionFailed(U2FErrorCode.ERROR_BLUETOOTH_CONNECT_FAILED);
            } else if (BleTransmitService.ACTION_GATT_CONNECT_READY.equals(action)) {
                onConnect(mDeviceAddress);
            } else if (BleTransmitService.ACTION_GATT_WRITE_ERROR.equals(action)
                    || BleTransmitService.ACTION_GATT_READ_ERROR.equals(action)) {
                connectionFailed(U2FErrorCode.ERROR_BLUETOOTH_REFUSED);
            } else if (BleTransmitService.ACTION_GATT_READ_PENDING.equals(action)) {
                setTimeoutTimer();
            } else if (BleTransmitService.ACTION_GATT_ON_USER_ACTION.equals(action)) {
                onUserAction();
            } else if (BleTransmitService.ACTION_GATT_ON_ACTION_RESULT.equals(action)) {
                onUserResult();
            }
        }
    };

    private String mDeviceAddress;

    public BleChannelIo(Context context) {
        mContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    private synchronized void setState(int state) {
        StatLog.printLog(TAG, "ble io setState() " + mState + " -> " + state);
        mState = state;
    }

    public synchronized int getState() {
        return mState;
    }

    private void bleStart() {
        if (mAdapter == null) {
            connectionFailed(U2FErrorCode.ERROR_BLUETOOTH_NOT_AVAILABLE);
            return;
        }
        openBluetooth();
    }

    private void openBluetooth() {
        if (!mAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(enableIntent);
        } else {
            pickDevice();
        }
    }

    private void pickDevice() {
//        if (mState != STATE_NONE) {
//            return;
//        }
//        if (stateCallback != null) {
//            stateCallback.onConnectStart();
//        }
//        mDeviceAddress = address;
//        Intent gattServiceIntent = new Intent(mContext, BleTransmitService.class);
//        mContext.bindService(gattServiceIntent, mServiceConnection, mContext.BIND_AUTO_CREATE);
//        setState(STATE_CONNECTING);
    }

    @Override
    public void reset() {
        StatLog.printLog(TAG, "ble io reset");
        setState(STATE_NONE);
    }

    @Override
    public synchronized void connect() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, filter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        bleStart();
    }

    @Override
    public synchronized void stop() {
        StatLog.printLog(TAG, "ble io release");
        try {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mGattUpdateReceiver);
        } catch (Exception e) {
        }
        try {
            cancelTimeoutTimer();
            mBleTransmitService.disconnect();
        } catch (Exception e) {
            StatLog.printLog(TAG, e.getMessage());
        }
        try {
            mContext.unbindService(mServiceConnection);
        } catch (Exception e) {
            StatLog.printLog(TAG, e.getMessage());
        }
        setState(STATE_NONE);
    }

    @Override
    public void setChannelStatCallback(ChannelStateCallback callback) {

    }

    @Override
    public void setIoDataCallback(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    @Override
    public void write(byte[] out) {
        if (mBleTransmitService != null) {
            if (mBleTransmitService.writeU2FCharacteristic(out)) {
            } else {
                connectionFailed(U2FErrorCode.ERROR_BLUETOOTH_REFUSED);
            }
        }
    }

    private void onConnect(String deviceName) {
        if (stateCallback != null) {
            stateCallback.onConnect(deviceName);
        }
        setState(STATE_CONNECTED);
    }

    private void connectionFailed(int error) {
        StatLog.printLog(TAG, "ble io connectionFailed error : " + error);
        if (stateCallback != null) {
            stateCallback.connectFailed(error);
        }
        stop();
    }

    private void connectionLost() {
        if (stateCallback != null) {
            stateCallback.onConnectLost();
        }
        stop();
    }

    private void onUserAction() {
        StatLog.printLog(TAG, "on user action");
        if (messageCallback != null) {
            messageCallback.onMessage(6985, new byte[]{0});
        }
    }

    private void onUserResult() {
        StatLog.printLog(TAG, "on action result");
        if (messageCallback != null) {
            messageCallback.onMessage(6986, new byte[]{0});
        }
    }

    private void onMessage(String dataBase64) {
        if (TextUtils.isEmpty(dataBase64)) {
            return;
        }
        cancelTimeoutTimer();
        byte[] data = Base64.decode(dataBase64, Base64.URL_SAFE);
        StatLog.printLog(TAG, "ble io read data len :" + data.length + " data : " + new String(Hex.encodeHex(data)));
        if (messageCallback != null) {
            messageCallback.onMessage(data.length, data);
        }
    }

    private void cancelTimeoutTimer() {
        StatLog.printLog(TAG, "ble io cancel timeout timer");
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void setTimeoutTimer() {
        StatLog.printLog(TAG, "ble io set timeout timer");
        cancelTimeoutTimer();
        mTimer = new Timer();
        mTimer.schedule(new TimeoutTask(), TIMEOUT);
    }

    @Override
    public void onBluetoothOpen() {
        pickDevice();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleTransmitService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleTransmitService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleTransmitService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleTransmitService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleTransmitService.ACTION_GATT_CONNECT_FAILED);
        intentFilter.addAction(BleTransmitService.ACTION_GATT_CONNECT_READY);
        intentFilter.addAction(BleTransmitService.ACTION_GATT_WRITE_ERROR);
        intentFilter.addAction(BleTransmitService.ACTION_GATT_READ_ERROR);
        intentFilter.addAction(BleTransmitService.ACTION_GATT_ON_USER_ACTION);
        intentFilter.addAction(BleTransmitService.ACTION_GATT_ON_ACTION_RESULT);
        return intentFilter;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBleTransmitService = ((BleTransmitService.LocalBinder) service).getService();
            if (!mBleTransmitService.initialize()) {
                StatLog.printLog(TAG, "ble io mBleTransmitService start failed");
                connectionFailed(U2FErrorCode.ERROR_BLUETOOTH_CONNECT_FAILED);
            }
            if (mBleTransmitService.connect(mDeviceAddress)) {
                StatLog.printLog(TAG, "ble io mBleTransmitService connected");
            } else {
                connectionFailed(U2FErrorCode.ERROR_BLUETOOTH_CONNECT_FAILED);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            StatLog.printLog(TAG, "ble io onServiceDisconnected");
        }
    };

    class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            StatLog.printLog(TAG, "ble io read data timeout release connection");
            connectionFailed(U2FErrorCode.ERROR_DATA_TIMEOUT);
        }
    }
}
