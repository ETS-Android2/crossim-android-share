package cn.wildfire.chat.kit.notifylist;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class NotifyListActivity extends WfcBaseActivity {
    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }
    @Override
    protected void afterViews() {
        NotifyListFragment fragment = new NotifyListFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }
}
