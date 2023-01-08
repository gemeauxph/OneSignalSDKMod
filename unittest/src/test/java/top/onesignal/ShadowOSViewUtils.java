package top.onesignal;

import android.app.Activity;
import androidx.annotation.NonNull;

import org.robolectric.annotation.Implements;

import top.oneconnectapi.onesignal.OSViewUtils;

@Implements(OSViewUtils.class)
public class ShadowOSViewUtils {

   public static boolean isActivityFullyReady(@NonNull Activity activity) {
      return true;
   }

}
