package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsMessageQueue;
import net.anei.cadpage.SmsPopupUtils;
import net.anei.cadpage.vendors.VendorManager;

/**
 *  Fix Cadpage settings
 */
public class FixMsgSupportEvent extends DonateEvent {

  private FixMsgSupportEvent() {
    super(null, R.string.donate_fix_msg_support_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    SmsPopupUtils.fixMsgSupport(activity);
    closeEvents(activity);
  }

  private static final FixMsgSupportEvent instance = new FixMsgSupportEvent();
  public static FixMsgSupportEvent instance() {
    return instance;
  }

}
