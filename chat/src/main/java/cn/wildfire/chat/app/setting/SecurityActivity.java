package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.app.main.SplashActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfirechat.chat.R;

public class SecurityActivity extends WfcBaseActivity {

    @BindView(R.id.exitOptionItemView)
    TextView exitOptionItemView;

    @Override
    protected int contentLayout() {
        return R.layout.activity_security;
    }

    @Override
    protected void afterViews() {

    }

    @OnClick(R.id.exitOptionItemView)
    public void onLogout(){
        ChatManagerHolder.gChatManager.disconnect(true, false);
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        sp = getSharedPreferences("moment", Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        OKHttpHelper.clearCookies();

        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
