package com.yalin.u2fclient.interfaces;

/**
 * define u2f error code
 */
public interface U2FErrorCode {
    /**
     * bluetooth not available
     */
    int ERROR_BLUETOOTH_NOT_AVAILABLE = 101;
    /**
     * bluetooth pair refused
     */
    int ERROR_BLUETOOTH_REFUSED = 102;
    /**
     * bluetooth not paired
     */
    int ERROR_BLUETOOTH_NOT_PAIR = 103;
    /**
     * bluetooth connect failed
     */
    int ERROR_BLUETOOTH_CONNECT_FAILED = 104;
    /**
     * input u2f data error
     */
    int ERROR_DATA_ERROR = 105;
    /**
     * user not register this device
     */
    int ERROR_USER_NOT_REGISTER = 106;
    /**
     * user cancel
     */
    int ERROR_USER_CANCEL = 107;
    /**
     * this device already registered
     */
    int ERROR_USER_REGISTERED = 108;
    /**
     * device u2f version not support
     */
    int ERROR_DEVICE_VERSION_NOT_SUPPORT = 109;
    /**
     * device waiting for user presence time out
     */
    int ERROR_USER_PRESENCE_TIME_OUT = 110;
    /**
     * nfc not available
     */
    int ERROR_NFC_NOT_AVAILABLE = 111;

    /**
     * nfc connect error
     */
    int ERROR_NFC_CONNECT_ERROR = 112;

    /**
     * nfc not open
     */
    int ERROR_NFC_NOT_OPENED = 113;

    /**
     * u2f device not support a option type
     * e.g try transaction confirm besides qKey
     */
    int ERROR_TYPE_ERROR = 114;

    /**
     * u2f handle data timeout
     * general condition,the timeout is 10 second
     */
    int ERROR_DATA_TIMEOUT = 115;

    int ERROR_BLE_SYSTEM_NOT_SUPPORT = 116;
}
