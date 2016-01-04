package com.yalin.u2fclient.protocol;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.yalin.u2fclient.domain.U2FRequest;
import com.yalin.u2fclient.u2f.ChannelIo;
import com.yalin.u2fclient.u2f.ChannelIoFactory;

/**
 * Created by YaLin on 2015/11/19.
 */
public class ProtocolFactory {

    public static Protocol getInstance(Context context, Intent intent, Handler handler, Protocol.U2FProtocolResultCallback callback) {
        int optionType = intent.getIntExtra(Protocol.OPTION_TYPE_KEY, Protocol.REGISTER_OPERATE_TYPE);
        ChannelIo io = ChannelIoFactory.getInstance(context, intent);
        if (io == null) {
            return null;
        }

        int deviceType = intent.getIntExtra(Protocol.DEVICE_TYPE_KEY, Protocol.DEFAULT_DEVICE_TYPE);
        U2FRequest u2FRequest = intent.getParcelableExtra(Protocol.DATA_KEY);
        Protocol protocolImpl;
        switch (deviceType) {
            case Protocol.BLUETOOTH_DEVICE_TYPE:
                protocolImpl = new BluetoothProtocolImpl(u2FRequest, io, optionType, handler);
                break;
            case Protocol.NFC_DEVICE_TYPE:
                protocolImpl = new NFCProtocolImpl(u2FRequest, io, optionType, handler);
                break;
            case Protocol.BLE_DEVICE_TYPE:
                protocolImpl = new BluetoothProtocolImpl(u2FRequest, io, optionType, handler);
                break;
            default:
                return null;
        }
        protocolImpl.setU2FProtocolResultCallback(callback);
        return protocolImpl;
    }
}
