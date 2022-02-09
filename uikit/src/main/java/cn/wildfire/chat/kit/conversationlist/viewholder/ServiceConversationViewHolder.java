package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.LitappInfo;

@ConversationInfoType(type = Conversation.ConversationType.Service, line = 0)
@EnableContextMenu
public class ServiceConversationViewHolder extends ConversationViewHolder{
    public ServiceConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        LitappInfo litappInfo = ChatManagerHolder.gChatManager.getLitappInfo(conversationInfo.conversation.target, false);
        String name;
        String portrait;
        if (litappInfo != null) {
            name = litappInfo.name;
            portrait = litappInfo.portrait;
        } else {
            name = "通知";
            portrait = null;
        }

        if (TextUtils.isEmpty(portrait)) {
            portrait = ImageUtils.getGroupGridPortrait(getFragment().getContext(), conversationInfo.conversation.target, 60);
        }

        GlideApp
                .with(fragment)
                .load(portrait)
                .placeholder(R.mipmap.ic_group_cheat)
                .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(fragment.getContext(), 4)))
                .into(portraitImageView);
        nameTextView.setText(name);
    }
}
