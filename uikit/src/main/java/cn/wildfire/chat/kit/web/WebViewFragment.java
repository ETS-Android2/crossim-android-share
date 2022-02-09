package cn.wildfire.chat.kit.web;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.constant;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class WebViewFragment extends Fragment {
    private int mIndex;
    private String mUrl;
    private WebCallModelVue mModelVue;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;
    private static final String TAG = "WebViewFragment";

    public WebViewFragment(){}
    public WebViewFragment(int index, String url){
        mIndex = index;
        mUrl = url;
        mModelVue = new WebCallModelVue(getActivity());
    }
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater){
        super.onCreateOptionsMenu(menu, inflater);
        if(mIndex == constant.PAGE_TYPE_ME){
            menu.clear();
            inflater.inflate(R.menu.litapp_me, menu);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.meQrcode) {
            UserInfo userInfo = ChatManager.Instance().getUserInfo(null, false);
            if(userInfo != null){
                String qrCodeValue = WfcScheme.QR_CODE_PREFIX_USER + userInfo.uid;
                startActivity(QRCodeActivity.buildQRCodeIntent(getActivity(), "二维码", userInfo.portrait, qrCodeValue, userInfo.uid));
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.web_fragment, container, false);
        ButterKnife.bind(this, view);
        if(mIndex == constant.PAGE_TYPE_ME)
            setHasOptionsMenu(true);

        WebView mWebView = view.findViewById(R.id.mainWebView);
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
        mWebView.getSettings().setUserAgentString(mWebView.getSettings().getUserAgentString()+" ospn");
        mWebView.addJavascriptInterface(mModelVue, "osnsdk");
        Log.d(TAG,mWebView.getSettings().getUserAgentString());
        mWebView.setWebChromeClient(new WebChromeClient(){
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                Log.d(TAG,"onShowFileChooser: "+ Arrays.toString(fileChooserParams.getAcceptTypes()));
                uploadMessageAboveL = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                startActivityForResult(Intent.createChooser(intent, "Image Browser"), FILE_CHOOSER_RESULT_CODE);
                return true;
            }
        });
        mWebView.setWebViewClient(new WebViewClient(){
            @SuppressLint("CommitPrefEdits")
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG,"url: "+url);
            }
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG,"shouldOverrideUrlLoading url: "+request.getUrl());
                view.loadUrl(request.getUrl().toString());
                return true;
            }
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Log.d(TAG,"shouldInterceptRequest url: "+request.getUrl());
                return super.shouldInterceptRequest(view, request);
            }
        });
        //setCookie(mUrl);
        mWebView.loadUrl(mUrl);
        return view;
    }
//    public String getDomain(String url){
//        url = url.replace("http://", "").replace("https://", "");
//        if (url.contains("/")) {
//            url = url.substring(0, url.indexOf('/'));
//        }
//        return url;
//    }
//    public void setCookie(String url){
//        SharedPreferences sp = getActivity().getSharedPreferences("config", Context.MODE_PRIVATE);
//        String cookie = sp.getString("cookie", null);
//        if(cookie != null && !cookie.isEmpty()){
//            CookieManager cookieManager = CookieManager.getInstance();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                cookieManager.removeSessionCookies(null);
//                cookieManager.flush();
//            } else {
//                cookieManager.removeSessionCookie();
//                CookieSyncManager.getInstance().sync();
//            }
//            String domain = getDomain(url);
//            cookieManager.setAcceptCookie(true);
//            cookieManager.setCookie(domain, cookie);
//            Log.d(TAG, "set domain: "+domain+", cookie: "+cookie);
//        }
//    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessageAboveL)
                return;
            onActivityResultAboveL(requestCode, resultCode, data);
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
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
        Log.d(TAG,"onActivityResultAboveL: "+ Arrays.toString(results));
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }
}
