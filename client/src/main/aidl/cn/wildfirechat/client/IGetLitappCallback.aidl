// IGetLitappCallback.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.LitappInfo;

interface IGetLitappCallback {
    void onSuccess(in LitappInfo litapp);
    void onFailure(in int errorCode);
}