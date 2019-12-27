package net.anei.cadpage.billing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.appcoins.sdk.billing.AppCoinsBillingStateListener;
import com.appcoins.sdk.billing.AppcoinsBillingClient;
import com.appcoins.sdk.billing.BillingFlowParams;
import com.appcoins.sdk.billing.Purchase;
import com.appcoins.sdk.billing.PurchasesResult;
import com.appcoins.sdk.billing.PurchasesUpdatedListener;
import com.appcoins.sdk.billing.ResponseCode;
import com.appcoins.sdk.billing.SkuDetails;
import com.appcoins.sdk.billing.SkuDetailsParams;
import com.appcoins.sdk.billing.SkuDetailsResponseListener;
import com.appcoins.sdk.billing.helpers.CatapultBillingAppCoinsFactory;
import com.appcoins.sdk.billing.types.SkuType;

import net.anei.cadpage.Log;
import net.anei.cadpage.ManagePreferences;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@SuppressWarnings({"SpellCheckingInspection"})
class AptoideBilling extends Billing implements PurchasesUpdatedListener {

  private static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApHzsCH1/xZtjQRkLmIWzrY6SASyVdUGp71sD86K849MQrLY8Dn7xnPV4+Z+dyy8cBkjEFPSImrnkeLKQFg6ASHxqV+eLskZl0CGBjg0u+ImRD37RpQoJtP/VcNgpQIo8V0qNoZv2E9l1Q0Y7lnlPJiE7fhwUk4oyG6eLihJreQ4UeXon7nA81iBOFvsdlVAc+ovHnk4MaGZ4Vyp7lsOg76PbVYgUHlg10df3KT0jdmH0EJUdVIUZibYQonS2BX1kz8VRvFJ7GQAbrfRtDaIU0qCniSMmggpL+K05opkB8bV+I7MjhMEDDMEXEhVOGRHt0NFRS3SuL8ZR8Iy4myWyrwIDAQAB";

  private AppcoinsBillingClient mBillingClient;

  @Override
  public void initialize(Context context) {}

  @Override
  public void initialize(Activity activity) {
    if (Log.DEBUG) Log.v("Starting Aptoide billing setup.");
    mBillingClient = CatapultBillingAppCoinsFactory.BuildAppcoinsBilling(activity, BASE_64_ENCODED_PUBLIC_KEY, this);
    restoreTransactions(activity);
  }

  /**
   * Shutdown billing manager
   */
  @Override
  public void destroy() {
    Log.v("Destroying Aptoide billing");

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
    mBillingClient.startConnection(new AppCoinsBillingStateListener() {
      @Override
      public void onBillingSetupFinished(int billingResponseCode) {
        Log.v("Aptoide billing connection complete. Response code: " + billingResponseCode);

        if (billingResponseCode == ResponseCode.OK.getValue()) {
          setStatus(BillingStatus.OK);
        }

        else {
          setStatus(BillingStatus.NOT_SUPPORTED);
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
    Log.v("Aptoide Restore Billing Transactions");
    DonationCalculator calc = new DonationCalculator(3);
//    collectResults(calc, BillingClient.SkuType.SUBS);
    collectResults(calc, BillingClient.SkuType.INAPP);
    calc.save();
  }

  @SuppressWarnings("SameParameterValue")
  private void collectResults(DonationCalculator calc, String skuType) {
    PurchasesResult result = mBillingClient.queryPurchases(skuType);
    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
      List<Purchase> list = result.getPurchases();
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

    int curYear = ManagePreferences.paidYear();
    String year = null;
    String purchaseDate = null;
    String curDate = ManagePreferences.currentDateString();

    // If paid subscription has already purchased, use the previous
    // purchase date.  Unless user subscription has expired, in
    // which case we will give them a break and ignore the
    // previous expired subscription
    if (curYear > 0 && !ManagePreferences.freeSub()) {
      year = Integer.toString(curYear + 1);
      purchaseDate = ManagePreferences.purchaseDateString();
      if (purchaseDate == null) purchaseDate = ManagePreferences.currentDateString();
      String expDateYMD = year + purchaseDate.substring(0, 4);
      String curDateYMD = curDate.substring(4) + curDate.substring(0, 4);
      if (curDateYMD.compareTo(expDateYMD) > 0) purchaseDate = null;
    }

    // If there was no previous subscription, or we are ignoring it
    // because it has expired, use current date to compute things
    if (purchaseDate == null) {
      purchaseDate = curDate;
      year = purchaseDate.substring(4);
    }

    String item = "cadpage_" + year;
    String payload = purchaseDate;

    Log.v("Purchase request for " + item + " payload:" + payload);

    BillingFlowParams billingFlowParams =
        new BillingFlowParams(item, SkuType.inapp.toString(),
            "orderId=" + System.currentTimeMillis(),
            payload,
            null);

    Log.v(billingFlowParams.toString());
    int response = mBillingClient.launchBillingFlow(activity, billingFlowParams);
    if (response != BillingClient.BillingResponseCode.OK) {
      Log.e("Purchase failure: " + response);
    }

    SkuDetailsParams params = new SkuDetailsParams();
    params.setItemType(SkuType.inapp.toString());
    mBillingClient.querySkuDetailsAsync(params, new SkuDetailsResponseListener(){
      @Override
      public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
        Log.v("SkuDetailsResponse: " + responseCode);
        if (skuDetailsList != null) {
          for (SkuDetails details : skuDetailsList) {
            Log.v(details.toString());
          }
        }
      }
    });
  }

  @Override
  void onActivityResult(int requestCode, int resultCode, Intent data) {
    mBillingClient.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onPurchasesUpdated(int billingResult, @Nullable List<Purchase> purchases) {

    if (Log.DEBUG) Log.v("Purchase result:" + billingResult);
    if (billingResult == BillingClient.BillingResponseCode.OK) {
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
    if (purchase.getPurchaseState() != 0) return;

    // Get the purchase Sku code and confirm that it starts with Cadpage
    String itemId = purchase.getSku();
    if (!itemId.startsWith("cadpage_")) return;

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
      int curMonthDay = (cal.get(Calendar.MONTH)+1)*100+cal.get(Calendar.DAY_OF_MONTH);
      if (curMonthDay < Integer.parseInt(purchaseDate.substring(0,4))) iYear--;
      year = Integer.toString(iYear);
      subStatus = purchase.isAutoRenewing() ? 2 : 1;
      Log.v("curMonthDay="+curMonthDay+"  - " + purchaseDate.substring(0,4) + "  iYear=" + iYear);
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
