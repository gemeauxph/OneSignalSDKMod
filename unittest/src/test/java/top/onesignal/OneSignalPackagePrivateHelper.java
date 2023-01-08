package top.onesignal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hms.push.RemoteMessage;

import top.oneconnectapi.onesignal.BadgeCountUpdater;
import top.oneconnectapi.onesignal.BundleCompat;
import top.oneconnectapi.onesignal.BundleCompatFactory;
import top.oneconnectapi.onesignal.DelayedConsentInitializationParameters;
import top.oneconnectapi.onesignal.FCMBroadcastReceiver;
import top.oneconnectapi.onesignal.GenerateNotification;
import top.oneconnectapi.onesignal.JSONUtils;
import top.oneconnectapi.onesignal.NotificationBundleProcessor;
import top.oneconnectapi.onesignal.NotificationChannelManager;
import top.oneconnectapi.onesignal.NotificationLimitManager;
import top.oneconnectapi.onesignal.NotificationOpenedProcessor;
import top.oneconnectapi.onesignal.NotificationPayloadProcessorHMS;
import top.oneconnectapi.onesignal.NotificationSummaryManager;
import top.oneconnectapi.onesignal.OSInAppMessageAction;
import top.oneconnectapi.onesignal.OSInAppMessageController;
import top.oneconnectapi.onesignal.OSInAppMessageInternal;
import top.oneconnectapi.onesignal.OSInAppMessageRedisplayStats;
import top.oneconnectapi.onesignal.OSNotification;
import top.oneconnectapi.onesignal.OSNotificationController;
import top.oneconnectapi.onesignal.OSNotificationFormatHelper;
import top.oneconnectapi.onesignal.OSNotificationGenerationJob;
import top.oneconnectapi.onesignal.OSNotificationRestoreWorkManager;
import top.oneconnectapi.onesignal.OSObservable;
import top.oneconnectapi.onesignal.OSRemoteParamController;
import top.oneconnectapi.onesignal.OSSessionManager;
import top.oneconnectapi.onesignal.OSSharedPreferences;
import top.oneconnectapi.onesignal.OSSharedPreferencesWrapper;
import top.oneconnectapi.onesignal.OSSyncService;
import top.oneconnectapi.onesignal.OSTime;
import top.oneconnectapi.onesignal.OSTrigger;
import top.oneconnectapi.onesignal.OSUtils;
import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.OneSignalDb;
import top.oneconnectapi.onesignal.OneSignalDbContract;
import top.oneconnectapi.onesignal.OneSignalHmsEventBridge;
import top.oneconnectapi.onesignal.OneSignalPrefs;
import top.oneconnectapi.onesignal.OneSignalRemoteParams;
import top.oneconnectapi.onesignal.OneSignalRestClient;
import top.oneconnectapi.onesignal.OneSignalStateSynchronizer;
import top.oneconnectapi.onesignal.PushRegistratorFCM;
import top.oneconnectapi.onesignal.UserState;
import top.oneconnectapi.onesignal.UserStateSynchronizer;
import top.oneconnectapi.onesignal.WebViewManager;
import top.oneconnectapi.onesignal.influence.data.OSTrackerFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.robolectric.util.Scheduler;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static top.test.onesignal.TestHelpers.threadAndTaskWait;
import static org.robolectric.Shadows.shadowOf;

public class OneSignalPackagePrivateHelper {

   public static final String GOOGLE_SENT_TIME_KEY = OSNotificationController.GOOGLE_SENT_TIME_KEY;
   public static final String GOOGLE_TTL_KEY = OSNotificationController.GOOGLE_TTL_KEY;

   public static final String IN_APP_MESSAGES_JSON_KEY = OSInAppMessageController.IN_APP_MESSAGES_JSON_KEY;

   public static final long MIN_ON_SESSION_TIME_MILLIS = OneSignal.MIN_ON_SESSION_TIME_MILLIS;

   private static final String LOGCAT_TAG = "OS_PACKAGE_HELPER";

   private static abstract class RunnableArg<T> {
      abstract void run(T object) throws Exception;
   }

