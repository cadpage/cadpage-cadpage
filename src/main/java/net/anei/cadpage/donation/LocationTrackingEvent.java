package net.anei.cadpage.donation;

import android.content.Context;
import android.os.Build;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.PermissionManager;
import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/**
 Location Tracking Alert

 When you respond to an Active911 alert, Cadpage can report your current real time location so
 it can be displayed on the Active911 map display.  Enabling this feature will require changing
 you location tracking permissions.
 */
public class LocationTrackingEvent extends DonateScreenEvent {

  public LocationTrackingEvent() {
    super(null, R.string.location_tracking_title, R.string.location_tracking_text,
          LocationTrackingOnEvent.instance(),
          LocationTrackingOffEvent.instance());
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    Context context = CadPageApplication.getContext();
    return VendorManager.instance().isActive911Active() &&
           !ManagePreferences.reportPosition().equals("N") &&
           ! (PermissionManager.isGranted(context, PermissionManager.ACCESS_FINE_LOCATION) &&
              (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || PermissionManager.isGranted(context, PermissionManager.ACCESS_BACKGROUND_LOCATION)));
  }

  private static final LocationTrackingEvent instance = new LocationTrackingEvent();
  
  public static LocationTrackingEvent instance() {
    return instance;
  }
}
