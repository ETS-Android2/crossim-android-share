package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ImsInfo implements Parcelable {
    public String urlSpace;

    public ImsInfo() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.urlSpace);
    }

    protected ImsInfo(Parcel in) {
        this.urlSpace = in.readString();
    }

    public static final Creator<ImsInfo> CREATOR = new Creator<ImsInfo>() {
        @Override
        public ImsInfo createFromParcel(Parcel source) {
            return new ImsInfo(source);
        }

        @Override
        public ImsInfo[] newArray(int size) {
            return new ImsInfo[size];
        }
    };
}
