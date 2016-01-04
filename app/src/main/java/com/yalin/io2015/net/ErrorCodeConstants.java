package com.yalin.io2015.net;


import com.yalin.io2015.R;
import com.yalin.io2015.application.IoApplication;

/**
 * Created by 雅麟 on 2015/6/19.
 */
public enum ErrorCodeConstants {
    Success(0, 0),
    NoDeviceError(201, R.string.no_device_error),
    NetworkError(297, R.string.network_error_info),
    ServerError(298, R.string.server_error_info),
    UnknownError(299, R.string.unknown_error);


    public final int serverNumber;
    public final int resId;

    ErrorCodeConstants(int number, int resId) {
        this.serverNumber = number;
        this.resId = resId;
    }

    public static ErrorCodeConstants getValue(int error) {
        for (ErrorCodeConstants item : values()) {
            if (error == item.serverNumber) {
                return item;
            }
        }
        return UnknownError;
    }
}
