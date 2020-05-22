package net.anei.cadpage.preferences;

import android.content.Context;
import android.util.AttributeSet;

import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.R;
import net.anei.cadpage.parsers.MsgParser;

import androidx.preference.Preference;

public class OverrideLocationPreference extends Preference {
  public OverrideLocationPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    setup();
  }

  public OverrideLocationPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setup();
  }

  public OverrideLocationPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setup();
  }

  public OverrideLocationPreference(Context context) {
    super(context);
    setup();
  }

  private void setup() {
    setFragment("net.anei.cadpage.PreferenceLocationDefaultsFragment");
    setTitle(R.string.pref_loc_defaults_title);
    setSummaryProvider(preference -> {
      String city, state;
      if (ManagePreferences.overrideDefaults()) {
        city = ManagePreferences.defaultCity();
        state = ManagePreferences.defaultState();
      } else {
        MsgParser parser = ManagePreferences.getCurrentParser();
        city = parser.getDefaultCity();
        state = parser.getDefaultState();
      }
      if (city.length() == 0 && state.length() == 0) return "None";
      if (city.length() == 0) return state;
      if (state.length() == 0) return city;
      return city + ", " + state;
    });

  }
}
