package com.yalin.u2fclient;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.yalin.u2fclient.constants.Constants;
import com.yalin.u2fclient.domain.U2FRequest;
import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.u2fclient.interfaces.U2FResultCallback;
import com.yalin.u2fclient.log.StatLog;
import com.yalin.u2fclient.plugin.PhoneMonitorApi;
import com.yalin.u2fclient.u2f.U2FService;

/**
 * u2f sdk interface class
 */
public class U2FClientApi {
    private static final String TAG = U2FClientApi.class.getSimpleName();
    private static Context applicationContext;

    /**
     * start sdk,you should invoked this method in Application.onCreate();
     *
     * @param context application context
     */
    public static void init(Context context) {
        if (context == null) {
            throw new IllegalStateException("Api context can not be null");
        }
        applicationContext = context;
//        Plugins.start(context);
    }

    public static Context getContext() {
        return applicationContext;
    }

    /**
     * start bluetooth register
     *
     * @param registerRequest   get from your server
     * @param u2FResultCallback a callback received register result
     */
    @Deprecated
    public static void startBluetoothRegister(U2FRequest registerRequest, String address, U2FResultCallback u2FResultCallback) {
        if (registerRequest == null) {
            if (u2FResultCallback != null) {
                u2FResultCallback.onError(U2FErrorCode.ERROR_DATA_ERROR);
            }
            return;
        }
        U2FService.registerBluetooth(applicationContext, registerRequest, address, u2FResultCallback);
    }

    /**
     * start bluetooth verify
     *
     * @param verifyRequest     get from your server
     * @param u2FResultCallback a callback received register result
     */
    @Deprecated
    public static void startBluetoothVerify(U2FRequest verifyRequest, String address, U2FResultCallback u2FResultCallback) {
        if (verifyRequest == null) {
            if (u2FResultCallback != null) {
                u2FResultCallback.onError(U2FErrorCode.ERROR_DATA_ERROR);
            }
            return;
        }
        U2FService.verifyBluetooth(applicationContext, verifyRequest, address, u2FResultCallback);
    }

    /**
     * start ble register
     *
     * @param registerRequest   get from your server
     * @param u2FResultCallback a callback received register result
     */
    public static void startBleRegister(U2FRequest registerRequest, U2FResultCallback u2FResultCallback) {
        if (registerRequest == null) {
            if (u2FResultCallback != null) {
                u2FResultCallback.onError(U2FErrorCode.ERROR_DATA_ERROR);
            }
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (u2FResultCallback != null) {
                u2FResultCallback.onError(U2FErrorCode.ERROR_BLE_SYSTEM_NOT_SUPPORT);
            }
            return;
        }
        U2FService.registerBle(applicationContext, registerRequest, u2FResultCallback);
    }

    /**
     * start ble verify
     *
     * @param verifyRequest     get from your server
     * @param u2FResultCallback a callback received register result
     */
    public static void startBleVerify(U2FRequest verifyRequest, U2FResultCallback u2FResultCallback) {
        if (verifyRequest == null) {
            if (u2FResultCallback != null) {
                u2FResultCallback.onError(U2FErrorCode.ERROR_DATA_ERROR);
            }
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (u2FResultCallback != null) {
                u2FResultCallback.onError(U2FErrorCode.ERROR_BLE_SYSTEM_NOT_SUPPORT);
            }
            return;
        }
        U2FService.verifyBle(applicationContext, verifyRequest, u2FResultCallback);
    }

    /**
     * start ble transaction
     *
     * @param transactionRequest
     * @param u2FResultCallback
     */
    public static void startBleTransactionConfirm(U2FRequest transactionRequest, U2FResultCallback u2FResultCallback) {
        if (transactionRequest == null) {
            if (u2FResultCallback != null) {
                u2FResultCallback.onError(U2FErrorCode.ERROR_DATA_ERROR);
            }
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (u2FResultCallback != null) {
                u2FResultCallback.onError(U2FErrorCode.ERROR_BLE_SYSTEM_NOT_SUPPORT);
            }
            return;
        }
        U2FService.verifyBle(applicationContext, transactionRequest, u2FResultCallback);
    }

    /**
     * start nfc register
     *
     * @param registerRequest
     * @param u2FResultCallback
     */
    public static void startNFCRegister(U2FRequest registerRequest, U2FResultCallback u2FResultCallback) {
        if (registerRequest == null) {
            StatLog.printLog(TAG, "startNFCRegister register request is null");
            if (u2FResultCallback != null) {
                u2FResultCallback.onError(U2FErrorCode.ERROR_DATA_ERROR);
            }
            return;
        }
        U2FService.registerNFC(applicationContext, registerRequest, u2FResultCallback);
    }

    /**
     * start nfc verify
     *
     * @param verifyRequest
     * @param u2FResultCallback
     */
    public static void startNFCVerify(U2FRequest verifyRequest, U2FResultCallback u2FResultCallback) {
        if (verifyRequest == null) {
            StatLog.printLog(TAG, "startNFCVerify verifyRequest is null or size == 0");
            if (u2FResultCallback != null) {
                u2FResultCallback.onError(U2FErrorCode.ERROR_DATA_ERROR);
            }
            return;
        }
        U2FService.verifyNFC(applicationContext, verifyRequest, u2FResultCallback);
    }

    /**
     * cancel all u2f progress
     */
    public static void cancelU2F() {
        Intent intent = new Intent(Constants.DEVICE_CANCEL);
        applicationContext.sendBroadcast(intent);
    }

    /**
     * set the qkey's uid,
     * set will only success one time, if you repeatedly set, will not take effect.
     *
     * @param uid
     */
    public static void setUID(byte[] uid) {
        PhoneMonitorApi.setUID(applicationContext, uid);
    }

    /**
     * synchronization qkey's time
     */
    public static void synTime(Object callback) {
        PhoneMonitorApi.synTime(applicationContext, System.currentTimeMillis() / 1000, callback);
    }
}
