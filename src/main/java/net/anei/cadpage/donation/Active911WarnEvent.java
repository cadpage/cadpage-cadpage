package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;
import net.anei.cadpage.vendors.VendorManager;

/**
 Active911 parser alert

 The information in this alert was not provided by Cadpage.
 This alert was processed with a new parsing system that Active911 is setting
 up to replace the Cadpage parsing system.  We are working with Active911 to
 restore Cadpage parsing services.

 */
public class Active911WarnEvent extends DonateScreenEvent {

  public Active911WarnEvent() {
    super(null, R.string.active911_warn_title, R.string.active911_warn_text,
          Active911Warn2Event.instance());
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return VendorManager.instance().isWarnActive911();
  }
  
  private static final Active911WarnEvent instance = new Active911WarnEvent();
  
  public static Active911WarnEvent instance() {
    return instance;
  }
}
