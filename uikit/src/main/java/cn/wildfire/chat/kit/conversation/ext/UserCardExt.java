/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.contact.ContactListActivity;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.conversation.forward.ForwardPromptView;
import cn.wildfirechat.message.CardMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.CardMessageContent.CardType_Channel;
import static cn.wildfirechat.message.CardMessageContent.CardType_Group;
import static cn.wildfirechat.message.CardMessageContent.CardType_Litapp;
import static cn.wildfirechat.message.CardMessageContent.CardType_User;

public class UserCardExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation
     */
    @ExtContextMenuItem
    public void pickContact(View containerView, Conversation conversation) {
        Intent intent = new Intent(fragment.getActivity(), ContactListActivity.class);
        if (conversation.type == Conversation.ConversationType.Single) {
            ArrayList<String> filterUserList = new ArrayList<>();
            filterUserList.add(conversation.target);
            intent.putExtra(ContactListActivity.FILTER_USER_LIST, filterUserList);
        }
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            UserInfo userInfo = data.getParcelableExtra("userInfo");
            ChannelInfo channelInfo = data.getParcelableExtra("channelInfo");
            GroupInfo groupInfo =  data.getParcelableExtra("groupInfo");
            LitappInfo litappInfo = data.getParcelableExtra("litappInfo");
            if (userInfo != null) {
                sendUserCard(new CardMessageContent(CardType_User, userInfo.uid, userInfo.name, userInfo.displayName, userInfo.portrait));
            } else if (channelInfo != null) {
                sendUserCard(new CardMessageContent(CardType_Channel, channelInfo.channelId, channelInfo.name, channelInfo.name, channelInfo.portrait));
            } else if(groupInfo != null){
                sendUserCard(new CardMessageContent(CardType_Group, groupInfo.target, groupInfo.name, groupInfo.name, groupInfo.portrait));
            } else if(litappInfo != null){
                sendUserCard(new CardMessageContent(CardType_Litapp, litappInfo.target, litappInfo.name, litappInfo.displayName, litappInfo.portrait, litappInfo.theme, litappInfo.url));
            }
        }
    }

    private void sendUserCard(CardMessageContent cardMessageContent) {
        ForwardPromptView view = new ForwardPromptView(fragment.getActivity());
        String desc = "";
        switch (cardMessageContent.getType()) {
            case 0:
                desc = "[个人名片]";
                break;
            case 1:
                desc = "[群组]";
                break;
            case 2:
                desc = "[聊天室]";
                break;
            case 3:
                desc = "[频道]";
                break;
            case 4:
                desc = "[小程序]";
                break;
            default:
                break;
        }

        desc += cardMessageContent.getDisplayName();

        if (conversation.type == Conversation.ConversationType.Single) {
            UserInfo targetUser = ChatManager.Instance().getUserInfo(conversation.target, false);
            view.bind(targetUser.displayName, targetUser.portrait, desc);
        } else if (conversation.type == Conversation.ConversationType.Group) {
            GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(conversation.target, false);
            view.bind(groupInfo.name, groupInfo.portrait, desc);
        }
        MaterialDialog dialog = new MaterialDialog.Builder(fragment.getActivity())
            .customView(view, false)
            .negativeText("取消")
            .positiveText("发送")
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    //CardMessageContent cardMessageContent = new CardMessageContent(type, target, name, displayName, portrait);
                    //cardMessageContent.setName(name);
                    messageViewModel.sendMessage(conversation, cardMessageContent);
                    if (!TextUtils.isEmpty(view.getEditText())) {
                        TextMessageContent content = new TextMessageContent(view.getEditText());
                        messageViewModel.sendMessage(conversation, content);
                    }
                    dialog.dismiss();
                }
            })
            .build();
        dialog.show();
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_user_card;
    }

    @Override
    public String title(Context context) {
        return "名片";
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return title(context);
    }
}
