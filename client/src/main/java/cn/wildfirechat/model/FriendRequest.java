/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 14/12/2017.
 */

public class FriendRequest implements Parcelable {
    public int type;
    public int direction;
    public String target; //friendID, groupID
    public String originalUser; //originalUser
    public String userID;
    public String reason;
    public int status;
    public int readStatus;
    public long timestamp;

    public static int RequestType_Friend = 0;
    public static int RequestType_InviteGroup = 1;
    public static int RequestType_ApplyMember = 2;

    public static int RequestStatus_Sent = 0;
    public static int RequestStatus_Accepted = 1;
    public static int RequestStatus_Rejected = 3;
    public static int Direction_Sent = 0;
    public static int Direction_Recv = 1;

    public FriendRequest() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type);
        dest.writeInt(this.direction);
        dest.writeString(this.target);
        dest.writeString(this.originalUser);
        dest.writeString(this.userID);
        dest.writeString(this.reason);
        dest.writeInt(this.status);
        dest.writeInt(this.readStatus);
        dest.writeLong(this.timestamp);
    }

    protected FriendRequest(Parcel in) {
        this.type = in.readInt();
        this.direction = in.readInt();
        this.target = in.readString();
        this.originalUser = in.readString();
        this.userID = in.readString();
        this.reason = in.readString();
        this.status = in.readInt();
        this.readStatus = in.readInt();
        this.timestamp = in.readLong();
    }

    public static final Creator<FriendRequest> CREATOR = new Creator<FriendRequest>() {
        @Override
        public FriendRequest createFromParcel(Parcel source) {
            return new FriendRequest(source);
        }

        @Override
        public FriendRequest[] newArray(int size) {
            return new FriendRequest[size];
        }
    };
}
