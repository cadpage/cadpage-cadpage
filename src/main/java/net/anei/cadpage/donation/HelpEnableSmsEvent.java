package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;

/*

Text dispatch alerts

You need to tell Cadpage something about where you are so that it knows how to interpret the alerts
you are receiving.  Usually, this is just a matter of selecting the location where your dispatch
center is located
 */
public class HelpEnableSmsEvent extends DonateScreenEvent {

  protected HelpEnableSmsEvent() {
    super(R.string.help_text_dispatch_title, R.string.help_text_dispatch_wintitle, R.string.help_enable_sms_text,
          HelpDoEnableSmsEvent.instance());
  }

  @Override
  public void create(Activity activity, SmsMmsMessage msg) {

    // If SMS message processing is enabled, we want to switch to the regular text processing menu
    // Otherwise process normally
    if (ManagePreferences.enableMsgType().contains("S")) {
      ((DonateActivity)activity).switchEvent(HelpTextDispatchEvent.instance(), msg);
    } else {
      super.create(activity, msg);
    }
  }

  @Override
  public void followup(Activity activity, int req, int result) {
    if (ManagePreferences.isFunctional()) {
      DonateActivity.launchActivity(activity, HelpCadpageReadyEvent.instance(), null);
    }
  }

  private static final HelpEnableSmsEvent instance = new HelpEnableSmsEvent();
  public static HelpEnableSmsEvent instance() {
    return instance;
  }

}
