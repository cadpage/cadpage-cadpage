package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.SmsPopupUtils;
import net.anei.cadpage.billing.BillingManager;

/**
 * Purchase through Google Play Store
 */
public class AndroidDonate1Event extends DonateEvent {

  private AndroidDonate1Event() {
    super(AlertStatus.GREEN, DONATE_ANDROID_TITLE);
  }

  @Override
  public boolean isEnabled() {
    return BillingManager.instance().isSupported() &&
           DonationManager.instance().status() != DonationManager.DonationStatus.PAID_RENEW &&
           !DonationManager.instance().isEarlyRenewalWarning();
  }

  @Override
  protected void doEvent(Activity activity) {
    if (!SmsPopupUtils.haveNet(activity)) return;
    BillingManager.instance().startPurchase(activity, this);
  }
  
  private static final AndroidDonate1Event instance = new AndroidDonate1Event();
  
  public static AndroidDonate1Event instance() {
    return instance;
  }

}