   private static void processNetworkHandles(RunnableArg runnable) throws Exception {
      Set<Map.Entry<Integer, UserStateSynchronizer.NetworkHandlerThread>> entrySet;

      entrySet = OneSignalStateSynchronizer.getPushStateSynchronizer().networkHandlerThreads.entrySet();
      for (Map.Entry<Integer, UserStateSynchronizer.NetworkHandlerThread> handlerThread : entrySet)
         runnable.run(handlerThread.getValue());

      entrySet = OneSignalStateSynchronizer.getEmailStateSynchronizer().networkHandlerThreads.entrySet();
      for (Map.Entry<Integer, UserStateSynchronizer.NetworkHandlerThread> handlerThread : entrySet)
         runnable.run(handlerThread.getValue());

      entrySet = OneSignalStateSynchronizer.getSMSStateSynchronizer().networkHandlerThreads.entrySet();
      for (Map.Entry<Integer, UserStateSynchronizer.NetworkHandlerThread> handlerThread : entrySet)
         runnable.run(handlerThread.getValue());
   }

   private static boolean startedRunnable;
   public static boolean runAllNetworkRunnables() throws Exception {
      startedRunnable = false;

      RunnableArg runnable = new RunnableArg<UserStateSynchronizer.NetworkHandlerThread>() {
         @Override
         void run(UserStateSynchronizer.NetworkHandlerThread handlerThread) throws Exception {
            synchronized (handlerThread.mHandler) {
               Scheduler scheduler = shadowOf(handlerThread.getLooper()).getScheduler();
               while (scheduler.runOneTask())
                  startedRunnable = true;
            }
         }
      };

      processNetworkHandles(runnable);

      return startedRunnable;
   }

   private static boolean isExecutingRunnable(Scheduler scheduler) throws Exception {
      Field isExecutingRunnableField = Scheduler.class.getDeclaredField("isExecutingRunnable");
      isExecutingRunnableField.setAccessible(true);
      return (Boolean)isExecutingRunnableField.get(scheduler);
   }

   public static void OneSignal_sendPurchases(JSONArray purchases, boolean newAsExisting, OneSignalRestClient.ResponseHandler responseHandler) {
      OneSignal.sendPurchases(purchases, newAsExisting, responseHandler);
   }

   /**
    * Only necessary when not fully init OneSignal SDK
    * initWithContext required to setup a notification extension service
    */
   public static void OneSignal_setupNotificationServiceExtension() {
      OSNotificationController.setupNotificationServiceExtension(OneSignal.appContext);
   }

   public static void OneSignal_savePrivacyConsentRequired(boolean required) {
      OneSignal_getRemoteParamController().savePrivacyConsentRequired(required);
   }

   public static OSRemoteParamController OneSignal_getRemoteParamController() {
      return OneSignal.getRemoteParamController();
   }

   public static OSSessionManager.SessionListener OneSignal_getSessionListener() {
      return OneSignal.getSessionListener();
   }

   public static void OneSignal_setTime(OSTime time) {
      OneSignal.setTime(time);
   }

   public static void OneSignal_setSharedPreferences(OSSharedPreferences preferences) {
      OneSignal.setSharedPreferences(preferences);
   }

   public static void OneSignal_setSessionManager(OSSessionManager sessionManager) {
      OneSignal.setSessionManager(sessionManager);
   }

   public static void OneSignal_setTrackerFactory(OSTrackerFactory trackerFactory) {
      OneSignal.setTrackerFactory(trackerFactory);
   }

   public static JSONObject bundleAsJSONObject(Bundle bundle) {
      return NotificationBundleProcessor.bundleAsJSONObject(bundle);
   }

   public static String toUnescapedEUIDString(JSONObject json) {
      return JSONUtils.toUnescapedEUIDString(json);
   }

   public static void OneSignal_handleNotificationOpen(Activity context, final JSONArray data, final String notificationId) {
      OneSignal.handleNotificationOpen(context, data, notificationId);
   }

   public static BigInteger OneSignal_getAccentColor(JSONObject fcmJson) {
      return GenerateNotification.getAccentColor(fcmJson);
   }

