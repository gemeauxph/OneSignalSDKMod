package top.test.onesignal;

import android.annotation.SuppressLint;
import android.app.Activity;

import androidx.annotation.Nullable;

import top.oneconnectapi.onesignal.InAppMessagingHelpers;
import top.oneconnectapi.onesignal.MockOSTimeImpl;
import top.oneconnectapi.onesignal.OSInAppMessageLifecycleHandler;
import top.oneconnectapi.onesignal.OSInAppMessage;
import top.oneconnectapi.onesignal.OSInAppMessageAction;
import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSTestInAppMessageInternal;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSTestInAppMessageAction;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSTestTrigger;
import top.oneconnectapi.onesignal.ShadowCustomTabsClient;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowDynamicTimer;
import top.oneconnectapi.onesignal.ShadowJobService;
import top.oneconnectapi.onesignal.ShadowNotificationManagerCompat;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorFCM;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.example.BlankActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSTestTrigger.OSTriggerKind;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSTestTrigger.OSTriggerOperator;
import static top.oneconnectapi.onesignal.ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OneSignal_setTime;
import static top.test.onesignal.TestHelpers.assertMainThread;
import static top.test.onesignal.TestHelpers.threadAndTaskWait;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import junit.framework.Assert;

import top.oneconnectapi.onesignal.MockOSTimeImpl;
import top.oneconnectapi.onesignal.OSInAppMessage;
import top.oneconnectapi.onesignal.OSTrigger;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.ShadowCustomTabsSession;
import top.oneconnectapi.onesignal.ShadowDynamicTimer;
import top.oneconnectapi.onesignal.ShadowJobService;
import top.oneconnectapi.onesignal.ShadowNotificationManagerCompat;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.ShadowPushRegistratorFCM;
import top.oneconnectapi.onesignal.StaticResetHelper;

