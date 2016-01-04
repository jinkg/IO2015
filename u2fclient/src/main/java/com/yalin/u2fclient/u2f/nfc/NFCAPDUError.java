package com.yalin.u2fclient.u2f.nfc;

public class NFCAPDUError extends Exception {
    private final int code;

    public NFCAPDUError(int code) {
        super(String.format("APDU status: %04x", code));
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
