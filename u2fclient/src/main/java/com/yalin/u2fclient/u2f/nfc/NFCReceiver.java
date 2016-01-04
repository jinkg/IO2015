package com.yalin.u2fclient.u2f.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;

import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.u2fclient.log.StatLog;

/**
 * Created by YaLin on 2015/11/25.
 */
public class NFCReceiver extends BroadcastReceiver {
    private static final String TAG = NFCReceiver.class.getSimpleName();

    public interface NFCStatCallback {
        void onTagReceived(Tag tag);

        void onUserCancel(int reason);
    }

    private NFCStatCallback mCallback;

    public NFCReceiver(NFCStatCallback callback) {
        mCallback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        StatLog.printLog(TAG, "nfc receiver received action : " + action);
        if (NFCConstants.ACTION_TAG_RECEIVED.equals(action)) {
            Tag tag = intent.getParcelableExtra(ReceiverUtil.TAG_KEY);
            if (mCallback != null) {
                mCallback.onTagReceived(tag);
            }
        } else if (NFCConstants.ACTION_USER_CANCEL.equals(action)) {
            if (mCallback != null) {
                int reason = intent.getIntExtra(ReceiverUtil.CANCEL_REASON_KEY, U2FErrorCode.ERROR_USER_CANCEL);
                mCallback.onUserCancel(reason);
            }
        }
    }
}
