package cn.wildfire.chat.kit.litapp;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnLitappInfoUpdateListener;

public class LitappViewModel extends ViewModel implements OnLitappInfoUpdateListener {
    private MutableLiveData<List<LitappInfo>> litappInfoUpdateLiveData;

    public LitappViewModel() {
        super();
        ChatManager.Instance().addLitappInfoUpdateListener(this);
    }
    @Override
    protected void onCleared() {
        ChatManager.Instance().removeLitappInfoUpdateListener(this);
    }

    public MutableLiveData<List<LitappInfo>> litappInfoLiveData() {
        if (litappInfoUpdateLiveData == null) {
            litappInfoUpdateLiveData = new MutableLiveData<>();
        }
        return litappInfoUpdateLiveData;
    }

    public LitappInfo getLitappInfo(String litappId, boolean refresh) {
        return ChatManager.Instance().getLitappInfo(litappId, refresh);
    }
    public List<LitappInfo> getLitappList(){
        return ChatManager.Instance().getLitappList();
    }

    @Override
    public void onLitappInfoUpdate(List<LitappInfo> litappInfos) {
        if (litappInfoUpdateLiveData != null) {
            litappInfoUpdateLiveData.setValue(litappInfos);
        }
    }
}
