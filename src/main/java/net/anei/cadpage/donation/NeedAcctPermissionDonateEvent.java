package net.anei.cadpage.donation;

import net.anei.cadpage.R;

/*
Hey, I've already contributed
 
You have not given Cadpage permission to access your account and phone information.
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
    return !UserAcctManager.instance().isAcctSupport();
  }
  
  private static final NeedAcctPermissionDonateEvent instance = new NeedAcctPermissionDonateEvent();
  public static NeedAcctPermissionDonateEvent instance() {
    return instance;
  }

}
