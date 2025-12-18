package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;
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
    SmsPopupUtils.fixMsgSupport(activity);
    closeEvents(activity);
  }

  private static final HelpCadpagePagingRegisterEvent instance = new HelpCadpagePagingRegisterEvent();
  public static HelpCadpagePagingRegisterEvent instance() {
    return instance;
  }
}
