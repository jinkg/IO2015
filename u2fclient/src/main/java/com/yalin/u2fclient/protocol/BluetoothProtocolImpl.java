package com.yalin.u2fclient.protocol;

import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;

import com.yalin.u2fclient.log.StatLog;
import com.yalin.u2fclient.parser.ActionHandler;
import com.yalin.u2fclient.domain.ClientData;
import com.yalin.u2fclient.domain.FinishRegisterData;
import com.yalin.u2fclient.domain.FinishVerifyData;
import com.yalin.u2fclient.domain.StartRegisterDeviceData;
import com.yalin.u2fclient.domain.StartVerifyDeviceData;
import com.yalin.u2fclient.domain.U2FRequest;
import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.u2fclient.parser.QKeyDataParserImpl;
import com.yalin.u2fclient.u2f.ChannelIo;
import com.yalin.u2fclient.u2f.Constants;


import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by YaLin on 2015/12/7.
 */
public class BluetoothProtocolImpl implements Protocol, ChannelIo.MessageCallback, ChannelIo.ChannelStateCallback {

    public static final String TAG = BluetoothProtocolImpl.class.getSimpleName();

    public U2FProtocolResultCallback u2FProtocolResultCallback;

    ClientData clientData;
    String clientDataString;

    ChannelIo channelIo;

    Object request;

    final int type;
    final U2FRequest u2FRequest;
    final Handler mHandler;

    VerifyThread verifyThread;
    RegisterThread registerThread;

    private int mState = STATE_IDLE;

    public BluetoothProtocolImpl(U2FRequest u2FRequest, ChannelIo channelIo, int type, Handler handler) {
        if (u2FRequest == null) {
            throw new IllegalStateException("u2fRequest can not be null");
        }
        this.u2FRequest = u2FRequest;
        this.channelIo = channelIo;
        this.channelIo.setIoDataCallback(this);
        this.channelIo.setChannelStatCallback(this);
        this.type = type;
        this.mHandler = handler;
    }

    @Override
    public void setU2FProtocolResultCallback(U2FProtocolResultCallback u2FProtocolResultCallback) {
        this.u2FProtocolResultCallback = u2FProtocolResultCallback;
    }

    public synchronized void setState(int newState) {
        StatLog.printLog(TAG, "bluetooth protocol set state  : " + newState);
        mState = newState;
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
        StatLog.printLog(TAG, "bluetooth protocol register start get version");
        verifyThread = null;
        registerThread = new RegisterThread(u2FRequest);
        new VersionTask().execute();
    }

    @Override
    public void verify() {
        StatLog.printLog(TAG, "bluetooth protocol verify start get version");
        registerThread = null;
        verifyThread = new VerifyThread(u2FRequest);
        new VersionTask().execute();
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

    private void sendMessage(byte[] message) {
        if (channelIo.getState() != ChannelIo.STATE_CONNECTED) {
            return;
        }
        StatLog.printLog(TAG, "bluetooth protocol send message : " + new String(Hex.encodeHex(message)));
        if (message.length > 0) {
            channelIo.write(message);
        }
    }

    @Override
    public void onMessage(int len, byte[] message) {
        StatLog.printLog(TAG, "bluetooth protocol onMessage : " + new String(Hex.encodeHex(message)));
        ByteBuffer data = ByteBuffer.allocate(len);
        data.put(message, 0, len);
        new QKeyDataParserImpl(bluetoothActionHandler).parse(data.array(), mState);
    }

    ActionHandler bluetoothActionHandler = new ActionHandler() {
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
                u2FProtocolResultCallback.onDataError(errorCode);
            }
            channelIo.setIoDataCallback(null);
        }

        @Override
        public void onVersion(String version) {
            if (verifyThread != null) {
                StatLog.printLog(TAG, "bluetooth protocol get version success start verify");
                verifyThread.setVersion(version);
                verifyThread.start();
            } else if (registerThread != null) {
                StatLog.printLog(TAG, "bluetooth protocol get version success start register");
                registerThread.setVersion(version);
                registerThread.start();
            }
        }

