package net.anei.cadpage.donation;

import android.content.Context;
import android.os.Build;

import net.anei.cadpage.CadPageApplication;
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
  protected Object[] getTextParms(int type) {
    if (type == PARM_TEXT) {
      Context context = CadPageApplication.getContext();
      String parm = context.getText(R.string.location_tracking_on_base_text).toString();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        int resId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? R.string.location_tracking_on_A11_text : R.string.location_tracking_on_A10_text;
        parm = context.getText(resId).toString() + "\n\n" + parm;
      }
      return new Object[]{parm};
    }
    return null;
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
