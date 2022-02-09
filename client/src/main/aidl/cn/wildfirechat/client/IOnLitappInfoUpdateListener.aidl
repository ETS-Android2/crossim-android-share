// IOnReceiveMessage.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements
import cn.wildfirechat.model.LitappInfo;

interface IOnLitappInfoUpdateListener {
    void onLitappInfoUpdated(in List<LitappInfo> litappInfos);
}
