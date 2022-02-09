package cn.wildfire.chat.kit.notifylist;

import android.view.View;

import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversationlist.ConversationListAdapter;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.model.Conversation;

public class NotifyListFragment extends ProgressFragment {
    private RecyclerView recyclerView;
    private ConversationListAdapter adapter;
    private static final List<Conversation.ConversationType> types = Collections.singletonList(Conversation.ConversationType.Service);
    private static final List<Integer> lines = Collections.singletonList(0);

    private ConversationListViewModel conversationListViewModel;
    private LinearLayoutManager layoutManager;

    @Override
    protected int contentLayout() {
        return R.layout.conversationlist_frament;
    }

    @Override
    protected void afterViews(View view) {
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
    }

    private void reloadConversations() {
        conversationListViewModel.reloadConversationList();
        conversationListViewModel.reloadConversationUnreadStatus();
    }
}