   public static BundleCompat createInternalPayloadBundle(Bundle bundle) {
      BundleCompat retBundle = BundleCompatFactory.getInstance();
      retBundle.putString("json_payload", OneSignalPackagePrivateHelper.bundleAsJSONObject(bundle).toString());
      return retBundle;
   }

   public static void NotificationBundleProcessor_ProcessFromFCMIntentService(Context context, Bundle bundle) {
      NotificationBundleProcessor.processFromFCMIntentService(context, createInternalPayloadBundle(bundle));
   }

   public static void NotificationBundleProcessor_ProcessFromFCMIntentService_NoWrap(Context context, BundleCompat bundle) {
      NotificationBundleProcessor.processFromFCMIntentService(context, bundle);
   }

   public static void FCMBroadcastReceiver_processBundle(Context context, Bundle bundle) {
      OneSignalPackagePrivateHelper.ProcessBundleReceiverCallback bundleReceiverCallback = new OneSignalPackagePrivateHelper.ProcessBundleReceiverCallback() {
         @Override
         public void onBundleProcessed(@Nullable OneSignalPackagePrivateHelper.ProcessedBundleResult processedResult) {
         }
      };

      FCMBroadcastReceiver_processBundle(context, bundle, bundleReceiverCallback);
   }

   public static void FCMBroadcastReceiver_processBundle(Context context, Bundle bundle, OneSignalPackagePrivateHelper.ProcessBundleReceiverCallback bundleReceiverCallback) {
      NotificationBundleProcessor.processBundleFromReceiver(context, bundle, bundleReceiverCallback);
   }

   public static void FCMBroadcastReceiver_onReceived_withIntent(Context context, Intent intent) {
      FCMBroadcastReceiver receiver = new FCMBroadcastReceiver();
      intent.setAction("com.google.android.c2dm.intent.RECEIVE");
      receiver.onReceive(context, intent);
   }

   public static void FCMBroadcastReceiver_onReceived_withBundle(Context context, Bundle bundle) throws Exception {
      FCMBroadcastReceiver receiver = new FCMBroadcastReceiver();
      Intent intent = new Intent();
      intent.setAction("com.google.android.c2dm.intent.RECEIVE");
      intent.putExtras(bundle);
      receiver.onReceive(context,intent);
      threadAndTaskWait();
   }

   public static void HMSEventBridge_onMessageReceive(final Context context, final RemoteMessage message) {
      OneSignalHmsEventBridge.onMessageReceived(context, message);
   }

   public static void HMSProcessor_processDataMessageReceived(final Context context, final String jsonStrPayload) {
      NotificationPayloadProcessorHMS.processDataMessageReceived(context, jsonStrPayload);
   }

   public static int NotificationBundleProcessor_Process(Context context, boolean restoring, JSONObject jsonPayload) {
      OSNotificationGenerationJob notificationJob = new OSNotificationGenerationJob(context, jsonPayload);
      notificationJob.setRestoring(restoring);
      return NotificationBundleProcessor.processJobForDisplay(notificationJob, true);
   }

   public static class ProcessBundleReceiverCallback implements top.oneconnectapi.onesignal.NotificationBundleProcessor.ProcessBundleReceiverCallback {

      @Override
      public void onBundleProcessed(@Nullable top.oneconnectapi.onesignal.NotificationBundleProcessor.ProcessedBundleResult processedResult) {
         onBundleProcessed(new ProcessedBundleResult(processedResult));
      }

      public void onBundleProcessed(@Nullable ProcessedBundleResult processedResult) {

      }
   }

   public static class ProcessedBundleResult extends NotificationBundleProcessor.ProcessedBundleResult {
      top.oneconnectapi.onesignal.NotificationBundleProcessor.ProcessedBundleResult processedResult;

      public ProcessedBundleResult(top.oneconnectapi.onesignal.NotificationBundleProcessor.ProcessedBundleResult processedResult) {
         this.processedResult = processedResult;
      }

      public boolean isProcessed() {
         return processedResult.processed();
      }
   }

   public static class NotificationTable extends OneSignalDbContract.NotificationTable {
   }

   public static class InAppMessageTable extends OneSignalDbContract.InAppMessageTable {
   }

