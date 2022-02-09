package com.ospn.osnsdk.callback;

import com.ospn.osnsdk.data.OsnFriendInfo;
import com.ospn.osnsdk.data.OsnGroupInfo;
import com.ospn.osnsdk.data.OsnMessageInfo;
import com.ospn.osnsdk.data.OsnRequestInfo;
import com.ospn.osnsdk.data.OsnUserInfo;
import com.ospn.osnsdk.data.serviceInfo.OsnServiceInfo;

import java.util.List;

public interface OSNListener {
    void onConnectSuccess (String state);
    void onConnectFailed (String error);
    void onSetMessage(OsnMessageInfo msgInfo);
    void onRecvMessage (List<OsnMessageInfo> msgList);
    void onRecvRequest(OsnRequestInfo request);
    void onFriendUpdate(List<OsnFriendInfo> friendList); //friend为本地数据，使用list
    void onUserUpdate(OsnUserInfo userInfo, List<String> keys);
    void onGroupUpdate(String state, OsnGroupInfo groupInfo, List<String> keys);
    void onServiceInfo(List<OsnServiceInfo> infos);

    String getConfig(String key);
    void setConfig(String key, String value);
}
