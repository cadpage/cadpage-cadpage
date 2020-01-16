package net.anei.cadpage.donation;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

import java.text.DateFormat;
import java.util.Date;

/**
Your Cadpage subscription is current

Your current Cadpage subscription will expire on\n%s
 */
public class PaidRenewDonateEvent extends DonateScreenEvent {

  private PaidRenewDonateEvent() {
    super(AlertStatus.GREEN, R.string.donate_paid_renew_title, R.string.donate_paid_renew_text,
          VendorEvent.instance(1),
          AndroidDonateEvent.instance(),
          DonateAndroidSuppressedEvent.instance(),
          PaypalDonateEvent.instance(),
          RequestRefundDonateEvent.instance(),
          MagicWordEvent.instance(),
          DonateResetMarketEvent.instance());
  }

  @Override
  public boolean isEnabled() {
    return (DonationManager.instance().status() == DonationManager.DonationStatus.PAID_RENEW);
  }

  @Override
  protected Object[] getTextParms(int type) {
    
    String subType = ManagePreferences.subscriptionType();
      
    switch (type) {
    
    case PARM_TITLE:
      return new Object[]{subType};
      
    case PARM_TEXT:
      Date expireDate = DonationManager.instance().expireDate();
      String sDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(expireDate);
      return new Object[]{subType, sDate};

    default:
      return null;
    }
  }
  
  private static final PaidRenewDonateEvent instance = new PaidRenewDonateEvent();
  
  public static PaidRenewDonateEvent instance() {
    return instance;
  }

}
