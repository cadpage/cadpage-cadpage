package net.anei.cadpage.preferences;

import android.content.Context;
import android.util.AttributeSet;

import net.anei.cadpage.donation.BillingAccountEvent;

public class BillingAccountPreference extends EditTextPreference {
  public BillingAccountPreference(Context context) {
    super(context);
  }

  public BillingAccountPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onClick() {
    BillingAccountEvent.instance().launch(getContext());
  }

  @Override
  public String translateValue(String value) {
    if (value.length() == 0) value = "not specified";
    return value;
  }
}
