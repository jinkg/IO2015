package com.yalin.io2015.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.yalin.io2015.data.User;
import com.yalin.u2fclient.U2FClientApi;
import com.yalin.u2fclient.domain.FinishRegisterData;
import com.yalin.u2fclient.domain.FinishVerifyData;
import com.yalin.u2fclient.domain.U2FRequest;
import com.yalin.u2fclient.interfaces.U2FResultCallback;
import com.yalin.io2015.R;
import com.yalin.io2015.constants.Constants;
import com.yalin.io2015.log.StatLog;
import com.yalin.io2015.net.GetRequest;
import com.yalin.io2015.net.NetService;
import com.yalin.io2015.net.PostRequest;
import com.yalin.io2015.net.RequestQueueHelper;
import com.yalin.io2015.net.response.BaseResponse;
import com.yalin.io2015.net.response.StartU2FResponse;
import com.yalin.io2015.util.U2FErrorCodeParser;

public class MainActivity extends BaseLoadActivity implements View.OnClickListener, U2FResultCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REGISTRATION = 1;
    private static final int VERIFY = 2;
    View rootCoordinator;

    U2FRequest u2FRequest;

    int mode = REGISTRATION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(Constants.BLUETOOTH_DEVICE_PICK);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private void initView() {
        findViewById(R.id.btn_show1).setOnClickListener(this);
        findViewById(R.id.btn_show2).setOnClickListener(this);
        findViewById(R.id.btn_show3).setOnClickListener(this);
        rootCoordinator = findViewById(R.id.root_coordinator);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_MAIN;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_show1:
                Snackbar.make(rootCoordinator, "haha", Snackbar.LENGTH_SHORT)
                        .show();
                break;
            case R.id.btn_show2:
                mode = REGISTRATION;
                testStartRegistration();
                break;
            case R.id.btn_show3:
                mode = VERIFY;
                testStartVerify();
                break;
            default:
                super.onClick(v);
                break;
        }
    }

    private void testStartRegistration() {
        String username = User.getUserAccount(getApplicationContext());
        showLoading();
        RequestQueue requestQueue = RequestQueueHelper.getInstance(getApplicationContext());
        GetRequest<StartU2FResponse> request = new GetRequest<>(getApplicationContext(), NetService.startRegistration(username), StartU2FResponse.class,
                new Response.Listener() {
                    @Override
                    public void onResponse(Object response) {
                        startU2FSuccess(((StartU2FResponse) response).data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestFailed(error.getMessage());
                    }
                }
        );
        request.setTag(TAG);
        requestQueue.add(request);
    }

    private void testFinishRegistration(FinishRegisterData data) {
        String username = User.getUserAccount(getApplicationContext());
        showLoading();
        RequestQueue requestQueue = RequestQueueHelper.getInstance(getApplicationContext());
        PostRequest<BaseResponse> request = new PostRequest<>(getApplicationContext(), NetService.FINISH_REGISTRATION_URL, BaseResponse.class, NetService.finishRegistration(username, data.toJsonString()),
                new Response.Listener() {
                    @Override
                    public void onResponse(Object response) {
                        finishRegistrationSuccess();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestFailed(error.getMessage());
                    }
                }
        );
        request.setTag(TAG);
        requestQueue.add(request);
    }

    private void testStartVerify() {
        String username = User.getUserAccount(getApplicationContext());
        showLoading();
        RequestQueue requestQueue = RequestQueueHelper.getInstance(getApplicationContext());
        GetRequest<StartU2FResponse> request = new GetRequest<>(getApplicationContext(), NetService.startVerify(username), StartU2FResponse.class,
                new Response.Listener() {
                    @Override
                    public void onResponse(Object response) {
                        startU2FSuccess(((StartU2FResponse) response).data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestFailed(error.getMessage());
                    }
                }
        );
        request.setTag(TAG);
        requestQueue.add(request);
    }

    private void testFinishVerify(FinishVerifyData data) {
        String username = User.getUserAccount(getApplicationContext());
        showLoading();
        RequestQueue requestQueue = RequestQueueHelper.getInstance(getApplicationContext());
        PostRequest<BaseResponse> request = new PostRequest<>(getApplicationContext(), NetService.FINISH_VERIFY_URL, BaseResponse.class, NetService.finishVerify(username, data.toJsonString()),
                new Response.Listener() {
                    @Override
                    public void onResponse(Object response) {
                        finishVerifySuccess();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestFailed(error.getMessage());
                    }
                }
        );
        request.setTag(TAG);
        requestQueue.add(request);
    }


    private void startU2FSuccess(U2FRequest request) {
        u2FRequest = request;
        dismissLoading();
        pickDevice();
    }

    private void finishRegistrationSuccess() {
        dismissLoading();
        Snackbar.make(rootCoordinator, R.string.registration_success, Snackbar.LENGTH_SHORT)
                .show();
    }

    private void finishVerifySuccess() {
        dismissLoading();
        Snackbar.make(rootCoordinator, R.string.authentication_success, Snackbar.LENGTH_SHORT)
                .show();
    }

    private void requestFailed(String msg) {
        dismissLoading();
        Snackbar.make(rootCoordinator, msg, Snackbar.LENGTH_SHORT)
                .show();
    }

    private void pickDevice() {
        DeviceListActivity.open(this);
    }

    @Override
    public void onConnectionStart() {
        StatLog.printLog(TAG, "onConnectionStart");
        showLoading();
    }

    @Override
    public void onConnect(String address) {
        StatLog.printLog(TAG, "onConnect");
    }

    @Override
    public void onConnectionLost() {
        StatLog.printLog(TAG, "onConnectionLost");
    }

    @Override
    public void onUserAction() {
        StatLog.printLog(TAG, "onUserAction");
    }

    @Override
    public void onActionFinish() {
        StatLog.printLog(TAG, "onActionFinish");
    }

    @Override
    public void onRegisterResponse(FinishRegisterData data) {
        StatLog.printLog(TAG, "onRegisterResponse: " + data.toJsonString());
        testFinishRegistration(data);
    }

    @Override
    public void onVerifyResponse(FinishVerifyData data) {
        StatLog.printLog(TAG, "onVerifyResponse: " + data.toJsonString());
        testFinishVerify(data);
    }

    @Override
    public void onError(int errorCode) {
        dismissLoading();
        Snackbar.make(rootCoordinator, U2FErrorCodeParser.parseError(errorCode), Snackbar.LENGTH_SHORT)
                .show();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == Constants.BLUETOOTH_DEVICE_PICK) {
                String deviceAddress = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                if (u2FRequest != null) {
                    if (mode == REGISTRATION) {
                        U2FClientApi.startBluetoothRegister(u2FRequest, deviceAddress, MainActivity.this);
                    } else if (mode == VERIFY) {
                        U2FClientApi.startBluetoothVerify(u2FRequest, deviceAddress, MainActivity.this);
                    }
                }
            }
        }
    };
}
