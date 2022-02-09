package cn.wildfire.chat.kit.litapp;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.jetbrains.annotations.NotNull;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

public class LitappActivity extends WfcBaseActivity {
    WebView mWebView;
    boolean isLogin = false;
    LitappInfo litappInfo;
    private ValueCallback<Uri[]> uploadMessageAboveL;

    /** 视频全屏参数 */
    protected static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    private View customView;
    private FrameLayout fullscreenContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;

    boolean isSysExit = false;
    boolean isJumpOut = false;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;

    @Override
    protected int contentLayout() {
        return R.layout.activity_litapp;
    }
    protected int menu() {
        return R.menu.litapp;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void afterViews() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        String hostip = sp.getString("hostip", null);

        ChatManager.init(getApplication(),hostip);
        litappInfo = getIntent().getParcelableExtra("litappInfo");
        if(litappInfo == null)
            return;
        setTitle(litappInfo.name);

        JSONObject json = new JSONObject();
        json.put("Litapp",litappInfo);
        Log.d("Litapp",json.toString());

        mWebView = findViewById(R.id.litappWebview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.getSettings().setGeolocationEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
        mWebView.setWebChromeClient(new WebChromeClient(){
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                Log.d("Litapp", "onShowFileChooser: "+fileChooserParams);
                uploadMessageAboveL = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                startActivityForResult(Intent.createChooser(intent, "Image Browser"), FILE_CHOOSER_RESULT_CODE);
                return true;
            }
            @Override
            public View getVideoLoadingProgressView() {
                FrameLayout frameLayout = new FrameLayout(LitappActivity.this);
                frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return frameLayout;
            }
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                showCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder b = new AlertDialog.Builder(LitappActivity.this);
                b.setTitle("");
                b.setMessage(message);
                b.setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm());
                b.setCancelable(false);
                b.create().show();
                return true;
            }
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder b = new AlertDialog.Builder(LitappActivity.this);
                b.setTitle("");
                b.setMessage(message);
                b.setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm());
                b.setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel());
                b.create().show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
                result.confirm();
                return super.onJsPrompt(view, url, message, message, result);
            }
        });
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("Litapp", "onPageFinished: "+url);
                if(isLogin && !isJumpOut){
                    isJumpOut = true;
                    mWebView.clearHistory();
                }
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                try{
                    Log.d("Litapp", "shouldOverrideUrlLoading: "+url);
                    url = URLDecoder.decode(url, "utf-8");
                    if(url.startsWith("app://")){
                        JSONObject data = new JSONObject();
                        JSONObject json = JSON.parseObject(url.substring(6));
                        String callback = json.getString("callback");
                        switch(json.getString("command")){
                            case "Login":
                                Log.d("Litapp","Login to url: "+json.getString("url"));
                                url = json.getString("url");
                                if(url == null)
                                    url = litappInfo.url;
                                ChatManager.Instance().ltbLogin(litappInfo, url, new GeneralCallback() {
                                    @Override
                                    public void onSuccess() {
                                        runOnUiThread(()->{
                                            Log.d("Litapp","ltbLogin success");
                                            isLogin = true;
                                            data.put("errCode","0:success");
                                            mWebView.loadUrl("javascript:"+callback+"("+data.toString()+")");
                                        });
                                    }

                                    @Override
                                    public void onFail(int errorCode) {
                                        runOnUiThread(()->{
                                            Log.d("Litapp","error: "+errorCode);
                                            isLogin = false;
                                            data.put("errCode","1:failure");
                                            mWebView.loadUrl("javascript:"+callback+"("+data.toString()+")");
                                        });
                                    }
                                });
                                break;
                            case "GetUserInfo":
                                if(!isLogin){
                                    data.put("errCode","1:need login");
                                    mWebView.loadUrl("javascript:"+callback+"("+data.toString()+")");
                                    Log.d("Litapp",data.toString());
                                    break;
                                }
                                UserInfo userInfo = ChatManager.Instance().getUserInfo(null,false);
                                data.put("errCode","0:success");
                                data.put("userID",userInfo.uid);
                                data.put("userName",userInfo.name);
                                data.put("nickName",userInfo.displayName);
                                data.put("portrait",userInfo.portrait);
                                mWebView.loadUrl("javascript:"+callback+"("+data.toString()+")");
                                Log.d("Litapp",data.toString());
                                break;
                            case "SignData":
                                if(!isLogin){
                                    data.put("errCode","1:need login");
                                    mWebView.loadUrl("javascript:"+callback+"("+data.toString()+")");
                                    Log.d("Litapp",data.toString());
                                    break;
                                }
                                String sign = ChatManager.Instance().signData(json.getString("data"));
                                data.put("sign",sign);
                                data.put("data",json.getString("data"));
                                mWebView.loadUrl("javascript:"+callback+"("+data.toString()+")");
                                break;
                            case "AddFriend":
                                userInfo = ChatManager.Instance().getUserInfo(json.getString("userID"),false);
                                Intent intent = new Intent(mWebView.getContext(), UserInfoActivity.class);
                                intent.putExtra("userInfo", userInfo);
                                startActivity(intent);
                                break;
                        }
                        return true;
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                return false;
            }
        });

        if(!CheckRight(litappInfo.target)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("授权");
            builder.setMessage(litappInfo.displayName + " 可能会访问您的头像等公开信息，是否授权访问");
            builder.setPositiveButton("确定", (dialog, which) -> {
                AddRight(litappInfo.target);
                mWebView.loadUrl(litappInfo.url);
            });
            builder.setNegativeButton("取消", (dialog, which) -> finish());
            builder.show();
        }
        else{
            mWebView.loadUrl(litappInfo.url);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessageAboveL)
                return;
            onActivityResultAboveL(requestCode, resultCode, data);
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        Log.d("Litapp", "onActivityResultAboveL: "+ Arrays.toString(results));
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }
    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()){
            mWebView.goBack();
            return;
        }
        if(!isSysExit){
            isSysExit = true;

            Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isSysExit =false;
                }
            }, 2000);
        }else {
            finish();
        }
    }
    private void AddRight(String osnID){
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        String cache = sp.getString("litappAccess", null);
        JSONArray json = null;
        if(cache != null && !cache.isEmpty()) {
            try{
                json = JSON.parseArray(cache);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        if(json == null)
            json = new JSONArray();
        json.add(osnID);
        sp.edit().putString("litappAccess", json.toString()).apply();
    }
    private boolean CheckRight(String osnID){
        boolean hasRight = false;
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        String cache = sp.getString("litappAccess", null);
        if(cache != null && !cache.isEmpty()){
            try{
                JSONArray json = JSON.parseArray(cache);
                for(Object o:json){
                    if(osnID.equalsIgnoreCase((String)o)){
                        hasRight = true;
                        break;
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return hasRight;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.clear_cache) {
            mWebView.clearCache(true);
        } else if(item.getItemId() == R.id.litapp_qrcode){
            if(litappInfo != null){
                String qrCodeValue = WfcScheme.QR_CODE_PREFIX_LITAPP + litappInfo.target;
                startActivity(QRCodeActivity.buildQRCodeIntent(this, "二维码", litappInfo.portrait, qrCodeValue, litappInfo.target));
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /** 全屏容器界面 */
    static class FullscreenHolder extends FrameLayout {

        public FullscreenHolder(Context ctx) {
            super(ctx);
            setBackgroundColor(ctx.getResources().getColor(android.R.color.black));
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent evt) {
            return true;
        }
    }
    /** 视频播放全屏 **/
    private void showCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        // if a view already exists then immediately terminate the new one
        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }

        LitappActivity.this.getWindow().getDecorView();

        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        fullscreenContainer = new FullscreenHolder(LitappActivity.this);
        fullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
        decor.addView(fullscreenContainer, COVER_SCREEN_PARAMS);
        customView = view;
        setStatusBarVisibility(false);
        customViewCallback = callback;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    /** 隐藏视频全屏 */
    private void hideCustomView() {
        if (customView == null) {
            return;
        }

        setStatusBarVisibility(true);
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        decor.removeView(fullscreenContainer);
        fullscreenContainer = null;
        customView = null;
        customViewCallback.onCustomViewHidden();
        mWebView.setVisibility(View.VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    private void setStatusBarVisibility(boolean visible) {
        int flag = visible ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setFlags(flag, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (customView != null) {
                hideCustomView();
            } else if (mWebView.canGoBack()) {
                mWebView.goBack();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    @Override
    public void onConfigurationChanged(@NotNull Configuration config) {
        super.onConfigurationChanged(config);
    }
}
