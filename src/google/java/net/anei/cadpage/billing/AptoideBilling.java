package net.anei.cadpage.billing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

@SuppressWarnings({"SpellCheckingInspection"})
class AptoideBilling extends Billing {

  @Override
  public void initialize(Context context) {}

  @Override
  public void initialize(Activity activity) {}

  @Override
  public void destroy() {}

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  void doConnect() {}

  @Override
  void doRestoreTransactions(Context context) {}

  @Override
  void doStartPurchase(BillingActivity activity) {
    throw new RuntimeException("Aptoide purchase requested in Google Play Store version");
  }

  @Override
  void onActivityResult(int requestCode, int resultCode, Intent data) {}
}
