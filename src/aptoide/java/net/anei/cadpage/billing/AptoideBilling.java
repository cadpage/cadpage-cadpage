package net.anei.cadpage.billing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import com.aptoide.sdk.billing.AptoideBillingClient;
import com.aptoide.sdk.billing.BillingFlowParams;
import com.aptoide.sdk.billing.BillingResult;
import com.aptoide.sdk.billing.ProductDetails;
import com.aptoide.sdk.billing.Purchase;
import com.aptoide.sdk.billing.PurchasesUpdatedListener;
import com.aptoide.sdk.billing.QueryProductDetailsParams;
import com.aptoide.sdk.billing.QueryPurchasesParams;
import com.aptoide.sdk.billing.UnfetchedProduct;
import com.aptoide.sdk.billing.listeners.AptoideBillingClientStateListener;

import androidx.annotation.Nullable;

import net.anei.cadpage.ContentQuery;
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

  private static final int RC_ONE_STEP = 99333;

  private AptoideBillingClient mBillingClient;

  @Override
  public void initialize(Context context) {}

  @Override
  public void initialize(Activity activity) {
    if (Log.DEBUG) Log.v("Starting Aptoide billing setup.");
    mBillingClient = AptoideBillingClient.newBuilder(activity)
        .setListener(this)
        .setPublicKey(BASE_64_ENCODED_PUBLIC_KEY)
        .build();

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
    mBillingClient.startConnection(new AptoideBillingClientStateListener() {
      @Override
      public void onBillingSetupFinished(BillingResult billingResult) {
        Log.v("Aptoide billing connection complete. Response code: " + billingResult);

        if (billingResult.getResponseCode() == AptoideBillingClient.BillingResponseCode.OK) {
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
    mBillingClient.queryPurchasesAsync(
              QueryPurchasesParams
                    .newBuilder()
                    .setProductType(AptoideBillingClient.ProductType.SUBS)
                    .build(),
            (billingResult, list) -> {
              if (billingResult.getResponseCode() == AptoideBillingClient.BillingResponseCode.OK) {
                DonationCalculator calc = new DonationCalculator(3);
                for (Purchase purchase : list) {
                  registerPurchaseState(purchase, calc);
                }
                calc.save();
              } else {
                Log.e("Error retrieving Aptoide purchases:" + billingResult);
              }

            }


    );
  }

  @Override
  void doStartPurchase(final BillingActivity activity) {

    String item = "cadpage_sub";
    QueryProductDetailsParams queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder().setProductList(
                                List.of(QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId("cadpage_sub")
                                                .setProductType(AptoideBillingClient.ProductType.SUBS)
                                                .build())
                ).build();

        mBillingClient.queryProductDetailsAsync(queryProductDetailsParams,
                (billingResult, productDetailsResult) -> {
                  	ProductDetails subProductDetails = null;
										if (billingResult.getResponseCode() == AptoideBillingClient.BillingResponseCode.OK) {
                        for (ProductDetails productDetails : productDetailsResult.getProductDetailsList()) {
                          if (subProductDetails == null)  subProductDetails = productDetails;
                        }

                        for (UnfetchedProduct unfetchedProduct : productDetailsResult.getUnfetchedProductList()) {
                          Log.v("Unfeteched Apdtoide product: " + unfetchedProduct.getProductId());
                        }
                    }

                    if (subProductDetails == null) {
                      Log.e("Failed to retrieve Aptoide Cadpage subscription product");
                      return;
                    }

                    String user = ManagePreferences.billingAccount();

                    List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = List.of(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                             .setProductDetails(subProductDetails)
                                             .build()
                    );

                    BillingFlowParams billingFlowParams =
                            BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(productDetailsParamsList)
                                    .setObfuscatedAccountId(user)
                                    .setFreeTrial(true)
                                    .build();

                    Thread thread = new Thread(() -> {
                        final BillingResult billingResult2 = mBillingClient.launchBillingFlow(activity, billingFlowParams);
                        activity.runOnUiThread(() -> {
                            if (billingResult2.getResponseCode() != AptoideBillingClient.BillingResponseCode.OK) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                builder.setMessage("Error purchasing with response code : " + billingResult2.getResponseCode());
                                builder.setNeutralButton("OK", null);
                                Log.e("Error purchasing with response code : " + billingResult2.getResponseCode());
                                builder.create().show();
                            }
                        });
                    });
                    thread.start();
                }
        );
  }


  private boolean purchase2(BillingActivity activity, String item, String payload, String reference) {
    Uri uri = Uri.parse("https://apichain.catappult.io/transaction/inapp").buildUpon()
        .appendQueryParameter("product", item)
        .appendQueryParameter("domain", "net.anei.cadpage")
        .appendQueryParameter("data", payload)
        .appendQueryParameter("order_reference", reference)
        .build();

    Intent intent = buildTargetIntent(activity, uri.toString());
    Log.v("Puchase request intent");
    ContentQuery.dumpIntent(intent);
    try {
      activity.startActivityForResult(intent, RC_ONE_STEP);
      return true;
    } catch (Exception ex) {
      Log.e(ex);
      return false;
    }
  }

  private Intent buildTargetIntent(BillingActivity activity, String url) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));

    // Check if there is an application that can process the AppCoins Billing
    // flow
    PackageManager packageManager = activity.getApplicationContext().getPackageManager();
    List<ResolveInfo> appsList = packageManager
        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    for (ResolveInfo app : appsList) {
      if (app.activityInfo.packageName.equals("cm.aptoide.pt")) {
        // If there's aptoide installed always choose Aptoide as default to open
        // url
        intent.setPackage(app.activityInfo.packageName);
        break;
      } else if (app.activityInfo.packageName.equals("com.appcoins.wallet")) {
        // If Aptoide is not installed and wallet is installed then choose Wallet
        // as default to open url
        intent.setPackage(app.activityInfo.packageName);
      }
    }
    return intent;
  }


    @Override
  void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == RC_ONE_STEP) {
      if (resultCode == Activity.RESULT_OK) {
        Log.v("Purchase Result Intent");
        ContentQuery.dumpIntent(data);
      }
      return;
    }
    if (mBillingClient != null) mBillingClient.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onPurchasesUpdated(int billingResult, @Nullable List<Purchase> purchases) {

    if (Log.DEBUG) Log.v("Purchase result:" + billingResult);
    if (billingResult == ResponseCode.OK.getValue()) {
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
    String itemId = purchase.getProducts().get(0);
    if (!itemId.equals("cadpage_sub")) return;

    // Subscriptions start with sub.  Purchase date will be the actual
    // purchase date.  The fact that a subscription is being reported means that it
    // should be honored, so adjust the year to make the subscription current
    String purchaseDate = DATE_FMT.format(new Date(purchase.getPurchaseTime()));
    Calendar cal = new GregorianCalendar();
    cal.setTimeInMillis(System.currentTimeMillis());
    int iYear = cal.get(Calendar.YEAR);
    int curMonthDay = (cal.get(Calendar.MONTH)+1)*100+cal.get(Calendar.DAY_OF_MONTH);
    if (curMonthDay < Integer.parseInt(purchaseDate.substring(0,4))) iYear--;
    String year = Integer.toString(iYear);
    int subStatus = purchase.isAutoRenewing() ? 2 : 1;
    Log.v("curMonthDay="+curMonthDay+"  - " + purchaseDate.substring(0,4) + "  iYear=" + iYear);
    calc.subscription(year, purchaseDate, null, subStatus);
  }
}
