package cn.wildfire.chat.kit.contact.newfriend;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.group.GroupInfoActivity;
import cn.wildfire.chat.kit.litapp.LitappInfoActivity;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetGroupInfoCallback;
import cn.wildfirechat.remote.GetLitappInfoCallback;
import cn.wildfirechat.remote.GetUserInfoCallback;

public class AddOsnIDActivity extends WfcBaseActivity {
    @BindView(R2.id.content)
    EditText content;

    @Override
    protected int contentLayout() {
        return R.layout.activity_add_osnid;
    }
    @OnClick(R2.id.addTarget)
    protected void onClick(View view) {
        String osnID = content.getText().toString();
        if(osnID.startsWith("OSNU")){
            ChatManager.Instance().getUserInfo(osnID, false, new GetUserInfoCallback() {
                @Override
                public void onSuccess(UserInfo userInfo) {
                    Intent intent = new Intent(AddOsnIDActivity.this, UserInfoActivity.class);
                    intent.putExtra("userInfo", userInfo);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFail(int errorCode) {
                    Toast.makeText(AddOsnIDActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else if(osnID.startsWith("OSNG")){
            ChatManager.Instance().getGroupInfo(osnID, false, new GetGroupInfoCallback() {
                @Override
                public void onSuccess(GroupInfo groupInfo) {
                    Intent intent = new Intent(AddOsnIDActivity.this, GroupInfoActivity.class);
                    intent.putExtra("groupId", groupInfo.target);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFail(int errorCode) {
                    Toast.makeText(AddOsnIDActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else if(osnID.startsWith("OSNS")){
            ChatManager.Instance().getLitappInfoEx(osnID, false, new GetLitappInfoCallback() {
                @Override
                public void onSuccess(LitappInfo litappInfo) {
                    Intent intent = new Intent(AddOsnIDActivity.this, LitappInfoActivity.class);
                    intent.putExtra("litappId", litappInfo.target);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFail(int errorCode) {
                    Toast.makeText(AddOsnIDActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else{
            Toast.makeText(this, "错误的OsnID", Toast.LENGTH_SHORT).show();
        }
    }
}
