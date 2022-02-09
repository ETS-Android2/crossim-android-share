/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.litapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetLitappsCallback;


public class LitappListFragment extends Fragment implements LitappListAdapter.OnLitappClickListener {
    @BindView(R2.id.recyclerView)
    RecyclerView recyclerView;
    private LitappListAdapter litappListAdapter;
    private boolean pick;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            pick = args.getBoolean("pick", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.channel_list_frament, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void init() {
        litappListAdapter = new LitappListAdapter(this);
        litappListAdapter.setOnLitappItemClickListener(this);

        recyclerView.setAdapter(litappListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ChatManager.Instance().getLitappList(new GetLitappsCallback() {
            @Override
            public void onSuccess(List<LitappInfo> litappInfos) {
                if(litappInfos == null)
                    litappInfos = new ArrayList<>();
                litappListAdapter.setLitappInfos(litappInfos);
                litappListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFail(int errorCode) {
            }
        });
    }

    @Override
    public void onLitappClick(LitappInfo litappInfo) {
        if (pick) {
            Intent intent = new Intent();
            intent.putExtra("litappInfo", litappInfo);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        } else {
            Intent intent = new Intent(getContext(),LitappActivity.class);
            intent.putExtra("litappInfo", litappInfo);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }
}
