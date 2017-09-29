package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.Context;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;


abstract class AccountScreenEvent  extends DonateScreenEvent {


  public interface AllowAcctPermissionAction extends AccountPermApprovedEvent.AllowAcctPermissionAction{};


  private Activity curActivity;
  private   AllowAcctPermissionAction action;

  protected AccountScreenEvent(AlertStatus alertStatus, int titleId, final AllowAcctPermissionAction action) {
    super(alertStatus, titleId, R.string.donate_allow_acct_permission_text);
    setEvents(new AccountPermApprovedEvent(action),
              AccountPermDeniedEvent.instance()
    );
    this.action = action;
  }

  @Override
  public void create(Activity activity, SmsMmsMessage msg) {
    this.curActivity = activity;
    super.create(activity, msg);
  }

  @Override
  public boolean launchActivity(Activity activity) {

    // If user has not approved access to email account info, proceed with normal launch

    if (! ManagePreferences.grantAccountAccess()) return true;

    if (action != null) action.doEvent(activity);
    return false;
  }
}