        @Override
        public void onDataResult(byte[] data) {
            if (u2FProtocolResultCallback != null) {
                u2FProtocolResultCallback.onActionResult();
            }
            String resultData = Base64.encodeToString(data, 3, data.length - 5, Base64.URL_SAFE);
            String clientDataStr = Base64.encodeToString(clientDataString.getBytes(Charset.forName("ASCII")), Base64.URL_SAFE);
            switch (type) {
                case REGISTER_OPERATE_TYPE:
                    StatLog.printLog(TAG, "bluetooth protocol register success");
                    FinishRegisterData response = new FinishRegisterData(resultData, clientDataStr);
                    if (u2FProtocolResultCallback != null) {
                        u2FProtocolResultCallback.onRegisterResponse(response);
                    }
                    break;
                case VERIFY_OPERATE_TYPE:
                    StatLog.printLog(TAG, "bluetooth protocol verify success");
                    if (request instanceof StartVerifyDeviceData) {
                        FinishVerifyData verifyData = new FinishVerifyData(((StartVerifyDeviceData) request).getKeyHandle(),
                                clientDataStr, resultData);
                        if (u2FProtocolResultCallback != null) {
                            u2FProtocolResultCallback.onVerifyResponse(verifyData);
                        }
                    }
                    break;
            }
            setState(STATE_IDLE);
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
            setState(STATE_GET_VERSION);
            byte[] apdu = new byte[8];
            ByteBuffer.wrap(apdu)
                    .put((byte) 0x83)   //CMD
                    .put((byte) 0)      //HLEN
                    .put((byte) 0x05)   //LLEN
                    .put((byte) 0)      //CLA
                    .put((byte) 0x60)   //INS
                    .put((byte) 0x00)   //P1
                    .put((byte) 0x00)   //P2
                    .put((byte) 0x00);
            sendMessage(apdu);
            return null;
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
            clientData = new ClientData(ENROLL_TYPE, registerDeviceData.getChallenge(), FACET_ID);
            clientDataString = clientData.toJsonString();
            byte[] appParam = DigestUtils.sha256(registerDeviceData.getAppId());
            byte[] clientParam = DigestUtils.sha256(clientDataString);

            byte[] apdu = new byte[10 + 32 + 32];
            ByteBuffer.wrap(apdu)
                    .put((byte) 0x83)   //CMD
                    .put((byte) 0)      //HLEN
                    .put((byte) 0x47)   //LLEN
                    .put((byte) 0)      //CLA
                    .put((byte) 0x20)   //INS
                    .put((byte) 0x00)   //P1
                    .put((byte) 0x00)   //P2
                    .put((byte) 0x00)   //LC1
                    .put((byte) 0x00)   //LC2
                    .put((byte) 0x40)   //LC3
                    .put(clientParam)
                    .put(appParam);
            sendMessage(apdu);
            if (u2FProtocolResultCallback != null) {
                u2FProtocolResultCallback.onUserAction();
            }
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
            clientData = new ClientData(VERIFY_TYPE, verifyDeviceData.getChallenge(), FACET_ID);
            clientDataString = clientData.toJsonString();
            byte[] appParam = DigestUtils.sha256(verifyDeviceData.getAppId());
            byte[] clientParam = DigestUtils.sha256(clientDataString);
            byte[] kh = Base64.decode(verifyDeviceData.getKeyHandle(), Base64.URL_SAFE);

            int totalLen = 10 + 32 + 32 + 1 + kh.length;
            int dataLen = totalLen - 3;
            byte[] dataLenByte = intToByteArray(dataLen);
            int cmdDataLen = totalLen - 10;
            byte[] cmdDataLenByte = intToByteArray(cmdDataLen);
            byte cla = (byte) 0x00;
            byte[] apdu = new byte[totalLen];
            ByteBuffer.wrap(apdu)
                    .put((byte) 0x83)       //CMD
                    .put(dataLenByte[2])    //HLEN
                    .put(dataLenByte[3])    //LLEN
                    .put(cla)          //CLA
                    .put((byte) 0x40)       //INS
                    .put((byte) 0x03)       //P1
                    .put((byte) 0x00)       //P2
                    .put((byte) 0x00)       //LC1
                    .put(cmdDataLenByte[2]) //LC2
                    .put(cmdDataLenByte[3]) //LC3
                    .put(clientParam)
                    .put(appParam)
                    .put((byte) kh.length)
                    .put(kh);

            sendMessage(apdu);
            if (u2FProtocolResultCallback != null) {
                u2FProtocolResultCallback.onUserAction();
            }
            return null;
        }
    }

    class TransactionTask extends VerifyTask {

        TransactionTask(StartVerifyDeviceData startVerifyDevice) {
            super(startVerifyDevice);
        }

        @Override
        protected Void doInBackground(Void... params) {
            setState(STATE_REQUEST_VERIFY);
            clientData = new ClientData(VERIFY_TYPE, verifyDeviceData.getChallenge(), FACET_ID);
            byte[] challengeBytes = Base64.decode(clientData.challenge, Base64.URL_SAFE);
            byte[] priceBytes = new byte[8];
            ByteBuffer.wrap(priceBytes)
                    .put(challengeBytes, 0, 8);
            clientDataString = clientData.toJsonString();
            byte[] appParam = DigestUtils.sha256(verifyDeviceData.getAppId());
            byte[] clientParam = DigestUtils.sha256(clientDataString);
            ByteBuffer.wrap(clientParam)
                    .put(priceBytes);

            byte[] kh = Base64.decode(verifyDeviceData.getKeyHandle(), Base64.URL_SAFE);

            int totalLen = 10 + 32 + 32 + 1 + kh.length;
            int dataLen = totalLen - 3;
            byte[] dataLenByte = intToByteArray(dataLen);
            int cmdDataLen = totalLen - 10;
            byte[] cmdDataLenByte = intToByteArray(cmdDataLen);
            byte cla = (byte) 0x80;
            byte[] apdu = new byte[totalLen];
            ByteBuffer.wrap(apdu)
                    .put((byte) 0x83)       //CMD
                    .put(dataLenByte[2])    //HLEN
                    .put(dataLenByte[3])    //LLEN
                    .put(cla)          //CLA
                    .put((byte) 0x40)       //INS
                    .put((byte) 0x03)       //P1
                    .put((byte) 0x00)       //P2
                    .put((byte) 0x00)       //LC1
                    .put(cmdDataLenByte[2]) //LC2
                    .put(cmdDataLenByte[3]) //LC3
                    .put(clientParam)
                    .put(appParam)
                    .put((byte) kh.length)
                    .put(kh);

            sendMessage(apdu);
            if (u2FProtocolResultCallback != null) {
                u2FProtocolResultCallback.onUserAction();
            }
            return null;
        }
    }

    class VerifyThread extends Thread {
        U2FRequest startVerifyRequest;
        boolean keyHandleExist = false;
        String version;

        VerifyThread(U2FRequest startVerifyDeviceList) {
            startVerifyRequest = startVerifyDeviceList;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public synchronized void notify(boolean keyHandleExist) {
            this.keyHandleExist = keyHandleExist;
            notify();
        }

        @Override
        public synchronized void run() {
            StatLog.printLog(TAG, "bluetooth protocol verify start check only");
            setState(STATE_CHECK_ONLY);
            StartVerifyDeviceData startVerifyDeviceData = null;
            for (StartVerifyDeviceData verifyDeviceData : startVerifyRequest.getAuthenticateRequests()) {
                if (!TextUtils.equals(version, verifyDeviceData.getVersion())) {
                    continue;
                }
                clientData = new ClientData(VERIFY_TYPE, verifyDeviceData.getChallenge(), FACET_ID);
                clientDataString = clientData.toJsonString();
                byte[] appParam = DigestUtils.sha256(verifyDeviceData.getAppId());
                byte[] clientParam = DigestUtils.sha256(clientDataString);
                byte[] kh = Base64.decode(verifyDeviceData.getKeyHandle(), Base64.URL_SAFE);

                int totalLen = 10 + 32 + 32 + 1 + kh.length;
                int dataLen = totalLen - 3;
                byte[] dataLenByte = intToByteArray(dataLen);
                int cmdDataLen = totalLen - 10;
                byte[] cmdDataLenByte = intToByteArray(cmdDataLen);

                byte[] apdu = new byte[totalLen];
                ByteBuffer.wrap(apdu)
                        .put((byte) 0x83)       //CMD
                        .put(dataLenByte[2])    //HLEN
                        .put(dataLenByte[3])    //LLEN
                        .put((byte) 0)          //CLA
                        .put((byte) 0x40)       //INS
                        .put((byte) 0x07)       //P1
                        .put((byte) 0x00)       //P2
                        .put((byte) 0x00)       //LC1
                        .put(cmdDataLenByte[2]) //LC2
                        .put(cmdDataLenByte[3]) //LC3
                        .put(clientParam)
                        .put(appParam)
                        .put((byte) kh.length)
                        .put(kh);

                sendMessage(apdu);
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (keyHandleExist) {
                    startVerifyDeviceData = verifyDeviceData;
                    break;
                }
            }
            if (startVerifyDeviceData != null) {
                request = startVerifyDeviceData;
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
            notify();
        }

        @Override
        public synchronized void run() {
            StartRegisterDeviceData data = startRegisterRequest.getRegisterRequests().get(0);
            if (!TextUtils.equals(version, data.getVersion())) {
                if (u2FProtocolResultCallback != null) {
                    u2FProtocolResultCallback.onDataError(U2FErrorCode.ERROR_DEVICE_VERSION_NOT_SUPPORT);
                }
            }
            StatLog.printLog(TAG, "bluetooth protocol register start check only");
            setState(STATE_CHECK_ONLY);
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

                    int totalLen = 10 + 32 + 32 + 1 + kh.length;
                    int dataLen = totalLen - 3;
                    byte[] dataLenByte = intToByteArray(dataLen);
                    int cmdDataLen = totalLen - 10;
                    byte[] cmdDataLenByte = intToByteArray(cmdDataLen);

                    byte[] apdu = new byte[totalLen];
                    ByteBuffer.wrap(apdu)
                            .put((byte) 0x83)       //CMD
                            .put(dataLenByte[2])    //HLEN
                            .put(dataLenByte[3])    //LLEN
                            .put((byte) 0)          //CLA
                            .put((byte) 0x40)       //INS
                            .put((byte) 0x07)       //P1
                            .put((byte) 0x00)       //P2
                            .put((byte) 0x00)       //LC1
                            .put(cmdDataLenByte[2]) //LC2
                            .put(cmdDataLenByte[3]) //LC3
                            .put(clientParam)
                            .put(appParam)
                            .put((byte) kh.length)
                            .put(kh);

                    sendMessage(apdu);
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
                request = data;
                new RegisterTask(data).execute();
            }
        }
    }

    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }
}

