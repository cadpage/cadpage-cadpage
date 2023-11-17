package net.anei.cadpage.donation;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

/**
 Full screen notifications are not enabled

 Full screen notifications must be enabled to support Cadpage popup alerts.

 */
public class CheckPopupAuthorizedEvent extends DonateScreenEvent {

  public CheckPopupAuthorizedEvent() {
    super(null, R.string.check_popup_authorized_title, R.string.check_popup_authorized_text,
          EnablePopupAuthorizationEvent.instance(),
          DisableCadpagePopupEvent.instance());
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false;
    if (!ManagePreferences.popupEnabled()) return false;
    NotificationManager nm = (NotificationManager)
        CadPageApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
    return !nm.canUseFullScreenIntent();
  }

  @Override
  public void onRestart(DonateActivity activity) {
    if (!isEnabled()) closeEvents(activity);
  }

  private static final CheckPopupAuthorizedEvent instance = new CheckPopupAuthorizedEvent();
  
  public static CheckPopupAuthorizedEvent instance() {
    return instance;
  }
}
