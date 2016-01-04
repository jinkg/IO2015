package com.yalin.u2fclient.u2f.nfc;

import android.content.Context;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.support.v4.content.LocalBroadcastManager;

import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.u2fclient.parser.NFCDataParserImpl;
import com.yalin.u2fclient.u2f.ChannelIo;


import java.io.IOException;

/**
 * Created by YaLin on 2015/11/25.
 */
public class NFCChannelIo implements ChannelIo, NFCReceiver.NFCStatCallback {
    private static final String TAG = NFCChannelIo.class.getSimpleName();

    private static final byte[] SELECT_COMMAND = {0x00, (byte) 0xa4, 0x04, 0x00, 0x08, (byte) 0xa0, 0x00, 0x00, 0x06, 0x47, 0x2f, 0x00, 0x01};
    private static final byte[] SELECT_COMMAND_YUBICO = {0x00, (byte) 0xa4, 0x04, 0x00, 0x07, (byte) 0xa0, 0x00, 0x00, 0x05, 0x27, 0x10, 0x02};
    private static final byte[] GET_RESPONSE_COMMAND = {0x00, (byte) 0xc0, 0x00, 0x00, (byte) 0xff};

    private Context mContext;
    private NfcAdapter mAdapter;
    protected ChannelStateCallback stateCallback;
    private IsoDep isoTag;
    private MessageCallback mMessageCallback;

    private ConnectThread mConnectThread;
    private int mState;
    private NFCReceiver receiver = new NFCReceiver(this);

    public NFCChannelIo(Context context) {
        mContext = context;
        mAdapter = NfcAdapter.getDefaultAdapter(mContext);
    }

    @Override
    public void reset() {
        if (isoTag != null) {
            try {
                isoTag.close();
            } catch (IOException e) {

            }
        }
//        StatLog.printLog(TAG, "nfc io  reset");
        setState(STATE_LISTEN);
    }

    @Override
    public synchronized void connect() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(NFCConstants.ACTION_TAG_RECEIVED);
        filter.addAction(NFCConstants.ACTION_USER_CANCEL);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, filter);
