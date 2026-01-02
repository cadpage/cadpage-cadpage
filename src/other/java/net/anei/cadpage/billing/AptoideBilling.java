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
  void doStartPurchase(Activity activity) {
    throw new RuntimeException("Aptoide billing is not currently implemented");
  }
}
