package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class LitappInfo implements Parcelable{
    public String target;
    public String name;
    public String displayName;
    public String portrait;
    public String theme;
    public String url;
    public String info;

    public LitappInfo() {
    }
    public LitappInfo(String target){
        this.target = target;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.target);
        dest.writeString(this.name);
        dest.writeString(this.displayName);
        dest.writeString(this.portrait);
        dest.writeString(this.theme);
        dest.writeString(this.url);
        dest.writeString(this.info);
    }

    protected LitappInfo(Parcel in) {
        this.target = in.readString();
        this.name = in.readString();
        this.displayName = in.readString();
        this.portrait = in.readString();
        this.theme = in.readString();
        this.url = in.readString();
        this.info = in.readString();
    }

    public static final Creator<LitappInfo> CREATOR = new Creator<LitappInfo>() {
        @Override
        public LitappInfo createFromParcel(Parcel source) {
            return new LitappInfo(source);
        }

        @Override
        public LitappInfo[] newArray(int size) {
            return new LitappInfo[size];
        }
    };
}
