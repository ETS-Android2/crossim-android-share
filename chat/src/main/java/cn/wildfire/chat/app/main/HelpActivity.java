package cn.wildfire.chat.app.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

public class HelpActivity extends WfcBaseActivity {

    @BindView(R2.id.context)
    EditText etContext;

    @Override
    protected void afterViews() {
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_help;
    }

    @OnClick(R2.id.btn_submit)
    protected void onClick(View view) {
        String context = etContext.getText().toString();
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("正在发送")//标题
                .setMessage("正在发送，轻稍候。。。")//内容
                .setIcon(R.mipmap.ic_channel)//图标
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(HelpActivity.this, "这是取消按钮", Toast.LENGTH_SHORT).show();
                    }})
                .create();
        alertDialog.show();
        ChatManager.Instance().sendBroadcast(context, new GeneralCallback() {
            @Override
            public void onSuccess() {
                alertDialog.dismiss();
                Toast.makeText(HelpActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(int errorCode) {
                alertDialog.dismiss();
                Toast.makeText(HelpActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
