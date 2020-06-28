package net.anei.cadpage.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.preference.Preference;

import net.anei.cadpage.R;

public class LocationSettingsPreference extends Preference {

  public LocationSettingsPreference(Context context) {
    super(context);
  }

  public LocationSettingsPreference(Context context, AttributeSet attrs) {
    super(context, attrs);

    boolean direct;
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceLocationFragment);
    try {
      direct = a.getBoolean(R.styleable.PreferenceLocationFragment_direct, false);
    } finally {
      a.recycle();
    }

    Bundle args = getExtras();
    args.putBoolean("direct",  direct);
  }
}
