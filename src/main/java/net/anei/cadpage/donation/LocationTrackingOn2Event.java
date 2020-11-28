package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.PermissionManager;
import net.anei.cadpage.R;

/**
 Really enable location tracking permissions
 */

public class LocationTrackingOn2Event extends DonateEvent {

  public LocationTrackingOn2Event() {
    super(null, R.string.location_tracking_on2_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    ManagePreferences.getPermissionManager().
            request(9999,
                    new String[]{
                            PermissionManager.ACCESS_FINE_LOCATION,
                            PermissionManager.ACCESS_BACKGROUND_LOCATION
                    });
  }

  private static final LocationTrackingOn2Event instance = new LocationTrackingOn2Event();
  
  public static LocationTrackingOn2Event instance() {
    return instance;
  }
}
