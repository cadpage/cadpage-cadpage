package net.anei.cadpage.donation;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import net.anei.cadpage.CadPageActivity;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      ActivityManager fm = (ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE);
      assert fm != null;
      fm.clearApplicationUserData();
    }

    // Have to do this the old fashion way
    else {
      // Delete any private files
      String[] files = activity.fileList();
      for (String file : files) activity.deleteFile(file);

      // Clear any shared preferences
      ManagePreferences.clearAll();
      VendorManager.instance().clearAll();

      // Set up an intent to relaunch Cadpage in 1 second
      AlarmManager myAM = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
      Intent intent = CadPageActivity.getLaunchIntent(activity);
      PendingIntent pendIntent =
              PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_ONE_SHOT);

      long triggerTime = System.currentTimeMillis() + 1000L;
      myAM.set(AlarmManager.RTC_WAKEUP, triggerTime, pendIntent);

      // Kill off this process and let it relaunch
      System.exit(2);
    }
  }
  
  private static final ClearDataEvent instance = new ClearDataEvent();
  
  public static ClearDataEvent instance() {
    return instance;
  }

}
