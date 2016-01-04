package com.yalin.u2fclient.u2f.bt;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yalin.u2fclient.log.StatLog;


/**
 * Created by YaLin on 2015/11/19.
 */
public class BluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = BluetoothReceiver.class.getSimpleName();

    public interface BluetoothStatCallback {
        void onBluetoothOpen();
    }

    private BluetoothStatCallback mCallback;

    public BluetoothReceiver(BluetoothStatCallback statCallback) {
        mCallback = statCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        StatLog.printLog(TAG, "bluetooth receiver received action : " + action);
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            if (BluetoothAdapter.STATE_ON == intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                if (mCallback != null) {
                    mCallback.onBluetoothOpen();
                }
            }
        }
    }
}
