package cn.wildfirechat.remote;

import cn.wildfirechat.model.LitappInfo;

public interface GetLitappInfoCallback {
    void onSuccess(LitappInfo litappInfo);
    void onFail(int errorCode);
}
