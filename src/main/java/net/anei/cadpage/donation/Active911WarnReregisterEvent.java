package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.MsgOptionManager;
import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/**
 Reregister with Active911
 */
public class Active911WarnReregisterEvent extends DonateEvent {

  public Active911WarnReregisterEvent() {
    super(null, R.string.active911_warn_reregister_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    VendorManager.instance().forceActive911Registration(activity);
  }
  
  private static final Active911WarnReregisterEvent instance = new Active911WarnReregisterEvent();
  
  public static Active911WarnReregisterEvent instance() {
    return instance;
  }

}
