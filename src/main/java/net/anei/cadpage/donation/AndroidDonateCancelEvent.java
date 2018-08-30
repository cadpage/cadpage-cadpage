package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;
import net.anei.cadpage.billing.BillingManager;

/**
 * I will purchase a subscription later
 */
public class AndroidDonateCancelEvent extends DonateEvent {

  private AndroidDonateCancelEvent() {
    super(AlertStatus.GREEN, R.string.donate_android_cancel_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    closeEvents(activity);
  }
  
  private static final AndroidDonateCancelEvent instance = new AndroidDonateCancelEvent();
  
  public static AndroidDonateCancelEvent instance() {
    return instance;
  }

}
