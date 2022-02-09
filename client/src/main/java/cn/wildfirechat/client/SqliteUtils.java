package cn.wildfirechat.client;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ospn.osnsdk.data.OsnFriendInfo;
import com.ospn.osnsdk.data.OsnMemberInfo;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.CardMessageContent;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.StickerMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.UnknownMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.notification.AddGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.CreateGroupNotificationContent;
import cn.wildfirechat.message.notification.DismissGroupNotificationContent;
import cn.wildfirechat.message.notification.FriendAddedMessageContent;
import cn.wildfirechat.message.notification.FriendGreetingMessageContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.QuitGroupNotificationContent;
import cn.wildfirechat.message.notification.TransferGroupOwnerNotificationContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.LitappInfo;
import cn.wildfirechat.model.UnreadCount;
import cn.wildfirechat.model.UserInfo;

import static com.ospn.osnsdk.utils.OsnUtils.logInfo;

public class SqliteUtils {
    private static SQLiteDatabase mDB = null;
    private static long tMessageID = 1;

    public static void initDB(String path){
        try {
            if(mDB != null)
                return;
            logInfo("path: "+path);
            mDB = SQLiteDatabase.openOrCreateDatabase(path, null);

            String sql = "create table if not exists t_user(_id integer primary key autoincrement, " +
                    "osnID char(128) UNIQUE, " +
                    "name nvarchar(20), " +
                    "portrait text, " +
                    "displayName nvarchar(20), " +
                    "urlSpace text)";
            mDB.execSQL(sql);
            sql = "create table if not exists t_friend(_id integer primary key autoincrement, " +
                    "osnID char(128) , " +
                    "friendID char(128) UNIQUE, " +
                    "remarks char(128) , " +
                    "state tinyint default 0)";
            mDB.execSQL(sql);
            sql = "create table if not exists t_friendRequest(_id integer primary key autoincrement, " +
                    "type tinyint , " + //add 2021.5.21
                    "direction tinyint , " +
                    "target char(128) , " +
                    "originalUser char(128) , " + //add 2021.5.21
                    "userID char(128) , " + //add 2021.5.21
                    "reason char(128) , " +
                    "status tinyint , " +
                    "readStatus tinyint , " +
                    "timestamp long)";
            mDB.execSQL(sql);
            sql = "create table if not exists t_message(_id integer primary key autoincrement, " +
                    "mid integer, " +
                    "osnID char(128), " +
                    "cType tinyint, " +
                    "target char(128), " +
                    "dir tinyint, " +
                    "state tinyint default 0," + //MessageStatus
                    "uid integer, " +
                    "timestamp integer, " +
                    "msgType tinyint default 0," +
                    "msgText text," +
                    "msgHash text)";
            mDB.execSQL(sql);
            sql = "create table if not exists t_conversation(_id integer primary key autoincrement, " +
                    "type tinyint, " +
                    "target char(128), " +
                    "line tinyint," +
                    "timestamp integer," +
                    "draft text," +
                    "unreadCount int default 0," +
                    "unreadMention int default 0," +
                    "unreadMentionAll int default 0," +
                    "isTop tinyint," +
                    "isSilent tinyint ,"+
                    "unique(target,line) on conflict replace)";
            mDB.execSQL(sql);
            sql = "create table if not exists t_group(_id integer primary key autoincrement, " +
                    "groupID char(128) unique, " +
                    "name char(20), " +
                    "portrait char(128), " +
                    "owner char(128), " +
                    "type tinyint, " +
                    "memberCount int, " +
                    "extra text, " +
                    "updateDt long, " +
                    "fav int default 0, " +
                    "mute tinyint default 0, " +
                    "joinType tinyint default 0, " +
                    "passType tinyint default 0, " +
                    "privateChat tinyint default 0, " +
                    "maxMemberCount long default 200, " +
                    "showAlias tinyint default 0)";
            mDB.execSQL(sql);
            sql = "create table if not exists t_groupMember(_id integer primary key autoincrement, " +
                    "groupID char(128), " +
                    "memberID char(128), " +
                    "alias char(20), " +
                    "type tinyint default 0, " +
                    "updateDt long, " +
                    "createDt long, UNIQUE(groupID, memberID) ON CONFLICT REPLACE)";
            mDB.execSQL(sql);
            sql = "create table if not exists t_litapp(_id integer primary key autoincrement, " +
                    "target char(128) UNIQUE, " +
                    "name char(64), " +
                    "displayName char(64), " +
                    "portrait varchar(512), " +
                    "theme varchar(512), " +
                    "url varchar(512), "+
                    "info text)";
            mDB.execSQL(sql);

//            updateTable();

            Cursor cursor = mDB.rawQuery("select mid from t_message order by mid DESC limit 1", null);
            if(cursor.moveToNext())
                tMessageID = cursor.getInt(0)+1;
            cursor.close();

            logInfo("tMessageID: " + tMessageID);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void closeDB(){
        if(mDB != null){
            mDB.close();
            mDB = null;
        }
    }
    public static void updateTable(){
        String[] sqls = {
            "alter table t_user add column urlSpace text null", //add 2021.5.15
            "alter table t_groupMember add column type tinyint default 0", //add 2021.5.20
            "alter table t_groupMember add column mute tinyint default 0", //add 2021.5.20
            "alter table t_friendRequest add column type tinyint default 0", //add 2021.5.21
            "alter table t_friendRequest add column originalUser char(128) not null", //add 2021.5.21
            "alter table t_friendRequest add column userID char(128) not null", //add 2021.5.21
            "alter table t_group add column passType tinyint default 0", //add 2021.5.22
            "alter table t_message add column msgHash text", //add 2021.10.20
        };
        for(String sql : sqls) {
            try {
                mDB.execSQL(sql);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void insertUser(UserInfo userInfo){
        if(mDB == null)
            return;
        try{
            String sql = "insert or replace into t_user(osnID,name,displayName,portrait,urlSpace) values('"+userInfo.uid+"','"+userInfo.name+"','"+userInfo.displayName+"','"+userInfo.portrait+"','"+userInfo.urlSpace+"')";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void updateUser(UserInfo userInfo){
        try{
            String sql = "update t_user set displayName='"+userInfo.displayName+
                    "',portrait='"+userInfo.portrait+
                    "' where osnID='"+userInfo.uid+"'";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void updateUser(UserInfo userInfo, List<String> keys){
        try{
            String sql = null;
            for(String k:keys) {
                switch(k){
                    case "displayName":
                        sql = "update t_user set displayName='" + userInfo.displayName +"' where osnID='" + userInfo.uid + "'";
                        break;
                    case "portrait":
                        sql = "update t_user set portrait='" + userInfo.portrait +"' where osnID='" + userInfo.uid + "'";
                        break;
                    case "urlSpace":
                        sql = "update t_user set urlSpace='" + userInfo.urlSpace +"' where osnID='" + userInfo.uid + "'";
                        break;
                }
                mDB.execSQL(sql);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static UserInfo queryUser(String userID){
        UserInfo userInfo = null;
        if(mDB == null){
            return userInfo;
        }
        try{
            Cursor cursor = mDB.rawQuery("select (select remarks from t_friend where friendID=?) " +
                    "as remarks,* from t_user where osnID=?", new String[]{userID, userID});
            if(cursor.moveToFirst())
                userInfo = getUserInfo(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return userInfo;
    }
    public static List<UserInfo> queryUsers(String keyword){
        List<UserInfo> userInfos = new ArrayList<>();
        try{
            Cursor cursor = mDB.rawQuery("select * from t_user where displayName like ?", new String[]{"%"+keyword+"%"});
            while(cursor.moveToFirst())
                userInfos.add(getUserInfo(cursor));
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return userInfos;
    }

    public static void insertFriendRequest(FriendRequest friendRequest){
        try {
            String sql = "insert or replace into t_friendRequest(type,direction,target,originalUser,userID,reason,status,readStatus,timestamp) values('"+
                    friendRequest.type+"','"+
                    friendRequest.direction+"','"+
                    friendRequest.target+"','"+
                    friendRequest.originalUser+"','"+
                    friendRequest.userID+"','"+
                    friendRequest.reason+"',"+
                    friendRequest.status+","+
                    friendRequest.readStatus+","+
                    friendRequest.timestamp+")";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void updateFriendRequests(List<FriendRequest> friendRequestList){
        try{
            for(FriendRequest request : friendRequestList){
                String sql = "update t_friendRequest set status="+request.status+", readStatus="+request.readStatus+
                        " where target='"+request.target+"' and userID='"+request.userID+"'";
                mDB.execSQL(sql);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static FriendRequest queryFriendRequest(String userID){
        FriendRequest request = null;
        try{

            Cursor cursor = mDB.rawQuery("select * from t_friendRequest where target=?", new String[]{userID});
            if(cursor.moveToFirst())
                request = getFriendRequest(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return request;
    }
    public static FriendRequest queryFriendRequest(String userID, String groupID){
        FriendRequest request = null;
        try{

            Cursor cursor = mDB.rawQuery("select * from t_friendRequest where target=? and userID=?", new String[]{groupID,userID});
            if(cursor.moveToFirst())
                request = getFriendRequest(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return request;
    }
    public static List<FriendRequest> listFriendRequest(){
        List<FriendRequest> friendRequestList = new ArrayList<>();
        try{
            Cursor cursor = mDB.rawQuery("select * from t_friendRequest", null);
            while(cursor.moveToNext()){
                FriendRequest request = getFriendRequest(cursor);
                friendRequestList.add(request);
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return friendRequestList;
    }
    public static List<FriendRequest> queryUnreadFriendRequest(){
        List<FriendRequest> friendRequestList = new ArrayList<>();
        if(mDB == null){
            return friendRequestList;
        }
        try{
            Cursor cursor = mDB.rawQuery("select * from t_friendRequest where readStatus=0", null);
            if(cursor.moveToFirst()){
                FriendRequest request = getFriendRequest(cursor);
                friendRequestList.add(request);
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return friendRequestList;
    }

    public static void insertFriend(OsnFriendInfo friendInfo){
        if(mDB == null)
            return;
        try {
            String sql = "insert or replace into t_friend(osnID,friendID,state) values('"+friendInfo.userID+"','"+friendInfo.friendID+"',"+friendInfo.state+")";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void deleteFriend(String friendID){
        try{
            String sql = "delete from t_friend where friendID='"+friendID+"'";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void updateFriend(OsnFriendInfo friendInfo){
        try{
            String sql = "update t_friend set state="+friendInfo.state+",remarks='"+friendInfo.remarks+"' where friendID='"+friendInfo.friendID+"'";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void updateFriend(OsnFriendInfo friendInfo, List<String> keys){
        try{
            String sql = null;
            for(String k:keys) {
                switch(k){
                    case "state":
                        sql = "update t_friend set state="+friendInfo.state+" where friendID='"+friendInfo.friendID+"'";
                        break;
                    case "remarks":
                        sql = "update t_friend set remarks='"+friendInfo.remarks+"' where friendID='"+friendInfo.friendID+"'";
                        break;
                }
                mDB.execSQL(sql);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static List<String> listFriends(){
        List<String> userList = new ArrayList<>();
        if(mDB == null){
            return userList;
        }
        try{
            Cursor cursor = mDB.rawQuery("select friendID from t_friend", null);
            while (cursor.moveToNext()){
                String friendID = cursor.getString(0);
                userList.add(friendID);
            }
            cursor.close();
            return userList;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return userList;
    }
    public static List<String> listFriends(int state){
        List<String> userList = new ArrayList<>();
        try{
            Cursor cursor = mDB.rawQuery("select friendID from t_friend where state=?", new String[]{String.valueOf(state)});
            while (cursor.moveToNext()){
                String friendID = cursor.getString(0);
                userList.add(friendID);
            }
            cursor.close();
            return userList;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return userList;
    }
    public static OsnFriendInfo queryFriend(String friendID){
        OsnFriendInfo friendInfo = null;
        if(mDB == null)
            return null;
        try{
            Cursor cursor = mDB.rawQuery("select * from t_friend where friendID=?", new String[]{friendID});
            if(cursor.moveToNext()) {
                friendInfo = new OsnFriendInfo();
                friendInfo.userID = cursor.getString(cursor.getColumnIndex("osnID"));
                friendInfo.friendID = friendID;
                friendInfo.remarks = cursor.getString(cursor.getColumnIndex("remarks"));
                friendInfo.state = cursor.getInt(cursor.getColumnIndex("state"));
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return friendInfo;
    }
    public static void clearFriend(){
        try{
            String sql = "delete from t_friend";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void insertConversation(int type, String target, int line){
        try{
            String sql = "insert into t_conversation(type,target,line,timestamp) values("+type+",'"+target+"',"+line+","+System.currentTimeMillis()+")";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void deleteConversation(int type, String target, int line){
        try{
            String sql = "delete from t_conversation where type="+type+" and target='"+target+"' and line="+line+"";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static ConversationInfo queryConversation(int type, String target, int line){
        ConversationInfo conversationInfo = null;
        try{
            Cursor cursor = mDB.rawQuery("select * from t_conversation where type=? and target=? and line=?", new String[]{String.valueOf(type), target, String.valueOf(line)});
            if(cursor.moveToNext())
                conversationInfo = getConversation(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return conversationInfo;
    }
    public static void updateConversation(ConversationInfo conversationInfo){
        try{
            String sql = "update t_conversation set timestamp="+conversationInfo.timestamp+
                    ", draft="+conversationInfo.draft+
                    ", unreadCount="+conversationInfo.unreadCount.unread+
                    ", unreadMention="+conversationInfo.unreadCount.unreadMention+
                    ", unreadMentionAll="+conversationInfo.unreadCount.unreadMentionAll+
                    ", isTop="+(conversationInfo.isTop?1:0)+
                    ", isSilent="+(conversationInfo.isSilent?1:0)+
                    " where type="+conversationInfo.conversation.type.getValue()+
                    " and target='"+conversationInfo.conversation.target+"'"+
                    " and line="+conversationInfo.conversation.line;
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void updateConversation(ConversationInfo conversationInfo, List<String> keys){
        try{
            for(String k : keys){
                String sql = null;
                switch(k){
                    case "top":
                        sql = "update t_conversation set isTop="+(conversationInfo.isTop?1:0);
                        break;
                    case "silent":
                        sql = "update t_conversation set isSilent="+(conversationInfo.isSilent?1:0);
                        break;
                    case "draft":
                        sql = "update t_conversation set draft='"+conversationInfo.draft+"'";
                        break;
                    case "timestamp":
                        sql = "update t_conversation set timestamp="+conversationInfo.timestamp;
                        break;
                }
                if(sql == null)
                    continue;
                sql += " where type="+conversationInfo.conversation.type.getValue()+
                       " and target='"+conversationInfo.conversation.target+"'"+
                       " and line="+conversationInfo.conversation.line;
                mDB.execSQL(sql);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void clearConversation(){
        try{
            String sql = "delete from t_conversation";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void clearConversationUnread(int type, String target, int line){
        try{
            String sql = "update t_conversation set unreadCount=0, unreadMention=0,unreadMentionAll=0 where type="+type+" and target='"+target+"'"+" and line="+line;
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static List<ConversationInfo> listConversations(int type, int line){
        List<ConversationInfo> conversationInfoList = new ArrayList<>();
        try{
            Cursor cursor = mDB.rawQuery("select * from t_conversation where type=? and line=? order by timestamp desc", new String[]{String.valueOf(type),String.valueOf(line)});
            while(cursor.moveToNext()){
                ConversationInfo conversationInfo = getConversation(cursor);
                conversationInfoList.add(conversationInfo);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return conversationInfoList;
    }
    public static List<ConversationInfo> listAllConversations(int[] types, int[] lines){
        List<ConversationInfo> conversationInfoList = new ArrayList<>();
        if(mDB == null){
            return conversationInfoList;
        }
        try{
            String[] args = new String[types.length*2];
            StringBuilder sql = new StringBuilder("select * from t_conversation where ");
            for(int i = 0; i < types.length; ++i){
                if(i > 0)
                    sql.append("or ");
                sql.append("type=? and line=? ");
                args[i*2] = String.valueOf(types[i]);
                args[i*2+1] = String.valueOf(lines[0]);
            }
            sql.append("order by timestamp desc");
            Cursor cursor = mDB.rawQuery(sql.toString(), args);
            while(cursor.moveToNext()){
                ConversationInfo conversationInfo = getConversation(cursor);
                conversationInfoList.add(conversationInfo);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return conversationInfoList;
    }

    public static long insertMessage(Message msg){
        try{
            String data;
            JSONObject json = new JSONObject();
            switch(msg.content.getMessageContentType()){
                case MessageContentType.ContentType_Text:
                    TextMessageContent textMessageContent = (TextMessageContent)msg.content;
                    json.put("text",textMessageContent.getContent());
                    break;
                case MessageContentType.ContentType_File:
                    FileMessageContent fileMessageContent = (FileMessageContent)msg.content;
                    json.put("localPath", fileMessageContent.localPath);
                    json.put("remoteUrl", fileMessageContent.remoteUrl);
                    json.put("name", fileMessageContent.getName());
                    json.put("size", fileMessageContent.getSize());
                    break;
                case MessageContentType.ContentType_Voice:
                    SoundMessageContent soundMessageContent = (SoundMessageContent)msg.content;
                    json.put("localPath", soundMessageContent.localPath);
                    json.put("remoteUrl", soundMessageContent.remoteUrl);
                    json.put("duration",soundMessageContent.getDuration());
                    break;
                case MessageContentType.ContentType_Video:
                    VideoMessageContent videoMessageContent = (VideoMessageContent)msg.content;
                    json.put("localPath", videoMessageContent.localPath);
                    json.put("remoteUrl", videoMessageContent.remoteUrl);
                    byte[] thumbnail = videoMessageContent.getThumbnailBytes();
                    if(thumbnail != null)
                        json.put("thumbnail", Base64.encodeToString(thumbnail,Base64.NO_WRAP));
                    break;
                case MessageContentType.ContentType_Image:
                    ImageMessageContent imageMessageContent = (ImageMessageContent)msg.content;
                    json.put("localPath", imageMessageContent.localPath);
                    json.put("remoteUrl", imageMessageContent.remoteUrl);
                    json.put("width", imageMessageContent.getImageWidth());
                    json.put("height", imageMessageContent.getImageHeight());
                    break;
                case MessageContentType.ContentType_Sticker:
                    StickerMessageContent stickerMessageContent = (StickerMessageContent)msg.content;
                    json.put("localPath", stickerMessageContent.localPath);
                    json.put("remoteUrl", stickerMessageContent.remoteUrl);
                    json.put("width", stickerMessageContent.width);
                    json.put("height", stickerMessageContent.height);
                    break;
                case MessageContentType.ContentType_Friend_Added:
                case MessageContentType.ContentType_Friend_Greeting:
                    break;
                case MessageContentType.ContentType_CREATE_GROUP:
                    CreateGroupNotificationContent createGroupNotificationContent = (CreateGroupNotificationContent)msg.content;
                    json.put("creator",createGroupNotificationContent.creator);
                    json.put("groupName",createGroupNotificationContent.groupName);
                    break;
                case MessageContentType.ContentType_KICKOF_GROUP_MEMBER:
                    KickoffGroupMemberNotificationContent kickoffGroupMemberNotificationContent = (KickoffGroupMemberNotificationContent)msg.content;
                    json.put("operator",kickoffGroupMemberNotificationContent.operator);
                    json.put("members", kickoffGroupMemberNotificationContent.kickedMembers);
                    break;
                case MessageContentType.ContentType_ADD_GROUP_MEMBER:
                    AddGroupMemberNotificationContent addGroupMemberNotificationContent = (AddGroupMemberNotificationContent)msg.content;
                    json.put("invitor",addGroupMemberNotificationContent.invitor);
                    json.put("invitees",addGroupMemberNotificationContent.invitees);
                    break;
                case MessageContentType.ContentType_QUIT_GROUP:
                    QuitGroupNotificationContent quitGroupNotificationContent = (QuitGroupNotificationContent)msg.content;
                    json.put("operator",quitGroupNotificationContent.operator);
                    break;
                case MessageContentType.ContentType_DISMISS_GROUP:
                    DismissGroupNotificationContent dismissGroupNotificationContent = (DismissGroupNotificationContent)msg.content;
                    json.put("operator",dismissGroupNotificationContent.operator);
                    break;
                case MessageContentType.ContentType_TRANSFER_GROUP_OWNER:
                    TransferGroupOwnerNotificationContent transferGroupOwnerNotificationContent = (TransferGroupOwnerNotificationContent)msg.content;
                    json.put("operator",transferGroupOwnerNotificationContent.operator);
                    json.put("newOwner",transferGroupOwnerNotificationContent.newOwner);
                    break;
                case MessageContentType.ContentType_Card:
                    CardMessageContent cardMessageContent = (CardMessageContent)msg.content;
                    json.put("cardType",cardMessageContent.getType());
                    json.put("target",cardMessageContent.getTarget());
                    json.put("name",cardMessageContent.getName());
                    json.put("displayName",cardMessageContent.getDisplayName());
                    json.put("portrait",cardMessageContent.getPortrait());
                    json.put("theme",cardMessageContent.getTheme());
                    json.put("url",cardMessageContent.getUrl());
                    json.put("info",cardMessageContent.getInfo());
                    break;
                default:
                    logInfo("unknown type: "+msg.content.getMessageContentType());
                    break;
            }
            data = json.toString();

            long mid = tMessageID++;
            String sql = "insert into t_message(mid,uid,osnID,cType,target,dir,state,timestamp,msgType,msgText,msgHash) " +
                    "values('"+mid+
                    "','"+mid+
                    "','"+msg.sender+
                    "','"+msg.conversation.type.getValue()+
                    "','"+msg.conversation.target+
                    "','"+msg.direction.value()+
                    "','"+msg.status.value()+
                    "','"+msg.serverTime+
                    "','"+msg.content.getMessageContentType()+
                    "','"+data+
                    "','"+msg.messageHash+
                    "')";
            mDB.execSQL(sql);
            logInfo("mid: " + mid+", timestamp: "+msg.serverTime);
            return mid;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }
    public static void deleteMessage(long mid){
        try{
            logInfo("mid: "+mid);
            String sql = "delete from t_message where mid="+mid;
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void clearMessage(String target){
        try{
            String sql = "delete from t_message where target='"+target+"'";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static Message queryMessage(long mid){
        Message message = null;
        try{
            Cursor cursor = mDB.rawQuery("select * from t_message where mid=?",new String[]{String.valueOf(mid)});
            if(cursor.moveToNext())
                message = getMessage(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return message;
    }
    public static boolean queryMessage(long timestamp, String target){
        boolean hasMsg = false;
        try{
            Cursor cursor = mDB.rawQuery("select * from t_message where timestamp=? and target=?",new String[]{String.valueOf(timestamp),target});
            if(cursor.moveToNext())
                hasMsg = true;
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return hasMsg;
    }
    public static Message queryMessage(String hash){
        Message message = null;
        try{
            Cursor cursor = mDB.rawQuery("select * from t_message where msgHash=?",new String[]{hash});
            if(cursor.moveToNext())
                message = getMessage(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return message;
    }
    public static List<Message> queryMessages(Conversation conversation, long timestamp, boolean before, int count, boolean include){
        List<Message> messageList = new ArrayList<>();
        try{
            Cursor cursor = before
                    ? mDB.rawQuery("select * from (select * from t_message where target=? and cType=? and timestamp"+(include?"<=":"<")+"? order by timestamp desc limit ?) tmp order by timestamp",
                    new String[]{conversation.target,String.valueOf(conversation.type.getValue()),String.valueOf(timestamp),String.valueOf(count)})
                    : mDB.rawQuery("select * from t_message where target=? and cType=? and timestamp"+(include?">=":">")+"? order by timestamp limit ?",
                    new String[]{conversation.target,String.valueOf(conversation.type.getValue()),String.valueOf(timestamp),String.valueOf(count)});
            while(cursor.moveToNext()){
                Message message = getMessage(cursor);
                messageList.add(message);
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return messageList;
    }
    public static List<Message> queryMessages(Conversation conversation, String keyword, boolean desc, int limit, int offset){
        List<Message> messageList = new ArrayList<>();
        try{
            Cursor cursor = mDB.rawQuery("select * from t_message where target=? and msgText like ? limit ? offset ?",
                    new String[]{conversation.target,String.valueOf("%"+keyword+"%"),String.valueOf(limit),String.valueOf(offset)});
            while(cursor.moveToNext()){
                Message message = getMessage(cursor);
                messageList.add(message);
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return messageList;
    }
    public static void updateMessage(long mid, int state){
        try{
            String sql = "update t_message set state="+state+" where mid="+mid;
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void updateMessage(long mid, int state, String msgHash){
        try{
            String sql = "update t_message set state="+state+",msgHash='"+msgHash+"' where mid="+mid;
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static Message getLastMessage(Conversation conversation){
        Message message = null;
        try {
            Cursor cursor = mDB.rawQuery("select * from t_message where target=? and cType=? order by timestamp desc limit 1",
                    new String[]{conversation.target, String.valueOf(conversation.type.getValue())});
            if(cursor.moveToNext())
                message = getMessage(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return message;
    }
    public static Message getLastNotify(){
        Message message = null;
        try {
            Cursor cursor = mDB.rawQuery("select * from t_message where cType=? order by timestamp desc limit 1",
                    new String[]{"5"});
            if(cursor.moveToNext())
                message = getMessage(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return message;
    }

    public static void insertGroup(GroupInfo groupInfo){
        try {
            String sql = "insert or replace into t_group(groupID,name,portrait,owner,type,joinType,passType,mute,memberCount,fav) values('" +
                    groupInfo.target+"','" +
                    groupInfo.name+"','"+
                    groupInfo.portrait+"','"+
                    groupInfo.owner+"',"+
                    groupInfo.type.value()+","+
                    groupInfo.joinType+","+
                    groupInfo.passType+","+
                    groupInfo.mute+","+
                    groupInfo.memberCount+","+
                    groupInfo.fav+")";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void deleteGroup(String groupID){
        try{
            String sql = "delete from t_group where groupID='" + groupID + "'";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void updateGroup(GroupInfo groupInfo, List<String> keys){
        try{
            String sql = null;
            for(String k:keys) {
                switch(k){
                    case "name":
                        sql = "update t_group set name='" + groupInfo.name + "' where groupID='" + groupInfo.target+"'";
                        break;
                    case "portrait":
                        sql = "update t_group set portrait='" + groupInfo.portrait + "' where groupID='" + groupInfo.target+"'";
                        break;
                    case "fav":
                        sql = "update t_group set fav='" + groupInfo.fav + "' where groupID='" + groupInfo.target+"'";
                        break;
                    case "showAlias":
                        sql = "update t_group set showAlias=" + groupInfo.showAlias + " where groupID='" + groupInfo.target+"'";
                        break;
                    case "memberCount":
                        sql = "update t_group set memberCount=" + groupInfo.memberCount + " where groupID='" + groupInfo.target+"'";
                        break;
                    case "type":
                        sql = "update t_group set type=" + groupInfo.type.value() + " where groupID='" + groupInfo.target+"'";
                        break;
                    case "joinType":
                        sql = "update t_group set joinType=" + groupInfo.joinType + " where groupID='" + groupInfo.target+"'";
                        break;
                    case "passType":
                        sql = "update t_group set passType=" + groupInfo.passType + " where groupID='" + groupInfo.target+"'";
                        break;
                    case "mute":
                        sql = "update t_group set mute=" + groupInfo.mute + " where groupID='" + groupInfo.target+"'";
                        break;
                }
                mDB.execSQL(sql);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static GroupInfo queryGroup(String groupID){
        GroupInfo groupInfo = null;
        try{
            Cursor cursor = mDB.rawQuery("select * from t_group where groupID=?", new String[]{groupID});
            if(cursor.moveToNext())
                groupInfo = getGroupInfo(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return groupInfo;
    }
    public static List<GroupInfo> listGroups(){
        List<GroupInfo> groupInfoList = new ArrayList<>();
        try{
            Cursor cursor = mDB.rawQuery("select * from t_group", null);
            while(cursor.moveToNext()){
                GroupInfo groupInfo = getGroupInfo(cursor);
                groupInfoList.add(groupInfo);
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return groupInfoList;
    }

    public static List<LitappInfo> listLitapps(){
        List<LitappInfo> litappInfos = new ArrayList<>();
        if(mDB == null){
            return litappInfos;
        }
        try{
            Cursor cursor = mDB.rawQuery("select * from t_litapp", null);
            while(cursor.moveToNext()){
                LitappInfo litappInfo = getLitappInfo(cursor);
                litappInfos.add(litappInfo);
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return litappInfos;
    }
    public static void insertLitapp(LitappInfo litappInfo){
        try {
            String sql = "insert or replace into t_litapp(target,name,displayName,portrait,theme,url,info) values(" +
                    "'"+litappInfo.target+"'," +
                    "'"+litappInfo.name+"',"+
                    "'"+litappInfo.displayName+"',"+
                    "'"+litappInfo.portrait+"',"+
                    "'"+litappInfo.theme+"',"+
                    "'"+litappInfo.url+"',"+
                    "'"+litappInfo.info+"')";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static LitappInfo queryLitapp(String target){
        LitappInfo litappInfo = null;
        try{
            Cursor cursor = mDB.rawQuery("select * from t_litapp where target=?", new String[]{target});
            if(cursor.moveToNext())
                litappInfo = getLitappInfo(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return litappInfo;
    }

    public static void insertMembers(List<GroupMember> members){
        try {
            for (GroupMember m : members) {
                String sql = "insert or replace into t_groupMember(groupID,memberID,type,alias) values('" +
                        m.groupId + "','" +
                        m.memberId + "',"+
                        m.type.value() + ",'"+
                        m.alias+"')";
                mDB.execSQL(sql);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void deleteMembers(List<OsnMemberInfo> members){
        try{
            for(OsnMemberInfo m:members) {
                String sql = "delete from t_groupMember where groupID='" + m.groupID + "' and memberID='" + m.osnID + "'";
                mDB.execSQL(sql);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void updateMember(GroupMember groupMember, List<String> keys){
        try{
            String sql = null;
            for(String k:keys) {
                switch(k){
                    case "alias":
                        sql = "update t_groupMember set alias='" + groupMember.alias;
                        break;
                    case "type":
                        sql = "update t_groupMember set type='" + groupMember.type.value();
                        break;
                }
                sql += "' where groupID='" + groupMember.groupId+"' and memberID='"+groupMember.memberId+"'";
                mDB.execSQL(sql);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void clearMembers(String groupID){
        try{
            String sql = "delete from t_groupMember where groupID='" + groupID + "'";
            mDB.execSQL(sql);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public static List<GroupMember> queryMembers(String groupID){
        List<GroupMember> memberList = new ArrayList<>();
        try{
            Cursor cursor = mDB.rawQuery("select * from t_groupMember where groupID=?", new String[]{groupID});
            while(cursor.moveToNext()){
                GroupMember groupMember = getMember(cursor);
                memberList.add(groupMember);
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return memberList;
    }
    public static GroupMember queryMember(String groupID, String memberID){
        GroupMember groupMember = null;
        try{
            Cursor cursor = mDB.rawQuery("select * from t_groupMember where groupID=? and memberID=?", new String[]{groupID,memberID});
            if(cursor.moveToNext())
                groupMember = getMember(cursor);
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return groupMember;
    }

    private static LitappInfo getLitappInfo(Cursor cursor){
        LitappInfo litappInfo = new LitappInfo();
        litappInfo.target = cursor.getString(1);
        litappInfo.name = cursor.getString(2);
        litappInfo.displayName = cursor.getString(3);
        litappInfo.portrait = cursor.getString(4);
        litappInfo.theme = cursor.getString(5);
        litappInfo.url = cursor.getString(6);
        litappInfo.info = cursor.getString(7);
        return litappInfo;
    }
    private static GroupInfo getGroupInfo(Cursor cursor){
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.target = cursor.getString(1);
        groupInfo.name = cursor.getString(2);
        groupInfo.portrait = cursor.getString(3);
        groupInfo.owner = cursor.getString(4);
        groupInfo.type = GroupInfo.GroupType.type(cursor.getInt(5));
        groupInfo.memberCount = cursor.getInt(6);
        groupInfo.extra = cursor.getString(7);
        groupInfo.updateDt = cursor.getLong(8);
        groupInfo.fav = cursor.getInt(9);
        groupInfo.mute = cursor.getInt(10);
        groupInfo.joinType = cursor.getInt(11);
        groupInfo.passType = cursor.getInt(12);
        groupInfo.privateChat = cursor.getInt(13);
        groupInfo.maxMemberCount = cursor.getInt(14);
        groupInfo.showAlias = cursor.getInt(15);
        return groupInfo;
    }
    private static UserInfo getUserInfo(Cursor cursor){
        UserInfo userInfo = new UserInfo();
        userInfo.uid = cursor.getString(cursor.getColumnIndex("osnID"));
        userInfo.name = cursor.getString(cursor.getColumnIndex("name"));
        userInfo.portrait = cursor.getString(cursor.getColumnIndex("portrait"));
        userInfo.displayName = cursor.getString(cursor.getColumnIndex("displayName"));
        userInfo.urlSpace = cursor.getString(cursor.getColumnIndex("urlSpace"));
        userInfo.friendAlias = cursor.getString(cursor.getColumnIndex("remarks"));
        return userInfo;
    }
    private static Message getMessage(Cursor cursor){
        try {
            Message message = new Message();
            message.messageId = cursor.getLong(1);
            message.sender = cursor.getString(2);
            message.conversation = new Conversation(Conversation.ConversationType.type(cursor.getInt(3)), cursor.getString(4), 0);
            message.direction = MessageDirection.direction(cursor.getInt(5));
            message.status = MessageStatus.status(cursor.getInt(6));
            message.messageUid = cursor.getLong(7);
            message.serverTime = cursor.getLong(8);
            message.messageHash = cursor.getString(11);
            String data = cursor.getString(10);
            int msgType = cursor.getInt(9);
            JSONArray array;
            JSONObject json = JSON.parseObject(data);
            switch (msgType) {
                case MessageContentType.ContentType_Text:
                    TextMessageContent textMessageContent = new TextMessageContent();
                    textMessageContent.setContent(json.getString("text"));
                    message.content = textMessageContent;
                    break;
                case MessageContentType.ContentType_Friend_Added:
                    message.content = new FriendAddedMessageContent();
                    break;
                case MessageContentType.ContentType_Friend_Greeting:
                    message.content = new FriendGreetingMessageContent();
                    break;
                case MessageContentType.ContentType_CREATE_GROUP:
                    CreateGroupNotificationContent groupNotificationContent = new CreateGroupNotificationContent();
                    groupNotificationContent.creator = json.getString("creator");
                    groupNotificationContent.groupName = json.getString("groupName");
                    message.content = groupNotificationContent;
                    break;
                case MessageContentType.ContentType_Image:
                    ImageMessageContent imageMessageContent = new ImageMessageContent();
                    imageMessageContent.localPath = json.getString("localPath");
                    imageMessageContent.remoteUrl = json.getString("remoteUrl");
                    imageMessageContent.imageWidth = json.getDoubleValue("width");
                    imageMessageContent.imageHeight = json.getDoubleValue("height");
                    message.content = imageMessageContent;
                    break;
                case MessageContentType.ContentType_File:
                    FileMessageContent fileMessageContent = new FileMessageContent();
                    fileMessageContent.localPath = json.getString("localPath");
                    fileMessageContent.remoteUrl = json.getString("remoteUrl");
                    fileMessageContent.setName(json.getString("name"));
                    fileMessageContent.setSize(json.getIntValue("size"));
                    message.content = fileMessageContent;
                    break;
                case MessageContentType.ContentType_Voice:
                    SoundMessageContent soundMessageContent = new SoundMessageContent();
                    soundMessageContent.localPath = json.getString("localPath");
                    soundMessageContent.remoteUrl = json.getString("remoteUrl");
                    soundMessageContent.setDuration(json.getIntValue("duration"));
                    message.content = soundMessageContent;
                    break;
                case MessageContentType.ContentType_Video:
                    VideoMessageContent videoMessageContent = new VideoMessageContent();
                    videoMessageContent.localPath = json.getString("localPath");
                    videoMessageContent.remoteUrl = json.getString("remoteUrl");
                    if(json.containsKey("thumbnail"))
                        videoMessageContent.setThumbnailBytes(Base64.decode(json.getString("thumbnail"),0));
                    message.content = videoMessageContent;
                    break;
                case MessageContentType.ContentType_Sticker:
                    StickerMessageContent stickerMessageContent = new StickerMessageContent();
                    stickerMessageContent.localPath = json.getString("localPath");
                    stickerMessageContent.remoteUrl = json.getString("remoteUrl");
                    stickerMessageContent.height = json.getIntValue("height");
                    stickerMessageContent.width = json.getIntValue("width");
                    message.content = stickerMessageContent;
                    break;
                case MessageContentType.ContentType_KICKOF_GROUP_MEMBER:
                    KickoffGroupMemberNotificationContent kickoffGroupMemberNotificationContent = new KickoffGroupMemberNotificationContent();
                    kickoffGroupMemberNotificationContent.kickedMembers = new ArrayList<>();
                    kickoffGroupMemberNotificationContent.operator = json.getString("operator");
                    array = json.getJSONArray("members");
                    if(array != null)
                        kickoffGroupMemberNotificationContent.kickedMembers.addAll(array.toJavaList(String.class));
                    message.content = kickoffGroupMemberNotificationContent;
                    break;
                case MessageContentType.ContentType_ADD_GROUP_MEMBER:
                    AddGroupMemberNotificationContent addGroupMemberNotificationContent = new AddGroupMemberNotificationContent();
                    addGroupMemberNotificationContent.invitor = json.getString("invitor");
                    addGroupMemberNotificationContent.invitees = new ArrayList<>();
                    array = json.getJSONArray("invitees");
                    if(array != null)
                        addGroupMemberNotificationContent.invitees.addAll(array.toJavaList(String.class));
                    message.content = addGroupMemberNotificationContent;
                    break;
                case MessageContentType.ContentType_QUIT_GROUP:
                    QuitGroupNotificationContent quitGroupNotificationContent = new QuitGroupNotificationContent();
                    quitGroupNotificationContent.operator = json.getString("operator");
                    message.content = quitGroupNotificationContent;
                    break;
                case MessageContentType.ContentType_DISMISS_GROUP:
                    DismissGroupNotificationContent dismissGroupNotificationContent = new DismissGroupNotificationContent();
                    dismissGroupNotificationContent.operator = json.getString("operator");
                    message.content = dismissGroupNotificationContent;
                    break;
                case MessageContentType.ContentType_TRANSFER_GROUP_OWNER:
                    TransferGroupOwnerNotificationContent transferGroupOwnerNotificationContent = new TransferGroupOwnerNotificationContent();
                    transferGroupOwnerNotificationContent.operator = json.getString("operator");
                    transferGroupOwnerNotificationContent.newOwner = json.getString("newOwner");
                    message.content = transferGroupOwnerNotificationContent;
                    break;
                case MessageContentType.ContentType_Card:
                    CardMessageContent cardMessageContent = new CardMessageContent();
                    cardMessageContent.setType(json.getIntValue("cardType"));
                    cardMessageContent.setTarget(json.getString("target"));
                    cardMessageContent.setName(json.getString("name"));
                    cardMessageContent.setDisplayName(json.getString("displayName"));
                    cardMessageContent.setPortrait(json.getString("portrait"));
                    cardMessageContent.setTheme(json.getString("theme"));
                    cardMessageContent.setUrl(json.getString("url"));
                    cardMessageContent.setInfo(json.getString("info"));
                    message.content = cardMessageContent;
                    break;
                default:
                    message.content = new UnknownMessageContent();
                    logInfo("unknown msgType: " + msgType);
                    break;
            }
            return message;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    private static ConversationInfo getConversation(Cursor cursor){
        ConversationInfo conversationInfo = new ConversationInfo();
        conversationInfo.conversation = new Conversation(Conversation.ConversationType.type(cursor.getInt(1)),cursor.getString(2),cursor.getInt(3));
        conversationInfo.lastMessage = getLastMessage(conversationInfo.conversation);
        conversationInfo.timestamp = cursor.getLong(4);
        conversationInfo.draft = cursor.getString(5);
        conversationInfo.unreadCount = new UnreadCount();
        conversationInfo.unreadCount.unread = cursor.getInt(6);
        conversationInfo.unreadCount.unreadMention = cursor.getInt(7);
        conversationInfo.unreadCount.unreadMentionAll = cursor.getInt(8);
        conversationInfo.isTop = cursor.getInt(9) != 0;
        conversationInfo.isSilent = cursor.getInt(10) != 0;
        return conversationInfo;
    }
    private static GroupMember getMember(Cursor cursor){
        GroupMember groupMember = new GroupMember();
        groupMember.groupId = cursor.getString(1);
        groupMember.memberId = cursor.getString(2);
        groupMember.alias = cursor.getString(3);
        groupMember.type = GroupMember.GroupMemberType.type(cursor.getInt(4));
        groupMember.updateDt = cursor.getLong(5);
        groupMember.createDt = cursor.getLong(6);
        return groupMember;
    }
    private static FriendRequest getFriendRequest(Cursor cursor){
        FriendRequest request = new FriendRequest();
        request.type = cursor.getInt(1);
        request.direction = cursor.getInt(2);
        request.target = cursor.getString(3);
        request.originalUser = cursor.getString(4);
        request.userID = cursor.getString(5);
        request.reason = cursor.getString(6);
        request.status = cursor.getInt(7);
        request.readStatus = cursor.getInt(8);
        request.timestamp = cursor.getLong(9);
        return request;
    }

}
