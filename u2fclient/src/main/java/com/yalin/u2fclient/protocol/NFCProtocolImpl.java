package com.yalin.u2fclient.protocol;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;

import com.yalin.u2fclient.U2FClientApi;
import com.yalin.u2fclient.domain.ClientData;
import com.yalin.u2fclient.domain.FinishRegisterData;
import com.yalin.u2fclient.domain.FinishVerifyData;
import com.yalin.u2fclient.domain.StartRegisterDeviceData;
import com.yalin.u2fclient.domain.StartVerifyDeviceData;
import com.yalin.u2fclient.domain.U2FRequest;
import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.u2fclient.log.StatLog;
import com.yalin.u2fclient.parser.ActionHandler;
import com.yalin.u2fclient.parser.NFCDataParserImpl;
import com.yalin.u2fclient.u2f.ChannelIo;
import com.yalin.u2fclient.u2f.Constants;
import com.yalin.u2fclient.u2f.nfc.ReceiverUtil;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by YaLin on 2015/11/25.
 */
public class NFCProtocolImpl implements Protocol, ChannelIo.MessageCallback, ChannelIo.ChannelStateCallback {
    private static final String TAG = NFCProtocolImpl.class.getSimpleName();

    final ChannelIo channelIo;
    final int type;
    final U2FRequest u2FRequest;
    final Handler mHandler;

    ClientData clientData;
    String clientDataString;

    Object deviceData;
    private U2FProtocolResultCallback u2FProtocolResultCallback;

    VerifyThread verifyThread;
    RegisterThread registerThread;

    private int mState = STATE_IDLE;

    public NFCProtocolImpl(U2FRequest u2FRequest, ChannelIo io, int type, Handler handler) {
        if (u2FRequest == null) {
            throw new IllegalStateException("u2fRequest can not be null");
        }
        this.u2FRequest = u2FRequest;
        this.channelIo = io;
        this.channelIo.setIoDataCallback(this);
        this.channelIo.setChannelStatCallback(this);
        this.type = type;
        this.mHandler = handler;
    }

    @Override
    public void start() {
        if (channelIo != null) {
            channelIo.connect();
        }
    }

    @Override
    public void release() {
        if (channelIo != null) {
            channelIo.stop();
        }
    }

    @Override
    public void getVersion() {

    }

    @Override
    public void register() {
        StatLog.printLog(TAG, "nfc protocol register start");
        verifyThread = null;
        registerThread = new RegisterThread(u2FRequest);
        new VersionTask().execute();
        StatLog.printLog(TAG, "nfc protocol send ACTION_DATA_START");
        LocalBroadcastManager.getInstance(U2FClientApi.getContext()).sendBroadcast(ReceiverUtil.getDataStartIntent());
    }

    @Override
    public void verify() {
        StatLog.printLog(TAG, "nfc protocol verify start");
        registerThread = null;
        verifyThread = new VerifyThread(u2FRequest);
        new VersionTask().execute();
        StatLog.printLog(TAG, "nfc protocol send ACTION_DATA_START");
        LocalBroadcastManager.getInstance(U2FClientApi.getContext()).sendBroadcast(ReceiverUtil.getDataStartIntent());
    }

    @Override
    public void doOperate() {
        switch (type) {
            case Protocol.REGISTER_OPERATE_TYPE:
                register();
                break;
            case Protocol.VERIFY_OPERATE_TYPE:
                verify();
                break;
        }
    }


    @Override
    public void setU2FProtocolResultCallback(U2FProtocolResultCallback u2FProtocolResultCallback) {
        this.u2FProtocolResultCallback = u2FProtocolResultCallback;
    }

    public synchronized void setState(int newState) {
        mState = newState;
    }

    @Override
    public void onMessage(int len, byte[] message) {
        ByteBuffer data = ByteBuffer.allocate(len);
        data.put(message, 0, len);
        new NFCDataParserImpl(nfcActionHandler).parse(data.array(), mState);
    }

