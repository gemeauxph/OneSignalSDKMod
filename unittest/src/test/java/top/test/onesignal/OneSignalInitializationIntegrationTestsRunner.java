package top.test.onesignal;

import androidx.test.core.app.ApplicationProvider;

import top.oneconnectapi.onesignal.MockOSTimeImpl;
import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.ShadowCustomTabsClient;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignalWithMockSetupContextListeners;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorFCM;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.example.BlankActivity;

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

import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.MIN_ON_SESSION_TIME_MILLIS;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_setTime;
import static top.oneconnectapi.onesignal.ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse;
import static top.test.onesignal.RestClientAsserts.assertNumberOfOnSessions;
import static top.test.onesignal.TestHelpers.threadAndTaskWait;

import top.oneconnectapi.onesignal.MockOSTimeImpl;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.ShadowCustomTabsClient;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowOneSignalWithMockSetupContextListeners;
import top.oneconnectapi.onesignal.ShadowPushRegistratorFCM;
import top.oneconnectapi.onesignal.StaticResetHelper;

@Config(
        packageName = "top.oneconnectapi.onesignal.example",
        shadows = {
            ShadowOSUtils.class,
            ShadowOneSignalRestClient.class,
            ShadowPushRegistratorFCM.class,
            ShadowCustomTabsClient.class,
            ShadowCustomTabsSession.class,
        },
        sdk = 26
)

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class OneSignalInitializationIntegrationTestsRunner {
    private ActivityController<BlankActivity> blankActivityController;
    private MockOSTimeImpl time;

    @BeforeClass // Runs only once, before any tests
    public static void setUpClass() throws Exception {
        ShadowLog.stream = System.out;
        TestHelpers.beforeTestSuite();
        StaticResetHelper.saveStaticValues();
    }

    @Before
    public void beforeEachTest() throws Exception {
        TestHelpers.beforeTestInitAndCleanup();
        ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse();
        blankActivityController = Robolectric.buildActivity(BlankActivity.class).create();

        time = new MockOSTimeImpl();
        OneSignalPackagePrivateHelper.OneSignal_setTime(time);
    }

    private static final long MIN_ON_SESSION_TIME_SEC = OneSignalPackagePrivateHelper.MIN_ON_SESSION_TIME_MILLIS / 1_000L;
    private static final String APP_ID = "11111111-2222-3333-4444-55555555555";
    private static void helper_OneSignal_initWithAppContext() {
        OneSignal.initWithContext(ApplicationProvider.getApplicationContext());
    }
    private void helper_OneSignal_initWithActivity() {
        OneSignal.initWithContext(blankActivityController.get());
    }

    private void helper_advanceSystemTimeToNextOnSession() {
        time.advanceSystemAndElapsedTimeBy(MIN_ON_SESSION_TIME_SEC + 1);
    }

    @Test
    public void setRequiresUserPrivacyConsent_withTrue_CalledFirst_DoesNOTCreatePlayer() throws Exception {
        OneSignal.setRequiresUserPrivacyConsent(true);

        OneSignal.setAppId(APP_ID);
        helper_OneSignal_initWithAppContext();
        TestHelpers.threadAndTaskWait();

        RestClientAsserts.assertRemoteParamsWasTheOnlyNetworkCall();
    }

    // This test reproduces https://github.com/OneSignal/OneSignal-Android-SDK/issues/1514
    @Test
    @Config(shadows = { ShadowOneSignalWithMockSetupContextListeners.class })
    public void initWithContext_setupContextListenersNotCompleted_doesNotProduceNPE() throws Exception {
        OneSignal.setAppId(APP_ID);

        // call initWithContext() but don't complete setupContextListeners() via the Shadow class
        // this prevents the initialization of outcomeEventsController in setupContextListeners()
        helper_OneSignal_initWithAppContext();
        TestHelpers.threadAndTaskWait();
        // we implicitly test that no exception is thrown
    }

    @Test
    public void setRequiresUserPrivacyConsent_withFalseAndRemoteTrue_DoesNOTCreatePlayer() throws Exception {
        ShadowOneSignalRestClient.setRemoteParamsRequirePrivacyConsent(true);
        OneSignal.setRequiresUserPrivacyConsent(false);

        OneSignal.setAppId(APP_ID);
        helper_OneSignal_initWithAppContext();
        TestHelpers.threadAndTaskWait();

        RestClientAsserts.assertRemoteParamsWasTheOnlyNetworkCall();
    }

    /**
     * on_session calls should only be made if the user left the app for MIN_ON_SESSION_TIME_SEC or longer
     * This test ensures that we meet the out of focus requirement, at lest through initWithContext
     *   being called a 2nd time code path.
     */
    @Test
    public void initWithContext_calledA2ndTimeAfter30OrMoreSeconds_doesNotStartNewSession() throws Exception {
        // 1. Basic OneSignal init with Activity
        OneSignal.setAppId(APP_ID);
        helper_OneSignal_initWithActivity();
        TestHelpers.threadAndTaskWait();

        // 2. Keep the app in focus for 30+ seconds, which is the time required to
        helper_advanceSystemTimeToNextOnSession();

        // 3. Developer or OneSignal internally calls OneSignal.initWithContext
        helper_OneSignal_initWithActivity();
        TestHelpers.threadAndTaskWait();

        // 4. Ensure we do NOT make an /players/{player_id}/on_session network call.
        RestClientAsserts.assertNumberOfOnSessions(0);
    }

}
