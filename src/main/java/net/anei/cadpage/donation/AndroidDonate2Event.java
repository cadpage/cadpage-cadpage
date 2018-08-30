package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;
import net.anei.cadpage.billing.BillingManager;

/**
 * Go ahead with purchase now
 */
public class AndroidDonate2Event extends DonateEvent {

  private AndroidDonate2Event() {
    super(AlertStatus.GREEN, R.string.donate_android2_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    if (!SmsPopupUtils.haveNet(activity)) return;
    BillingManager.instance().startPurchase(activity, this);
  }
  
  private static final AndroidDonate2Event instance = new AndroidDonate2Event();
  
  public static AndroidDonate2Event instance() {
    return instance;
  }

}
