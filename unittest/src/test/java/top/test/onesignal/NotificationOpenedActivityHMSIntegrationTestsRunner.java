package top.test.onesignal;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import top.oneconnectapi.onesignal.NotificationOpenedActivityHMS;
import top.oneconnectapi.onesignal.OSNotificationOpenedResult;
import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.UserState;
import top.oneconnectapi.onesignal.ShadowCustomTabsClient;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowHmsInstanceId;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOSViewUtils;
import top.oneconnectapi.onesignal.ShadowOSWebView;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorHMS;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.example.BlankActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
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

import java.util.UUID;

import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.NotificationBundleProcessor.PUSH_ADDITIONAL_DATA_KEY;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSNotificationFormatHelper.PAYLOAD_OS_ROOT_CUSTOM;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSNotificationFormatHelper.PAYLOAD_OS_NOTIFICATION_ID;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.GenerateNotification.BUNDLE_KEY_ACTION_ID;
import static top.oneconnectapi.onesignal.InAppMessagingHelpers.ONESIGNAL_APP_ID;
import static top.oneconnectapi.onesignal.ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse;
import static top.test.onesignal.RestClientAsserts.assertNotificationOpenAtIndex;
import static top.test.onesignal.TestHelpers.fastColdRestartApp;
import static top.test.onesignal.TestHelpers.threadAndTaskWait;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import top.oneconnectapi.onesignal.GenerateNotification;
import top.oneconnectapi.onesignal.InAppMessagingHelpers;
import top.oneconnectapi.onesignal.OSNotificationFormatHelper;
import top.oneconnectapi.onesignal.OSNotificationOpenedResult;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.ShadowCustomTabsClient;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowHmsInstanceId;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOSViewUtils;
import top.oneconnectapi.onesignal.ShadowOSWebView;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorHMS;
import top.oneconnectapi.onesignal.StaticResetHelper;

