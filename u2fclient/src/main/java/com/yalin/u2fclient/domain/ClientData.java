package com.yalin.u2fclient.domain;

import com.google.gson.Gson;

/**
 * U2F ClientData data struct,generate by sdk
 */
public class ClientData implements JsonStringInterface {
    public String typ;
    public String challenge;
    public String origin;

    public ClientData(String typ, String challenge, String origin) {
        this.typ = typ;
        this.challenge = challenge;
        this.origin = origin;
    }

    @Override
    public String toJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
