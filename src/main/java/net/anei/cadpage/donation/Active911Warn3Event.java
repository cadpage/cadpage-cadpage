package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/**
 What do I do?

 The first step is to run the Active911 app to make sure that it has performed that switchover.

 Select the Launch Active911 button, then use the back button to return to this screen and
 proceed to the next step.

 */
public class Active911Warn3Event extends DonateScreenEvent {

  public Active911Warn3Event() {
    super(null, R.string.active911_warn_3_title, R.string.active911_warn_3_text,
          Active911WarnReregisterEvent.instance());
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  protected Object[] getTextParms(int type) {
    return new String[]{VendorManager.instance().getActive911Code()};
  }

  private static final Active911Warn3Event instance = new Active911Warn3Event();
  
  public static Active911Warn3Event instance() {
    return instance;
  }
}
