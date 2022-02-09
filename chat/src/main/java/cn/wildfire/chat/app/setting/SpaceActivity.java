package cn.wildfire.chat.app.setting;

import android.widget.EditText;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;

import java.util.Collections;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.ModifyMyInfoType;
import cn.wildfirechat.model.UserInfo;

public class SpaceActivity extends WfcBaseActivity {
    @BindView(R.id.spaceUrl)
    EditText spaceUrl;

    private UserViewModel userViewModel;
    private UserInfo userInfo;

    @Override
    protected int contentLayout() {
        return R.layout.activity_space;
    }

    @Override
    protected void afterViews() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        userInfo = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        if(userInfo != null)
            spaceUrl.setText(userInfo.urlSpace);
    }

    @OnClick(R.id.spaceSet)
    public void setSpaceUrl(){
        ModifyMyInfoEntry entry = new ModifyMyInfoEntry();
        entry.type = ModifyMyInfoType.Modify_UrlSpace;
        entry.value = spaceUrl.getText().toString();
        userViewModel.modifyMyInfo(Collections.singletonList(entry));
        finish();
    }
}