@Config(
    packageName = "top.oneconnectapi.onesignal.example",
    shadows = {
        ShadowOSUtils.class,
        ShadowPushRegistratorHMS.class,
        ShadowOneSignalRestClient.class,
        ShadowCustomTabsClient.class,
        ShadowOSWebView.class,
        ShadowOSViewUtils.class,
        ShadowCustomTabsClient.class,
        ShadowCustomTabsSession.class,
            ShadowHmsInstanceId.class,
    },
    sdk = 26
)
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class NotificationOpenedActivityHMSIntegrationTestsRunner {

    private static final String TEST_ACTION_ID = "myTestActionId";

    @BeforeClass // Runs only once, before any tests
    public static void setUpClass() throws Exception {
        ShadowLog.stream = System.out;
        TestHelpers.beforeTestSuite();
        StaticResetHelper.saveStaticValues();
    }

    @Before
    public void beforeEachTest() throws Exception {
        TestHelpers.beforeTestInitAndCleanup();
        ShadowOSUtils.supportsHMS(true);
        // Set remote_params GET response
        ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse();
    }

    @AfterClass
    public static void afterEverything() throws Exception {
        TestHelpers.beforeTestInitAndCleanup();
    }

    @After
    public void afterEachTest() throws Exception {
        TestHelpers.afterTestCleanup();
    }

    private static @NonNull Intent helper_baseHMSOpenIntent() {
        return new Intent()
                .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                .setAction("android.intent.action.VIEW");
    }

    private static @NonNull Intent helper_basicOSHMSOpenIntent() throws JSONException {
        return helper_baseHMSOpenIntent()
                .putExtra(
                        OSNotificationFormatHelper.PAYLOAD_OS_ROOT_CUSTOM,
                        new JSONObject() {{
                            put(OSNotificationFormatHelper.PAYLOAD_OS_NOTIFICATION_ID, UUID.randomUUID().toString());
                        }}.toString()
        );
    }

    private static @NonNull Intent helper_basicOSHMSOpenIntentWithActionId(final @NonNull String actionId) throws JSONException {
        return helper_baseHMSOpenIntent()
                .putExtra(
                        OSNotificationFormatHelper.PAYLOAD_OS_ROOT_CUSTOM,
                        new JSONObject() {{
                            put(OSNotificationFormatHelper.PAYLOAD_OS_NOTIFICATION_ID, UUID.randomUUID().toString());
                            put(GenerateNotification.BUNDLE_KEY_ACTION_ID, actionId);
                        }}.toString()
                );
    }

    private static void helper_startHMSOpenActivity(@NonNull Intent intent) {
        Robolectric.buildActivity(NotificationOpenedActivityHMS.class, intent).create();
    }

    private static void helper_initSDKAndFireHMSNotificationBarebonesOSOpenIntent() throws Exception {
        Intent intent = helper_basicOSHMSOpenIntent();
        helper_initSDKAndFireHMSNotificationOpenWithIntent(intent);
    }

    private static void helper_initSDKAndFireHMSNotificationActionButtonTapIntent(@NonNull String actionId) throws Exception {
        Intent intent = helper_basicOSHMSOpenIntentWithActionId(actionId);
        helper_initSDKAndFireHMSNotificationOpenWithIntent(intent);
    }

    private static void helper_initSDKAndFireHMSNotificationOpenWithIntent(@NonNull Intent intent) throws Exception {
        OneSignal.setAppId(InAppMessagingHelpers.ONESIGNAL_APP_ID);
        OneSignal.initWithContext(ApplicationProvider.getApplicationContext());
        TestHelpers.fastColdRestartApp();

        helper_startHMSOpenActivity(intent);
    }

    // Since the Activity has to be public it could be started outside of a OneSignal flow.
    // Ensure it doesn't crash the app.
    @Test
    public void emptyIntent_doesNotThrow() {
        helper_startHMSOpenActivity(helper_baseHMSOpenIntent());
    }

    @Test
    public void barebonesOSPayload_startsMainActivity() throws Exception {
        helper_initSDKAndFireHMSNotificationBarebonesOSOpenIntent();

        Intent startedActivity = shadowOf((Application) ApplicationProvider.getApplicationContext()).getNextStartedActivity();
        assertNotNull(startedActivity);
        assertEquals(startedActivity.getComponent().getClassName(), BlankActivity.class.getName());
    }

    @Test
    public void barebonesOSPayload_makesNotificationOpenRequest() throws Exception {
        helper_initSDKAndFireHMSNotificationBarebonesOSOpenIntent();
        assertNotificationOpenAtIndex(2, OneSignalPackagePrivateHelper.UserState.DEVICE_TYPE_HUAWEI);
    }

    private static String lastActionId;
    @Test
    public void firesOSNotificationOpenCallbackWithActionId() throws Exception {
        helper_initSDKAndFireHMSNotificationActionButtonTapIntent(TEST_ACTION_ID);

        OneSignal.setAppId(InAppMessagingHelpers.ONESIGNAL_APP_ID);
        OneSignal.initWithContext(ApplicationProvider.getApplicationContext());
        OneSignal.setNotificationOpenedHandler(new OneSignal.OSNotificationOpenedHandler() {
            @Override
            public void notificationOpened(OSNotificationOpenedResult result) {
                lastActionId = result.getAction().getActionId();
            }
        });

        assertEquals(TEST_ACTION_ID, lastActionId);
    }

    @Test
    public void osIAMPreview_showsPreview() throws Exception {
        ActivityController<BlankActivity> blankActivityController = Robolectric.buildActivity(BlankActivity.class).create();
        Activity blankActivity = blankActivityController.get();
        OneSignal.setAppId(InAppMessagingHelpers.ONESIGNAL_APP_ID);
        OneSignal.initWithContext(blankActivity);
        TestHelpers.threadAndTaskWait();

        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        Intent intent = helper_baseHMSOpenIntent()
                .putExtra(
                        OSNotificationFormatHelper.PAYLOAD_OS_ROOT_CUSTOM,
                        new JSONObject() {{
                            put(OSNotificationFormatHelper.PAYLOAD_OS_NOTIFICATION_ID, UUID.randomUUID().toString());
                            put(PUSH_ADDITIONAL_DATA_KEY, new JSONObject() {{
                                put("os_in_app_message_preview_id", "UUID");
                            }});
                        }}.toString()
                );

        helper_startHMSOpenActivity(intent);
        TestHelpers.threadAndTaskWait();
        TestHelpers.threadAndTaskWait();
        assertEquals("PGh0bWw+PC9odG1sPgoKPHNjcmlwdD4KICAgIHNldFBsYXllclRhZ3MoKTsKPC9zY3JpcHQ+", ShadowOSWebView.lastData);
    }
}
