package top.onesignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import top.oneconnectapi.onesignal.OSLogger;
import top.oneconnectapi.onesignal.OSSessionManager;
import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.influence.data.OSTrackerFactory;
import top.oneconnectapi.onesignal.influence.domain.OSInfluence;

import org.json.JSONObject;

import java.util.List;

public class MockSessionManager extends OSSessionManager {

    public MockSessionManager(@NonNull SessionListener sessionListener, OSTrackerFactory trackerFactory, OSLogger logger) {
        super(sessionListener, trackerFactory, logger);
    }

    @Override
    public void initSessionFromCache() {
        super.initSessionFromCache();
    }

    @Override
    public void addSessionIds(@NonNull JSONObject jsonObject, List<OSInfluence> endingInfluences) {
        super.addSessionIds(jsonObject, endingInfluences);
    }

    @Override
    public void restartSessionIfNeeded(OneSignal.AppEntryAction entryAction) {
        super.restartSessionIfNeeded(entryAction);
    }

    @Override
    public void onInAppMessageReceived(@Nullable String messageId) {
        super.onInAppMessageReceived(messageId);
    }

    @Override
    public void onDirectInfluenceFromIAMClick(@Nullable String messageId) {
        super.onDirectInfluenceFromIAMClick(messageId);
    }

    @Override
    public void onDirectInfluenceFromIAMClickFinished() {
        super.onDirectInfluenceFromIAMClickFinished();
    }

    @Override
    public void onNotificationReceived(@Nullable String messageId) {
        super.onNotificationReceived(messageId);
    }

    public void onDirectInfluenceFromNotificationOpen(@Nullable String notificationId) {
        super.onDirectInfluenceFromNotificationOpen(OneSignal.AppEntryAction.NOTIFICATION_CLICK, notificationId);
    }

    @Override
    public void onDirectInfluenceFromNotificationOpen(OneSignal.AppEntryAction entryAction, @Nullable String notificationId) {
        super.onDirectInfluenceFromNotificationOpen(entryAction, notificationId);
    }

    @Override
    public List<OSInfluence> getInfluences() {
        return super.getInfluences();
    }

    @Override
    public void attemptSessionUpgrade(OneSignal.AppEntryAction entryAction) {
        super.attemptSessionUpgrade(entryAction);
    }
}