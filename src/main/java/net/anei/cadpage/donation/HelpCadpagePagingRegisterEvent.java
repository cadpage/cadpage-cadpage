package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.SupportApp;
import net.anei.cadpage.vendors.VendorManager;

/*
Register with Cadpage paging service
 */

class HelpCadpagePagingRegisterEvent extends DonateEvent {

  protected HelpCadpagePagingRegisterEvent() {
    super(null, R.string.help_cadpage_paging_register_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    VendorManager.instance().CadpageServicePopup(activity);
    SupportApp.instance().fixMsgSupport();
    closeEvents(activity);
  }

  private static final HelpCadpagePagingRegisterEvent instance = new HelpCadpagePagingRegisterEvent();
  public static HelpCadpagePagingRegisterEvent instance() {
    return instance;
  }
}
