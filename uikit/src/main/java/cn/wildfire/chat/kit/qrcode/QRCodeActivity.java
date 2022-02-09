/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.qrcode;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.king.zxing.util.CodeUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;

public class QRCodeActivity extends WfcBaseActivity {
    private String title;
    private String logoUrl;
    private String qrCodeValue;
    private String rawData;

    @BindView(R2.id.qrCodeImageView)
    ImageView qrCodeImageView;
    @BindView(R2.id.copyButton)
    Button copyButton;

    public static Intent buildQRCodeIntent(Context context, String title, String logoUrl, String qrCodeValue, String rawData) {
        Intent intent = new Intent(context, QRCodeActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("logoUrl", logoUrl);
        intent.putExtra("qrCodeValue", qrCodeValue);
        intent.putExtra("rawData", rawData);
        return intent;
    }

    @Override
    protected void beforeViews() {
        super.beforeViews();
        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        qrCodeValue = intent.getStringExtra("qrCodeValue");
        logoUrl = intent.getStringExtra("logoUrl");
        rawData = intent.getStringExtra("rawData");
    }

    @Override
    protected int contentLayout() {
        return R.layout.qrcode_activity;
    }

    @Override
    protected void afterViews() {
        setTitle(title);

        genQRCode();
    }

    private void genQRCode() {
        GlideApp.with(this)
                .asBitmap()
                .load(logoUrl)
                .placeholder(R.mipmap.avatar_def)
                .into(new CustomViewTarget<ImageView, Bitmap>(qrCodeImageView) {
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // the errorDrawable will always be bitmapDrawable here
                        if (errorDrawable instanceof BitmapDrawable) {
                            Bitmap bitmap = ((BitmapDrawable) errorDrawable).getBitmap();
                            Bitmap qrBitmap = CodeUtils.createQRCode(qrCodeValue, 400, bitmap);
                            qrCodeImageView.setImageBitmap(qrBitmap);
                        }
                    }

                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition transition) {
                        Bitmap bitmap = CodeUtils.createQRCode(qrCodeValue, 400, resource);
                        qrCodeImageView.setImageBitmap(bitmap);
                    }

                    @Override
                    protected void onResourceCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    @OnClick(R2.id.copyButton)
    void copyQRData(){
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData mClipData = ClipData.newPlainText("二维码", rawData);
        cm.setPrimaryClip(mClipData);

        Toast.makeText(this,"复制成功", Toast.LENGTH_SHORT).show();
    }
}
