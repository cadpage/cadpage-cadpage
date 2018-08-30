package net.anei.cadpage.donation;

import android.app.Activity;

import net.anei.cadpage.R;
import net.anei.cadpage.SmsPopupUtils;
import net.anei.cadpage.billing.BillingManager;

/**
 * Purchase through Google Play Store
 */
public class AndroidDonateConfirmEvent extends DonateScreenEvent {

  public AndroidDonateConfirmEvent() {
    super(AlertStatus.GREEN, R.string.donate_android_title, R.string.donate_android_confirm_text,
          AndroidDonate2Event.instance(),
          AndroidDonateCancelEvent.instance());
  }

  @Override
  public boolean isEnabled() {
    return BillingManager.instance().isSupported() && DonationManager.instance().isEarlyRenewalWarning();
  }

  @Override
  protected Object[] getTextParms(int type) {
    if (type == PARM_TEXT) {
      DonationManager mgr = DonationManager.instance();
      return new Object[]{mgr.expireDate(), mgr.daysTillExpire()};
    }
    return null;
  }

  private static final AndroidDonateConfirmEvent instance = new AndroidDonateConfirmEvent();
  
  public static AndroidDonateConfirmEvent instance() {
    return instance;
  }

}
