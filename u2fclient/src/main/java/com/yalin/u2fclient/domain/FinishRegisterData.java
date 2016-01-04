package com.yalin.u2fclient.domain;


import com.google.gson.Gson;


/**
 * u2f FinishRegister data struct,you should send this object to you server when finish register
 */
public class FinishRegisterData implements JsonStringInterface {
    private String registrationData;
    private String clientData;

    public FinishRegisterData(String registrationData, String clientData) {
        this.registrationData = registrationData;
        this.clientData = clientData;
    }

    public String getRegistrationData() {
        return registrationData;
    }

    public String getClientData() {
        return clientData;
    }

    @Override
    public String toJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
