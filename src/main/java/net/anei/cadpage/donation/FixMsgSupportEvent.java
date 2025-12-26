package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.SupportApp;

/**
 *  Fix Cadpage settings
 */
public class FixMsgSupportEvent extends DonateEvent {

  private FixMsgSupportEvent() {
    super(null, R.string.donate_fix_msg_support_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    SupportApp.instance().fixMsgSupport();
    closeEvents(activity);
  }

  private static final FixMsgSupportEvent instance = new FixMsgSupportEvent();
  public static FixMsgSupportEvent instance() {
    return instance;
  }

}
