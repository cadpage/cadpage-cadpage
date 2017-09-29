package net.anei.cadpage.donation;

import android.app.Activity;
import android.preference.PreferenceManager;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.PermissionManager;
import net.anei.cadpage.Permissions;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;
import net.anei.cadpage.SmsPopupUtils;

/*
We may need to access your email account information

You have upgraded to a version of Cadpage that treats your email accounts as private user
information, which we will not access without your explicit permission.  And you have a
payment status which we may not be able to confirm without that email account information.\n\n

You are strongly advised to allow Cadpage to access your account information now.  If you do
not, we may loose track of your payment subscription status somewhere down the road.
 */
public class NeedAcctPermissionUpgradeEvent extends DonateScreenEvent {

  protected NeedAcctPermissionUpgradeEvent() {
    super(AlertStatus.RED, R.string.donate_need_acct_permission_upgrade_title, R.string.donate_need_acct_permission_upgrade_text,
            new AllowAcctPermissionDonateEvent(null));
  }

  @Override
  public boolean isEnabled() {

    // Only enabled if user recently upgraded to account security upgrade
    if (!ManagePreferences.isAccountSecurityUpgrade()) return false;

    // And user has not somehow already enabled account access
    if (ManagePreferences.grantAccountAccess()) return false;

    // And Cadpage has been granted account access permission
    if (!ManagePreferences.getPermissionManager().isGranted(PermissionManager.GET_ACCOUNTS)) return false;

    // And their payment status indicates they require authorization from  our authorization server
    return DonationManager.instance().reqAuthServer();
  }

  @Override
  public void create(Activity activity, SmsMmsMessage msg) {
    super.create(activity, msg);
    ManagePreferences.clearAccountSecurityUpgrade();
  }

  private static final NeedAcctPermissionUpgradeEvent instance = new NeedAcctPermissionUpgradeEvent();
  public static NeedAcctPermissionUpgradeEvent instance() {
    return instance;
  }

}
