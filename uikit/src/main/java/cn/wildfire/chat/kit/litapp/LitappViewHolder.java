/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.litapp;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.LitappInfo;

public class LitappViewHolder extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    private LitappListAdapter adapter;
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;

    protected LitappInfo litappInfo;

    public LitappViewHolder(Fragment fragment, LitappListAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        ButterKnife.bind(this, itemView);
    }

    // TODO hide the last diver line
    public void onBind(LitappInfo litappInfo) {
        this.litappInfo = litappInfo;
        nameTextView.setText(litappInfo.name);
        GlideApp.with(fragment).load(litappInfo.portrait).placeholder(R.mipmap.ic_channel_1).into(portraitImageView);
    }

    public LitappInfo getLitappInfo() {
        return litappInfo;
    }
}
