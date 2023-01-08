package top.test.onesignal;

import android.app.NotificationManager;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.NotificationLimitManager;
import top.oneconnectapi.onesignal.ShadowCustomTabsClient;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowGenerateNotification;
import top.oneconnectapi.onesignal.ShadowNotificationLimitManager;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorFCM;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.example.BlankActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.NotificationBundleProcessor_ProcessFromFCMIntentService;
import static top.oneconnectapi.onesignal.ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse;
import static top.test.onesignal.GenerateNotificationRunner.getBaseNotifBundle;
import static top.test.onesignal.TestHelpers.afterTestCleanup;
import static top.test.onesignal.TestHelpers.threadAndTaskWait;
import static junit.framework.Assert.assertEquals;

import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.ShadowCustomTabsClient;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowGenerateNotification;
import top.oneconnectapi.onesignal.ShadowNotificationLimitManager;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorFCM;
import top.oneconnectapi.onesignal.StaticResetHelper;

@Config(packageName = "top.oneconnectapi.onesignal.example",
        shadows = {
            ShadowNotificationLimitManager.class,
            ShadowPushRegistratorFCM.class,
            ShadowOSUtils.class,
            ShadowOneSignalRestClient.class,
            ShadowCustomTabsClient.class,
            ShadowCustomTabsSession.class,
        },
        sdk = 26
)
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class NotificationLimitManagerRunner {

   private BlankActivity blankActivity;
   private NotificationManager notificationManager;

   @BeforeClass // Runs only once, before any tests
   public static void setUpClass() throws Exception {
      ShadowLog.stream = System.out;
      TestHelpers.beforeTestSuite();
      StaticResetHelper.saveStaticValues();
   }

   @Before
   public void beforeEachTest() throws Exception {
      ActivityController<BlankActivity> blankActivityController = Robolectric.buildActivity(BlankActivity.class).create();
      blankActivity = blankActivityController.get();
      notificationManager = (NotificationManager)blankActivity.getSystemService(Context.NOTIFICATION_SERVICE);
      TestHelpers.beforeTestInitAndCleanup();

      // Set remote_params GET response
      ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse();
      OneSignal.setAppId("b2f7f966-d8cc-11e4-bed1-df8f05be55ba");
      OneSignal.initWithContext(blankActivity);
      TestHelpers.threadAndTaskWait();
   }

   @After
   public void afterEachTest() throws Exception {
      TestHelpers.afterTestCleanup();
   }

   @Test
   public void clearStandardMakingRoomForOneWhenAtLimit() throws Throwable {
      createNotification(blankActivity, 1);
      createNotification(blankActivity, 2);

      OneSignalPackagePrivateHelper.NotificationLimitManager.clearOldestOverLimitStandard(blankActivity, 1);
      TestHelpers.threadAndTaskWait();

      assertEquals(1, notificationManager.getActiveNotifications().length);
      assertEquals(2, notificationManager.getActiveNotifications()[0].getId());
   }

   @Test
   public void clearStandardShouldNotCancelAnyNotificationsWhenUnderLimit() throws Throwable {
      createNotification(blankActivity, 1);

      OneSignalPackagePrivateHelper.NotificationLimitManager.clearOldestOverLimitStandard(blankActivity, 1);
      TestHelpers.threadAndTaskWait();

      assertEquals(1, notificationManager.getActiveNotifications().length);
   }

   @Test
   public void clearStandardShouldSkipGroupSummaryNotification() throws Throwable {
      NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(blankActivity, "");
      notifBuilder.setWhen(1);
      // We should not clear summary notifications, these will go away if all child notifications are canceled
      notifBuilder.setGroupSummary(true);
      NotificationManagerCompat.from(blankActivity).notify(1, notifBuilder.build());

      createNotification(blankActivity, 2);

      OneSignalPackagePrivateHelper.NotificationLimitManager.clearOldestOverLimitStandard(blankActivity, 1);
      TestHelpers.threadAndTaskWait();

      assertEquals(1 , notificationManager.getActiveNotifications()[0].getId());
   }

   // Helper Methods
   private static void createNotification(Context context, int notifId) {
      NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context, "");
      notifBuilder.setWhen(notifId); // Android automatically sets this normally.
      NotificationManagerCompat.from(context).notify(notifId, notifBuilder.build());
   }

   @Test
   @Config(shadows = { ShadowGenerateNotification.class })
   public void clearFallbackMakingRoomForOneWhenAtLimit() throws Exception {
      OneSignalPackagePrivateHelper.NotificationBundleProcessor_ProcessFromFCMIntentService(blankActivity,  GenerateNotificationRunner.getBaseNotifBundle("UUID1"));
      TestHelpers.threadAndTaskWait();
      OneSignalPackagePrivateHelper.NotificationBundleProcessor_ProcessFromFCMIntentService(blankActivity,  GenerateNotificationRunner.getBaseNotifBundle("UUID2"));
      TestHelpers.threadAndTaskWait();

      OneSignalPackagePrivateHelper.NotificationLimitManager.clearOldestOverLimitFallback(blankActivity, 1);
      TestHelpers.threadAndTaskWait();

      assertEquals(1, notificationManager.getActiveNotifications().length);
   }

   @Test
   @Config(shadows = { ShadowGenerateNotification.class })
   public void clearFallbackShouldNotCancelAnyNotificationsWhenUnderLimit() throws Exception {
      OneSignalPackagePrivateHelper.NotificationBundleProcessor_ProcessFromFCMIntentService(blankActivity,  GenerateNotificationRunner.getBaseNotifBundle("UUID1"));
      TestHelpers.threadAndTaskWait();

      OneSignalPackagePrivateHelper.NotificationLimitManager.clearOldestOverLimitFallback(blankActivity, 1);
      TestHelpers.threadAndTaskWait();

      assertEquals(1, notificationManager.getActiveNotifications().length);
   }
}
