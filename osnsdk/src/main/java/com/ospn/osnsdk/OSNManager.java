package com.ospn.osnsdk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ospn.osnsdk.callback.OSNGeneralCallback;
import com.ospn.osnsdk.callback.OSNGeneralCallbackT;
import com.ospn.osnsdk.callback.OSNListener;
import com.ospn.osnsdk.callback.OSNTransferCallback;
import com.ospn.osnsdk.callback.SYNCallback;
import com.ospn.osnsdk.data.OsnFriendInfo;
import com.ospn.osnsdk.data.OsnGroupInfo;
import com.ospn.osnsdk.data.OsnMemberInfo;
import com.ospn.osnsdk.data.OsnMessageInfo;
import com.ospn.osnsdk.data.OsnRequestInfo;
import com.ospn.osnsdk.data.OsnUserInfo;
import com.ospn.osnsdk.data.serviceInfo.OsnLitappInfo;
import com.ospn.osnsdk.data.serviceInfo.OsnServiceInfo;
import com.ospn.osnsdk.utils.ECUtils;
import com.ospn.osnsdk.utils.HttpUtils;
import com.ospn.osnsdk.utils.OsnUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.ospn.osnsdk.utils.OsnUtils.logInfo;
import static com.ospn.osnsdk.utils.OsnUtils.takeMessage;

public class OSNManager {
    private String mOsnID = null;
    private String mOsnKey = null;
    private String mServiceID = null;
    private String mAesKey = null;
    private String mUserName = null;
    private String mDeviceID = null;
    private long mRID = System.currentTimeMillis();
    private boolean mLogined = false;
    private boolean mInitSync = false;
    private long mMsgSyncID = 0;
    private boolean mMsgSynced = false;
    private String mHost = null;
    private int mPort = 8100;
    private Socket mSock = null;
    private OSNListener mOsnListener;
    private final Object mSendLock = new Object();
    private final ConcurrentHashMap<String, SYNCallback> mIDMap = new ConcurrentHashMap<>();
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private static OSNManager INST = null;

