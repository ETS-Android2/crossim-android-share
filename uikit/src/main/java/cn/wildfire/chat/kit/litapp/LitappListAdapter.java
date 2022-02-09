/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.litapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.channel.viewholder.CategoryViewHolder;
import cn.wildfire.chat.kit.channel.viewholder.ChannelViewHolder;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.group.GroupViewHolder;
import cn.wildfire.chat.kit.group.OnGroupItemClickListener;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.LitappInfo;

public class LitappListAdapter extends RecyclerView.Adapter<LitappViewHolder> {
    private List<LitappInfo> litappInfos;
    private Fragment fragment;
    private OnLitappClickListener onLitappClickListener;

    public LitappListAdapter(Fragment fragment) {
        this.fragment = fragment;
    }

    public void setLitappInfos(List<LitappInfo> litappInfos) {
        this.litappInfos = litappInfos;
    }

    public List<LitappInfo> getLitappInfos() {
        return litappInfos;
    }

    public void setOnLitappItemClickListener(OnLitappClickListener onLitappItemClickListener) {
        this.onLitappClickListener = onLitappItemClickListener;
    }

    @NonNull
    @Override
    public LitappViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.litapp_list_item, parent, false);
        LitappViewHolder viewHolder = new LitappViewHolder(fragment, this, view);
        view.findViewById(R.id.contactLinearLayout).setOnClickListener(v -> {
            if (onLitappClickListener != null) {
                onLitappClickListener.onLitappClick(viewHolder.getLitappInfo());
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull LitappViewHolder holder, int position) {
        holder.onBind(litappInfos.get(position));
    }

    @Override
    public int getItemCount() {
        return litappInfos == null ? 0 : litappInfos.size();
    }

    public interface OnLitappClickListener {
        void onLitappClick(LitappInfo litappInfo);
    }
}
