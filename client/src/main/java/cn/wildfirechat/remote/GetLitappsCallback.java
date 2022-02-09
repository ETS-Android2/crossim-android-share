package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.LitappInfo;

public interface GetLitappsCallback {
    void onSuccess(List<LitappInfo> litappInfos);
    void onFail(int errorCode);
}
