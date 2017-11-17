package net.anei.cadpage.donation;

import net.anei.cadpage.R;

/*
Cadpage is good to go!

Hopefully, Cadpage will start working properly.  If you still have problems, use the
"Email the Developer" setting to contact the developer for assistance.

 */
public class HelpNotWorkingEvent extends DonateScreenEvent {

  protected HelpNotWorkingEvent() {
    super(R.string.help_not_working_title, R.string.help_text_dispatch_wintitle, R.string.help_loc_not_supported_text,
          EmailHelpEvent.instance(),
          DoneDonateEvent.instance());
  }

  private static final HelpNotWorkingEvent instance = new HelpNotWorkingEvent();
  public static HelpNotWorkingEvent instance() {
    return instance;
  }

}
