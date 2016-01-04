package com.yalin.u2fclient.parser;

import com.yalin.u2fclient.interfaces.U2FErrorCode;
import com.yalin.u2fclient.protocol.Protocol;

import java.nio.charset.Charset;

/**
 * Created by YaLin on 2015/12/3.
 */
public class QKeyDataParserImpl extends DataParser {
    private static final String TAG = QKeyDataParserImpl.class.getSimpleName();
    protected static final byte DATA_SUCCESS_TAG = (byte) 0x83;
    protected static final byte DATA_RESULT_PRE_SUFFIX_TAG = (byte) 0x9b;
    protected static final byte DATA_RESULT_SUFFIX_OK_TAG = 0x00;
    protected static final byte DATA_RESULT_SUFFIX_BAD_TAG = 0x01;
    protected static final int DATA_VERSION_LENGTH = 11;
    protected static final int DATA_STATE_LENGTH = 5;

    public QKeyDataParserImpl(ActionHandler handler) {
        super(handler);
    }

    @Override
    public void parse(byte[] data, int state) {
//        StatLog.printLog(TAG, "qKey action parse : " + new String(Hex.encodeHex(data)));
        if (data[0] != DATA_SUCCESS_TAG) {
            actionHandler.onDataError(U2FErrorCode.ERROR_DATA_ERROR);
        } else if (data.length == DATA_STATE_LENGTH) {
            if (state == Protocol.STATE_CHECK_ONLY) {
                if (data[3] == DATA_RESULT_PRE_SUFFIX_TAG) {
                    if (data[4] == DATA_RESULT_SUFFIX_OK_TAG) {
//                        StatLog.printLog(TAG, "qKey action parse check only success");
                        actionHandler.onCheckOnlyUserNotPresence();
                    } else if (data[4] == DATA_RESULT_SUFFIX_BAD_TAG) {
//                        StatLog.printLog(TAG, "qKey action parse check only failed");
                        actionHandler.onCheckOnlyBadKeyHandle();
                    }
                } else {
//                    StatLog.printLog(TAG, "qKey action parse data error");
                    actionHandler.onDataError(U2FErrorCode.ERROR_DATA_ERROR);
                }
            } else {
//                StatLog.printLog(TAG, "qKey action parse ERROR_USER_PRESENCE_TIME_OUT");
                actionHandler.onDataError(U2FErrorCode.ERROR_USER_PRESENCE_TIME_OUT);
            }
        } else if (data.length == DATA_VERSION_LENGTH) {
            String version = new String(data, 3, 6, Charset.forName("ASCII"));
//            StatLog.printLog(TAG, "qKey action parse get version success");
            actionHandler.onVersion(version);
        } else {
//            StatLog.printLog(TAG, "qKey action parse on action result");
            actionHandler.onDataResult(data);
        }
    }
}
