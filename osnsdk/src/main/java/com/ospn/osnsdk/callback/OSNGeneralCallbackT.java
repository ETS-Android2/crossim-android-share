package com.ospn.osnsdk.callback;

public interface OSNGeneralCallbackT<T> {
    void onFailure(String error);
    void onSuccess(T t);
}
