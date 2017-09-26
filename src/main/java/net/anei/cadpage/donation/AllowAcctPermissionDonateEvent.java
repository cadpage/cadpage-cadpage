package net.anei.cadpage.donation;

import android.app.Activity;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/**
We do not know who you are

You have not given Cadpage permission to access your account and phone information.
Without this permission, Cadpage can not confirm if you have purchased a Cadpage
subscription. 

 */
public class AllowAcctPermissionDonateEvent extends AccountScreenEvent {
  
  public AllowAcctPermissionDonateEvent() {
    super(AlertStatus.YELLOW, R.string.donate_allow_acct_permission_title);
  }

  @Override
  void doAccountPermissionApproved(Activity activity) {

    // Don't do anything if we aren't hooked to network
    if (!SmsPopupUtils.haveNet(activity)) return;

    // Request complete status reload
    DonationManager.instance().refreshStatus(activity);

    // Close donation screens
    closeEvents(activity);
  }

  private static final AllowAcctPermissionDonateEvent instance = new AllowAcctPermissionDonateEvent();
  
  public static AllowAcctPermissionDonateEvent instance() {
    return instance;
  }
}
