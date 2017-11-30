package net.anei.cadpage.preferences;

import android.content.Context;
import android.util.AttributeSet;

import net.anei.cadpage.R;

public class ScannerTimeoutPreference extends EditTextPreference {
  public ScannerTimeoutPreference(Context context) {
    super(context);
  }

  public ScannerTimeoutPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public String translateValue(String value) {
    if (value.length() == 0 || Integer.parseInt(value) == 0) {
      return getContext().getString(R.string.pref_scanner_timeout_value_zero);
    } else {
      return getContext().getString(R.string.pref_scanner_timeout_value_other, value);
    }
  }
}
