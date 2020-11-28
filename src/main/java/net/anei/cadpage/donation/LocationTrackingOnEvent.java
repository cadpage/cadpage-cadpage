package net.anei.cadpage.donation;

import net.anei.cadpage.R;

/**
 Enable location tracking permissions

 For this to work properly, you must allow location access all of the time.\n
 \n
 Cadpage will collect location data to support this feature even when Cadpage is closed or not
 visible because the map/navigation app is running.  Location data will never be collected for
 longer than 20 min after the alert is received.
 */
public class LocationTrackingOnEvent extends DonateScreenEvent {

  public LocationTrackingOnEvent() {
    super(null, R.string.location_tracking_on_title, R.string.location_tracking_on_text,
          LocationTrackingOn2Event.instance(),
          LocationTrackingOffEvent.instance());
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public void onRestart(DonateActivity activity) {
    if (!LocationTrackingEvent.instance().isEnabled()) closeEvents(activity);
  }

  private static final LocationTrackingOnEvent instance = new LocationTrackingOnEvent();
  
  public static LocationTrackingOnEvent instance() {
    return instance;
  }
}
