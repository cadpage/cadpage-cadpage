package net.anei.cadpage.donation;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import net.anei.cadpage.CadPageActivity;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/**
 Disable Cadpage show alarm popup option
 */
public class DisableCadpagePopupEvent extends DonateEvent {

  private DisableCadpagePopupEvent() {
    super(null, R.string.disable_cadpage_popup_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    ManagePreferences.setPopupEnabled(false);
    closeEvents(activity);
  }
  
  private static final DisableCadpagePopupEvent instance = new DisableCadpagePopupEvent();
  
  public static DisableCadpagePopupEvent instance() {
    return instance;
  }

}
