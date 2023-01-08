package top.test.onesignal;

import androidx.test.core.app.ApplicationProvider;

import top.oneconnectapi.onesignal.OneSignal;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.StaticResetHelper;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.UserState.DEVICE_TYPE_ANDROID;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.UserState.DEVICE_TYPE_FIREOS;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.UserState.DEVICE_TYPE_HUAWEI;
import static top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper.getDeviceType;
import static org.junit.Assert.assertEquals;

import top.oneconnectapi.onesignal.OneSignalPackagePrivateHelper;
import top.oneconnectapi.onesignal.ShadowOSUtils;
import top.oneconnectapi.onesignal.StaticResetHelper;
import top.oneconnectapi.onesignal.UserState;

@Config(
    packageName = "top.oneconnectapi.onesignal.example",
    shadows = {
        ShadowOSUtils.class
    },
    sdk = 26
)
@RunWith(RobolectricTestRunner.class)
public class DeviceTypeTestsRunner {

    @BeforeClass // Runs only once, before any tests
    public static void setUpClass() throws Exception {
        ShadowLog.stream = System.out;
        TestHelpers.beforeTestSuite();
        StaticResetHelper.saveStaticValues();
    }

    @Before
    public void beforeEachTest() throws Exception {
        TestHelpers.beforeTestInitAndCleanup();
        OneSignal.initWithContext(ApplicationProvider.getApplicationContext());
    }

    @AfterClass
    public static void afterEverything() throws Exception {
        TestHelpers.beforeTestInitAndCleanup();
    }

    @After
    public void afterEachTest() throws Exception {
        TestHelpers.afterTestCleanup();
    }

    @Test
    public void noAvailablePushChannels_defaultsToAndroid() {
        Assert.assertEquals(UserState.DEVICE_TYPE_ANDROID, OneSignalPackagePrivateHelper.getDeviceType());
    }

    @Test
    public void onlyADM_isFireOS() {
        ShadowOSUtils.supportsADM = true;
        Assert.assertEquals(UserState.DEVICE_TYPE_FIREOS, OneSignalPackagePrivateHelper.getDeviceType());
    }

    @Test
    public void onlyFCM_isAndroid() {
        ShadowOSUtils.hasFCMLibrary = true;
        Assert.assertEquals(UserState.DEVICE_TYPE_ANDROID, OneSignalPackagePrivateHelper.getDeviceType());
    }

    @Test
    public void FCMAndGMSEnabled_isAndroid() {
        ShadowOSUtils.isGMSInstalledAndEnabled = true;
        ShadowOSUtils.hasFCMLibrary = true;
        Assert.assertEquals(UserState.DEVICE_TYPE_ANDROID, OneSignalPackagePrivateHelper.getDeviceType());
    }

    @Test
    public void supportedHMS_isHuawei() {
        ShadowOSUtils.isHMSCoreInstalledAndEnabled = true;
        ShadowOSUtils.hasAllRecommendedHMSLibraries(true);

        Assert.assertEquals(UserState.DEVICE_TYPE_HUAWEI, OneSignalPackagePrivateHelper.getDeviceType());
    }

    @Test
    public void supportsFCMAndHMS_PreferAndroid() {
        ShadowOSUtils.isGMSInstalledAndEnabled = true;
        ShadowOSUtils.hasFCMLibrary = true;

        ShadowOSUtils.isHMSCoreInstalledAndEnabled = true;
        ShadowOSUtils.hasAllRecommendedHMSLibraries(true);

        // Prefer Google Services over Huawei if both available
        Assert.assertEquals(UserState.DEVICE_TYPE_ANDROID, OneSignalPackagePrivateHelper.getDeviceType());
    }

    @Test
    public void hasFCMButNoGMSOnDeviceAndHasHMS_isHuawei() {
        ShadowOSUtils.isGMSInstalledAndEnabled = false;
        ShadowOSUtils.hasFCMLibrary = true;

        ShadowOSUtils.isHMSCoreInstalledAndEnabled = true;
        ShadowOSUtils.hasAllRecommendedHMSLibraries(true);

        // Use HMS since device does not have the "Google Play services" app or it is disabled
        Assert.assertEquals(UserState.DEVICE_TYPE_HUAWEI, OneSignalPackagePrivateHelper.getDeviceType());
    }

    @Test
    public void noPushSDKsAndOnlyHMSCoreInstalled_isHuawei() {
        ShadowOSUtils.isHMSCoreInstalledAndEnabled = true;
        Assert.assertEquals(UserState.DEVICE_TYPE_HUAWEI, OneSignalPackagePrivateHelper.getDeviceType());
    }

    @Test
    public void noPushSDKsAndOnlyGoogleServicesInstalled_isAndroid() {
        ShadowOSUtils.isGMSInstalledAndEnabled = true;
        Assert.assertEquals(UserState.DEVICE_TYPE_ANDROID, OneSignalPackagePrivateHelper.getDeviceType());
    }

    @Test
    public void supportsFCMAndADM_PreferADM() {
        ShadowOSUtils.isGMSInstalledAndEnabled = true;
        ShadowOSUtils.hasFCMLibrary = true;

        ShadowOSUtils.supportsADM = true;

        // Prefer ADM as if available it will always be native to the device
        Assert.assertEquals(UserState.DEVICE_TYPE_FIREOS, OneSignalPackagePrivateHelper.getDeviceType());
    }
}
