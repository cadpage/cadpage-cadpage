package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupConfigActivity;

class HelpDoEnableSmsEvent extends DonateEvent {

  protected HelpDoEnableSmsEvent() {
    super(null, R.string.help_do_enable_sms_title);
  }

  @Override
  protected void doEvent(final Activity activity) {
    ManagePreferences.setEnableMsgType("CS", new Runnable(){
      @Override
      public void run() {
        ((DonateActivity)activity).switchEvent(HelpTextDispatchEvent.instance(), null);
      }
    });
  }

  private static final HelpDoEnableSmsEvent instance = new HelpDoEnableSmsEvent();
  public static HelpDoEnableSmsEvent instance() {
    return instance;
  }
}
