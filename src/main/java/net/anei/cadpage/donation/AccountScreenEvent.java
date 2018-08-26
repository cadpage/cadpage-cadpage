package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;


abstract class AccountScreenEvent  extends DonateScreenEvent {


  public interface AllowAcctPermissionAction extends AccountPermApprovedEvent.AllowAcctPermissionAction{}


  private AllowAcctPermissionAction action;

  protected AccountScreenEvent(AlertStatus alertStatus, int titleId) {
    this(alertStatus, titleId, null);
  }

  protected AccountScreenEvent(AlertStatus alertStatus, int titleId, final AllowAcctPermissionAction action) {
    super(alertStatus, titleId, R.string.donate_allow_acct_permission_text);
    setEvents(new AccountPermApprovedEvent(action),
              AccountPermDeniedEvent.instance()
    );
    this.action = action;
  }

  protected void setAction(AllowAcctPermissionAction action) {
    this.action = action;
  }

  @Override
  public boolean launchActivity(Activity activity) {

    // If user has not approved access to email account info, proceed with normal launch

    if (ManagePreferences.billingAccount() == null) return true;

    if (action != null) action.doEvent(activity);
    return false;
  }
}
