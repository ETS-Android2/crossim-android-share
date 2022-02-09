package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.content.Intent;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.notifylist.NotifyListActivity;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;

@ConversationInfoType(type = Conversation.ConversationType.Notify, line = 0)
@EnableContextMenu
public class NotifyConversationViewHolder extends ConversationViewHolder {
    public NotifyConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        GlideApp
                .with(fragment)
                .load(R.mipmap.ic_channel_1)
                .placeholder(R.mipmap.ic_channel_1)
                .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(fragment.getContext(), 4)))
                .into(portraitImageView);
        nameTextView.setText("通知消息");
    }

    public void onClick(View itemView) {
        ChatManager.Instance().clearUnreadStatus(new Conversation(Conversation.ConversationType.Notify,"0",0));
        Intent intent = new Intent(fragment.getActivity(), NotifyListActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        fragment.startActivity(intent);
    }
}
