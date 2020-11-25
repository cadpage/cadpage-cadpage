package net.anei.cadpage.donation;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.PermissionManager;
import net.anei.cadpage.vendors.VendorManager;

/**
 Active911 parser alert

 The information in this alert was not provided by Cadpage.
 This alert was processed with a new parsing system that Active911 is setting
 up to replace the Cadpage parsing system.  We are working with Active911 to
 restore Cadpage parsing services.

 */
public class LocationTrackingEvent extends DonateScreenEvent {

  public LocationTrackingEvent() {
    super(null, 0, 0);
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return VendorManager.instance().isActive911Active() &&
           !ManagePreferences.reportPosition().equals("N") &&
           ! (PermissionManager.isGranted(PermissionManager.ACCESS_FINE_LOCATION) &&
              PermissionManager.isGranted(PermissionManager.ACCESS_BACKGROUND_LOCATION));
  }
  
  private static final LocationTrackingEvent instance = new LocationTrackingEvent();
  
  public static LocationTrackingEvent instance() {
    return instance;
  }
}
