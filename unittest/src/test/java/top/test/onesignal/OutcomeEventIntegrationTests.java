package top.test.onesignal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import top.oneconnectapi.onesignal.MockOSLog;
import top.oneconnectapi.onesignal.MockOSSharedPreferences;
import top.oneconnectapi.onesignal.MockOSTimeImpl;
import top.oneconnectapi.onesignal.MockOneSignalDBHelper;
import top.oneconnectapi.onesignal.MockSessionManager;
import top.oneconnectapi.onesignal.OSNotificationOpenedResult;
import top.oneconnectapi.onesignal.OSSessionManager;
import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.OneSignalShadowPackageManager;
import top.oneconnectapi.onesignal.ShadowCustomTabsClient;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowFocusHandler;
import top.oneconnectapi.onesignal.ShadowGMSLocationController;
import top.oneconnectapi.onesignal.ShadowGenerateNotification;
import top.oneconnectapi.onesignal.ShadowJobService;
import top.oneconnectapi.onesignal.ShadowNotificationManagerCompat;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorFCM;
import top.oneconnectapi.onesignal.ShadowTimeoutHandler;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.example.BlankActivity;
import top.oneconnectapi.onesignal.influence.data.OSTrackerFactory;
import top.oneconnectapi.onesignal.influence.domain.OSInfluence;
import top.oneconnectapi.onesignal.influence.domain.OSInfluenceChannel;
import top.oneconnectapi.onesignal.influence.domain.OSInfluenceType;

import org.json.JSONArray;
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
import org.robolectric.shadows.ShadowPausedSystemClock;

import java.util.Arrays;
import java.util.List;

import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.FCMBroadcastReceiver_onReceived_withBundle;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_getSessionListener;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_setSessionManager;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_setSharedPreferences;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_setTime;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_setTrackerFactory;
import static top.oneconnectapi.onesignal.ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse;
import static top.test.onesignal.GenerateNotificationRunner.getBaseNotifBundle;
import static top.test.onesignal.RestClientAsserts.assertMeasureAtIndex;
import static top.test.onesignal.RestClientAsserts.assertMeasureOnV2AtIndex;
import static top.test.onesignal.RestClientAsserts.assertOnFocusAtIndex;
import static top.test.onesignal.RestClientAsserts.assertOnFocusAtIndexDoesNotHaveKeys;
import static top.test.onesignal.RestClientAsserts.assertOnFocusAtIndexForPlayerId;
import static top.test.onesignal.RestClientAsserts.assertRestCalls;
import static top.test.onesignal.TestHelpers.afterTestCleanup;
import static top.test.onesignal.TestHelpers.assertAndRunSyncService;
import static top.test.onesignal.TestHelpers.fastColdRestartApp;
import static top.test.onesignal.TestHelpers.getAllNotificationRecords;
import static top.test.onesignal.TestHelpers.getAllUniqueOutcomeNotificationRecordsDB;
import static top.test.onesignal.TestHelpers.pauseActivity;
import static top.test.onesignal.TestHelpers.threadAndTaskWait;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import junit.framework.Assert;

import top.oneconnectapi.onesignal.MockOSLog;
import top.oneconnectapi.onesignal.MockOSSharedPreferences;
import top.oneconnectapi.onesignal.MockOSTimeImpl;
import top.oneconnectapi.onesignal.MockOneSignalDBHelper;
import top.oneconnectapi.onesignal.MockSessionManager;
import top.oneconnectapi.onesignal.OSNotificationOpenedResult;
import top.oneconnectapi.onesignal.OSSessionManager;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.OneSignalShadowPackageManager;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowFocusHandler;
import top.oneconnectapi.onesignal.ShadowGMSLocationController;
import top.oneconnectapi.onesignal.ShadowGenerateNotification;
import top.oneconnectapi.onesignal.ShadowJobService;
import top.oneconnectapi.onesignal.ShadowNotificationManagerCompat;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorFCM;
import top.oneconnectapi.onesignal.ShadowTimeoutHandler;
import top.oneconnectapi.onesignal.StaticResetHelper;

