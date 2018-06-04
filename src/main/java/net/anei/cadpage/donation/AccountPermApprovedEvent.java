package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

class AccountPermApprovedEvent extends DonateEvent {


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
    ManagePreferences.setGrantAccountAccess(MainDonateEvent.instance().getGrantAccountPref(), true,
      new Runnable(){
        @Override
        public void run() {
          if (action != null) action.doEvent(activity);
          closeEvents(activity);
        }
      });
  }
}
