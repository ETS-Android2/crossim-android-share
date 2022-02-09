/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.client;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ospn.osnsdk.OSNManager;
import com.ospn.osnsdk.callback.OSNGeneralCallback;
import com.ospn.osnsdk.callback.OSNGeneralCallbackT;
import com.ospn.osnsdk.callback.OSNListener;
import com.ospn.osnsdk.callback.OSNTransferCallback;
import com.ospn.osnsdk.data.OsnFriendInfo;
import com.ospn.osnsdk.data.OsnGroupInfo;
import com.ospn.osnsdk.data.OsnMemberInfo;
import com.ospn.osnsdk.data.OsnMessageInfo;
import com.ospn.osnsdk.data.OsnRequestInfo;
import com.ospn.osnsdk.data.OsnUserInfo;
import com.ospn.osnsdk.data.serviceInfo.OsnLitappInfo;
import com.ospn.osnsdk.data.serviceInfo.OsnServiceInfo;
import com.ospn.osnsdk.utils.HttpUtils;
import com.ospn.osnsdk.utils.OsnUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfirechat.ErrorCode;
import cn.wildfirechat.message.CallStartMessageContent;
import cn.wildfirechat.message.CardMessageContent;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.LocationMessageContent;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.message.PTextMessageContent;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.StickerMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.notification.AddGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.ChangeGroupNameNotificationContent;
import cn.wildfirechat.message.notification.ChangeGroupPortraitNotificationContent;
import cn.wildfirechat.message.notification.CreateGroupNotificationContent;
import cn.wildfirechat.message.notification.DeleteMessageContent;
import cn.wildfirechat.message.notification.DismissGroupNotificationContent;
import cn.wildfirechat.message.notification.FriendAddedMessageContent;
import cn.wildfirechat.message.notification.FriendGreetingMessageContent;
import cn.wildfirechat.message.notification.GroupJoinTypeNotificationContent;
import cn.wildfirechat.message.notification.GroupMuteMemberNotificationContent;
import cn.wildfirechat.message.notification.GroupMuteNotificationContent;
import cn.wildfirechat.message.notification.GroupPrivateChatNotificationContent;
import cn.wildfirechat.message.notification.GroupSetManagerNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupAliasNotificationContent;
import cn.wildfirechat.message.notification.QuitGroupNotificationContent;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.message.notification.TipNotificationContent;
import cn.wildfirechat.message.notification.TransferGroupOwnerNotificationContent;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.ConversationSearchResult;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.GroupSearchResult;
import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.model.ModifyGroupInfoType;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.ModifyMyInfoType;
import cn.wildfirechat.model.NullGroupInfo;
import cn.wildfirechat.model.NullGroupMember;
import cn.wildfirechat.model.NullUserInfo;
import cn.wildfirechat.model.ReadEntry;
import cn.wildfirechat.model.UnreadCount;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.RecoverReceiver;
import cn.wildfirechat.remote.UserSettingScope;

import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnected;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusConnecting;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusKickOffline;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusLogout;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusReceiveing;
import static cn.wildfirechat.client.ConnectionStatus.ConnectionStatusUnconnected;
import static cn.wildfirechat.message.CardMessageContent.CardType_Channel;
import static cn.wildfirechat.message.CardMessageContent.CardType_ChatRoom;
import static cn.wildfirechat.message.CardMessageContent.CardType_Group;
import static cn.wildfirechat.message.CardMessageContent.CardType_Litapp;
import static cn.wildfirechat.message.CardMessageContent.CardType_Share;
import static cn.wildfirechat.message.CardMessageContent.CardType_User;
import static cn.wildfirechat.model.FriendRequest.RequestType_ApplyMember;
import static cn.wildfirechat.model.FriendRequest.RequestType_Friend;
import static cn.wildfirechat.model.FriendRequest.RequestType_InviteGroup;
import static com.ospn.osnsdk.utils.OsnUtils.logInfo;

/**
 * Created by heavyrain lee on 2017/11/19.
 */

