package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

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
