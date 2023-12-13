package net.anei.cadpage.donation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import net.anei.cadpage.CadPageActivity;
import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;
import net.anei.cadpage.vendors.VendorManager;

/**
Reinstall Cadpage
 */
public class ClearDataEvent extends DonateEvent {
  
  private ClearDataEvent() {
    super(AlertStatus.YELLOW, R.string.donate_btn_yes);
  }

  @Override
  protected void doEvent(Activity activity) {

    // They gave us a nifty API to do this in Android 4.4
    ActivityManager fm = (ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE);
    assert fm != null;
    fm.clearApplicationUserData();
  }
  
  private static final ClearDataEvent instance = new ClearDataEvent();
  
  public static ClearDataEvent instance() {
    return instance;
  }

}
