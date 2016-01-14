package com.yalin.io2015.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.util.ArrayMap;


import java.util.Map;

/**
 * Created by YaLin on 2015/9/15.
 */
public class NetService {
    static {
        init();
    }

    private static final String HTTP_SP = "http_url";
    public static final String HTTP_KEY = "http";

    public static String HTTP_URL;

    public static final String BASE_URL = "http://192.168.1.107:8080";

    public static final String START_REGISTRATION_SUB = "/startRegistration";
    public static final String FINISH_REGISTRATION_SUB = "/finishRegistration";
    public static final String START_VERIFY_SUB = "/startAuthentication";
    public static final String FINISH_VERIFY_SUB = "/finishAuthentication";

    public static String START_REGISTRATION_URL;
    public static String FINISH_REGISTRATION_URL;
    public static String START_VERIFY_URL;
    public static String FINISH_VERIFY_URL;

    private static void init() {
        getHttpUrl();
        START_REGISTRATION_URL = HTTP_URL + START_REGISTRATION_SUB;
        FINISH_REGISTRATION_URL = HTTP_URL + FINISH_REGISTRATION_SUB;
        START_VERIFY_URL = HTTP_URL + START_VERIFY_SUB;
        FINISH_VERIFY_URL = HTTP_URL + FINISH_VERIFY_SUB;
    }

    public static String getHttpUrl() {
        HTTP_URL = BASE_URL;
        return HTTP_URL;
    }

    public static void storeHttpUrl(Context context, String server) {
        SharedPreferences sp = context.getSharedPreferences(
                HTTP_SP, Context.MODE_PRIVATE);
        HTTP_URL = server;
        sp.edit().putString(HTTP_KEY, server)
                .apply();
        init();
    }

    public static String startRegistration(String username) {
        return START_REGISTRATION_URL + "?username=" + username;
    }

    public static Map<String, String> finishRegistration(String username, String tokenResponse) {
        ArrayMap params = new ArrayMap();
        params.put("username", username);
        params.put("tokenResponse", tokenResponse);
        return params;
    }

    public static String startVerify(String username) {
        return START_VERIFY_URL + "?username=" + username;
    }

    public static Map<String, String> finishVerify(String username, String tokenResponse) {
        ArrayMap params = new ArrayMap();
        params.put("username", username);
        params.put("tokenResponse", tokenResponse);
        return params;
    }
}
