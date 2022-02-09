package cn.wildfire.chat.kit.common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class ComplaintActivity extends WfcBaseActivity {
    private final String TAG = this.getClass().getSimpleName();

//    @BindView(R.id.ib_titleBar_back)
//    ImageButton ib_titleBar_back;
//
//    @BindView(R.id.tv_titleBar_title)
//    TextView tv_titleBar_title;

    @BindView(R2.id.et_content)
    EditText et_content;
//
//    @BindView(R.id.rv_picturesContent)
//    RecyclerView rv_picturesContent;

//    private CustomConfirmDialog submitDialog;
//
//    private MediaDataSource mediaDataSource;
//    private CommonDataSource textDataSource;
//    private ComplaintBody textDataBody;
//
//    // 图片多选适配器
//    private CommonMultipleImageAdapter gridImageAdapter;
//    private List<LocalMedia> selectedImagesDatas = new ArrayList<>();
    private String complaintContent;

    @Override
    protected void afterViews() {
        initViews();
        initDatas();
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_complaint;
    }

    /**
     * 设置列表及适配器
     */
    private void initViews() {
//        tv_titleBar_title.setText(R.string.string_complaint_or_feedback);
//        gridImageAdapter = new CommonMultipleImageAdapter(this, () -> {
//            // 点击添加图片
//            selectPicture(PictureMimeType.ofImage(), 9, PictureConfig.MULTIPLE
//                    , BaseConstant.REQUEST.REQUEST_CODE_IMAGE, selectedImagesDatas);
//        }, R.layout.item_common_multiple_image);
//        gridImageAdapter.setOnItemClickListener((position, v) -> {
//            // 图片点击预览
//            if (selectedImagesDatas.size() > 0) {
//                PictureSelector.create(ComplaintActivity.this)
//                        .openGallery(PictureMimeType.ofAll())
//                        .loadImageEngine(MyGlideEngine.createGlideEngine())
//                        .openExternalPreview(position, selectedImagesDatas);
//            }
//        });
//        gridImageAdapter.setDatas(selectedImagesDatas);
//        gridImageAdapter.setSelectMaxCount(9);
//        rv_picturesContent.setLayoutManager(new GridLayoutManager(this, 4, GridLayoutManager.VERTICAL, false));
//        rv_picturesContent.addItemDecoration(new CommonItemDecoration3(SizeUtils.dp2px(6), SizeUtils.dp2px(12)));
//        rv_picturesContent.setAdapter(gridImageAdapter);
    }

    private void initDatas() {
        // 初始化DataSource
//        textDataSource = new CommonDataSource(this);
//        mediaDataSource = new MediaDataSource(this);
//
//        // 初始化提交Body
//        textDataBody = new ComplaintBody();
//        textDataBody.setSourceId(sourceId);
//        textDataBody.setType(type);
    }

    /**
     * 从相册选取图片
     */
    @SuppressLint("CheckResult")
    private void selectPicture(int mimeType, int maxSelectNum, int selectionMode, int requsetCode, List<String> selectedImages) {
//        RxPermissions rxPermissions = new RxPermissions(ComplaintActivity.this);
//        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
//                , Manifest.permission.CAMERA).subscribe(granted -> {
//                    if (granted) {
//                        PictureSelector pictureSelector = PictureSelector.create(ComplaintActivity.this);
//                        PictureConfigUtil.openGallery(pictureSelector, mimeType, maxSelectNum
//                                , selectionMode, false, requsetCode, selectedImages);
//                    }
//                }
//        );
    }

    @OnClick(R2.id.btn_submit)
    protected void onClick(View view) {
        complaintContent = et_content.getText().toString();
        if (TextUtils.isEmpty(complaintContent)) {
            Toast.makeText(this, "输入内容不能为空", Toast.LENGTH_SHORT);
            return;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("正在提交")//标题
                .setMessage("正在提交，轻稍候。。。")//内容
                .setIcon(R.mipmap.ic_channel)//图标
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(ComplaintActivity.this, "这是取消按钮", Toast.LENGTH_SHORT).show();
                    }})
                .create();
        alertDialog.show();

        new Handler(getMainLooper()).postDelayed(() -> {
            alertDialog.dismiss();
            Toast.makeText(ComplaintActivity.this, "提交完成", Toast.LENGTH_SHORT).show();
            finish();
        }, 1000);
    }

    /**
     * 提交媒体图片
     */
    private void submitMediaData() {
        // 提交图片
//        mediaDataSource.uploadComplaintMediaMulti(selectedImagesDatas, new DataCallback<List<CommonMediaModel>>() {
//            @Override
//            public void onSuccess(List<CommonMediaModel> data) {
//                textDataBody.setMedias(data);
//                submitTextData();
//            }
//
//            @Override
//            public void onFail(String msg) {
//                submitDialog.dismiss();
//                showCustomMessage(R.mipmap.toast_operation_fail, msg);
//            }
//        });
    }

    /**
     * 提交投诉数据
     */
    private void submitTextData() {
//        textDataSource.submitComplaintData(textDataBody, new DataCallback() {
//            @Override
//            public void onSuccess(Object data) {
//                submitDialog.dismiss();
//                showCustomMessage(R.mipmap.toast_submit_success, "提交成功");
//                PictureFileUtils.deleteAllCacheDirFile(ComplaintActivity.this);
//                finish();
//            }
//
//            @Override
//            public void onFail(String msg) {
//                submitDialog.dismiss();
//                showCustomMessage(R.mipmap.toast_operation_fail, msg);
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
//                case BaseConstant.REQUEST.REQUEST_CODE_IMAGE:
//                    // 商品图片选择回调
//                    List<LocalMedia> selectedImages = PictureSelector.obtainMultipleResult(data);
//                    if (!AppUtil.isListEmpty(selectedImages)) {
//                        selectedImagesDatas = selectedImages;
//                        if (gridImageAdapter != null) {
//                            gridImageAdapter.setDatas(selectedImagesDatas);
//                            gridImageAdapter.notifyDataSetChanged();
//                        }
//                    } else {
//                        LogUtils.e(TAG, "selectedImagesDatas：null");
//                    }
//                    break;

                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (mediaDataSource != null) {
//            mediaDataSource.destroy();
//        }
//        if (textDataSource != null) {
//            textDataSource.destroy();
//        }
        // 包括裁剪和压缩后的缓存，要在上传成功后调用，注意：需要系统sd卡权限
        //PictureFileUtils.deleteAllCacheDirFile(this);
    }

    public static void startActivity(Activity mContext, Long sourceId, int type) {
        Intent intent = new Intent(mContext, ComplaintActivity.class);
        intent.putExtra("sourceId", sourceId);
        intent.putExtra("type", type);
        mContext.startActivity(intent);
    }

}
