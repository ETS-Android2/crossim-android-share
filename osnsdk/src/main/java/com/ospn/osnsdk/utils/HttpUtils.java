package com.ospn.osnsdk.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ospn.osnsdk.callback.OSNTransferCallback;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static com.ospn.osnsdk.utils.OsnUtils.logInfo;

public class HttpUtils {
    public static String doGet(String sUrl){
        String result = null;
        try{
            URL url = new URL(sUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-Type","application/json");
            httpURLConnection.setInstanceFollowRedirects(true);
            httpURLConnection.connect();

            if(httpURLConnection.getResponseCode() == 200){
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuffer = new StringBuilder();
                String line;
                while((line = bufferedReader.readLine()) != null)
                    stringBuffer.append(line);
                result = stringBuffer.toString();
            }
            else{
                logInfo("response code: "+httpURLConnection.getResponseCode());
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
    public static String doPost(String sUrl, String data){
        String result = null;
        try{
            URL url = new URL(sUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type","application/json");
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.connect();
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(data.getBytes());
            outputStream.flush();

            int read;
            byte[] buffer = new byte[4096];
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            InputStream inputStream = httpURLConnection.getInputStream();
            while((read=inputStream.read(buffer)) > 0)
                byteArrayOutputStream.write(buffer,0,read);
            result = byteArrayOutputStream.toString();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
    public static JSONObject postFormdata(String url, Map<String,String> textMap, Map<String,String> fileMap){
        try{
            URL urls = new URL(url);
            String boundary = "----------"+System.currentTimeMillis();
            HttpURLConnection httpURLConnection = (HttpURLConnection)urls.openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
            httpURLConnection.connect();

            OutputStream outputStream = httpURLConnection.getOutputStream();
            StringBuilder builder = new StringBuilder();
            if(textMap != null){
                for(String key : textMap.keySet()){
                    builder.append("\r\n--").append(boundary).append("\r\n");
                    builder.append("Content-Disposition: form-data;name=\"").append(key).append("\"").append("\"\r\n\r\n");
                    builder.append(textMap.get(key));
                }
                outputStream.write(builder.toString().getBytes());
            }
            if(fileMap != null){
                for(String key : fileMap.keySet()){
                    String fileName = fileMap.get(key);
                    if(fileName == null){
                        continue;
                    }
                    File file = new File(fileName);
                    if(!file.exists()){
                        logInfo("file no exist: "+file.getAbsolutePath());
                    }
                    String contentType;
                    if (fileName.endsWith(".png")) {
                        contentType = "image/png";
                    }else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".jpe")) {
                        contentType = "image/jpeg";
                    }else if (fileName.endsWith(".gif")) {
                        contentType = "image/gif";
                    }else if (fileName.endsWith(".ico")) {
                        contentType = "image/image/x-icon";
                    }else{
                        contentType = "application/octet-stream";
                    }
                    builder.setLength(0);
                    builder.append("\r\n--").append(boundary).append("\r\n");
                    builder.append("Content-Disposition: form-data;name=\"").
                            append(key).append("\"").append("\"\r\n\r\n");
                    builder.append("\r\n--").append(boundary).append("\r\n");
                    builder.append("Content-Disposition: form-data;name=\"").append(key).
                            append("\";filename=\"").append(fileName).append("\"\r\n");
                    builder.append("Content-Type:").append(contentType).append("\r\n\r\n");
                    outputStream.write(builder.toString().getBytes());
                    DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file));
                    int bytes;
                    byte[] bufferOut = new byte[1024];
                    while ((bytes = dataInputStream.read(bufferOut)) != -1) {
                        outputStream.write(bufferOut, 0, bytes);
                    }
                    dataInputStream.close();
                }
            }
            builder.setLength(0);
            builder.append("\r\n--").append(boundary).append("--\r\n");
            outputStream.write(builder.toString().getBytes());
            outputStream.flush();

            StringBuilder stringBuffer = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String line;
            while((line=bufferedReader.readLine()) != null)
                stringBuffer.append(line).append("\r\n");
            bufferedReader.close();
            outputStream.close();
            httpURLConnection.disconnect();
            return JSON.parseObject(stringBuffer.toString());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public static String upload(String sUrl, String type, String fileName, byte[] data, OSNTransferCallback callback){
        try{
            logInfo(sUrl);
            logInfo(fileName);
            URL url = new URL(sUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);

            String boundary = "----------"+System.currentTimeMillis();
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
            httpURLConnection.connect();

            OutputStream outputStream = httpURLConnection.getOutputStream();
            StringBuilder builder = new StringBuilder();
            builder.append("--");
            builder.append(boundary);
            builder.append("\r\n");
            builder.append("Content-Disposition: form-data;name=\"");
            String prefix = type.equalsIgnoreCase("portrait") ? "P":"C";
            builder.append(prefix);
            builder.append(fileName);
            builder.append("\"");
            builder.append("\r\n\r\n\r\n");
            outputStream.write(builder.toString().getBytes());

            builder.delete(0,builder.length());
            builder.append("--");
            builder.append(boundary);
            builder.append("\r\n");
            builder.append("Content-Disposition: form-data;name=\"file\";filename=\"");
            builder.append(prefix);
            builder.append(fileName);
            builder.append("\"");
            builder.append("\r\n");
            builder.append("Content-Type:application/octet-stream");
            builder.append("\r\n\r\n");
            outputStream.write(builder.toString().getBytes());

            int i;
            int bLength = 8192;
            for(i = 0; i < data.length/bLength; ++i){
                outputStream.write(data,i*bLength,bLength);
                if(callback != null)
                    callback.onProgress(i*bLength,data.length);
            }
            if(data.length%bLength != 0){
                outputStream.write(data,i*bLength,data.length%bLength);
                if(callback!=null)
                    callback.onProgress(data.length,data.length);
            }

            outputStream.write("\r\n".getBytes());
            outputStream.write(("\r\n--"+boundary+"--\r\n").getBytes());
            outputStream.flush();

            StringBuilder stringBuffer = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String line;
            while((line=bufferedReader.readLine()) != null)
                stringBuffer.append(line).append("\r\n");
            bufferedReader.close();
            outputStream.close();
            httpURLConnection.disconnect();

            JSONObject json = JSON.parseObject(stringBuffer.toString());
            if(callback!=null)
                callback.onSuccess(json.toString());
            return json.getString("url");
        }
        catch (Exception e){
            e.printStackTrace();
            if(callback!=null)
                callback.onFailure(e.toString());
        }
        return null;
    }
    public static void download(String sUrl, String path, OSNTransferCallback callback){
        try{
            logInfo(sUrl);
            logInfo(path);

            URL url = new URL(sUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            InputStream inputStream = httpURLConnection.getInputStream();
            File tmp = new File(path+".tmp");
            OutputStream outputStream = new FileOutputStream(tmp);
            int len;
            byte[] data = new byte[8192];
            while((len=inputStream.read(data))!=-1)
                outputStream.write(data,0,len);
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            httpURLConnection.disconnect();
            if(!tmp.renameTo(new File(path)))
                logInfo("rename failed: "+tmp.getAbsolutePath());
            if(callback!=null)
                callback.onSuccess(null);
        }
        catch (Exception e){
            e.printStackTrace();
            if(callback!=null)
                callback.onFailure(e.toString());
        }
    }
}