@Config(packageName = "top.oneconnectapi.onesignal.example",
        shadows = {
            ShadowOneSignalRestClient.class,
            ShadowPushRegistratorFCM.class,
            ShadowOSUtils.class,
            ShadowCustomTabsClient.class,
            ShadowCustomTabsSession.class,
            ShadowNotificationManagerCompat.class,
            ShadowJobService.class,
            ShadowDynamicTimer.class,
        },
        sdk = 26
)
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class InAppMessagingUnitTests {

    private static final String IAM_CLICK_ID = "button_id_123";
    private static final double REQUIRED_TIMER_ACCURACY = 1.25;
    private static final int LIMIT = 5;
    private static final long DELAY = 60;

    private static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal message;

    @SuppressLint("StaticFieldLeak")
    private static Activity blankActivity;
    private static ActivityController<BlankActivity> blankActivityController;

    @BeforeClass
    public static void setupClass() throws Exception {
        ShadowLog.stream = System.out;

        message = InAppMessagingHelpers.buildTestMessageWithSingleTrigger(
           OSTrigger.OSTriggerKind.SESSION_TIME,
           null,
           OSTrigger.OSTriggerOperator.GREATER_THAN_OR_EQUAL_TO.toString(),
           3
        );

        TestHelpers.beforeTestSuite();

        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        StaticResetHelper.saveStaticValues();
    }

    @Before
    public void beforeEachTest() throws Exception {
        blankActivityController = Robolectric.buildActivity(BlankActivity.class).create();
        blankActivity = blankActivityController.get();
        lastAction = null;
        lastMessage = null;
        iamLifecycleCounter = 0;

        TestHelpers.beforeTestInitAndCleanup();

        OneSignalInit();
    }

    @After
    public void afterEachTest() throws Exception {
        // reset back to the default
        ShadowDynamicTimer.shouldScheduleTimers = true;
        ShadowDynamicTimer.hasScheduledTimer = false;
        TestHelpers.afterTestCleanup();

        InAppMessagingHelpers.clearTestState();
    }

    private static void setLocalTriggerValue(String key, Object localValue) {
        if (localValue != null)
            OneSignal.addTrigger(key, localValue);
        else
            OneSignal.removeTriggerForKey(key);
    }

    /**
     * Convenience function that saves a local trigger (localValue) for the property name "test_property"
     * then creates an in-app message with a trigger (triggerValue) for the same property name. It
     * then evaluates the message for the given trigger conditions and returns the boolean, which
     * indicates whether or not the message should be shown.
     *
     * For example, we can set up a test where the app has a property value of 3 and we want to
     * test to make sure that if a message has a trigger value of 2 and an operator > that it
     * returns true when evaluated, because 3 > 2
     */
    private static boolean comparativeOperatorTest(OSTrigger.OSTriggerOperator operator, Object triggerValue, Object localValue) throws JSONException {
        setLocalTriggerValue("test_property", localValue);
        OneSignalPackagePrivateHelper.OSTestInAppMessageInternal testMessage = InAppMessagingHelpers.buildTestMessageWithSingleTrigger(OSTrigger.OSTriggerKind.CUSTOM, "test_property", operator.toString(), triggerValue);
        return InAppMessagingHelpers.evaluateMessage(testMessage);
    }

    @Test
    public void testBuiltMessage() {
        UUID.fromString(message.getMessageId()); // Throws if invalid
        assertNotNull(message.variants);
    }

    @Test
    public void testBuiltMessageVariants() {
        assertEquals(message.variants.get("android").get("es"), InAppMessagingHelpers.TEST_SPANISH_ANDROID_VARIANT_ID);
        assertEquals(message.variants.get("android").get("en"), InAppMessagingHelpers.TEST_ENGLISH_ANDROID_VARIANT_ID);
    }

    @Test
    public void testBuiltMessageReDisplay() throws JSONException {
        OneSignalPackagePrivateHelper.OSTestInAppMessageInternal message = InAppMessagingHelpers.buildTestMessageWitRedisplay(
                LIMIT,
                DELAY
        );
        assertTrue(message.getRedisplayStats().isRedisplayEnabled());
        assertEquals(LIMIT, message.getRedisplayStats().getDisplayLimit());
        assertEquals(DELAY, message.getRedisplayStats().getDisplayDelay());
        assertEquals(-1, message.getRedisplayStats().getLastDisplayTime());
        assertEquals(0, message.getRedisplayStats().getDisplayQuantity());

        OneSignalPackagePrivateHelper.OSTestInAppMessageInternal messageWithoutDisplay = InAppMessagingHelpers.buildTestMessageWithSingleTrigger(
                OSTrigger.OSTriggerKind.SESSION_TIME,
                null,
                OSTrigger.OSTriggerOperator.GREATER_THAN_OR_EQUAL_TO.toString(),
                3
        );
        assertFalse(messageWithoutDisplay.getRedisplayStats().isRedisplayEnabled());
        assertEquals(1, messageWithoutDisplay.getRedisplayStats().getDisplayLimit());
        assertEquals(0, messageWithoutDisplay.getRedisplayStats().getDisplayDelay());
        assertEquals(-1, messageWithoutDisplay.getRedisplayStats().getLastDisplayTime());
        assertEquals(0, messageWithoutDisplay.getRedisplayStats().getDisplayQuantity());
    }

    @Test
    public void testBuiltMessageRedisplayLimit() throws JSONException {
        OneSignalPackagePrivateHelper.OSTestInAppMessageInternal message = InAppMessagingHelpers.buildTestMessageWitRedisplay(
                LIMIT,
                DELAY
        );

        for (int i = 0; i < LIMIT; i++) {
            assertTrue(message.getRedisplayStats().shouldDisplayAgain());
            message.getRedisplayStats().incrementDisplayQuantity();
        }

        message.getRedisplayStats().incrementDisplayQuantity();
        assertFalse(message.getRedisplayStats().shouldDisplayAgain());
    }

    @Test
    public void testBuiltMessageRedisplayDelay() throws JSONException {
        MockOSTimeImpl time = new MockOSTimeImpl();
        OneSignalPackagePrivateHelper.OneSignal_setTime(time);
        OneSignalPackagePrivateHelper.OSTestInAppMessageInternal message = InAppMessagingHelpers.buildTestMessageWitRedisplay(
                LIMIT,
                DELAY
        );

        assertTrue(message.getRedisplayStats().isDelayTimeSatisfied());

        message.getRedisplayStats().setLastDisplayTimeToCurrent(time);
        time.advanceSystemTimeBy(DELAY);
        assertTrue(message.getRedisplayStats().isDelayTimeSatisfied());

        message.getRedisplayStats().setLastDisplayTimeToCurrent(time);
        time.advanceSystemTimeBy(DELAY - 1);
        assertFalse(message.getRedisplayStats().isDelayTimeSatisfied());
    }

    @Test
    public void testBuiltMessageRedisplayCLickId() throws JSONException {
        OneSignalPackagePrivateHelper.OSTestInAppMessageInternal message = InAppMessagingHelpers.buildTestMessageWitRedisplay(
                LIMIT,
                DELAY
        );

        assertTrue(message.getClickedClickIds().isEmpty());
        assertTrue(message.isClickAvailable(IAM_CLICK_ID));

        message.addClickId(IAM_CLICK_ID);
        message.clearClickIds();

        assertTrue(message.getClickedClickIds().isEmpty());

        message.addClickId(IAM_CLICK_ID);
        message.addClickId(IAM_CLICK_ID);
        assertEquals(1, message.getClickedClickIds().size());

        assertFalse(message.isClickAvailable(IAM_CLICK_ID));

        OneSignalPackagePrivateHelper.OSTestInAppMessageInternal messageWithoutDisplay = InAppMessagingHelpers.buildTestMessageWithSingleTrigger(
                OSTrigger.OSTriggerKind.SESSION_TIME,
                null,
                OSTrigger.OSTriggerOperator.GREATER_THAN_OR_EQUAL_TO.toString(),
                3
        );

        messageWithoutDisplay.addClickId(IAM_CLICK_ID);
        assertFalse(messageWithoutDisplay.isClickAvailable(IAM_CLICK_ID));
    }

    @Test
    public void testBuiltMessageTrigger() {
        OneSignalPackagePrivateHelper.OSTestTrigger trigger = (OneSignalPackagePrivateHelper.OSTestTrigger)message.triggers.get(0).get(0);

        Assert.assertEquals(trigger.kind, OSTrigger.OSTriggerKind.SESSION_TIME);
        Assert.assertEquals(trigger.operatorType, OSTrigger.OSTriggerOperator.GREATER_THAN_OR_EQUAL_TO);
        assertNull(trigger.property);
        Assert.assertEquals(trigger.value, 3);
    }

    @Test
    public void testParsesMessageActions() throws JSONException {
        OneSignalPackagePrivateHelper.OSTestInAppMessageAction action = new OneSignalPackagePrivateHelper.OSTestInAppMessageAction(InAppMessagingHelpers.buildTestActionJson());

        assertEquals(action.getClickId(), InAppMessagingHelpers.IAM_CLICK_ID);
        assertEquals(action.getClickName(), "click_name");
        assertEquals(action.getClickUrl(), "https://www.onesignal.com");
        assertTrue(action.closes());
        assertEquals(action.getUrlTarget(), OSInAppMessageAction.OSInAppMessageActionUrlType.IN_APP_WEBVIEW);
    }

    @Test
    public void testSaveMultipleTriggerValuesGetTrigger() throws Exception {
        HashMap<String, Object> testTriggers = new HashMap<>();
        testTriggers.put("test1", "value1");
        testTriggers.put("test2", "value2");

        OneSignal.addTriggers(testTriggers);

        Map<String, Object> triggers = OneSignal.getTriggers();
        assertEquals(2, triggers.entrySet().size());

        for (Map.Entry<String, Object> entry : triggers.entrySet()) {
            assertEquals(testTriggers.get(entry.getKey()), entry.getValue());
        }
    }

    @Test
    public void testSaveMultipleTriggerValues() {
        HashMap<String, Object> testTriggers = new HashMap<>();
        testTriggers.put("test1", "value1");
        testTriggers.put("test2", "value2");

        OneSignal.addTriggers(testTriggers);

        assertEquals(OneSignal.getTriggerValueForKey("test1"), "value1");
        assertEquals(OneSignal.getTriggerValueForKey("test2"), "value2");
    }

    @Test
    public void testAddTriggersFromJsonString_StringsTest() throws Exception {
        JSONObject jsonObject = new JSONObject() {{
            put("key1", "value1");
            put("key2", "value2");
        }};

        addTriggersFromJsonString(jsonObject.toString());

        assertEquals(OneSignal.getTriggerValueForKey("key1"), "value1");
        assertEquals(OneSignal.getTriggerValueForKey("key2"), "value2");
    }

    @Test
    public void testAddTriggersFromJsonString_NullValue() throws Exception {
        JSONObject jsonObject = new JSONObject() {{
            put("key", null);
        }};

        addTriggersFromJsonString(jsonObject.toString());

        assertNull(OneSignal.getTriggerValueForKey("key"));
    }

    @Test
    public void testAddTriggersFromJsonString_IntTest() throws Exception {
        JSONObject jsonObject = new JSONObject() {{
            put("key", 1);
        }};

        addTriggersFromJsonString(jsonObject.toString());

        assertEquals(1, OneSignal.getTriggerValueForKey("key"));
    }

    @Test
    public void testAddTriggersFromJsonString_NestedJSONArray() throws Exception {
        JSONObject jsonObject = new JSONObject() {{
            put("key", new JSONArray() {{
                put("value");
            }});
        }};

        addTriggersFromJsonString(jsonObject.toString());

        assertEquals(
           new ArrayList<String>() {{
               add("value");
           }},
           OneSignal.getTriggerValueForKey("key")
        );
    }

    @Test
    public void testAddTriggersFromJsonString_NestedJSONObject() throws Exception {
        JSONObject jsonObject = new JSONObject() {{
            put("key", new JSONObject() {{
                put("nestedKey", "value");
            }});
        }};

        addTriggersFromJsonString(jsonObject.toString());

        assertEquals(
           new HashMap<String, Object>() {{
              put("nestedKey", "value");
           }},
           OneSignal.getTriggerValueForKey("key")
       );
    }

    public static void addTriggersFromJsonString(String triggersJsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(triggersJsonString);
        OneSignal.addTriggers(OneSignalPackagePrivateHelper.JSONUtils.jsonObjectToMap(jsonObject));
    }

    @Test
    public void testDeleteSavedTriggerValueGetTriggers() {
        OneSignal.addTrigger("test1", "value1");
        assertEquals(OneSignal.getTriggerValueForKey("test1"), "value1");

        OneSignal.removeTriggerForKey("test1");
        assertNull(OneSignal.getTriggers().get("test1"));
    }

    @Test
    public void testDeleteSavedTriggerValue() {
        OneSignal.addTrigger("test1", "value1");
        assertEquals(OneSignal.getTriggerValueForKey("test1"), "value1");

        OneSignal.removeTriggerForKey("test1");
        assertNull(OneSignal.getTriggerValueForKey("test1"));
    }

    @Test
    public void testRemoveTriggersForKeysFromArray_SingleKey() {
        OneSignal.addTrigger("key", "value");

        List<String> triggersToRemove = new ArrayList<>();
        triggersToRemove.add("key");
        OneSignal.removeTriggersForKeys(triggersToRemove);

        assertNull(OneSignal.getTriggerValueForKey("key"));
    }

    @Test
    public void testGreaterThanOperator() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.GREATER_THAN, 1, 2));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.GREATER_THAN, 5, 3));
    }

    @Test
    public void testGreaterThanOperatorWithString() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.GREATER_THAN, 1, "2"));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.GREATER_THAN, 5, "3"));
    }

    @Test
    public void testGreaterThanOrEqualToOperator() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.GREATER_THAN_OR_EQUAL_TO, 2, 2.9));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.GREATER_THAN_OR_EQUAL_TO, 4, 3));
    }

    @Test
    public void testLessThanOperator() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN, 32, 2));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN, 2, 3));
    }

    @Test
    public void testLessThanOperatorWithInvalidStrings() throws JSONException {
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN, 2, ""));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN, 2, "a1"));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN, 2, "a"));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN, 2, "0x01"));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN, 2, null));
    }

    @Test
    public void testLessThanOperatorWithString() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN, 32, "2"));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN, 2, "3"));
    }

    @Test
    public void testLessThanOrEqualToOperator() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN_OR_EQUAL_TO, 5, 4));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.LESS_THAN_OR_EQUAL_TO, 3, 4));
    }

    @Test
    public void testEqualityOperator() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.EQUAL_TO, 0.1, 0.1));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.EQUAL_TO, 0.0, 2));
        // Test mixed Number types (Integer & Double)
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.EQUAL_TO, 1, 1.0));
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.EQUAL_TO, 1.0, 1));
    }

    @Test
    public void testEqualityOperatorWithStrings() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.EQUAL_TO, "a", "a"));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.EQUAL_TO, "a", "b"));
    }

    @Test
    public void testEqualityOperatorWithTriggerStringAndValueNumber() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.EQUAL_TO, "1", 1));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.EQUAL_TO, "2", 1));
    }

    @Test
    public void testNotEqualOperator() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.NOT_EQUAL_TO, 3, 3.01));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.NOT_EQUAL_TO, 3.1, 3.1));
    }

    @Test
    public void testContainsOperator() throws JSONException {
        ArrayList localValue = new ArrayList<String>() {{
            add("test string 1");
        }};

        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.CONTAINS, "test string 1", localValue));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.CONTAINS, "test string 2", localValue));
    }

    @Test
    public void testExistsOperator() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.EXISTS, null, "test"));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.EXISTS, null, null));
    }

    @Test
    public void testNotExistsOperator() throws JSONException {
        assertTrue(comparativeOperatorTest(OSTrigger.OSTriggerOperator.NOT_EXISTS, null, null));
        assertFalse(comparativeOperatorTest(OSTrigger.OSTriggerOperator.NOT_EXISTS, null, "test"));
    }

    @Test
    public void testMessageSchedulesSessionDurationTimer() throws JSONException {
        OneSignalPackagePrivateHelper.OSTestTrigger trigger = InAppMessagingHelpers.buildTrigger(OSTrigger.OSTriggerKind.SESSION_TIME, null, OSTrigger.OSTriggerOperator.EQUAL_TO.toString(), 10);

        InAppMessagingHelpers.resetSessionLaunchTime();

        // this evaluates the message and should schedule a timer for 10 seconds into the session
        assertFalse(InAppMessagingHelpers.dynamicTriggerShouldFire(trigger));
        // verify that the timer was scheduled ~10 seconds
        assertTrue(roughlyEqualTimerValues(10.0, ShadowDynamicTimer.mostRecentTimerDelaySeconds()));
    }

    // This test makes sure that time-based triggers are considered once all non-time-based
    // triggers evaluate to true and will set up a timer if needed
    @Test
    public void testMixedTriggersScheduleTimer() throws JSONException {
        final OneSignalPackagePrivateHelper.OSTestTrigger timeBasedTrigger = InAppMessagingHelpers.buildTrigger(OSTrigger.OSTriggerKind.SESSION_TIME, null, OSTrigger.OSTriggerOperator.GREATER_THAN.toString(), 5.0);
        final OneSignalPackagePrivateHelper.OSTestTrigger normalTrigger = InAppMessagingHelpers.buildTrigger(OSTrigger.OSTriggerKind.CUSTOM, "prop1", OSTrigger.OSTriggerOperator.LESS_THAN_OR_EQUAL_TO.toString(), 3);

        // the time based trigger will be false (but should schedule a timer)
        // while the normal trigger should evaluate to true
        setLocalTriggerValue("prop1", 3);

        ArrayList<ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>> triggers = new ArrayList<ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>>() {{
            add(new ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>() {{
                add(timeBasedTrigger);
                add(normalTrigger);
            }});
        }};

        OneSignalPackagePrivateHelper.OSTestInAppMessageInternal testMessage = InAppMessagingHelpers.buildTestMessageWithMultipleTriggers(triggers);
        assertFalse(InAppMessagingHelpers.evaluateMessage(testMessage));
        assertTrue(ShadowDynamicTimer.hasScheduledTimer);
        assertTrue(roughlyEqualTimerValues(5.0, ShadowDynamicTimer.mostRecentTimerDelaySeconds()));
    }

    @Test
    public void testShouldTriggerWhen1OutOf3OrsAreMeet() throws JSONException {
        setLocalTriggerValue("prop1", 3);

        ArrayList<ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>> triggers = new ArrayList<ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>>() {{
            add(new ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>() {{
                add(InAppMessagingHelpers.buildTrigger(OSTrigger.OSTriggerKind.CUSTOM,"prop1", OSTrigger.OSTriggerOperator.EQUAL_TO.toString(), 1));
            }});
            add(new ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>() {{
                add(InAppMessagingHelpers.buildTrigger(OSTrigger.OSTriggerKind.CUSTOM,"prop1", OSTrigger.OSTriggerOperator.EQUAL_TO.toString(), 2));
            }});
            add(new ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>() {{
                add(InAppMessagingHelpers.buildTrigger(OSTrigger.OSTriggerKind.CUSTOM,"prop1", OSTrigger.OSTriggerOperator.EQUAL_TO.toString(), 3));
            }});
        }};

        OneSignalPackagePrivateHelper.OSTestInAppMessageInternal testMessage = InAppMessagingHelpers.buildTestMessageWithMultipleTriggers(triggers);
        assertTrue(InAppMessagingHelpers.evaluateMessage(testMessage));
    }

    private boolean roughlyEqualTimerValues(double desired, double actual) {
        return Math.abs(desired - actual) < REQUIRED_TIMER_ACCURACY;
    }

    private void OneSignalInit() {
        ShadowOneSignalRestClient.setRemoteParamsGetHtmlResponse();
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.setAppId(InAppMessagingHelpers.ONESIGNAL_APP_ID);
        OneSignal.initWithContext(blankActivity);
        blankActivityController.resume();
    }

    private static @Nullable OSInAppMessageAction lastAction;
    @Test
    public void testOnMessageActionOccurredOnMessage() throws Exception {
        OneSignal.setInAppMessageClickHandler(new OneSignal.OSInAppMessageClickHandler() {
            @Override
            public void inAppMessageClicked(OSInAppMessageAction result) {
                lastAction = result;
                // Ensure we are on the main thread when running the callback, since the app developer
                //   will most likely need to update UI.
                TestHelpers.assertMainThread();
            }
        });
        TestHelpers.threadAndTaskWait();

        OneSignalPackagePrivateHelper.onMessageActionOccurredOnMessage(message,
           new JSONObject() {{
                put("id", "button_id_123");
                put("name", "my_click_name");
            }}
        );

        // Ensure we make REST call to OneSignal to report click.
        ShadowOneSignalRestClient.Request iamClickRequest = ShadowOneSignalRestClient.requests.get(2);
        assertEquals("in_app_messages/" + message.getMessageId() + "/click", iamClickRequest.url);
        assertEquals(InAppMessagingHelpers.ONESIGNAL_APP_ID, iamClickRequest.payload.get("app_id"));
        assertEquals(1, iamClickRequest.payload.get("device_type"));
        assertEquals(message.variants.get("android").get("en"), iamClickRequest.payload.get("variant_id"));
        assertEquals(ShadowOneSignalRestClient.pushUserId, iamClickRequest.payload.get("player_id"));
        assertEquals(true, iamClickRequest.payload.get("first_click"));
        assertEquals("button_id_123", iamClickRequest.payload.get("click_id"));

        // Ensure we fire public callback that In-App was clicked.
        assertEquals(lastAction.getClickName(), "my_click_name");
    }

    @Test
    public void testOnMessageWasShown() throws Exception {
        TestHelpers.threadAndTaskWait();

        OneSignalPackagePrivateHelper.onMessageWasShown(message);

        ShadowOneSignalRestClient.Request iamImpressionRequest = ShadowOneSignalRestClient.requests.get(2);

        assertEquals("in_app_messages/" + message.getMessageId() + "/impression", iamImpressionRequest.url);
        assertEquals(InAppMessagingHelpers.ONESIGNAL_APP_ID, iamImpressionRequest.payload.get("app_id"));
        assertEquals(ShadowOneSignalRestClient.pushUserId, iamImpressionRequest.payload.get("player_id"));
        assertEquals(1, iamImpressionRequest.payload.get("device_type"));
        assertEquals(true, iamImpressionRequest.payload.get("first_impression"));
    }

    @Test
    public void testOnPageChanged() throws Exception {
        TestHelpers.threadAndTaskWait();

        OneSignalPackagePrivateHelper.onPageChanged(message, InAppMessagingHelpers.buildTestPageJson());

        ShadowOneSignalRestClient.Request iamPageImpressionRequest = ShadowOneSignalRestClient.requests.get(2);

        assertEquals("in_app_messages/" + message.getMessageId() + "/pageImpression", iamPageImpressionRequest.url);
        assertEquals(InAppMessagingHelpers.ONESIGNAL_APP_ID, iamPageImpressionRequest.payload.get("app_id"));
        assertEquals(ShadowOneSignalRestClient.pushUserId, iamPageImpressionRequest.payload.get("player_id"));
        assertEquals(1, iamPageImpressionRequest.payload.get("device_type"));
        assertEquals(InAppMessagingHelpers.IAM_PAGE_ID, iamPageImpressionRequest.payload.get("page_id"));
    }

    /* Tests for IAM Lifecycle */

    private static @Nullable
    OSInAppMessage lastMessage;
    private static int iamLifecycleCounter;
    @Test
    public void testIAMLifecycleEventsFlow() throws Exception {
        OneSignal.setInAppMessageLifecycleHandler(new OSInAppMessageLifecycleHandler() {
            @Override
            public void onWillDisplayInAppMessage(OSInAppMessage message) {
                lastMessage = message;
                iamLifecycleCounter++;
            }

            @Override
            public void onDidDisplayInAppMessage(OSInAppMessage message) {
                lastMessage = message;
                iamLifecycleCounter++;
            }

            @Override
            public void onWillDismissInAppMessage(OSInAppMessage message) {
                lastMessage = message;
                iamLifecycleCounter++;
            }

            @Override
            public void onDidDismissInAppMessage(OSInAppMessage message) {
                lastMessage = message;
                iamLifecycleCounter++;
            }
        });
        TestHelpers.threadAndTaskWait();

        assertEquals(0, iamLifecycleCounter);
        // maybe need threadAndTaskWait
        OneSignalPackagePrivateHelper.onMessageWillDisplay(message);
        assertEquals(1, iamLifecycleCounter);

        OneSignalPackagePrivateHelper.onMessageDidDisplay(message);
        assertEquals(2,iamLifecycleCounter);

        OneSignalPackagePrivateHelper.onMessageWillDismiss(message);
        assertEquals(3,iamLifecycleCounter);

        OneSignalPackagePrivateHelper.onMessageDidDismiss(message);
        assertEquals(4,iamLifecycleCounter);

        assertNotNull(lastMessage);
        assertEquals(lastMessage.getMessageId(), message.getMessageId());
    }
}
