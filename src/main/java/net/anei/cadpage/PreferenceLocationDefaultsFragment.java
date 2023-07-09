package net.anei.cadpage;

import android.os.Bundle;
import android.text.InputType;

import net.anei.cadpage.parsers.MsgParser;
import net.anei.cadpage.preferences.EditTextPreference;
import net.anei.cadpage.preferences.LocationManager;

import androidx.fragment.app.Fragment;
import androidx.preference.TwoStatePreference;

public class PreferenceLocationDefaultsFragment extends PreferenceRestorableFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    Fragment parent = getTargetFragment();
    assert parent != null;
    LocationManager locMgr = ((LocationManager.Provider) parent).getLocationManager();

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_location_defaults, rootKey);

    // Have to play some games with the override default settings
    // If the override defaults is turned on, enable the default city and state items
    // If it is turned off, force the default city and state to the current parser
    // defaults and disable them.
    final TwoStatePreference overrideDefaultPref = findPreference(getString(R.string.pref_override_default_key));
    final EditTextPreference defCityPref = findPreference(getString(R.string.pref_defcity_key));
    final EditTextPreference defStatePref = findPreference(getString(R.string.pref_defstate_key));

    MsgParser parser = locMgr.getParser();
    final String parserDefCity = parser.getDefaultCity();
    final String parserDefState = parser.getDefaultState();

    assert overrideDefaultPref != null && defCityPref != null && defStatePref != null;
    defCityPref.setOnBindEditTextListener((editText) -> {
      editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
    });
    defStatePref.setOnBindEditTextListener((editText) -> {
      editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
    });

    overrideDefaultPref.setOnPreferenceChangeListener((preference, newValue) -> {
      boolean value = (Boolean) newValue;
      if (! value) {
        defCityPref.setText(parserDefCity);
        defCityPref.refreshSummary();
        defStatePref.setText(parserDefState);
        defStatePref.refreshSummary();
      }
      defCityPref.setEnabled(value);
      defStatePref.setEnabled(value);
      return true;
    });

    // Make an initial call to our preference change listener to set up the
    // correct default summary displays
    overrideDefaultPref.getOnPreferenceChangeListener().
        onPreferenceChange(overrideDefaultPref, ManagePreferences.overrideDefaults());
  }
}
