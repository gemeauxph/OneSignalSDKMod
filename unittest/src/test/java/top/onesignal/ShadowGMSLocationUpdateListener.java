package top.onesignal;

import android.location.Location;

import com.huawei.hms.location.HWLocation;
import com.huawei.hms.location.LocationResult;

import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

import top.oneconnectapi.onesignal.GMSLocationController;

@Implements(GMSLocationController.LocationUpdateListener.class)
public class ShadowGMSLocationUpdateListener {

    public static void provideFakeLocation(Location location) {
        GMSLocationController.locationUpdateListener.onLocationChanged(location);
    }

}
