package net.anei.cadpage.donation;

import android.app.Activity;
import net.anei.cadpage.R;

/**
Recalculate Purchase Status
 */
public class DonateResetMarketEvent extends AccountScreenEvent {
  
  private DonateResetMarketEvent() {
    super(AlertStatus.YELLOW, R.string.donate_reset_market_title);
    setAction(new AllowAcctPermissionAction() {
      @Override
      public void doEvent(Activity activity) {

        // Request complete status reload
        DonationManager.instance().refreshStatus(activity);
        closeEvents(activity);
      }
    });
  }

  private static final DonateResetMarketEvent instance = new DonateResetMarketEvent();
  
  public static DonateResetMarketEvent instance() {
    return instance;
  }

}
