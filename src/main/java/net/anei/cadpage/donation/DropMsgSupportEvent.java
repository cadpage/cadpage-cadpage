package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import net.anei.cadpage.Log;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/**
 *  I do not require Cadpage message support
 */
public class DropMsgSupportEvent extends DonateEvent {

  protected DropMsgSupportEvent() {
    super(null, R.string.donate_drop_msg_support_title);
  }

  @Override
  public boolean isEnabled() {
    // This is only an option if registered with a direct paging vendor
    return VendorManager.instance().isRegistered();
  }

  @Override
  protected void doEvent(Activity activity) {
    ManagePreferences.setEnableMsgType("C", null);
    closeEvents(activity);
  }

  private static final DropMsgSupportEvent instance = new DropMsgSupportEvent();
  public static DropMsgSupportEvent instance() {
    return instance;
  }

}
