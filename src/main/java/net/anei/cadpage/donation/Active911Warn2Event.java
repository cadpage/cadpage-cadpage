package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/**
 What do I do?

 The first step is to run the Active911 app to make sure that it has performed that switchover.

 Select the Launch Active911 button, then use the back button to return to this screen and
 proceed to the next step.

 */
public class Active911Warn2Event extends DonateScreenEvent {

  public Active911Warn2Event() {
    super(null, R.string.active911_warn_2_title, R.string.active911_warn_2_text,
          Active911WarnA911Event.instance(),
          Active911Warn3Event.instance());
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
  
  private static final Active911Warn2Event instance = new Active911Warn2Event();
  
  public static Active911Warn2Event instance() {
    return instance;
  }
}
