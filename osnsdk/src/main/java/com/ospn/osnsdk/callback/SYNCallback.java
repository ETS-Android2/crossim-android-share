package com.ospn.osnsdk.callback;

import com.alibaba.fastjson.JSONObject;

public interface SYNCallback {
    void onCallback(String id, JSONObject json);
}
