package com.ospn.osnsdk.data;

public class OsnRequestInfo {
    public String reason;
    public String userID;
    public String friendID;
    public String originalUser;
    public String targetUser;
    public long timeStamp;
    public boolean isGroup;
    public boolean isApply;
    //isApply=false 邀请加入群组: originalUser 邀请 friendID 加入 userID
    //isApply=true  审批邀请入群 originalUser 邀请 targetUser 加入 userID
    //isApply=true originalUser=null  审批加入群组 targetUser 申请加入 userID
}
