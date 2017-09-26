package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.Context;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;


abstract class AccountScreenEvent  extends DonateScreenEvent {


  private Activity curActivity;

  protected AccountScreenEvent(AlertStatus alertStatus, int titleId) {
    super(alertStatus, titleId, R.string.donate_email_acct_permission_text);
    setEvents(new AccountPermApprovedEvent(new Runnable(){
                @Override
                public void run() {
                  doAccountPermissionApproved(curActivity);
                  closeEvents(curActivity);
                }
              }),
              AccountPermDeniedEvent.instance()
    );
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

    // If access has already been approved, perform the requested function.
    doAccountPermissionApproved(activity);
    return false;
  }

  /**
   * Action to be performed in access to account information is granted
   * @param activity current activity
   */
  abstract void doAccountPermissionApproved(Activity activity);
}
