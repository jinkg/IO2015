package com.yalin.u2fclient.domain;

import com.google.gson.Gson;

/**
 * u2f FinishVerifyData data struct,you should send this object to you server when finish verify
 */
public class FinishVerifyData implements JsonStringInterface {
    private String signatureData;
    private String clientData;
    private String keyHandle;

    public FinishVerifyData(String keyHandle, String clientData, String signatureData) {
        this.keyHandle = keyHandle;
        this.clientData = clientData;
        this.signatureData = signatureData;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public String getClientData() {
        return clientData;
    }

    public String getKeyHandle() {
        return keyHandle;
    }

    @Override
    public String toJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
