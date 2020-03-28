package net.anei.cadpage.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;

import net.anei.cadpage.R;

import androidx.preference.Preference;

public class LocationMenuPreference extends Preference {

  public LocationMenuPreference(Context context) {
    super(context);
  }

  public LocationMenuPreference(Context context, AttributeSet attrs) {
    super(context, attrs);

    boolean multi;
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceLocationMenu);
    try {
      multi = a.getBoolean(R.styleable.PreferenceLocationMenu_multi, false);
    } finally {
      a.recycle();
    }

    Bundle args = getExtras();
    args.putBoolean("multi",  multi);
  }
}
