package com.ospn.osnsdk.callback;

public interface OSNGeneralCallback {
    void onSuccess(String json);
    void onFailure(String error);
}
