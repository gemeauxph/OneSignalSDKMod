package top.onesignal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSTestInAppMessageInternal;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSTestTrigger;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.OSTestTrigger.OSTriggerKind;

import top.oneconnectapi.onesignal.OSDynamicTriggerController;
import top.oneconnectapi.onesignal.OSInAppMessageInternal;
import top.oneconnectapi.onesignal.OSTrigger;
import top.oneconnectapi.onesignal.OneSignal;

public class InAppMessagingHelpers {
    public static final String TEST_SPANISH_ANDROID_VARIANT_ID = "d8cc-11e4-bed1-df8f05be55ba-a4b3gj7f";
    public static final String TEST_ENGLISH_ANDROID_VARIANT_ID = "11e4-bed1-df8f05be55ba-a4b3gj7f-d8cc";
    public static final String ONESIGNAL_APP_ID = "b4f7f966-d8cc-11e4-bed1-df8f05be55ba";
    public static final String IAM_CLICK_ID = "12345678-1234-1234-1234-123456789012";
    public static final String IAM_PAGE_ID = "12345678-1234-ABCD-1234-123456789012";
    public static final String IAM_HAS_LIQUID = "has_liquid";

    // unit tests will create an IAM based off JSON of another IAM
    // toJSONObject uses key of "messageId" so we need to replace that with "id" for creating IAM
    public static JSONObject convertIAMtoJSONObject(OSInAppMessageInternal inAppMessage) {
        JSONObject json = inAppMessage.toJSONObject();
        try {
            json.put("id", json.get("messageId"));
            json.remove("messageId");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    public static boolean evaluateMessage(OSInAppMessageInternal message) {
        return OneSignal.getInAppMessageController().triggerController.evaluateMessageTriggers(message);
    }

    public static boolean dynamicTriggerShouldFire(OSTrigger trigger) {
        return OneSignal.getInAppMessageController().triggerController.dynamicTriggerController.dynamicTriggerShouldFire(trigger);
    }

    public static void resetSessionLaunchTime() {
        OSDynamicTriggerController.resetSessionLaunchTime();
    }

    public static void clearTestState() {
        OneSignal.pauseInAppMessages(false);
        OneSignal.getInAppMessageController().getInAppMessageDisplayQueue().clear();
    }

    // Convenience method that wraps an object in a JSON Array
    public static JSONArray wrap(final Object object) {
        return new JSONArray() {{ put(object); }};
    }

    private static JSONArray basicTrigger(final OSTrigger.OSTriggerKind kind, final String key, final String operator, final Object value) throws JSONException {
        JSONObject triggerJson = new JSONObject() {{
            put("id", UUID.randomUUID().toString());
            put("kind", kind.toString());
            put("property", key);
            put("operator", operator);
            put("value", value);
        }};

        return wrap(wrap(triggerJson));
    }

    public static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal buildTestMessageWitRedisplay(final int limit, final long delay) throws JSONException {
        return buildTestMessageWithMultipleDisplays(null, limit, delay);
    }

    // Most tests build a test message using only one trigger.
    // This convenience method makes it easy to build such a message
    public static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal buildTestMessageWithSingleTrigger(final OSTrigger.OSTriggerKind kind, final String key, final String operator, final Object value) throws JSONException {
        JSONArray triggersJson = basicTrigger(kind, key, operator, value);

        return buildTestMessage(triggersJson);
    }

    public static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal buildTestMessageWithSingleTriggerAndRedisplay(final OSTrigger.OSTriggerKind kind, final String key, final String operator,
                                                                                                                         final Object value, int limit, long delay) throws JSONException {
        JSONArray triggersJson = basicTrigger(kind, key, operator, value);

        return buildTestMessageWithMultipleDisplays(triggersJson, limit, delay);
    }

    private static JSONObject basicIAMJSONObject(final JSONArray triggerJson) throws JSONException {
        // builds a test message to test JSON parsing constructor of OSInAppMessage
        JSONObject json = new JSONObject() {{
            put("id", UUID.randomUUID().toString());
            put("variants", new JSONObject() {{
                put("android", new JSONObject() {{
                    put("es", TEST_SPANISH_ANDROID_VARIANT_ID);
                    put("en", TEST_ENGLISH_ANDROID_VARIANT_ID);
                }});
            }});
            put("max_display_time", 30);
            if (triggerJson != null)
                put("triggers", triggerJson);
            else
                put("triggers", new JSONArray());
            put("actions", new JSONArray() {{
                put(buildTestActionJson());
            }});
        }};

        return json;
    }

    public static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal buildTestMessageWithLiquid(final JSONArray triggerJson) throws JSONException {
        JSONObject json = basicIAMJSONObject(triggerJson);
        json.put(IAM_HAS_LIQUID, true);
        return new OneSignalPackagePrivateHelper.OSTestInAppMessageInternal(json);
    }

    public static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal buildTestMessageWithSingleTriggerAndLiquid(final OSTrigger.OSTriggerKind kind, final String key, final String operator, final Object value) throws JSONException {
        JSONArray triggersJson = basicTrigger(kind, key, operator, value);
        return buildTestMessageWithLiquid(triggersJson);
    }

    private static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal buildTestMessageWithMultipleDisplays(final JSONArray triggerJson, final int limit, final long delay) throws JSONException {
        JSONObject json = basicIAMJSONObject(triggerJson);
        json.put("redisplay",  new JSONObject() {{
            put("limit", limit);
            put("delay", delay);//in seconds
        }});

        return new OneSignalPackagePrivateHelper.OSTestInAppMessageInternal(json);
    }

    public static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal buildTestMessage(final JSONArray triggerJson) throws JSONException {
        return new OneSignalPackagePrivateHelper.OSTestInAppMessageInternal(basicIAMJSONObject(triggerJson));
    }

    public static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal buildTestMessageWithEndTime(final OSTrigger.OSTriggerKind kind, final String key, final String operator, final Object value, final boolean pastEndTime) throws JSONException {
        JSONArray triggerJson = basicTrigger(kind, key, operator, value);
        JSONObject json = basicIAMJSONObject(triggerJson);
        if (pastEndTime) {
            json.put("end_time", "1960-01-01T00:00:00.000Z");
        } else {
            json.put("end_time", "2200-01-01T00:00:00.000Z");
        }
        return new OneSignalPackagePrivateHelper.OSTestInAppMessageInternal(json);
    }

    public static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal buildTestMessageWithMultipleTriggers(ArrayList<ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>> triggers) throws JSONException {
        JSONArray ors = buildTriggers(triggers);
        return buildTestMessage(ors);
    }

    public static OneSignalPackagePrivateHelper.OSTestInAppMessageInternal buildTestMessageWithMultipleTriggersAndRedisplay(ArrayList<ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>> triggers, int limit, long delay) throws JSONException {
        JSONArray ors = buildTriggers(triggers);
        return buildTestMessageWithMultipleDisplays(ors, limit, delay);
    }

    private static JSONArray buildTriggers(ArrayList<ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger>> triggers) throws JSONException {
        JSONArray ors = new JSONArray();

        for (ArrayList<OneSignalPackagePrivateHelper.OSTestTrigger> andBlock : triggers) {
            JSONArray ands = new JSONArray();

            for (final OneSignalPackagePrivateHelper.OSTestTrigger trigger : andBlock) {
                ands.put(new JSONObject() {{
                    put("id", UUID.randomUUID().toString());
                    put("kind", trigger.kind.toString());
                    put("property", trigger.property);
                    put("operator", trigger.operatorType.toString());
                    put("value", trigger.value);
                }});
            }

            ors.put(ands);
        }

        return ors;
    }

    public static OneSignalPackagePrivateHelper.OSTestTrigger buildTrigger(final OSTrigger.OSTriggerKind kind, final String key, final String operator, final Object value) throws JSONException {
        JSONObject triggerJson = new JSONObject() {{
            put("id", UUID.randomUUID().toString());
            put("kind", kind.toString());
            put("property", key);
            put("operator", operator);
            put("value", value);
        }};

        return new OneSignalPackagePrivateHelper.OSTestTrigger(triggerJson);
    }

    public static JSONObject buildTestActionJson() throws JSONException {
        return new JSONObject() {{
            put("click_type", "button");
            put("id", IAM_CLICK_ID);
            put("name", "click_name");
            put("url", "https://www.onesignal.com");
            put("url_target", "webview");
            put("close", true);
            put("pageId", IAM_PAGE_ID);
            put("data", new JSONObject() {{
                put("test", "value");
            }});
        }};
    }

    public static JSONObject buildTestPageJson() throws JSONException {
        return new JSONObject() {{
            put("pageIndex", 1);
            put("pageId", IAM_PAGE_ID);
        }};
    }
}
