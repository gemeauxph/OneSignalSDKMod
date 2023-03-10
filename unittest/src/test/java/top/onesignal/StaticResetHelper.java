// Clears static properties on OneSignal to simulate an app cold start.

package top.onesignal;

import org.json.JSONArray;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import top.oneconnectapi.onesignal.ActivityLifecycleListener;
import top.oneconnectapi.onesignal.LocationController;
import top.oneconnectapi.onesignal.OSDynamicTriggerController;
import top.oneconnectapi.onesignal.OSInAppMessageController;
import top.oneconnectapi.onesignal.OSInAppMessageControllerFactory;
import top.oneconnectapi.onesignal.OSLogWrapper;
import top.oneconnectapi.onesignal.OSLogger;
import top.oneconnectapi.onesignal.OSNotificationWorkManager;
import top.oneconnectapi.onesignal.OSRemoteParamController;
import top.oneconnectapi.onesignal.OSSessionManager;
import top.oneconnectapi.onesignal.OSTaskController;
import top.oneconnectapi.onesignal.OSTaskRemoteController;
import top.oneconnectapi.onesignal.OSUtils;
import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.OneSignalDbHelper;
import top.oneconnectapi.onesignal.OneSignalStateSynchronizer;
import top.oneconnectapi.onesignal.WebViewManager;

public class StaticResetHelper {

   private static Collection<ClassState> classes = new ArrayList<>();

   public static void load() {
      OSLogger logger = new OSLogWrapper();
      classes.add(new ClassState(OneSignal.class, field -> {
         if (field.getName().equals("unprocessedOpenedNotifis")) {
            field.set(null, new ArrayList<JSONArray>());
            return true;
         } else if (field.getName().equals("remoteParamController")) {
            field.set(null, new OSRemoteParamController());
            return true;
         } else if (field.getName().equals("taskController")) {
            field.set(null, new OSTaskController(logger));
            return true;
         } else if (field.getName().equals("taskRemoteController")) {
            field.set(null, new OSTaskRemoteController(OneSignal.getRemoteParamController(), logger));
            return true;
         } else if (field.getName().equals("inAppMessageControllerFactory")) {
            field.set(null, new OSInAppMessageControllerFactory());
            return true;
         }
         return false;
      }));

      classes.add(new ClassState(OneSignalStateSynchronizer.class, field
              -> {
         if (field.getName().equals("userStatePushSynchronizer") || field.getName().equals("userStateEmailSynchronizer")) {
            field.set(null, null);
            return true;
         }
         return false;
      }));

      classes.add(new ClassState(OneSignalDbHelper.class, null));
      classes.add(new ClassState(LocationController.class, null));
      classes.add(new ClassState(OSInAppMessageController.class, null));
      classes.add(new ClassState(ActivityLifecycleListener.class, null));
      classes.add(new ClassState(OSDynamicTriggerController.class, field -> {
         if (field.getName().equals("sessionLaunchTime")) {
            field.set(null, new Date());
            return true;
         }
         return false;
      }));
      classes.add(new ClassState(OSSessionManager.class, null));
      classes.add(new ClassState(MockSessionManager.class, null));
      classes.add(new ClassState(OSNotificationWorkManager.class, field -> {
         if (field.getName().equals("notificationIds")) {
            field.set(null, OSUtils.newConcurrentSet());
            return true;
         }
         return false;
      }));
   }

   private interface OtherFieldHandler {
      boolean onOtherField(Field field) throws Exception;
   }

   static private class ClassState {
      private OtherFieldHandler otherFieldHandler;
      private Class stateClass;
      private Map<Field, Object> orginalVals = new HashMap<>();

      ClassState(Class inClass, OtherFieldHandler inOtherFieldHandler) {
         stateClass = inClass;
         otherFieldHandler = inOtherFieldHandler;
      }

      private Object tryClone(Object v) throws Exception {
         if (v instanceof Cloneable
                 && !Modifier.isFinal(v.getClass().getModifiers()))
            return v.getClass().getMethod("clone").invoke(v);
         return v;
      }

      private void saveStaticValues() throws Exception {
         Field[] allFields = stateClass.getDeclaredFields();
         for (Field field : allFields) {
            int fieldModifiers = field.getModifiers();
            if (Modifier.isStatic(fieldModifiers)
                && !Modifier.isFinal(fieldModifiers)) {
               field.setAccessible(true);
               Object value = tryClone(field.get(null));
               orginalVals.put(field, value);
            }
         }
      }

      private void restSetStaticFields() throws Exception {
         // appContext is manually set to null first since so many things depend on it.
         OneSignal.appContext = null;
         for (Map.Entry<Field, Object> entry : orginalVals.entrySet()) {
            Field field = entry.getKey();
            field.setAccessible(true);

            Object value = entry.getValue();
            if (otherFieldHandler == null || !otherFieldHandler.onOtherField(field))
               field.set(null, tryClone(value));
         }
      }
   }

   public static void saveStaticValues() throws Exception {
      for (ClassState aClass : classes)
         aClass.saveStaticValues();
   }

   public static void restSetStaticFields() throws Exception {
      for (ClassState aClass : classes)
         aClass.restSetStaticFields();

      clearWebViewManger();
   }

   private static void clearWebViewManger() throws NoSuchFieldException, IllegalAccessException {
      Field field = WebViewManager.class.getDeclaredField("lastInstance");
      field.setAccessible(true);
      field.set(null, null);
   }
}
