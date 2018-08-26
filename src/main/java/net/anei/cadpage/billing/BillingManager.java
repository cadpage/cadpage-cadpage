package net.anei.cadpage.billing;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import net.anei.cadpage.Log;
import net.anei.cadpage.donation.DonationCalculator;

import org.json.JSONException;
import org.json.JSONObject;


@SuppressWarnings("RedundantIfStatement")
public class BillingManager implements PurchasesUpdatedListener {

  // private static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwirww3C/PjZeoU9xe49Z24nhKpw2nml2bhyRtp2hZysWnxskv+DpqBDXPW4o8CLnHzIld4aq6tSZhecoHRtjmpMsh+eYr76VoITEa8F/7JN5+niOspLoM7n8CpFxCDtQ4ILKXLTm5GKsfbEl7D2um0WnwVIaw3sBWKh99YAeXKp7tB/Oj8h9p9L7BLFbI00jVXzmg+4920hJ2mA0EeGM1sSdJxyh0V0k7jLEZwe8mo0nL21Ss+NbA9IVf6j4nIf4A0NUbOrTtPEBIaN1HpsKUyqdpwUYX9RIRybKE5nW0SJ3VNBBa+0ld5Yg4c5uikUznJeEVk+9KE9gV8NcNJzNLQIDAQAB";

  private BillingClient mBillingClient;

  private List<Runnable> eventQueue = null;
  
  /**
   * @return true if In-app billing is supported for this system
   */
  public boolean isSupported() {
    return mBillingClient != null && mBillingClient.isReady();
  }
  
  /**
   * Initialize billing manager
   * @param context current context
   */
  public void initialize(Context context) {

    // Start setup. This is asynchronous and the specified listener
    // will be called once setup completes.
    if (Log.DEBUG) Log.v("Starting billing setup.");

    mBillingClient = BillingClient.newBuilder(context).setListener(this).build();

    // Restore transactions when billing connection is established
    // Queue event to restore our purchase status
    restoreTransactions();
  }

  /**
   * Shutdown billing manager
   */
  public void destroy() {
    Log.v("Destroying the billnig manager.");

    if (mBillingClient != null && mBillingClient.isReady()) {
      mBillingClient.endConnection();
      mBillingClient = null;
    }
  }

  private boolean inProgress = false;
  
  /**
   * Queue runnable event to be run when billing is up and running
   * @param event Runnable event to be run when billing is supported
   */
  private void runWhenSupported(Runnable event) {

    // We do not do any locking because we are only supposed to be called on the UI thread
    // But might be a good idea to confirm that
    if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
      throw new RuntimeException("BillingManger.runWhenSupported() needs to be called on UI thread");
    }

    // If billing is up and running, just run event
    if (mBillingClient.isReady()) {
      event.run();
      return;
    }

    // Otherwise add the event to the event queue
    if (eventQueue == null) eventQueue = new ArrayList<>();
    eventQueue.add(event);

    // If the billing connection is in progress, do not start it a second time
    if (!inProgress) {
      inProgress = true;
      Log.v("Initiating billing connection");
      mBillingClient.startConnection(new BillingClientStateListener() {
        @Override
        public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
          Log.v("Billing connection complete. Response code: " + billingResponseCode);
          inProgress = false;

          // If we are connected, run all of the queued events
          if (billingResponseCode == BillingClient.BillingResponse.OK) {

            // Run any events that have been queued waiting for this to happen
            if (eventQueue != null) {
              for (Runnable event : eventQueue) event.run();
            }
            eventQueue = null;
          }
        }

        @Override
        public void onBillingServiceDisconnected() {
          inProgress = false;
        }
      });
    }
  }

  /**
   * Request transaction history restore
   */
  public void restoreTransactions() {
    Log.v("Queue Restore Billing Transactions");
    runWhenSupported(new Runnable(){
      @Override
      public void run() {
        Log.v("Restore Billing Transactions");
        DonationCalculator calc = new DonationCalculator(1);
        collectResults(calc, BillingClient.SkuType.SUBS);
        collectResults(calc, BillingClient.SkuType.INAPP);
        calc.save();
      }

      private void collectResults(DonationCalculator calc, String skuType) {
        Purchase.PurchasesResult result = mBillingClient.queryPurchases(skuType);
        if (result.getResponseCode() == BillingClient.BillingResponse.OK) {
          List<Purchase> list = result.getPurchasesList();
          if (list != null) {
            for (Purchase purchase : list) {
              registerPurchaseState(purchase, calc);
            }
          }
        } else {
          Log.e("Error retrieving " + skuType + " purchases:" + result.getResponseCode());
        }
      }
    });
  }

  /**
   * Request purchase of current year product
   * @param activity current activity
   */
  public void startPurchase(Activity activity) {
    if (!isSupported()) return;

    if (activity == null) return;
    if (activity.isFinishing()) return;

    if (Log.DEBUG) Log.v("Initiating subscription purchase");

    mBillingClient.launchBillingFlow(activity,
        BillingFlowParams.newBuilder()
            .setSku("cadpage_sub")
            .setType(BillingClient.SkuType.SUBS)
            .build());
  }

  @Override
  public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

    if (Log.DEBUG) Log.v("Purchase result:" + responseCode);
    if (responseCode == BillingClient.BillingResponse.OK) {
      if (purchases != null) {
        DonationCalculator calc = new DonationCalculator(1);
        calc.load();
        for (Purchase purchase : purchases) {
          if (Log.DEBUG) Log.v(purchase.toString());
          registerPurchaseState(purchase, calc);
        }
        calc.save();
      }
    }
  }

  private static final DateFormat DATE_FMT = new SimpleDateFormat("MMddyyyy");

  /**
   * Parser purchase history into a donation calculator
   * @param purchase purchase object
   * @param calc donation calculator
   */
  private void registerPurchaseState(Purchase purchase, DonationCalculator calc) {
    if (Log.DEBUG) Log.v(purchase.toString());

    // Get the purchase Sku code and confirm that it starts with Cadpage
    String itemId = purchase.getSku();
    if (itemId.startsWith("cadpage_")) {

      // Subscriptions start with sub.  Purchase date will be the actual
      // purchase date and year will be the actual purchase year
      String year = itemId.substring(8);
      String purchaseDate;
      if (year.startsWith("sub")) {
        purchaseDate = DATE_FMT.format(new Date(purchase.getPurchaseTime()));
        year = purchaseDate.substring(4);
      }

      // We used to emulate subscriptions with a series of inapp product purchases.  We do not do
      // that anymore, but still need to support to old purchases.  Year is derived from name of
      // the purchased product.  Purchase date was stored in developerPayload field which is not
      // supported by billing library, so we will have to retrieve it ourselves
      else {
        try {
          purchaseDate = new JSONObject(purchase.getOriginalJson()).optString("developerPayload");
        } catch (JSONException ex) {
          throw new RuntimeException("Error parsing developer payload");
        }
      }
      calc.subscription(year, purchaseDate, null);
    }
  }

  private static final BillingManager instance = new BillingManager();
  
  public static BillingManager instance() {
    return instance;
  }
}
