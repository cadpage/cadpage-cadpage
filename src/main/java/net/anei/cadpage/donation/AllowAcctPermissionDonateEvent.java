package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

public class AllowAcctPermissionDonateEvent extends AccountScreenEvent {

  public AllowAcctPermissionDonateEvent(final AllowAcctPermissionAction action) {
    super(AlertStatus.YELLOW, R.string.donate_allow_acct_permission_title, action);
  }
}
