package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

class HelpCodeMessagingRegisterEvent extends DonateEvent {

  protected HelpCodeMessagingRegisterEvent() {
    super(null, R.string.help_codemessaging_register_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    VendorManager.instance().userRegisterReq(activity, "CodeMessaging");
  }

  private static final HelpCodeMessagingRegisterEvent instance = new HelpCodeMessagingRegisterEvent();
  public static HelpCodeMessagingRegisterEvent instance() {
    return instance;
  }
}