@Config(packageName = "top.oneconnectapi.onesignal.example",
        shadows = {
                ShadowPausedSystemClock.class,
                ShadowOneSignalRestClient.class,
                ShadowPushRegistratorFCM.class,
                ShadowGMSLocationController.class,
                ShadowOSUtils.class,
                ShadowCustomTabsClient.class,
                ShadowCustomTabsSession.class,
                ShadowNotificationManagerCompat.class,
                ShadowJobService.class,
                ShadowFocusHandler.class
        },
        sdk = 26)
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class OutcomeEventIntegrationTests {

    private static final String ONESIGNAL_APP_ID = "b2f7f966-d8cc-11e4-bed1-df8f05be55ba";
    private static final String ONESIGNAL_NOTIFICATION_ID = "97d8e764-81c2-49b0-a644-713d052ae7d5";
    private static final String ONESIGNAL_OUTCOME_NAME = "Testing_Outcome";

    @SuppressLint("StaticFieldLeak")
    private static Activity blankActivity;
    private static ActivityController<BlankActivity> blankActivityController;
    private static String notificationOpenedMessage;
    private MockOneSignalDBHelper dbHelper;
    private MockOSLog logger = new MockOSLog();
    private MockOSTimeImpl time;
    private MockSessionManager sessionManager;
    private OneSignalPackagePrivateHelper.OSSharedPreferencesWrapper preferences;
    private OSTrackerFactory trackerFactory;
    private static List<OSInfluence> lastInfluencesEnding;
    private static OSNotificationOpenedResult notificationOpenedResult;

    OSSessionManager.SessionListener sessionListener = lastInfluences -> {
        OneSignalPackagePrivateHelper.OneSignal_getSessionListener().onSessionEnding(lastInfluences);
        OutcomeEventIntegrationTests.lastInfluencesEnding = lastInfluences;
    };

    private static OneSignal.OSNotificationOpenedHandler getNotificationOpenedHandler() {
        return openedResult -> notificationOpenedMessage = openedResult.getNotification().getBody();
    }

    @BeforeClass // Runs only once, before any tests
    public static void setUpClass() throws Exception {
        ShadowLog.stream = System.out;

        TestHelpers.beforeTestSuite();
        StaticResetHelper.saveStaticValues();

        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
    }

    private static void cleanUp() throws Exception {
        notificationOpenedMessage = null;

        TestHelpers.beforeTestInitAndCleanup();
        // Set remote_params GET response
        ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse();
    }

    @Before
    public void beforeEachTest() throws Exception {
        blankActivityController = Robolectric.buildActivity(BlankActivity.class).create();
        blankActivity = blankActivityController.get();
        time = new MockOSTimeImpl();
        dbHelper = new MockOneSignalDBHelper(ApplicationProvider.getApplicationContext());
        preferences = new OneSignalPackagePrivateHelper.OSSharedPreferencesWrapper();
        trackerFactory = new OSTrackerFactory(preferences, logger, time);
        sessionManager = new MockSessionManager(sessionListener, trackerFactory, logger);
        cleanUp();
    }

    @After
    public void afterEachTest() throws Exception {
        lastInfluencesEnding = null;
        notificationOpenedResult = null;
        TestHelpers.afterTestCleanup();
    }

    @AfterClass
    public static void afterEverything() throws Exception {
        cleanUp();
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testAppSessions_beforeOnSessionCalls() throws Exception {
        foregroundAppAfterReceivingNotification();

        // Check session INDIRECT
        assertNotificationChannelIndirectInfluence(1);

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Click notification
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"UUID\" } }]"), ONESIGNAL_NOTIFICATION_ID + "2");
        TestHelpers.threadAndTaskWait();

        // Foreground app
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Check session DIRECT
        assertNotificationChannelDirectInfluence(ONESIGNAL_NOTIFICATION_ID + "2");
        // Upgrade on influence will end indirect session
        assertEquals(1, lastInfluencesEnding.size());
        // Upgrade on influence will end indirect session
        assertEquals(OSInfluenceChannel.NOTIFICATION, lastInfluencesEnding.get(0).getInfluenceChannel());
        assertEquals(OSInfluenceType.INDIRECT, lastInfluencesEnding.get(0).getInfluenceType());
        assertEquals(1, lastInfluencesEnding.get(0).getIds().length());
        assertEquals(ONESIGNAL_NOTIFICATION_ID + "1", lastInfluencesEnding.get(0).getIds().get(0));
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testAppSessions_afterOnSessionCalls() throws Exception {
        foregroundAppAfterReceivingNotification();

        // Check session INDIRECT
        assertNotificationChannelIndirectInfluence(1);

        // Background app for 30 seconds
        TestHelpers.pauseActivity(blankActivityController);
        time.advanceSystemTimeBy(31);

        // Click notification
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"UUID\" } }]"), ONESIGNAL_NOTIFICATION_ID + "2");
        TestHelpers.threadAndTaskWait();

        // Foreground app
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Check session DIRECT
        assertNotificationChannelDirectInfluence(ONESIGNAL_NOTIFICATION_ID + "2");
        // Upgrade on influence will end indirect session
        assertEquals(1, lastInfluencesEnding.size());
        // Upgrade on influence will end indirect session
        assertEquals(OSInfluenceChannel.NOTIFICATION, lastInfluencesEnding.get(0).getInfluenceChannel());
        assertEquals(OSInfluenceType.INDIRECT, lastInfluencesEnding.get(0).getInfluenceType());
        assertEquals(1, lastInfluencesEnding.get(0).getIds().length());
        assertEquals(ONESIGNAL_NOTIFICATION_ID + "1", lastInfluencesEnding.get(0).getIds().get(0));
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testIndirectAttributionWindow_withNoNotifications() throws Exception {
        foregroundAppAfterReceivingNotification();

        // Check received notifications matches indirectNotificationIds
        assertEquals(new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1"), trackerFactory.getNotificationChannelTracker().getIndirectIds());
        // Check session INDIRECT
        assertNotificationChannelIndirectInfluence(1);

        // Background app for attribution window time
        TestHelpers.pauseActivity(blankActivityController);
        time.advanceSystemTimeBy(24L * 60 * 60L);

        // Foreground app
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Check session UNATTRIBUTED
        assertNotificationChannelUnattributedInfluence();
    }

    @Test
    public void testUniqueOutcomeMeasureOnlySentOncePerClickedNotification_whenSendingMultipleUniqueOutcomes_inDirectSession() throws Exception {
        foregroundAppAfterClickingNotification();

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Check measure end point was most recent request and contains clicked notification
        assertMeasureAtIndex(3, true, ONESIGNAL_OUTCOME_NAME, new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1"));
        // Only 4 requests have been made
        assertRestCalls(4);

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Make still only 4 requests have been made
        assertRestCalls(4);
    }

    @Test
    public void testOnV2UniqueOutcomeMeasureOnlySentOncePerClickedNotification_whenSendingMultipleUniqueOutcomes_inDirectSession() throws Exception {
        // Enable IAM v2
        preferences = new MockOSSharedPreferences();
        trackerFactory = new OSTrackerFactory(preferences, logger, time);
        sessionManager = new MockSessionManager(sessionListener, trackerFactory, logger);
        preferences.saveBool(preferences.getPreferencesName(), preferences.getOutcomesV2KeyName(), true);
        OneSignalPackagePrivateHelper.OneSignal_setSharedPreferences(preferences);
        foregroundAppAfterClickingNotification();

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        JSONArray notificationIds = new JSONArray();
        notificationIds.put(ONESIGNAL_NOTIFICATION_ID + "1");

        // Check measure end point was most recent request and contains clicked notification
        assertMeasureOnV2AtIndex(3, ONESIGNAL_OUTCOME_NAME, new JSONArray(), notificationIds, null, null);
        // Only 4 requests have been made
        assertRestCalls(4);

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Make still only 4 requests have been made
        assertRestCalls(4);
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testUniqueOutcomeMeasureOnlySentOncePerNotification_whenSendingMultipleUniqueOutcomes_inIndirectSessions() throws Exception {
        foregroundAppAfterReceivingNotification();

        // Check notificationIds equal indirectNotificationIds from OSSessionManager
        JSONArray notificationIds = new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1");
        assertEquals(notificationIds, trackerFactory.getNotificationChannelTracker().getIndirectIds());
        // Make sure session is INDIRECT
        assertNotificationChannelIndirectInfluence(1);

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Check measure end point was most recent request and contains received notification
        assertMeasureAtIndex(2, false, ONESIGNAL_OUTCOME_NAME, new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1"));
        // Only 3 requests have been made
        assertRestCalls(3);

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Make still only 3 requests have been made
        assertRestCalls(3);

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Receive notification
        Bundle bundle = GenerateNotificationRunner.getBaseNotifBundle(ONESIGNAL_NOTIFICATION_ID + "2");
        OneSignalPackagePrivateHelper.FCMBroadcastReceiver_onReceived_withBundle(blankActivity, bundle);

        // Wait 31 seconds to start new session
        time.advanceSystemTimeBy(31);

        // Foreground app will start a new session upgrade
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Check notificationIds are not equal indirectNotificationIds from OSSessionManager
        notificationIds.put(ONESIGNAL_NOTIFICATION_ID + "2");
        assertEquals(notificationIds, trackerFactory.getNotificationChannelTracker().getIndirectIds());
        // Make sure session is INDIRECT
        assertNotificationChannelIndirectInfluence(2);

        // Check measure end point was most recent request and contains received notification
        assertMeasureAtIndex(4, false, ONESIGNAL_OUTCOME_NAME, new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "2"));
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testOnV2UniqueOutcomeMeasureOnlySentOncePerNotification_whenSendingMultipleUniqueOutcomes_inIndirectSessions() throws Exception {
        // Enable IAM v2
        preferences = new MockOSSharedPreferences();
        trackerFactory = new OSTrackerFactory(preferences, logger, time);
        sessionManager = new MockSessionManager(sessionListener, trackerFactory, logger);
        preferences.saveBool(preferences.getPreferencesName(), preferences.getOutcomesV2KeyName(), true);
        OneSignalPackagePrivateHelper.OneSignal_setSharedPreferences(preferences);
        foregroundAppAfterReceivingNotification();

        // Check notificationIds equal indirectNotificationIds from OSSessionManager
        JSONArray notificationIds = new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1");
        assertEquals(notificationIds, trackerFactory.getNotificationChannelTracker().getIndirectIds());
        // Make sure session is INDIRECT
        assertNotificationChannelIndirectInfluence(1);

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Check measure end point was most recent request and contains received notification
        assertMeasureOnV2AtIndex(2, ONESIGNAL_OUTCOME_NAME, null, null, new JSONArray(), notificationIds);
        // Only 3 requests have been made
        assertRestCalls(3);

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Make still only 3 requests have been made
        assertRestCalls(3);

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Receive notification
        Bundle bundle = GenerateNotificationRunner.getBaseNotifBundle(ONESIGNAL_NOTIFICATION_ID + "2");
        OneSignalPackagePrivateHelper.FCMBroadcastReceiver_onReceived_withBundle(blankActivity, bundle);

        // Wait 31 seconds to start new session
        time.advanceSystemTimeBy(31);

        // Foreground app will start a new session upgrade
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Check notificationIds are not equal indirectNotificationIds from OSSessionManager
        notificationIds.put(ONESIGNAL_NOTIFICATION_ID + "2");
        assertEquals(notificationIds, trackerFactory.getNotificationChannelTracker().getIndirectIds());
        // Make sure session is INDIRECT
        assertNotificationChannelIndirectInfluence(2);

        // Check measure end point was most recent request and contains received notification
        assertMeasureOnV2AtIndex(4, ONESIGNAL_OUTCOME_NAME, null, null, new JSONArray(),  new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "2"));
    }

    @Test
    public void testOutcomeNameSentWithMeasureOncePerSession_whenSendingMultipleUniqueOutcomes_inUnattributedSession() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        // Make sure session is UNATTRIBUTED
        assertNotificationChannelUnattributedInfluence();

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Check measure end point was most recent request and contains received notification
        assertMeasureAtIndex(2, ONESIGNAL_OUTCOME_NAME);
        // Only 3 requests have been made
        assertRestCalls(3);

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Make still only 3 requests have been made
        assertRestCalls(3);

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Wait 31 seconds to start new session
        time.advanceSystemTimeBy(31);

        // Foreground app
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Make sure session is UNATTRIBUTED
        assertNotificationChannelUnattributedInfluence();

        // Check measure end point was most recent request and contains received notification
        assertMeasureAtIndex(4, ONESIGNAL_OUTCOME_NAME);
    }

    @Test
    public void testOnV2OutcomeNameSentWithMeasureOncePerSession_whenSendingMultipleUniqueOutcomes_inUnattributedSession() throws Exception {
        // Enable IAM v2
        preferences = new MockOSSharedPreferences();
        trackerFactory = new OSTrackerFactory(preferences, logger, time);
        sessionManager = new MockSessionManager(sessionListener, trackerFactory, logger);
        preferences.saveBool(preferences.getPreferencesName(), preferences.getOutcomesV2KeyName(), true);
        OneSignalPackagePrivateHelper.OneSignal_setSharedPreferences(preferences);

        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        // Make sure session is UNATTRIBUTED
        assertNotificationChannelUnattributedInfluence();

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Check measure end point was most recent request and contains received notification
        assertMeasureOnV2AtIndex(2, ONESIGNAL_OUTCOME_NAME, null, null, null, null);
        // Only 3 requests have been made
        assertRestCalls(3);

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Make still only 3 requests have been made
        assertRestCalls(3);

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Wait 31 seconds to start new session
        time.advanceSystemTimeBy(31);

        // Foreground app
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Make sure session is UNATTRIBUTED
        assertNotificationChannelUnattributedInfluence();

        // Check measure end point was most recent request and contains received notification
        assertMeasureOnV2AtIndex(4, ONESIGNAL_OUTCOME_NAME, null, null, null, null);
    }

    @Test
    public void testCorrectOutcomeSent_fromNotificationOpenedHandler() throws Exception {
        // Init OneSignal with a custom opened handler
        OneSignalInit(new OneSignal.OSNotificationOpenedHandler() {
            @Override
            public void notificationOpened(OSNotificationOpenedResult result) {
                OneSignal.sendOutcome(ONESIGNAL_OUTCOME_NAME);
            }
        });
        TestHelpers.threadAndTaskWait();

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Receive and open a notification
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"UUID\" } }]"), ONESIGNAL_NOTIFICATION_ID);
        TestHelpers.threadAndTaskWait();

        // Foreground the application
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Make sure a measure request is made with the correct session and notifications
        assertMeasureAtIndex(3, true, ONESIGNAL_OUTCOME_NAME, new JSONArray("[" + ONESIGNAL_NOTIFICATION_ID + "]"));
    }

    @Test
    public void testOnV2CorrectOutcomeSent_fromNotificationOpenedHandler() throws Exception {
        // Enable IAM v2
        preferences = new MockOSSharedPreferences();
        trackerFactory = new OSTrackerFactory(preferences, logger, time);
        sessionManager = new MockSessionManager(sessionListener, trackerFactory, logger);
        preferences.saveBool(preferences.getPreferencesName(), preferences.getOutcomesV2KeyName(), true);
        OneSignalPackagePrivateHelper.OneSignal_setSharedPreferences(preferences);

        // Init OneSignal with a custom opened handler
        OneSignalInit(new OneSignal.OSNotificationOpenedHandler() {
            @Override
            public void notificationOpened(OSNotificationOpenedResult result) {
                OneSignal.sendOutcome(ONESIGNAL_OUTCOME_NAME);
            }
        });
        TestHelpers.threadAndTaskWait();

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Receive and open a notification
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"UUID\" } }]"), ONESIGNAL_NOTIFICATION_ID);
        TestHelpers.threadAndTaskWait();

        // Foreground the application
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        JSONArray notificationIds = new JSONArray();
        notificationIds.put(ONESIGNAL_NOTIFICATION_ID);

        // Make sure a measure request is made with the correct session and notifications
        assertMeasureOnV2AtIndex(3, ONESIGNAL_OUTCOME_NAME, new JSONArray(), notificationIds, null, null);
    }

    @Test
    public void testNoDirectSession_fromNotificationOpen_whenAppIsInForeground() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        // Make sure no notification data exists
        assertNull(notificationOpenedMessage);

        // Click notification
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"UUID\" } }]"), ONESIGNAL_NOTIFICATION_ID);
        TestHelpers.threadAndTaskWait();

        // Check message String matches data sent in open handler
        assertEquals("Test Msg", notificationOpenedMessage);
        // Make sure session is not DIRECT
        assertFalse(trackerFactory.getNotificationChannelTracker().getInfluenceType().isDirect());
        // Make sure session is not INDIRECT
        assertFalse(trackerFactory.getNotificationChannelTracker().getInfluenceType().isIndirect());
        // Make sure no session is ending
        assertNull(lastInfluencesEnding);
    }

    @Test
    public void testDirectSession_fromNotificationOpen_whenAppIsInBackground() throws Exception {
        foregroundAppAfterClickingNotification();

        // Check message String matches data sent in open handler
        assertEquals("Test Msg", notificationOpenedMessage);
        // Make sure notification influence is DIRECT
        assertNotificationChannelDirectInfluence(ONESIGNAL_NOTIFICATION_ID + "1");
    }

    @Test
    @Config(shadows = { OneSignalShadowPackageManager.class })
    public void testDirectSession_fromNotificationClick_OpenHandleByUser_whenAppIsInBackground() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        OneSignalShadowPackageManager.addManifestMetaData("top.oneconnectapi.onesignal.NotificationOpened.DEFAULT", "DISABLE");
        OneSignal.setNotificationOpenedHandler(result -> {
            OneSignal.onesignalLog(OneSignal.LOG_LEVEL.VERBOSE, "OSNotificationOpenedHandler called with result: " + result);
            notificationOpenedResult = result;

            // App opened after clicking notification, but Robolectric needs this to simulate onAppFocus() code after a click
            blankActivityController.resume();
        });

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        String notificationID = ONESIGNAL_NOTIFICATION_ID + "1";
        sessionManager.onNotificationReceived(notificationID);

        // Click notification before new session
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"" + notificationID + "\" } }]"), notificationID);
        TestHelpers.threadAndTaskWait();

        assertNotNull(notificationOpenedResult);
        // Check message String matches data sent in open handler
        assertEquals("Test Msg", notificationOpenedResult.getNotification().getBody());

        // Make sure notification influence is DIRECT
        assertNotificationChannelDirectInfluence(notificationID);
        // Make sure iam influence is UNATTRIBUTED
        assertIAMChannelUnattributedInfluence();

        for (OSInfluence influence : lastInfluencesEnding) {
            assertEquals(OSInfluenceType.UNATTRIBUTED, influence.getInfluenceType());
        }
    }

    @Test
    @Config(shadows = { OneSignalShadowPackageManager.class })
    public void testDirectSession_fromNotificationClick_OpenHandleByUser_NewSession_whenAppIsInBackground() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        OneSignalShadowPackageManager.addManifestMetaData("top.oneconnectapi.onesignal.NotificationOpened.DEFAULT", "DISABLE");
        OneSignal.setNotificationOpenedHandler(result -> {
            OneSignal.onesignalLog(OneSignal.LOG_LEVEL.VERBOSE, "OSNotificationOpenedHandler called with result: " + result);
            notificationOpenedResult = result;

            // App opened after clicking notification, but Robolectric needs this to simulate onAppFocus() code after a click
            blankActivityController.resume();
        });

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        String notificationID = ONESIGNAL_NOTIFICATION_ID + "1";
        sessionManager.onNotificationReceived(notificationID);

        time.advanceSystemAndElapsedTimeBy(61);

        // Click notification before new session
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"" + notificationID + "\" } }]"), notificationID);
        TestHelpers.threadAndTaskWait();

        assertNotNull(notificationOpenedResult);
        // Check message String matches data sent in open handler
        assertEquals("Test Msg", notificationOpenedResult.getNotification().getBody());

        // Make sure notification influence is DIRECT
        assertNotificationChannelDirectInfluence(notificationID);
        // Make sure iam influence is UNATTRIBUTED
        assertIAMChannelUnattributedInfluence();

        for (OSInfluence influence : lastInfluencesEnding) {
            assertEquals(OSInfluenceType.UNATTRIBUTED, influence.getInfluenceType());
        }
    }

    @Test
    @Config(shadows = { OneSignalShadowPackageManager.class, ShadowTimeoutHandler.class })
    public void testDirectSession_fromNotificationClick_NoOpened_whenAppIsInBackground() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        OneSignalShadowPackageManager.addManifestMetaData("top.oneconnectapi.onesignal.NotificationOpened.DEFAULT", "DISABLE");
        OneSignal.setNotificationOpenedHandler(result -> {
            OneSignal.onesignalLog(OneSignal.LOG_LEVEL.VERBOSE, "OSNotificationOpenedHandler called with result: " + result);
            notificationOpenedResult = result;
        });

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        String notificationID = ONESIGNAL_NOTIFICATION_ID + "1";
        sessionManager.onNotificationReceived(notificationID);

        // Mock timeout to 1, given that we are delaying the complete inside OSNotificationOpenedResult
        // We depend on the timeout complete
        ShadowTimeoutHandler.setMockDelayMillis(1);

        // Click notification before new session
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"" + notificationID + "\" } }]"), notificationID);
        TestHelpers.threadAndTaskWait();

        assertNotNull(notificationOpenedResult);
        // Check message String matches data sent in open handler
        assertEquals("Test Msg", notificationOpenedResult.getNotification().getBody());

        // Make sure notification influence is UNATTRIBUTED
        assertNotificationChannelUnattributedInfluence();
        // Make sure iam influence is UNATTRIBUTED
        assertIAMChannelUnattributedInfluence();

        assertNull(lastInfluencesEnding);
    }

    @Test
    @Config(shadows = { OneSignalShadowPackageManager.class, ShadowTimeoutHandler.class })
    public void testDirectSession_fromNotificationClick_NoOpened_NewSession_whenAppIsInBackground() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        OneSignalShadowPackageManager.addManifestMetaData("top.oneconnectapi.onesignal.NotificationOpened.DEFAULT", "DISABLE");
        OneSignal.setNotificationOpenedHandler(result -> {
            OneSignal.onesignalLog(OneSignal.LOG_LEVEL.VERBOSE, "OSNotificationOpenedHandler called with result: " + result);
            notificationOpenedResult = result;
        });

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        String notificationID = ONESIGNAL_NOTIFICATION_ID + "1";
        sessionManager.onNotificationReceived(notificationID);

        // Mock timeout to 1, given that we are delaying the complete inside OSNotificationOpenedResult
        // We depend on the timeout complete
        ShadowTimeoutHandler.setMockDelayMillis(1);
        time.advanceSystemAndElapsedTimeBy(61);

        // Click notification before new session
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"" + notificationID + "\" } }]"), notificationID);
        TestHelpers.threadAndTaskWait();

        assertNotNull(notificationOpenedResult);
        // Check message String matches data sent in open handler
        assertEquals("Test Msg", notificationOpenedResult.getNotification().getBody());

        // Make sure notification influence is UNATTRIBUTED
        assertNotificationChannelUnattributedInfluence();
        // Make sure iam influence is UNATTRIBUTED
        assertIAMChannelUnattributedInfluence();

        assertNull(lastInfluencesEnding);
    }

    @Test
    @Config(shadows = { OneSignalShadowPackageManager.class })
    public void testDirectSession_fromNotificationClick_OpenHandleByUser_whenAppIsInForeground() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        OneSignalShadowPackageManager.addManifestMetaData("top.oneconnectapi.onesignal.NotificationOpened.DEFAULT", "DISABLE");
        OneSignal.setNotificationOpenedHandler(result -> {
            OneSignal.onesignalLog(OneSignal.LOG_LEVEL.VERBOSE, "OSNotificationOpenedHandler called with result: " + result);
            notificationOpenedResult = result;
        });

        String notificationID = ONESIGNAL_NOTIFICATION_ID + "1";
        sessionManager.onNotificationReceived(notificationID);
        // Click notification before new session
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"" + notificationID + "\" } }]"), notificationID);
        TestHelpers.threadAndTaskWait();

        assertNotNull(notificationOpenedResult);
        // Check message String matches data sent in open handler
        assertEquals("Test Msg", notificationOpenedResult.getNotification().getBody());

        // Make sure notification influence is UNATTRIBUTED
        assertNotificationChannelUnattributedInfluence();
        // Make sure iam influence is UNATTRIBUTED
        assertIAMChannelUnattributedInfluence();

        assertNull(lastInfluencesEnding);
    }

    @Test
    public void testIndirectSession_wontOverrideUnattributedSession_fromNotificationReceived_whenAppIsInForeground() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        // Make sure session is unattributed
        assertNotificationChannelUnattributedInfluence();
        assertIAMChannelUnattributedInfluence();

        // Receive notification
        Bundle bundle = GenerateNotificationRunner.getBaseNotifBundle(ONESIGNAL_NOTIFICATION_ID);
        OneSignalPackagePrivateHelper.FCMBroadcastReceiver_onReceived_withBundle(blankActivity, bundle);

        // Make sure notification influence is not INDIRECT
        assertFalse(trackerFactory.getNotificationChannelTracker().getInfluenceType().isIndirect());
        // Make sure not indirect notifications exist
        assertNull(trackerFactory.getNotificationChannelTracker().getIndirectIds());
        // Make sure not session is ending
        assertNull(lastInfluencesEnding);
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testDirectSession_willOverrideIndirectSession_whenAppIsInBackground() throws Exception {
        time.advanceSystemAndElapsedTimeBy(0);
        foregroundAppAfterReceivingNotification();

        // Foreground for 10 seconds
        time.advanceSystemAndElapsedTimeBy(10);

        // Make sure session is INDIRECT
        assertNotificationChannelIndirectInfluence(1);

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Click notification before new session
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"UUID\" } }]"), ONESIGNAL_NOTIFICATION_ID + "2");
        TestHelpers.threadAndTaskWait();

        // Foreground app
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Make sure on_focus is sent immediately since DIRECT session is going to override
        assertOnFocusAtIndex(3, new JSONObject() {{
            put("active_time", 10);
            put("direct", false);
            put("notification_ids", new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1"));
        }});
        // Check directNotificationId is set to clicked notification
        assertEquals(ONESIGNAL_NOTIFICATION_ID + "2", trackerFactory.getNotificationChannelTracker().getDirectId());
        // Make sure session is DIRECT
        assertNotificationChannelDirectInfluence(ONESIGNAL_NOTIFICATION_ID + "2");
    }

    @Test
    public void testDirectSession_willOverrideDirectSession_whenAppIsInBackground() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Click notification before new session
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"UUID\" } }]"), ONESIGNAL_NOTIFICATION_ID + "2");
        TestHelpers.threadAndTaskWait();

        // Check directNotificationId is set to clicked notification
        assertEquals(ONESIGNAL_NOTIFICATION_ID + "2", trackerFactory.getNotificationChannelTracker().getDirectId());
        // Make sure session is DIRECT
        assertNotificationChannelDirectInfluence(ONESIGNAL_NOTIFICATION_ID + "2");
        // Make sure session is ending
        assertEquals(1, lastInfluencesEnding.size());
        assertEquals(OSInfluenceChannel.NOTIFICATION, lastInfluencesEnding.get(0).getInfluenceChannel());
        assertEquals(OSInfluenceType.UNATTRIBUTED, lastInfluencesEnding.get(0).getInfluenceType());
        assertNull(lastInfluencesEnding.get(0).getIds());
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testIndirectSession_fromDirectSession_afterNewSession() throws Exception {
        foregroundAppAfterClickingNotification();

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Receive notification
        Bundle bundle = GenerateNotificationRunner.getBaseNotifBundle(ONESIGNAL_NOTIFICATION_ID + "2");
        OneSignalPackagePrivateHelper.FCMBroadcastReceiver_onReceived_withBundle(blankActivity, bundle);

        // Wait 31 seconds
        time.advanceSystemTimeBy(31);

        // Foreground app through icon before new session
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Check on_session is triggered
        assertTrue(ShadowOneSignalRestClient.lastUrl.matches("players/.*/on_session"));
        // Make sure no directNotificationId exist
        assertNull(trackerFactory.getNotificationChannelTracker().getDirectId());
        // Make sure indirectNotificationIds are correct
        assertEquals(new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1").put(ONESIGNAL_NOTIFICATION_ID + "2"), trackerFactory.getNotificationChannelTracker().getIndirectIds());
        // Make sure session is INDIRECT
        assertNotificationChannelIndirectInfluence(2);

        // Make sure session is ending
        assertEquals(1, lastInfluencesEnding.size());
        assertEquals(OSInfluenceChannel.NOTIFICATION, lastInfluencesEnding.get(0).getInfluenceChannel());
        assertEquals(OSInfluenceType.DIRECT, lastInfluencesEnding.get(0).getInfluenceType());
        assertEquals(1, lastInfluencesEnding.get(0).getIds().length());
        assertEquals(ONESIGNAL_NOTIFICATION_ID + "1", lastInfluencesEnding.get(0).getIds().get(0));
    }

    @Test
    public void testIndirectSession_wontOverrideDirectSession_beforeNewSession() throws Exception {
        foregroundAppAfterClickingNotification();

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Receive notification
        Bundle bundle = GenerateNotificationRunner.getBaseNotifBundle(ONESIGNAL_NOTIFICATION_ID + "2");
        OneSignalPackagePrivateHelper.FCMBroadcastReceiver_onReceived_withBundle(blankActivity, bundle);

        // Foreground app through icon before new session
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Make sure no indirectNotificationIds exist
        assertNull(trackerFactory.getNotificationChannelTracker().getIndirectIds());
        // Check directNotificationId is set to clicked notification
        assertEquals(ONESIGNAL_NOTIFICATION_ID + "1", trackerFactory.getNotificationChannelTracker().getDirectId());
        // Make sure session is DIRECT
        assertNotificationChannelDirectInfluence(ONESIGNAL_NOTIFICATION_ID + "1");
        // Make sure no session is ending
        assertNull(lastInfluencesEnding);
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testIndirectSession_wontOverrideIndirectSession_beforeNewSession() throws Exception {
        foregroundAppAfterReceivingNotification();

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Receive another notification
        Bundle bundle = GenerateNotificationRunner.getBaseNotifBundle(ONESIGNAL_NOTIFICATION_ID + "2");
        OneSignalPackagePrivateHelper.FCMBroadcastReceiver_onReceived_withBundle(blankActivity, bundle);

        // Foreground app through icon before new session
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Make sure indirectNotificationIds are correct
        JSONArray indirectNotificationIds = new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1");
        assertEquals(indirectNotificationIds, trackerFactory.getNotificationChannelTracker().getIndirectIds());

        // Make sure session is INDIRECT
        assertNotificationChannelIndirectInfluence(1);
        // Make sure no session is ending
        assertNull(lastInfluencesEnding);
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testIndirectSession_sendsOnFocusFromSyncJob_after10SecondSession() throws Exception {
        time.advanceSystemAndElapsedTimeBy(0);
        foregroundAppAfterReceivingNotification();

        // App in foreground for 10 seconds
        time.advanceSystemAndElapsedTimeBy(10);

        // Background app
        // Sync job will be scheduled here but not run yet
        TestHelpers.pauseActivity(blankActivityController);

        TestHelpers.assertAndRunSyncService();
        assertOnFocusAtIndex(2, new JSONObject() {{
            put("active_time", 10);
            put("direct", false);
            put("notification_ids", new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1"));
        }});
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testIndirectSession_sendsOnFocusFromSyncJob_evenAfterKillingApp_after10SecondSession() throws Exception {
        time.advanceSystemAndElapsedTimeBy(0);
        foregroundAppAfterReceivingNotification();

        // App in foreground for 10 seconds
        time.advanceSystemAndElapsedTimeBy(10);

        // Background app
        // Sync job will be scheduled here but not run yet
        TestHelpers.pauseActivity(blankActivityController);

        TestHelpers.fastColdRestartApp();

        TestHelpers.assertAndRunSyncService();

        assertOnFocusAtIndex(3, new JSONObject() {{
            put("active_time", 10);
            put("direct", false);
            put("notification_ids", new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1"));
        }});
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testIndirectSession_sendsOnFocusAttributionForPushPlayer_butNotEmailPlayer() throws Exception {
        time.advanceSystemAndElapsedTimeBy(0);
        OneSignal.setEmail("test@test.com");
        foregroundAppAfterReceivingNotification();

        // App in foreground for 10 seconds
        time.advanceSystemAndElapsedTimeBy(10);

        // Background app
        // Sync job will be scheduled here but not run yet
        TestHelpers.pauseActivity(blankActivityController);

        TestHelpers.assertAndRunSyncService();
        // Ensure we send notification attribution for push player
        assertOnFocusAtIndexForPlayerId(4, ShadowOneSignalRestClient.pushUserId);
        assertOnFocusAtIndex(4, new JSONObject() {{
            put("active_time", 10);
            put("direct", false);
            put("notification_ids", new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1"));
        }});

        // Ensure we DO NOT send notification attribution for email player
        //   Otherwise it would look like 2 different session to outcomes.
        assertOnFocusAtIndexForPlayerId(5, ShadowOneSignalRestClient.emailUserId);
        assertOnFocusAtIndexDoesNotHaveKeys(5, Arrays.asList("direct", "notification_ids"));
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testIndirectSessionNotificationsUpdated_onNewIndirectSession() throws Exception {
        foregroundAppAfterReceivingNotification();

        // Make sure indirectNotificationIds are correct
        JSONArray indirectNotificationIds = new JSONArray().put(ONESIGNAL_NOTIFICATION_ID + "1");
        assertEquals(indirectNotificationIds, trackerFactory.getNotificationChannelTracker().getIndirectIds());

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Receive notification
        Bundle bundle = GenerateNotificationRunner.getBaseNotifBundle(ONESIGNAL_NOTIFICATION_ID + "2");
        OneSignalPackagePrivateHelper.FCMBroadcastReceiver_onReceived_withBundle(blankActivity, bundle);
        indirectNotificationIds.put(ONESIGNAL_NOTIFICATION_ID + "2");

        // App in background for 31 seconds to trigger new session
        time.advanceSystemTimeBy(31);

        // Foreground app through icon
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Make sure indirectNotificationIds are updated and correct
        assertEquals(indirectNotificationIds, trackerFactory.getNotificationChannelTracker().getIndirectIds());

        // Make sure session is ending
        assertEquals(1, lastInfluencesEnding.size());
        assertEquals(OSInfluenceChannel.NOTIFICATION, lastInfluencesEnding.get(0).getInfluenceChannel());
        assertEquals(OSInfluenceType.INDIRECT, lastInfluencesEnding.get(0).getInfluenceType());
        assertEquals(1, lastInfluencesEnding.get(0).getIds().length());
        assertEquals(ONESIGNAL_NOTIFICATION_ID + "1", lastInfluencesEnding.get(0).getIds().get(0));
    }

    @Test
    @Config(shadows = { ShadowGenerateNotification.class })
    public void testCleaningCachedNotifications_after7Days_willAlsoCleanUniqueOutcomeNotifications() throws Exception {
        foregroundAppAfterReceivingNotification();

        Assert.assertEquals(1, TestHelpers.getAllNotificationRecords(dbHelper).size());
        Assert.assertEquals(0, TestHelpers.getAllUniqueOutcomeNotificationRecordsDB(dbHelper).size());

        // Should add a new unique outcome notifications (total in cache = 0 + 1)
        OneSignal.sendUniqueOutcome("unique_1");
        TestHelpers.threadAndTaskWait();

        // Should not add a new unique outcome notifications (total in cache = 1)
        OneSignal.sendUniqueOutcome("unique_1");
        TestHelpers.threadAndTaskWait();

        Assert.assertEquals(1, TestHelpers.getAllNotificationRecords(dbHelper).size());
        Assert.assertEquals(1, TestHelpers.getAllUniqueOutcomeNotificationRecordsDB(dbHelper).size());

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        // Wait for 30 seconds to trigger new session
        time.advanceSystemTimeBy(31);

        // Receive notification
        Bundle bundle = GenerateNotificationRunner.getBaseNotifBundle(ONESIGNAL_NOTIFICATION_ID + "2");
        OneSignalPackagePrivateHelper.FCMBroadcastReceiver_onReceived_withBundle(blankActivity, bundle);

        // Foreground app through icon
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Should add two unique outcome notifications (total in cache = 1 + 2)
        OneSignal.sendUniqueOutcome("unique_2");
        TestHelpers.threadAndTaskWait();

        // Should add two unique outcome notifications (total in cache = 3 + 2)
        OneSignal.sendUniqueOutcome("unique_3");
        TestHelpers.threadAndTaskWait();

        // Make sure only 2 notifications exist still, but 5 unique outcome notifications exist
        Assert.assertEquals(2, TestHelpers.getAllNotificationRecords(dbHelper).size());
        Assert.assertEquals(5, TestHelpers.getAllUniqueOutcomeNotificationRecordsDB(dbHelper).size());

        // Wait a week to clear cached notifications
        time.advanceSystemTimeBy(604_800);

        // Restart the app and re-init OneSignal
        TestHelpers.fastColdRestartApp();
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        // Make sure when notification cache is cleaned so is the unique outcome events cache
        Assert.assertEquals(0, TestHelpers.getAllNotificationRecords(dbHelper).size());
        Assert.assertEquals(0, TestHelpers.getAllUniqueOutcomeNotificationRecordsDB(dbHelper).size());
    }

    @Test
    public void testDelayOutcomes() throws Exception {
        OneSignal.sendOutcome(ONESIGNAL_OUTCOME_NAME);
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        // Foreground app through icon
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        assertMeasureAtIndex(1, ONESIGNAL_OUTCOME_NAME);
    }

    @Test
    public void testSendOutcomesFailWhenRequiresUserPrivacyConsent() throws Exception {
        // Enable IAM v2
        preferences = new MockOSSharedPreferences();
        trackerFactory = new OSTrackerFactory(preferences, logger, time);
        sessionManager = new MockSessionManager(sessionListener, trackerFactory, logger);
        preferences.saveBool(preferences.getPreferencesName(), preferences.getOutcomesV2KeyName(), true);
        OneSignalPackagePrivateHelper.OneSignal_setSharedPreferences(preferences);

        OneSignalInit();
        TestHelpers.threadAndTaskWait();
        assertRestCalls(2);
        OneSignal.setRequiresUserPrivacyConsent(true);

        // Make sure session is UNATTRIBUTED
        assertNotificationChannelUnattributedInfluence();

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Check that the task has been queued until consent is given
        assertRestCalls(2);

        // Send outcome event
        OneSignal.sendOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Ensure still only 2 requests have been made
        assertRestCalls(2);

        OneSignal.provideUserConsent(true);
        TestHelpers.threadAndTaskWait();

        // Send unique outcome event
        OneSignal.sendUniqueOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Send outcome event
        OneSignal.sendOutcome(ONESIGNAL_OUTCOME_NAME);
        TestHelpers.threadAndTaskWait();

        // Make sure session is UNATTRIBUTED
        assertNotificationChannelUnattributedInfluence();

        // Check measure end point was most recent request and contains received notification
        assertMeasureOnV2AtIndex(3, ONESIGNAL_OUTCOME_NAME, null, null, null, null);
    }

    private void foregroundAppAfterClickingNotification() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        // Make sure no notification data exists
        assertNull(notificationOpenedMessage);
        // Make no direct notification id is set
        assertNull(trackerFactory.getNotificationChannelTracker().getDirectId());
        // Make sure all influences are UNATTRIBUTED
        List<OSInfluence> influences = sessionManager.getInfluences();
        for (OSInfluence influence : influences) {
            assertTrue(influence.getInfluenceType().isUnattributed());
        }

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        String notificationID = ONESIGNAL_NOTIFICATION_ID + "1";
        sessionManager.onNotificationReceived(notificationID);
        // Click notification before new session
        OneSignalPackagePrivateHelper.OneSignal_handleNotificationOpen(blankActivity, new JSONArray("[{ \"alert\": \"Test Msg\", \"custom\": { \"i\": \"" + notificationID + "\" } }]"), notificationID);
        TestHelpers.threadAndTaskWait();

        // App opened after clicking notification, but Robolectric needs this to simulate onAppFocus() code after a click
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Check directNotificationId is set to clicked notification
        assertEquals(notificationID, trackerFactory.getNotificationChannelTracker().getDirectId());
        // Make sure notification influence is DIRECT
        assertNotificationChannelDirectInfluence(notificationID);
        // Make sure iam influence is UNATTRIBUTED
        assertIAMChannelUnattributedInfluence();

        // Upgrade on influence will end unattributed session
        assertEquals(1, lastInfluencesEnding.size());
        for (OSInfluence influence : lastInfluencesEnding) {
            assertEquals(OSInfluenceType.UNATTRIBUTED, influence.getInfluenceType());
        }

        // Reset for upcoming asserts
        lastInfluencesEnding = null;
    }

    private void foregroundAppAfterReceivingNotification() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        // Make sure all influences are UNATTRIBUTED
        List<OSInfluence> influences = sessionManager.getInfluences();
        for (OSInfluence influence : influences) {
            assertTrue(influence.getInfluenceType().isUnattributed());
        }

        // Background app
        TestHelpers.pauseActivity(blankActivityController);

        String notificationID = ONESIGNAL_NOTIFICATION_ID + "1";
        // Receive notification
        Bundle bundle = GenerateNotificationRunner.getBaseNotifBundle(ONESIGNAL_NOTIFICATION_ID + "1");
        OneSignalPackagePrivateHelper.FCMBroadcastReceiver_onReceived_withBundle(blankActivity, bundle);

        // Check notification was saved
        assertEquals(1, trackerFactory.getNotificationChannelTracker().getLastReceivedIds().length());
        assertEquals(notificationID, trackerFactory.getNotificationChannelTracker().getLastReceivedIds().get(0));

        // Foreground app through icon
        blankActivityController.resume();
        TestHelpers.threadAndTaskWait();

        // Upgrade on influence will end unattributed session
        assertEquals(1, lastInfluencesEnding.size());
        for (OSInfluence influence : lastInfluencesEnding) {
            assertEquals(OSInfluenceType.UNATTRIBUTED, influence.getInfluenceType());
        }

        // Reset for upcoming asserts
        lastInfluencesEnding = null;
    }

    private void assertNotificationChannelDirectInfluence(String id) throws JSONException {
        OSInfluence influence = trackerFactory.getNotificationChannelTracker().getCurrentSessionInfluence();
        assertTrue(influence.getInfluenceType().isDirect());
        assertEquals(1, influence.getIds().length());
        assertEquals(id, influence.getIds().get(0));
    }

    private void assertNotificationChannelIndirectInfluence(int indirectIdsLength) {
        OSInfluence influence = trackerFactory.getNotificationChannelTracker().getCurrentSessionInfluence();
        assertTrue(influence.getInfluenceType().isIndirect());
        assertEquals(indirectIdsLength, influence.getIds().length());
    }

    private void assertIAMChannelUnattributedInfluence() {
        OSInfluence influence = trackerFactory.getIAMChannelTracker().getCurrentSessionInfluence();
        assertTrue(influence.getInfluenceType().isUnattributed());
        assertNull(influence.getIds());
    }

    private void assertIAMChannelDirectInfluence() {
        OSInfluence influence = trackerFactory.getIAMChannelTracker().getCurrentSessionInfluence();
        assertTrue(influence.getInfluenceType().isDirect());
        assertEquals(1, influence.getIds().length());
    }

    private void assertIAMChannelIndirectInfluence(int indirectIdsLength) {
        OSInfluence influence = trackerFactory.getIAMChannelTracker().getCurrentSessionInfluence();
        assertTrue(influence.getInfluenceType().isIndirect());
        assertEquals(indirectIdsLength, influence.getIds().length());
    }

    private void assertNotificationChannelUnattributedInfluence() {
        OSInfluence influence = trackerFactory.getNotificationChannelTracker().getCurrentSessionInfluence();
        assertTrue(influence.getInfluenceType().isUnattributed());
        assertNull(influence.getIds());
    }

    private void OneSignalInit() throws Exception {
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        ShadowOSUtils.subscribableStatus = 1;
        // Set mocks for mocking behaviour
        OneSignalPackagePrivateHelper.OneSignal_setTime(time);
        OneSignalPackagePrivateHelper.OneSignal_setTrackerFactory(trackerFactory);
        OneSignalPackagePrivateHelper.OneSignal_setSessionManager(sessionManager);
        OneSignal.setAppId(ONESIGNAL_APP_ID);
        OneSignal.initWithContext(blankActivity);
        OneSignal.setNotificationOpenedHandler(getNotificationOpenedHandler());
        TestHelpers.threadAndTaskWait();
        blankActivityController.resume();
    }

    private void OneSignalInit(OneSignal.OSNotificationOpenedHandler notificationOpenedHandler) throws Exception {
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        ShadowOSUtils.subscribableStatus = 1;
        // Set mocks for mocking behaviour
        OneSignalPackagePrivateHelper.OneSignal_setTime(time);
        OneSignalPackagePrivateHelper.OneSignal_setTrackerFactory(trackerFactory);
        OneSignalPackagePrivateHelper.OneSignal_setSessionManager(sessionManager);
        OneSignal.setAppId(ONESIGNAL_APP_ID);
        OneSignal.initWithContext(blankActivity);
        OneSignal.setNotificationOpenedHandler(notificationOpenedHandler);
        TestHelpers.threadAndTaskWait();
        blankActivityController.resume();
    }
}