    ActionHandler nfcActionHandler = new ActionHandler() {
        @Override
        public void onCheckOnlyUserNotPresence() {
            if (verifyThread != null) {
                verifyThread.notify(true);
            } else if (registerThread != null) {
                registerThread.notify(true);
            }
        }

        @Override
        public void onCheckOnlyBadKeyHandle() {
            if (verifyThread != null) {
                verifyThread.notify(false);
            } else if (registerThread != null) {
                registerThread.notify(false);
            }
        }

        @Override
        public void onDataError(int errorCode) {
            if (u2FProtocolResultCallback != null) {
                u2FProtocolResultCallback.onDataError(U2FErrorCode.ERROR_DATA_ERROR);
            }
            channelIo.setIoDataCallback(null);
        }

        @Override
        public void onVersion(String version) {
            if (verifyThread != null) {
                StatLog.printLog(TAG, "nfc protocol get version success start verify");
                verifyThread.setVersion(version);
                verifyThread.start();
            } else if (registerThread != null) {
                StatLog.printLog(TAG, "nfc protocol get version success start register");
                registerThread.setVersion(version);
                registerThread.start();
            }
        }

        @Override
        public void onDataResult(byte[] data) {
            String resultData = Base64.encodeToString(data, Base64.URL_SAFE);
            String clientDataStr = Base64.encodeToString(clientDataString.getBytes(Charset.forName("ASCII")), Base64.URL_SAFE);
            switch (type) {
                case Protocol.REGISTER_OPERATE_TYPE:
                    StatLog.printLog(TAG, "nfc protocol register success");
                    FinishRegisterData response = new FinishRegisterData(resultData, clientDataStr);
                    if (u2FProtocolResultCallback != null) {
                        u2FProtocolResultCallback.onRegisterResponse(response);
                    }
                    break;
                case Protocol.VERIFY_OPERATE_TYPE:
                    StatLog.printLog(TAG, "nfc protocol verify success");
                    if (deviceData instanceof StartVerifyDeviceData) {
                        FinishVerifyData verifyData = new FinishVerifyData(((StartVerifyDeviceData) deviceData).getKeyHandle(),
                                clientDataStr, resultData);
                        if (u2FProtocolResultCallback != null) {
                            u2FProtocolResultCallback.onVerifyResponse(verifyData);
                        }
                    }
                    break;
            }
            channelIo.setIoDataCallback(null);
        }
    };

    @Override
    public void onConnectStart() {
        StatLog.printLog(TAG, "onConnectionStart");
        mHandler.obtainMessage(Constants.MESSAGE_CONNECT_START).sendToTarget();
    }

    @Override
    public void onConnect(String deviceName) {
        StatLog.printLog(TAG, "onConnect");
        doOperate();
        mHandler.obtainMessage(Constants.MESSAGE_CONNECT, deviceName).sendToTarget();
    }

    @Override
    public void connectFailed(int error) {
        mHandler.obtainMessage(Constants.MESSAGE_CONNECT_FAILED, error, 0).sendToTarget();
    }

    @Override
    public void onConnectLost() {
        StatLog.printLog(TAG, "onConnectionLost");
        mHandler.obtainMessage(Constants.MESSAGE_CONNECT_LOST).sendToTarget();
    }

    class VersionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            StatLog.printLog(TAG, "nfc protocol get version");
            setState(STATE_GET_VERSION);
            int totalLen = 5;