   public static class OSNotificationRestoreWorkManager extends top.oneconnectapi.onesignal.OSNotificationRestoreWorkManager {
      public static int getDEFAULT_TTL_IF_NOT_IN_PAYLOAD() {
         return DEFAULT_TTL_IF_NOT_IN_PAYLOAD;
      }
   }

   public static class OSNotificationGenerationJob extends top.oneconnectapi.onesignal.OSNotificationGenerationJob {
      OSNotificationGenerationJob(Context context) {
         super(context);
      }

      OSNotificationGenerationJob(Context context, JSONObject jsonPayload) {
         super(context, jsonPayload);
      }
   }

   public static class OneSignalSyncServiceUtils_SyncRunnable extends OSSyncService.SyncRunnable {
      @Override
      protected void stopSync() {
      }
   }

   public static class FCMBroadcastReceiver extends top.oneconnectapi.onesignal.FCMBroadcastReceiver {}

   public static class PushRegistratorFCM extends top.oneconnectapi.onesignal.PushRegistratorFCM {
      public PushRegistratorFCM(@NonNull Context context, @Nullable Params params) {
         super(context, params);
      }
   }

   public static class OneSignalRestClient extends top.oneconnectapi.onesignal.OneSignalRestClient {
      public static abstract class ResponseHandler extends top.oneconnectapi.onesignal.OneSignalRestClient.ResponseHandler {
         @Override
         public void onSuccess(String response) {}
         @Override
         public void onFailure(int statusCode, String response, Throwable throwable) {}
      }
   }

   public static String NotificationChannelManager_createNotificationChannel(Context context, JSONObject payload) {
      OSNotificationGenerationJob notificationJob = new OSNotificationGenerationJob(context);
      notificationJob.setJsonPayload(payload);
      return NotificationChannelManager.createNotificationChannel(notificationJob);
   }

   public static void NotificationChannelManager_processChannelList(Context context, JSONArray jsonArray) {
      NotificationChannelManager.processChannelList(context, jsonArray);
   }

   public static void NotificationOpenedProcessor_processFromContext(Activity context, Intent intent) {
      NotificationOpenedProcessor.processFromContext(context, intent);
   }

   public static void NotificationSummaryManager_updateSummaryNotificationAfterChildRemoved(Context context, OneSignalDb db, String group, boolean dismissed) {
      NotificationSummaryManager.updateSummaryNotificationAfterChildRemoved(context, db, group, dismissed);
   }

   public class TestOneSignalPrefs extends OneSignalPrefs {}

   public static void OneSignal_onAppLostFocus() {
      OneSignal.onAppLostFocus();
   }

   public static DelayedConsentInitializationParameters OneSignal_delayedInitParams() {
      return OneSignal.getDelayedInitParams();
   }

   public static ConcurrentLinkedQueue<Runnable> OneSignal_taskQueueWaitingForInit() {
      return OneSignal.getTaskRemoteController().getTaskQueueWaitingForInit();
   }

   public static void OneSignal_OSTaskController_ShutdownNow() {
      OneSignal.getTaskRemoteController().shutdownNow();
      OneSignal.getTaskController().shutdownNow();
   }

   public static boolean OneSignal_requiresUserPrivacyConsent() {
      return OneSignal.requiresUserPrivacyConsent();
   }

   public static boolean OneSignal_locationShared() {
      return OneSignal.isLocationShared();
   }

   public static boolean OneSignal_areNotificationsEnabledForSubscribedState() {
      return OneSignal.areNotificationsEnabledForSubscribedState();
   }

   public static boolean OneSignal_getDisableGMSMissingPrompt() {
      return OneSignal.getDisableGMSMissingPrompt();
   }

   public static String OneSignal_appId() {
      return OneSignal.appId;
   }

   public static boolean OneSignal_isInForeground() {
      return OneSignal.isInForeground();
   }

   static public class OSSharedPreferencesWrapper extends top.oneconnectapi.onesignal.OSSharedPreferencesWrapper {}

   static public class RemoteOutcomeParams extends OneSignalRemoteParams.InfluenceParams {

      public RemoteOutcomeParams() {
         this(true, true, true);
      }

