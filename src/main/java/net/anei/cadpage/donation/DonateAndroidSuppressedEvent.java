package net.anei.cadpage.donation;

import net.anei.cadpage.R;
import net.anei.cadpage.billing.BillingManager;

/**
Donate through Android Market

 Sadly, Aptoide Store payments are not working and Aptoide support has been unhelpful.
 Until this is resolved, Paypal payments are your only option.

 */
public class DonateAndroidSuppressedEvent extends DonateScreenEvent {

  public DonateAndroidSuppressedEvent() {
    super(AlertStatus.YELLOW, DONATE_ANDROID_TITLE, R.string.donate_android_suppressed_text,
          PaypalDonateEvent.instance());
  }

  @Override
  public boolean isEnabled() {
    return BillingManager.instance().isSuppressed();
  }
  
  private static final DonateAndroidSuppressedEvent instance = new DonateAndroidSuppressedEvent();
  
  public static DonateAndroidSuppressedEvent instance() {
    return instance;
  }

}
