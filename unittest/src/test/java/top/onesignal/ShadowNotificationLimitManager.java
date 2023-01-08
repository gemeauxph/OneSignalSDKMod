package top.onesignal;

import org.robolectric.annotation.Implements;

import top.oneconnectapi.onesignal.NotificationLimitManager;

@Implements(NotificationLimitManager.class)
public class ShadowNotificationLimitManager {
   private static int MAX_NUMBER_OF_NOTIFICATIONS_INT = 2;

   public static int getMaxNumberOfNotificationsInt() {
      return MAX_NUMBER_OF_NOTIFICATIONS_INT;
   }

   public static String getMaxNumberOfNotificationsString() {
      return Integer.toString(MAX_NUMBER_OF_NOTIFICATIONS_INT);
   }
}
