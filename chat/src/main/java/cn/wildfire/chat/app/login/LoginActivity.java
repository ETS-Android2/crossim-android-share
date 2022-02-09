/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.login;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.afollestad.materialdialogs.MaterialDialog;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ospn.osnsdk.utils.ECUtils;
import com.ospn.osnsdk.utils.OsnUtils;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.internal.DebouncingOnClickListener;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.MyApp;
import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfirechat.chat.BuildConfig;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

import static cn.wildfire.chat.app.MyApp.logInfo;

/**
 * use {@link SMSLoginActivity} instead
 */

public class LoginActivity extends WfcBaseActivity {
    @BindView(R.id.loginButton)
    Button loginButton;
    @BindView(R.id.accountEditText)
    EditText accountEditText;
    @BindView(R.id.passwordEditText)
    EditText passwordEditText;
    @BindView(R.id.ipAddr)
    EditText ipEditText;
    @BindView(R.id.resetHost)
    Button resetHostButton;
    @BindView(R.id.regAddr)
    EditText regEditText;
    @BindView(R.id.resetReg)
    Button resetRegButton;
    @BindView(R.id.button_genAccount)
    Button buttonGenAccount;
    @BindView(R.id.button_login)
    Button buttonLogin;

    @BindView(R.id.toRegister)
    TextView toRegister;

    String hostip;
    String regip;

