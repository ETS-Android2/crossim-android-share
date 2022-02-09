package cn.wildfire.chat.kit.utils;

import android.text.TextUtils;
import android.widget.TextView;

/**
 * 用户信息设置工具
 */
public class UserInfoSetUtil {

    /**
     * 设置用户名
     *
     * @param remarks 备注
     * @param nickname 昵称
     * @param isOneSelf 是否本人
     * nameTextView.setText(
     */
    public static void setUserNameToTextView(TextView tv_username, String remarks, String nickname, boolean isOneSelf) {
        if (tv_username != null) {
            if(isOneSelf) {
                //昵称
                if (!TextUtils.isEmpty(nickname)) {
                    tv_username.setText(nickname);
                }
            } else {
                if (!TextUtils.isEmpty(remarks)) {
                    //备注
                    tv_username.setText(remarks);
                } else {
                    //昵称
                    if (!TextUtils.isEmpty(nickname)) {
                        tv_username.setText(nickname);
                    }
                }
            }
        }
    }

}
