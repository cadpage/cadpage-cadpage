package net.anei.cadpage.billing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;

import net.anei.cadpage.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@SuppressWarnings({"UnnecessaryReturnStatement", "SpellCheckingInspection"})
class GoogleBilling extends Billing implements PurchasesUpdatedListener {

  // private static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwirww3C/PjZeoU9xe49Z24nhKpw2nml2bhyRtp2hZysWnxskv+DpqBDXPW4o8CLnHzIld4aq6tSZhecoHRtjmpMsh+eYr76VoITEa8F/7JN5+niOspLoM7n8CpFxCDtQ4ILKXLTm5GKsfbEl7D2um0WnwVIaw3sBWKh99YAeXKp7tB/Oj8h9p9L7BLFbI00jVXzmg+4920hJ2mA0EeGM1sSdJxyh0V0k7jLEZwe8mo0nL21Ss+NbA9IVf6j4nIf4A0NUbOrTtPEBIaN1HpsKUyqdpwUYX9RIRybKE5nW0SJ3VNBBa+0ld5Yg4c5uikUznJeEVk+9KE9gV8NcNJzNLQIDAQAB";

  private BillingClient mBillingClient;

  @Override
  public void initialize(Context context) {

    if (Log.DEBUG) Log.v("Starting Google billing setup.");
    mBillingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build();

    restoreTransactions(context);
  }

  @Override
  public void initialize(Activity activity) {}

  /**
   * Shutdown billing manager
   */
  @Override
  public void destroy() {
    Log.v("Destroying Google billing");

    if (mBillingClient != null && mBillingClient.isReady()) {
      mBillingClient.endConnection();
      mBillingClient = null;
    }
  }

  @Override
  public boolean isReady() {
    return mBillingClient != null && mBillingClient.isReady();
  }

  @Override
  void doConnect() {
    mBillingClient.startConnection(new BillingClientStateListener() {
      @SuppressLint("SwitchIntDef")
      @Override
      public void onBillingSetupFinished(BillingResult billingResponseCode) {
        Log.v("Google billing connection complete. Response code: " + billingResponseCode.getDebugMessage());

        switch (billingResponseCode.getResponseCode()) {
          case BillingClient.BillingResponseCode.OK:
            setStatus(BillingStatus.OK);
            return;

          case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
            setStatus(BillingStatus.NOT_CONNECTED);
            return;

          default:
            setStatus(BillingStatus.NOT_SUPPORTED);
            return;
        }
      }

      @Override
      public void onBillingServiceDisconnected() {
        setStatus(BillingStatus.NOT_CONNECTED);
      }
    });
  }

  @Override
  void doRestoreTransactions(Context context) {
    Log.v("Google Restore Billing Transactions");
    DonationCalculator calc = new DonationCalculator(1);
    collectResults(calc, BillingClient.SkuType.SUBS);
    collectResults(calc, BillingClient.SkuType.INAPP);
    calc.save();
  }

  private void collectResults(DonationCalculator calc, String skuType) {
    Purchase.PurchasesResult result = mBillingClient.queryPurchases(skuType);
    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
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

  @Override
  void doStartPurchase(BillingActivity activity) {

    List<String> skuList = new ArrayList<> ();
    skuList.add("cadpage_sub");
    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder()
        .setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
    mBillingClient.querySkuDetailsAsync(params.build(),
        (billingResult, skuDetailsList) -> {
          if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e("Error retrieving SKU details: " + billingResult.getDebugMessage());
          } else if (skuDetailsList == null || skuDetailsList.size() == 0) {
            Log.e("No SKU details found");
          } else {
            mBillingClient.launchBillingFlow(activity,
                BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetailsList.get(0))
                    .build());
          }
        });

  }

  @Override
  public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {

    if (Log.DEBUG) Log.v("Purchase result:" + billingResult.getDebugMessage());
    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
      if (purchases != null) {
        DonationCalculator calc = new DonationCalculator(1);
        calc.load();
        for (Purchase purchase : purchases) {
          if (Log.DEBUG) Log.v(purchase.toString());
          registerPurchaseState(purchase, calc);
        }
        calc.save();
      }
      endPurchase(true);
    } else {
      endPurchase(false);
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

    // Confirm item purchase is complete
    if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;

    // Acknowledge the purchase if it hasn't already been acknowledged.
    if (!purchase.isAcknowledged()) {
      if (Log.DEBUG) Log.v("Acknowledging purchase");
      AcknowledgePurchaseParams acknowledgePurchaseParams =
              AcknowledgePurchaseParams.newBuilder()
                      .setPurchaseToken(purchase.getPurchaseToken())
                      .build();
      mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> Log.v("Purchase acknowledged: " + billingResult.getDebugMessage()));
    }

    // Get the purchase Sku code and confirm that it starts with Cadpage
    for (String itemId : purchase.getSkus()) {
      if (!itemId.startsWith("cadpage_")) continue;

      // Subscriptions start with sub.  Purchase date will be the actual
      // purchase date.  The fact that a subscription is being reported means that it
      // should be honored, so adjust the year to make the subscription current
      String year = itemId.substring(8);
      String purchaseDate;
      int subStatus;
      if (year.startsWith("sub")) {
        purchaseDate = DATE_FMT.format(new Date(purchase.getPurchaseTime()));
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        int iYear = cal.get(Calendar.YEAR);
        int curMonthDay = (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH);
        if (curMonthDay < Integer.parseInt(purchaseDate.substring(0, 4))) iYear--;
        year = Integer.toString(iYear);
        subStatus = purchase.isAutoRenewing() ? 2 : 1;
        Log.v("curMonthDay=" + curMonthDay + "  - " + purchaseDate.substring(0, 4) + "  iYear=" + iYear);
      }

      // We used to emulate subscriptions with a series of inapp product purchases.  We do not do
      // that anymore, but still need to support to old purchases.  Year is derived from name of
      // the purchased product.  Purchase date was stored in developerPayload field
      else {
        purchaseDate = purchase.getDeveloperPayload();
        subStatus = 0;
      }
      calc.subscription(year, purchaseDate, null, subStatus);
    }
  }
}
