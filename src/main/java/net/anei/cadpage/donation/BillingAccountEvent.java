package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;


public class BillingAccountEvent extends DonateScreenEvent {

  private BillingAccountEvent() {
    super(null, R.string.donate_billing_account_title, R.string.donate_billing_account_text);
    setEvents(new AccountPermApprovedEvent(null),
              AccountPermDeniedEvent.instance()
    );
  }

  @Override
  protected Object[] getTextParms(Activity activity, int type) {
    if (type == PARM_TEXT) {
      String acct = ManagePreferences.billingAccount();
      if (acct == null) acct = activity.getText(R.string.billing_acct_not_specified).toString();
      return new Object[]{acct};
    }
    return null;
  }

  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  private static final BillingAccountEvent instance = new BillingAccountEvent();
  public static BillingAccountEvent instance() {
    return instance;
  }
}
