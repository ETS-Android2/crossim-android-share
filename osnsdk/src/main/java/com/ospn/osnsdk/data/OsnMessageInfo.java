package com.ospn.osnsdk.data;

import com.alibaba.fastjson.JSONObject;

public class OsnMessageInfo {
    public String userID;
    public String target;
    public String content;
    public long timeStamp;
    public boolean isGroup;
    public String originalUser;
    public String hash;

    public static OsnMessageInfo toMessage(JSONObject json, JSONObject data){
        OsnMessageInfo messageInfo = new OsnMessageInfo();
        messageInfo.userID = json.getString("from");
        messageInfo.target = json.getString("to");
        messageInfo.timeStamp = json.getLong("timestamp");
        messageInfo.content = data.getString("content");
        if(messageInfo.userID != null)
            messageInfo.isGroup = messageInfo.userID.startsWith("OSNG");
        else
            messageInfo.isGroup = false;
        messageInfo.originalUser = data.getString("originalUser");
        messageInfo.hash = json.getString("hash");
        return messageInfo;
    }
}
