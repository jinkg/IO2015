package com.yalin.u2fclient.parser;

import com.yalin.u2fclient.interfaces.U2FErrorCode;

import java.nio.charset.Charset;

/**
 * Created by YaLin on 2015/12/3.
 */
public class NFCDataParserImpl extends DataParser {
    private static final String TAG = NFCDataParserImpl.class.getSimpleName();
    public static final byte DATA_CHECK_ONLY_SUCCESS_TAG = (byte) 0x69;
    public static final byte DATA_CHECK_ONLY_FAILED_TAG = (byte) 0x6a;
    public static final byte DATA_ERROR_TAG = (byte) 0x00;
    protected static final int DATA_VERSION_LENGTH = 6;

    public NFCDataParserImpl(ActionHandler handler) {
        super(handler);
    }

    @Override
    public void parse(byte[] data, int state) {
//        StatLog.printLog(TAG, "nfc action parse : " + new String(Hex.encodeHex(data)));
        if (data.length == DATA_VERSION_LENGTH) {
            String version = new String(data, Charset.forName("ASCII"));
//            StatLog.printLog(TAG, "nfc action parse get version success");
            actionHandler.onVersion(version);
        } else if (data[0] == DATA_CHECK_ONLY_SUCCESS_TAG) {
//            StatLog.printLog(TAG, "nfc protocol check only success");
            actionHandler.onCheckOnlyUserNotPresence();
        } else if (data[0] == DATA_CHECK_ONLY_FAILED_TAG) {
//            StatLog.printLog(TAG, "nfc protocol check only failed");
            actionHandler.onCheckOnlyBadKeyHandle();
        } else if (data[0] == DATA_ERROR_TAG) {
//            StatLog.printLog(TAG, "nfc action parse data error");
            actionHandler.onDataError(U2FErrorCode.ERROR_DATA_ERROR);
        } else {
//            StatLog.printLog(TAG, "nfc action parse on action result");
            actionHandler.onDataResult(data);
        }
    }
}
