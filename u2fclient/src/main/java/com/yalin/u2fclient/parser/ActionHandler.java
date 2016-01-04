package com.yalin.u2fclient.parser;

/**
 * Created by YaLin on 2015/12/3.
 */
public interface ActionHandler {
    void onCheckOnlyUserNotPresence();

    void onCheckOnlyBadKeyHandle();

    void onDataError(int errorCode);

    void onVersion(String version);

    void onDataResult(byte[] data);
}
