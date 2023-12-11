package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import net.anei.cadpage.Log;
import net.anei.cadpage.R;

public class InstallCadpageSupportAppEvent extends DonateEvent {
  private static final Uri DOWNLOAD_URL =       Uri.parse("https://drive.google.com/file/d/1iNRe4sW2_iG0iG4nffMyK5fToooJ_WmH/view?usp=drive_web");

  protected InstallCadpageSupportAppEvent() {
    super(null, R.string.donate_install_cadpage_support_app_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    Intent intent = new Intent(Intent.ACTION_VIEW, DOWNLOAD_URL);
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
