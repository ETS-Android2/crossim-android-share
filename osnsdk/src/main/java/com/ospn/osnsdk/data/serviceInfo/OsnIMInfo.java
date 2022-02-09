package com.ospn.osnsdk.data.serviceInfo;

import com.alibaba.fastjson.JSONObject;

public class OsnIMInfo extends OsnServiceInfo{
    public String urlSpace;

    public static OsnIMInfo toIMInfo(JSONObject json){
        OsnIMInfo imInfo = new OsnIMInfo();
        imInfo.type = json.getString("type");
        imInfo.urlSpace = json.getString("urlSpace");
        return imInfo;
    }
}
