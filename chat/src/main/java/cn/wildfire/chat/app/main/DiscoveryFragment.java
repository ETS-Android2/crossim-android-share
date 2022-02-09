/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.WfcIntent;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.chatroom.ChatRoomListActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.litapp.LitappActivity;
import cn.wildfire.chat.kit.litapp.LitappListActivity;
import cn.wildfire.chat.kit.litapp.LitappViewModel;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
//import cn.wildfire.chat.kit.voip.conference.CreateConferenceActivity;
import cn.wildfire.chat.kit.widget.OptionItemView;
//import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.remote.ChatManager;

public class DiscoveryFragment extends Fragment {
    @BindView(R.id.openspn)
    OptionItemView momentOptionItemView;
    @BindView(R.id.crossim)
    OptionItemView conferenceOptionItemView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment_discovery, container, false);
        ButterKnife.bind(this, view);
        initMoment();
//        if (!AVEngineKit.isSupportConference()) {
//            conferenceOptionItemView.setVisibility(View.GONE);
//        }
        return view;
    }

    private void updateMomentBadgeView() {
        List<Message> messages = ChatManager.Instance().getMessagesEx2(Collections.singletonList(Conversation.ConversationType.Single), Collections.singletonList(1), Arrays.asList(MessageStatus.Unread), 0, true, 100, null);
        int count = messages == null ? 0 : messages.size();
        momentOptionItemView.setBadgeCount(count);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (WfcUIKit.getWfcUIKit().isSupportMoment()) {
            updateMomentBadgeView();
        }
    }

    @OnClick(R.id.crossim)
    void chatRoom() {
        Intent intent = new Intent(getActivity(), ChatRoomListActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.robotOptionItemView)
    void robot() {
        Intent intent = ConversationActivity.buildConversationIntent(getActivity(), Conversation.ConversationType.Single, "FireRobot", 0);
        startActivity(intent);
    }

    @OnClick(R.id.channelOptionItemView)
    void channel() {

    }

    @OnClick(R.id.cookbookOptionItemView)
    void cookbook() {
        String title = getString(R.string.app_doc);
        WfcWebViewActivity.loadUrl(getContext(), title, "https://docs.wildfirechat.cn");
    }

    @OnClick(R.id.litboyOptionItemView)
    void litboy(){
        Intent intent = new Intent(getContext(), LitappListActivity.class);
        startActivity(intent);
    }


    private void initMoment() {
//        if (!WfcUIKit.getWfcUIKit().isSupportMoment()) {
//            momentOptionItemView.setVisibility(View.GONE);
//            return;
//        }
        MessageViewModel messageViewModel = ViewModelProviders.of(this).get(MessageViewModel.class);
        messageViewModel.messageLiveData().observe(getViewLifecycleOwner(), uiMessage -> updateMomentBadgeView());
        messageViewModel.clearMessageLiveData().observe(getViewLifecycleOwner(), o -> updateMomentBadgeView());

        //ChatManager.Instance().getLitappInfo("OSNS6qJNK58n44LbMw4W8jfAGetyXnYNnP5hLAj8XmmMk37vpD7", true);
    }

    @OnClick(R.id.openspn)
    void moment() {
        LitappInfo litappInfo = ChatManager.Instance().getLitappInfo("OSNS6qJNK58n44LbMw4W8jfAGetyXnYNnP5hLAj8XmmMk37vpD7", false);
        if(litappInfo != null){
            Intent intent = new Intent(getContext(), LitappActivity.class);
            intent.putExtra("litappInfo", litappInfo);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @OnClick(R.id.conferenceOptionItemView)
    void conference() {
//        Intent intent = new Intent(getActivity(), CreateConferenceActivity.class);
//        startActivity(intent);
    }
}
