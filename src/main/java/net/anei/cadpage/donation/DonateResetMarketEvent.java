package net.anei.cadpage.donation;

import android.app.Activity;
import net.anei.cadpage.R;
import net.anei.cadpage.billing.BillingManager;

/**
Recalculate Purchase Status
 */
public class DonateResetMarketEvent extends AccountScreenEvent {
  
  private DonateResetMarketEvent() {
    super(AlertStatus.YELLOW, R.string.donate_reset_market_title);
    setAction(activity -> {

      // Request complete status reload
      BillingManager.instance().restoreTransactions(activity);
      closeEvents(activity);
    });
  }

  private static final DonateResetMarketEvent instance = new DonateResetMarketEvent();
  
  public static DonateResetMarketEvent instance() {
    return instance;
  }

}
