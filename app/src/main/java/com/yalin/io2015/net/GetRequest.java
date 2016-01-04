package com.yalin.io2015.net;


import android.content.Context;

import com.android.volley.Response;
import com.yalin.io2015.net.response.BaseResponse;

import java.util.Map;

/**
 * Created by 雅麟 on 2015/3/21.
 */
public class GetRequest<T extends BaseResponse> extends BaseRequest<T> {

    public GetRequest(Context context, String url, Class<T> cls, Map<String, String> header, Response.Listener listener, Response.ErrorListener errorListener) {
        super(context, Method.GET, url, cls, header, listener, errorListener);
    }

    public GetRequest(Context context, String url, Class<T> cls, Response.Listener listener, Response.ErrorListener errorListener) {
        this(context, url, cls, null, listener, errorListener);
    }

}