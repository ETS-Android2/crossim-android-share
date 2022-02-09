/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;
import cn.wildfirechat.remote.UploadMediaCallback;

public class PickContactActivity extends WfcBaseActivity {
    public static final String PARAM_MAX_COUNT = "maxCount";
    public static final String PARAM_INITIAL_CHECKED_IDS = "initialCheckedIds";
    public static final String PARA_UNCHECKABLE_IDS = "uncheckableIds";
    public static final String RESULT_PICKED_USERS = "pickedUsers";

    private MenuItem menuItem;
    private TextView confirmTv;
    private boolean sharePick = false;
    private String shareText = null;
    private ArrayList<Uri> shareImage = null;

    private PickUserViewModel pickUserViewModel;
    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = new Observer<UIUserInfo>() {
        @Override
        public void onChanged(@Nullable UIUserInfo userInfo) {
            List<UIUserInfo> list = pickUserViewModel.getCheckedUsers();
            updatePickStatus(list);
        }
    };

    protected void updatePickStatus(List<UIUserInfo> userInfos) {
        if (userInfos == null || userInfos.isEmpty()) {
            confirmTv.setText("确定");
            menuItem.setEnabled(false);
        } else {
            confirmTv.setText("确定(" + userInfos.size() + ")");
            menuItem.setEnabled(true);
        }
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        pickUserViewModel = ViewModelProviders.of(this).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
        Intent intent = getIntent();
        int maxCount = intent.getIntExtra(PARAM_MAX_COUNT, 0);
        if (maxCount > 0) {
            pickUserViewModel.setMaxPickCount(maxCount);
        }

        pickUserViewModel.setInitialCheckedIds(intent.getStringArrayListExtra(PARAM_INITIAL_CHECKED_IDS));
        pickUserViewModel.setUncheckableIds(intent.getStringArrayListExtra(PARA_UNCHECKABLE_IDS));

        initView();

        intent = getIntent();
        if(intent != null){
            String action = intent.getAction();
            if(Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)){
                sharePick = true;
                String type = intent.getType();
                if(type.startsWith("image")){
                    if(Intent.ACTION_SEND.equals(action)) {
                        shareImage = new ArrayList<>();
                        shareImage.add(intent.getParcelableExtra(Intent.EXTRA_STREAM));
                    }
                    else
                        shareImage = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                }
                else{
                    shareText = intent.getStringExtra(Intent.EXTRA_TEXT);
                }
            }
        }
    }

    @Override
    protected int menu() {
        return R.menu.contact_pick;
    }

    @Override
    protected void afterMenus(Menu menu) {
        menuItem = menu.findItem(R.id.confirm);
        menuItem.setEnabled(false);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        confirmTv = menuItem.getActionView().findViewById(R.id.confirm_tv);
        confirmTv.setOnClickListener(v -> onOptionsItemSelected(menuItem));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            onConfirmClick();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        PickContactFragment fragment = new PickContactFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }

    protected void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos) {
        if(sharePick){
            MessageViewModel messageViewModel = ViewModelProviders.of(this).get(MessageViewModel.class);
            if(shareText != null){
                for(UIUserInfo uiUserInfo : newlyCheckedUserInfos){
                    UserInfo userInfo = uiUserInfo.getUserInfo();
                    messageViewModel.sendShareMsg(userInfo.uid, shareText, "来自分享", null, null, "身边大爱", "app://123456");
                }
            }
            else {
                try {
                    Uri uri = shareImage.get(0);
                    ContentResolver contentResolver = getContentResolver();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    InputStream inputStream = contentResolver.openInputStream(uri);
                    byte[] data = new byte[2048];
                    int readed = 0;
                    while((readed=inputStream.read(data))!=0) {
                        byteArrayOutputStream.write(data, 0, readed);
                        if(readed != 2048)
                            break;
                    }
                    ChatManager.Instance().uploadMedia(uri.getPath(), byteArrayOutputStream.toByteArray(), MessageContentMediaType.IMAGE.getValue(), new GeneralCallback2() {
                        @Override
                        public void onSuccess(String result) {
                            for (UIUserInfo uiUserInfo : newlyCheckedUserInfos) {
                                UserInfo userInfo = uiUserInfo.getUserInfo();
                                messageViewModel.sendShareMsg(userInfo.uid, "分享测试", "来自分享", result, null, "身边大爱", "app://123456");
                            }
                            finish();
                        }
                        @Override
                        public void onFail(int errorCode) {
                            finish();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            Intent intent = new Intent();
            ArrayList<UserInfo> newlyPickedInfos = new ArrayList<>();
            for (UIUserInfo info : newlyCheckedUserInfos) {
                newlyPickedInfos.add(info.getUserInfo());
            }
            intent.putExtra(RESULT_PICKED_USERS, newlyPickedInfos);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    protected void onConfirmClick() {
        List<UIUserInfo> newlyCheckedUserInfos = pickUserViewModel.getCheckedUsers();
        onContactPicked(newlyCheckedUserInfos);
    }

    public static Intent buildPickIntent(Context context, int maxCount, ArrayList<String> initialChecedIds, ArrayList<String> uncheckableIds) {
        Intent intent = new Intent(context, PickContactActivity.class);
        intent.putExtra(PARAM_MAX_COUNT, maxCount);
        intent.putExtra(PARAM_INITIAL_CHECKED_IDS, initialChecedIds);
        intent.putExtra(PARA_UNCHECKABLE_IDS, uncheckableIds);
        return intent;
    }
}
