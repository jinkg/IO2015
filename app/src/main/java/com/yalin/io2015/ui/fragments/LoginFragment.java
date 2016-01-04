package com.yalin.io2015.ui.fragments;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.jin.uitoolkit.fragment.BaseLoadingFragment;
import com.jin.uitoolkit.util.Utils;
import com.yalin.io2015.R;
import com.yalin.io2015.data.User;
import com.yalin.io2015.net.GetRequest;
import com.yalin.io2015.net.MyVolleyError;
import com.yalin.io2015.net.NetService;
import com.yalin.io2015.net.RequestQueueHelper;
import com.yalin.io2015.net.response.StartU2FResponse;
import com.yalin.io2015.ui.AccountActivity;


/**
 * Created by 雅麟 on 2015/3/22.
 */
public class LoginFragment extends BaseLoadingFragment implements View.OnClickListener {

    public interface LoginSuccessCallback {
        void onLoginSuccess();

        void onNeedVerify(String username);

        void onVerifySuccess();
    }

    private static final String TAG = "LoginFragment";

    View rootCoordinator;
    EditText etUsername;
    EditText etPassword;
    Button btnLogin;

    TextInputLayout tilAccount;
    TextInputLayout tilPassword;


    private LoginSuccessCallback callback;

    public static LoginFragment getInstance(LoginSuccessCallback callback) {
        LoginFragment fragment = new LoginFragment();
        fragment.callback = callback;
        return fragment;
    }

    public static void open(int container, FragmentManager manager, LoginSuccessCallback callback) {
        if (manager.findFragmentByTag(TAG) != null) {
            return;
        }
        manager.beginTransaction().setCustomAnimations(
                R.anim.push_left_in,
                R.anim.push_left_out,
                R.anim.push_right_in,
                R.anim.push_right_out)
                .add(container, getInstance(callback), TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public View createContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        rootCoordinator = view.findViewById(R.id.root_coordinator);
        etUsername = (EditText) view.findViewById(R.id.login_et_account);
        etPassword = (EditText) view.findViewById(R.id.login_et_password);
        btnLogin = (Button) view.findViewById(R.id.login_btn_login);
        btnLogin.setOnClickListener(this);
        tilAccount = (TextInputLayout) view.findViewById(R.id.login_til_account);
        tilPassword = (TextInputLayout) view.findViewById(R.id.login_til_password);
    }

    @Override
    public void onStart() {
        super.onStart();
        etPassword.setText(null);
        etUsername.post(new Runnable() {
            @Override
            public void run() {
                etUsername.requestFocus();
            }
        });
    }

    @Override
    protected String getRequestTag() {
        return LoginFragment.class.getName();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_btn_login:
                Utils.closeInput(getActivity());
                login();
                break;
        }
    }

    private void login() {
        final String username = etUsername.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Snackbar.make(rootCoordinator, R.string.user_is_null, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        startVerify(username);
    }

    void startVerify(final String username) {
        showLoading();
        RequestQueue requestQueue = RequestQueueHelper.getInstance(getActivity().getApplicationContext());
        GetRequest<StartU2FResponse> request = new GetRequest<>(getActivity().getApplicationContext(), NetService.startVerify(username), StartU2FResponse.class, null,
                new Response.Listener() {
                    @Override
                    public void onResponse(Object response) {
                        if (getActivity() != null) {
                            dismissLoading();
                            if (getActivity() instanceof AccountActivity) {
                                ((AccountActivity) getActivity()).onNeedVerify(username);
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof MyVolleyError) {
                            if (((MyVolleyError) error).errorConstant.serverNumber == 201) {
                                loginSuccess(username);
                            }
                        }
                        requestFailed(error.getMessage());
                    }
                }
        );
        request.setTag(getRequestTag());
        requestQueue.add(request);
    }

    void requestFailed(String msg) {
        if (getActivity() == null) {
            return;
        }
        Snackbar.make(rootCoordinator, msg, Snackbar.LENGTH_SHORT)
                .show();
        dismissLoading();
    }

    private void loginSuccess(String username) {
        User user = new User(username);
        User.storeUserInfo(getActivity().getApplicationContext(), user);
        if (callback != null) {
            callback.onLoginSuccess();
            callback = null;
        }
        if (getActivity() == null) {
            return;
        }
        dismissLoading();
        Snackbar.make(rootCoordinator, R.string.login_success, Snackbar.LENGTH_SHORT)
                .show();
    }

    void gotoVerify(String username) {
        if (callback != null) {
            callback.onNeedVerify(username);
        }
    }
}
