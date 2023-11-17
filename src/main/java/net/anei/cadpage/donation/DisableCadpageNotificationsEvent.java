package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

/**
 Turn off Cadpage notifications
 */
public class DisableCadpageNotificationsEvent extends DonateEvent {

  private DisableCadpageNotificationsEvent() {
    super(null, R.string.disable_notifications_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    ManagePreferences.setNotifyEnabled(false);
    closeEvents(activity);
  }
  
  private static final DisableCadpageNotificationsEvent instance = new DisableCadpageNotificationsEvent();
  
  public static DisableCadpageNotificationsEvent instance() {
    return instance;
  }

}
