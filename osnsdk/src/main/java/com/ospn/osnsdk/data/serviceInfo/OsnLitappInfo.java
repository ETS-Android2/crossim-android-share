package com.ospn.osnsdk.data.serviceInfo;

import com.alibaba.fastjson.JSONObject;

public class OsnLitappInfo extends OsnServiceInfo{
    public String target;
    public String name;
    public String displayName;
    public String portrait;
    public String theme;
    public String url;

    public static OsnLitappInfo toLitappInfo(JSONObject json){
        OsnLitappInfo litappInfo = new OsnLitappInfo();
        litappInfo.type = json.getString("type");
        litappInfo.target = json.getString("target");
        litappInfo.name = json.getString("name");
        litappInfo.displayName = json.getString("displayName");
        litappInfo.portrait = json.getString("portrait");
        litappInfo.theme = json.getString("theme");
        litappInfo.url = json.getString("url");
        return litappInfo;
    }
}
