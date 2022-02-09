package cn.wildfire.chat.kit.litapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.lifecycle.ViewModelStore;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import okhttp3.OkHttpClient;
import q.rorbin.badgeview.DisplayUtil;

import static android.content.ContentValues.TAG;

public class LitappGridFragment extends ProgressFragment {
    private RecyclerView recyclerView;
    private LitappGridAdapter adapter;

    public static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;
        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.bottom = space;
            outRect.left = space;
            outRect.right = space;
            outRect.top = space;
        }
    }
    public static class LitappGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static Fragment fragment;
        private List<LitappInfo> litappInfoList = new ArrayList<>();
        private int itemWidth;

        public LitappGridAdapter(LitappGridFragment litappGridFragment) {
            super();
            fragment = litappGridFragment;
            itemWidth = (fragment.getResources().getDisplayMetrics().widthPixels - DisplayUtil.dp2px(fragment.getContext(),24)) / 2;
        }
        public void setLitappInfoList(List<LitappInfo> litappInfoList){
            this.litappInfoList = litappInfoList;
            notifyDataSetChanged();
        }
        public void setLitappInfo(LitappInfo litappInfo){
            boolean finded = false;
            for(int i = 0; i < litappInfoList.size(); ++i){
                LitappInfo info = litappInfoList.get(i);
                if(litappInfo.target.equalsIgnoreCase(info.target)){
                    info.target = litappInfo.target;
                    info.name = litappInfo.name;
                    info.displayName = litappInfo.displayName;
                    info.portrait = litappInfo.portrait;
                    info.theme = litappInfo.theme;
                    info.url = litappInfo.url;
                    info.info = litappInfo.info;
                    finded = true;
                }
            }
            if(finded)
                notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(fragment.getContext()).inflate(R.layout.litapp_grid_item, parent, false);
            view.setOnClickListener(LitappGridAdapter::onClick);
            return new Holder(fragment,view,itemWidth);
        }

        private static void onClick(View view) {
            Holder holder = (Holder)view.getTag();
            Intent intent = new Intent(fragment.getContext(), LitappActivity.class);
            intent.putExtra("litappInfo", holder.litappInfo);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            fragment.startActivity(intent);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
            Holder holder = (Holder)viewHolder;
            if(position < litappInfoList.size()){
                holder.onBind(litappInfoList.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return litappInfoList.size();
        }


        static class Holder extends RecyclerView.ViewHolder {
            private Fragment fragment;
            private LitappInfo litappInfo;

            @BindView(R2.id.contentLayout)
            RelativeLayout contentLayout;
            @BindView(R2.id.userCardPortraitImageView)
            ImageView portraitImageView;
            @BindView(R2.id.theme)
            ImageView theme;
            @BindView(R2.id.userCardNameTextView)
            TextView nameTextView;
            @BindView(R2.id.content)
            TextView content;
            @BindView(R2.id.cardType)
            TextView cardType;
            public Holder(Fragment fragment, View itemView, int itemWidth) {
                super(itemView);
                this.fragment = fragment;
                itemView.setTag(this);
                ButterKnife.bind(this, itemView);

                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                layoutParams.height = itemWidth*16/9;
                layoutParams.width = itemWidth;
                itemView.setLayoutParams(layoutParams);
            }
            public void onBind(LitappInfo litappInfo){
                this.litappInfo = litappInfo;
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                nameTextView.setText(litappInfo.name);
                content.setText(litappInfo.displayName);
                cardType.setText("小程序名片");
                GlideApp
                        .with(fragment)
                        .load(litappInfo.portrait)
                        .transforms(new CenterCrop(), new RoundedCorners(10))
                        .placeholder(R.mipmap.avatar_def)
                        .into(portraitImageView);
                GlideApp
                        .with(fragment)
                        .load(litappInfo.theme)
                        .transforms(new CenterCrop(), new RoundedCorners(10))
                        .placeholder(R.mipmap.avatar_def)
                        .into(theme);
            }
        }
    }

    @Override
    protected int contentLayout() {
        return R.layout.litapp_grid_fragment;
    }
    @Override
    protected void afterViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new SpacesItemDecoration(DisplayUtil.dp2px(this.getContext(), 4)));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //防止第一行到顶部有空白区域
                layoutManager.invalidateSpanAssignments();
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        adapter = new LitappGridAdapter(this);
        recyclerView.setAdapter(adapter);

        LitappViewModel litappViewModel = ViewModelProviders.of(this).get(LitappViewModel.class);
        litappViewModel.litappInfoLiveData().observe(this, infos -> {
            for (LitappInfo info : infos) {
                Log.d("LitappGridFragment", info.target);
                adapter.setLitappInfo(info);
            }
        });

        Fragment fragment = this;
        new Thread(()-> {
            OKHttpHelper.get("http://" + ChatManager.Instance().getHost() + ":8300/mainlitapps", null, new SimpleCallback<String>() {
                @Override
                public void onUiSuccess(String s) {
                    JSONObject json = JSON.parseObject(s);
                    JSONArray array = json.getJSONArray("litapps");
                    if(array == null)
                        array = new JSONArray();
                    List<LitappInfo> litappInfoList = litappViewModel.getLitappList();
                    List<LitappInfo> mainLitapps = new ArrayList<>();
                    for(Object o : array){
                        String serviceID = (String)o;
                        boolean finded = false;
                        for(LitappInfo info : litappInfoList){
                            if(info.target.equalsIgnoreCase(serviceID)) {
                                mainLitapps.add(info);
                                finded = true;
                                break;
                            }
                        }
                        if(!finded && !serviceID.isEmpty())
                            mainLitapps.add(new LitappInfo(serviceID));
                    }
                    fragment.getActivity().runOnUiThread(()->{
                        adapter.setLitappInfoList(mainLitapps);
                        showContent();
                    });
                    for(LitappInfo litappInfo : mainLitapps)
                        litappViewModel.getLitappInfo(litappInfo.target, true);
                }
                @Override
                public void onUiFailure(int code, String msg) {
                    fragment.getActivity().runOnUiThread(()->{
                        showContent();
                    });
                }
            });
        }).start();
    }
}
