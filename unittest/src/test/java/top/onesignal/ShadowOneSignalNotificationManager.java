package top.onesignal;

import android.content.Context;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import top.oneconnectapi.onesignal.OneSignalNotificationManager;

@Implements(OneSignalNotificationManager.class)
public class ShadowOneSignalNotificationManager {

   private static boolean notificationChannelEnabled = true;

   public static void setNotificationChannelEnabled(boolean notificationChannelEnabled) {
      ShadowOneSignalNotificationManager.notificationChannelEnabled = notificationChannelEnabled;
   }

   @Implementation
   public static boolean areNotificationsEnabled(Context context, String channelId) {
      return notificationChannelEnabled;
   }

   /**
    * Reset all static values (should be called before each test)
    */
   public static void resetStatics() {
      notificationChannelEnabled = true;
   }
}