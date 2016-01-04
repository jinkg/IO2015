package com.yalin.u2fclient.domain;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * u2f U2FRequest data struct
 * you should get all field from you server,then assembly this object and pass it to Register Api
 */
public class U2FRequest implements Parcelable {

    private ArrayList<StartRegisterDeviceData> registerRequests;
    private ArrayList<StartVerifyDeviceData> authenticateRequests;

    protected U2FRequest(Parcel in) {
        registerRequests = in.createTypedArrayList(StartRegisterDeviceData.CREATOR);
        authenticateRequests = in.createTypedArrayList(StartVerifyDeviceData.CREATOR);
    }

    public ArrayList<StartRegisterDeviceData> getRegisterRequests() {
        return registerRequests;
    }

    public void setRegisterRequests(ArrayList<StartRegisterDeviceData> registerRequests) {
        this.registerRequests = registerRequests;
    }

    public ArrayList<StartVerifyDeviceData> getAuthenticateRequests() {
        return authenticateRequests;
    }

    public void setAuthenticateRequests(ArrayList<StartVerifyDeviceData> authenticateRequests) {
        this.authenticateRequests = authenticateRequests;
    }

    public static final Creator<U2FRequest> CREATOR = new Creator<U2FRequest>() {
        @Override
        public U2FRequest createFromParcel(Parcel in) {
            return new U2FRequest(in);
        }

        @Override
        public U2FRequest[] newArray(int size) {
            return new U2FRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(registerRequests);
        dest.writeTypedList(authenticateRequests);
    }
}
