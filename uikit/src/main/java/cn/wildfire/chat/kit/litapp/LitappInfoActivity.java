/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.litapp;

import android.content.Intent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.remote.ChatManager;

public class LitappInfoActivity extends WfcBaseActivity {
    private String userId;
    private String litappId;
    private LitappInfo litappInfo;
    private boolean isJoined;
    private LitappViewModel litappViewModel;

    @BindView(R2.id.groupNameTextView)
    TextView groupNameTextView;
    @BindView(R2.id.portraitImageView)
    ImageView groupPortraitImageView;
    @BindView(R2.id.actionButton)
    Button actionButton;

    private MaterialDialog dialog;

    @Override
    protected void afterViews() {
        Intent intent = getIntent();
        litappId = intent.getStringExtra("litappId");

        litappViewModel = ViewModelProviders.of(this).get(LitappViewModel.class);
        litappViewModel.litappInfoLiveData().observe(this, litappInfos -> {
            dismissLoading();
            for (LitappInfo info : litappInfos) {
                litappInfo = info;
                showLitappInfo(info);
            }
        });

        litappInfo = ChatManager.Instance().getLitappInfo(litappId, true);
        if(litappInfo != null)
            showLitappInfo(litappInfo);
        else
            showLoading();
    }

    private void updateActionButtonStatus() {
        if (isJoined) {
            actionButton.setText("进入群聊");
        } else {
            actionButton.setText("加入群聊");
        }
    }

    private void showLoading() {
        if (dialog == null) {
            dialog = new MaterialDialog.Builder(this)
                    .progress(true, 100)
                    .build();
            dialog.show();
        }
    }

    private void dismissLoading() {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }
        dialog.dismiss();
        dialog = null;
    }

    private void showLitappInfo(LitappInfo litappInfo) {
        if (litappInfo == null) {
            return;
        }
        GlideApp.with(this)
                .load(litappInfo.portrait)
                .placeholder(R.mipmap.ic_group_cheat)
                .into(groupPortraitImageView);
        groupNameTextView.setText(litappInfo.name);
        actionButton.setText("打开小程序");
    }

    @Override
    protected int contentLayout() {
        return R.layout.litapp_info_activity;
    }

    @OnClick(R2.id.actionButton)
    void action() {
        Intent intent = new Intent(this,LitappActivity.class);
        intent.putExtra("litappInfo", litappInfo);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
