package net.anei.cadpage.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.preference.Preference;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;

public class ResponseBtnPreference extends Preference {

  private int button = -1;

  public ResponseBtnPreference(Context context) {
    super(context);
  }

  public ResponseBtnPreference(Context context, AttributeSet attrs) {
    super(context, attrs);

    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceButtonResponseBtn);
    try {
      button = a.getInt(R.styleable.PreferenceButtonResponseBtn_button, -1);
    } finally {
      a.recycle();
    }

    Bundle args = getExtras();
    args.putInt("button", button);

    setSummaryProvider(preference -> ManagePreferences.callbackButtonTitle(button));
  }
}
