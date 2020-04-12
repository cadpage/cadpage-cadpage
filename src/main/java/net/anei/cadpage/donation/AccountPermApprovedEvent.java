package net.anei.cadpage.donation;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.common.AccountPicker;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

public class AccountPermApprovedEvent extends DonateEvent {


  public static final int BILLING_ACCT_REQ = 99991;

  public interface AllowAcctPermissionAction {
    public void doEvent(Activity activity);
  }

  private final AllowAcctPermissionAction action;

  public AccountPermApprovedEvent(AllowAcctPermissionAction  action) {
    super(null, R.string.donate_email_acct_permission_granted_text);
    this.action = action;
  }

  @Override
  protected void doEvent(final Activity activity) {
    Intent intent =
        AccountPicker.newChooseAccountIntent(
            null,
            null,
            new String[]{"com.google"},
            true,
            null,
            null,
            null,
            null);
    activity.startActivityForResult(intent, BILLING_ACCT_REQ);
  }

  @Override
  public boolean followup(Activity activity, int req, int result, Intent intent) {
    if (req == BILLING_ACCT_REQ) {
      if (result == Activity.RESULT_OK) {
        ManagePreferences.setBillingAccount(intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
        if (action != null) action.doEvent(activity);
      }
      closeEvents(activity);
    }
    return false;
  }
}
