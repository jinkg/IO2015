package com.yalin.u2fclient.u2f.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;


import com.yalin.u2fclient.log.StatLog;

import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BleTransmitService extends Service {
    private final static String TAG = BleTransmitService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_SENDING = 3;
    public static final int STATE_READING = 4;
    public static final int STATE_READ_PENDING = 5;
    public static final int STATE_SEND_PENDING = 6;

    public final static String ACTION_GATT_CONNECTED =
            "com.idsmanager.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.idsmanager.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.idsmanager.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.idsmanager.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.idsmanager.bluetooth.le.EXTRA_DATA";
    public final static String ACTION_GATT_CONNECT_FAILED =
            "com.idsmanager.bluetooth.le.ACTION_GATT_CONNECT_FAILED";
    public final static String ACTION_GATT_CONNECT_READY =
            "com.idsmanager.bluetooth.le.ACTION_GATT_CONNECT_READY";
    public final static String ACTION_GATT_WRITE_ERROR =
            "com.idsmanager.bluetooth.le.ACTION_TASK_COMPLETE";
    public final static String ACTION_GATT_READ_ERROR =
            "com.idsmanager.bluetooth.le.ACTION_GATT_READ_ERROR";
    public final static String ACTION_GATT_READ_PENDING =
            "com.idsmanager.bluetooth.le.ACTION_GATT_READ_PENDING";
    public final static String ACTION_GATT_READING =
            "com.idsmanager.bluetooth.le.ACTION_GATT_READING";
    public final static String ACTION_GATT_ON_USER_ACTION =
            "com.idsmanager.bluetooth.le.ACTION_GATT_ON_USER_ACTION";
    public final static String ACTION_GATT_ON_ACTION_RESULT =
            "com.idsmanager.bluetooth.le.ACTION_GATT_ON_ACTION_RESULT";

    public static final UUID QKEY_SERVICE_UUID = UUID
            .fromString("4649444F-2E55-3246-2E42-542E4C452E51");
    public static final UUID QKEY_CHAR_UUID = UUID
            .fromString("75324350-DC65-4690-AE7F-9D54E0059C86");
    public static final UUID QKEY_NOTIFY_UUID = UUID
            .fromString("75325354-E94E-430C-A438-A8ED46016D93");

    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MILL = 500;
    private Queue<byte[]> dataQueue = new LinkedList<>();
    private byte[] lastPacket;
    private byte lastConfirm;
    private int resendTimes = 0;

    private byte[] receivedBytes;
    private ByteBuffer receivedByteBuffer;

    private BluetoothGattService rxService;
    private BluetoothGattCharacteristic rxChar;

    private Timer mTimer;
    private static final long WAIT_CONFIRM_TIMEOUT = 1000;
    private static final long WAIT_DATA_TIMEOUT = 1000;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            StatLog.printLog(TAG, "onConnectionStateChange status = " + status + " newState = " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    intentAction = ACTION_GATT_CONNECT_FAILED;
                    mConnectionState = STATE_DISCONNECTED;
                    broadcastReceivedData(intentAction);
                    mBluetoothGatt.disconnect();
                    return;
                }
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastReceivedData(intentAction);
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastReceivedData(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            boolean isSuccess = false;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                rxService = mBluetoothGatt.getService(QKEY_SERVICE_UUID);
                if (rxService != null) {
                    rxChar = rxService.getCharacteristic(QKEY_CHAR_UUID);
                    if (rxChar != null) {
                        isSuccess = true;
                    }
                }
                if (isSuccess) {
                    StatLog.printLog(TAG, "onServicesDiscovered success, enable read notification!");
                    enableReadCharacteristicNotification();
                    broadcastReceivedData(ACTION_GATT_SERVICES_DISCOVERED);
                    return;
                }
            }
            if (!isSuccess) {
                StatLog.printLog(TAG, "onServicesDiscovered failed, disconnect!");
                mConnectionState = STATE_DISCONNECTED;
                broadcastReceivedData(ACTION_GATT_CONNECT_FAILED);
                mBluetoothGatt.disconnect();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastReceivedData(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastReceivedData(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                StatLog.printLog(TAG, "onDescriptorWrite, connection established");
                broadcastReceivedData(ACTION_GATT_CONNECT_READY);
            } else {
                StatLog.printLog(TAG, "onDescriptorWrite error, connection failed");
                mConnectionState = STATE_DISCONNECTED;
                broadcastReceivedData(ACTION_GATT_CONNECT_FAILED);
                mBluetoothGatt.disconnect();
            }
        }
    };

    private void broadcastReceivedData(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastReceivedData(final String action,
                                       final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            if (attachReceived(data)) {
                String dataBase64 = Base64.encodeToString(receivedBytes, Base64.URL_SAFE);
                StatLog.printLog(TAG, "received data:" + dataBase64);
                intent.putExtra(EXTRA_DATA, dataBase64);
                clearBuffer();
                setState(STATE_CONNECTED);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        }
    }

    public class LocalBinder extends Binder {
        public BleTransmitService getService() {
            return BleTransmitService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                StatLog.printLog(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            StatLog.printLog(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            StatLog.printLog(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        StatLog.printLog(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            StatLog.printLog(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        stopSelf();
    }

    public void close() {
        setState(STATE_DISCONNECTED);
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    private void enableReadCharacteristicNotification() {
        BluetoothGattService RxService = mBluetoothGatt.getService(QKEY_SERVICE_UUID);
        if (RxService == null) {
            return;
        }
        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(QKEY_NOTIFY_UUID);
        if (RxChar == null) {
            return;
        }
        setCharacteristicNotification(RxChar, true);
    }

    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                               boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(BleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    private void setState(int state) {
        StatLog.printLog(TAG, "set state : " + mConnectionState + " -> " + state);
        mConnectionState = state;
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public int getConnectionState() {
        return mConnectionState;
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        return mBluetoothGatt == null ? false : mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean writeU2FCharacteristic(byte[] value) {
        if (mConnectionState != STATE_CONNECTED) {
            StatLog.printLog(TAG, "write u2f data error, state error : " + mConnectionState + " " + new String(Hex.encodeHex(value)));
            return false;
        }
        StatLog.printLog(TAG, "write data : " + new String(Hex.encodeHex(value)));

        clearBuffer();
        return sendData(value);
    }

    final int MAX_PACKET_LEN = 20;

    private boolean sendData(byte[] data) {
        setState(STATE_SEND_PENDING);
        dataQueue.clear();
        int dataLen = data.length;
        if (dataLen == 0)
            return false;

        if (dataLen <= MAX_PACKET_LEN) {
            dataQueue.offer(data);
            StatLog.printLog(TAG, "data len <= 20 , packet count : " + dataQueue.size());
            setState(STATE_SENDING);
            return sendNextPacket();

        }
        dataLen -= MAX_PACKET_LEN;
        byte[] firstPacket = new byte[MAX_PACKET_LEN];
        ByteBuffer.wrap(firstPacket)
                .put(data, 0, MAX_PACKET_LEN);
        dataQueue.offer(firstPacket);
        int leftPacketCount = dataLen / (MAX_PACKET_LEN - 1);
        int lastPacketLen = dataLen % (MAX_PACKET_LEN - 1);
        int i = 0;
        for (; i < leftPacketCount; i++) {
            byte[] packet = new byte[MAX_PACKET_LEN];
            ByteBuffer.wrap(packet)
                    .put((byte) i)
                    .put(data, MAX_PACKET_LEN + i * (MAX_PACKET_LEN - 1), (MAX_PACKET_LEN - 1));
            dataQueue.offer(packet);
        }
        if (lastPacketLen > 0) {
            byte[] lastPacket = new byte[lastPacketLen + 1];
            ByteBuffer.wrap(lastPacket)
                    .put((byte) i)
                    .put(data, MAX_PACKET_LEN + i * (MAX_PACKET_LEN - 1), lastPacketLen);
            dataQueue.offer(lastPacket);
        }
        StatLog.printLog(TAG, "split packet complete,packet count : " + dataQueue.size());
        setState(STATE_SENDING);
        return sendNextPacket();
    }

    private boolean sendNextPacket() {
        if ((lastPacket = dataQueue.poll()) != null) {
            StatLog.printLog(TAG, "send next packet");
            try {
                return sendPacket(lastPacket);
            } catch (Exception e) {
                return false;
            }
        } else {
            if (mConnectionState == STATE_SENDING) {
                StatLog.printLog(TAG, "send all packet complete, do reading...");
                broadcastReceivedData(ACTION_GATT_READ_PENDING);
                setState(STATE_READ_PENDING);
                return true;
            }
        }

        return false;
    }


    private boolean writePacket(byte[] data) {
        if (!rxChar.setValue(data)) {
            return false;
        }
        boolean writeSuccess = mBluetoothGatt.writeCharacteristic(rxChar);
        if (writeSuccess) {
            setTimeoutTimer(WAIT_CONFIRM_TIMEOUT);
        }
        return writeSuccess;
    }

    private boolean resendPacket() {
        StatLog.printLog(TAG, "resendPacket packet times : " + resendTimes);
        if (resendTimes < MAX_RETRY_COUNT) {
            resendTimes++;
            try {
                return writePacket(lastPacket);
            } catch (Exception e) {
                StatLog.printLog(TAG, "resendPacket failed : " + resendTimes);
                return false;
            }
        }
        StatLog.printLog(TAG, "resendPacket failed times over : " + resendTimes);
        return false;
    }

    private boolean writeConfirm(byte confirm, boolean retry) {
        lastConfirm = confirm;
        if (!rxChar.setValue(new byte[]{confirm})) {
            return false;
        }
        boolean confirmSuccess = mBluetoothGatt.writeCharacteristic(rxChar);
        if (confirmSuccess && retry) {
            setTimeoutTimer(WAIT_DATA_TIMEOUT);
        }
        return confirmSuccess;
    }

    private boolean reconfirm() {
        if (resendTimes < MAX_RETRY_COUNT) {
            resendTimes++;
            try {
                return writeConfirm(lastConfirm, true);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private void receivedConfirm(byte[] result) {
        StatLog.printLog(TAG, "receivedConfirm :" + new String(Hex.encodeHex(result)));
        cancelTimeoutTimer();
        if (lastPacket != null) {
            if (result[0] == ~lastPacket[0]) {
                resendTimes = 0;
                if (sendNextPacket()) {
                    return;
                }
            }
        }
        StatLog.printLog(TAG, "received confirm but write next packet error.");
        broadcastReceivedData(ACTION_GATT_WRITE_ERROR);
    }

    private boolean attachReceived(byte[] received) {
        if (received.length == 1) {
            receivedConfirm(received);
        } else {
            return parseReceived(received);
        }
        return false;
    }

    int packetSeq = 0;
    final byte errorBytes = 0x00;

    private boolean parseReceived(byte[] data) {
        StatLog.printLog(TAG, "parse received packet : " + new String(Hex.encodeHex(data)));
        cancelTimeoutTimer();
        resendTimes = 0;
        if (data[0] == (byte) 0x83 && data.length != 5) {
            clearBuffer();
            int totalLen = ((data[1] << 8) | (data[2] & 0xff) & 0xffff) + 3;
            StatLog.printLog(TAG, "read head packet, data total len : " + totalLen);
            receivedBytes = new byte[totalLen];
            receivedByteBuffer = ByteBuffer.wrap(receivedBytes);
            receivedByteBuffer.put(data);
            byte reply = (byte) ~data[0];
            sendConfirm(reply);
            setState(STATE_READING);
            broadcastReceivedData(ACTION_GATT_READING);
            if (totalLen > 15) {
                broadcastReceivedData(ACTION_GATT_ON_ACTION_RESULT);
            }
        } else if (data[0] == packetSeq && receivedByteBuffer != null) {
            StatLog.printLog(TAG, "read packet, seq : " + data[0]);
            if (receivedByteBuffer == null) {
                writeConfirm(errorBytes, false);
                close();
                return false;
            }
            packetSeq++;
            receivedByteBuffer.put(data, 1, data.length - 1);
            StatLog.printLog(TAG, String.format("received data[0] : %x", data[0]));
            byte reply = (byte) ~data[0];
            sendConfirm(reply);
        } else if (data[0] == (byte) 0x83 && data.length == 5) {
            if (data[3] == (byte) 0x69 && data[4] == (byte) 0x85) {
                StatLog.printLog(TAG, "read 0x69 0x85..");
                broadcastReceivedData(ACTION_GATT_ON_USER_ACTION);
            } else {
                receivedBytes = new byte[5];
                receivedByteBuffer = ByteBuffer.wrap(receivedBytes);
                receivedByteBuffer.put(data);
            }
            byte reply = (byte) ~data[0];
            writeConfirm(reply, false);
        } else {
            StatLog.printLog(TAG, "read unknown packet");
            close();
            writeConfirm(errorBytes, false);
        }

        if (receivedByteBuffer != null && receivedBytes != null) {
            StatLog.printLog(TAG, "position=" + receivedByteBuffer.position() + " totalLength=" + receivedBytes.length);
            if (receivedByteBuffer.position() == receivedBytes.length) {
                StatLog.printLog(TAG, "assemble packet complete");
                return true;
            }
        }

        return false;
    }

    private void sendConfirm(byte confirm) {
        StatLog.printLog(TAG, String.format("send confirm %x", confirm));
        int i = 0;
        for (; i < MAX_RETRY_COUNT; i++) {
            if (writeConfirm(confirm, true)) {
                StatLog.printLog(TAG, "send confirm success!");
                break;
            }
            try {
                Thread.sleep(RETRY_DELAY_MILL);
            } catch (Exception e) {
            }
            StatLog.printLog(TAG, "resendPacket confirm.");
        }
        if (i == MAX_RETRY_COUNT) {
            StatLog.printLog(TAG, "send confirm failed!");
        }
    }

    private boolean sendPacket(byte[] packet) {
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            StatLog.printLog(TAG, "send packet times : " + i);
            if (writePacket(packet)) {
                return true;
            }
            try {
                Thread.sleep(RETRY_DELAY_MILL);
            } catch (Exception e) {
            }
        }
        return false;
    }

    private synchronized void cancelTimeoutTimer() {
        StatLog.printLog(TAG, "cancelTimeoutTimer");
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    private synchronized void setTimeoutTimer(long timeout) {
        cancelTimeoutTimer();
        StatLog.printLog(TAG, "setTimeoutTimer");
        mTimer = new Timer();
        mTimer.schedule(new TimeoutTask(), timeout);
    }

    class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            if (mConnectionState == STATE_SENDING) {
                StatLog.printLog(TAG, "send data timeout!");
                if (!resendPacket()) {
                    release();
                    broadcastReceivedData(ACTION_GATT_WRITE_ERROR);
                }
            } else if (mConnectionState == STATE_READING) {
                StatLog.printLog(TAG, "read data timeout!");
                if (!reconfirm()) {
                    release();
                    broadcastReceivedData(ACTION_GATT_READ_ERROR);
                }
            }
        }
    }

    private void release() {
        StatLog.printLog(TAG, "release");
        writeConfirm(errorBytes, true);
        clearBuffer();
        close();
    }

    private void clearBuffer() {
        StatLog.printLog(TAG, "clear buffer!");
        receivedBytes = null;
        receivedByteBuffer = null;
        packetSeq = 0;
    }
}
