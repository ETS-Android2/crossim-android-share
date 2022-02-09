/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactListActivity;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.conversationlist.notification.ConnectionStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.PCOnlineStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotificationViewModel;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.viewmodel.SettingViewModel;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusLogout;

public class ConversationListFragment2 extends ProgressFragment {
    private RecyclerView recyclerView;
    private ConversationListAdapter adapter;
    private static final List<Conversation.ConversationType> types = Arrays.asList(Conversation.ConversationType.Single,
            Conversation.ConversationType.Group,
            Conversation.ConversationType.Channel);
    private static final List<Integer> lines = Arrays.asList(0);

    private ContactViewModel contactViewModel;
    private ConversationListViewModel conversationListViewModel;
    private SettingViewModel settingViewModel;
    private LinearLayoutManager layoutManager;
    private Menu mMenu;
    private boolean notifyNewContract = false;

    @Override
    protected int contentLayout() {
        return R.layout.conversationlist_frament;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu, inflater);

        menu.clear();
        inflater.inflate(R.menu.conversation_list_simpilfy, menu);
        mMenu = menu;
        updateNotifyMenu();
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.addressBook) {
            notifyNewContract = false;
            updateNotifyMenu();
            Intent intent = new Intent(this.getContext(), ContactListActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    void updateNotifyMenu(){
        if(mMenu != null){
            MenuItem menuItem = mMenu.findItem(R.id.addressBook);
            menuItem.setIconTintList(null);
            menuItem.setIcon(notifyNewContract ? R.mipmap.icon_address_book_white_notify : R.mipmap.icon_address_book_white);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void afterViews(View view) {
        setHasOptionsMenu(true);
        recyclerView = view.findViewById(R.id.recyclerView);
        init();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (adapter != null && isVisibleToUser) {
            reloadConversations();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadConversations();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void init() {
        adapter = new ConversationListAdapter(this);
        conversationListViewModel = ViewModelProviders
                .of(getActivity(), new ConversationListViewModelFactory(types, lines))
                .get(ConversationListViewModel.class);
        conversationListViewModel.conversationListLiveData().observe(this, conversationInfos -> {
            showContent();
            adapter.setConversationInfos(conversationInfos);
        });
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setAdapter(adapter);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        userViewModel.userInfoLiveData().observe(this, new Observer<List<UserInfo>>() {
            @Override
            public void onChanged(List<UserInfo> userInfos) {
                int start = layoutManager.findFirstVisibleItemPosition();
                int end = layoutManager.findLastVisibleItemPosition();
                adapter.notifyItemRangeChanged(start, end - start + 1);
            }
        });
        GroupViewModel groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        groupViewModel.groupInfoUpdateLiveData().observe(this, new Observer<List<GroupInfo>>() {
            @Override
            public void onChanged(List<GroupInfo> groupInfos) {
                int start = layoutManager.findFirstVisibleItemPosition();
                int end = layoutManager.findLastVisibleItemPosition();
                adapter.notifyItemRangeChanged(start, end - start + 1);
            }
        });

        StatusNotificationViewModel statusNotificationViewModel = WfcUIKit.getAppScopeViewModel(StatusNotificationViewModel.class);
        statusNotificationViewModel.statusNotificationLiveData().observe(this, new Observer<Object>() {
            @Override
            public void onChanged(Object o) {
                adapter.updateStatusNotification(statusNotificationViewModel.getNotificationItems());
            }
        });
        conversationListViewModel.connectionStatusLiveData().observe(this, status -> {
            ConnectionStatusNotification connectionStatusNotification = new ConnectionStatusNotification();
            switch (status) {
                case ConnectionStatus.ConnectionStatusConnecting:
                    connectionStatusNotification.setValue("正在连接...");
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusReceiveing:
                    connectionStatusNotification.setValue("正在同步...");
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusConnected:
                    statusNotificationViewModel.hideStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusUnconnected:
                    connectionStatusNotification.setValue("连接失败");
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusLogout:
                    ChatManager.Instance().clearFriend(null);
                    ChatManager.Instance().clearConversation(true);
                    adapter.setConversationInfos(new ArrayList<>());
                    break;
                default:
                    break;
            }
        });
        settingViewModel = new ViewModelProvider(this).get(SettingViewModel.class);
        settingViewModel.settingUpdatedLiveData().observe(this, o -> {
            if (ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusReceiveing) {
                return;
            }
            conversationListViewModel.reloadConversationList(true);
            conversationListViewModel.reloadConversationUnreadStatus();

            List<PCOnlineInfo> infos = ChatManager.Instance().getPCOnlineInfos();
            statusNotificationViewModel.clearStatusNotificationByType(PCOnlineStatusNotification.class);
            if (infos.size() > 0) {
                for (PCOnlineInfo info : infos) {
                    PCOnlineStatusNotification notification = new PCOnlineStatusNotification(info);
                    statusNotificationViewModel.showStatusNotification(notification);

                    SharedPreferences sp = getActivity().getSharedPreferences("config", Context.MODE_PRIVATE);
                    sp.edit().putBoolean("wfc_uikit_had_pc_session", true).commit();
                }
            }
        });
        List<PCOnlineInfo> pcOnlineInfos = ChatManager.Instance().getPCOnlineInfos();
        if (pcOnlineInfos != null && !pcOnlineInfos.isEmpty()) {
            for (PCOnlineInfo info : pcOnlineInfos) {
                PCOnlineStatusNotification notification = new PCOnlineStatusNotification(info);
                statusNotificationViewModel.showStatusNotification(notification);

                SharedPreferences sp = getActivity().getSharedPreferences("config", Context.MODE_PRIVATE);
                sp.edit().putBoolean("wfc_uikit_had_pc_session", true).commit();
            }
        }
        contactViewModel = ViewModelProviders.of(this).get(ContactViewModel.class);
        contactViewModel.friendRequestUpdatedLiveData().observe(this, count -> {
            if (count == null || count == 0) {
                notifyNewContract = false;
            } else {
                notifyNewContract = true;
            }
            updateNotifyMenu();
        });
    }

    private void reloadConversations() {
        if (ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusReceiveing ||
                ChatManager.Instance().getConnectionStatus() == ConnectionStatusLogout) {
            return;
        }
        conversationListViewModel.reloadConversationList();
        conversationListViewModel.reloadConversationUnreadStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}
