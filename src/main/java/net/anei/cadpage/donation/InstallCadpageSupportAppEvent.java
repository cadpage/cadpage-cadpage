package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import net.anei.cadpage.Log;
import net.anei.cadpage.R;

public class InstallCadpageSupportAppEvent extends DonateEvent {

  protected InstallCadpageSupportAppEvent() {
    super(null, R.string.donate_install_cadpage_support_app_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cadpage.org/download-cadpage"));
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try {
      activity.startActivity(intent);
    } catch (ActivityNotFoundException ex) {
      Log.e(ex);
    }
    closeEvents(activity);
  }

  private static final InstallCadpageSupportAppEvent instance = new InstallCadpageSupportAppEvent();
  public static InstallCadpageSupportAppEvent instance() {
    return instance;
  }

}
