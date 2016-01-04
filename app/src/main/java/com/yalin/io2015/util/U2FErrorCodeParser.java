package com.yalin.io2015.util;


import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.io2015.R;

/**
 * Created by YaLin on 2015/12/2.
 */
public class U2FErrorCodeParser {
    public static int parseError(int errorCode) {
        switch (errorCode) {
            case U2FErrorCode.ERROR_BLUETOOTH_CONNECT_FAILED:
                return R.string.bluetooth_connect_failed;
            case U2FErrorCode.ERROR_DATA_ERROR:
                return R.string.bluetooth_data_error;
            case U2FErrorCode.ERROR_USER_REGISTERED:
                return R.string.user_has_registered;
            case U2FErrorCode.ERROR_USER_NOT_REGISTER:
                return R.string.user_not_register_this_device;
            case U2FErrorCode.ERROR_DEVICE_VERSION_NOT_SUPPORT:
                return R.string.device_version_not_support;
            case U2FErrorCode.ERROR_NFC_NOT_AVAILABLE:
                return R.string.nfc_not_available;
            case U2FErrorCode.ERROR_NFC_NOT_OPENED:
                return R.string.nfc_not_opened;
            case U2FErrorCode.ERROR_DATA_TIMEOUT:
                return R.string.u2f_data_timeout;
            case U2FErrorCode.ERROR_BLUETOOTH_REFUSED:
                return R.string.u2f_bluetooth_refuse;
            case U2FErrorCode.ERROR_TYPE_ERROR:
                return R.string.u2f_option_not_supported;
            case U2FErrorCode.ERROR_NFC_CONNECT_ERROR:
                return R.string.u2f_nfc_connect_failed;
            case U2FErrorCode.ERROR_USER_PRESENCE_TIME_OUT:
                return R.string.u2f_option_timeout;
            case U2FErrorCode.ERROR_USER_CANCEL:
                return 0;
            default:
                return R.string.u2f_option_error;
        }
    }
}
