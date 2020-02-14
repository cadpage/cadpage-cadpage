package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsMessageQueue;
import net.anei.cadpage.SmsPopupUtils;
import net.anei.cadpage.vendors.VendorManager;

/**
 *  I do not require Cadpage message support
 */
public class DropMsgSupportEvent extends DonateEvent {

  private DropMsgSupportEvent() {
    super(null, R.string.donate_drop_msg_support_title);
  }

  @Override
  public boolean isEnabled() {

    // This is only an option if registered with a direct paging vendor
    // or if there are zero text alerts in the message queue.  The later
    // case will most likely happen when Cadpage is initially started with
    // the intention of registering with Cadpage
    return VendorManager.instance().isRegistered() ||
           !SmsMessageQueue.getInstance().containsTextAlerts();
  }

  @Override
  protected void doEvent(Activity activity) {
    SmsPopupUtils.fixMsgSupport(activity);
    closeEvents(activity);
  }

  private static final DropMsgSupportEvent instance = new DropMsgSupportEvent();
  public static DropMsgSupportEvent instance() {
    return instance;
  }

}
