package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

class HelpDoEnableSmsEvent extends DonateEvent {

  private HelpDoEnableSmsEvent() {
    super(null, R.string.help_do_enable_sms_title);
  }

  @Override
  protected void doEvent(final Activity activity) {

    // Our primarytask is to turn on SMS message processing.  But we have to do this in a way that
    // checks to make sure the proper permissions are enabled and/or the message support app is
    // installed if necessary.  And there we no problems with either check, we have to fake an
    // activity restart which triggers HelpEnableSmsEvent to switch to the next menu in sequence
    ManagePreferences.setEnableMsgType("CS", () -> {
      if (activity instanceof DonateActivity) {
        if (SmsPopupUtils.checkMsgSupport(activity, false) <= 0) {
          ((DonateActivity) activity).fakeRestart();
        }
      }
    });
  }

  private static final HelpDoEnableSmsEvent instance = new HelpDoEnableSmsEvent();
  public static HelpDoEnableSmsEvent instance() {
    return instance;
  }
}
