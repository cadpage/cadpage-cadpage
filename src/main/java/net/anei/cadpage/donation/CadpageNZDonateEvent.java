package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.donation.DonationManager.DonationStatus;

/**
  
  New Zealand location
  
    You are using a location in the New Zealand.\n
    We are not charging UK users at this time.  Partly because non-US location
    support is still experimental and partly because we want encourage more
    users to try it there.  We may change this at some later time if/when
    Cadpage becomes well established there.
  
 */
public class CadpageNZDonateEvent extends DonateScreenEvent {
  
  public CadpageNZDonateEvent() {
    super(AlertStatus.GREEN, R.string.donate_cadpage_nz_title, R.string.donate_cadpage_nz_text,
           VendorEvent.instance(2),
           AndroidDonateEvent.instance(),
           DonateAndroidSuppressedEvent.instance(),
           MagicWordEvent.instance());
  }
  
  @Override
  public boolean isEnabled() {
    return (DonationManager.instance().status() == DonationStatus.SPONSOR &&
            "NZ".equals(DonationManager.instance().sponsor()));
  }

  private static final CadpageNZDonateEvent instance = new CadpageNZDonateEvent();
  
  public static CadpageNZDonateEvent instance() {
    return instance;
  }

}
