package net.anei.cadpage.donation;

import android.app.Activity;
import android.os.Build;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.PermissionManager;
import net.anei.cadpage.R;

/**
 Really enable location tracking permissions
 */

public class LocationTrackingOnEvent extends DonateEvent {

  public LocationTrackingOnEvent() {
    super(null, R.string.location_tracking_on_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    ManagePreferences.checkPermLocationTracking((ok, permissions, granted) -> {
      if (ok) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !PermissionManager.isGranted(activity, PermissionManager.ACCESS_BACKGROUND_LOCATION)) {
          ((DonateActivity) activity).switchEvent(LocationTrackingOn1Event.instance(), null);
        } else {
          closeEvents(activity);
        }
      }
    });
  }

  private static final LocationTrackingOnEvent instance = new LocationTrackingOnEvent();
  
  public static LocationTrackingOnEvent instance() {
    return instance;
  }
}