      public RemoteOutcomeParams(boolean direct, boolean indirect, boolean unattributed) {
         directEnabled = direct;
         indirectEnabled = indirect;
         unattributedEnabled = unattributed;
      }
   }

   public static class BadgeCountUpdater extends top.oneconnectapi.onesignal.BadgeCountUpdater {
      public static void update(OneSignalDb db, Context context) {
         top.oneconnectapi.onesignal.BadgeCountUpdater.update(db, context);
      }
   }

   public static class NotificationLimitManager extends top.oneconnectapi.onesignal.NotificationLimitManager {
      public static void clearOldestOverLimitFallback(Context context, int notificationsToMakeRoomFor) {
         top.oneconnectapi.onesignal.NotificationLimitManager.clearOldestOverLimitFallback(context, notificationsToMakeRoomFor);
      }

      public static void clearOldestOverLimitStandard(Context context, int notificationsToMakeRoomFor) throws Throwable {
         top.oneconnectapi.onesignal.NotificationLimitManager.clearOldestOverLimitStandard(context, notificationsToMakeRoomFor);
      }
   }

   public static class OneSignalDbContract extends top.oneconnectapi.onesignal.OneSignalDbContract {}

   /** In-App Messaging Helpers */

   public static class OSTestInAppMessageInternal extends OSInAppMessageInternal {

      public OSTestInAppMessageInternal(@NonNull String messageId, int displaysQuantity, long lastDisplayTime, boolean displayed, Set<String> clickIds) {
         super(messageId, clickIds, displayed, new OSInAppMessageRedisplayStats(displaysQuantity, lastDisplayTime));
      }

      OSTestInAppMessageInternal(JSONObject json) throws JSONException {
         super(json);
      }

      public void setMessageId(String messageId) {
         this.messageId = messageId;
      }

      @Override
      protected ArrayList<ArrayList<OSTrigger>> parseTriggerJson(JSONArray triggersJson) throws JSONException {
         ArrayList<ArrayList<OSTrigger>> parsedTriggers = new ArrayList<>();

         for (int i = 0; i < triggersJson.length(); i++) {
            JSONArray ands = triggersJson.getJSONArray(i);

            ArrayList<OSTrigger> parsed = new ArrayList<>();

            for (int j = 0; j < ands.length(); j++) {
               OSTrigger trig = new OSTestTrigger(ands.getJSONObject(j));

               parsed.add(trig);
            }

            parsedTriggers.add(parsed);
         }

         return parsedTriggers;
      }

      @Override
      public void setDisplayDuration(double displayDuration) {
         super.setDisplayDuration(displayDuration);
      }

      @NonNull
      @Override
      public Set<String> getClickedClickIds() {
         return super.getClickedClickIds();
      }

      @Override
      public boolean isClickAvailable(String clickId) {
         return super.isClickAvailable(clickId);
      }

      @Override
      public  void clearClickIds() {
         super.clearClickIds();
      }

      @Override
      public  void addClickId(String clickId) {
         super.addClickId(clickId);
      }

      @Override
      public double getDisplayDuration() {
         return super.getDisplayDuration();
      }

      @Override
      public OSTestInAppMessageDisplayStats getRedisplayStats() {
         return new OSTestInAppMessageDisplayStats(super.getRedisplayStats());
      }

      @Override
      public void setRedisplayStats(int displayQuantity, long lastDisplayTime) {
         super.setRedisplayStats(displayQuantity, lastDisplayTime);
      }

      public JSONObject toJSONObject() {
         return super.toJSONObject();
      }
   }

   public static class OSTestInAppMessageDisplayStats extends OSInAppMessageRedisplayStats {

      private OSInAppMessageRedisplayStats displayStats;

      OSTestInAppMessageDisplayStats(OSInAppMessageRedisplayStats displayStats) {
         this.displayStats = displayStats;
      }

      @Override
      public void setDisplayStats(OSInAppMessageRedisplayStats displayStats) {
         this.displayStats.setDisplayStats(displayStats);
      }

      @Override
      public long getLastDisplayTime() {
         return this.displayStats.getLastDisplayTime();
      }

