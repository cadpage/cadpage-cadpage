package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import net.anei.cadpage.R;

/*
  Enable full screen notifications
 */

public class EnablePopupAuthorizationEvent extends DonateEvent {

  EnablePopupAuthorizationEvent() {
    super(null, R.string.enable_popup_authorization_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:net.anei.cadpage"));
      intent.putExtra(Settings.EXTRA_APP_PACKAGE,"net.anei.cadpage");
      activity.startActivity(intent);
    }
  }

  private static final EnablePopupAuthorizationEvent instance = new EnablePopupAuthorizationEvent();

  public static EnablePopupAuthorizationEvent instance() {
    return instance;
  }
}
