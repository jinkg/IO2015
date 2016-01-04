package com.yalin.u2fclient.parser;

/**
 * Created by YaLin on 2015/12/3.
 */
public abstract class DataParser {
    protected ActionHandler actionHandler;

    public DataParser(ActionHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("you must pass a valid object");
        }
        actionHandler = handler;
    }

    public abstract void parse(byte[] data, int state);
}
