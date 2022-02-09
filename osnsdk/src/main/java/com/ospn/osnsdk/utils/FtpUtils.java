package com.ospn.osnsdk.utils;

import com.alibaba.fastjson.JSONObject;
import com.ospn.osnsdk.callback.OSNTransferCallback;

//import org.apache.commons.net.ftp.FTP;
//import org.apache.commons.net.ftp.FTPClient;
//import org.apache.commons.net.ftp.FTPReply;

import java.io.OutputStream;

public class FtpUtils {
    public static String ftpHost = null;
//
//    public static String uploadData(String fileName, byte[] data, OSNTransferCallback callback){
//        FTPClient ftpClient = null;
//        try{
//            ftpClient = new FTPClient();
//            ftpClient.connect(ftpHost);
//            ftpClient.setControlEncoding("utf-8");
//            if(!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())){
//                if(callback != null)
//                    callback.onFailure("connect failed");
//                return null;
//            }
//            if(!ftpClient.login("test","ims1wax@QSZ")){
//                if(callback != null)
//                    callback.onFailure("login failed");
//                return null;
//            }
//            String name = fileName+ System.currentTimeMillis();
//            String url = ftpClient.printWorkingDirectory()+"/"+Base58.encode(OsnUtils.Sha256(name.getBytes()));
//            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
//            ftpClient.enterLocalPassiveMode();
//            OutputStream outputStream = ftpClient.appendFileStream(url);
//
//            int i;
//            int bLength = 8192;
//            for(i = 0; i < data.length/bLength; ++i){
//                outputStream.write(data,i*bLength,bLength);
//                if(callback!=null)
//                    callback.onProgress(i*bLength,data.length);
//            }
//            if(data.length%bLength != 0){
//                outputStream.write(data,i*bLength,data.length%bLength);
//                if(callback!=null)
//                    callback.onProgress(data.length,data.length);
//            }
//            outputStream.flush();
//            outputStream.close();
//            if(ftpClient.completePendingCommand()) {
//                JSONObject json = new JSONObject();
//                json.put("url", url);
//                if(callback!=null)
//                    callback.onSuccess(json.toString());
//                return json.toString();
//            }
//            if(callback!=null)
//                callback.onFailure("upload break");
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//        finally {
//            if(ftpClient != null && ftpClient.isConnected()) {
//                try{ftpClient.disconnect();}catch (Exception e){e.printStackTrace();}
//            }
//        }
//        return null;
//    }
}
