package net.anei.cadpage.donation;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

/*
    Active911 alerts

    You need to register Cadpage with the Active911 service.  To do this, you will need your Active911 device
    activation code which you can get from your department.

 */
public class HelpActive911Event extends DonateScreenEvent {

  protected HelpActive911Event() {
    super(R.string.help_active911_title, R.string.help_active911_title, R.string.help_active911_text,
          HelpActive911RegisterEvent.instance(),
          DoneDonateEvent.instance());
  }

  @Override
  public void onRestart(DonateActivity activity) {

    // If Cadpage is now functional, switch to the Cadpage Ready menu node
    if (ManagePreferences.isFunctional()) activity.switchEvent(HelpCadpageReadyEvent.instance(), null);
  }

  private static final HelpActive911Event instance = new HelpActive911Event();
  public static HelpActive911Event instance() {
    return instance;
  }

}
