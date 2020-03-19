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

    boolean mtree;
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceLocationMenu);
    try {
      mtree = a.getBoolean(R.styleable.PreferenceLocationMenu_mtree, false);
    } finally {
      a.recycle();
    }

    Bundle args = getExtras();
    args.putBoolean("mtree",  mtree);
  }
}
