package com.yalin.io2015.net;


import android.content.Context;
import android.support.v4.util.ArrayMap;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.yalin.io2015.net.response.BaseResponse;

import org.json.JSONArray;

import java.util.Map;

/**
 * Created by 雅麟 on 2015/3/21.
 */
public class PostRequest<T extends BaseResponse> extends BaseRequest<T> {
    protected Map<String, String> mParams;

    protected Map<String, JSONArray> mListParams;

    public PostRequest(Context context, String url, Class<T> cls, Map<String, String> header, Map<String, String> params, Response.Listener listener, Response.ErrorListener errorListener) {
        super(context, Method.POST, url, cls, header, listener, errorListener);
        mParams = params;
    }

    public PostRequest(Context context, String url, Class<T> cls, Map<String, String> params, Response.Listener listener, Response.ErrorListener errorListener) {
        this(context, url, cls, null, params, listener, errorListener);
    }

    public void addListParams(String key, JSONArray array) {
        if (mListParams == null) {
            mListParams = new ArrayMap<>();
        }
        mListParams.put(key, array);
    }

    public void addListParams(Map<String, JSONArray> arrayMap) {
        if (mListParams == null) {
            mListParams = new ArrayMap<>();
        }
        mListParams.putAll(arrayMap);
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return mParams;
    }
}