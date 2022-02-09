package cn.wildfire.chat.kit.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;

import static android.content.Context.DOWNLOAD_SERVICE;

public class UpdateManager extends BroadcastReceiver {
    private long mDowloadID = 0;
    private static UpdateManager inst = null;
    private static final String TAG = "UpdateManager";

    public static UpdateManager inst(){
        if(inst != null)
            return inst;
        inst = new UpdateManager();
        return inst;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)){
            long downloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
            if(downloadID == mDowloadID){
                DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                File apkFile = null;
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadID);
                query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
                Cursor cur = downloader.query(query);
                if (cur != null) {
                    if (cur.moveToFirst()) {
                        String uriString = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        if (!TextUtils.isEmpty(uriString)) {
                            apkFile = new File(Uri.parse(uriString).getPath());
                        }
                    }
                    cur.close();
                }
                intent = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    String appId = context.getApplicationInfo().packageName;
                    Uri contentUri = FileProvider.getUriForFile(context, appId + ".fileprovider", apkFile);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive");

                }else{
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                }
                context.startActivity(intent);
            }
        }
    }
    public void startUpdate(Context context, String url){
        String appName = url.substring(url.lastIndexOf('/')+1);
        String downloadPath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + appName;
        File apkFile = new File(downloadPath);
        if (apkFile.exists()) {
            if(!apkFile.delete()){
                Log.e(TAG, "delete oldVersion error");
            }
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("正在下载" + appName);
        //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, appName);
        request.setVisibleInDownloadsUi(true);
        request.setMimeType("application/vnd.android.package-archive");

        DownloadManager downloadManager = (DownloadManager)context.getSystemService(DOWNLOAD_SERVICE);
        if(mDowloadID != 0)
            downloadManager.remove(mDowloadID);
        mDowloadID = downloadManager.enqueue(request);
    }
}
