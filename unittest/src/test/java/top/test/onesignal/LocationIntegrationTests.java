package top.test.onesignal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import androidx.test.core.app.ApplicationProvider;

import com.huawei.hms.location.HWLocation;
import top.oneconnectapi.onesignal.MockOSLog;
import top.oneconnectapi.onesignal.MockOSSharedPreferences;
import top.oneconnectapi.onesignal.MockOSTimeImpl;
import top.oneconnectapi.onesignal.MockOneSignalDBHelper;
import top.oneconnectapi.onesignal.MockSessionManager;
import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.ShadowCustomTabsClient;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowFocusHandler;
import top.oneconnectapi.onesignal.ShadowFusedLocationApiWrapper;
import top.oneconnectapi.onesignal.ShadowGMSLocationController;
import top.oneconnectapi.onesignal.ShadowGMSLocationUpdateListener;
import top.oneconnectapi.onesignal.ShadowGoogleApiClientBuilder;
import top.oneconnectapi.onesignal.ShadowGoogleApiClientCompatProxy;
import top.oneconnectapi.onesignal.ShadowHMSFusedLocationProviderClient;
import top.oneconnectapi.onesignal.ShadowHMSLocationUpdateListener;
import top.oneconnectapi.onesignal.ShadowHmsInstanceId;
import top.oneconnectapi.onesignal.ShadowHuaweiTask;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignal;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorFCM;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.SyncJobService;
import top.oneconnectapi.onesignal.SyncService;
import top.oneconnectapi.onesignal.example.BlankActivity;
import top.oneconnectapi.onesignal.influence.data.OSTrackerFactory;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Method;

import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_getSessionListener;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_setSessionManager;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_setTime;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_setTrackerFactory;
import static top.oneconnectapi.onesignal.ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse;
import static top.test.onesignal.TestHelpers.afterTestCleanup;
import static top.test.onesignal.TestHelpers.fastColdRestartApp;
import static top.test.onesignal.TestHelpers.restartAppAndElapseTimeToNextSession;
import static top.test.onesignal.TestHelpers.threadAndTaskWait;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import top.oneconnectapi.onesignal.MockOSLog;
import top.oneconnectapi.onesignal.MockOSSharedPreferences;
import top.oneconnectapi.onesignal.MockOSTimeImpl;
import top.oneconnectapi.onesignal.MockOneSignalDBHelper;
import top.oneconnectapi.onesignal.MockSessionManager;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowFocusHandler;
import top.oneconnectapi.onesignal.ShadowFusedLocationApiWrapper;
import top.oneconnectapi.onesignal.ShadowGMSLocationController;
import top.oneconnectapi.onesignal.ShadowGMSLocationUpdateListener;
import top.oneconnectapi.onesignal.ShadowGoogleApiClientBuilder;
import top.oneconnectapi.onesignal.ShadowGoogleApiClientCompatProxy;
import top.oneconnectapi.onesignal.ShadowHMSFusedLocationProviderClient;
import top.oneconnectapi.onesignal.ShadowHuaweiTask;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignal;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorFCM;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.SyncJobService;
import top.oneconnectapi.onesignal.SyncService;