//        StatLog.printLog(TAG, "nfc io register NFCReceiver");
        if (!nfcStart()) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
        }
    }

    @Override
    public synchronized void stop() {
        try {
//            StatLog.printLog(TAG, "nfc io unregister NFCReceiver");
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
//            StatLog.printLog(TAG, "nfc io send ACTION_STOP_SERVICE broadcast");
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(ReceiverUtil.getStopServiceIntent());
        } catch (Exception e) {
        }

        mContext = null;
        setState(STATE_NONE);
    }

    @Override
    public void setChannelStatCallback(ChannelStateCallback callback) {
        stateCallback = callback;
    }

    @Override
    public void setIoDataCallback(MessageCallback messageCallback) {
        mMessageCallback = messageCallback;
    }

    private synchronized void setState(int state) {
//        StatLog.printLog(TAG, "nfc io state changed : " + state);
        mState = state;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public void write(byte[] apdu) {
        try {
//            StatLog.printLog(TAG, "nfc io write data : " + new String(Hex.encodeHex(apdu)));
            byte[] data = send(apdu);
            if (mMessageCallback != null && data != null) {
                mMessageCallback.onMessage(data.length, data);
            }
        } catch (Exception e) {
//            StatLog.printLog(TAG, "nfc io write data error : " + e.getMessage());
//            StatLog.printLog(TAG, "nfc io write data error send ACTION_DATA_CANCEL broadcast");
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(ReceiverUtil.getDataCancelIntent());
        }

    }

    private byte[] send(byte[] apdu) throws IOException, NFCAPDUError {
        if (!isoTag.isConnected()) {
            return null;
        }
        byte[] cmd = apdu;
        int status = 0x6100;
        byte[] data = new byte[0];

        while ((status & 0xff00) == 0x6100) {
            byte[] resp = isoTag.transceive(cmd);
            status = ((0xff & resp[resp.length - 2]) << 8) | (0xff & resp[resp.length - 1]);
            data = concat(data, resp, resp.length - 2);
            cmd = GET_RESPONSE_COMMAND;
        }

        if (status != 0x9000) {
//            StatLog.printLog(TAG, "nfc io send error status : " + status);
            if (status == 0x6a82) {
                throw new NFCAPDUError(status);
            }
            if (mMessageCallback != null) {
                if (status == 0x6985) {
                    mMessageCallback.onMessage(1, new byte[]{NFCDataParserImpl.DATA_CHECK_ONLY_SUCCESS_TAG});
                } else if (status == 0x6a80) {
                    mMessageCallback.onMessage(1, new byte[]{NFCDataParserImpl.DATA_CHECK_ONLY_FAILED_TAG});
                } else {
                    mMessageCallback.onMessage(1, new byte[]{NFCDataParserImpl.DATA_ERROR_TAG});
                }
            }
            return null;
        }

        return data;
    }

    private boolean nfcStart() {
        if (mAdapter == null) {
            connectionFailed(U2FErrorCode.ERROR_NFC_NOT_AVAILABLE);
            return false;
        } else if (mAdapter.isEnabled()) {
//            StatLog.printLog(TAG, "nfc io open nfc activity");
            NFCIntentActivity.open(mContext);
            if (stateCallback != null) {
                stateCallback.onConnectStart();
            }
            return true;
        } else {
            connectionFailed(U2FErrorCode.ERROR_NFC_NOT_OPENED);
            return false;
        }
    }

    private void onConnect(String deviceName) {
//        StatLog.printLog(TAG, "nfc io connect : " + deviceName);
        if (stateCallback != null) {
            stateCallback.onConnect(deviceName);
        }
    }

    private void connectionFailed(int error) {
        if (stateCallback != null) {
            stateCallback.connectFailed(error);
        }
//        StatLog.printLog(TAG, "nfc connect failed : " + error);
        reset();
    }

    private void connectionLost() {
        if (stateCallback != null) {
            stateCallback.onConnectLost();
        }
//        StatLog.printLog(TAG, "nfc connect lost");
        reset();
    }

    @Override
    public void onTagReceived(Tag tag) {
        isoTag = IsoDep.get(tag);
//        StatLog.printLog(TAG, "nfc onTagReceived");
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        mConnectThread = new ConnectThread(isoTag);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    @Override
    public void onUserCancel(int reason) {
//        StatLog.printLog(TAG, "nfc io user cancel reason is : " + reason);
        if (reason == U2FErrorCode.ERROR_USER_CANCEL) {
            connectionLost();
        } else {
            connectionFailed(reason);
        }
    }

    private static byte[] concat(byte[] a, byte[] b, int length) {
        byte[] res = new byte[a.length + length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, length);
        return res;
    }

    private class ConnectThread extends Thread {
        private IsoDep mTag;

        public ConnectThread(IsoDep tag) {
            mTag = tag;
        }

        public void run() {
            mTag.setTimeout(5000);
            try {
//                StatLog.printLog(TAG, "nfc io connect tag");
                mTag.connect();
                send(SELECT_COMMAND);
                onConnect(mTag.toString());
            } catch (NFCAPDUError e) {
//                StatLog.printLog(TAG, "nfc io connect tag error : " + e.getCode());
                try {
                    if (e.getCode() == 0x6a82) {
//                        StatLog.printLog(TAG, "nfc io connect tag error code == 0x6a82 send SELECT_COMMAND_YUBICO");
                        send(SELECT_COMMAND_YUBICO);
                        onConnect(mTag.toString());
                    }
                } catch (Exception e1) {
                    connectionFailed(U2FErrorCode.ERROR_NFC_CONNECT_ERROR);
                }
            } catch (IOException e) {
                connectionFailed(U2FErrorCode.ERROR_NFC_CONNECT_ERROR);
            }
        }

        public void cancel() {
            try {
                mTag.close();
            } catch (IOException e) {
            }
        }
    }
}
