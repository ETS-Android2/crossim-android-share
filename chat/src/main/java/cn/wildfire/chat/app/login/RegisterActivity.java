package cn.wildfire.chat.app.login;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ospn.osnsdk.utils.ECUtils;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.app.AppService;
//import cn.wildfire.chat.app.Config;
import cn.wildfire.chat.app.MyApp;
import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.app.setting.SettingActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfirechat.chat.BuildConfig;
import cn.wildfirechat.chat.R;
//import cn.wildfirechat.remote.ConnectedCallback;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

public class RegisterActivity extends WfcBaseActivity {
    @BindView(R.id.registerButton)
    Button loginButton;
    @BindView(R.id.accountEditText)
    EditText accountEditText;
    @BindView(R.id.passwordEditText)
    EditText passwordEditText;
    @BindView(R.id.toLogin)
    TextView toLogin;
    @BindView(R.id.ipAddr)
    EditText ipEditText;
    @BindView(R.id.resetHost)
    Button resetHostButton;

    String registerIp = null;

    @Override
    protected int contentLayout() {
        return R.layout.register_activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        registerIp = sp.getString("registerIp", null);
        if(registerIp == null){
            registerIp = BuildConfig.REGISTER_IP;
            sp.edit().putString("registerIp", registerIp).apply();
        }
        ipEditText.setText(registerIp);
    }

    @Override
    protected boolean showHomeMenuItem() {
        return false;
    }

    @OnTextChanged(value = R.id.accountEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputAccount(Editable editable) {
        if (!TextUtils.isEmpty(passwordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            loginButton.setEnabled(true);
        } else {
            loginButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.passwordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputPassword(Editable editable) {
        if (!TextUtils.isEmpty(accountEditText.getText()) && !TextUtils.isEmpty(editable)) {
            loginButton.setEnabled(true);
        } else {
            loginButton.setEnabled(false);
        }
    }

    @OnClick(R.id.toLogin)
    void toLogin(){
        finish();
    }

    @OnClick(R.id.registerButton)
    void register() {

        String account = accountEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("正在注册...")
                .progress(true, 10)
                .cancelable(false)
                .build();
        dialog.show();

        new Thread(()-> {
            Map<String, Object> params = new HashMap<>();
            params.put("command", "Register");
            params.put("name", account);
            params.put("token", ECUtils.osnHash(password.getBytes()));
            String osnIDs[] = ECUtils.createOsnID("user");
            params.put("osnID", osnIDs[0]);
            params.put("osnKey", osnIDs[1]);
            OKHttpHelper.post("http://" + registerIp + ":8101/", params, new SimpleCallback<String>() {
                @Override
                public void onUiSuccess(String s) {
                    dialog.dismiss();
                    JSONObject json = JSON.parseObject(s);
                    Toast.makeText(RegisterActivity.this,
                            json.getString("errCode").startsWith("0") ? "注册成功" : "注册失败",
                            Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onUiFailure(int code, String msg) {
                    dialog.dismiss();
                    Toast.makeText(RegisterActivity.this,msg,Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @OnClick(R.id.resetHost)
    void resetHost(){
        registerIp = ipEditText.getText().toString();
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        sp.edit().putString("registerIp",registerIp).apply();
        Toast.makeText(this, "切换服务器成功", Toast.LENGTH_SHORT).show();
    }
}
