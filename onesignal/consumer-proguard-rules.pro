-dontwarn top.oneconnectapi.onesignal.**

# These 2 methods are called with reflection.
-keep class com.google.android.gms.common.api.GoogleApiClient {
    void connect();
    void disconnect();
}

# Need to keep as these 2 methods are called with reflection from top.oneconnectapi.onesignal.PushRegistratorFCM
-keep class com.google.firebase.iid.FirebaseInstanceId {
    static com.google.firebase.iid.FirebaseInstanceId getInstance(com.google.firebase.FirebaseApp);
    java.lang.String getToken(java.lang.String, java.lang.String);
}

-keep class top.oneconnectapi.onesignal.ActivityLifecycleListenerCompat** {*;}

# Observer backcall methods are called with reflection
-keep class top.oneconnectapi.onesignal.OSSubscriptionState {
    void changed(top.oneconnectapi.onesignal.OSPermissionState);
}

-keep class top.oneconnectapi.onesignal.OSPermissionChangedInternalObserver {
    void changed(top.oneconnectapi.onesignal.OSPermissionState);
}

-keep class top.oneconnectapi.onesignal.OSSubscriptionChangedInternalObserver {
    void changed(top.oneconnectapi.onesignal.OSSubscriptionState);
}

-keep class top.oneconnectapi.onesignal.OSEmailSubscriptionChangedInternalObserver {
    void changed(top.oneconnectapi.onesignal.OSEmailSubscriptionState);
}

-keep class top.oneconnectapi.onesignal.OSSMSSubscriptionChangedInternalObserver {
    void changed(top.oneconnectapi.onesignal.OSSMSSubscriptionState);
}

-keep class ** implements top.oneconnectapi.onesignal.OSPermissionObserver {
    void onOSPermissionChanged(top.oneconnectapi.onesignal.OSPermissionStateChanges);
}

-keep class ** implements top.oneconnectapi.onesignal.OSSubscriptionObserver {
    void onOSSubscriptionChanged(top.oneconnectapi.onesignal.OSSubscriptionStateChanges);
}

-keep class ** implements top.oneconnectapi.onesignal.OSEmailSubscriptionObserver {
    void onOSEmailSubscriptionChanged(top.oneconnectapi.onesignal.OSEmailSubscriptionStateChanges);
}

-keep class ** implements top.oneconnectapi.onesignal.OSSMSSubscriptionObserver {
    void onOSEmailSubscriptionChanged(top.oneconnectapi.onesignal.OSSMSSubscriptionStateChanges);
}

-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.AdwHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.ApexHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.AsusHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.DefaultBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.EverythingMeHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.HuaweiHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.LGHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.NewHtcHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.NovaHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.OPPOHomeBader { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.SamsungHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.SonyHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.VivoHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.XiaomiHomeBadger { <init>(...); }
-keep class top.oneconnectapi.onesignal.shortcutbadger.impl.ZukHomeBadger { <init>(...); }


-dontwarn com.amazon.**

-dontwarn com.huawei.**

# Proguard ends up removing this class even if it is used in AndroidManifest.xml so force keeping it.
-keep public class top.oneconnectapi.onesignal.ADMMessageHandler {*;}

-keep public class top.oneconnectapi.onesignal.ADMMessageHandlerJob {*;}

# OSRemoteNotificationReceivedHandler is an interface designed to be extend then referenced in the
#    app's AndroidManifest.xml as a meta-data tag.
# This doesn't count as a hard reference so this entry is required.
-keep class ** implements top.oneconnectapi.onesignal.OneSignal$OSRemoteNotificationReceivedHandler {
   void remoteNotificationReceived(android.content.Context, top.oneconnectapi.onesignal.OSNotificationReceivedEvent);
}

-keep class top.oneconnectapi.onesignal.JobIntentService$* {*;}

-keep class top.oneconnectapi.onesignal.OneSignalUnityProxy {*;}