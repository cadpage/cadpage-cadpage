package net.anei.cadpage.donation;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

/*
We do not know who you are
 
You have not given Cadpage permission to access your account information.
Without this permission, Cadpage can not confirm if you have purchased a Cadpage
subscription.

 */
public class NeedAcctPermissionDonateEvent extends DonateScreenEvent {

  protected NeedAcctPermissionDonateEvent() {
    super(AlertStatus.YELLOW, R.string.donate_need_acct_permission_title, R.string.donate_need_acct_permission_text,
          AllowAcctPermissionDonateEvent.instance());
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