@Config(packageName = "top.oneconnectapi.onesignal.example",
        shadows = {
                ShadowOneSignalRestClient.class,
                ShadowPushRegistratorFCM.class,
                ShadowOSUtils.class,
                ShadowCustomTabsClient.class,
                ShadowCustomTabsSession.class,
                ShadowHmsInstanceId.class,
                ShadowFocusHandler.class
        },
        sdk = 21
)
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class LocationIntegrationTests {

    private static final String ONESIGNAL_APP_ID = "b4f7f966-d8cc-11e4-bed1-df8f05be55ba";

    @SuppressLint("StaticFieldLeak")
    private static Activity blankActivity;
    private static ActivityController<BlankActivity> blankActivityController;

    private MockOSTimeImpl time;
    private OSTrackerFactory trackerFactory;
    private MockSessionManager sessionManager;
    private MockOneSignalDBHelper dbHelper;

    private static void cleanUp() throws Exception {
        ShadowGMSLocationController.reset();

        TestHelpers.beforeTestInitAndCleanup();

        // Set remote_params GET response
        ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse();
    }

    @BeforeClass // Runs only once, before any tests
    public static void setUpClass() throws Exception {
        ShadowLog.stream = System.out;

        TestHelpers.beforeTestSuite();

//        Field OneSignal_CurrentSubscription = OneSignal.class.getDeclaredField("subscribableStatus");
//        OneSignal_CurrentSubscription.setAccessible(true);

        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        StaticResetHelper.saveStaticValues();
    }

    @Before
    public void beforeEachTest() throws Exception {
        blankActivityController = Robolectric.buildActivity(BlankActivity.class).create();
        blankActivity = blankActivityController.get();

        time = new MockOSTimeImpl();
        trackerFactory = new OSTrackerFactory(new MockOSSharedPreferences(), new MockOSLog(), time);
        sessionManager = new MockSessionManager(OneSignalPackagePrivateHelper.OneSignal_getSessionListener(), trackerFactory, new MockOSLog());
        dbHelper = new MockOneSignalDBHelper(ApplicationProvider.getApplicationContext());

        TestHelpers.setupTestWorkManager(blankActivity);

        cleanUp();

        OneSignalPackagePrivateHelper.OneSignal_setTime(time);
    }

    @After
    public void afterEachTest() throws Exception {
        TestHelpers.afterTestCleanup();
    }

    @AfterClass
    public static void afterEverything() throws Exception {
        cleanUp();
    }

    @Test
    @Config(shadows = {ShadowGoogleApiClientBuilder.class, ShadowGoogleApiClientCompatProxy.class, ShadowFusedLocationApiWrapper.class})
    public void shouldUpdateAllLocationFieldsWhenTimeStampChanges() throws Exception {
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");
        OneSignalInit();
        TestHelpers.threadAndTaskWait();
        assertEquals(1.0, ShadowOneSignalRestClient.lastPost.getDouble("lat"));
        assertEquals(2.0, ShadowOneSignalRestClient.lastPost.getDouble("long"));
        assertEquals(3.0, ShadowOneSignalRestClient.lastPost.getDouble("loc_acc"));
        assertEquals(0.0, ShadowOneSignalRestClient.lastPost.getDouble("loc_type"));

        ShadowOneSignalRestClient.lastPost = null;
        ShadowFusedLocationApiWrapper.lat = 30d;
        ShadowFusedLocationApiWrapper.log = 2.0d;
        ShadowFusedLocationApiWrapper.accuracy = 5.0f;
        ShadowFusedLocationApiWrapper.time = 2L;
        TestHelpers.restartAppAndElapseTimeToNextSession(time);
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        assertEquals(30.0, ShadowOneSignalRestClient.lastPost.getDouble("lat"));
        assertEquals(2.0, ShadowOneSignalRestClient.lastPost.getDouble("long"));
        assertEquals(5.0, ShadowOneSignalRestClient.lastPost.getDouble("loc_acc"));
        assertEquals(0.0, ShadowOneSignalRestClient.lastPost.getDouble("loc_type"));
    }

    @Test
    @Config(shadows = {ShadowOneSignal.class})
    @SuppressWarnings("unchecked") // getDeclaredMethod
    public void testLocationTimeout() throws Exception {
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        Class klass = Class.forName("top.oneconnectapi.onesignal.GMSLocationController");
        Method method = klass.getDeclaredMethod("startFallBackThread");
        method.setAccessible(true);
        method.invoke(null);
        method = klass.getDeclaredMethod("fireFailedComplete");
        method.setAccessible(true);
        method.invoke(null);
        TestHelpers.threadAndTaskWait();

        assertFalse(ShadowOneSignal.messages.contains("GoogleApiClient timeout"));
    }

    @Test
    @Config(shadows = {
            ShadowGoogleApiClientBuilder.class,
            ShadowGoogleApiClientCompatProxy.class,
            ShadowFusedLocationApiWrapper.class },
            sdk = 19)
    public void testLocationSchedule() throws Exception {
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_FINE_LOCATION");
        ShadowFusedLocationApiWrapper.lat = 1.0d;
        ShadowFusedLocationApiWrapper.log = 2.0d;
        ShadowFusedLocationApiWrapper.accuracy = 3.0f;
        ShadowFusedLocationApiWrapper.time = 12345L;

        // location if we have permission
        OneSignalInit();
        TestHelpers.threadAndTaskWait();
        assertEquals(1.0, ShadowOneSignalRestClient.lastPost.optDouble("lat"));
        assertEquals(2.0, ShadowOneSignalRestClient.lastPost.optDouble("long"));
        assertEquals(3.0, ShadowOneSignalRestClient.lastPost.optDouble("loc_acc"));
        assertEquals(1, ShadowOneSignalRestClient.lastPost.optInt("loc_type"));

        // Checking make sure an update is scheduled.
        AlarmManager alarmManager = (AlarmManager)ApplicationProvider.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        assertEquals(1, shadowOf(alarmManager).getScheduledAlarms().size());
        Intent intent = shadowOf(shadowOf(alarmManager).getNextScheduledAlarm().operation).getSavedIntent();
        assertEquals(SyncService.class, shadowOf(intent).getIntentClass());

        // Setting up a new point and testing it is sent
        Location fakeLocation = new Location("UnitTest");
        fakeLocation.setLatitude(1.1d);
        fakeLocation.setLongitude(2.2d);
        fakeLocation.setAccuracy(3.3f);
        fakeLocation.setTime(12346L);
        ShadowGMSLocationUpdateListener.provideFakeLocation(fakeLocation);

        Robolectric.buildService(SyncService.class, intent).startCommand(0, 0);
        TestHelpers.threadAndTaskWait();
        assertEquals(1.1d, ShadowOneSignalRestClient.lastPost.optDouble("lat"));
        assertEquals(2.2d, ShadowOneSignalRestClient.lastPost.optDouble("long"));
        assertEquals(3.3f, ShadowOneSignalRestClient.lastPost.opt("loc_acc"));

        assertEquals(false, ShadowOneSignalRestClient.lastPost.opt("loc_bg"));

        // Testing loc_bg
        blankActivityController.pause();
        TestHelpers.threadAndTaskWait();
        fakeLocation.setTime(12347L);
        ShadowGMSLocationUpdateListener.provideFakeLocation(fakeLocation);
        Robolectric.buildService(SyncService.class, intent).startCommand(0, 0);
        TestHelpers.threadAndTaskWait();
        assertEquals(1.1d, ShadowOneSignalRestClient.lastPost.optDouble("lat"));
        assertEquals(2.2d, ShadowOneSignalRestClient.lastPost.optDouble("long"));
        assertEquals(3.3f, ShadowOneSignalRestClient.lastPost.opt("loc_acc"));
        assertEquals(true, ShadowOneSignalRestClient.lastPost.opt("loc_bg"));
        assertEquals(1, ShadowOneSignalRestClient.lastPost.optInt("loc_type"));
    }

    @Test
    @Config(shadows = {
            ShadowGoogleApiClientBuilder.class,
            ShadowGoogleApiClientCompatProxy.class,
            ShadowFusedLocationApiWrapper.class },
            sdk = 19)
    public void testLocationFromSyncAlarm() throws Exception {
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");

        ShadowFusedLocationApiWrapper.lat = 1.1d;
        ShadowFusedLocationApiWrapper.log = 2.1d;
        ShadowFusedLocationApiWrapper.accuracy = 3.1f;
        ShadowFusedLocationApiWrapper.time = 12346L;

        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        TestHelpers.fastColdRestartApp();
        AlarmManager alarmManager = (AlarmManager)ApplicationProvider.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        shadowOf(alarmManager).getScheduledAlarms().clear();
        ShadowOneSignalRestClient.lastPost = null;

        ShadowFusedLocationApiWrapper.lat = 1.0;
        ShadowFusedLocationApiWrapper.log = 2.0d;
        ShadowFusedLocationApiWrapper.accuracy = 3.0f;
        ShadowFusedLocationApiWrapper.time = 12345L;

        blankActivityController.pause();
        TestHelpers.threadAndTaskWait();

        Robolectric.buildService(SyncService.class, new Intent()).startCommand(0, 0);
        TestHelpers.threadAndTaskWait();

        assertEquals(1.0, ShadowOneSignalRestClient.lastPost.optDouble("lat"));
        assertEquals(2.0, ShadowOneSignalRestClient.lastPost.optDouble("long"));
        assertEquals(3.0, ShadowOneSignalRestClient.lastPost.optDouble("loc_acc"));
        assertEquals(0, ShadowOneSignalRestClient.lastPost.optInt("loc_type"));
        assertEquals(12345L, ShadowOneSignalRestClient.lastPost.optInt("loc_time_stamp"));
        assertEquals(true, ShadowOneSignalRestClient.lastPost.opt("loc_bg"));

        // Checking make sure an update is scheduled.
        alarmManager = (AlarmManager)ApplicationProvider.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        assertEquals(1, shadowOf(alarmManager).getScheduledAlarms().size());
        Intent intent = shadowOf(shadowOf(alarmManager).getNextScheduledAlarm().operation).getSavedIntent();
        assertEquals(SyncService.class, shadowOf(intent).getIntentClass());
        shadowOf(alarmManager).getScheduledAlarms().clear();
    }

    @Test
    @Config(shadows = {ShadowGoogleApiClientBuilder.class, ShadowGoogleApiClientCompatProxy.class, ShadowFusedLocationApiWrapper.class})
    public void shouldSendLocationToEmailRecord() throws Exception {
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");

        OneSignalInit();
        OneSignal.setEmail("josh@onesignal.com");
        TestHelpers.threadAndTaskWait();

        JSONObject postEmailPayload = ShadowOneSignalRestClient.requests.get(2).payload;
        assertEquals(11, postEmailPayload.getInt("device_type"));
        assertEquals(1.0, postEmailPayload.getDouble("lat"));
        assertEquals(2.0, postEmailPayload.getDouble("long"));
        assertEquals(3.0, postEmailPayload.getDouble("loc_acc"));
        assertEquals(0.0, postEmailPayload.getDouble("loc_type"));
    }

    @Test
    @Config(shadows = {ShadowGoogleApiClientBuilder.class, ShadowGoogleApiClientCompatProxy.class, ShadowFusedLocationApiWrapper.class})
    public void shouldSendLocationToSMSRecord() throws Exception {
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");

        OneSignalInit();
        OneSignal.setSMSNumber("123456789");
        TestHelpers.threadAndTaskWait();

        JSONObject postSMSPayload = ShadowOneSignalRestClient.requests.get(2).payload;
        assertEquals(OneSignalPackagePrivateHelper.UserState.DEVICE_TYPE_SMS, postSMSPayload.getInt("device_type"));
        assertEquals(1.0, postSMSPayload.getDouble("lat"));
        assertEquals(2.0, postSMSPayload.getDouble("long"));
        assertEquals(3.0, postSMSPayload.getDouble("loc_acc"));
        assertEquals(0.0, postSMSPayload.getDouble("loc_type"));
    }

    @Test
    @Config(shadows = {ShadowGoogleApiClientBuilder.class, ShadowGoogleApiClientCompatProxy.class, ShadowFusedLocationApiWrapper.class})
    public void shouldRegisterWhenPromptingAfterInit() throws Exception {
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");
        ShadowGoogleApiClientCompatProxy.skipOnConnected = true;

        // Test promptLocation right after init race condition
        OneSignalInit();
        OneSignal.promptLocation();

        ShadowGoogleApiClientBuilder.connectionCallback.onConnected(null);
        TestHelpers.threadAndTaskWait();

        ShadowOneSignalRestClient.Request request = ShadowOneSignalRestClient.requests.get(1);
        assertEquals(ShadowOneSignalRestClient.REST_METHOD.POST, request.method);
        assertEquals(1, request.payload.get("device_type"));
        assertEquals(ShadowPushRegistratorFCM.regId, request.payload.get("identifier"));
    }

    @Test
    @Config(shadows = {ShadowGoogleApiClientBuilder.class, ShadowGoogleApiClientCompatProxy.class, ShadowFusedLocationApiWrapper.class})
    public void shouldCallOnSessionEvenIfSyncJobStarted() throws Exception {
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");

        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        TestHelpers.restartAppAndElapseTimeToNextSession(time);
        ShadowGoogleApiClientCompatProxy.skipOnConnected = true;
        OneSignalInit();

        SyncJobService syncJobService = Robolectric.buildService(SyncJobService.class).create().get();
        syncJobService.onStartJob(null);
        TestHelpers.getThreadByName("OS_SYNCSRV_BG_SYNC").join();
        OneSignalPackagePrivateHelper.runAllNetworkRunnables();
        ShadowGoogleApiClientBuilder.connectionCallback.onConnected(null);
        TestHelpers.threadAndTaskWait();

        ShadowOneSignalRestClient.Request request = ShadowOneSignalRestClient.requests.get(3);
        assertEquals(ShadowOneSignalRestClient.REST_METHOD.POST, request.method);
        assertEquals("players/a2f7f967-e8cc-11e4-bed1-118f05be4511/on_session", request.url);
    }

    // ####### Unit Test Huawei Location ########

    @Test
    @Config(shadows = {ShadowHMSFusedLocationProviderClient.class})
    public void shouldUpdateAllLocationFieldsWhenTimeStampChanges_Huawei() throws Exception {
        ShadowOSUtils.supportsHMS(true);
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");
        OneSignalInit();
        TestHelpers.threadAndTaskWait();
        assertEquals(1.0, ShadowOneSignalRestClient.lastPost.getDouble("lat"));
        assertEquals(2.0, ShadowOneSignalRestClient.lastPost.getDouble("long"));
        assertEquals(3.0, ShadowOneSignalRestClient.lastPost.getDouble("loc_acc"));
        assertEquals(0.0, ShadowOneSignalRestClient.lastPost.getDouble("loc_type"));

        ShadowOneSignalRestClient.lastPost = null;
        ShadowHMSFusedLocationProviderClient.resetStatics();
        ShadowHMSFusedLocationProviderClient.lat = 30d;
        ShadowHMSFusedLocationProviderClient.log = 2.0d;
        ShadowHMSFusedLocationProviderClient.accuracy = 5.0f;
        ShadowHMSFusedLocationProviderClient.time = 2L;
        TestHelpers.restartAppAndElapseTimeToNextSession(time);
        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        assertEquals(30.0, ShadowOneSignalRestClient.lastPost.getDouble("lat"));
        assertEquals(2.0, ShadowOneSignalRestClient.lastPost.getDouble("long"));
        assertEquals(5.0, ShadowOneSignalRestClient.lastPost.getDouble("loc_acc"));
        assertEquals(0.0, ShadowOneSignalRestClient.lastPost.getDouble("loc_type"));
    }

    @Test
    @Config(shadows = {
            ShadowHMSFusedLocationProviderClient.class
    }, sdk = 19)
    public void testLocationSchedule_Huawei() throws Exception {
        ShadowOSUtils.supportsHMS(true);
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_FINE_LOCATION");
        ShadowHMSFusedLocationProviderClient.lat = 1.0d;
        ShadowHMSFusedLocationProviderClient.log = 2.0d;
        ShadowHMSFusedLocationProviderClient.accuracy = 3.0f;
        ShadowHMSFusedLocationProviderClient.time = 12345L;

        // location if we have permission
        OneSignalInit();
        TestHelpers.threadAndTaskWait();
        assertEquals(1.0, ShadowOneSignalRestClient.lastPost.optDouble("lat"));
        assertEquals(2.0, ShadowOneSignalRestClient.lastPost.optDouble("long"));
        assertEquals(3.0, ShadowOneSignalRestClient.lastPost.optDouble("loc_acc"));
        assertEquals(1, ShadowOneSignalRestClient.lastPost.optInt("loc_type"));

        // Checking make sure an update is scheduled.
        AlarmManager alarmManager = (AlarmManager)ApplicationProvider.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        assertEquals(1, shadowOf(alarmManager).getScheduledAlarms().size());
        Intent intent = shadowOf(shadowOf(alarmManager).getNextScheduledAlarm().operation).getSavedIntent();
        assertEquals(SyncService.class, shadowOf(intent).getIntentClass());

        // Setting up a new point and testing it is sent
        HWLocation fakeLocation = new HWLocation();
        fakeLocation.setLatitude(1.1d);
        fakeLocation.setLongitude(2.2d);
        fakeLocation.setAccuracy(3.3f);
        fakeLocation.setTime(12346L);
        ShadowHMSLocationUpdateListener.provideFakeLocation_Huawei(fakeLocation);

        Robolectric.buildService(SyncService.class, intent).startCommand(0, 0);
        TestHelpers.threadAndTaskWait();
        assertEquals(1.1d, ShadowOneSignalRestClient.lastPost.optDouble("lat"));
        assertEquals(2.2d, ShadowOneSignalRestClient.lastPost.optDouble("long"));
        assertEquals(3.3f, ShadowOneSignalRestClient.lastPost.opt("loc_acc"));

        assertEquals(false, ShadowOneSignalRestClient.lastPost.opt("loc_bg"));

        // Testing loc_bg
        blankActivityController.pause();
        TestHelpers.threadAndTaskWait();

        fakeLocation.setTime(12347L);
        ShadowHMSLocationUpdateListener.provideFakeLocation_Huawei(fakeLocation);
        Robolectric.buildService(SyncService.class, intent).startCommand(0, 0);
        TestHelpers.threadAndTaskWait();
        assertEquals(1.1d, ShadowOneSignalRestClient.lastPost.optDouble("lat"));
        assertEquals(2.2d, ShadowOneSignalRestClient.lastPost.optDouble("long"));
        assertEquals(3.3f, ShadowOneSignalRestClient.lastPost.opt("loc_acc"));
        assertEquals(true, ShadowOneSignalRestClient.lastPost.opt("loc_bg"));
        assertEquals(1, ShadowOneSignalRestClient.lastPost.optInt("loc_type"));
    }

    @Test
    @Config(shadows = {
            ShadowHMSFusedLocationProviderClient.class,
            ShadowHuaweiTask.class
    }, sdk = 19)
    public void testLocationFromSyncAlarm_Huawei() throws Exception {
        ShadowOSUtils.supportsHMS(true);
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");

        ShadowHMSFusedLocationProviderClient.lat = 1.1d;
        ShadowHMSFusedLocationProviderClient.log = 2.1d;
        ShadowHMSFusedLocationProviderClient.accuracy = 3.1f;
        ShadowHMSFusedLocationProviderClient.time = 12346L;

        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        TestHelpers.fastColdRestartApp();
        AlarmManager alarmManager = (AlarmManager)ApplicationProvider.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        shadowOf(alarmManager).getScheduledAlarms().clear();

        ShadowOneSignalRestClient.lastPost = null;
        ShadowHMSFusedLocationProviderClient.resetStatics();
        ShadowHMSFusedLocationProviderClient.lat = 1.0;
        ShadowHMSFusedLocationProviderClient.log = 2.0d;
        ShadowHMSFusedLocationProviderClient.accuracy = 3.0f;
        ShadowHMSFusedLocationProviderClient.time = 12345L;
        ShadowHMSFusedLocationProviderClient.shadowTask = true;
        ShadowHuaweiTask.result = ShadowHMSFusedLocationProviderClient.getLocation();

        Robolectric.buildService(SyncService.class, new Intent()).startCommand(0, 0);
        TestHelpers.threadAndTaskWait();

        assertEquals(1.0, ShadowOneSignalRestClient.lastPost.optDouble("lat"));
        assertEquals(2.0, ShadowOneSignalRestClient.lastPost.optDouble("long"));
        assertEquals(3.0, ShadowOneSignalRestClient.lastPost.optDouble("loc_acc"));
        assertEquals(0, ShadowOneSignalRestClient.lastPost.optInt("loc_type"));
        assertEquals(12345L, ShadowOneSignalRestClient.lastPost.optInt("loc_time_stamp"));
        assertEquals(true, ShadowOneSignalRestClient.lastPost.opt("loc_bg"));

        // Checking make sure an update is scheduled.
        alarmManager = (AlarmManager)ApplicationProvider.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        assertEquals(1, shadowOf(alarmManager).getScheduledAlarms().size());
        Intent intent = shadowOf(shadowOf(alarmManager).getNextScheduledAlarm().operation).getSavedIntent();
        assertEquals(SyncService.class, shadowOf(intent).getIntentClass());
        shadowOf(alarmManager).getScheduledAlarms().clear();
    }

    @Test
    @Config(shadows = {ShadowHMSFusedLocationProviderClient.class})
    public void shouldSendLocationToEmailRecord_Huawei() throws Exception {
        ShadowOSUtils.supportsHMS(true);
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");

        OneSignalInit();
        OneSignal.setEmail("josh@onesignal.com");
        TestHelpers.threadAndTaskWait();

        JSONObject postEmailPayload = ShadowOneSignalRestClient.requests.get(2).payload;
        assertEquals(11, postEmailPayload.getInt("device_type"));
        assertEquals(1.0, postEmailPayload.getDouble("lat"));
        assertEquals(2.0, postEmailPayload.getDouble("long"));
        assertEquals(3.0, postEmailPayload.getDouble("loc_acc"));
        assertEquals(0.0, postEmailPayload.getDouble("loc_type"));
    }

    @Test
    @Config(shadows = {ShadowHMSFusedLocationProviderClient.class})
    public void shouldSendLocationToSMSRecord_Huawei() throws Exception {
        ShadowOSUtils.supportsHMS(true);
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");

        OneSignalInit();
        OneSignal.setSMSNumber("123456789");
        TestHelpers.threadAndTaskWait();

        JSONObject postEmailPayload = ShadowOneSignalRestClient.requests.get(2).payload;
        assertEquals(OneSignalPackagePrivateHelper.UserState.DEVICE_TYPE_SMS, postEmailPayload.getInt("device_type"));
        assertEquals(1.0, postEmailPayload.getDouble("lat"));
        assertEquals(2.0, postEmailPayload.getDouble("long"));
        assertEquals(3.0, postEmailPayload.getDouble("loc_acc"));
        assertEquals(0.0, postEmailPayload.getDouble("loc_type"));
    }

    @Test
    @Config(shadows = {ShadowHMSFusedLocationProviderClient.class, ShadowHuaweiTask.class})
    public void shouldRegisterWhenPromptingAfterInit_Huawei() throws Exception {
        ShadowOSUtils.supportsHMS(true);
        ShadowHMSFusedLocationProviderClient.skipOnGetLocation = true;
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");

        // Test promptLocation right after init race condition
        OneSignalInit();
        OneSignal.promptLocation();

        ShadowHuaweiTask.callSuccessListener(ShadowHMSFusedLocationProviderClient.getLocation());
        TestHelpers.threadAndTaskWait();

        ShadowOneSignalRestClient.Request request = ShadowOneSignalRestClient.requests.get(1);
        assertEquals(ShadowOneSignalRestClient.REST_METHOD.POST, request.method);
        assertEquals(OneSignalPackagePrivateHelper.UserState.DEVICE_TYPE_HUAWEI, request.payload.get("device_type"));
        assertEquals(ShadowHmsInstanceId.token, request.payload.get("identifier"));
    }

    @Test
    @Config(shadows = {ShadowHMSFusedLocationProviderClient.class, ShadowHuaweiTask.class})
    public void shouldCallOnSessionEvenIfSyncJobStarted_Huawei() throws Exception {
        ShadowOSUtils.supportsHMS(true);
        ShadowHMSFusedLocationProviderClient.shadowTask = true;
        ShadowHuaweiTask.result = ShadowHMSFusedLocationProviderClient.getLocation();
        shadowOf(RuntimeEnvironment.application).grantPermissions("android.permission.ACCESS_COARSE_LOCATION");

        OneSignalInit();
        TestHelpers.threadAndTaskWait();

        TestHelpers.restartAppAndElapseTimeToNextSession(time);
        ShadowHMSFusedLocationProviderClient.skipOnGetLocation = true;
        OneSignalInit();

        SyncJobService syncJobService = Robolectric.buildService(SyncJobService.class).create().get();
        syncJobService.onStartJob(null);
        TestHelpers.getThreadByName("OS_SYNCSRV_BG_SYNC").join();
        OneSignalPackagePrivateHelper.runAllNetworkRunnables();
        ShadowHuaweiTask.callSuccessListener(ShadowHMSFusedLocationProviderClient.getLocation());
        TestHelpers.threadAndTaskWait();

        ShadowOneSignalRestClient.Request request = ShadowOneSignalRestClient.requests.get(3);
        assertEquals(ShadowOneSignalRestClient.REST_METHOD.POST, request.method);
        assertEquals("players/a2f7f967-e8cc-11e4-bed1-118f05be4511/on_session", request.url);
    }

    private void OneSignalInit() {
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        ShadowOSUtils.subscribableStatus = 1;
        OneSignalPackagePrivateHelper.OneSignal_setTime(time);
        OneSignalPackagePrivateHelper.OneSignal_setTrackerFactory(trackerFactory);
        OneSignalPackagePrivateHelper.OneSignal_setSessionManager(sessionManager);
        OneSignal.setAppId(ONESIGNAL_APP_ID);
        OneSignal.initWithContext(blankActivity);
        blankActivityController.resume();
    }
}
