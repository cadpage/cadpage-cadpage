package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/*
We do not know who you are
 
You have not given Cadpage permission to access your account information.
Without this permission, Cadpage can not confirm if you have purchased a Cadpage
subscription.

 */
public class NeedAcctPermissionDonateEvent extends AccountScreenEvent {

  protected NeedAcctPermissionDonateEvent() {
    super(AlertStatus.YELLOW, R.string.donate_need_acct_permission_title,
          new AllowAcctPermissionAction() {
              @Override
              public void doEvent(Activity activity) {

                // Don't do anything if we aren't hooked to network
                if (!SmsPopupUtils.haveNet(activity)) return;

                // Request complete status reload
                DonationManager.instance().refreshStatus(activity);
              }
            }
          );
  }

  @Override
  public boolean isEnabled() {
    return !ManagePreferences.grantAccountAccess();
  }

  private static final NeedAcctPermissionDonateEvent instance = new NeedAcctPermissionDonateEvent();
  public static NeedAcctPermissionDonateEvent instance() {
    return instance;
  }

}
