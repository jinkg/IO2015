package com.yalin.u2fclient.u2f;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.yalin.u2fclient.domain.FinishRegisterData;
import com.yalin.u2fclient.domain.FinishVerifyData;
import com.yalin.u2fclient.domain.U2FRequest;
import com.yalin.u2fclient.interfaces.U2FResultCallback;
import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.u2fclient.log.StatLog;
import com.yalin.u2fclient.protocol.Protocol;
import com.yalin.u2fclient.protocol.ProtocolFactory;


import static com.yalin.u2fclient.constants.Constants.*;

/**
 * Created by YaLin on 2015/10/28.
 */
public class U2FService extends Service implements Protocol.U2FProtocolResultCallback {
    private static final String TAG = U2FService.class.getSimpleName();
    private static final int STATE_IDLE = 1;
    private static final int STATE_CONNECTING = 2;

    private static U2FResultCallback sU2FResultCallback;

    private int mState = STATE_IDLE;

    private String mConnectedDeviceName = null;
    private Protocol mProtocolImpl;

    BroadcastReceiver blueStatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (RELEASE_ACTION.equals(action)) {
                release();
            } else if (DEVICE_CANCEL.equals(action)) {
                onError(U2FErrorCode.ERROR_USER_CANCEL);
            }
        }
    };

    public static void registerBluetooth(Context context, U2FRequest data, String address, U2FResultCallback statCallback) {
        if (data == null || TextUtils.isEmpty(address)) {
            return;
        }
        setU2FResultCallback(statCallback);
        Intent intent = new Intent(context, U2FService.class);
        intent.putExtra(Protocol.OPTION_TYPE_KEY, Protocol.REGISTER_OPERATE_TYPE);
        intent.putExtra(Protocol.DATA_KEY, data);
        intent.putExtra(Protocol.ADDRESS_KEY, address);
        intent.putExtra(Protocol.DEVICE_TYPE_KEY, Protocol.BLUETOOTH_DEVICE_TYPE);
        context.startService(intent);
    }

    public static void verifyBluetooth(Context context, U2FRequest data, String address, U2FResultCallback statCallback) {
        if (data == null || TextUtils.isEmpty(address)) {
            return;
        }
        setU2FResultCallback(statCallback);
        Intent intent = new Intent(context, U2FService.class);
        intent.putExtra(Protocol.OPTION_TYPE_KEY, Protocol.VERIFY_OPERATE_TYPE);
        intent.putExtra(Protocol.DATA_KEY, data);
        intent.putExtra(Protocol.ADDRESS_KEY, address);
        intent.putExtra(Protocol.DEVICE_TYPE_KEY, Protocol.BLUETOOTH_DEVICE_TYPE);
        context.startService(intent);
    }

    public static void registerBle(Context context, U2FRequest data, U2FResultCallback statCallback) {
        setU2FResultCallback(statCallback);
        Intent intent = new Intent(context, U2FService.class);
        intent.putExtra(Protocol.OPTION_TYPE_KEY, Protocol.REGISTER_OPERATE_TYPE);
        intent.putExtra(Protocol.DATA_KEY, data);
        intent.putExtra(Protocol.DEVICE_TYPE_KEY, Protocol.BLE_DEVICE_TYPE);
        context.startService(intent);
    }

    public static void verifyBle(Context context, U2FRequest data, U2FResultCallback statCallback) {
        setU2FResultCallback(statCallback);
        Intent intent = new Intent(context, U2FService.class);
        intent.putExtra(Protocol.OPTION_TYPE_KEY, Protocol.VERIFY_OPERATE_TYPE);
        intent.putExtra(Protocol.DATA_KEY, data);
        intent.putExtra(Protocol.DEVICE_TYPE_KEY, Protocol.BLE_DEVICE_TYPE);
        context.startService(intent);
    }

    public static void registerNFC(Context context, U2FRequest data, U2FResultCallback statCallback) {
        setU2FResultCallback(statCallback);
        Intent intent = new Intent(context, U2FService.class);
        intent.putExtra(Protocol.OPTION_TYPE_KEY, Protocol.REGISTER_OPERATE_TYPE);
        intent.putExtra(Protocol.DATA_KEY, data);
        intent.putExtra(Protocol.DEVICE_TYPE_KEY, Protocol.NFC_DEVICE_TYPE);
        context.startService(intent);
    }

    public static void verifyNFC(Context context, U2FRequest data, U2FResultCallback statCallback) {
        setU2FResultCallback(statCallback);
        Intent intent = new Intent(context, U2FService.class);
        intent.putExtra(Protocol.OPTION_TYPE_KEY, Protocol.VERIFY_OPERATE_TYPE);
        intent.putExtra(Protocol.DATA_KEY, data);
        intent.putExtra(Protocol.DEVICE_TYPE_KEY, Protocol.NFC_DEVICE_TYPE);
        context.startService(intent);
    }

    public static void setU2FResultCallback(U2FResultCallback registerCallback) {
        sU2FResultCallback = registerCallback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final IntentFilter homeFilter = new IntentFilter();
        homeFilter.addAction(DEVICE_CANCEL);
        homeFilter.addAction(RELEASE_ACTION);
        registerReceiver(blueStatReceiver, homeFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        if (mState == STATE_CONNECTING) {
            return START_STICKY;
        }
        doU2f(intent);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mProtocolImpl != null) {
                mProtocolImpl.release();
            }
            unregisterReceiver(blueStatReceiver);
        } catch (Exception e) {
        }
        sU2FResultCallback = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void doU2f(Intent intent) {
        if (mProtocolImpl != null) {
            mProtocolImpl.release();
        }
        mState = STATE_CONNECTING;
        mProtocolImpl = ProtocolFactory.getInstance(this, intent, mHandler, this);
        if (mProtocolImpl != null) {
            mProtocolImpl.start();
        } else {
            mState = STATE_IDLE;
        }
    }

    private void release() {
        sU2FResultCallback = null;
        mState = STATE_IDLE;
        stopSelf();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_REGISTER_RESULT:
                    if (sU2FResultCallback != null) {
                        sU2FResultCallback.onRegisterResponse((FinishRegisterData) msg.obj);
                    }
                    release();
                    break;
                case Constants.MESSAGE_USER_ACTION:
                    if (sU2FResultCallback != null) {
                        sU2FResultCallback.onUserAction();
                    }
                    break;
                case Constants.MESSAGE_ACTION_RESULT:
                    if (sU2FResultCallback != null) {
                        sU2FResultCallback.onActionFinish();
                    }
                    break;
                case Constants.MESSAGE_VERIFY_RESULT:
                    if (sU2FResultCallback != null) {
                        sU2FResultCallback.onVerifyResponse((FinishVerifyData) msg.obj);
                    }
                    release();
                    break;
                case Constants.MESSAGE_DATA_ERROR:
                    onError(msg.arg1);
                    break;
                case Constants.MESSAGE_CONNECT_START:
                    if (sU2FResultCallback != null) {
                        sU2FResultCallback.onConnectionStart();
                    }
                    break;
                case Constants.MESSAGE_CONNECT:
                    mConnectedDeviceName = (String) msg.obj;
                    if (sU2FResultCallback != null) {
                        sU2FResultCallback.onConnect(mConnectedDeviceName);
                    }
                    break;
                case Constants.MESSAGE_CONNECT_LOST:
                    if (sU2FResultCallback != null) {
                        sU2FResultCallback.onConnectionLost();
                    }
                    release();
                    break;
                case Constants.MESSAGE_CONNECT_FAILED:
                    onError(msg.arg1);
                    break;
            }
        }
    };

    void onError(int errorCode) {
        if (sU2FResultCallback != null) {
            sU2FResultCallback.onError(errorCode);
        }
        release();
    }

    @Override
    public void onRegisterResponse(FinishRegisterData registerResponse) {
        StatLog.printLog(TAG, "onRegisterResponse");
        mHandler.obtainMessage(Constants.MESSAGE_REGISTER_RESULT, registerResponse).sendToTarget();
    }

    @Override
    public void onUserAction() {
        StatLog.printLog(TAG, "onUserAction");
        mHandler.obtainMessage(Constants.MESSAGE_USER_ACTION).sendToTarget();
    }

    @Override
    public void onActionResult() {
        StatLog.printLog(TAG, "onActionResult");
        mHandler.obtainMessage(Constants.MESSAGE_ACTION_RESULT).sendToTarget();
    }

    @Override
    public void onVerifyResponse(FinishVerifyData verifyData) {
        StatLog.printLog(TAG, "onVerifyResponse");
        mHandler.obtainMessage(Constants.MESSAGE_VERIFY_RESULT, verifyData).sendToTarget();
    }

    @Override
    public void onDataError(int errorCode) {
        StatLog.printLog(TAG, "onError");
        mHandler.obtainMessage(Constants.MESSAGE_DATA_ERROR, errorCode, 0).sendToTarget();
    }
}
