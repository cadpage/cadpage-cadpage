package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.Context;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.PermissionManager;
import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/**
 Turn off location tracking
 */

public class LocationTrackingOffEvent extends DonateEvent {

  public LocationTrackingOffEvent() {
    super(null, R.string.location_tracking_off_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    ManagePreferences.setReportPosition("N");
    closeEvents(activity);
  }

  private static final LocationTrackingOffEvent instance = new LocationTrackingOffEvent();
  
  public static LocationTrackingOffEvent instance() {
    return instance;
  }
}
