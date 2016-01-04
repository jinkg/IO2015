package com.yalin.u2fclient.protocol;


import com.yalin.u2fclient.domain.FinishRegisterData;
import com.yalin.u2fclient.domain.FinishVerifyData;

/**
 * Created by YaLin on 2015/10/27.
 */
public interface Protocol {
    String ENROLL_TYPE = "navigator.id.finishEnrollment";
    String VERIFY_TYPE = "navigator.id.getAssertion";

    String FACET_ID = "http://192.168.1.7:8080/RPServer";

    String ADDRESS_KEY = "address";
    String OPTION_TYPE_KEY = "option_type";
    String DATA_KEY = "data";
    String DEVICE_TYPE_KEY = "device_type";

    int DEFAULT_DEVICE_TYPE = 0;
    int BLUETOOTH_DEVICE_TYPE = 1;
    int NFC_DEVICE_TYPE = 2;
    int BLE_DEVICE_TYPE = 3;

    int STATE_IDLE = 0;
    int STATE_GET_VERSION = 1;
    int STATE_CHECK_ONLY = 2;
    int STATE_REQUEST_REGISTER = 3;
    int STATE_REQUEST_VERIFY = 4;

    int REGISTER_OPERATE_TYPE = 1;
    int VERIFY_OPERATE_TYPE = 2;
    int SEARCH_BLUETOOTH_OPERATE_TYPE = 3;

    interface U2FProtocolResultCallback {
        void onRegisterResponse(FinishRegisterData registerResponse);

        void onUserAction();

        void onActionResult();

        void onVerifyResponse(FinishVerifyData verifyData);

        void onDataError(int errorCode);
    }

    void start();

    void release();

    void getVersion();

    void register();

    void verify();

    void doOperate();

    void setU2FProtocolResultCallback(U2FProtocolResultCallback u2FProtocolResultCallback);
}