      @Override
      public void setLastDisplayTime(long lastDisplayTime) {
         this.displayStats.setLastDisplayTime(lastDisplayTime);
      }

      public void setLastDisplayTimeToCurrent(OSTime time) {
         this.displayStats.setLastDisplayTime(time.getCurrentTimeMillis() / 1000);
      }

      @Override
      public void incrementDisplayQuantity() {
         this.displayStats.incrementDisplayQuantity();
      }

      @Override
      public int getDisplayQuantity() {
         return this.displayStats.getDisplayQuantity();
      }

      @Override
      public void setDisplayQuantity(int displayQuantity) {
         this.displayStats.setDisplayQuantity(displayQuantity);
      }

      @Override
      public int getDisplayLimit() {
         return this.displayStats.getDisplayLimit();
      }

      @Override
      public void setDisplayLimit(int displayLimit) {
         this.displayStats.setDisplayLimit(displayLimit);
      }

      @Override
      public long getDisplayDelay() {
         return this.displayStats.getDisplayDelay();
      }

      @Override
      public void setDisplayDelay(long displayDelay) {
         this.displayStats.setDisplayDelay(displayDelay);
      }

      @Override
      public boolean shouldDisplayAgain() {
         return this.displayStats.shouldDisplayAgain();
      }

      @Override
      public boolean isDelayTimeSatisfied() {
         return this.displayStats.isDelayTimeSatisfied();
      }

      @Override
      public boolean isRedisplayEnabled() {
         return this.displayStats.isRedisplayEnabled();
      }
   }

   public static class OSTestTrigger extends OSTrigger {
      public OSTestTrigger(JSONObject json) throws JSONException {
         super(json);
      }
   }

   public static class OSTestInAppMessageAction extends OSInAppMessageAction {
      public boolean closes() {
         return super.doesCloseMessage();
      }
      public String getClickId() { return super.getClickId(); }

      public OSTestInAppMessageAction(JSONObject json) throws JSONException {
         super(json);
      }
   }

   public static void dismissCurrentMessage() {
      OSInAppMessageInternal message = OneSignal.getInAppMessageController().getCurrentDisplayedInAppMessage();
      if (message != null) {
         OneSignal.getInAppMessageController().messageWasDismissed(message);
      } else {
         Log.e(LOGCAT_TAG, "No currently displaying IAM to dismiss!");
      }
   }

   public static boolean isInAppMessageShowing() {
      return OneSignal.getInAppMessageController().isInAppMessageShowing();
   }

   public static String getShowingInAppMessageId() {
      return OneSignal.getInAppMessageController().getCurrentDisplayedInAppMessage().messageId;
   }

   public static ArrayList<OSInAppMessageInternal> getInAppMessageDisplayQueue() {
      return OneSignal.getInAppMessageController().getInAppMessageDisplayQueue();
   }

   public static void onMessageActionOccurredOnMessage(@NonNull final OSInAppMessageInternal message, @NonNull final JSONObject actionJson) throws JSONException {
      OneSignal.getInAppMessageController().onMessageActionOccurredOnMessage(message, actionJson);
   }

   public static void onMessageWasShown(@NonNull OSInAppMessageInternal message) {
      OneSignal.getInAppMessageController().onMessageWasShown(message);
   }

   public static void onPageChanged(@NonNull OSInAppMessageInternal message, @NonNull final JSONObject eventJson) {
      OneSignal.getInAppMessageController().onPageChanged(message, eventJson);
   }

   /** IAM Lifecycle */
   public static void onMessageWillDisplay(@NonNull final OSInAppMessageInternal message) {
      OneSignal.getInAppMessageController().onMessageWillDisplay(message);
   }

   public static void onMessageDidDisplay(@NonNull final OSInAppMessageInternal message) {
      OneSignal.getInAppMessageController().onMessageDidDisplay(message);
   }

   public static void onMessageWillDismiss(@NonNull final OSInAppMessageInternal message) {
      OneSignal.getInAppMessageController().onMessageWillDismiss(message);
   }

   public static void onMessageDidDismiss(@NonNull final OSInAppMessageInternal message) {
      OneSignal.getInAppMessageController().onMessageDidDismiss(message);
   }
   
