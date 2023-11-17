package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;

import net.anei.cadpage.R;

/*
  Enable Cadpage app notifications
 */

public class EnableNotificationsEvent extends DonateEvent {

  EnableNotificationsEvent() {
    super(null, R.string.enable_notifications_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
      intent.putExtra(Settings.EXTRA_APP_PACKAGE,"net.anei.cadpage");
      activity.startActivity(intent);
    }
  }

  private static final EnableNotificationsEvent instance = new EnableNotificationsEvent();

  public static EnableNotificationsEvent instance() {
    return instance;
  }
}
