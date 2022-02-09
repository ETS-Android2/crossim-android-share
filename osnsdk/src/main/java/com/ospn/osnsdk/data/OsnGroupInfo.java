package com.ospn.osnsdk.data;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OsnGroupInfo {
    public String groupID;
    public String name;
    public String privateKey;
    public String owner;
    public String portrait;
    public int memberCount;
    public int type;
    public int joinType;
    public int passType;
    public int mute;
    public List<OsnMemberInfo> userList;

    public OsnGroupInfo(){
        userList = new ArrayList<>();
    }
    public static OsnGroupInfo toGroupInfo(JSONObject json){
        OsnGroupInfo groupInfo = new OsnGroupInfo();
        groupInfo.groupID = json.getString("groupID");
        groupInfo.name = json.getString("name");
        groupInfo.privateKey = "";
        groupInfo.owner = json.getString("owner");
        groupInfo.type = json.getIntValue("type");
        groupInfo.joinType = json.getIntValue("joinType");
        groupInfo.passType = json.getIntValue("passType");
        groupInfo.mute = json.getIntValue("mute");
        groupInfo.portrait = json.getString("portrait");
        groupInfo.memberCount = json.getIntValue("memberCount");
        JSONArray array = json.getJSONArray("userList");
        if(array != null) {
            for (Object o : array) {
                OsnMemberInfo memberInfo;
                if(o instanceof String){
                    memberInfo = new OsnMemberInfo();
                    memberInfo.osnID = (String)o;
                    memberInfo.groupID = groupInfo.groupID;
                }
                else{
                    JSONObject m = (JSONObject) o;
                    memberInfo = OsnMemberInfo.toMemberInfo(m);
                }
                groupInfo.userList.add(memberInfo);
            }
        }
        return groupInfo;
    }
    public OsnMemberInfo hasMember(String osnID){
        for(OsnMemberInfo m:userList){
            if(m.osnID.equalsIgnoreCase(osnID))
                return m;
        }
        return null;
    }
}
