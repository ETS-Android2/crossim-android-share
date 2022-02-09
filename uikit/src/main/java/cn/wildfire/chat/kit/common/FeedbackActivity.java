package cn.wildfire.chat.kit.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Random;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;

public class FeedbackActivity extends WfcBaseActivity {

    @BindView(R2.id.user)
    EditText etUser;
    @BindView(R2.id.phone)
    EditText etPhone;
    @BindView(R2.id.context)
    EditText etContext;

    @Override
    protected void afterViews() {
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_feedback;
    }

    @OnClick(R2.id.btn_submit)
    protected void onClick(View view) {
        String user = etUser.getText().toString();
        String phone = etPhone.getText().toString();
        String context = etContext.getText().toString();

        if (TextUtils.isEmpty(user) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(context)) {
            Toast.makeText(this, "输入内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("正在提交")//标题
                .setMessage("正在提交，轻稍候。。。")//内容
                .setIcon(R.mipmap.ic_channel)//图标
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(FeedbackActivity.this, "这是取消按钮", Toast.LENGTH_SHORT).show();
                    }})
                .create();
        alertDialog.show();

        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        String hostIp = sp.getString("hostip", null);
        OKHttpHelper.get("http://" + hostIp + ":8300/userFeedback", null, new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String s) {
                runOnUiThread(()->{
                    Random random = new Random();
                    int delayed = random.nextInt(3000);
                    try {
                        Thread.sleep(delayed);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(FeedbackActivity.this, "提交完成", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                    finish();
                });
            }
            @Override
            public void onUiFailure(int code, String msg) {
                runOnUiThread(()->{
                    Toast.makeText(FeedbackActivity.this, "提交失败", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                    finish();
                });
            }
        });
    }
}
