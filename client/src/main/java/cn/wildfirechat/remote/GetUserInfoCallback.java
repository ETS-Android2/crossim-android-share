/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import cn.wildfirechat.model.UserInfo;

public interface GetUserInfoCallback {

    void onSuccess(UserInfo userInfo);

    void onFail(int errorCode);
}