public class ClientService extends Service {
    public final static int MAX_IPC_SIZE = 800 * 1024;
    private final ClientServiceStub mBinder = new ClientServiceStub();
    private final Map<Integer, Class<? extends MessageContent>> contentMapper = new HashMap<>();
    private int mConnectionStatus;
    private String mBackupDeviceToken;
    private int mBackupPushType;
    private Handler handler;
    private Context mContext;
    private String mHost = null;
    private String mUserId = null;
    private boolean logined;
    private boolean isConnect = false;
    private boolean isInitSdk = false;
    private SharedPreferences mSp = null;
    private List<String> keywords = null;
    private final OSNManager osnsdk = OSNManager.Instance();
    private final RemoteCallbackList<IOnReceiveMessageListener> onReceiveMessageListeners = new WfcRemoteCallbackList<>();
    private final RemoteCallbackList<IOnConnectionStatusChangeListener> onConnectionStatusChangeListenes = new WfcRemoteCallbackList<>();
    private final RemoteCallbackList<IOnFriendUpdateListener> onFriendUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private final RemoteCallbackList<IOnUserInfoUpdateListener> onUserInfoUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private final RemoteCallbackList<IOnGroupInfoUpdateListener> onGroupInfoUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private final RemoteCallbackList<IOnLitappInfoUpdateListener> onLitappInfoUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private final RemoteCallbackList<IOnSettingUpdateListener> onSettingUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private final RemoteCallbackList<IOnChannelInfoUpdateListener> onChannelInfoUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    private final RemoteCallbackList<IOnGroupMembersUpdateListener> onGroupMembersUpdateListenerRemoteCallbackList = new WfcRemoteCallbackList<>();
    OSNListener mListener = new OSNListener() {
        @Override
        public String getConfig(String key) {
            try {
                return mSp.getString(key, null);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        public void setConfig(String key, String value) {
            if (value == null)
                mSp.edit().remove(key);
            else
                mSp.edit().putString(key, value).commit();
        }

        @Override
        public void onConnectSuccess(String state) {
            if (state.equalsIgnoreCase("logined"))
                mUserId = osnsdk.getUserID();
            onConnectionStatusChanged(ConnectionStatusConnected);
            isConnect = true;
        }

        @Override
        public void onConnectFailed(String error) {
            logined = false;
            isConnect = false;
            if (error.contains("KickOff"))
                onConnectionStatusChanged(ConnectionStatusKickOffline);
            else
                onConnectionStatusChanged(ConnectionStatusUnconnected);
        }

        @Override
        public void onSetMessage(OsnMessageInfo msgInfo) {
            try{
                JSONObject data = JSON.parseObject(msgInfo.content);
                String type = data.getString("type");
                String hash = data.getString("messageHash");
                logInfo("type: "+type+", hash: "+hash);
                if(type == null || hash == null){
                    return;
                }
                Message msg = SqliteUtils.queryMessage(hash);
                if(msg == null){
                    logInfo("no found message hash: "+hash);
                    return;
                }
                Message message = new Message();
                message.messageId = 0;
                message.direction = MessageDirection.Receive;
                message.status = MessageStatus.Readed;
                message.messageUid = 0;
                message.serverTime = System.currentTimeMillis();
                SqliteUtils.deleteMessage(msg.messageId);
                if(type.equalsIgnoreCase("delete")){
                    message.content = new DeleteMessageContent(msg.sender, msg.messageUid);
                    //SqliteUtils.insertMessage(message);
                    onDeleteMessage(msg.messageUid);
                } else {
                    message.content = new RecallMessageContent(msg.sender, msg.messageUid);
                    //SqliteUtils.insertMessage(message);
                    onRecallMessage(msg.messageUid);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRecvMessage(List<OsnMessageInfo> msgList) {
            List<Message> messageList = recvMessage(msgList, false);
            if (messageList.isEmpty())
                return;
            for (Message message : messageList) {
                ConversationInfo conversationInfo = SqliteUtils.queryConversation(message.conversation.type.getValue(), message.conversation.target, message.conversation.line);
                if (conversationInfo == null) {
                    SqliteUtils.insertConversation(message.conversation.type.getValue(), message.conversation.target, message.conversation.line);
                    conversationInfo = SqliteUtils.queryConversation(message.conversation.type.getValue(), message.conversation.target, message.conversation.line);
                }
                conversationInfo.timestamp = message.serverTime;
                conversationInfo.lastMessage = message;
                conversationInfo.unreadCount.unread += 1;
                SqliteUtils.updateConversation(conversationInfo);

                if (message.conversation.type == Conversation.ConversationType.Service) {
                    conversationInfo = SqliteUtils.queryConversation(Conversation.ConversationType.Notify.getValue(), "0", message.conversation.line);
                    if (conversationInfo == null) {
                        SqliteUtils.insertConversation(Conversation.ConversationType.Notify.getValue(), "0", message.conversation.line);
                        conversationInfo = SqliteUtils.queryConversation(Conversation.ConversationType.Notify.getValue(), "0", message.conversation.line);
                    }
                    conversationInfo.timestamp = message.serverTime;
                    conversationInfo.lastMessage = message;
                    conversationInfo.unreadCount.unread += 1;
                    SqliteUtils.updateConversation(conversationInfo);
                }
            }
            onReceiveMessage(messageList, false);
        }

        @Override
        public void onRecvRequest(OsnRequestInfo request) {
            try {
                if (request.isGroup) {
                    FriendRequest friendRequest = new FriendRequest();
                    friendRequest.type = request.isApply ? RequestType_ApplyMember : RequestType_InviteGroup;
                    friendRequest.readStatus = 0;
                    friendRequest.direction = FriendRequest.Direction_Recv;
                    friendRequest.reason = request.reason;
                    friendRequest.status = 0;
                    friendRequest.target = request.userID;
                    friendRequest.originalUser = request.originalUser;
                    friendRequest.userID = request.targetUser;
                    friendRequest.timestamp = request.timeStamp;
                    SqliteUtils.insertFriendRequest(friendRequest);

                    //osnsdk.joinGroup(request.userID, null, null);
                    onFriendRequestUpdated(new String[]{request.reason});
                } else {
                    if (mBinder.isMyFriend(request.userID))
                        osnsdk.acceptFriend(request.userID, null);
                    else {
                        FriendRequest friendRequest = new FriendRequest();
                        friendRequest.type = RequestType_Friend;
                        friendRequest.readStatus = 0;
                        friendRequest.direction = FriendRequest.Direction_Recv;
                        friendRequest.reason = request.reason;
                        friendRequest.status = 0;
                        friendRequest.target = request.userID;
                        friendRequest.timestamp = request.timeStamp;
                        SqliteUtils.insertFriendRequest(friendRequest);
                        onFriendRequestUpdated(new String[]{request.reason});
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFriendUpdate(List<OsnFriendInfo> friendList) {
            List<String> friends = new ArrayList<>();
            for (OsnFriendInfo f : friendList) {
                friends.add(f.friendID);
                if (SqliteUtils.queryFriend(f.friendID) != null) {
                    if (f.state == OsnFriendInfo.Deleted) {
                        logInfo("delete friend - userID:" + f.userID + ", friendID: " + f.friendID);
                        SqliteUtils.deleteFriend(f.friendID);
                    } else if (f.state == OsnFriendInfo.Blacked) {
                        logInfo("blacked friend - friendID: " + f.friendID);
                        SqliteUtils.updateFriend(f);
                    }
                    continue;
                }

                if (f.state == OsnFriendInfo.Normal) {
                    SqliteUtils.insertFriend(f);
                    addFriend(f);
                } else if (f.state == OsnFriendInfo.Syncst) {
                    OsnFriendInfo friendInfo = osnsdk.getFriendInfo(f.friendID, null);
                    if (friendInfo == null) {
                        logInfo("get friendInfo failed: " + f.friendID);
                        continue;
                    }
                    logInfo("syncst friend: " + friendInfo);
                    SqliteUtils.insertFriend(friendInfo);
                } else {
                    logInfo("friend state is wait: " + f.friendID);
                    SqliteUtils.insertFriend(f);
                }
            }
            onFriendListUpdated(friends.toArray(new String[1]));
        }

        @Override
        public void onUserUpdate(OsnUserInfo osnUserInfo, List<String> keys) {
            UserInfo userInfo = SqliteUtils.queryUser(osnUserInfo.userID);
            if (userInfo == null)
                return;
            for (String k : keys) {
                if (k.equalsIgnoreCase("displayName"))
                    userInfo.displayName = osnUserInfo.displayName;
                else if (k.equalsIgnoreCase("portrait"))
                    userInfo.portrait = osnUserInfo.portrait;
                else if (k.equalsIgnoreCase("urlSpace"))
                    userInfo.urlSpace = osnUserInfo.urlSpace;
            }
            SqliteUtils.updateUser(userInfo, keys);
            onUserInfoUpdated(Collections.singletonList(userInfo));
        }

        @Override
        public void onGroupUpdate(String state, OsnGroupInfo osnGroupInfo, List<String> keys) {
            try {
                GroupInfo groupInfo;
                List<GroupMember> memberList;

                switch (state) {
                    case "NewlyGroup":
                        //groupInfo = SqliteUtils.queryGroup(osnGroupInfo.groupID);
                        addGroup(osnGroupInfo);
                        break;

                    case "SyncGroup":
                        groupInfo = SqliteUtils.queryGroup(osnGroupInfo.groupID);
                        if (groupInfo == null) {
                            osnsdk.getGroupInfo(osnGroupInfo.groupID, new OSNGeneralCallbackT<OsnGroupInfo>() {
                                @Override
                                public void onFailure(String error) {
                                    logInfo("SyncGroup null groupID: " + osnGroupInfo.groupID);
                                    addGroupNull(osnGroupInfo.groupID);
                                }

                                @Override
                                public void onSuccess(OsnGroupInfo osnGroupInfo) {
                                    addGroup(osnGroupInfo);
                                }
                            });
                        }
                        break;
                    case "UpdateGroup":
                        groupInfo = SqliteUtils.queryGroup(osnGroupInfo.groupID);
                        if (groupInfo != null) {
                            for (String k : keys) {
                                switch (k) {
                                    case "name":
                                        groupInfo.name = osnGroupInfo.name;
                                        logInfo("new group name: " + groupInfo.name);
                                        break;
                                    case "portrait":
                                        groupInfo.portrait = osnGroupInfo.portrait;
                                        logInfo("new group portrait: " + groupInfo.portrait);
                                        break;
                                    case "type":
                                        groupInfo.type = GroupInfo.GroupType.type(osnGroupInfo.type);
                                        logInfo("new group type: " + groupInfo.type.value());
                                        break;
                                    case "joinType":
                                        groupInfo.joinType = osnGroupInfo.joinType;
                                        logInfo("new group joinType: " + groupInfo.joinType);
                                        break;
                                    case "passType":
                                        groupInfo.passType = osnGroupInfo.passType;
                                        logInfo("new group passType: " + groupInfo.passType);
                                        break;
                                    case "mute":
                                        groupInfo.mute = osnGroupInfo.mute;
                                        logInfo("new group mute: " + groupInfo.mute);
                                        break;
                                }
                            }
                            SqliteUtils.updateGroup(groupInfo, keys);
                            onGroupInfoUpdated(Collections.singletonList(groupInfo));
                        }
                        break;

                    case "UpdateMember":
                        groupInfo = SqliteUtils.queryGroup(osnGroupInfo.groupID);
                        if (groupInfo == null) {
                            logInfo("no my groupID: " + osnGroupInfo.groupID);
                            break;
                        }
                        memberList = new ArrayList<>();
                        for (OsnMemberInfo m : osnGroupInfo.userList) {
                            GroupMember groupMember = SqliteUtils.queryMember(m.groupID, m.osnID);
                            if (groupMember == null) {
                                logInfo("no my memberID: " + m.osnID);
                                continue;
                            }
                            memberList.add(groupMember);
                            List<String> keyx = new ArrayList<>();
                            for (String k : keys) {
                                if (k.equalsIgnoreCase("nickName")) {
                                    groupMember.alias = m.nickName;
                                    logInfo("new member alias: " + m.nickName + ", osnID: " + m.osnID);
                                    keyx.add("alias");
                                } else if (k.equalsIgnoreCase("type")) {
                                    if (m.type == OsnMemberInfo.MemberType_Normal)
                                        groupMember.type = GroupMember.GroupMemberType.Normal;
                                    else if (m.type == OsnMemberInfo.MemberType_Owner)
                                        groupMember.type = GroupMember.GroupMemberType.Owner;
                                    else if (m.type == OsnMemberInfo.MemberType_Admin)
                                        groupMember.type = GroupMember.GroupMemberType.Manager;
                                    keyx.add("type");
                                    logInfo("new member type: " + m.type + ", osnID: " + m.osnID);
                                }
                            }
                            SqliteUtils.updateMember(groupMember, keyx);
                        }
                        onGroupMembersUpdated(osnGroupInfo.groupID, memberList);
                        break;

                    case "DelMember":
                        groupInfo = SqliteUtils.queryGroup(osnGroupInfo.groupID);
                        if (groupInfo != null) {
                            memberList = SqliteUtils.queryMembers(osnGroupInfo.groupID);
                            List<OsnMemberInfo> delList = getMemberX(osnGroupInfo.userList, memberList, false);
                            if (!delList.isEmpty()) {
                                for (OsnMemberInfo memberInfo : delList)
                                    logInfo("delete members: " + memberInfo.osnID);
                                SqliteUtils.deleteMembers(delList);
                                groupInfo.memberCount -= delList.size();
                                SqliteUtils.updateGroup(groupInfo, Collections.singletonList("memberCount"));
                                onGroupInfoUpdated(Collections.singletonList(groupInfo));
                                onGroupMembersUpdated(osnGroupInfo.groupID, memberList);
                                delMemberNotify(osnGroupInfo.groupID, delList, state);
                            } else
                                logInfo("del is empty");
                        }
                        break;
                    case "AddMember":
                        groupInfo = SqliteUtils.queryGroup(osnGroupInfo.groupID);
                        if (groupInfo != null) {
                            memberList = SqliteUtils.queryMembers(osnGroupInfo.groupID);
                            List<OsnMemberInfo> addList = getMemberX(osnGroupInfo.userList, memberList, true);
                            if (!addList.isEmpty()) {
                                memberList.clear();
                                for (OsnMemberInfo m : addList) {
                                    if (m.nickName == null) {
                                        UserInfo userInfo = SqliteUtils.queryUser(m.osnID);
                                        if (userInfo == null) {
                                            OsnUserInfo u = osnsdk.getUserInfo(m.osnID, null);
                                            if (u != null) {
                                                userInfo = toClientUser(u);
                                                SqliteUtils.insertUser(userInfo);
                                            }
                                        }
                                        if (userInfo != null)
                                            m.nickName = userInfo.name;
                                    }
                                    memberList.add(toClientMember(m));
                                    logInfo("add members: " + m.osnID);
                                }
                                groupInfo.memberCount += memberList.size();
                                SqliteUtils.updateGroup(groupInfo, Collections.singletonList("memberCount"));
                                SqliteUtils.insertMembers(memberList);
                                onGroupInfoUpdated(Collections.singletonList(groupInfo));
                                onGroupMembersUpdated(osnGroupInfo.groupID, memberList);
                                addMemberNotify(osnGroupInfo.groupID, addList);
                            } else
                                logInfo("add is empty");
                        }
                        break;
                    case "QuitGroup":
                        groupInfo = SqliteUtils.queryGroup(osnGroupInfo.groupID);
                        if (groupInfo != null) {
                            OsnMemberInfo memberInfo = osnGroupInfo.userList.get(0);
                            if (memberInfo.osnID.equalsIgnoreCase(mUserId))
                                delGroup(osnGroupInfo.groupID, "quit");
                            else {
                                SqliteUtils.deleteMembers(osnGroupInfo.userList);
                                memberList = SqliteUtils.queryMembers(osnGroupInfo.groupID);
                                onGroupMembersUpdated(osnGroupInfo.groupID, memberList);
                            }
                            logInfo("quitGroup: " + memberInfo.osnID);
                            delMemberNotify(osnGroupInfo.groupID, Collections.singletonList(memberInfo), state);
                        }
                        break;
                    case "DelGroup":
                        groupInfo = SqliteUtils.queryGroup(osnGroupInfo.groupID);
                        if (groupInfo != null) {
                            delGroup(osnGroupInfo.groupID, "dismiss");
                            delGroupNotify(osnGroupInfo.groupID);
                            logInfo("delGroup: " + osnGroupInfo.groupID);
                        }
                        break;
                    default:
                        logInfo("unknown GroupUpdate state: " + state);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceInfo(List<OsnServiceInfo> infos) {
            List<LitappInfo> infoLists = new ArrayList<>();
            for(OsnServiceInfo info : infos){
                if (info instanceof OsnLitappInfo) {
                    LitappInfo litappInfo = toClientLitapp((OsnLitappInfo) info);
                    infoLists.add(litappInfo);
                }
            }
            onLitappInfoUpdated(infoLists);
        }

    };
    private final RemoteCallbackList<IOnConferenceEventListener> onConferenceEventListenerRemoteCallbackList = new WfcRemoteCallbackList<>();

    private UserInfo toClientUser(OsnUserInfo userInfo) {
        UserInfo u = new UserInfo();
        u.uid = userInfo.userID;
        u.name = userInfo.name;
        u.displayName = userInfo.displayName;
        u.portrait = userInfo.portrait;
        u.urlSpace = userInfo.urlSpace;
        return u;
    }

    private GroupMember toClientMember(OsnMemberInfo memberInfo) {
        GroupMember m = new GroupMember();
        m.groupId = memberInfo.groupID;
        m.memberId = memberInfo.osnID;
        if (memberInfo.type == OsnMemberInfo.MemberType_Normal)
            m.type = GroupMember.GroupMemberType.Normal;
        else if (memberInfo.type == OsnMemberInfo.MemberType_Owner)
            m.type = GroupMember.GroupMemberType.Owner;
        else if (memberInfo.type == OsnMemberInfo.MemberType_Admin)
            m.type = GroupMember.GroupMemberType.Manager;
        else
            m.type = GroupMember.GroupMemberType.Normal;
        m.alias = memberInfo.nickName;
        return m;
    }

    private GroupInfo toClientGroup(OsnGroupInfo groupInfo) {
        GroupInfo g = new GroupInfo();
        g.target = groupInfo.groupID;
        g.name = groupInfo.name;
        g.portrait = groupInfo.portrait;
        g.owner = groupInfo.owner;
        g.type = GroupInfo.GroupType.type(groupInfo.type);
        g.joinType = groupInfo.joinType;
        g.passType = groupInfo.passType;
        g.mute = groupInfo.mute;
        g.memberCount = groupInfo.memberCount;
        return g;
    }

    private LitappInfo toClientLitapp(OsnLitappInfo osnLitappInfo) {
        LitappInfo litappInfo = new LitappInfo();
        litappInfo.target = osnLitappInfo.target;
        litappInfo.name = osnLitappInfo.name;
        litappInfo.displayName = osnLitappInfo.displayName;
        litappInfo.portrait = osnLitappInfo.portrait;
        litappInfo.theme = osnLitappInfo.theme;
        litappInfo.url = osnLitappInfo.url;
        litappInfo.info = "";
        return litappInfo;
    }

    private OsnLitappInfo toSdkLitapp(LitappInfo litappInfo) {
        OsnLitappInfo osnLitappInfo = new OsnLitappInfo();
        osnLitappInfo.target = litappInfo.target;
        osnLitappInfo.name = litappInfo.name;
        osnLitappInfo.displayName = litappInfo.displayName;
        osnLitappInfo.portrait = litappInfo.portrait;
        osnLitappInfo.theme = litappInfo.theme;
        osnLitappInfo.url = litappInfo.url;
        return osnLitappInfo;
    }

    private boolean isChange(GroupInfo g0, GroupInfo g1) {
        return !(g0.owner + g0.name + g0.portrait + g0.memberCount + g0.type).equalsIgnoreCase(g1.owner + g1.name + g1.portrait + g1.memberCount + g1.type);
    }

    private boolean filterMessage(Message msg) {
        boolean filted = false;
        if (keywords != null && msg.content instanceof TextMessageContent) {
            TextMessageContent textMessageContent = (TextMessageContent) msg.content;
            String text = textMessageContent.getContent();
            for (String k : keywords) {
                int index = text.indexOf(k);
                if (index != -1) {
                    text = text.replace(k, "***");
                    textMessageContent.setContent(text);
                    filted = true;
                }
            }
        }
        return filted;
    }

    private List<Message> recvMessage(List<OsnMessageInfo> msgList, boolean isHistory) {
        List<Message> messages = new ArrayList<>();
        try {
            for (OsnMessageInfo messageInfo : msgList) {
                if (SqliteUtils.queryMessage(messageInfo.timeStamp, messageInfo.userID))
                    continue;
                Message message = new Message();
                message.messageId = 0;
                message.sender = messageInfo.userID;
                message.messageHash = messageInfo.hash == null ? "" : messageInfo.hash;
                if (messageInfo.userID.equalsIgnoreCase(mUserId)) {
                    if (messageInfo.target.startsWith("OSNG"))
                        message.conversation = new Conversation(Conversation.ConversationType.Group, messageInfo.target, 0);
                    else if (messageInfo.target.startsWith("OSNU"))
                        message.conversation = new Conversation(Conversation.ConversationType.Single, messageInfo.target, 0);
                    else
                        message.conversation = new Conversation(Conversation.ConversationType.Service, messageInfo.target, 0);
                    message.direction = MessageDirection.Send;
                    message.status = MessageStatus.Sent;
                } else {
                    if (messageInfo.userID.startsWith("OSNG")) {
                        message.sender = messageInfo.originalUser;
                        message.conversation = new Conversation(Conversation.ConversationType.Group, messageInfo.userID, 0);
                    } else if (messageInfo.userID.startsWith("OSNU"))
                        message.conversation = new Conversation(Conversation.ConversationType.Single, messageInfo.userID, 0);
                    else
                        message.conversation = new Conversation(Conversation.ConversationType.Service, messageInfo.userID, 0);
                    message.direction = MessageDirection.Receive;
                    message.status = isHistory ? MessageStatus.Readed : MessageStatus.Unread;
                }
                message.messageUid = 0;
                message.serverTime = messageInfo.timeStamp;
                JSONObject json = JSON.parseObject(messageInfo.content);
                String msgType = json.getString("type");
                if (msgType.equalsIgnoreCase("text")) {
                    TextMessageContent textMessageContent = new TextMessageContent();
                    textMessageContent.setContent(json.getString("data"));
                    message.content = textMessageContent;
                    filterMessage(message);
                } else if (msgType.equalsIgnoreCase("card")) {
                    CardMessageContent cardMessageContent = new CardMessageContent();
                    switch (json.getString("cardType")) {
                        case "user":
                            cardMessageContent.setType(CardType_User);
                            break;
                        case "group":
                            cardMessageContent.setType(CardType_Group);
                            break;
                        case "chatroom":
                            cardMessageContent.setType(CardType_ChatRoom);
                            break;
                        case "channel":
                            cardMessageContent.setType(CardType_Channel);
                            break;
                        case "litapp":
                            cardMessageContent.setType(CardType_Litapp);
                            break;
                        case "share":
                            cardMessageContent.setType(CardType_Share);
                            break;
                    }
                    cardMessageContent.setTarget(json.getString("target"));
                    cardMessageContent.setName(json.getString("name"));
                    cardMessageContent.setDisplayName(json.getString("displayName"));
                    cardMessageContent.setPortrait(json.getString("portrait"));
                    cardMessageContent.setTheme(json.getString("theme"));
                    cardMessageContent.setUrl(json.getString("url"));
                    cardMessageContent.setInfo(json.getString("info"));
                    message.content = cardMessageContent;
                } else {
                    String url = json.getString("url");
                    String name = json.getString("name");
                    String path = getExternalCacheDir().getAbsolutePath() + "/" + name;
                    File file = new File(path);
                    if (file.exists()) {
                        int pos = name.lastIndexOf(".");
                        if (pos < 0)
                            pos = name.length();
                        path = getExternalCacheDir().getAbsolutePath() + "/" + name.substring(0, pos) + "." + System.currentTimeMillis() + name.substring(pos);
                        logInfo("repath: " + path);
                    }
                    if (msgType.equalsIgnoreCase("image") ||
                            msgType.equalsIgnoreCase("sticker") ||
                            msgType.equalsIgnoreCase("video"))
                        osnsdk.downloadData(url, path, null);
                    switch (msgType) {
                        case "file":
                            FileMessageContent fileMessageContent = new FileMessageContent();
                            fileMessageContent.remoteUrl = url;
                            fileMessageContent.localPath = path;
                            fileMessageContent.setName(json.getString("name"));
                            fileMessageContent.setSize(json.getInteger("size"));
                            message.content = fileMessageContent;
                            break;
                        case "image":
                            ImageMessageContent imageMessageContent = new ImageMessageContent();
                            imageMessageContent.remoteUrl = url;
                            imageMessageContent.localPath = path;
                            imageMessageContent.imageHeight = json.getDouble("width");
                            imageMessageContent.imageHeight = json.getDouble("height");
                            message.content = imageMessageContent;
                            break;
                        case "voice":
                            SoundMessageContent soundMessageContent = new SoundMessageContent();
                            soundMessageContent.remoteUrl = url;
                            soundMessageContent.localPath = path;
                            soundMessageContent.setDuration(json.getIntValue("duration"));
                            message.content = soundMessageContent;
                            break;
                        case "video":
                            VideoMessageContent videoMessageContent = new VideoMessageContent();
                            videoMessageContent.remoteUrl = url;
                            videoMessageContent.localPath = path;
                            if (json.containsKey("thumbnail"))
                                videoMessageContent.setThumbnailBytes(Base64.decode(json.getString("thumbnail"), 0));
                            message.content = videoMessageContent;
                            break;
                        case "sticker":
                            StickerMessageContent stickerMessageContent = new StickerMessageContent();
                            stickerMessageContent.remoteUrl = url;
                            stickerMessageContent.localPath = path;
                            stickerMessageContent.width = json.getIntValue("width");
                            stickerMessageContent.height = json.getIntValue("height");
                            message.content = stickerMessageContent;
                            break;
                        default:
                            logInfo("unknown msgType:" + json.getString("type"));
                            break;
                    }
                }
                if (message.content != null) {
                    message.messageId = message.messageUid = SqliteUtils.insertMessage(message);
                    messages.add(message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    private List<GroupMember> updateMember(String groupID, List<OsnMemberInfo> memberList) {
        List<GroupMember> members = new ArrayList<>();
        try {
            if (memberList == null || memberList.isEmpty())
                memberList = osnsdk.getMemberInfo(groupID, null);
            if (memberList == null || memberList.isEmpty())
                return members;

            SqliteUtils.clearMembers(groupID);
            for (OsnMemberInfo m : memberList) {
                if (m.osnID != null) {
                    if (m.nickName == null) {
                        UserInfo userInfo = SqliteUtils.queryUser(m.osnID);
                        if (userInfo == null) {
                            OsnUserInfo u = osnsdk.getUserInfo(m.osnID, null);
                            if (u != null) {
                                userInfo = toClientUser(u);
                                SqliteUtils.insertUser(userInfo);
                            }
                        }
                        if (userInfo != null)
                            m.nickName = userInfo.name;
                    }
                    members.add(toClientMember(m));
                }
                logInfo("memberID: " + m.osnID + ", nickName: " + m.nickName + ", type: " + m.type);
            }
            if (members.size() != 0)
                SqliteUtils.insertMembers(members);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return members;
    }

    private void updateGroup(OsnGroupInfo osnGroupInfo, boolean isUpdateMember) {
        try {
            GroupInfo groupInfo = toClientGroup(osnGroupInfo);
            groupInfo.fav = 1;
            SqliteUtils.insertGroup(groupInfo);
            if (isUpdateMember)
                updateMember(osnGroupInfo.groupID, osnGroupInfo.userList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addGroup(OsnGroupInfo groupInfo) {
        try {
            OsnUtils.logInfo("groupID: " + groupInfo.groupID + ", name: " + groupInfo.name);

            updateGroup(groupInfo, true);
            ConversationInfo conversationInfo = SqliteUtils.queryConversation(Conversation.ConversationType.Group.getValue(), groupInfo.groupID, 0);
            if (conversationInfo == null)
                SqliteUtils.insertConversation(Conversation.ConversationType.Group.getValue(), groupInfo.groupID, 0);

            Message message = new Message();
            message.sender = groupInfo.owner;
            message.direction = MessageDirection.Receive;
            message.status = MessageStatus.Readed;
            message.conversation = new Conversation(Conversation.ConversationType.Group, groupInfo.groupID, 0);
            message.serverTime = System.currentTimeMillis();
            CreateGroupNotificationContent createGroupNotificationContent = new CreateGroupNotificationContent();
            createGroupNotificationContent.groupName = groupInfo.name;
            createGroupNotificationContent.creator = groupInfo.owner;
            message.content = createGroupNotificationContent;
            SqliteUtils.insertMessage(message);

            onReceiveMessage(Collections.singletonList(message), false);
            onGroupInfoUpdated(Collections.singletonList(toClientGroup(groupInfo)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addGroupNull(String groupID) {
        try {
            OsnGroupInfo groupInfo = new OsnGroupInfo();
            groupInfo.groupID = groupID;
            groupInfo.owner = mUserId;
            groupInfo.name = groupID;
            OsnMemberInfo memberInfo = new OsnMemberInfo();
            memberInfo.osnID = mUserId;
            memberInfo.groupID = groupID;
            memberInfo.type = OsnMemberInfo.MemberType_Normal;
            groupInfo.userList.add(memberInfo);
            addGroup(groupInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMemberNotify(String groupID, List<OsnMemberInfo> members) {
        Message message = new Message();
        message.sender = groupID;
        message.direction = MessageDirection.Receive;
        message.status = MessageStatus.Readed;
        message.conversation = new Conversation(Conversation.ConversationType.Group, groupID, 0);
        message.serverTime = System.currentTimeMillis();

        List<String> memberList = new ArrayList<>();
        for (OsnMemberInfo memberInfo : members)
            memberList.add(memberInfo.osnID);
        AddGroupMemberNotificationContent addGroupMemberNotificationContent = new AddGroupMemberNotificationContent();
        addGroupMemberNotificationContent.invitees = memberList;
        addGroupMemberNotificationContent.invitor = groupID;
        message.content = addGroupMemberNotificationContent;
        SqliteUtils.insertMessage(message);
        onReceiveMessage(Collections.singletonList(message), false);
    }

    private void delMemberNotify(String groupID, List<OsnMemberInfo> members, String state) {
        Message message = new Message();
        message.sender = groupID;
        message.direction = MessageDirection.Receive;
        message.status = MessageStatus.Readed;
        message.conversation = new Conversation(Conversation.ConversationType.Group, groupID, 0);
        message.serverTime = System.currentTimeMillis();

        for (OsnMemberInfo memberInfo : members) {
            if (state.equalsIgnoreCase("DelMember")) {
                KickoffGroupMemberNotificationContent kickoffGroupMemberNotificationContent = new KickoffGroupMemberNotificationContent();
                kickoffGroupMemberNotificationContent.operator = groupID;
                kickoffGroupMemberNotificationContent.kickedMembers = new ArrayList<>();
                kickoffGroupMemberNotificationContent.kickedMembers.add(memberInfo.osnID);
                message.content = kickoffGroupMemberNotificationContent;
            } else if (state.equalsIgnoreCase("QuitGroup")) {
                QuitGroupNotificationContent quitGroupNotificationContent = new QuitGroupNotificationContent();
                quitGroupNotificationContent.operator = memberInfo.osnID;
                message.content = quitGroupNotificationContent;
            }
            SqliteUtils.insertMessage(message);
        }
        onReceiveMessage(Collections.singletonList(message), false);
    }

    private void delGroupNotify(String groupID) {
        Message message = new Message();
        message.sender = groupID;
        message.direction = MessageDirection.Receive;
        message.status = MessageStatus.Readed;
        message.conversation = new Conversation(Conversation.ConversationType.Group, groupID, 0);
        message.serverTime = System.currentTimeMillis();
        DismissGroupNotificationContent dismissGroupNotificationContent = new DismissGroupNotificationContent();
        dismissGroupNotificationContent.operator = groupID;
        message.content = dismissGroupNotificationContent;
        SqliteUtils.insertMessage(message);
        onReceiveMessage(Collections.singletonList(message), false);
    }

    private void delGroup(String groupID, String state) {
        SqliteUtils.deleteGroup(groupID);
        SqliteUtils.deleteConversation(Conversation.ConversationType.Group.getValue(), groupID, 0);
        SqliteUtils.clearMembers(groupID);
    }

    private void addFriend(OsnFriendInfo friendInfo) {
        Message message = new Message();
        message.sender = mUserId;
        message.conversation = new Conversation(Conversation.ConversationType.Single, friendInfo.friendID, 0);
        message.direction = MessageDirection.Receive;
        message.status = MessageStatus.Readed;
        message.serverTime = System.currentTimeMillis();

        TextMessageContent textMessageContent = new TextMessageContent();
        FriendRequest request = SqliteUtils.queryFriendRequest(friendInfo.friendID);
        textMessageContent.setContent(request == null ? "你好" : request.reason);
        message.content = textMessageContent;
        SqliteUtils.insertMessage(message);

        message.content = new FriendGreetingMessageContent();
        message.serverTime = System.currentTimeMillis();
        SqliteUtils.insertMessage(message);

        message.content = new FriendAddedMessageContent();
        message.serverTime = System.currentTimeMillis();
        SqliteUtils.insertMessage(message);
        SqliteUtils.insertConversation(Conversation.ConversationType.Single.getValue(), friendInfo.friendID, 0);
    }

    private List<OsnMemberInfo> getMemberX(List<OsnMemberInfo> m0, List<GroupMember> m1, boolean exclude) {
        List<OsnMemberInfo> list = new ArrayList<>();
        for (OsnMemberInfo m : m0) {
            boolean finded = false;
            for (GroupMember o : m1) {
                if (m.osnID.equalsIgnoreCase(o.memberId)) {
                    finded = true;
                    break;
                }
            }
            if (finded) {
                if (!exclude)
                    list.add(m);
            } else {
                if (exclude)
                    list.add(m);
            }
        }
        return list;
    }

    private void initDB(String dbPath) {
        SharedPreferences sp = mContext.getSharedPreferences("ospnConfig", Context.MODE_PRIVATE);
        if (dbPath == null) {
            dbPath = sp.getString("dbPath", null);
        } else {
            sp.edit().putString("dbPath", dbPath).apply();
        }
        if (dbPath != null)
            SqliteUtils.initDB(dbPath);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //this.clientId = intent.getStringExtra("clientId");
        return mBinder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        mContext = this;
        mSp = getSharedPreferences("osnp.config", Context.MODE_PRIVATE);

        try {
            mBinder.registerMessageContent(AddGroupMemberNotificationContent.class.getName());
            mBinder.registerMessageContent(CallStartMessageContent.class.getName());
            mBinder.registerMessageContent(ChangeGroupNameNotificationContent.class.getName());
            mBinder.registerMessageContent(ChangeGroupPortraitNotificationContent.class.getName());
            mBinder.registerMessageContent(CreateGroupNotificationContent.class.getName());
            mBinder.registerMessageContent(DismissGroupNotificationContent.class.getName());
            mBinder.registerMessageContent(FileMessageContent.class.getName());
            mBinder.registerMessageContent(ImageMessageContent.class.getName());
            //mBinder.registerMessageContent(ImageTextMessageContent.class.getName());
            mBinder.registerMessageContent(KickoffGroupMemberNotificationContent.class.getName());
            mBinder.registerMessageContent(LocationMessageContent.class.getName());
            mBinder.registerMessageContent(ModifyGroupAliasNotificationContent.class.getName());
            mBinder.registerMessageContent(QuitGroupNotificationContent.class.getName());
            mBinder.registerMessageContent(RecallMessageContent.class.getName());
            mBinder.registerMessageContent(DeleteMessageContent.class.getName());
            mBinder.registerMessageContent(SoundMessageContent.class.getName());
            mBinder.registerMessageContent(StickerMessageContent.class.getName());
            mBinder.registerMessageContent(TextMessageContent.class.getName());
            mBinder.registerMessageContent(PTextMessageContent.class.getName());
            mBinder.registerMessageContent(TipNotificationContent.class.getName());
            mBinder.registerMessageContent(FriendAddedMessageContent.class.getName());
            mBinder.registerMessageContent(FriendGreetingMessageContent.class.getName());
            mBinder.registerMessageContent(TransferGroupOwnerNotificationContent.class.getName());
            mBinder.registerMessageContent(VideoMessageContent.class.getName());
            mBinder.registerMessageContent(TypingMessageContent.class.getName());
            mBinder.registerMessageContent(GroupMuteNotificationContent.class.getName());
            mBinder.registerMessageContent(GroupJoinTypeNotificationContent.class.getName());
            mBinder.registerMessageContent(GroupPrivateChatNotificationContent.class.getName());
            mBinder.registerMessageContent(GroupSetManagerNotificationContent.class.getName());
            mBinder.registerMessageContent(GroupMuteMemberNotificationContent.class.getName());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        //initDB(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void openXlog() {
        String processName = getApplicationInfo().packageName;

        if (processName == null) {
            return;
        }

        final String SDCARD;
        if (checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            SDCARD = getCacheDir().getAbsolutePath();
        } else {
            SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        final String logPath = SDCARD + "/marscore/log";
        final String logCache = SDCARD + "/marscore/cache";

        String logFileName = processName.indexOf(":") == -1 ? "MarsSample" : ("MarsSample_" + processName.substring(processName.indexOf(":") + 1));

//        if (BuildConfig.DEBUG) {
//            Xlog.appenderOpen(Xlog.LEVEL_VERBOSE, AppednerModeAsync, logCache, logPath, logFileName, "");
//            Xlog.setConsoleLogOpen(true);
//        } else {
//            Xlog.appenderOpen(Xlog.LEVEL_INFO, AppednerModeAsync, logCache, logPath, logFileName, "");
//            Xlog.setConsoleLogOpen(false);
//        }
//        Log.setLogImp(new Xlog());
    }

    public void onConnectionStatusChanged(int status) {
        handler.post(() -> {
            int i = onConnectionStatusChangeListenes.beginBroadcast();
            IOnConnectionStatusChangeListener listener;
            while (i > 0) {
                i--;
                listener = onConnectionStatusChangeListenes.getBroadcastItem(i);
                try {
                    listener.onConnectionStatusChange(status);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onConnectionStatusChangeListenes.finishBroadcast();
        });
    }

    public void onRecallMessage(long messageUid) {
        handler.post(() -> {
            int receiverCount = onReceiveMessageListeners.beginBroadcast();
            IOnReceiveMessageListener listener;
            while (receiverCount > 0) {
                receiverCount--;
                listener = onReceiveMessageListeners.getBroadcastItem(receiverCount);
                try {
                    listener.onRecall(messageUid);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onReceiveMessageListeners.finishBroadcast();
        });
    }

    public void onDeleteMessage(long messageUid) {
        handler.post(() -> {
            int receiverCount = onReceiveMessageListeners.beginBroadcast();
            IOnReceiveMessageListener listener;
            while (receiverCount > 0) {
                receiverCount--;
                listener = onReceiveMessageListeners.getBroadcastItem(receiverCount);
                try {
                    listener.onDelete(messageUid);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onReceiveMessageListeners.finishBroadcast();
        });
    }

    public void onUserReceivedMessage(Map<String, Long> map) {
        handler.post(() -> {
            int receiverCount = onReceiveMessageListeners.beginBroadcast();
            IOnReceiveMessageListener listener;
            while (receiverCount > 0) {
                receiverCount--;
                listener = onReceiveMessageListeners.getBroadcastItem(receiverCount);
                try {
                    listener.onDelivered(map);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onReceiveMessageListeners.finishBroadcast();
        });
    }

    public void onUserReadedMessage(List<ReadEntry> list) {
        handler.post(() -> {
            List<ReadEntry> l = new ArrayList<>();
//            for (ProtoReadEntry entry : list) {
//                ReadEntry r = new ReadEntry();
//                r.conversation = new Conversation(Conversation.ConversationType.type(entry.conversationType), entry.target, entry.line);
//                r.userId = entry.userId;
//                r.readDt = entry.readDt;
//                l.add(r);
//            }

            int receiverCount = onReceiveMessageListeners.beginBroadcast();
            IOnReceiveMessageListener listener;
            while (receiverCount > 0) {
                receiverCount--;
                listener = onReceiveMessageListeners.getBroadcastItem(receiverCount);
                try {
                    listener.onReaded(l);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onReceiveMessageListeners.finishBroadcast();
        });
    }

    private void onReceiveMessageInternal(List<Message> messages) {
        int receiverCount = onReceiveMessageListeners.beginBroadcast();
        IOnReceiveMessageListener listener;
        while (receiverCount > 0) {
            receiverCount--;
            listener = onReceiveMessageListeners.getBroadcastItem(receiverCount);
            try {
//                SafeIPCMessageEntry entry;
//                int startIndex = 0;
//                do {
//                    entry = buildSafeIPCMessages(protoMessages, startIndex, false);
//                    listener.onReceive(entry.messages, entry.messages.size() > 0 && startIndex != protoMessages.length - 1);
//                    startIndex = entry.index + 1;
//                } while (entry.index > 0 && entry.index < protoMessages.length - 1);
                listener.onReceive(messages, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        onReceiveMessageListeners.finishBroadcast();
    }

    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        if (mConnectionStatus == ConnectionStatusReceiveing && hasMore) {
            return;
        }
        if (messages.isEmpty()) {
            return;
        }
        handler.post(() -> onReceiveMessageInternal(messages));
    }

    public void onFriendListUpdated(String[] friendList) {
        handler.post(() -> {
            int i = onFriendUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnFriendUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onFriendUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onFriendListUpdated(Arrays.asList(friendList));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onFriendUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    public void onFriendRequestUpdated(String[] newRequestList) {
        handler.post(() -> {
            int i = onFriendUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnFriendUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onFriendUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onFriendRequestUpdated(Arrays.asList(newRequestList));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onFriendUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    public void onGroupInfoUpdated(List<GroupInfo> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        handler.post(() -> {
            int i = onGroupInfoUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnGroupInfoUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onGroupInfoUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onGroupInfoUpdated(groups);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onGroupInfoUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    public void onLitappInfoUpdated(List<LitappInfo> litappInfos) {
        if (litappInfos == null || litappInfos.isEmpty()) {
            return;
        }
        handler.post(() -> {
            int i = onLitappInfoUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnLitappInfoUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onLitappInfoUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onLitappInfoUpdated(litappInfos);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onLitappInfoUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    public void onGroupMembersUpdated(String groupId, List<GroupMember> members) {
        handler.post(() -> {
            int i = onGroupMembersUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnGroupMembersUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onGroupMembersUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onGroupMembersUpdated(groupId, members);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onGroupMembersUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    // 参数里面直接带上scope, key, value
    public void onSettingUpdated() {
        handler.post(() -> {
            int i = onSettingUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnSettingUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onSettingUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onSettingUpdated();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onSettingUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    public void onUserInfoUpdated(List<UserInfo> users) {
        handler.post(() -> {
            int i = onUserInfoUpdateListenerRemoteCallbackList.beginBroadcast();
            IOnUserInfoUpdateListener listener;
            while (i > 0) {
                i--;
                listener = onUserInfoUpdateListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onUserInfoUpdated(users);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onUserInfoUpdateListenerRemoteCallbackList.finishBroadcast();
        });
    }

    public void onConferenceEvent(String s) {
        handler.post(() -> {
            int i = onConferenceEventListenerRemoteCallbackList.beginBroadcast();
            IOnConferenceEventListener listener;
            while (i > 0) {
                i--;
                listener = onConferenceEventListenerRemoteCallbackList.getBroadcastItem(i);
                try {
                    listener.onConferenceEvent(s);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            onConferenceEventListenerRemoteCallbackList.finishBroadcast();
        });
    }

    private static class SafeIPCMessageEntry {
        List<Message> messages;
        int index;
        public SafeIPCMessageEntry() {
            messages = new ArrayList<>();
            index = 0;
        }
    }

    private class ClientServiceStub extends IRemoteClient.Stub {
        @Override
        public void litappLogin(LitappInfo litappInfo, String url, IGeneralCallback callback) {
            osnsdk.lpLogin(toSdkLitapp(litappInfo), url, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        logInfo(error);
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void register(String userName, String password, String serviceID, IGeneralCallback callback) throws RemoteException {
            osnsdk.initSDK(mHost, mListener);
//            osnsdk.register(userName, password, serviceID, new OSNGeneralCallback() {
//                @Override
//                public void onSuccess(String json) {
//                    try {
//                        callback.onSuccess();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void onFailure(String error) {
//                    try {
//                        callback.onFailure(-1);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
        }

        @Override
        public boolean connect(String userName, String userPwd) throws RemoteException {
            if (!isInitSdk) {
                isInitSdk = true;
                osnsdk.initSDK(mHost, mListener);
                if(userName == null){
                    return true;
                }
            }

            onConnectionStatusChanged(ConnectionStatusConnecting);
            if (keywords == null) {
                new Thread(() -> {
                    String filterUrl = "http://" + mHost + ":8300/keywordFilter";
                    String data = HttpUtils.doGet(filterUrl);
                    if (data != null) {
                        logInfo("keywords: " + data);
                        JSONObject json = JSON.parseObject(data);
                        JSONArray keywordList = json.getJSONArray("keywords");
                        keywords = keywordList.toJavaList(String.class);
                    }
                }).start();
            }
            JSONObject json = JSON.parseObject(userPwd);
            logined = osnsdk.loginWithShare(json);
            if (logined) {
                mUserId = osnsdk.getUserID();
                String sdcard = getFilesDir().getAbsolutePath();
                String dbPath = sdcard + "/" + mUserId + ".db";
                initDB(dbPath);
            }
            return logined;
        }

        @Override
        public void setOnReceiveMessageListener(IOnReceiveMessageListener listener) throws RemoteException {
            onReceiveMessageListeners.register(listener);
        }

        @Override
        public void setOnConnectionStatusChangeListener(IOnConnectionStatusChangeListener listener) throws RemoteException {
            onConnectionStatusChangeListenes.register(listener);
        }


        @Override
        public void setOnUserInfoUpdateListener(IOnUserInfoUpdateListener listener) throws RemoteException {
            onUserInfoUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnGroupInfoUpdateListener(IOnGroupInfoUpdateListener listener) throws RemoteException {
            onGroupInfoUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnFriendUpdateListener(IOnFriendUpdateListener listener) throws RemoteException {
            onFriendUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnSettingUpdateListener(IOnSettingUpdateListener listener) throws RemoteException {
            onSettingUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnChannelInfoUpdateListener(IOnChannelInfoUpdateListener listener) throws RemoteException {
            onChannelInfoUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnConferenceEventListener(IOnConferenceEventListener listener) throws RemoteException {
            onConferenceEventListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnLitappInfoUpdateListener(IOnLitappInfoUpdateListener listener) throws RemoteException {
            onLitappInfoUpdateListenerRemoteCallbackList.register(listener);
        }

        @Override
        public void setOnGroupMembersUpdateListener(IOnGroupMembersUpdateListener listener) throws RemoteException {
            onGroupMembersUpdateListenerRemoteCallbackList.register(listener);
        }


        @Override
        public void disconnect(boolean disablePush, boolean clearSession) throws RemoteException {
            onConnectionStatusChanged(ConnectionStatusLogout);
            mSp.edit().clear().apply();

            logined = false;
            mUserId = null;

            osnsdk.logout(null);
            SqliteUtils.closeDB();
        }

        @Override
        public void setForeground(int isForeground) throws RemoteException {
//            BaseEvent.onForeground(isForeground == 1);
        }

        @Override
        public void onNetworkChange() {
            //BaseEvent.onNetworkChange();
        }

        @Override
        public void setServerAddress(String host) throws RemoteException {
            mHost = host;
        }

        @Override
        public void registerMessageContent(String msgContentCls) throws RemoteException {
            try {
                Class cls = Class.forName(msgContentCls);
                Constructor c = cls.getConstructor();
                if (c.getModifiers() != Modifier.PUBLIC) {
                    throw new IllegalArgumentException("the default constructor of your custom messageContent class should be public");
                }
                ContentTag tag = (ContentTag) cls.getAnnotation(ContentTag.class);
                if (tag != null) {
                    Class curClazz = contentMapper.get(tag.type());
                    if (curClazz != null && !curClazz.equals(cls)) {
                        throw new IllegalArgumentException("messageContent type duplicate " + msgContentCls);
                    }
                    contentMapper.put(tag.type(), cls);
//                    try {
//                        ProtoLogic.registerMessageFlag(tag.type(), tag.flag().getValue());
//                    } catch (Throwable e) {
//                        // ref to: https://github.com/Tencent/mars/issues/334
//                        ProtoLogic.registerMessageFlag(tag.type(), tag.flag().getValue());
//                    }
                } else {
                    throw new IllegalStateException("ContentTag annotation must be set!");
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("custom messageContent class can not found: " + msgContentCls);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("custom messageContent class must have a default constructor");
            }
        }

        private void sendmsg(Message msg, final ISendMessageCallback callback, int expireDuration, long mid, long timestamp) throws RemoteException {
            try {
                msg.sender = mUserId;
                ConversationInfo conversationInfo = SqliteUtils.queryConversation(msg.conversation.type.getValue(), msg.conversation.target, msg.conversation.line);
                if (conversationInfo == null)
                    logInfo("conversationInfo == null");
                else {
                    conversationInfo.timestamp = msg.serverTime;
                }

                JSONObject json = new JSONObject();
                int msgType = msg.content.getMessageContentType();
                if (msgType == MessageContentType.ContentType_Text ||
                        msgType == MessageContentType.ContentType_Card) {
                    if (msgType == MessageContentType.ContentType_Text) {
                        TextMessageContent textMessageContent = (TextMessageContent) msg.content;
                        json.put("type", "text");
                        json.put("data", textMessageContent.getContent());
                    } else if (msgType == MessageContentType.ContentType_Card) {
                        CardMessageContent cardMessageContent = (CardMessageContent) msg.content;
                        json.put("type", "card");
                        switch (cardMessageContent.getType()) {
                            case CardType_User:
                                json.put("cardType", "user");
                                break;
                            case CardType_Group:
                                json.put("cardType", "group");
                                break;
                            case CardType_ChatRoom:
                                json.put("cardType", "chatroom");
                                break;
                            case CardType_Channel:
                                json.put("cardType", "channel");
                                break;
                            case CardType_Litapp:
                                json.put("cardType", "litapp");
                                break;
                            case CardType_Share:
                                json.put("cardType", "share");
                                break;
                        }
                        json.put("target", cardMessageContent.getTarget());
                        json.put("name", cardMessageContent.getName());
                        json.put("displayName", cardMessageContent.getDisplayName());
                        json.put("portrait", cardMessageContent.getPortrait());
                        json.put("theme", cardMessageContent.getTheme());
                        json.put("url", cardMessageContent.getUrl());
                        json.put("info", cardMessageContent.getInfo());
                    }
                    osnsdk.sendMessage(json.toString(), msg.conversation.target, new OSNGeneralCallback() {
                        @Override
                        public void onSuccess(String json) {
                            try {
                                JSONObject result = JSON.parseObject(json);
                                SqliteUtils.updateMessage(mid, MessageStatus.Sent.value(), result.getString("msgHash"));
                                if (conversationInfo != null)
                                    SqliteUtils.updateConversation(conversationInfo, Collections.singletonList("timestamp"));
                                callback.onSuccess(mid, timestamp);
                            } catch (Exception e) {
                                logInfo(e.toString());
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            try {
                                SqliteUtils.updateMessage(mid, MessageStatus.Send_Failure.value());
                                if (conversationInfo != null)
                                    SqliteUtils.updateConversation(conversationInfo, Collections.singletonList("timestamp"));
                                callback.onFailure(-1);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    MediaMessageContent mediaMessageContent = (MediaMessageContent) msg.content;
                    int idx = mediaMessageContent.localPath.lastIndexOf('/');
                    String name = idx == 0 || idx == mediaMessageContent.localPath.length() - 1
                            ? mediaMessageContent.localPath
                            : mediaMessageContent.localPath.substring(idx + 1);
                    switch (msgType) {
                        case MessageContentType.ContentType_File:
                            FileMessageContent fileMessageContent = (FileMessageContent) mediaMessageContent;
                            json.put("type", "file");
                            json.put("name", fileMessageContent.getName());
                            json.put("size", fileMessageContent.getSize());
                            break;
                        case MessageContentType.ContentType_Image:
                            ImageMessageContent imageMessageContent = (ImageMessageContent) mediaMessageContent;
                            json.put("type", "image");
                            json.put("name", name);
                            json.put("width", imageMessageContent.getImageWidth());
                            json.put("height", imageMessageContent.getImageHeight());
                            break;
                        case MessageContentType.ContentType_Video:
                            VideoMessageContent videoMessageContent = (VideoMessageContent) mediaMessageContent;
                            //小视频自动下载，不需要缩略图
                            //byte[] thumbnail = videoMessageContent.getThumbnailBytes();
                            json.put("type", "video");
                            json.put("name", name);
                            //if(thumbnail != null)
                            //    json.put("thumbnail", Base64.encodeToString(thumbnail,Base64.NO_WRAP));
                            break;
                        case MessageContentType.ContentType_Voice:
                            SoundMessageContent soundMessageContent = (SoundMessageContent) mediaMessageContent;
                            json.put("type", "voice");
                            json.put("name", name);
                            json.put("duration", soundMessageContent.getDuration());
                            break;
                        case MessageContentType.ContentType_Sticker:
                            StickerMessageContent stickerMessageContent = (StickerMessageContent) mediaMessageContent;
                            json.put("type", "sticker");
                            json.put("name", name);
                            json.put("width", stickerMessageContent.width);
                            json.put("height", stickerMessageContent.height);
                            break;
                        default:
                            logInfo("unknown type: " + msg.content.getMessageContentType());
                            return;
                    }
                    FileInputStream fileInputStream = new FileInputStream(new File(mediaMessageContent.localPath));
                    byte[] fileData = new byte[fileInputStream.available()];
                    fileInputStream.read(fileData);
                    fileInputStream.close();

                    osnsdk.uploadData(mediaMessageContent.localPath, "cache", fileData, new OSNTransferCallback() {
                        @Override
                        public void onSuccess(String data) {
                            try {
                                JSONObject ulJson = JSON.parseObject(data);
                                String remoteUrl = ulJson.getString("url");
                                callback.onMediaUploaded(remoteUrl);
                                json.put("url", remoteUrl);
                                logInfo("url: " + ulJson.toString());

                                osnsdk.sendMessage(json.toString(), msg.conversation.target, new OSNGeneralCallback() {
                                    @Override
                                    public void onSuccess(String json) {
                                        try {
                                            JSONObject result = JSON.parseObject(json);
                                            SqliteUtils.updateMessage(mid, MessageStatus.Sent.value(), result.getString("msgHash"));
                                            if (conversationInfo != null)
                                                SqliteUtils.updateConversation(conversationInfo, Collections.singletonList("timestamp"));
                                            callback.onSuccess(mid, timestamp);
                                        } catch (Exception e) {
                                            logInfo(e.toString());
                                        }
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        try {
                                            SqliteUtils.updateMessage(mid, MessageStatus.Send_Failure.value());
                                            if (conversationInfo != null)
                                                SqliteUtils.updateConversation(conversationInfo, Collections.singletonList("timestamp"));
                                            callback.onFailure(-1);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                return;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            SqliteUtils.updateMessage(mid, MessageStatus.Send_Failure.value());
                            if (conversationInfo != null)
                                SqliteUtils.updateConversation(conversationInfo, Collections.singletonList("timestamp"));
                            try {
                                callback.onFailure(-1);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onProgress(long progress, long total) {
                            try {
                                callback.onProgress(progress, total);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            try {
                                SqliteUtils.updateMessage(mid, MessageStatus.Send_Failure.value());
                                if (conversationInfo != null)
                                    SqliteUtils.updateConversation(conversationInfo, Collections.singletonList("timestamp"));
                                callback.onFailure(-1);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null)
                    callback.onFailure(-1);
            }
        }

        @Override
        public void sendSavedMessage(Message msg, int expireDuration, ISendMessageCallback callback) throws RemoteException {
            logInfo("");
            throw new RemoteException();
        }

        @Override
        public void send(Message msg, final ISendMessageCallback callback, int expireDuration) throws RemoteException {
            logInfo("");
            if (msg.content instanceof TypingMessageContent) {
                callback.onPrepared(0, msg.serverTime);
                callback.onSuccess(0, msg.serverTime);
                return;
            }
            if (msg.conversation.target.startsWith("OSNS")) {
                callback.onFailure(-1);
                return;
            }
            if (SqliteUtils.queryConversation(msg.conversation.type.getValue(), msg.conversation.target, msg.conversation.line) == null)
                SqliteUtils.insertConversation(msg.conversation.type.getValue(), msg.conversation.target, msg.conversation.line);
            long mid = SqliteUtils.insertMessage(msg);
            msg.messageId = msg.messageUid = mid;
            callback.onPrepared(mid, msg.serverTime);
            sendmsg(msg, callback, expireDuration, mid, msg.serverTime);
        }
        public void broadcast(String json, final IGeneralCallback callback) throws RemoteException {
            logInfo(json);
            osnsdk.sendBroadcast(json, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        logInfo(e.toString());
                    }
                }
                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void recall(long messageUid, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            Message message = SqliteUtils.queryMessage(messageUid);
            if(message == null){
                callback.onFailure(-1);
                return;
            }
            long timestamp = System.currentTimeMillis();
            if(message.sender.equalsIgnoreCase(mUserId) &&
                    timestamp > message.serverTime &&
                    (timestamp - message.serverTime) < 5*60*1000){
                osnsdk.deleteMessage(message.messageHash, message.conversation.target, null);
            }
            SqliteUtils.deleteMessage(messageUid);
            callback.onSuccess();
        }

        @Override
        public long getServerDeltaTime() throws RemoteException {
            return 0;
        }

        @Override
        public List<ConversationInfo> getConversationList(int[] conversationTypes, int[] lines) throws RemoteException {
            List<ConversationInfo> out = SqliteUtils.listAllConversations(conversationTypes, lines);
            for (ConversationInfo conversationInfo : out) {
                //notify only one item
                if (conversationInfo.conversation.type == Conversation.ConversationType.Notify) {
                    conversationInfo.lastMessage = SqliteUtils.getLastNotify();
                    break;
                }
            }

            List<ConversationInfo> outList = new ArrayList<>();
            for (ConversationInfo conversationInfo : out) {
                if (conversationInfo.unreadCount.hasUnread())
                    outList.add(conversationInfo);
            }
            for (ConversationInfo conversationInfo : out) {
                if (!conversationInfo.unreadCount.hasUnread())
                    outList.add(conversationInfo);
            }
            logInfo("size: " + outList.size());
            return outList;
        }

        @Override
        public ConversationInfo getConversation(int conversationType, String target, int line) throws RemoteException {
            logInfo("");
            return SqliteUtils.queryConversation(conversationType, target, line);
        }

        @Override
        public long getFirstUnreadMessageId(int conversationType, String target, int line) throws RemoteException {
            logInfo("");
            return 0;
        }

        @Override
        public List<Message> getMessages(Conversation conversation, long fromIndex, boolean before, int count, String withUser) throws RemoteException {
            logInfo("fromIndex: " + fromIndex + ", before: " + before + ", count: " + count + ", withUser: " + withUser);
            Message msg = fromIndex == 0 ? SqliteUtils.getLastMessage(conversation) : SqliteUtils.queryMessage(fromIndex);
            if (msg == null)
                return new ArrayList<>();
            return SqliteUtils.queryMessages(conversation, msg.serverTime, before, count, fromIndex == 0);
        }

        @Override
        public List<Message> getMessagesEx(int[] conversationTypes, int[] lines, int[] contentTypes, long fromIndex, boolean before, int count, String withUser) throws RemoteException {
            logInfo("");
            //            ProtoMessage[] protoMessages = ProtoLogic.getMessagesEx(conversationTypes, lines, contentTypes, fromIndex, before, count, withUser);
//            SafeIPCMessageEntry entry = buildSafeIPCMessages(protoMessages, 0, before);
//            if (entry.messages.size() != protoMessages.length) {
//                android.util.Log.e(TAG, "getMessagesEx, drop messages " + (protoMessages.length - entry.messages.size()));
//            }
//            return entry.messages;
            return null;
        }

        @Override
        public List<Message> getMessagesEx2(int[] conversationTypes, int[] lines, int[] messageStatus, long fromIndex, boolean before, int count, String withUser) throws RemoteException {
            logInfo("");
            //            ProtoMessage[] protoMessages = ProtoLogic.getMessagesEx2(conversationTypes, lines, messageStatus, fromIndex, before, count, withUser);
//            SafeIPCMessageEntry entry = buildSafeIPCMessages(protoMessages, 0, before);
//            if (entry.messages.size() != protoMessages.length) {
//                android.util.Log.e(TAG, "getMessagesEx2, drop messages " + (protoMessages.length - entry.messages.size()));
//            }
//            return entry.messages;
            return null;
        }

        @Override
        public void getMessagesInTypesAsync(Conversation conversation, int[] contentTypes, long fromIndex, boolean before, int count, String withUser, IGetMessageCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getMessagesInStatusAsync(Conversation conversation, int[] messageStatus, long fromIndex, boolean before, int count, String withUser, IGetMessageCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getMessagesAsync(Conversation conversation, long fromIndex, boolean before, int count, String withUser, IGetMessageCallback callback) throws RemoteException {
            logInfo("");
            List<Message> messageList = getMessages(conversation, fromIndex, before, count, withUser);
            callback.onSuccess(messageList, messageList.size() == count);
        }

        @Override
        public void getMessagesExAsync(int[] conversationTypes, int[] lines, int[] contentTypes, long fromIndex, boolean before, int count, String withUser, IGetMessageCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getMessagesEx2Async(int[] conversationTypes, int[] lines, int[] messageStatus, long fromIndex, boolean before, int count, String withUser, IGetMessageCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getUserMessages(String userId, Conversation conversation, long fromIndex, boolean before, int count, IGetMessageCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getUserMessagesEx(String userId, int[] conversationTypes, int[] lines, int[] contentTypes, long fromIndex, boolean before, int count, IGetMessageCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getRemoteMessages(Conversation conversation, long beforeMessageUid, int count, IGetRemoteMessageCallback callback) throws RemoteException {
            logInfo("beforeMID: " + beforeMessageUid + ", count: " + count);
            Message message = SqliteUtils.queryMessage(beforeMessageUid);
            long timestamp = message == null ? System.currentTimeMillis() : message.serverTime;
            osnsdk.loadMessage(conversation.target, timestamp, count, true, new OSNGeneralCallbackT<List<OsnMessageInfo>>() {
                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onSuccess(List<OsnMessageInfo> osnMessageInfos) {
                    List<Message> messages = recvMessage(osnMessageInfos, true);
                    try {
                        callback.onSuccess(messages);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void getConversationFileRecords(Conversation conversation, String fromUser, long beforeMessageUid, int count, IGetFileRecordCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getMyFileRecords(long beforeMessageUid, int count, IGetFileRecordCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void deleteFileRecord(long messageUid, IGeneralCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void searchFileRecords(String keyword, Conversation conversation, String fromUser, long beforeMessageUid, int count, IGetFileRecordCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void searchMyFileRecords(String keyword, long beforeMessageUid, int count, IGetFileRecordCallback callback) throws RemoteException {
            logInfo("");
        }


        @Override
        public Message getMessage(long messageId) throws RemoteException {
            logInfo("messageId: " + messageId);
            return SqliteUtils.queryMessage(messageId);
        }

        @Override
        public Message getMessageByUid(long messageUid) throws RemoteException {
            logInfo("");
            return null;
        }

        @Override
        public Message insertMessage(Message message, boolean notify) throws RemoteException {
            logInfo("");
            return null;
        }

        @Override
        public boolean updateMessageContent(Message message) throws RemoteException {
            logInfo("");
            return false;
        }

        @Override
        public boolean updateMessageStatus(long messageId, int messageStatus) throws RemoteException {
            logInfo("");
            return true;
        }

        @Override
        public UnreadCount getUnreadCount(int conversationType, String target, int line) throws RemoteException {
            logInfo("");
            ConversationInfo conversationInfo = SqliteUtils.queryConversation(conversationType, target, line);
            if (conversationInfo != null)
                return conversationInfo.unreadCount;
            return new UnreadCount();
        }

        @Override
        public UnreadCount getUnreadCountEx(int[] conversationTypes, int[] lines) throws RemoteException {
            logInfo("");
            return null;
        }

        @Override
        public boolean clearUnreadStatus(int conversationType, String target, int line) throws RemoteException {
            logInfo("");
            SqliteUtils.clearConversationUnread(conversationType, target, line);
            return true;
        }

        @Override
        public boolean clearUnreadStatusEx(int[] conversationTypes, int[] lines) throws RemoteException {
            logInfo("");
            return false;
        }

        @Override
        public void clearAllUnreadStatus() throws RemoteException {
            logInfo("");
        }

        @Override
        public void clearMessages(int conversationType, String target, int line) throws RemoteException {
            logInfo("");
            SqliteUtils.clearMessage(target);
        }

        @Override
        public void clearMessagesEx(int conversationType, String target, int line, long before) throws RemoteException {
            logInfo("");
        }

        @Override
        public void setMediaMessagePlayed(long messageId) {
            logInfo("mid: " + messageId);
            try {
                Message message = getMessage(messageId);
                if (message != null)
                    SqliteUtils.updateMessage(messageId, MessageStatus.Played.value());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void clearConversation(boolean clearMsg) throws RemoteException {
            logInfo("clearMsg: "+clearMsg);
            SqliteUtils.clearConversation();
        }
        @Override
        public void removeConversation(int conversationType, String target, int line, boolean clearMsg) throws RemoteException {
            logInfo("type: " + conversationType + ", target: " + target + ", line: " + line);
            SqliteUtils.deleteConversation(conversationType, target, line);
        }

        @Override
        public void setConversationTop(int conversationType, String target, int line, boolean top, IGeneralCallback callback) throws RemoteException {
            logInfo("");
            ConversationInfo conversationInfo = SqliteUtils.queryConversation(conversationType, target, line);
            if (conversationInfo != null) {
                conversationInfo.isTop = top;
                SqliteUtils.updateConversation(conversationInfo, Collections.singletonList("top"));
            }
        }

        @Override
        public void setConversationDraft(int conversationType, String target, int line, String draft) throws RemoteException {
            logInfo("");
            ConversationInfo conversationInfo = SqliteUtils.queryConversation(conversationType, target, line);
            if (conversationInfo == null) {
                SqliteUtils.insertConversation(conversationType, target, line);
                conversationInfo = SqliteUtils.queryConversation(conversationType, target, line);
            }
            if (conversationInfo != null) {
                conversationInfo.draft = draft;
                SqliteUtils.updateConversation(conversationInfo, Collections.singletonList("draft"));
            }
        }

        @Override
        public void setConversationSilent(int conversationType, String target, int line, boolean silent, IGeneralCallback callback) throws RemoteException {
            logInfo("");
            ConversationInfo conversationInfo = SqliteUtils.queryConversation(conversationType, target, line);
            if (conversationInfo != null) {
                conversationInfo.isSilent = silent;
                SqliteUtils.updateConversation(conversationInfo, Collections.singletonList("silent"));
            }
        }

        @Override
        public Map getConversationRead(int conversationType, String target, int line) throws RemoteException {
            logInfo("");
//            ConversationInfo conversationInfo = SqliteUtils.queryConversation(conversationType,target,line);
//            if(conversationInfo == null && target.startsWith("OSNU"))
//                SqliteUtils.insertConversation(conversationType,target,line);
            return new HashMap();
        }

        @Override
        public Map getMessageDelivery(int conversationType, String target) throws RemoteException {
            logInfo("");
            return null;
        }

        @Override
        public void setConversationTimestamp(int conversationType, String target, int line, long timestamp) throws RemoteException {
            logInfo("");
            ConversationInfo conversationInfo = SqliteUtils.queryConversation(conversationType, target, line);
            if (conversationInfo != null) {
                conversationInfo.timestamp = timestamp;
                SqliteUtils.updateConversation(conversationInfo, Collections.singletonList("timestamp"));
            }
        }

        @Override
        public void searchUser(String keyword, int searchType, int page, final ISearchUserCallback callback) throws RemoteException {
            logInfo("");
            List<UserInfo> userInfos = SqliteUtils.queryUsers(keyword);
            callback.onSuccess(userInfos);
        }

        @Override
        public boolean isMyFriend(String userId) throws RemoteException {
            logInfo("");
            return SqliteUtils.queryFriend(userId) != null;
        }

        @Override
        public List<String> getMyFriendList(boolean refresh) throws RemoteException {
            logInfo("refresh: " + refresh);
            return SqliteUtils.listFriends();
        }

        @Override
        public boolean isBlackListed(String userId) throws RemoteException {
            OsnFriendInfo friendInfo = SqliteUtils.queryFriend(userId);
            if (friendInfo == null) {
                logInfo("no my friend: " + userId);
                return true;
            }
            logInfo(friendInfo.state == OsnFriendInfo.Blacked ? "true" : "false" + friendInfo.state);
            return friendInfo.state == OsnFriendInfo.Blacked;
        }

        @Override
        public List<String> getBlackList(boolean refresh) throws RemoteException {
            logInfo("");
            return SqliteUtils.listFriends(OsnFriendInfo.Blacked);
        }

        @Override
        public List<UserInfo> getMyFriendListInfo(boolean refresh) throws RemoteException {
            logInfo("");
            List<String> users = getMyFriendList(refresh);
            if (users == null)
                return null;
            List<UserInfo> userInfos = new ArrayList<>();
            UserInfo userInfo;
            for (String user : users) {
                userInfo = getUserInfo(user, null, false);
                if (userInfo == null)
                    userInfo = new UserInfo(user);
                userInfos.add(userInfo);
            }
            return userInfos;
        }

        @Override
        public void loadFriendRequestFromRemote() throws RemoteException {
            logInfo("");
        }

        @Override
        public String getUserSetting(int scope, String key) throws RemoteException {
            try {
                String value = null;
                if (scope == UserSettingScope.FavoriteGroup) {
                    GroupInfo groupInfo = SqliteUtils.queryGroup(key);
                    value = String.valueOf(groupInfo.fav);
                } else if (scope == UserSettingScope.GroupHideNickname) {
                    GroupInfo groupInfo = SqliteUtils.queryGroup(key);
                    value = String.valueOf(groupInfo.showAlias);
                }
                logInfo("scope: " + scope + ", key: " + key + ", value: " + value);
                return value;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public Map<String, String> getUserSettings(int scope) throws RemoteException {
            try {
                logInfo("scope: " + scope);
                Map<String, String> uMap = new HashMap<>();
                if (scope == UserSettingScope.FavoriteGroup) {
                    List<GroupInfo> groupInfoList = SqliteUtils.listGroups();
                    for (GroupInfo g : groupInfoList)
                        uMap.put(g.target, String.valueOf(g.fav));
                } else if (scope == UserSettingScope.GroupHideNickname) {
                    List<GroupInfo> groupInfoList = SqliteUtils.listGroups();
                    for (GroupInfo g : groupInfoList)
                        uMap.put(g.target, String.valueOf(g.showAlias));
                }
                for (String k : uMap.keySet()) {
                    logInfo("key: " + k + ", value: " + uMap.get(k));
                }
                return uMap;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void setUserSetting(int scope, String key, String value, final IGeneralCallback callback) throws RemoteException {
            try {
                logInfo("scope: " + scope + ", key: " + key + ", value: " + value);
                if (scope == UserSettingScope.FavoriteGroup) {
                    GroupInfo groupInfo = SqliteUtils.queryGroup(key);
                    groupInfo.fav = Integer.parseInt(value);
                    SqliteUtils.updateGroup(groupInfo, Collections.singletonList("fav"));
                    if (callback != null)
                        callback.onSuccess();
                } else if (scope == UserSettingScope.GroupHideNickname) {
                    GroupInfo groupInfo = SqliteUtils.queryGroup(key);
                    groupInfo.showAlias = Integer.parseInt(value);
                    SqliteUtils.updateGroup(groupInfo, Collections.singletonList("showAlias"));
                    if (callback != null)
                        callback.onSuccess();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String getLogPath() {
            logInfo("");
            return getCacheDir().getAbsolutePath() + "/log";
        }

        @Override
        public void startLog() throws RemoteException {
            logInfo("");
        }

        @Override
        public void stopLog() throws RemoteException {
            logInfo("");
        }

        @Override
        public void setDeviceToken(String token, int pushType) throws RemoteException {
            logInfo("");
            if (TextUtils.isEmpty(token)) {
                return;
            }
            mBackupDeviceToken = token;
        }

        @Override
        public List<FriendRequest> getFriendRequest(boolean incomming) throws RemoteException {
            logInfo("");
            return SqliteUtils.listFriendRequest();
        }

        @Override
        public FriendRequest getOneFriendRequest(String userId, boolean incomming) throws RemoteException {
            logInfo("");
            return null;
        }

        @Override
        public String getFriendAlias(String userId) throws RemoteException {
            logInfo("");
            OsnFriendInfo friendInfo = SqliteUtils.queryFriend(userId);
            return friendInfo == null ? null : friendInfo.remarks;
        }

        @Override
        public String getFriendExtra(String userId) throws RemoteException {
            logInfo("");
            return null;
        }

        @Override
        public void setFriendAlias(String userId, String alias, IGeneralCallback callback) throws RemoteException {
            logInfo("");
            OsnFriendInfo friendInfo = new OsnFriendInfo();
            friendInfo.friendID = userId;
            friendInfo.remarks = alias;
            osnsdk.modifyFriendInfo(Collections.singletonList("remarks"), friendInfo, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    if (callback != null) {
                        try {
                            friendInfo.remarks = alias;
                            SqliteUtils.updateFriend(friendInfo, Collections.singletonList("remarks"));
                            callback.onSuccess();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (callback != null) {
                        try {
                            callback.onFailure(-1);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        @Override
        public void clearUnreadFriendRequestStatus() throws RemoteException {
            logInfo("");
            List<FriendRequest> requestList = SqliteUtils.queryUnreadFriendRequest();
            if (requestList == null)
                return;
            for (FriendRequest request : requestList)
                request.readStatus = 1;
            SqliteUtils.updateFriendRequests(requestList);
        }

        @Override
        public int getUnreadFriendRequestStatus() throws RemoteException {
            logInfo("");
            List<FriendRequest> requestList = SqliteUtils.queryUnreadFriendRequest();
            return requestList == null ? 0 : requestList.size();
        }

        @Override
        public void removeFriend(String userId, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            osnsdk.deleteFriend(userId, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void sendFriendRequest(String userId, String reason, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            osnsdk.inviteFriend(userId, reason, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
//                        FriendRequest request = new FriendRequest();
//                        request.target = userId;
//                        request.reason = reason;
//                        request.direction = FriendRequest.Direction_Sent;
//                        SqliteUtils.insertFriendRequest(request);
                        callback.onSuccess();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void handleGroupRequest(String userId, String groupId, boolean accept, final IGeneralCallback callback) throws RemoteException {
            logInfo("userId: " + userId + ", groupId: " + groupId + ", accept: " + accept);
            FriendRequest request = SqliteUtils.queryFriendRequest(userId, groupId);
            request.status = accept ? FriendRequest.RequestStatus_Accepted : FriendRequest.RequestStatus_Rejected;
            SqliteUtils.updateFriendRequests(Collections.singletonList(request));
            if (accept) {
                if (request.type == RequestType_ApplyMember) {
                    osnsdk.acceptMember(userId, groupId, new OSNGeneralCallback() {
                        @Override
                        public void onSuccess(String json) {
                            try {
                                callback.onSuccess();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            try {
                                callback.onFailure(-1);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    osnsdk.joinGroup(groupId, null, new OSNGeneralCallback() {
                        @Override
                        public void onSuccess(String json) {
                            try {
                                callback.onSuccess();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            try {
                                callback.onFailure(-1);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            } else {
                if (request.type == RequestType_ApplyMember) {
                    osnsdk.rejectMember(userId, groupId, new OSNGeneralCallback() {
                        @Override
                        public void onSuccess(String json) {
                            try {
                                callback.onSuccess();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            try {
                                callback.onFailure(-1);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    osnsdk.rejectGroup(groupId, new OSNGeneralCallback() {
                        @Override
                        public void onSuccess(String json) {
                            try {
                                callback.onSuccess();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            try {
                                callback.onFailure(-1);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }

        @Override
        public void handleFriendRequest(String userId, boolean accept, String extra, final IGeneralCallback callback) throws RemoteException {
            logInfo("userId: " + userId + ", accept: " + accept);
            FriendRequest request = SqliteUtils.queryFriendRequest(userId);
            request.status = accept ? FriendRequest.RequestStatus_Accepted : FriendRequest.RequestStatus_Rejected;
            SqliteUtils.updateFriendRequests(Collections.singletonList(request));
            if (accept) {
                osnsdk.acceptFriend(userId, new OSNGeneralCallback() {
                    @Override
                    public void onSuccess(String json) {
                        try {
                            callback.onSuccess();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        try {
                            callback.onFailure(-1);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                osnsdk.rejectFriend(userId, new OSNGeneralCallback() {
                    @Override
                    public void onSuccess(String json) {
                        try {
                            callback.onSuccess();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        try {
                            callback.onFailure(-1);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        @Override
        public void setBlackList(String userId, boolean isBlacked, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            OsnFriendInfo friendInfo = SqliteUtils.queryFriend(userId);
            if (friendInfo == null) {
                if (callback != null)
                    callback.onFailure(-1);
                return;
            }
            friendInfo.state = isBlacked ? OsnFriendInfo.Blacked : OsnFriendInfo.Normal;
            osnsdk.modifyFriendInfo(Collections.singletonList("state"), friendInfo, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        SqliteUtils.updateFriend(friendInfo, Collections.singletonList("state"));
                        if (callback != null)
                            callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        if (callback != null)
                            callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void joinChatRoom(String chatRoomId, IGeneralCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void quitChatRoom(String chatRoomId, IGeneralCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getChatRoomInfo(String chatRoomId, long updateDt, IGetChatRoomInfoCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getChatRoomMembersInfo(String chatRoomId, int maxCount, IGetChatRoomMembersInfoCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void clearFriend(final IGeneralCallback callback) throws RemoteException {
            SqliteUtils.clearFriend();
        }
        @Override
        public void deleteFriend(String userId, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            osnsdk.deleteFriend(userId, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public GroupInfo getGroupInfo(String groupId, boolean refresh) throws RemoteException {
            logInfo("groupID: " + groupId + ", refresh: " + refresh);
            if (groupId.equalsIgnoreCase("null"))
                return new NullGroupInfo("null");
            GroupInfo groupInfo = SqliteUtils.queryGroup(groupId);
            if (groupInfo == null || refresh) {
                osnsdk.getGroupInfo(groupId, new OSNGeneralCallbackT<OsnGroupInfo>() {
                    @Override
                    public void onFailure(String error) {
                    }

                    @Override
                    public void onSuccess(OsnGroupInfo osnGroupInfo) {
                        updateGroup(osnGroupInfo, true);
                        GroupInfo groupNew = SqliteUtils.queryGroup(osnGroupInfo.groupID);
                        onGroupInfoUpdated(Collections.singletonList(groupNew));
                    }
                });
            }
            if (groupInfo == null)
                groupInfo = new NullGroupInfo(groupId);
            return groupInfo;
        }

        @Override
        public void getGroupInfoEx(String groupId, boolean refresh, IGetGroupCallback callback) throws RemoteException {
            logInfo("groupID: " + groupId + ", refresh: " + refresh);
            osnsdk.getGroupInfo(groupId, new OSNGeneralCallbackT<OsnGroupInfo>() {
                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onSuccess(OsnGroupInfo osnGroupInfo) {
                    try {
                        updateGroup(osnGroupInfo, false);
                        GroupInfo groupNew = SqliteUtils.queryGroup(osnGroupInfo.groupID);
                        callback.onSuccess(groupNew);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public UserInfo getUserInfo(String userId, String groupId, boolean refresh) throws RemoteException {
            logInfo("userID: " + userId + ", groupID: " + groupId + ", refresh: " + refresh);
            if (userId == null)
                userId = osnsdk.getUserID();
            if (userId.equalsIgnoreCase("null"))
                return new NullUserInfo(userId);

            UserInfo userInfo = SqliteUtils.queryUser(userId);
            if (userInfo == null || refresh) {
                osnsdk.getUserInfo(userId, new OSNGeneralCallbackT<OsnUserInfo>() {
                    @Override
                    public void onFailure(String error) {
                    }

                    @Override
                    public void onSuccess(OsnUserInfo osnUserInfo) {
                        UserInfo userInfo = toClientUser(osnUserInfo);
                        SqliteUtils.insertUser(userInfo);
                        onUserInfoUpdated(Collections.singletonList(userInfo));
                    }
                });
            }
            if (userInfo == null)
                userInfo = new NullUserInfo(userId);
            return userInfo;
        }

        @Override
        public List<UserInfo> getUserInfos(List<String> userIds, String groupId) throws RemoteException {
            logInfo("");
            List<UserInfo> userInfos = new ArrayList<>();
            for (String u : userIds) {
                UserInfo userInfo = getUserInfo(u, groupId, false);
                userInfos.add(userInfo);
            }
            return userInfos;
        }

        @Override
        public void getUserInfoEx(String userId, boolean refresh, IGetUserCallback callback) throws RemoteException {
            logInfo("userID: " + userId + ", refresh: " + refresh);
            osnsdk.getUserInfo(userId, new OSNGeneralCallbackT<OsnUserInfo>() {
                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onSuccess(OsnUserInfo osnUserInfo) {
                    try {
                        UserInfo userInfo = toClientUser(osnUserInfo);
                        SqliteUtils.insertUser(userInfo);
                        callback.onSuccess(userInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void uploadMedia(String fileName, byte[] data, int mediaType, final IUploadMediaCallback callback) throws RemoteException {
            logInfo("fileName: " + fileName + ", mediaType: " + mediaType);
            String type = mediaType == MessageContentMediaType.PORTRAIT.getValue() ? "portrait" : "cache";
            osnsdk.uploadData(fileName, type, data, new OSNTransferCallback() {
                @Override
                public void onSuccess(String data) {
                    try {
                        JSONObject json = JSON.parseObject(data);
                        callback.onSuccess(json.getString("url"));
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onProgress(long progress, long total) {
                    try {
                        callback.onProgress(progress, total);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void uploadMediaFile(String mediaPath, int mediaType, IUploadMediaCallback callback) throws RemoteException {
            logInfo("");
            try {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(mediaPath));
                int length = bufferedInputStream.available();
                byte[] data = new byte[length];
                bufferedInputStream.read(data);

//                String fileName = "";
//                if (mediaPath.contains("/")) {
//                    fileName = mediaPath.substring(mediaPath.lastIndexOf("/") + 1, mediaPath.length());
//                }
                uploadMedia(mediaPath, data, mediaType, callback);
            } catch (Exception e) {
                e.printStackTrace();
                e.printStackTrace();
                callback.onFailure(ErrorCode.FILE_NOT_EXIST);
            }
        }

        @Override
        public void modifyMyInfo(List<ModifyMyInfoEntry> values, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            List<String> keys = new ArrayList<>();
            OsnUserInfo userInfo = new OsnUserInfo();
            for (ModifyMyInfoEntry v : values) {
                if (v.type == ModifyMyInfoType.Modify_DisplayName) {
                    keys.add("displayName");
                    userInfo.displayName = v.value;
                } else if (v.type == ModifyMyInfoType.Modify_Portrait) {
                    keys.add("portrait");
                    userInfo.portrait = v.value;
                } else if (v.type == ModifyMyInfoType.Modify_UrlSpace) {
                    keys.add("urlSpace");
                    userInfo.urlSpace = v.value;
                }
            }
            osnsdk.modifyUserInfo(keys, userInfo, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public boolean deleteMessage(long messageId) throws RemoteException {
            logInfo("");
            Message message = SqliteUtils.queryMessage(messageId);
            if(message == null){
                return false;
            }
            long timestamp = System.currentTimeMillis();
            if(message.sender.equalsIgnoreCase(mUserId) &&
                    timestamp > message.serverTime &&
                    (timestamp - message.serverTime) < 5*60*1000){
                osnsdk.deleteMessage(message.messageHash, message.conversation.target, null);
            }
            SqliteUtils.deleteMessage(messageId);
            return true;
        }

        @Override
        public List<ConversationSearchResult> searchConversation(String keyword, int[] conversationTypes, int[] lines) throws RemoteException {
            logInfo("");
            List<ConversationSearchResult> output = new ArrayList<>();
            return output;
        }

        @Override
        public List<Message> searchMessage(Conversation conversation, String keyword, boolean desc, int limit, int offset) throws RemoteException {
            logInfo("key: " + keyword + ", desc: " + desc + ", limit: " + limit + ", offset: " + offset);
            return SqliteUtils.queryMessages(conversation, keyword, desc, limit, offset);
        }

        @Override
        public void searchMessagesEx(int[] conversationTypes, int[] lines, int[] contentTypes, String keyword, long fromIndex, boolean before, int count, IGetMessageCallback callback) throws RemoteException {
            logInfo("");
        }


        @Override
        public List<GroupSearchResult> searchGroups(String keyword) throws RemoteException {
            logInfo("");
            List<GroupSearchResult> output = new ArrayList<>();
            return output;
        }

        @Override
        public List<UserInfo> searchFriends(String keyworkd) throws RemoteException {
            logInfo("");
            List<UserInfo> friendInfos = new ArrayList<>();
            List<String> friends = SqliteUtils.listFriends();
            for (String f : friends) {
                UserInfo userInfo = SqliteUtils.queryUser(f);
                if (userInfo == null) {
                    OsnUserInfo osnUserInfo = osnsdk.getUserInfo(f, null);
                    if (osnUserInfo != null)
                        userInfo = toClientUser(osnUserInfo);
                }
                if (userInfo != null) {
                    if (userInfo.displayName.contains(keyworkd))
                        friendInfos.add(userInfo);
                }
            }
            return friendInfos;
        }

        @Override
        public String getEncodedClientId() throws RemoteException {
            logInfo("");
            return null;
        }

        @Override
        public void createGroup(String groupId, String groupName, String groupPortrait, int groupType, List<String> memberIds, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback2 callback) throws RemoteException {
            logInfo("groupId: " + groupId + ", groupName: " + groupName + ", groupType: " + groupType);
            for (String member : memberIds)
                logInfo("member: " + member);

            osnsdk.createGroup(groupName, memberIds, groupType, groupPortrait, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        JSONObject data = JSON.parseObject(json);
                        callback.onSuccess(data.getString("groupID"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void joinGroup(String groupId, final IGeneralCallback callback) {
            logInfo("groupId: " + groupId);
            osnsdk.joinGroup(groupId, null, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void addGroupMembers(String groupId, List<String> memberIds, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            osnsdk.addMember(groupId, memberIds, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void removeGroupMembers(String groupId, List<String> memberIds, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            osnsdk.delMember(groupId, memberIds, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void quitGroup(String groupId, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            osnsdk.quitGroup(groupId, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void dismissGroup(String groupId, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            osnsdk.dismissGroup(groupId, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                        delGroup(groupId, "dismiss");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void modifyGroupInfo(String groupId, int modifyType, String newValue, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            logInfo("groupID: " + groupId + ", type: " + modifyType + ", value: " + newValue);

            OsnGroupInfo groupInfo = new OsnGroupInfo();
            groupInfo.groupID = groupId;

            List<String> keys = new ArrayList<>();
            if (modifyType == ModifyGroupInfoType.Modify_Group_Name.getValue()) {
                keys.add("name");
                groupInfo.name = newValue;
            } else if (modifyType == ModifyGroupInfoType.Modify_Group_Portrait.getValue()) {
                keys.add("portrait");
                groupInfo.portrait = newValue;
            } else if (modifyType == ModifyGroupInfoType.Modify_Group_Type.getValue()) {
                keys.add("type");
                groupInfo.type = Integer.parseInt(newValue);
            } else if (modifyType == ModifyGroupInfoType.Modify_Group_JoinType.getValue()) {
                keys.add("joinType");
                groupInfo.joinType = Integer.parseInt(newValue);
            } else if (modifyType == ModifyGroupInfoType.Modify_Group_PassType.getValue()) {
                keys.add("passType");
                groupInfo.passType = Integer.parseInt(newValue);
            } else if (modifyType == ModifyGroupInfoType.Modify_Group_Mute.getValue()) {
                keys.add("mute");
                groupInfo.mute = Integer.parseInt(newValue);
            }
            osnsdk.modifyGroupInfo(keys, groupInfo, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void modifyGroupAlias(String groupId, String newAlias, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
            OsnMemberInfo info = new OsnMemberInfo();
            info.groupID = groupId;
            info.osnID = mUserId;
            info.nickName = newAlias;
            osnsdk.modifyMemberInfo(Collections.singletonList("nickName"), info, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        if (callback != null)
                            callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        if (callback != null)
                            callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void modifyGroupMemberAlias(String groupId, String memberId, String newAlias, int[] notifyLines, MessagePayload notifyMsg, IGeneralCallback callback) throws RemoteException {
            logInfo("");
            OsnMemberInfo info = new OsnMemberInfo();
            info.groupID = groupId;
            info.osnID = memberId;
            info.nickName = newAlias;
            osnsdk.modifyMemberInfo(Collections.singletonList("nickName"), info, new OSNGeneralCallback() {
                @Override
                public void onSuccess(String json) {
                    try {
                        if (callback != null)
                            callback.onSuccess();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(String error) {
                    try {
                        if (callback != null)
                            callback.onFailure(-1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public List<GroupMember> getGroupMembers(String groupId, boolean forceUpdate) throws RemoteException {
            logInfo("groupID: " + groupId + ", forceUpdate: " + forceUpdate);
            List<GroupMember> members = SqliteUtils.queryMembers(groupId);
            if (members == null || forceUpdate) {
                osnsdk.getMemberInfo(groupId, new OSNGeneralCallbackT<List<OsnMemberInfo>>() {
                    @Override
                    public void onFailure(String error) {
                    }

                    @Override
                    public void onSuccess(List<OsnMemberInfo> members) {
                        List<GroupMember> memberList = updateMember(groupId, members);
                        onGroupMembersUpdated(groupId, memberList);
                    }
                });
            }
            boolean isValied = false;
            for (GroupMember m : members) {
                if (m.memberId.equalsIgnoreCase(mUserId)) {
                    isValied = true;
                    break;
                }
                logInfo("memberID: " + m.memberId + ", alias: " + m.alias + ", type: " + m.type);
            }
            if (!isValied)
                members.clear();
            return members;
        }

        @Override
        public List<GroupMember> getGroupMembersByType(String groupId, int type) throws RemoteException {
            logInfo("");
            List<GroupMember> out = new ArrayList<>();
            return out;
        }

        @Override
        public GroupMember getGroupMember(String groupId, String memberId) throws RemoteException {
            GroupMember groupMember = SqliteUtils.queryMember(groupId, memberId);
            if (groupMember == null)
                groupMember = new NullGroupMember(groupId, memberId);
            logInfo("memberID: " + memberId + ", type: " + groupMember.type);
            return groupMember;
        }

        @Override
        public void getGroupMemberEx(String groupId, boolean forceUpdate, IGetGroupMemberCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void transferGroup(String groupId, String newOwner, int[] notifyLines, MessagePayload notifyMsg, final IGeneralCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void setGroupManager(String groupId, boolean isSet, List<String> memberIds, int[] notifyLines, MessagePayload notifyMsg, IGeneralCallback callback) throws RemoteException {
            logInfo("");
            String[] memberArray = new String[memberIds.size()];
            for (int i = 0; i < memberIds.size(); i++) {
                memberArray[i] = memberIds.get(i);
            }
        }

        @Override
        public boolean isGroupMember(String groupId, String userId) {
            logInfo("groupId: " + groupId + ", userId: " + userId);
            return SqliteUtils.queryMember(groupId, userId) != null;
        }

        @Override
        public void muteOrAllowGroupMember(String groupId, boolean isSet, List<String> memberIds, boolean isAllow, int[] notifyLines, MessagePayload notifyMsg, IGeneralCallback callback) throws RemoteException {
            logInfo("");
            String[] memberArray = new String[memberIds.size()];
            for (int i = 0; i < memberIds.size(); i++) {
                memberArray[i] = memberIds.get(i);
            }
        }

        @Override
        public byte[] encodeData(byte[] data) throws RemoteException {
            return null;
        }

        @Override
        public byte[] decodeData(byte[] data) throws RemoteException {
            return null;
        }

        @Override
        public String getHost() throws RemoteException {
            return mHost;
        }

        @Override
        public void setHost(String host) throws RemoteException {
            mHost = host;
            osnsdk.resetHost(host);
        }

        @Override
        public void createChannel(String channelId, String channelName, String channelPortrait, String desc, String extra, ICreateChannelCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void modifyChannelInfo(String channelId, int modifyType, String newValue, IGeneralCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public ChannelInfo getChannelInfo(String channelId, boolean refresh) throws RemoteException {
            logInfo("");
            return null;
        }

        @Override
        public void searchChannel(String keyword, ISearchChannelCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public boolean isListenedChannel(String channelId) throws RemoteException {
            logInfo("");
            return false;
        }

        @Override
        public void listenChannel(String channelId, boolean listen, IGeneralCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void destoryChannel(String channelId, IGeneralCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public List<String> getMyChannels() throws RemoteException {
            logInfo("");
            List<String> out = new ArrayList<>();
            return out;
        }

        @Override
        public List<String> getListenedChannels() throws RemoteException {
            logInfo("");
            List<String> out = new ArrayList<>();
            return out;
        }

        @Override
        public List<LitappInfo> getLitappList() {
            return SqliteUtils.listLitapps();
        }

        @Override
        public void addLitapp(LitappInfo litapp) {
            SqliteUtils.insertLitapp(litapp);
        }

        public LitappInfo getLitapp(String target) {
            return SqliteUtils.queryLitapp(target);
        }

        @Override
        public LitappInfo getLitappInfo(String target, boolean refresh) {
            LitappInfo litappInfo = getLitapp(target);
            if (litappInfo == null || refresh) {
                osnsdk.getServiceInfo(target, new OSNGeneralCallbackT<OsnServiceInfo>() {
                    @Override
                    public void onFailure(String error) {
                    }

                    @Override
                    public void onSuccess(OsnServiceInfo osnServiceInfo) {
                        if (osnServiceInfo instanceof OsnLitappInfo) {
                            LitappInfo litappInfo = toClientLitapp((OsnLitappInfo) osnServiceInfo);
                            addLitapp(litappInfo);
                            onLitappInfoUpdated(Collections.singletonList(litappInfo));
                        }
                    }
                });
            }
            return litappInfo;
        }

        @Override
        public void getLitappInfoEx(String target, boolean refresh, IGetLitappCallback callback) {
            LitappInfo litappInfo = getLitapp(target);
            if (litappInfo == null || refresh) {
                osnsdk.getServiceInfo(target, new OSNGeneralCallbackT<OsnServiceInfo>() {
                    @Override
                    public void onFailure(String error) {
                        try {
                            callback.onFailure(-1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onSuccess(OsnServiceInfo osnServiceInfo) {
                        try {
                            if (osnServiceInfo instanceof OsnLitappInfo) {
                                LitappInfo litappInfo = toClientLitapp((OsnLitappInfo) osnServiceInfo);
                                addLitapp(litappInfo);
                                callback.onSuccess(litappInfo);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        @Override
        public String signData(String data) {
            return osnsdk.signData(data);
        }

        @Override
        public String getImageThumbPara() throws RemoteException {
            logInfo("");
            return null;
        }

        @Override
        public void kickoffPCClient(String pcClientId, IGeneralCallback callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getApplicationId(String applicationId, IGeneralCallback2 callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public void getAuthorizedMediaUrl(long messageUid, int mediaType, String mediaPath, IGeneralCallback2 callback) throws RemoteException {
            logInfo("");
        }

        @Override
        public int getMessageCount(Conversation conversation) throws RemoteException {
            logInfo("");
            return 0;
        }

        @Override
        public boolean begainTransaction() throws RemoteException {
            logInfo("");
            return false;
        }

        @Override
        public void commitTransaction() throws RemoteException {
            logInfo("");
        }

        @Override
        public boolean isCommercialServer() throws RemoteException {
            logInfo("");
            return false;
        }

        @Override
        public boolean isReceiptEnabled() throws RemoteException {
            logInfo("");
            return false;
        }

        @Override
        public void sendConferenceRequest(long sessionId, String roomId, String request, String data, IGeneralCallback2 callback) throws RemoteException {
            logInfo("");
        }
    }

    private class WfcRemoteCallbackList<E extends IInterface> extends RemoteCallbackList<E> {
        @Override
        public void onCallbackDied(E callback, Object cookie) {
            Intent intent = new Intent(ClientService.this, RecoverReceiver.class);
            sendBroadcast(intent);
        }
    }
}
