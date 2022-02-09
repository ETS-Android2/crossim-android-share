package cn.wildfire.chat.kit.web;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.net.URLDecoder;

import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class WebCallModel {
    static String eMissData = "-1:miss data";
    static String eLoginFail = "-2:login failed";
    static String eUnknownCmd = "-100:unknown cmd";
    static String TAG = "WebCallModel";

    static JSONObject setResult(String error, JSONObject json){
        if(json == null)
            json = new JSONObject();
        json.put("errCode", error==null?"0:success":error);
        return json;
    }
    static JSONObject doLogin(JSONObject json){
        String username = json.getString("username");
        String password = json.getString("password");
        if(username == null || password == null)
            return setResult(eMissData, null);
        boolean result = ChatManager.Instance().connect(username,password);
        if(result)
            return setResult(null, null);
        return setResult(eLoginFail, null);
    }
    static JSONObject doLogout(JSONObject json){
        ChatManager.Instance().disconnect(true,true);
        return setResult(null, null);
    }
    static JSONObject getUserInfo(JSONObject json){
        String userID = json.getString("userID");
        if(userID == null)
            return setResult(eMissData, null);
        UserInfo userInfo = ChatManager.Instance().getUserInfo(userID, false);
        json.clear();
        json.put("userInfo", userInfo);
        return setResult(null, json);
    }
    public static boolean WebCallJava(Activity activity, WebView view, String url){
        try{
            Log.d(TAG,"url: "+url);
            url = URLDecoder.decode(url, "utf-8");
            if(url.startsWith("app://")){
                String finalUrl = url;
                new Thread(()->{
                    JSONObject result;
                    JSONObject json = JSON.parseObject(finalUrl.substring(6));
                    String callback = json.getString("callback");
                    switch(json.getString("command")){
                        case "Login":
                            result = doLogin(json);
                            break;
                        case "Logout":
                            result = doLogout(json);
                            break;
                        case "GetUserInfo":
                            result = getUserInfo(json);
                            break;
                        default:
                            result = setResult(eUnknownCmd,null);
                    }
                    if(callback != null){
                        activity.runOnUiThread(()->{
                            view.loadUrl("javascript:"+callback+"("+result.toString()+")");
                        });
                    }
                }).start();
                return true;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        view.loadUrl(url);
        return false;
    }
}