   // End IAM Lifecycle

   public static List<OSTestInAppMessageInternal> getRedisplayInAppMessages() {
      List<OSInAppMessageInternal> messages = OneSignal.getInAppMessageController().getRedisplayedInAppMessages();
      List<OSTestInAppMessageInternal> testMessages = new ArrayList<>();

      for (OSInAppMessageInternal message : messages) {
         try {
            JSONObject json = InAppMessagingHelpers.convertIAMtoJSONObject(message);
            OSTestInAppMessageInternal testInAppMessage = new OSTestInAppMessageInternal(json);
            testInAppMessage.getRedisplayStats().setDisplayStats(message.getRedisplayStats());
            testMessages.add(testInAppMessage);

         } catch (JSONException e) {
            e.printStackTrace();
         }
      }
      return testMessages;
   }

   public static boolean hasConfigChangeFlag(Activity activity, int configChangeFlag) {
      return OSUtils.hasConfigChangeFlag(activity, configChangeFlag);
   }

   public static int getDeviceType() {
      return new OSUtils().getDeviceType();
   }

   public abstract class UserState extends top.oneconnectapi.onesignal.UserState {
      UserState(String inPersistKey, boolean load) {
         super(inPersistKey, load);
      }
   }

   public static class WebViewManager extends top.oneconnectapi.onesignal.WebViewManager {

      public static void callDismissAndAwaitNextMessage() {
         lastInstance.dismissAndAwaitNextMessage(null);
      }

      public void dismissAndAwaitNextMessage(@Nullable final OneSignalGenericCallback callback) {
         super.dismissAndAwaitNextMessage(callback);
      }

      protected WebViewManager(@NonNull OSInAppMessageInternal message, @NonNull Activity activity, @NonNull top.oneconnectapi.onesignal.OSInAppMessageContent content) {
         super(message, activity, content);
      }
   }

   public static class JSONUtils extends top.oneconnectapi.onesignal.JSONUtils {

      public @Nullable static Map<String, Object> jsonObjectToMap(@Nullable JSONObject json) throws JSONException {
         return top.oneconnectapi.onesignal.JSONUtils.jsonObjectToMap(json);
      }
   }

   public static class GenerateNotification extends top.oneconnectapi.onesignal.GenerateNotification {}

   public static class NotificationBundleProcessor extends top.oneconnectapi.onesignal.NotificationBundleProcessor {}

   public static class OSNotificationFormatHelper extends top.oneconnectapi.onesignal.OSNotificationFormatHelper {}

   public static class NotificationPayloadProcessorHMS extends top.oneconnectapi.onesignal.NotificationPayloadProcessorHMS {}

   public static class OSTestNotification extends OSNotification {
      public OSTestNotification(@NonNull JSONObject payload) {
         super(payload);
      }

      // For testing purposes
      public static class OSTestNotificationBuilder {

         private List<OSNotification> groupedNotifications;

         private String notificationID;
         private String templateName, templateId;
         private String title, body;
         private JSONObject additionalData;
         private String smallIcon;
         private String largeIcon;
         private String bigPicture;
         private String smallIconAccentColor;
         private String launchURL;
         private String sound;
         private String ledColor;
         private int lockScreenVisibility = 1;
         private String groupKey;
         private String groupMessage;
         private List<ActionButton> actionButtons;
         private String fromProjectNumber;
         private BackgroundImageLayout backgroundImageLayout;
         private String collapseId;
         private int priority;
         private String rawPayload;

         public OSTestNotificationBuilder() {
         }

         public OSTestNotificationBuilder setGroupedNotifications(List<OSNotification> groupedNotifications) {
            this.groupedNotifications = groupedNotifications;
            return this;
         }

         public OSTestNotificationBuilder setNotificationID(String notificationID) {
            this.notificationID = notificationID;
            return this;
         }

         public OSTestNotificationBuilder setTemplateName(String templateName) {
            this.templateName = templateName;
            return this;
         }

         public OSTestNotificationBuilder setTemplateId(String templateId) {
            this.templateId = templateId;
            return this;
         }

         public OSTestNotificationBuilder setTitle(String title) {
            this.title = title;
            return this;
         }

