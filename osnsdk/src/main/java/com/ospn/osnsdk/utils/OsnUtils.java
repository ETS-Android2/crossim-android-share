package com.ospn.osnsdk.utils;

import android.annotation.SuppressLint;
import android.util.Base64;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ospn.osnsdk.OSNManager;
import com.ospn.osnsdk.utils.ECUtils;

import java.io.BufferedOutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static android.util.Base64.NO_WRAP;

public class OsnUtils {
    public static String mLogName = "OsnSDK.log";
    public static BufferedOutputStream mLogger = null;
    @SuppressLint("SimpleDateFormat")
    private static SimpleDateFormat mFormater= new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");

    public static byte[] sha256(byte[] data){
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(data);
            data = messageDigest.digest();
        } catch (Exception e){
            e.printStackTrace();
        }
        return data;
    }
    public static String aesEncrypt(byte[] data, byte[] key){
        try {
            byte[] iv = new byte[16];
            Arrays.fill(iv, (byte) 0);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] encData = cipher.doFinal(data);
            return Base64.encodeToString(encData,NO_WRAP);
        }
        catch (Exception e){
            e.printStackTrace();
            logInfo(e.toString());
        }
        return null;
    }
    public static String aesEncrypt(String data, String key){
        byte[] pwdHash = sha256(key.getBytes());
        return aesEncrypt(data.getBytes(), pwdHash);
    }
    public static byte[] aesDecrypt(byte[] data, byte[] key){
        try {
            byte[] iv = new byte[16];
            Arrays.fill(iv,(byte)0);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return cipher.doFinal(data);
        }
        catch (Exception e){
            e.printStackTrace();
            logInfo(e.toString());
        }
        return null;
    }
    public static String aesDecrypt(String data, String key){
        byte[] pwdHash = sha256(key.getBytes());
        byte[] decData = Base64.decode(data,0);
        if(decData == null)
            return null;
        decData = aesDecrypt(decData,pwdHash);
        return new String(decData);
    }
    public static byte[] getAesKey(){
        byte[] key = new byte[32];
        Random random = new Random();
        for(int i = 0; i < 32; ++i)
            key[i] = (byte)random.nextInt(256);
        return key;
    }

    public static JSONObject wrapMessage(String command, String from, String to, JSONObject data, String key){
        try {
            JSONObject json = new JSONObject();
            json.put("command", command);
            json.put("ver", "1");
            json.put("from", from);
            json.put("to", to);

            if(data == null)
                data = new JSONObject();
            json.put("content", data.toString());

            long timestamp = System.currentTimeMillis();
            String calc = from + to + timestamp + data.toString();
            String hash = ECUtils.osnHash(calc.getBytes());
            json.put("hash", hash);
            json.put("timestamp", timestamp);

            String sign = ECUtils.osnSign(key, hash.getBytes());
            json.put("sign", sign);
            json.put("crypto", "none");

            return json;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public static JSONObject makeMessage(String command, String from, String to, JSONObject data, String key){
        try {
            JSONObject json = new JSONObject();
            json.put("command", command);
            json.put("from", from);
            json.put("to", to);

            if(data == null) {
                json.put("content", "{}");
                json.put("crypto", "none");
            } else if(to == null){
                json.put("content", data.toString());
                json.put("crypto", "none");
            } else {
                byte[] aesKey = getAesKey();
                String encData = aesEncrypt(data.toString().getBytes(), aesKey);
                json.put("content", encData);
                json.put("crypto", "ecc-aes");

                String encKey = ECUtils.ecEncrypt2(to, aesKey);
                json.put("ecckey", encKey);

                if (key != null) {
                    byte[] msgKey = sha256(key.getBytes());
                    json.put("aeskey", aesEncrypt(aesKey, msgKey));
                }
            }

            long timestamp = System.currentTimeMillis();
            String calc = from + to + timestamp + json.getString("content");
            String hash = ECUtils.osnHash(calc.getBytes());

            json.put("hash", hash);
            json.put("timestamp", timestamp);

            if(key != null) {
                String sign = ECUtils.osnSign(key, hash.getBytes());
                json.put("sign", sign);
            }

            return json;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public static JSONObject takeMessage(JSONObject json, String key){
        try {
            byte[] data;
            byte[] aesKey;
            String crypto = json.getString("crypto");
            if(crypto.equalsIgnoreCase("none"))
                return JSON.parseObject(json.getString("content"));
            if(crypto.equalsIgnoreCase("ecc-aes")){
                if(json.getString("to").equalsIgnoreCase(OSNManager.Instance().getUserID()))
                    aesKey = ECUtils.ecDecrypt2(key, json.getString("ecckey"));
                else if(json.containsKey("aeskey")){
                    data = Base64.decode(json.getString("aeskey"),0);
                    aesKey = aesDecrypt(data, sha256(key.getBytes()));
                }
                else {
                    OsnUtils.logInfo("unknown key mode");
                    return null;
                }
            }
            else if(crypto.equalsIgnoreCase("aes")){
                data = Base64.decode(json.getString("aeskey"),0);
                aesKey = aesDecrypt(data, sha256(key.getBytes()));
            }
            else{
                OsnUtils.logInfo("unsupport crypto");
                return null;
            }
            if(aesKey == null || !json.containsKey("content"))
                return null;
            data = Base64.decode(json.getString("content"),0);
            data = aesDecrypt(data, aesKey);
            return JSON.parseObject(new String(data, Charset.forName("utf-8")));
        }
        catch (Exception e){
            logInfo(e.toString());
        }
        return null;
    }
    static public void logInfo(String info){
        try{
//            if(mLogger == null)
//                mLogger = new BufferedOutputStream(new FileOutputStream(mLogName));
//            Date date = new Date(System.currentTimeMillis());
//            String time = mFormater.format(date);
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String traceInfo = "["+Thread.currentThread().getId()+" " + stackTrace[3].getClassName() + "." + stackTrace[3].getMethodName() + "] ";
//            mLogger.write(time.getBytes());
//            mLogger.write(traceInfo.getBytes());
//            if(info != null)
//                mLogger.write(info.getBytes());
//            mLogger.write("\r\n".getBytes());

//            System.out.print(time);
            System.out.print(traceInfo);
            System.out.println(info);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
