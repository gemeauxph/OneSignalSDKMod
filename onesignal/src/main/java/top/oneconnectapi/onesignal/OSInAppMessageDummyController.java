package top.oneconnectapi.onesignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import top.oneconnectapi.onesignal.language.LanguageContext;
import top.oneconnectapi.onesignal.language.LanguageContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;

import top.oneconnectapi.onesignal.language.LanguageContext;

class OSInAppMessageDummyController extends OSInAppMessageController {

    /**
     * In App Messaging is not supported for Android 4.3 and older devices
     * This is a dummy controller that will be used for Android 4.3 and older devices
     * All methods should be overridden and as empty as possible (few return exceptions)
     */
    OSInAppMessageDummyController(OneSignalDbHelper dbHelper, OSTaskController taskController, OSLogger logger,
                                  OSSharedPreferences sharedPreferences, LanguageContext languageContext) {
        super(dbHelper, taskController, logger, sharedPreferences, languageContext);
    }

    @Override
    public void initRedisplayData() {
    }

    @Override
    void initWithCachedInAppMessages() { }

    @Override
    void receivedInAppMessageJson(@NonNull JSONArray json) throws JSONException { }

    @Override
    void onMessageActionOccurredOnMessage(@NonNull OSInAppMessageInternal message, @NonNull JSONObject actionJson) { }

    @Override
    void onMessageActionOccurredOnPreview(@NonNull OSInAppMessageInternal message, @NonNull JSONObject actionJson) { }

    @Override
    boolean isInAppMessageShowing() { return false; }

    @Nullable
    @Override
    OSInAppMessageInternal getCurrentDisplayedInAppMessage() { return null; }

    @Override
    public void messageWasDismissed(@NonNull OSInAppMessageInternal message) { }

    @Override
    void displayPreviewMessage(@NonNull String previewUUID) { }

    @Override
    public void messageTriggerConditionChanged() { }

    @Override
    void addTriggers(Map<String, Object> newTriggers) { }

    @Override
    void removeTriggersForKeys(Collection<String> keys) { }

    @Override
    void setInAppMessagingEnabled(boolean enabled) { }

    @Override
    void cleanCachedInAppMessages() {
    }

    @Nullable
    @Override
    Object getTriggerValue(String key) { return null; }
}

