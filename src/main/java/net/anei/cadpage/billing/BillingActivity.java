package net.anei.cadpage.billing;

import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

public abstract class BillingActivity extends FragmentActivity {

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    BillingManager.instance().onActivityResult(requestCode, resultCode, data);
    super.onActivityResult(requestCode, resultCode, data);
  }

}