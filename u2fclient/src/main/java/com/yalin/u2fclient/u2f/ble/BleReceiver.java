package com.yalin.u2fclient.u2f.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * Created by YaLin on 2015/11/19.
 */
public class BleReceiver extends BroadcastReceiver {
    public interface BluetoothStatCallback {
        void onBluetoothOpen();
    }

    private BluetoothStatCallback mCallback;

    public BleReceiver(BluetoothStatCallback statCallback) {
        mCallback = statCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            if (BluetoothAdapter.STATE_ON == intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                if (mCallback != null) {
                    mCallback.onBluetoothOpen();
                }
            }
        }
    }
}
