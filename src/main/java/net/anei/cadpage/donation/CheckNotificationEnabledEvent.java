package net.anei.cadpage.donation;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ManageNotification;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

/**

 App notifications are not enabled

 Cadpage is configured to generate alert notifications, but system app  notifications are disabled
*/
public class CheckNotificationEnabledEvent extends DonateScreenEvent {

  public CheckNotificationEnabledEvent() {
    super(null, R.string.check_notifications_title, R.string.check_notifications_text,
          EnableNotifyPopupEvent.instance(),
          DisableCadpagePopupEvent.instance());
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
    if (!ManagePreferences.notifyEnabled()) return false;
    NotificationManager nm = (NotificationManager)
        CadPageApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
    return nm.areNotificationsEnabled();
  }

  @Override
  public void onRestart(DonateActivity activity) {
    if (!isEnabled()) closeEvents(activity);
  }

  private static final CheckNotificationEnabledEvent instance = new CheckNotificationEnabledEvent();
  
  public static CheckNotificationEnabledEvent instance() {
    return instance;
  }
}
