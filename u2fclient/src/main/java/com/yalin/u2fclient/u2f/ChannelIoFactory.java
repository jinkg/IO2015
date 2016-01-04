package com.yalin.u2fclient.u2f;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.yalin.u2fclient.log.StatLog;
import com.yalin.u2fclient.protocol.Protocol;
import com.yalin.u2fclient.u2f.ble.BleChannelIo;
import com.yalin.u2fclient.u2f.bt.BluetoothChannelIo;
import com.yalin.u2fclient.u2f.nfc.NFCChannelIo;

/**
 * Created by YaLin on 2015/11/19.
 */
public class ChannelIoFactory {
    private static final String TAG = ChannelIoFactory.class.getSimpleName();

    public static ChannelIo getInstance(Context context, Intent intent) {
        if (intent == null) {
            throw new IllegalStateException("intent can not be null");
        }
        int type = intent.getIntExtra(Protocol.DEVICE_TYPE_KEY, Protocol.DEFAULT_DEVICE_TYPE);
        switch (type) {
            case Protocol.BLUETOOTH_DEVICE_TYPE:
                String address = intent.getStringExtra(Protocol.ADDRESS_KEY);
                if (TextUtils.isEmpty(address)) {
                    return null;
                }
                StatLog.printLog(TAG, "create bluetooth device io");
                return new BluetoothChannelIo(context, address);
            case Protocol.NFC_DEVICE_TYPE:
                StatLog.printLog(TAG, "create nfc device io");
                return new NFCChannelIo(context);
            case Protocol.BLE_DEVICE_TYPE:
                StatLog.printLog(TAG, "create ble device io");
                return new BleChannelIo(context);
            default:
                return null;
        }
    }
}
