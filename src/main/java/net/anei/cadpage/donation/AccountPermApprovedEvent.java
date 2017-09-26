package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

class AccountPermApprovedEvent extends DonateEvent {

  private Runnable action;

  public AccountPermApprovedEvent(Runnable action) {
    super(null, R.string.donate_email_acct_permission_granted_text);
    this.action = action;
  }

  @Override
  protected void doEvent(Activity activity) {
    ManagePreferences.setGrantAccountAccess(MainDonateEvent.instance().getGrantAccountPref(), true, action);
  }
}
