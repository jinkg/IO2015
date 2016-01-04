package com.yalin.io2015.data;

/**
 * Created by 雅麟 on 2015/6/9.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.Random;


/**
 * Created by 雅麟 on 2015/6/9.
 */
public class User {

    private static final String USER_SP = "login";

    public static final String USERNAME_KEY = "username";
    public static final String LOGIN_TIMES_KEY = "login_times";

    private String username;
    private int loginTimes;

    public User(String username) {
        this.username = username;
        loginTimes = new Random().nextInt(100);
    }

    public static SharedPreferences getUserSp(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                USER_SP, Context.MODE_PRIVATE);
        return sp;
    }

    /**
     * @param context recommend use application context
     * @return
     */
    public static boolean isLogin(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences sp = context.getSharedPreferences(
                USER_SP, Context.MODE_PRIVATE);
        String accessToken = sp.getString(USERNAME_KEY, null);
        return !TextUtils.isEmpty(accessToken);
    }

    public static String getUserAccount(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                USER_SP, Context.MODE_PRIVATE);
        return sp.getString(USERNAME_KEY, null);
    }

    public static int getLoginTimes(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                USER_SP, Context.MODE_PRIVATE);
        return sp.getInt(LOGIN_TIMES_KEY, 0);
    }


    /**
     * when login success, should store user info
     *
     * @param context recommend use application context
     * @return
     */
    public static void storeUserInfo(Context context, User user) {
        if (user == null) {
            throw new IllegalArgumentException("user can not be null");
        }

        SharedPreferences sp = context.getSharedPreferences(
                USER_SP, Context.MODE_PRIVATE);
        sp.edit().putString(USERNAME_KEY, user.username)
                .putInt(LOGIN_TIMES_KEY, user.loginTimes)
                .apply();
    }

    /**
     * when logout success, should delete user info
     *
     * @param context recommend use application context
     * @return
     */
    public static void deleteUserInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                USER_SP, Context.MODE_PRIVATE);
        sp.edit()
                .putString(USERNAME_KEY, null)
                .putInt(LOGIN_TIMES_KEY, 0)
                .apply();
    }
}
