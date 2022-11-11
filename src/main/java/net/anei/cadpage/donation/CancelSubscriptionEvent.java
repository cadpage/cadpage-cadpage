package net.anei.cadpage.donation;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import net.anei.cadpage.EmailDeveloperActivity;
import net.anei.cadpage.Log;
import net.anei.cadpage.R;

/**
 * Generate an email to the developers
 */
public class CancelSubscriptionEvent extends DonateEvent {

  public CancelSubscriptionEvent() {
    super(null, R.string.donate_cancel_subscription_title);
  }

  @Override
  protected void doEvent(Activity activity) {
    Intent intent = new Intent(Intent.ACTION_VIEW,
                               Uri.parse("https://play.google.com/store/account/subscriptions?sku=cadpage_sub&package=net.anei.cadpage"));
    try {
      activity.startActivity(intent);
    } catch (ActivityNotFoundException ex) {
      Log.e("Could not launch Cadpage subscription page view");
    }

  }

  private static final CancelSubscriptionEvent instance = new CancelSubscriptionEvent();
  public static CancelSubscriptionEvent instance() {
    return instance;
  }

}