            byte[] apdu = new byte[totalLen];
            ByteBuffer.wrap(apdu)
                    .put((byte) 0x00)
                    .put((byte) 0x03)       //ins
                    .put((byte) 0x03)       //p1
                    .put((byte) 0x00);
            channelIo.write(apdu);
            return null;
        }
    }

    class VerifyThread extends Thread {
        U2FRequest verifyRequest;
        boolean keyHandleExist = false;
        String version;

        VerifyThread(U2FRequest startVerifyDeviceList) {
            verifyRequest = startVerifyDeviceList;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public synchronized void notify(boolean keyHandleExist) {
            this.keyHandleExist = keyHandleExist;
        }

        @Override
        public synchronized void run() {
            StatLog.printLog(TAG, "nfc protocol start check only");
            setState(STATE_CHECK_ONLY);
            StartVerifyDeviceData startVerifyDeviceData = null;
            for (StartVerifyDeviceData verifyDeviceData : verifyRequest.getAuthenticateRequests()) {
                if (!TextUtils.equals(version, verifyDeviceData.getVersion())) {
                    continue;
                }
                clientData = new ClientData(VERIFY_TYPE, verifyDeviceData.getChallenge(), FACET_ID);
                clientDataString = clientData.toJsonString();
                byte[] appParam = DigestUtils.sha256(verifyDeviceData.getAppId());
                byte[] clientParam = DigestUtils.sha256(clientDataString);
                byte[] kh = Base64.decode(verifyDeviceData.getKeyHandle(), Base64.URL_SAFE);

                int totalLen = 5 + 32 + 32 + 1 + kh.length + 1;

                byte[] apdu = new byte[totalLen];
                ByteBuffer.wrap(apdu)
                        .put((byte) 0x00)
                        .put((byte) 0x02)       //ins
                        .put((byte) 0x07)       //p1
                        .put((byte) 0x00)
                        .put((byte) (64 + 1 + kh.length))          //length
                        .put(clientParam)
                        .put(appParam)
                        .put((byte) kh.length)
                        .put(kh)
                        .put((byte) 0xff);

                channelIo.write(apdu);
                if (keyHandleExist) {
                    startVerifyDeviceData = verifyDeviceData;
                    break;
                }
            }
            if (startVerifyDeviceData != null) {
                StatLog.printLog(TAG, "nfc protocol start verify");
                deviceData = startVerifyDeviceData;
                new VerifyTask(startVerifyDeviceData).execute();
            } else {
                if (u2FProtocolResultCallback != null) {
                    u2FProtocolResultCallback.onDataError(U2FErrorCode.ERROR_USER_NOT_REGISTER);
                }
            }
        }
    }


    class RegisterThread extends Thread {
        U2FRequest startRegisterRequest;
        boolean keyHandleExist = false;
        String version;

        RegisterThread(U2FRequest startVerifyDeviceList) {
            startRegisterRequest = startVerifyDeviceList;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public synchronized void notify(boolean keyHandleExist) {
            this.keyHandleExist = keyHandleExist;
        }

        @Override
        public synchronized void run() {
            StatLog.printLog(TAG, "nfc protocol start check only");
            setState(STATE_CHECK_ONLY);
            StartRegisterDeviceData data = startRegisterRequest.getRegisterRequests().get(0);
            if (!TextUtils.equals(version, data.getVersion())) {
                if (u2FProtocolResultCallback != null) {
                    u2FProtocolResultCallback.onDataError(U2FErrorCode.ERROR_DEVICE_VERSION_NOT_SUPPORT);
                }
                return;
            }
            StartVerifyDeviceData startVerifyDeviceData = null;
            if (startRegisterRequest.getAuthenticateRequests() != null) {
                for (StartVerifyDeviceData verifyDeviceData : startRegisterRequest.getAuthenticateRequests()) {
                    if (!TextUtils.equals(version, verifyDeviceData.getVersion())) {
                        continue;
                    }
                    clientData = new ClientData(VERIFY_TYPE, verifyDeviceData.getChallenge(), FACET_ID);
                    clientDataString = clientData.toJsonString();
                    byte[] appParam = DigestUtils.sha256(verifyDeviceData.getAppId());
                    byte[] clientParam = DigestUtils.sha256(clientDataString);
                    byte[] kh = Base64.decode(verifyDeviceData.getKeyHandle(), Base64.URL_SAFE);

                    int totalLen = 5 + 32 + 32 + 1 + kh.length + 1;

                    byte[] apdu = new byte[totalLen];
                    ByteBuffer.wrap(apdu)
                            .put((byte) 0x00)
                            .put((byte) 0x02)       //ins
                            .put((byte) 0x07)       //p1
                            .put((byte) 0x00)
                            .put((byte) (64 + 1 + kh.length))          //length
                            .put(clientParam)
                            .put(appParam)
                            .put((byte) kh.length)
                            .put(kh)
                            .put((byte) 0xff);

                    channelIo.write(apdu);
                    if (keyHandleExist) {
                        startVerifyDeviceData = verifyDeviceData;
                        break;
                    }
                }
            }
            if (startVerifyDeviceData != null) {
                if (u2FProtocolResultCallback != null) {
                    u2FProtocolResultCallback.onDataError(U2FErrorCode.ERROR_USER_REGISTERED);
                }
            } else {
                StatLog.printLog(TAG, "nfc protocol start register");
                deviceData = data;
                new RegisterTask(data).execute();
            }
        }
    }

    class RegisterTask extends AsyncTask<Void, Void, Void> {
        StartRegisterDeviceData registerDeviceData;

        RegisterTask(StartRegisterDeviceData startRegisterDeviceData) {
            registerDeviceData = startRegisterDeviceData;
        }

        @Override
        protected Void doInBackground(Void... params) {
            setState(STATE_REQUEST_REGISTER);
            deviceData = registerDeviceData;
            clientData = new ClientData(ENROLL_TYPE, registerDeviceData.getChallenge(), FACET_ID);
            clientDataString = clientData.toJsonString();
            byte[] appParam = DigestUtils.sha256(registerDeviceData.getAppId());
            byte[] clientParam = DigestUtils.sha256(clientDataString);

            byte[] apdu = new byte[5 + 32 + 32 + 1];
            ByteBuffer.wrap(apdu)
                    .put((byte) 0x00)
                    .put((byte) 0x01)   //ins
                    .put((byte) 0x03)   //p1
                    .put((byte) 0x00)
                    .put((byte) 64)     //length
                    .put(clientParam)
                    .put(appParam)
                    .put((byte) 0xff);  //256 byte response

            channelIo.write(apdu);
            return null;
        }
    }

    class VerifyTask extends AsyncTask<Void, Void, Void> {
        StartVerifyDeviceData verifyDeviceData;

        VerifyTask(StartVerifyDeviceData startVerifyDevice) {
            verifyDeviceData = startVerifyDevice;
        }

        @Override
        protected Void doInBackground(Void... params) {
            setState(STATE_REQUEST_VERIFY);
            deviceData = verifyDeviceData;
            clientData = new ClientData(VERIFY_TYPE, verifyDeviceData.getChallenge(), FACET_ID);
            clientDataString = clientData.toJsonString();
            byte[] appParam = DigestUtils.sha256(verifyDeviceData.getAppId());
            byte[] clientParam = DigestUtils.sha256(clientDataString);
            byte[] kh = Base64.decode(verifyDeviceData.getKeyHandle(), Base64.URL_SAFE);

            int totalLen = 5 + 32 + 32 + 1 + kh.length + 1;

            byte[] apdu = new byte[totalLen];
            ByteBuffer.wrap(apdu)
                    .put((byte) 0x00)
                    .put((byte) 0x02)       //ins
                    .put((byte) 0x03)       //p1
                    .put((byte) 0x00)
                    .put((byte) (64 + 1 + kh.length))          //length
                    .put(clientParam)
                    .put(appParam)
                    .put((byte) kh.length)
                    .put(kh);

            channelIo.write(apdu);
            return null;
        }
    }

}
