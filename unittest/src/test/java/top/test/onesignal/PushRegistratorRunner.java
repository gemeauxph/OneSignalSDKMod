/**
 * Modified MIT License
 *
 * Copyright 2018 OneSignal
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * 1. The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * 2. All copies of substantial portions of the Software may only be used in connection
 * with services provided by OneSignal.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package top.test.onesignal;

import android.app.Activity;

import androidx.annotation.NonNull;

import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.PushRegistratorFCM;
import top.oneconnectapi.onesignal.PushRegistrator;
import top.oneconnectapi.onesignal.ShadowFirebaseApp;
import top.oneconnectapi.onesignal.ShadowGooglePlayServicesUtil;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.example.BlankActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

import static top.test.onesignal.TestHelpers.threadAndTaskWait;

import static junit.framework.Assert.assertTrue;

import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.PushRegistrator;
import top.oneconnectapi.onesignal.ShadowFirebaseApp;
import top.oneconnectapi.onesignal.ShadowGooglePlayServicesUtil;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.ShadowOneSignalRestClient;
import top.oneconnectapi.onesignal.StaticResetHelper;

@Config(packageName = "top.oneconnectapi.onesignal.example",
        shadows = {
            ShadowGooglePlayServicesUtil.class,
            ShadowOSUtils.class,
            ShadowOneSignalRestClient.class,
            ShadowFirebaseApp.class,
        },
        sdk = 28
)
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class PushRegistratorRunner {

   private Activity blankActivity;

   @BeforeClass // Runs only once, before any tests
   public static void setUpClass() throws Exception {
      ShadowLog.stream = System.out;
      TestHelpers.beforeTestSuite();
      StaticResetHelper.saveStaticValues();
   }

   @Before // Before each test
   public void beforeEachTest() throws Exception {
      TestHelpers.beforeTestInitAndCleanup();
      blankActivity = Robolectric.buildActivity(BlankActivity.class).create().get();
   }

   @After
   public void afterEachTest() throws Exception {
      TestHelpers.afterTestCleanup();
   }

   static private class RegisteredHandler implements PushRegistrator.RegisteredHandler {
      private final Thread testThread;
      public boolean callbackFired;

      RegisteredHandler(@NonNull Thread testThread) {
         this.testThread = testThread;
      }

      @Override
      public void complete(String id, int status) {
         callbackFired = true;
         testThread.interrupt();
      }
   }

   private void initOneSignalAndWait() throws Exception {
      OneSignal.initWithContext(blankActivity);
      OneSignal.setAppId("11111111-2222-3333-4444-555555555555");
      TestHelpers.threadAndTaskWait();
   }

   private boolean performRegisterForPush() throws Exception {
      initOneSignalAndWait();

      RegisteredHandler registeredHandler = new RegisteredHandler(Thread.currentThread());

      OneSignalPackagePrivateHelper.PushRegistratorFCM pushReg = new OneSignalPackagePrivateHelper.PushRegistratorFCM(blankActivity, null);
      pushReg.registerForPush(blankActivity, "123456789", registeredHandler);
      try {Thread.sleep(5000);} catch (Throwable t) {}

      return registeredHandler.callbackFired;
   }

   @Test
   public void testGooglePlayServicesAPKMissingOnDevice() throws Exception {
      ShadowOSUtils.isGMSInstalledAndEnabled = false;
      boolean callbackFired = performRegisterForPush();
      assertTrue(callbackFired);
   }

   @Test
   public void testFCMPartOfGooglePlayServicesMissing() throws Exception {
      ShadowOSUtils.isGMSInstalledAndEnabled = true;
      boolean callbackFired = performRegisterForPush();
      assertTrue(callbackFired);
   }
}