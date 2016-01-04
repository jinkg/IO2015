package com.yalin.u2fclient.domain;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * u2f StartVerifyDeviceData data struct
 * you should get all field from you server,then assembly this objects ArrayList and pass the list to Verify Api
 */
public class StartVerifyDeviceData implements Parcelable {

    private String challenge;
    private String version;
    private String appId;
    private String keyHandle;

    protected StartVerifyDeviceData(Parcel in) {
        challenge = in.readString();
        version = in.readString();
        appId = in.readString();
        keyHandle = in.readString();
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getKeyHandle() {
        return keyHandle;
    }

    public void setKeyHandle(String keyHandle) {
        this.keyHandle = keyHandle;
    }

    public static final Creator<StartVerifyDeviceData> CREATOR = new Creator<StartVerifyDeviceData>() {
        @Override
        public StartVerifyDeviceData createFromParcel(Parcel in) {
            return new StartVerifyDeviceData(in);
        }

        @Override
        public StartVerifyDeviceData[] newArray(int size) {
            return new StartVerifyDeviceData[size];
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
        dest.writeString(keyHandle);
    }
}
