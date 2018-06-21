package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.Intent;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;

/*

Text dispatch alerts

You need to tell Cadpage something about where you are so that it knows how to interpret the alerts
you are receiving.  Usually, this is just a matter of selecting the location where your dispatch
center is located
 */
public class HelpTextDispatchEvent extends DonateScreenEvent {

  private HelpTextDispatchEvent() {
    super(R.string.help_text_dispatch_title, R.string.help_text_dispatch_wintitle, R.string.help_text_dispatch_text,
          HelpSelectLocationEvent.instance(),
          HelpLocNotSupportedEvent.instance(),
          HelpNotWorkingEvent.instance());
  }

  @Override
  public void create(Activity activity, SmsMmsMessage msg) {

    // If Cadpage is functional, switch to the Cadpage ready menu.  This can only happen
    // if the Enable SMS processing screen came up first and switched to us after enabling
    // SMS processing
    // Otherwise process normally
    if (ManagePreferences.isFunctional()) {
      ((DonateActivity)activity).switchEvent(HelpCadpageReadyEvent.instance(), msg);
    } else {
      super.create(activity, msg);
    }
  }

  @Override
  public boolean followup(Activity activity, int req, int result, Intent data) {
    if (ManagePreferences.isFunctional()) {
      ((DonateActivity)activity).switchEvent(HelpCadpageReadyEvent.instance(), null);
    }
    return true;
  }

  private static final HelpTextDispatchEvent instance = new HelpTextDispatchEvent();
  public static HelpTextDispatchEvent instance() {
    return instance;
  }

}
