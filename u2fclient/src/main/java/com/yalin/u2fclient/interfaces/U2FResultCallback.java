package com.yalin.u2fclient.interfaces;

import com.yalin.u2fclient.domain.FinishRegisterData;
import com.yalin.u2fclient.domain.FinishVerifyData;

/**
 * U2FResultCallback
 */
public interface U2FResultCallback {
    /**
     * invoke when sdk connect
     */
    void onConnectionStart();

    /**
     * invoke when connect be established between sdk and u2f device
     *
     * @param address the device name,if Ble device this is a Ble address
     */
    void onConnect(String address);

    /**
     * disconnect between sdk and device,will invoke
     */
    void onConnectionLost();

    /**
     * invoke when u2f device waiting for user presence,on this time,you should show prompt ui for user
     */
    void onUserAction();

    /**
     * when u2f device received user presence,this callback will be invoked,you should dismiss the prompt ui
     */
    void onActionFinish();

    /**
     * when u2f device's register data handle complete,invoked this
     *
     * @param data this result data,you should send it to your server
     */
    void onRegisterResponse(FinishRegisterData data);

    /**
     * when u2f device's verify data handle complete,invoked this
     *
     * @param data this result data,you should send it to your server
     */
    void onVerifyResponse(FinishVerifyData data);

    /**
     * invoked when error occurrence between sdk and device
     *
     * @param errorCode the error code,U2FErrorCode interface to see detail
     */
    void onError(int errorCode);
}
