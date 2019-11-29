package net.anei.cadpage.donation;

import net.anei.cadpage.BuildConfig;
import net.anei.cadpage.R;
import net.anei.cadpage.billing.BillingManager;

/**
Donate through Android Market

The Android Market app reports that in-app purchases are not supported.
You may need to upgrade the Market app to the latest version.  Or you may
need to give it a valid Google account (Select menu > Accounts in the Market
app) 

 */
public class DonateAndroidUnsuportedEvent extends DonateScreenEvent {

  private static int DONATE_ANDROID_TITLE =
      BuildConfig.APTOIDE ? R.string.donate_android_aptoide_title : R.string.donate_android_google_title;

  public DonateAndroidUnsuportedEvent() {
    super(AlertStatus.YELLOW, DONATE_ANDROID_TITLE, R.string.donate_android_unsupported_text);
  }

  @Override
  public boolean isEnabled() {
    return !BillingManager.instance().isSupported();
  }
  
  private static final DonateAndroidUnsuportedEvent instance = new DonateAndroidUnsuportedEvent();
  
  public static DonateAndroidUnsuportedEvent instance() {
    return instance;
  }

}
