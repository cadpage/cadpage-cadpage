package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

class AccountPermDeniedEvent extends DonateEvent {

  private Runnable run;

  public AccountPermDeniedEvent() {
    super(null, R.string.donate_email_acct_permission_denied_text);
  }

  @Override
  protected void doEvent(Activity activity) {
    closeEvents(activity);
  }

  private static final AccountPermDeniedEvent instance = new AccountPermDeniedEvent();

  public static AccountPermDeniedEvent instance() {
    return instance;
  }

}
