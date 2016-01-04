package com.yalin.io2015.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.yalin.u2fclient.U2FClientApi;
import com.yalin.u2fclient.domain.FinishRegisterData;
import com.yalin.u2fclient.domain.FinishVerifyData;
import com.yalin.u2fclient.domain.U2FRequest;
import com.yalin.u2fclient.interfaces.U2FResultCallback;
import com.jin.uitoolkit.fragment.BaseLoadingFragment;
import com.yalin.io2015.R;
import com.yalin.io2015.constants.Constants;
import com.yalin.io2015.data.User;
import com.yalin.io2015.net.GetRequest;
import com.yalin.io2015.net.NetService;
import com.yalin.io2015.net.PostRequest;
import com.yalin.io2015.net.RequestQueueHelper;
import com.yalin.io2015.net.response.BaseResponse;
import com.yalin.io2015.net.response.StartU2FResponse;
import com.yalin.io2015.ui.DeviceListActivity;
import com.yalin.io2015.util.U2FErrorCodeParser;


/**
 * Created by YaLin on 2015/10/29.
 */
public class VerifyFragment extends BaseLoadingFragment implements View.OnClickListener, U2FResultCallback {
    private U2FRequest startVerifyRequest;
    AlertDialog waitActionDialog;
    private String username;
    LoginFragment.LoginSuccessCallback loginSuccessCallback;

    private View rootCoordinator;

    public static VerifyFragment getInstance(String username, LoginFragment.LoginSuccessCallback loginSuccessCallback) {
        VerifyFragment fragment = new VerifyFragment();
        fragment.username = username;
        fragment.loginSuccessCallback = loginSuccessCallback;
        return fragment;
    }

    @Override
    public View createContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_verify, container, false);
        initView(view);

        return view;
    }

    void initView(View view) {
        rootCoordinator = view.findViewById(R.id.root_coordinator);
        view.findViewById(R.id.verify_btn_open).setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(Constants.BLUETOOTH_DEVICE_PICK);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

    @Override
    protected String getRequestTag() {
        return VerifyFragment.class.getName();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.verify_btn_open:
                startVerify();
                break;
        }
    }

    void selectDeviceType() {
        new AlertDialog.Builder(getActivity())
                .setItems(getActivity().getResources().getStringArray(R.array.device_type),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                switch (which) {
                                    case 0:
                                        pickDevice();
                                        break;
                                    case 1:
                                        break;
                                }
                            }
                        }).show();
    }

    @Override
    public void onConnectionStart() {
        showLoading();
    }

    @Override
    public void onConnect(String address) {

    }

    @Override
    public void onUserAction() {
    }

    @Override
    public void onActionFinish() {
        if (waitActionDialog != null) {
            waitActionDialog.dismiss();
        }
    }

    @Override
    public void onRegisterResponse(FinishRegisterData data) {

    }

    @Override
    public void onVerifyResponse(FinishVerifyData data) {
        if (getActivity() == null) {
            return;
        }
        showLoading();
        finishVerify(data);
    }

    @Override
    public void onError(int errorCode) {
        if (getActivity() == null) {
            return;
        }
        if (waitActionDialog != null && waitActionDialog.isShowing()) {
            waitActionDialog.dismiss();
        }
        dismissLoading();
        Snackbar.make(rootCoordinator, U2FErrorCodeParser.parseError(errorCode), Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onConnectionLost() {
        if (getActivity() == null) {
            return;
        }
        dismissLoading();
    }

    void startVerify() {
        showLoading();
        RequestQueue requestQueue = RequestQueueHelper.getInstance(getActivity().getApplicationContext());
        GetRequest<StartU2FResponse> request = new GetRequest<>(getActivity().getApplicationContext(), NetService.startVerify(username), StartU2FResponse.class, null,
                new Response.Listener() {
                    @Override
                    public void onResponse(Object response) {
                        startVerifySuccess(((StartU2FResponse) response).data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestFailed(error.getMessage());
                    }
                }
        );
        request.setTag(getRequestTag());
        requestQueue.add(request);
    }

    void startVerifySuccess(U2FRequest request) {
        startVerifyRequest = request;
        if (getActivity() == null) {
            return;
        }
        dismissLoading();
        selectDeviceType();
    }

    void requestFailed(String msg) {
        if (getActivity() == null) {
            return;
        }
        Snackbar.make(rootCoordinator, msg, Snackbar.LENGTH_SHORT)
                .show();
        dismissLoading();
    }

    public void finishVerify(FinishVerifyData finishVerifyData) {
        if (finishVerifyData == null) {
            return;
        }
        RequestQueue requestQueue = RequestQueueHelper.getInstance(getActivity().getApplicationContext());
        PostRequest<BaseResponse> request = new PostRequest<>(getActivity().getApplicationContext(), NetService.FINISH_VERIFY_URL, BaseResponse.class, NetService.finishVerify(username, finishVerifyData.toJsonString()),
                new Response.Listener() {
                    @Override
                    public void onResponse(Object response) {
                        onFinishSuccess(username);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestFailed(error.getMessage());
                    }
                }
        );
        requestQueue.add(request);
    }

    void onFinishSuccess(String username) {
        User user = new User(username);
        User.storeUserInfo(getActivity().getApplicationContext(), user);
        if (loginSuccessCallback != null) {
            loginSuccessCallback.onVerifySuccess();
        }
        if (getActivity() == null) {
            return;
        }
        dismissLoading();
        Snackbar.make(rootCoordinator, R.string.verify_success, Snackbar.LENGTH_SHORT)
                .show();
    }

    private void pickDevice() {
        DeviceListActivity.open(getActivity());
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == Constants.BLUETOOTH_DEVICE_PICK) {
                String deviceAddress = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                if (startVerifyRequest != null) {
                    U2FClientApi.startBluetoothVerify(startVerifyRequest, deviceAddress, VerifyFragment.this);
                }
            }
        }
    };
}
