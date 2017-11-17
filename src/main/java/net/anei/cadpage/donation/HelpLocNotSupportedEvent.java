package net.anei.cadpage.donation;

import net.anei.cadpage.R;

/*
Cadpage is good to go!

Hopefully, Cadpage will start working properly.  If you still have problems, use the
"Email the Developer" setting to contact the developer for assistance.

 */
public class HelpLocNotSupportedEvent extends DonateScreenEvent {

  protected HelpLocNotSupportedEvent() {
    super(R.string.help_loc_not_supported_title, R.string.help_text_dispatch_wintitle, R.string.help_loc_not_supported_text,
          EmailHelpEvent.instance(),
          DoneDonateEvent.instance());
  }

  private static final HelpLocNotSupportedEvent instance = new HelpLocNotSupportedEvent();
  public static HelpLocNotSupportedEvent instance() {
    return instance;
  }

}
