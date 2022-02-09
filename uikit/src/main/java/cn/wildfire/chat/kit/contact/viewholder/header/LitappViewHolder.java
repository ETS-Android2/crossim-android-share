package cn.wildfire.chat.kit.contact.viewholder.header;


import android.view.View;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.HeaderValue;

@SuppressWarnings("unused")
public class LitappViewHolder extends HeaderViewHolder<HeaderValue> {

    public LitappViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(HeaderValue headerValue) {

    }
}
