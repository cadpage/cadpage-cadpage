package net.anei.cadpage.donation;

import net.anei.cadpage.CallHistoryActivity;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

/*

Text dispatch alerts

You need to tell Cadpage something about where you are so that it knows how to interpret the alerts
you are receiving.  Usually, this is just a matter of selecting the location where your dispatch
center is located
 */
public class HelpTextDispatchEvent extends DonateScreenEvent {

  protected HelpTextDispatchEvent() {
    super(R.string.help_text_dispatch_title, R.string.help_text_dispatch_wintitle, R.string.help_text_dispatch_text,
          HelpSelectLocationEvent.instance());
  }

  private static final HelpTextDispatchEvent instance = new HelpTextDispatchEvent();
  public static HelpTextDispatchEvent instance() {
    return instance;
  }

}
