package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/**
 Reregister with Active911
 */
public class Active911WarnAllDoneEvent extends DonateEvent {

  public Active911WarnAllDoneEvent() {
    super(null, R.string.active911_warn_all_done_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    closeEvents(activity);
  }
  
  private static final Active911WarnAllDoneEvent instance = new Active911WarnAllDoneEvent();
  
  public static Active911WarnAllDoneEvent instance() {
    return instance;
  }

}
