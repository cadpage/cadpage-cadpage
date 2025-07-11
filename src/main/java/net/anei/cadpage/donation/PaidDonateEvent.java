package net.anei.cadpage.donation;

import java.text.DateFormat;
import java.util.Date;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

/**
Your Cadpage subscription is current

Your current Cadpage subscription will expire on\n%s
 */
public class PaidDonateEvent extends DonateScreenEvent {
  
  public PaidDonateEvent() {
    super(AlertStatus.GREEN, R.string.donate_paid_title, R.string.donate_paid_text,
          VendorEvent.instance(1),
          AndroidDonateEvent.instance(),
          DonateAndroidSuppressedEvent.instance(),
          RequestRefundDonateEvent.instance(),
          MagicWordEvent.instance(),
          DonateResetMarketEvent.instance());
  }

  @Override
  public boolean isEnabled() {
    return (DonationManager.instance().status() == DonationManager.DonationStatus.PAID);
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
  
  private static final PaidDonateEvent instance = new PaidDonateEvent();
  
  public static PaidDonateEvent instance() {
    return instance;
  }

}
