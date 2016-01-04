package com.yalin.u2fclient.u2f.nfc;

import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;

/**
 * Created by YaLin on 2015/11/25.
 */
public class ReceiverUtil {
    public static final String TAG_KEY = "tag";
    public static final String CANCEL_REASON_KEY = "reason";

    public static Intent getStopServiceIntent() {
        Intent intent = new Intent(NFCConstants.ACTION_STOP_SERVICE);
        return intent;
    }

    public static Intent getDataStartIntent() {
        Intent intent = new Intent(NFCConstants.ACTION_DATA_START);
        return intent;
    }

    public static Intent getDataCancelIntent() {
        Intent intent = new Intent(NFCConstants.ACTION_DATA_CANCEL);
        return intent;
    }


    public static Intent getSendTagIntent(Tag tag) {
        Intent intent = new Intent(NFCConstants.ACTION_TAG_RECEIVED);
        intent.putExtra(TAG_KEY, tag);
        return intent;
    }

    public static Intent getCancelIntent(int reason) {
        Intent intent = new Intent(NFCConstants.ACTION_USER_CANCEL);
        intent.putExtra(CANCEL_REASON_KEY, reason);
        return intent;
    }

    public static IntentFilter getNFCServiceIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NFCConstants.ACTION_STOP_SERVICE);
        intentFilter.addAction(NFCConstants.ACTION_DATA_START);
        intentFilter.addAction(NFCConstants.ACTION_DATA_CANCEL);
        return intentFilter;
    }
}
