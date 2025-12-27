package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.vendors.VendorManager;

/*
    
    Paging service requires a paid subscription
    
    Cadpage paging service is only available to users with current paid subscriptions.
    We have to pay to support each user on this service, we can not afford give
    it away.


 */
public class PagingSubRequiredEvent extends DonateScreenEvent {

  protected PagingSubRequiredEvent() {
    super(null, R.string.paging_sub_required_title, R.string.paging_sub_required_text,
          AndroidDonateEvent.instance(), 
          DonateAndroidUnsuportedEvent.instance(),
          DonateAndroidSuppressedEvent.instance(),
          AndroidDonateProblemEvent.instance(),
          DonateResetMarketEvent.instance(),
          WrongUserDonateEvent.instance());
  }
  
  @Override
  protected boolean overrideWindowTitle() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return !DonationManager.instance().isPaidSubscriber() &&
           !VendorManager.instance().isRegistered("CodeMessaging");
  }

  @Override
  public void onRestart(DonateActivity activity) {
    if (!isEnabled()) closeEvents(activity);
  }

  private static final PagingSubRequiredEvent instance = new PagingSubRequiredEvent();
  public static PagingSubRequiredEvent instance() {
    return instance;
  }
}