    private OSNManager() {
    }
    private JSONObject sendPackage(JSONObject json) {
        try {
            if (!mSock.isConnected())
                return null;
            String id;
            synchronized (this) {
                id = String.valueOf(mRID++);
            }
            json.put("id", id);

            logInfo(json.getString("command") + ": " + json.toString());
            byte[] jsonData = json.toString().getBytes();
            byte[] headData = new byte[4];
            headData[0] = (byte) ((jsonData.length >> 24) & 0xff);
            headData[1] = (byte) ((jsonData.length >> 16) & 0xff);
            headData[2] = (byte) ((jsonData.length >> 8) & 0xff);
            headData[3] = (byte) (jsonData.length & 0xff);
            synchronized (mSendLock) {
                OutputStream outputStream = mSock.getOutputStream();
                outputStream.write(headData);
                outputStream.write(jsonData);
                outputStream.flush();
            }

            final Object lock = new Object();
            final JSONObject[] result = {null};
            SYNCallback synCallback = (id1, json1) -> {
                try {
                    result[0] = json1;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (lock) {
                    lock.notify();
                }
            };
            mIDMap.put(id, synCallback);
            synchronized (lock) {
                lock.wait(10000);
            }
            mIDMap.remove(id);
            return result[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private JSONObject imRespond(String command, JSONObject json, String hash, OSNGeneralCallback callback) {
        if (!isSuccess(json)) {
            logInfo("error: (" + command + ") " + errCode(json));
            if (callback != null)
                callback.onFailure(errCode(json));
            return null;
        }
        JSONObject data = OsnUtils.takeMessage(json, mOsnKey);
        if(data != null){
            data.put("msgHash", hash);
        }
        if (callback != null) {
            if (data == null) {
                logInfo("error: (" + command + ") takeMessage");
                callback.onFailure("takeMessage");
            } else {
                callback.onSuccess(data.toString());
            }
        }
        return data;
    }
    private JSONObject imRequest(String command, String to, JSONObject data, OSNGeneralCallback callback) {
        try {
            JSONObject json = OsnUtils.makeMessage(command, mOsnID, to, data, mOsnKey);
            if (callback != null) {
                mExecutor.execute(() -> {
                    JSONObject result = sendPackage(json);
                    imRespond(command, result, json.getString("hash"), callback);
                });
                return null;
            }
            JSONObject result = sendPackage(json);
            return imRespond(command, result, json.getString("hash"), null);
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null)
                callback.onFailure(e.toString());
        }
        return null;
    }
    private boolean isSuccess(JSONObject json) {
        if (json == null || !json.containsKey("errCode"))
            return false;
        String errCode = json.getString("errCode");
        return errCode.equalsIgnoreCase("success") || errCode.equalsIgnoreCase("0:success");
    }
    private String errCode(JSONObject json) {
        if (json == null)
            return "null";
        if (!json.containsKey("errCode"))
            return "none";
        return json.getString("errCode");
    }
    private JSONObject userCert(){
        JSONObject cert = new JSONObject();
        long timestamp = System.currentTimeMillis();
        String signTime = mServiceID+timestamp;
        cert.put("serviceID", mServiceID);
        cert.put("timestamp", timestamp);
        cert.put("sign", ECUtils.osnSign(mOsnKey, OsnUtils.sha256(signTime.getBytes())));
        return cert;
    }
    private boolean login(OSNGeneralCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("command", "Login");
            json.put("user", mOsnID);
            json.put("name", mUserName);
            json.put("platform", "android");
            json.put("deviceID", mDeviceID);
            json.put("ver", "2");
            json.put("cert", userCert());
            JSONObject data = sendPackage(json);
            if (!isSuccess(data)) {
                if (callback != null)
                    callback.onFailure(errCode(data));
                return false;
            }
            data = takeMessage(data, mOsnKey);

            String osnID = mOsnID;
            mAesKey = data.getString("aesKey");
            mServiceID = data.getString("serviceID");
            mOsnListener.setConfig("aesKey", mAesKey);
            mOsnListener.setConfig("serviceID", mServiceID);

            mLogined = true;
            mOsnListener.onConnectSuccess("logined");
            if (callback != null)
                callback.onSuccess(json.toString());

            new Thread(() -> {
                imRequest("userCert", mServiceID, userCert(), null);
                if (!mInitSync || (osnID != null && !osnID.equalsIgnoreCase(mOsnID))) {
                    if (syncFriend() && syncGroup()) {
                        mInitSync = true;
                        mOsnListener.setConfig("initSync", "true");
                    }
                }
                long timestamp;
                while (true) {
                    timestamp = mMsgSyncID;
                    if (syncMessage(timestamp, 20) != 20)
                        break;
                    if (timestamp == mMsgSyncID)
                        break;
                }
                mMsgSynced = true;
            }).start();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private void setMsgSync(JSONObject json) {
        if (!json.containsKey("timestamp"))
            return;
        if(!mMsgSynced)
            return;
        long timestamp = json.getLongValue("timestamp");
        if (timestamp < mMsgSyncID)
            return;
        mMsgSyncID = timestamp;
        mOsnListener.setConfig("msgSync", String.valueOf(mMsgSyncID));
    }
    private List<OsnMessageInfo> getMessages(JSONObject json) {
        List<OsnMessageInfo> messages = new ArrayList<>();
        try {
            JSONArray array = json.getJSONArray("msgList");
            logInfo("msgList: " + array.size());

            for (Object o : array) {
                json = JSON.parseObject((String) o);
                if (json.containsKey("command") && json.getString("command").equalsIgnoreCase("Message")) {
                    JSONObject data = takeMessage(json, mOsnKey);
                    if (data != null) {
                        OsnMessageInfo messageInfo = new OsnMessageInfo();
                        messageInfo.userID = json.getString("from");
                        messageInfo.target = json.getString("to");
                        messageInfo.timeStamp = json.getLong("timestamp");
                        messageInfo.content = data.getString("content");
                        messageInfo.isGroup = messageInfo.userID.startsWith("OSNG");
                        messageInfo.originalUser = data.getString("originalUser");
                        messages.add(OsnMessageInfo.toMessage(json, data));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }
    private boolean syncGroup() {
        try {
            JSONObject json = imRequest("GetGroupList", mServiceID, null, null);
            if (json == null)
                return false;
            JSONArray groupList = json.getJSONArray("groupList");
            for (Object o : groupList) {
                OsnGroupInfo groupInfo = new OsnGroupInfo();
                groupInfo.groupID = (String) o;
                mOsnListener.onGroupUpdate("SyncGroup", groupInfo, null);
            }
            return true;
        } catch (Exception e) {
            logInfo(e.toString());
        }
        return false;
    }
    private boolean syncFriend() {
        try {
            JSONObject json = imRequest("GetFriendList", mServiceID, null, null);
            if (json == null)
                return false;
            JSONArray friendList = json.getJSONArray("friendList");
            List<OsnFriendInfo> friendInfoList = new ArrayList<>();
            for (Object o : friendList) {
                OsnFriendInfo friendInfo = new OsnFriendInfo();
                friendInfo.state = OsnFriendInfo.Syncst;
                friendInfo.userID = mOsnID;
                friendInfo.friendID = (String) o;
                friendInfoList.add(friendInfo);
            }
            if (friendInfoList.size() != 0)
                mOsnListener.onFriendUpdate(friendInfoList);
            return true;
        } catch (Exception e) {
            logInfo(e.toString());
        }
        return false;
    }
    private int syncMessage(long timestamp, int count) {
        try {
            JSONObject data = new JSONObject();
            data.put("timestamp", timestamp);
            data.put("count", count);
            JSONObject json = imRequest("MessageSync", mServiceID, data, null);
            if (json != null) {
                JSONArray array = json.getJSONArray("msgList");
                logInfo("msgList: " + array.size());

                boolean flag = false;
                List<JSONObject> messageInfos = new ArrayList<>();

                for (Object o : array) {
                    data = JSON.parseObject((String) o);
                    if (data.getString("command").equalsIgnoreCase("Message")) {
                        messageInfos.add(data);
                        flag = true;
                    } else {
                        if (flag) {
                            flag = false;
                            handleMessageRecv(messageInfos);
                            messageInfos.clear();
                        }
                        handleMessage((String) o);
                    }
                }
                if (!messageInfos.isEmpty())
                    handleMessageRecv(messageInfos);
                return array.size();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    private void handleAddFriend(JSONObject json, JSONObject data) {
        try {
            OsnRequestInfo request = new OsnRequestInfo();
            request.reason = data.getString("reason");
            request.userID = json.getString("from");
            request.friendID = json.getString("to");
            request.timeStamp = json.getLong("timestamp");
            request.isGroup = false;
            mOsnListener.onRecvRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleAgreeFriend(JSONObject json, JSONObject data) {
        logInfo("agreeFriend json: " + json.toString());
        logInfo("agreeFriend data: " + data.toString());
    }
    private void handleInviteGroup(JSONObject json, JSONObject data) {
        try {
            //邀请加入群组 originalUser 邀请 friendID 加入 userID
            OsnRequestInfo request = new OsnRequestInfo();
            request.reason = data.getString("reason");
            request.userID = json.getString("from");
            request.friendID = json.getString("to");
            request.timeStamp = json.getLong("timestamp");
            request.originalUser = data.getString("originalUser");
            request.isGroup = true;
            request.isApply = false;
            mOsnListener.onRecvRequest(request);
        } catch (Exception e) {
            logInfo(e.toString());
        }
    }
    private void handleJoinGroup(JSONObject json, JSONObject data) {
        try {
            //审批邀请入群 originalUser 邀请 target 加入 userID
            //审批加入群组 targetUser 申请加入 userID
            OsnRequestInfo request = new OsnRequestInfo();
            request.reason = data.getString("reason");
            request.userID = json.getString("from");
            request.friendID = json.getString("to");
            request.timeStamp = json.getLong("timestamp");
            request.originalUser = data.getString("originalUser");
            request.targetUser = data.getString("userID");
            request.isGroup = true;
            request.isApply = true;
            mOsnListener.onRecvRequest(request);
        } catch (Exception e) {
            logInfo(e.toString());
        }
    }
    private void handleMessageRecv(List<JSONObject> json) {
        try {
            List<OsnMessageInfo> messageInfos = new ArrayList<>();
            for (JSONObject o : json) {
                JSONObject data = takeMessage(o, mOsnKey);
                if (data != null) {
                    OsnMessageInfo messageInfo = OsnMessageInfo.toMessage(o, data);
                    messageInfos.add(messageInfo);
                }
            }
            mOsnListener.onRecvMessage(messageInfos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleMessageRecv(JSONObject json, JSONObject data) {
        try {
            OsnMessageInfo messageInfo = OsnMessageInfo.toMessage(json, data);
            mOsnListener.onRecvMessage(Collections.singletonList(messageInfo));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleMessageSet(JSONObject json, JSONObject data) {
        try {
            OsnMessageInfo messageInfo = OsnMessageInfo.toMessage(json, data);
            mOsnListener.onRecvMessage(Collections.singletonList(messageInfo));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleMessageSync(JSONObject json, JSONObject data) {
        try {
            JSONArray array = data.getJSONArray("msgList");
            logInfo("msgList: " + array.size());
            for (Object o : array)
                handleMessage((String) o);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleGroupUpdate(JSONObject json, JSONObject data) {
        try {
            JSONArray array = data.getJSONArray("infoList");
            OsnGroupInfo groupInfo = OsnGroupInfo.toGroupInfo(data);
            mOsnListener.onGroupUpdate(data.getString("state"), groupInfo, array == null ? null : array.toJavaList(String.class));
        } catch (Exception e) {
            logInfo(e.toString());
        }
    }
    private void handleUserUpdate(JSONObject json, JSONObject data) {
        try {
            OsnUserInfo userInfo = OsnUserInfo.toUserInfo(data);
            JSONArray array = data.getJSONArray("infoList");
            List<String> keys = new ArrayList<>(array.toJavaList(String.class));
            mOsnListener.onUserUpdate(userInfo, keys);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleFriendUpdate(JSONObject json, JSONObject data) {
        try {
            OsnFriendInfo friendInfo = OsnFriendInfo.toFriendInfo(data);
            mOsnListener.onFriendUpdate(Collections.singletonList(friendInfo));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleServiceInfo(JSONObject json, JSONObject data) {
        try{
            String type = data.getString("type");
            if(type.equalsIgnoreCase("infos")){
                mOsnListener.onServiceInfo(OsnServiceInfo.toServiceInfos(data));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void handleMessage(String msg) {
        try {
            JSONObject json = JSON.parseObject(msg);
            String command = json.getString("command");
            logInfo(command + ": " + json.toString());

            setMsgSync(json);

            String id = json.getString("id");
            if (id != null) {
                SYNCallback callback;
                callback = mIDMap.get(id);
                if (callback != null) {
                    callback.onCallback(id, json);
                    return;
                }
            }

            JSONObject data = OsnUtils.takeMessage(json, mOsnKey);
            if (data == null) {
                logInfo("[" + command + "] error: takeMessage");
                return;
            }
            switch (command) {
                case "AddFriend":
                    handleAddFriend(json, data);
                    break;
                case "AgreeFriend":
                    handleAgreeFriend(json, data);
                    break;
                case "InviteGroup":
                    handleInviteGroup(json, data);
                    break;
                case "JoinGroup":
                    handleJoinGroup(json, data);
                    break;
                case "Message":
                    handleMessageRecv(json, data);
                    break;
                case "SetMessage":
                    handleMessageSet(json, data);
                    break;
                case "MessageSync":
                    handleMessageSync(json, data);
                    break;
                case "UserUpdate":
                    handleUserUpdate(json, data);
                    break;
                case "FriendUpdate":
                    handleFriendUpdate(json, data);
                    break;
                case "GroupUpdate":
                    handleGroupUpdate(json, data);
                    break;
                case "KickOff":
                    mOsnListener.onConnectFailed("-1:KickOff");
                    break;
                case "ServiceInfo":
                    handleServiceInfo(json, data);
                    break;
                default:
                    logInfo("unknown command: " + command);
                    break;
            }
        } catch (Exception e) {
            logInfo(e.toString());
        }
    }
    private void initWorker() {
        if (mSock != null)
            return;

        new Thread(() -> {
            logInfo("Start worker thread.");
            while (true) {
                try {
                    logInfo("connect to server: " + mHost);
                    mLogined = false;
                    mMsgSynced = false;
                    mSock = new Socket();
                    try {
                        mSock.connect(new InetSocketAddress(mHost, mPort), 5000);
                    } catch (SocketTimeoutException e) {
                        logInfo(e.toString());
                    } catch (Exception e) {
                        logInfo(e.toString());
                        Thread.sleep(5000);
                    }
                    if (!mSock.isConnected()) {
                        mSock.close();
                        mExecutor.execute(() -> mOsnListener.onConnectFailed("sock connect error"));
                        continue;
                    }
                    mExecutor.execute(() -> mOsnListener.onConnectSuccess("connected"));
                    logInfo("connect to server success");

                    try {
                        InputStream inputStream = mSock.getInputStream();
                        byte[] head = new byte[4];
                        while (true) {
                            if (inputStream.read(head) != 4) {
                                logInfo("sock read error");
                                break;
                            }
                            int length = ((head[0] & 0xff) << 24) | ((head[1] & 0xff) << 16) | ((head[2] & 0xff) << 8) | (head[3] & 0xff);
                            byte[] data = new byte[length];
                            int read = 0;
                            while (read < length)
                                read += inputStream.read(data, read, length - read);
                            String msg = new String(data);
                            mExecutor.execute(() -> handleMessage(msg));
                        }
                        mExecutor.execute(() -> mOsnListener.onConnectFailed("sock read error"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        mExecutor.execute(() -> mOsnListener.onConnectFailed(e.toString()));
                    }
                    mSock.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    mExecutor.execute(() -> mOsnListener.onConnectFailed(e.toString()));
                }
            }
        }).start();
        new Thread(() -> {
            logInfo("Start heart thread.");

            JSONObject json = new JSONObject();
            json.put("command", "Heart");

            int time = 0;
            while (true) {
                try {
                    Thread.sleep(5000);
                    if (mSock != null && mSock.isConnected()) {
                        if (!mLogined && mOsnID != null) {
                            login(null);
                        } else if (++time % 2 != 0) {
                            JSONObject result = sendPackage(json);
                            if (!isSuccess(result)) {
                                logInfo("heart timeout");
                                mSock.close();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public static OSNManager Instance() {
        if (INST == null)
            INST = new OSNManager();
        return INST;
    }
    public void initSDK(String ip, OSNListener listener) {
        mHost = ip;
        mOsnListener = listener;
        mOsnID = mOsnListener.getConfig("osnID");
        mOsnKey = mOsnListener.getConfig("osnKey");
        mAesKey = mOsnListener.getConfig("aesKey");
        mServiceID = mOsnListener.getConfig("serviceID");
        mUserName = mOsnListener.getConfig("name");
        String msgSync = mOsnListener.getConfig("msgSync");
        mMsgSyncID = (msgSync == null ? 0 : Long.parseLong(msgSync));
        if (mMsgSyncID == 0) {
            mMsgSyncID = System.currentTimeMillis();
            mOsnListener.setConfig("msgSync", String.valueOf(mMsgSyncID));
        }
        String initSync = mOsnListener.getConfig("initSync");
        mInitSync = initSync != null && initSync.equalsIgnoreCase("true");
        mDeviceID = mOsnListener.getConfig("deviceID");
        if (mDeviceID == null) {
            mDeviceID = UUID.randomUUID().toString();
            mOsnListener.setConfig("deviceID", mDeviceID);
        }
        initWorker();
    }
    public void resetHost(String ip) {
        try {
            mHost = ip;
            if (mSock != null)
                mSock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean loginWithShare(JSONObject info){
        mOsnListener.setConfig("userInfo", info.toString());
        mOsnID = info.getString("osnID");
        mOsnKey = info.getString("osnKey");
        mUserName = info.getString("name");
        mOsnListener.setConfig("osnID", mOsnID);
        mOsnListener.setConfig("osnKey", mOsnKey);
        mOsnListener.setConfig("name", mUserName);
        return login(null);
    }
    public void logout(OSNGeneralCallback callback) {
        try {
            imRequest("Logout", mServiceID, userCert(), null);

            mOsnID = null;
            mLogined = false;
            mInitSync = false;
            mOsnListener.setConfig("osnID", null);
            mOsnListener.setConfig("initSync", "false");
            if (mSock != null)
                mSock.close();
            if (callback != null)
                callback.onSuccess(null);
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null)
                callback.onFailure(e.toString());
        }
    }
    public String getUserID() {
        return mOsnID;
    }
    public String getServiceID() {
        return mServiceID;
    }
    public OsnUserInfo getUserInfo(String userID, OSNGeneralCallbackT<OsnUserInfo> callback) {
        try {
            if (callback == null) {
                JSONObject json = imRequest("GetUserInfo", userID, null, null);
                if (json != null)
                    return OsnUserInfo.toUserInfo(json);
            } else {
                imRequest("GetUserInfo", userID, null, new OSNGeneralCallback() {
                    @Override
                    public void onSuccess(String json) {
                        try {
                            JSONObject jsonObject = JSON.parseObject(json);
                            callback.onSuccess(OsnUserInfo.toUserInfo(jsonObject));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public OsnGroupInfo getGroupInfo(String groupID, OSNGeneralCallbackT<OsnGroupInfo> callback) {
        try {
            if (callback == null) {
                JSONObject json = imRequest("GetGroupInfo", groupID, null, null);
                if (json != null)
                    return OsnGroupInfo.toGroupInfo(json);
            } else {
                imRequest("GetGroupInfo", groupID, null, new OSNGeneralCallback() {
                    @Override
                    public void onSuccess(String json) {
                        try {
                            JSONObject jsonObject = JSON.parseObject(json);
                            callback.onSuccess(OsnGroupInfo.toGroupInfo(jsonObject));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public List<OsnMemberInfo> getMemberInfo(String groupID, OSNGeneralCallbackT<List<OsnMemberInfo>> callback) {
        try {
            if (callback == null) {
                JSONObject json = imRequest("GetMemberInfo", groupID, null, null);
                if (json != null)
                    return OsnMemberInfo.toMemberInfos(json);
            } else {
                imRequest("GetMemberInfo", groupID, null, new OSNGeneralCallback() {
                    @Override
                    public void onSuccess(String json) {
                        try {
                            JSONObject jsonObject = JSON.parseObject(json);
                            callback.onSuccess(OsnMemberInfo.toMemberInfos(jsonObject));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public OsnServiceInfo getServiceInfo(String serviceID, OSNGeneralCallbackT<OsnServiceInfo> callback) {
        try {
            if (callback == null) {
                JSONObject json = imRequest("GetServiceInfo", serviceID, null, null);
                if (json != null)
                    return OsnServiceInfo.toServiceInfo(json);
            } else {
                imRequest("GetServiceInfo", serviceID, null, new OSNGeneralCallback() {
                    @Override
                    public void onSuccess(String json) {
                        try {
                            JSONObject jsonObject = JSON.parseObject(json);
                            callback.onSuccess(OsnServiceInfo.toServiceInfo(jsonObject));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public void findServiceInfo(String keyword, OSNGeneralCallbackT<List<OsnServiceInfo>> callback) {
        try {
            if(callback == null || keyword == null || keyword.isEmpty()){
                return;
            }
            JSONObject json = new JSONObject();
            json.put("keyword", keyword);
            json.put("type", "findService");
            imRequest("Broadcast", null, json, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public OsnFriendInfo getFriendInfo(String friendID, OSNGeneralCallbackT<OsnFriendInfo> callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("friendID", friendID);
            if (callback == null) {
                JSONObject json = imRequest("GetFriendInfo", mServiceID, data, null);
                if (json != null)
                    return OsnFriendInfo.toFriendInfo(json);
            } else {
                imRequest("GetFriendInfo", mServiceID, data, new OSNGeneralCallback() {
                    @Override
                    public void onSuccess(String json) {
                        try {
                            JSONObject jsonObject = JSON.parseObject(json);
                            callback.onSuccess(OsnFriendInfo.toFriendInfo(jsonObject));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public void modifyUserInfo(List<String> keys, OsnUserInfo userInfo, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        for (String k : keys) {
            if (k.equalsIgnoreCase("displayName"))
                data.put("displayName", userInfo.displayName);
            else if (k.equalsIgnoreCase("portrait"))
                data.put("portrait", userInfo.portrait);
            else if (k.equalsIgnoreCase("urlSpace"))
                data.put("urlSpace", userInfo.urlSpace);
        }
        imRequest("SetUserInfo", mServiceID, data, callback);
    }
    public void modifyFriendInfo(List<String> keys, OsnFriendInfo friendInfo, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        data.put("friendID", friendInfo.friendID);
        for (String k : keys) {
            if (k.equalsIgnoreCase("remarks"))
                data.put("remarks", friendInfo.remarks);
            else if (k.equalsIgnoreCase("state"))
                data.put("state", friendInfo.state);
        }
        imRequest("SetFriendInfo", mServiceID, data, callback);
    }
    public List<String> getFriendList(OSNGeneralCallbackT<List<String>> callback) {
        try {
            if (callback == null) {
                JSONObject json = imRequest("GetFriendList", mServiceID, null, null);
                if (json != null) {
                    List<String> friendInfoList = new ArrayList<>();
                    JSONArray friendList = json.getJSONArray("friendList");
                    for (Object o : friendList)
                        friendInfoList.add((String) o);
                    return friendInfoList;
                }
            } else {
                imRequest("GetFriendList", mServiceID, null, new OSNGeneralCallback() {
                    @Override
                    public void onSuccess(String json) {
                        try {
                            JSONObject jsonObject = JSON.parseObject(json);
                            List<String> friendInfoList = new ArrayList<>();
                            JSONArray friendList = jsonObject.getJSONArray("friendList");
                            for (Object o : friendList)
                                friendInfoList.add((String) o);
                            callback.onSuccess(friendInfoList);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public List<String> getGroupList(OSNGeneralCallbackT<List<String>> callback) {
        try {
            if (callback == null) {
                JSONObject json = imRequest("GetGroupList", mServiceID, null, null);
                if (json != null) {
                    List<String> groupInfoList = new ArrayList<>();
                    JSONArray groupList = json.getJSONArray("groupList");
                    for (Object o : groupList)
                        groupInfoList.add((String) o);
                    return groupInfoList;
                }
            } else {
                imRequest("GetGroupList", mServiceID, null, new OSNGeneralCallback() {
                    @Override
                    public void onSuccess(String json) {
                        try {
                            JSONObject jsonObject = JSON.parseObject(json);
                            List<String> groupInfoList = new ArrayList<>();
                            JSONArray groupList = jsonObject.getJSONArray("groupList");
                            for (Object o : groupList)
                                groupInfoList.add((String) o);
                            callback.onSuccess(groupInfoList);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public void inviteFriend(String userID, String reason, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        data.put("reason", reason);
        imRequest("AddFriend", userID, data, callback);
    }
    public void deleteFriend(String userID, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        data.put("friendID", userID);
        imRequest("DelFriend", mServiceID, data, callback);
    }
    public void acceptFriend(String userID, OSNGeneralCallback callback) {
        imRequest("AgreeFriend", userID, null, callback);
    }
    public void rejectFriend(String userID, OSNGeneralCallback callback) {
        imRequest("RejectFriend", userID, null, callback);
    }
    public void acceptMember(String userID, String groupID, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        data.put("userID", userID);
        imRequest("AgreeMember", groupID, data, callback);
    }
    public void rejectMember(String userID, String groupID, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        data.put("userID", userID);
        imRequest("RejectMember", userID, data, callback);
    }
    public void sendMessage(String text, String userID, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        data.put("content", text);
        if (userID.startsWith("OSNG"))
            data.put("originalUser", mOsnID);
        imRequest("Message", userID, data, callback);
    }
    public void sendBroadcast (String content, OSNGeneralCallback callback){
        JSONObject data = JSON.parseObject(content);
        imRequest("Broadcast", null, data, callback);
    }
    public void deleteMessage(String hash, String osnID, OSNGeneralCallback callback){
        JSONObject data = new JSONObject();
        String target = osnID == null || osnID.startsWith("OSNU") ? mServiceID : osnID;
        data.put("type", "delete");
        data.put("osnID", target);
        data.put("messageHash", hash);
        imRequest("SetMessage", target, data, callback);
    }
    public List<OsnMessageInfo> loadMessage(String userID, long timestamp, int count, boolean before, OSNGeneralCallbackT<List<OsnMessageInfo>> callback) {
        try {
            JSONObject data = new JSONObject();
            data.put("userID", userID);
            data.put("timestamp", timestamp);
            data.put("count", count);
            data.put("before", before);
            if (callback == null) {
                JSONObject json = imRequest("MessageLoad", mServiceID, data, null);
                if (json == null)
                    return null;
                return getMessages(json);
            }
            imRequest("MessageLoad", mServiceID, data, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String jsonObject) {
                    JSONObject json = JSON.parseObject(jsonObject);
                    List<OsnMessageInfo> messages = getMessages(json);
                    callback.onSuccess(messages);
                }

                @Override
                public void onFailure(String error) {
                    callback.onFailure(error);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public void createGroup(String groupName, List<String> member, int type, String portrait, OSNGeneralCallback callback) {
        JSONArray array = new JSONArray();
        array.addAll(member);
        JSONObject data = new JSONObject();
        data.put("name", groupName);
        data.put("type", type);
        data.put("portrait", portrait);
        data.put("userList", array);
        imRequest("CreateGroup", mServiceID, data, callback);
    }
    public void joinGroup(String groupID, String reason, OSNGeneralCallback callback) {
        JSONObject data = null;
        if (reason != null) {
            data = new JSONObject();
            data.put("reason", reason);
        }
        imRequest("JoinGroup", groupID, data, callback);
    }
    public void rejectGroup(String groupID, OSNGeneralCallback callback) {
        imRequest("RejectGroup", groupID, null, callback);
    }
    public void addMember(String groupID, List<String> members, OSNGeneralCallback callback) {
        JSONArray array = new JSONArray();
        array.addAll(members);
        JSONObject data = new JSONObject();
        data.put("state", "AddMember");
        data.put("memberList", array);
        imRequest("AddMember", groupID, data, callback);
    }
    public void delMember(String groupID, List<String> members, OSNGeneralCallback callback) {
        JSONArray array = new JSONArray();
        array.addAll(members);
        JSONObject data = new JSONObject();
        data.put("state", "DelMember");
        data.put("memberList", array);
        imRequest("DelMember", groupID, data, callback);
    }
    public void quitGroup(String groupID, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        data.put("state", "QuitGroup");
        imRequest("QuitGroup", groupID, data, callback);
    }
    public void dismissGroup(String groupID, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        data.put("state", "DelGroup");
        imRequest("DelGroup", groupID, data, callback);
    }
    public void modifyGroupInfo(List<String> keys, OsnGroupInfo groupInfo, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        for (String k : keys) {
            if (k.equalsIgnoreCase("name"))
                data.put("name", groupInfo.name);
            else if (k.equalsIgnoreCase("portrait"))
                data.put("portrait", groupInfo.portrait);
            else if (k.equalsIgnoreCase("type"))
                data.put("type", groupInfo.type);
            else if (k.equalsIgnoreCase("joinType"))
                data.put("joinType", groupInfo.joinType);
            else if (k.equalsIgnoreCase("passType"))
                data.put("passType", groupInfo.passType);
            else if (k.equalsIgnoreCase("mute"))
                data.put("mute", groupInfo.mute);
        }
        imRequest("SetGroupInfo", groupInfo.groupID, data, callback);
    }
    public void modifyMemberInfo(List<String> keys, OsnMemberInfo memberInfo, OSNGeneralCallback callback) {
        JSONObject data = new JSONObject();
        for (String k : keys) {
            if (k.equalsIgnoreCase("nickName"))
                data.put("nickName", memberInfo.nickName);
        }
        imRequest("SetMemberInfo", memberInfo.groupID, data, callback);
    }
    public void uploadData(String fileName, String type, byte[] data, OSNTransferCallback callback) {
        if (callback == null) {
            HttpUtils.upload("http://" + mHost + ":8800/", type, fileName, data, null);
            return;
        }
        new Thread(() -> {
            HttpUtils.upload("http://" + mHost + ":8800/", type, fileName, data, callback);
        }).start();
    }
    public void downloadData(String remoteUrl, String localPath, OSNTransferCallback callback) {
        if (callback == null) {
            HttpUtils.download(remoteUrl, localPath, callback);
            return;
        }
        new Thread(() -> {
            HttpUtils.download(remoteUrl, localPath, callback);
        }).start();
    }
    public void lpLogin(OsnLitappInfo litappInfo, String url, OSNGeneralCallback callback) {
        new Thread(() -> {
            String error = null;
            try {
                long randClient = System.currentTimeMillis();
                JSONObject json = new JSONObject();
                json.put("command", "GetServerInfo");
                json.put("user", mOsnID);
                json.put("random", randClient);
                String data = HttpUtils.doPost(url, json.toString());
                logInfo("result: "+data);

                json = JSON.parseObject(data);
                String serviceID = json.getString("serviceID");
                String randServer = json.getString("random");
                String serverInfo = json.getString("serviceInfo");
                String session = json.getString("session");

                if (!serviceID.equalsIgnoreCase(litappInfo.target)) {
                    error = "serviceID no equals litappID: " + litappInfo.target + ", serviceID: " + serviceID;
                } else {
                    data = mOsnID + randClient + serviceID + randServer + serverInfo;
                    String hash = ECUtils.osnHash(data.getBytes());
                    String sign = json.getString("sign");
                    logInfo("data: " + data);
                    logInfo("hash: " + hash);
                    logInfo("sign: " + sign);
                    if (hash.equalsIgnoreCase(json.getString("hash")) &&
                            ECUtils.osnVerify(serviceID, hash.getBytes(), sign)) {
                        hash = ECUtils.osnHash((serviceID + randServer + mOsnID + randClient).getBytes());
                        sign = ECUtils.osnSign(mOsnKey, hash.getBytes());
                        json.clear();
                        json.put("command", "Login");
                        json.put("user", mOsnID);
                        json.put("hash", hash);
                        json.put("sign", sign);
                        json.put("session", session);
                        data = HttpUtils.doPost(url, json.toString());

                        json = JSON.parseObject(data);
                        if (!isSuccess(json))
                            error = errCode(json);
                        else {
                            data = new String(ECUtils.ecDecrypt2(mOsnKey, json.getString("sessionKey")));
                            callback.onSuccess(data);
                        }
                    } else
                        error = "verify error";
                }
            } catch (Exception e) {
                e.printStackTrace();
                error = e.toString();
            }
            if (error != null)
                callback.onFailure(error);
        }).start();
    }
    public String signData(String data) {
        return ECUtils.osnSign(mOsnKey, data.getBytes());
    }
}
