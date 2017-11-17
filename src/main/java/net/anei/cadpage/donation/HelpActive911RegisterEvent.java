package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupConfigActivity;
import net.anei.cadpage.vendors.VendorManager;

class HelpActive911RegisterEvent extends DonateEvent {

  protected HelpActive911RegisterEvent() {
    super(null, R.string.help_active911_register_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    VendorManager.instance().userRegisterReq(activity, "Active911");
    closeEvents(activity);
  }

  private static final HelpActive911RegisterEvent instance = new HelpActive911RegisterEvent();
  public static HelpActive911RegisterEvent instance() {
    return instance;
  }
}