    @Override
    protected int contentLayout() {
        return R.layout.login_activity_account;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buttonGenAccount = (Button)findViewById(R.id.button_genAccount);
        buttonGenAccount.setOnClickListener(new DebouncingOnClickListener() {
            @Override
            public void doClick(View p0) {
                genAccount();
            }
        });
        buttonLogin = (Button) findViewById(R.id.button_login);
        buttonLogin.setOnClickListener(new DebouncingOnClickListener() {
            @Override
            public void doClick(View p0) {
                login2();
            }
        });

        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        boolean privateAgree = sp.getBoolean("privateAgree", false);

        hostip = sp.getString("hostip", "");
        ipEditText.setText(hostip);

        regip = sp.getString("registerIp", "");
        if(regip.isEmpty()){
            regip = BuildConfig.REGISTER_IP;
            sp.edit().putString("registerIp", regip).apply();
        }
        regEditText.setText(regip);

        if(!privateAgree){
            View view = View.inflate(this, R.layout.private_policy, null);
            WebView webView = view.findViewById(R.id.privateText);
            //webView.loadUrl("http://www.crossim.im/privacy1.htm");
            webView.loadUrl("file:///android_asset/privacy1.html");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(view);
            builder.setCancelable(false);
            builder.setPositiveButton("同意", new DialogInterface.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sp.edit().putBoolean("privateAgree", true).apply();
                    runOnUiThread(()->{checkNeedPermission();});
                }
            });
            builder.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });

            AlertDialog dialog = builder.create();
            WindowManager windowManager = getWindowManager();
            Display display = windowManager.getDefaultDisplay();
            android.view.WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
            layoutParams.height = (int) (display.getHeight() * 0.8);
            layoutParams.width = (int) (display.getWidth() * 0.8);
            dialog.getWindow().setAttributes(layoutParams);
            dialog.show();
        }
        getShareList();
    }
    private void getShareList(){
        new Thread(()->{
            OKHttpHelper.get("http://" + regip + ":8600/nodeInfo", null, new SimpleCallback<String>() {
                @Override
                public void onUiSuccess(String s) {
                    try{
                        JSONObject json = JSON.parseObject(s);
                        JSONArray infos = json.getJSONArray("infos");

                    } catch (Exception e){
                    }
                }
                @Override
                public void onUiFailure(int code, String msg) {
                }
            });
        }).start();
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkNeedPermission() {
        boolean granted = true;
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                granted = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    break;
                }
            }
        }
        if(!granted){
            requestPermissions(permissions, 100);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "需要相关权限才能正常使用", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == 100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "授权失败", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
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

    @OnClick(R.id.resetHost)
    void resetHost(){
        hostip = ipEditText.getText().toString();
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        sp.edit().putString("hostip",hostip).commit();
        ChatManager.Instance().setHost(hostip);
        Toast.makeText(this, "切换服务器成功", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.resetReg)
    void resetReg(){
        regip = regEditText.getText().toString();
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        sp.edit().putString("registerIp",regip).commit();
        Toast.makeText(this, "切换服务器成功", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.toRegister)
    void toRegister(){
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.loginButton)
    void login() {

        String account = accountEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content("登录中...")
            .progress(true, 10)
            .cancelable(false)
            .build();
        dialog.show();

        new Thread(()->{
            Map<String, Object> params = new HashMap<>();
            params.put("command", "Login");
            params.put("name", account);
            params.put("token", ECUtils.osnHash(password.getBytes()));

            OKHttpHelper.post("http://" + regip + ":8101/", params, new SimpleCallback<String>() {

                @Override
                public void onUiSuccess(String s) {
                    new Thread(()->{
                        JSONObject json = JSON.parseObject(s);
                        String errCode = json.getString("errCode");
                        if(errCode == null || !errCode.equalsIgnoreCase("0:success")){
                            dialog.dismiss();
                            if(errCode == null){
                                errCode = "unknown error null";
                            }
                            String finalErrCode = errCode;
                            runOnUiThread(()->{Toast.makeText(LoginActivity.this, finalErrCode, Toast.LENGTH_SHORT).show();});
                            return;
                        }
                        JSONObject data = json.getJSONObject("data");
                        boolean result = ChatManagerHolder.gChatManager.connect(account, data.toString());
                        dialog.dismiss();
                        if(result){
                            String userID = ChatManagerHolder.gChatManager.getUserId();
                            SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
                            sp.edit().putString("id", userID).putString("token", data.toString()).apply();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            runOnUiThread(()->{Toast.makeText(LoginActivity.this, "登录失败", Toast.LENGTH_SHORT).show();});
                        }
                    }).start();
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    dialog.dismiss();
                    logInfo("code: "+code+", msg: "+msg);
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @OnClick(R.id.button_genAccount)
    void genAccount(){
        // 1. 检查是否已经生成账号
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        String osnID = sp.getString("osnID", "");
        String osnKey;
        if (osnID == null || osnID.equalsIgnoreCase("")){
            // 2. 生成账号
            String[] keyPair = ECUtils.createOsnID("user");
            if (keyPair != null){
                osnID = keyPair[0];
                osnKey = keyPair[1];
                // 3. 保存到配置文件中
                String name = "anonymous";
                String displayName = "anonymous";
                String nickName = "anonymous";
                /*
                JSONObject json = new JSONObject();
                json.put("osnID", osnID);
                json.put("privateKey", privateKey);
                json.put("name", name);
                json.put("displayName", displayName);
                json.put("nickName", nickName);*/
                String password = ECUtils.osnHash(name.getBytes());

                sp.edit().putString("osnID",osnID).commit();
                sp.edit().putString("osnKey",osnKey).commit();
                sp.edit().putString("name",name).commit();
                sp.edit().putString("password",password).commit();
                sp.edit().putString("displayName",displayName).commit();
                sp.edit().putString("nickName",nickName).commit();
                sp.edit().putInt("maxGroup",100).commit();
                Toast.makeText(this, "账号生成完成", Toast.LENGTH_SHORT).show();
            }

        }
        else
        {
            Toast.makeText(this, "账号已经存在", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.button_login)
    void login2(){

        // 设置registerip 为 host ip
        //hostip = regip;
        //SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        //sp.edit().putString("hostip",hostip).commit();
        //ChatManager.Instance().setHost(hostip);
        //Toast.makeText(this, "切换服务器成功", Toast.LENGTH_SHORT).show();



        /*
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("登录中...")
                .progress(true, 10)
                .cancelable(false)
                .build();
        dialog.show();
        */



        /*
        Map<String, Object> params = new HashMap<>();
        params.put("command", "Login");
        params.put("name", account);
        params.put("token", ECUtils.osnHash(password.getBytes()));
        */

        new Thread(()->{

            SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
            String name = sp.getString("name", "anonymous");
            String password = sp.getString("password", "");
            String osnID = sp.getString("osnID", "");
            String osnKey = sp.getString("osnKey", "");
            int maxGroup = sp.getInt("maxGroup", 100);

            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("password", password);
            data.put("osnID", osnID);
            data.put("osnKey", osnKey);
            data.put("maxGroup", maxGroup);
            boolean result = ChatManagerHolder.gChatManager.connect(name, data.toString());

            if (result) {
                String userID = ChatManagerHolder.gChatManager.getUserId();

                sp.edit().putString("id", userID).putString("token", data.toString()).apply();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "登录失败", Toast.LENGTH_SHORT).show();
            }

        }).start();
    }
}
