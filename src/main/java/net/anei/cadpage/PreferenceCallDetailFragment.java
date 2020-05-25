package net.anei.cadpage;

import android.os.Build;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

public class PreferenceCallDetailFragment extends PreferenceFragment {

  private TwoStatePreference mPopupEnabledPreference;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_call_detail, rootKey);

    // Save specific preferences we might need later
    mPopupEnabledPreference = findPreference(getString(R.string.pref_popup_enabled_key));

    // The no show in call preference has no meaning in Android 10
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      deletePreference(R.string.pref_noShowInCall_key);
    }
    // If it is still there, it requires the READ_PHONE_STATE permission
    else {
      Preference pref = findPreference(getString(R.string.pref_noShowInCall_key));
      assert pref != null;
      pref.setOnPreferenceChangeListener((preference, newValue) -> ManagePreferences.checkNoShowInCall((TwoStatePreference) preference, (Boolean) newValue));
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    // Check for changes to values that are accessable from the widget
    mPopupEnabledPreference.setChecked(ManagePreferences.popupEnabled());
  }
}
