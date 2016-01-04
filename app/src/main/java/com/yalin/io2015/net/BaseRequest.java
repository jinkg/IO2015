package com.yalin.io2015.net;


import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.yalin.io2015.log.StatLog;
import com.yalin.io2015.net.response.BaseResponse;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by 雅麟 on 2015/6/17.
 */
public class BaseRequest<T extends BaseResponse> extends Request<T> {
    private static final String TAG = BaseRequest.class.getSimpleName();
    protected static final String ERROR_CODE_KEY = "errorCode";
    protected Response.Listener<T> mListener;
    protected Gson mGson;
    protected Class<T> mCls;
    protected Map<String, String> mHeaders;

    protected Context mContext;

    protected BaseRequest(Context context, int method, String url, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        mContext = context;
    }

    public BaseRequest(Context context, int method, String url, Class<T> cls, Map<String, String> headers, Response.Listener listener, Response.ErrorListener errorListener) {
        this(context, method, url, errorListener);
        mListener = listener;
        mGson = new Gson();
        mCls = cls;
        mHeaders = headers;
    }

    public void setHeaders(Map<String, String> headers) {
        mHeaders = headers;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        T parsedGSON;
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            StatLog.printLog(TAG, jsonString);
            JSONObject jsonObject = new JSONObject(jsonString);
            int error = jsonObject.getInt(ERROR_CODE_KEY);

            ErrorCodeConstants errorConstants = ErrorCodeConstants.getValue(error);
            if (errorConstants == ErrorCodeConstants.Success) {
                parsedGSON = mGson.fromJson(jsonString, mCls);
            } else {
                return Response.error(new MyVolleyError(mContext, errorConstants));
            }
        } catch (Exception e) {
            return Response.error(new MyVolleyError(mContext, ErrorCodeConstants.UnknownError));
        }

        return Response.success(parsedGSON, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError) {
        if (volleyError != null) {
            if (volleyError.networkResponse != null) {
                volleyError = new MyVolleyError(mContext, ErrorCodeConstants.ServerError);
            } else {
                if (volleyError instanceof TimeoutError) {
                    volleyError = new MyVolleyError(mContext, ErrorCodeConstants.ServerError);
                } else {
                    volleyError = new MyVolleyError(mContext, ErrorCodeConstants.NetworkError);
                }
            }
        } else {
            volleyError = new MyVolleyError(mContext, ErrorCodeConstants.UnknownError);
        }
        return super.parseNetworkError(volleyError);
    }

    @Override
    protected void deliverResponse(T response) {
        if (mListener != null) {
            mListener.onResponse(response);
        }
    }

    @Override
    public void deliverError(VolleyError error) {
        if (error != null && error.networkResponse != null) {
        }
        super.deliverError(error);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        if (mHeaders != null && mHeaders.size() > 0) {
            return mHeaders;
        }
        return super.getHeaders();
    }
}
