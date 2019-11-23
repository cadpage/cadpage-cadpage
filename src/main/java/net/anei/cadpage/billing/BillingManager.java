package net.anei.cadpage.billing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;

import net.anei.cadpage.BuildConfig;
import net.anei.cadpage.donation.DonateEvent;


public class BillingManager {

  private static final Billing[] billingClients = new Billing[]{
    new GoogleBilling(),
    new AuthServerBilling()
  };

  @SuppressLint("StaticFieldLeak")
  private static final Billing purchaseClient = (BuildConfig.APTOIDE ? null : billingClients[0]);

  /**
   * Initialize billing manager
   * @param context current context
   */
  public void initialize(Context context) {
    for (Billing client : billingClients) {
      client.initialize(context);
    }
  }

  /**
   * @return true if In-app billing is supported for this system
   */
  public boolean isSupported() {
    return purchaseClient != null && purchaseClient.isSupported();
  }

  /**
   * Shutdown billing manager
   */
  public void destroy() {
    for (Billing client : billingClients) {
      client.destroy();
    }
  }

  /**
   * Request transaction history restore
   */
  public void restoreTransactions(Context context) {
    for (Billing client : billingClients) {
      client.restoreTransactions(context);
    }
  }

  /**
   * Request purchase of current year product
   * @param activity current activity
   * @param donateEvent donation event or null if none
   */
  public void startPurchase(Activity activity, DonateEvent donateEvent) {
    if (purchaseClient == null) return;
    purchaseClient.startPurchase(activity, donateEvent);
  }

  private static final BillingManager instance = new BillingManager();
  
  public static BillingManager instance() {
    return instance;
  }
}
