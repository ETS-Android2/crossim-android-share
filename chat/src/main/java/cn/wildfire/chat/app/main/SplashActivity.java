/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import butterknife.ButterKnife;
import cn.wildfire.chat.app.login.LoginActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfire.chat.app.BaseApp.getContext;

public class SplashActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_DRAW_OVERLAY = 101;

    private SharedPreferences sharedPreferences;
    private String id;
    private String token;

    private void hideStatusBar() {
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
        hideStatusBar();

        sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        id = sharedPreferences.getString("id", null);
        token = sharedPreferences.getString("token", null);

        showNextScreen();
    }

    private void showNextScreen() {
        if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(token)) {
            new Thread(()->{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ChatManager.Instance().connect(id, token);
                runOnUiThread(this::showMain);
            }).start();
//            showMain();
        } else {
            showLogin();
        }
    }

    private void showMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
            android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        startActivity(intent, bundle);
        finish();
    }

    private void showLogin() {
        Intent intent;
        //intent = new Intent(this, SMSLoginActivity.class);
        intent = new Intent(this, LoginActivity.class);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
            android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        startActivity(intent, bundle);
        finish();
    }
}
