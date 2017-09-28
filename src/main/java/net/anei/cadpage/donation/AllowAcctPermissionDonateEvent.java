package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

public class AllowAcctPermissionDonateEvent extends DonateScreenEvent {

  public interface AllowAcctPermisionAction {
    public void doEvent(Activity activity);
  }

  public AllowAcctPermissionDonateEvent(final AllowAcctPermisionAction action) {
    super(AlertStatus.YELLOW, R.string.donate_allow_acct_permission_title, R.string.donate_allow_acct_permission_text);
    setEvents(new AccountPermApprovedEvent(action),
              AccountPermDeniedEvent.instance()
    );
  }
}