         public OSTestNotificationBuilder setBody(String body) {
            this.body = body;
            return this;
         }

         public OSTestNotificationBuilder setAdditionalData(JSONObject additionalData) {
            this.additionalData = additionalData;
            return this;
         }

         public OSTestNotificationBuilder setSmallIcon(String smallIcon) {
            this.smallIcon = smallIcon;
            return this;
         }

         public OSTestNotificationBuilder setLargeIcon(String largeIcon) {
            this.largeIcon = largeIcon;
            return this;
         }

         public OSTestNotificationBuilder setBigPicture(String bigPicture) {
            this.bigPicture = bigPicture;
            return this;
         }

         public OSTestNotificationBuilder setSmallIconAccentColor(String smallIconAccentColor) {
            this.smallIconAccentColor = smallIconAccentColor;
            return this;
         }

         public OSTestNotificationBuilder setLaunchURL(String launchURL) {
            this.launchURL = launchURL;
            return this;
         }

         public OSTestNotificationBuilder setSound(String sound) {
            this.sound = sound;
            return this;
         }

         public OSTestNotificationBuilder setLedColor(String ledColor) {
            this.ledColor = ledColor;
            return this;
         }

         public OSTestNotificationBuilder setLockScreenVisibility(int lockScreenVisibility) {
            this.lockScreenVisibility = lockScreenVisibility;
            return this;
         }

         public OSTestNotificationBuilder setGroupKey(String groupKey) {
            this.groupKey = groupKey;
            return this;
         }

         public OSTestNotificationBuilder setGroupMessage(String groupMessage) {
            this.groupMessage = groupMessage;
            return this;
         }

         public OSTestNotificationBuilder setActionButtons(List<ActionButton> actionButtons) {
            this.actionButtons = actionButtons;
            return this;
         }

         public OSTestNotificationBuilder setFromProjectNumber(String fromProjectNumber) {
            this.fromProjectNumber = fromProjectNumber;
            return this;
         }

         public OSTestNotificationBuilder setBackgroundImageLayout(BackgroundImageLayout backgroundImageLayout) {
            this.backgroundImageLayout = backgroundImageLayout;
            return this;
         }

         public OSTestNotificationBuilder setCollapseId(String collapseId) {
            this.collapseId = collapseId;
            return this;
         }

         public OSTestNotificationBuilder setPriority(int priority) {
            this.priority = priority;
            return this;
         }

         public OSTestNotificationBuilder setRawPayload(String rawPayload) {
            this.rawPayload = rawPayload;
            return this;
         }

         public OSNotification build() {
            OSNotification payload = new OSNotification();
            payload.setGroupedNotifications(groupedNotifications);
            payload.setNotificationId(notificationID);
            payload.setTemplateName(templateName);
            payload.setTemplateId(templateId);
            payload.setTitle(title);
            payload.setBody(body);
            payload.setAdditionalData(additionalData);
            payload.setSmallIcon(smallIcon);
            payload.setLargeIcon(largeIcon);
            payload.setBigPicture(bigPicture);
            payload.setSmallIconAccentColor(smallIconAccentColor);
            payload.setLaunchURL(launchURL);
            payload.setSound(sound);
            payload.setLedColor(ledColor);
            payload.setLockScreenVisibility(lockScreenVisibility);
            payload.setGroupKey(groupKey);
            payload.setGroupMessage(groupMessage);
            payload.setActionButtons(actionButtons);
            payload.setFromProjectNumber(fromProjectNumber);
            payload.setBackgroundImageLayout(backgroundImageLayout);
            payload.setCollapseId(collapseId);
            payload.setPriority(priority);
            payload.setRawPayload(rawPayload);
            return payload;
         }
      }
   }

   public static class OSObservable<ObserverType, StateType> extends top.oneconnectapi.onesignal.OSObservable<ObserverType, StateType> {
      public OSObservable(String methodName, boolean fireOnMainThread) {
         super(methodName, fireOnMainThread);
      }

      public void addObserver(ObserverType observer) {
         super.addObserver(observer);
      }

      public void removeObserver(ObserverType observer) {
         super.removeObserver(observer);
      }
   }
}