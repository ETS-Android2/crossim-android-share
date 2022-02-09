package com.ospn.osnsdk.data.serviceInfo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OsnServiceInfo {
    public String type;

    public static OsnServiceInfo toServiceInfo(JSONObject json){
        String type = json.getString("type");
        switch(type){
            case "IMS":
                return OsnIMInfo.toIMInfo(json);
            case "Litapp":
                return OsnLitappInfo.toLitappInfo(json);
            default:
                break;
        }
        return null;
    }
    public static List<OsnServiceInfo> toServiceInfos(JSONObject json){
        List<OsnServiceInfo> infos = new ArrayList<>();
        JSONArray litapps = json.getJSONArray("litapps");
        for(Object o : litapps){
            infos.add(OsnLitappInfo.toLitappInfo((JSONObject)o));
        }
        return infos;
    }
}
