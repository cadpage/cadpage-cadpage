package net.anei.cadpage.donation;

import android.app.Activity;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;

/**
Recalculate Purchase Status
 */
public class DonateResetMarketEvent extends AccountScreenEvent {
  
  public DonateResetMarketEvent() {
    super(AlertStatus.YELLOW, R.string.donate_reset_market_title,
        new AllowAcctPermissionAction() {
          @Override
          public void doEvent(Activity activity) {

            // Request complete status reload
            DonationManager.instance().refreshStatus(activity);
          }
        });
  }

  private static final DonateResetMarketEvent instance = new DonateResetMarketEvent();
  
  public static DonateResetMarketEvent instance() {
    return instance;
  }

}
