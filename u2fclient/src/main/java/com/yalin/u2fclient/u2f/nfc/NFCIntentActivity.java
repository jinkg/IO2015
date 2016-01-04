package com.yalin.u2fclient.u2f.nfc;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.u2fclient.log.StatLog;
import com.yalin.u2fclient.utils.ResourceUtil;


/**
 * Created by YaLin on 2015/11/25.
 */
public class NFCIntentActivity extends AppCompatActivity {
    private static final String TAG = NFCIntentActivity.class.getSimpleName();
    private static final int DATA_START = 1;
    private static final int DATA_CANCEL = 2;

    private boolean hasError = false;

    BroadcastReceiver serviceController = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            StatLog.printLog(TAG, "nfc activity service controller received action : " + action);
            if (NFCConstants.ACTION_STOP_SERVICE.equals(action)) {
                hasError = true;
                NFCIntentActivity.this.finish();
            } else if (NFCConstants.ACTION_DATA_START.equals(action)) {
                mHandler.obtainMessage(DATA_START).sendToTarget();
            } else if (NFCConstants.ACTION_DATA_CANCEL.equals(action)) {
                mHandler.obtainMessage(DATA_CANCEL).sendToTarget();
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DATA_START:
                    tvPrompt.setText(ResourceUtil.getStringId(NFCIntentActivity.this, NFCResourceNameConstants.STRING_NFC_PROMPT_DATA_START));
                    break;
                case DATA_CANCEL:
                    tvPrompt.setText(ResourceUtil.getStringId(NFCIntentActivity.this, NFCResourceNameConstants.STRING_NFC_PROMPT_DATA_CANCEL));
                    break;
            }
        }
    };

    TextView tvPrompt;

    public static void open(Context context) {
        Intent intent = new Intent(context, NFCIntentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ResourceUtil.getLayoutId(this, NFCResourceNameConstants.LAYOUT_NFC_INTENT));
        initView();
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceController, ReceiverUtil.getNFCServiceIntentFilter());
    }

    void initView() {
        tvPrompt = (TextView) findViewById(ResourceUtil.getId(this, NFCResourceNameConstants.ID_TV_NFC_PROMPT));
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatLog.printLog(TAG, "nfc activity enableForegroundDispatch");
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        Intent intent = getIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tagIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter iso = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        adapter.enableForegroundDispatch(this, tagIntent, new IntentFilter[]{iso},
                new String[][]{new String[]{IsoDep.class.getName()}});
    }

    @Override
    protected void onPause() {
        super.onPause();
        StatLog.printLog(TAG, "nfc activity disableForegroundDispatch");
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        adapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!hasError) {
            StatLog.printLog(TAG, "nfc activity onDestroy send ACTION_USER_CANCEL reason is : " + U2FErrorCode.ERROR_USER_CANCEL);
            LocalBroadcastManager.getInstance(this).sendBroadcast(ReceiverUtil.getCancelIntent(U2FErrorCode.ERROR_USER_CANCEL));
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceController);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            StatLog.printLog(TAG, "nfc activity received nfc tag send ACTION_TAG_RECEIVED");
            LocalBroadcastManager.getInstance(this).sendBroadcast(ReceiverUtil.getSendTagIntent(tag));
        }
    }
}
