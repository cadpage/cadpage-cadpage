package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupConfigActivity;

class HelpSelectLocationEvent extends DonateEvent {

  protected HelpSelectLocationEvent() {
    super(null, R.string.help_select_location_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    SmsPopupConfigActivity.selectLocation(activity);
  }

  private static final HelpSelectLocationEvent instance = new HelpSelectLocationEvent();
  public static HelpSelectLocationEvent instance() {
    return instance;
  }
}
