package cn.wildfire.chat.kit;

import androidx.core.app.NotificationCompat;

public class UiWrapper {
    public interface UiCallback{
        void getNotificationIcon(NotificationCompat.Builder builder);
    }
    private static UiCallback uiCallback = null;
    public static void setUiCallback(UiCallback callback){
        uiCallback = callback;
    }
    public static void getNotificationIcon(NotificationCompat.Builder builder){
        if(uiCallback != null)
            uiCallback.getNotificationIcon(builder);
    }
}
