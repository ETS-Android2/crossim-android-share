package com.ospn.osnsdk.data;

import com.alibaba.fastjson.JSONObject;

public class OsnFriendInfo {
    public String userID;
    public String friendID;
    public String remarks;
    public int state;

    public static final int Wait = 0;
    public static final int Normal = 1;
    public static final int Deleted = 2;
    public static final int Blacked = 3;
    public static final int Syncst = 4;

    public OsnFriendInfo(){}
    public static OsnFriendInfo toFriendInfo(JSONObject json){
        OsnFriendInfo friendInfo = new OsnFriendInfo();
        friendInfo.userID = json.getString("userID");
        friendInfo.friendID = json.getString("friendID");
        friendInfo.remarks = json.getString("remarks");
        friendInfo.state = json.getIntValue("state");
        return friendInfo;
    }
}
