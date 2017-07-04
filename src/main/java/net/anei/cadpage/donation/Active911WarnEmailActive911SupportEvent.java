package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.EmailDeveloperActivity;
import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/**
 Reregister with Active911
 */
public class Active911WarnEmailActive911SupportEvent extends DonateEvent {

  public Active911WarnEmailActive911SupportEvent() {
    super(null, R.string.active911_warn_email_active911_support_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    EmailDeveloperActivity.sendActive911SupportEmail(activity);
    activity.setResult(Activity.RESULT_OK);
    activity.finish();
  }
  
  private static final Active911WarnEmailActive911SupportEvent instance = new Active911WarnEmailActive911SupportEvent();
  
  public static Active911WarnEmailActive911SupportEvent instance() {
    return instance;
  }

}
