package net.anei.cadpage.donation;

import net.anei.cadpage.R;

/**
 No subscription required

 A purchased subscription is no longer required unless you are using the Cadpage paging service.

 */
public class FreeDonateEvent extends DonateScreenEvent {
  
  public FreeDonateEvent() {
    super(AlertStatus.GREEN, R.string.donate_free_title, R.string.donate_free_text);
  }
  
  @Override
  public boolean isEnabled() {
    return (DonationManager.instance().status() == DonationManager.DonationStatus.FREE);
  }
  
  private static final FreeDonateEvent instance = new FreeDonateEvent();
  
  public static FreeDonateEvent instance() {
    return instance;
  }

}
