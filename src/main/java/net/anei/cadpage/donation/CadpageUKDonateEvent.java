package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.donation.DonationManager.DonationStatus;

/**
  
  Sponsored by Cadpage

  You are using a location sponsored by Cadpage.\n 
  Cadpage sponsors the general locations because their performance is 
  unreliable and we do not want to charge users who are waiting until a
  better location can be implemented.
  
 */
public class CadpageUKDonateEvent extends DonateScreenEvent {
  
  public CadpageUKDonateEvent() {
    super(AlertStatus.GREEN, R.string.donate_cadpage_uk_title, R.string.donate_cadpage_uk_text,
           VendorEvent.instance(2),
           AndroidDonateEvent.instance(),
           DonateAndroidSuppressedEvent.instance(),
           MagicWordEvent.instance());
  }
  
  @Override
  public boolean isEnabled() {
    return (DonationManager.instance().status() == DonationStatus.SPONSOR &&
            "UK".equals(DonationManager.instance().sponsor()));
  }

  private static final CadpageUKDonateEvent instance = new CadpageUKDonateEvent();
  
  public static CadpageUKDonateEvent instance() {
    return instance;
  }

}
