package com.ospn.osnsdk.data;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OsnMemberInfo {
    public String osnID;
    public String groupID;
    public String remarks;
    public String nickName;
    public int type;
    public int mute;

    public static int MemberType_Wait = 0;
    public static int MemberType_Normal = 1;
    public static int MemberType_Owner = 2;
    public static int MemberType_Admin = 3;

    public static OsnMemberInfo toMemberInfo(JSONObject json){
        OsnMemberInfo memberInfo = new OsnMemberInfo();
        memberInfo.osnID = json.getString("osnID");
        memberInfo.groupID = json.getString("groupID");
        memberInfo.nickName = json.getString("nickName");
        memberInfo.remarks = json.getString("remarks");
        memberInfo.type = json.getIntValue("type");
        memberInfo.mute = json.getIntValue("mute");
        return memberInfo;
    }
    public static List<OsnMemberInfo> toMemberInfos(JSONObject json){
        JSONArray array = json.getJSONArray("userList");
        List<OsnMemberInfo> members = new ArrayList<>();
        if(array != null) {
            for (Object o : array)
                members.add(toMemberInfo((JSONObject) o));
        }
        return members;
    }
}
