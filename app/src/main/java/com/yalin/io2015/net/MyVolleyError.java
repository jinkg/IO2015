package com.yalin.io2015.net;

import android.content.Context;

import com.android.volley.VolleyError;

/**
 * Created by YaLin on 2015/7/20.
 */
public class MyVolleyError extends VolleyError {
    public ErrorCodeConstants errorConstant;

    public MyVolleyError(Context context, ErrorCodeConstants errorConstant) {
        super(context.getString(errorConstant.resId));
        this.errorConstant = errorConstant;
    }
}
