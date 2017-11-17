package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

/*
Cadpage is good to go!

Hopefully, Cadpage will start working properly.  If you still have problems, use the
"Email the Developer" setting to contact the developer for assistance.

 */
public class HelpCadpageReadyEvent extends DonateScreenEvent {

  protected HelpCadpageReadyEvent() {
    super(R.string.help_cadpage_ready_title, R.string.help_cadpage_ready_title, R.string.help_cadpage_ready_text,
          EmailHelpEvent.instance(),
          DoneDonateEvent.instance());
  }

  private static final HelpCadpageReadyEvent instance = new HelpCadpageReadyEvent();
  public static HelpCadpageReadyEvent instance() {
    return instance;
  }

}
