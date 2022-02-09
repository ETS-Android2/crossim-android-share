/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.group.GroupInfoActivity;
import cn.wildfire.chat.kit.litapp.LitappActivity;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.message.CardMessageContent;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

import static cn.wildfirechat.message.CardMessageContent.CardType_Litapp;
import static cn.wildfirechat.message.CardMessageContent.CardType_Share;

@MessageContentType(value = {
    CardMessageContent.class,

})
@EnableContextMenu
public class UserCardMessageContentViewHolder extends NormalMessageContentViewHolder {
    @BindView(R2.id.contentLayout)
    RelativeLayout contentLayout;
    @BindView(R2.id.userCardPortraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.userCardNameTextView)
    TextView nameTextView;
    @BindView(R2.id.userIdTextView)
    TextView userIdTextView;
    @BindView(R2.id.cardType)
    TextView cardType;

    @BindView(R2.id.contentLayout2)
    RelativeLayout contentLayout2;
    @BindView(R2.id.userCardPortraitImageView2)
    ImageView portraitImageView2;
    @BindView(R2.id.userCardNameTextView2)
    TextView nameTextView2;
    @BindView(R2.id.content)
    TextView content;
    @BindView(R2.id.theme)
    ImageView theme;
    @BindView(R2.id.cardType2)
    TextView cardType2;
    @BindView(R2.id.icon)
    ImageView icon;

    CardMessageContent userCardMessageContent;

    public UserCardMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBind(UiMessage message) {
        userCardMessageContent = (CardMessageContent) message.message.content;
        if(userCardMessageContent.getType() == CardType_Litapp){
            contentLayout.setVisibility(View.GONE);
            nameTextView2.setText(userCardMessageContent.getName());
            content.setText(userCardMessageContent.getDisplayName());
            cardType2.setText("小程序名片");
            GlideApp
                    .with(fragment)
                    .load(userCardMessageContent.getPortrait())
                    .transforms(new CenterCrop(), new RoundedCorners(10))
                    .placeholder(R.mipmap.avatar_def)
                    .into(portraitImageView2);
            String themes = userCardMessageContent.getTheme();
            if(themes == null || themes.isEmpty())
                theme.setVisibility(View.GONE);
            else{
                GlideApp
                        .with(fragment)
                        .load(themes)
                        .transforms(new CenterCrop(), new RoundedCorners(10))
                        .placeholder(R.mipmap.avatar_def)
                        .into(theme);
            }
        }
        else if(userCardMessageContent.getType() == CardType_Share){
            contentLayout.setVisibility(View.GONE);
            nameTextView2.setText(userCardMessageContent.getName());
            content.setText(userCardMessageContent.getDisplayName());
            cardType2.setText(userCardMessageContent.getTarget());
            GlideApp
                    .with(fragment)
                    .load(userCardMessageContent.getPortrait())
                    .transforms(new CenterCrop(), new RoundedCorners(10))
                    .placeholder(R.mipmap.avatar_def)
                    .into(portraitImageView2);
            GlideApp
                    .with(fragment)
                    .load(userCardMessageContent.getPortrait())
                    .transforms(new CenterCrop(), new RoundedCorners(10))
                    .placeholder(R.mipmap.avatar_def)
                    .into(icon);
            String themes = userCardMessageContent.getTheme();
            if(themes == null || themes.isEmpty())
                theme.setVisibility(View.GONE);
            else{
                GlideApp
                        .with(fragment)
                        .load(themes)
                        .transforms(new CenterCrop(), new RoundedCorners(10))
                        .placeholder(R.mipmap.avatar_def)
                        .into(theme);
            }
        }
        else {
            contentLayout2.setVisibility(View.GONE);
            nameTextView.setText(userCardMessageContent.getDisplayName());
            userIdTextView.setText(userCardMessageContent.getName());
            switch (userCardMessageContent.getType()) {
                case 0:
                    cardType.setText("个人名片");
                    break;
                case 1:
                    cardType.setText("群组名片");
                    break;
                case 2:
                    cardType.setText("聊天室名片");
                    break;
                case 3:
                    cardType.setText("频道名片");
                    break;
            }
            GlideApp
                    .with(fragment)
                    .load(userCardMessageContent.getPortrait())
                    .transforms(new CenterCrop(), new RoundedCorners(10))
                    .placeholder(R.mipmap.avatar_def)
                    .into(portraitImageView);
        }
    }

    @OnClick(R2.id.contentLayout)
    void onUserCardClick() {
        if(userCardMessageContent.getType() == 1){
            Intent intent = new Intent(fragment.getContext(), GroupInfoActivity.class);
            intent.putExtra("groupId", userCardMessageContent.getTarget());
            fragment.startActivity(intent);
        }
        else{
            Intent intent = new Intent(fragment.getContext(), UserInfoActivity.class);
            UserInfo userInfo = ChatManager.Instance().getUserInfo(userCardMessageContent.getTarget(), false);
            intent.putExtra("userInfo", userInfo);
            fragment.startActivity(intent);
        }
    }
    @OnClick(R2.id.contentLayout2)
    void onUserCardClick2() {
        if(userCardMessageContent.getType() == CardType_Share){
            String url = userCardMessageContent.getUrl();
            if(url.startsWith("app://")){
                MessageViewModel messageViewModel = ViewModelProviders.of(fragment).get(MessageViewModel.class);
                messageViewModel.onReportMessage(url);
            }
        }
        else{
            LitappInfo litappInfo = new LitappInfo();
            litappInfo.target = userCardMessageContent.getTarget();
            litappInfo.name = userCardMessageContent.getName();
            litappInfo.displayName = userCardMessageContent.getDisplayName();
            litappInfo.portrait = userCardMessageContent.getPortrait();
            litappInfo.theme = userCardMessageContent.getTheme();
            litappInfo.url = userCardMessageContent.getUrl();
            ChatManager.Instance().addLitapp(litappInfo, new GeneralCallback() {
                @Override
                public void onSuccess() {
                }
                @Override
                public void onFail(int errorCode) {
                }
            });
            Intent intent = new Intent(fragment.getContext(), LitappActivity.class);
            intent.putExtra("litappInfo", litappInfo);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            fragment.startActivity(intent);
        }
    }
}
