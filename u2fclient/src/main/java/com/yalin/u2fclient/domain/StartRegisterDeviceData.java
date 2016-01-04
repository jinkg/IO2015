package com.yalin.u2fclient.domain;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * u2f StartRegisterDeviceData data struct
 * you will get it from you server
 */
public class StartRegisterDeviceData implements Parcelable {

    private String challenge;
    private String version;
    private String appId;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    protected StartRegisterDeviceData(Parcel in) {
        challenge = in.readString();
        version = in.readString();
        appId = in.readString();
    }

    public static final Creator<StartRegisterDeviceData> CREATOR = new Creator<StartRegisterDeviceData>() {
        @Override
        public StartRegisterDeviceData createFromParcel(Parcel in) {
            return new StartRegisterDeviceData(in);
        }

        @Override
        public StartRegisterDeviceData[] newArray(int size) {
            return new StartRegisterDeviceData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(challenge);
        dest.writeString(version);
        dest.writeString(appId);
    }
}
