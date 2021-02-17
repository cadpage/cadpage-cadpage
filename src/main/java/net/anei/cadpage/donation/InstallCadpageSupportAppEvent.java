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

    // We do not close the event tree here because the user might not go through with the install.
    // Instead all callers recheck the support app status when they are restarted, and close the
    // event tree if all is well.
  }

  private static final InstallCadpageSupportAppEvent instance = new InstallCadpageSupportAppEvent();
  public static InstallCadpageSupportAppEvent instance() {
    return instance;
  }

}
