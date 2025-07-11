package net.anei.cadpage.billing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@SuppressWarnings({"UnnecessaryReturnStatement", "SpellCheckingInspection"})
class GoogleBilling extends Billing implements PurchasesUpdatedListener {

  // private static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwirww3C/PjZeoU9xe49Z24nhKpw2nml2bhyRtp2hZysWnxskv+DpqBDXPW4o8CLnHzIld4aq6tSZhecoHRtjmpMsh+eYr76VoITEa8F/7JN5+niOspLoM7n8CpFxCDtQ4ILKXLTm5GKsfbEl7D2um0WnwVIaw3sBWKh99YAeXKp7tB/Oj8h9p9L7BLFbI00jVXzmg+4920hJ2mA0EeGM1sSdJxyh0V0k7jLEZwe8mo0nL21Ss+NbA9IVf6j4nIf4A0NUbOrTtPEBIaN1HpsKUyqdpwUYX9RIRybKE5nW0SJ3VNBBa+0ld5Yg4c5uikUznJeEVk+9KE9gV8NcNJzNLQIDAQAB";

  private BillingClient mBillingClient = null;

  private ProductDetails cadpageSubProductDetails = null;

  @Override
  public void initialize(Context context) {

    if (Log.DEBUG) Log.v("Starting Google billing setup.");
    mBillingClient =
        BillingClient.newBuilder(context)
//              .enableAutoServiceReconnection()
              .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
              .setListener(this).build();

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
    Log.v("Connect Google Billing");
    mBillingClient.startConnection(new BillingClientStateListener() {
      @SuppressLint("SwitchIntDef")
      @Override
      public void onBillingSetupFinished(@NonNull BillingResult billingResponseCode) {
        Log.v("Google billing connection complete. Response code: " + billingResponseCode.getDebugMessage());

        switch (billingResponseCode.getResponseCode()) {
          case BillingClient.BillingResponseCode.OK:
            setStatus(BillingStatus.OK);

            QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder().setProductType(BillingClient.ProductType.SUBS).setProductId("cadpage_sub").build();
            QueryProductDetailsParams queryProductDetailParms = QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(product)).build();
            mBillingClient.queryProductDetailsAsync(queryProductDetailParms, (billingResult, productDetailsResult) -> {
              if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                List<ProductDetails> list = productDetailsResult.getProductDetailsList();
                if (!list.isEmpty()) {
                  cadpageSubProductDetails = list.get(0);
                  Log.v("Retrived subscription product:" + cadpageSubProductDetails.toString());
                } else {
                  Log.e("queryProductDetailsAsync return no products");
                }
              }
              else Log.e("queryProductDetailsAsync failure result:" + billingResult.getDebugMessage());
            });
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
        Log.v("Google billing disconnected");
        setStatus(BillingStatus.NOT_CONNECTED);
      }
    });
  }

  @Override
  void doRestoreTransactions(Context context) {
    Log.v("Google Restore Billing Transactions");
    final DonationCalculator calc = new DonationCalculator(1);
    collectResults(calc, BillingClient.ProductType.SUBS, () -> {
      collectResults(calc, BillingClient.ProductType.INAPP, () -> {
        CadPageApplication.runOnMainThread(() -> {
          calc.save();
        });
      });
    });
  }

  private void collectResults(DonationCalculator calc, String skuType, Runnable run) {
    QueryPurchasesParams parms = QueryPurchasesParams.newBuilder().setProductType(skuType).build();
    mBillingClient.queryPurchasesAsync(parms, (result, list) -> {
      if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
        for (Purchase purchase : list) {
          registerPurchaseState(purchase, calc);
        }
      } else {
        Log.e("Error retrieving " + skuType + " purchases:" + result.getDebugMessage());
      }
      run.run();
    });
  }

  @Override
  void doStartPurchase(BillingActivity activity) {
    Log.v("Launching Google billing flow");
    if (cadpageSubProductDetails == null) {
      Log.e("No subscription product defined");
      return;
    }
    List<ProductDetails.SubscriptionOfferDetails> list = cadpageSubProductDetails.getSubscriptionOfferDetails();
    if (list == null || list.isEmpty()) {
      Log.e("No subscription offers are available");
      return;
    }
    BillingFlowParams.ProductDetailsParams productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(cadpageSubProductDetails).setOfferToken(list.get(0).getOfferToken()).build();
    BillingFlowParams flowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(Collections.singletonList(productDetailsParams)).build();
    BillingResult billingResult = mBillingClient.launchBillingFlow(activity, flowParams);
    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
      Log.e("launchBillingFlow error result:" + billingResult.getDebugMessage());
    }
  }

  @Override

  public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {

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
    for (String itemId : purchase.getProducts()) {
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
