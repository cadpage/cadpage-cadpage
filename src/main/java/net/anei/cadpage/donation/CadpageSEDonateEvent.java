package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.donation.DonationManager.DonationStatus;

/**
  Sweden location

    You are using a location in Sweden.\n
    We are not charging Swedish users at this time.  Partly because non-US location
    support is still experimental and partly because we want encourage more
    users to try it there.  We may change this at some later time if/when
    Cadpage becomes well established there.
  
 */
public class CadpageSEDonateEvent extends DonateScreenEvent {
  
  public CadpageSEDonateEvent() {
    super(AlertStatus.GREEN, R.string.donate_cadpage_se_title, R.string.donate_cadpage_se_text,
           VendorEvent.instance(2),
           AndroidDonateEvent.instance(),
           DonateAndroidSuppressedEvent.instance(),
           MagicWordEvent.instance());
  }
  
  @Override
  public boolean isEnabled() {
    return (DonationManager.instance().status() == DonationStatus.SPONSOR &&
            "SE".equals(DonationManager.instance().sponsor()));
  }

  private static final CadpageSEDonateEvent instance = new CadpageSEDonateEvent();
  
  public static CadpageSEDonateEvent instance() {
    return instance;
  }

}
