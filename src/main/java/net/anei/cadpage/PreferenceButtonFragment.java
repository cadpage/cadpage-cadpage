package net.anei.cadpage;

import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import androidx.preference.EditTextPreference;

public class PreferenceButtonFragment extends PreferenceRestorableFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_button, rootKey);

    // Set up the response button preferences
    setupResponseButtonConfig(1, R.string.pref_callback1_screen_key, R.string.pref_callback1_type_key, R.string.pref_callback1_title_key, R.string.pref_callback1_key);
    setupResponseButtonConfig(2, R.string.pref_callback2_screen_key, R.string.pref_callback2_type_key, R.string.pref_callback2_title_key, R.string.pref_callback2_key);
    setupResponseButtonConfig(3, R.string.pref_callback3_screen_key, R.string.pref_callback3_type_key, R.string.pref_callback3_title_key, R.string.pref_callback3_key);
    setupResponseButtonConfig(4, R.string.pref_callback4_screen_key, R.string.pref_callback4_type_key, R.string.pref_callback4_title_key, R.string.pref_callback4_key);
    setupResponseButtonConfig(5, R.string.pref_callback5_screen_key, R.string.pref_callback5_type_key, R.string.pref_callback5_title_key, R.string.pref_callback5_key);
    setupResponseButtonConfig(6, R.string.pref_callback6_screen_key, R.string.pref_callback6_type_key, R.string.pref_callback6_title_key, R.string.pref_callback6_key);
  }

  /**
   * Setup correlations between the different response button preferences
   * @param screenResId ID of response button screen preference
   * @param typeResId ID of response button type preference
   * @param descResId ID of response button description preference
   * @param codeResId ID of response button phone/code preference
   */
  private void setupResponseButtonConfig(final int button,
                                         int screenResId, int typeResId, int descResId, int codeResId) {

    // Find all of the preferences
    final PreferenceScreen screenPref = findPreference(getString(screenResId));
    final ListPreference typePref = findPreference(getString(typeResId));
    final EditTextPreference descPref = findPreference(getString(descResId));
    final EditTextPreference codePref = findPreference(getString(codeResId));

    // Lock screen summary to value of description preference
    assert screenPref != null;
    assert descPref != null;
    screenPref.setSummary(descPref.getText());
    descPref.setOnPreferenceChangeListener((preference, newValue) -> {
      screenPref.setSummary(newValue.toString());
      return true;
    });

    // Code field is only enabled if response type is set to something
    assert typePref != null;
    assert codePref != null;
    codePref.setEnabled(typePref.getValue().length() > 0);
    typePref.setOnPreferenceChangeListener((preference, value) -> {

      // Check if we have appropriate permission
      if (!ManagePreferences.checkResponseType(button, (ListPreference)preference, value.toString())) return false;

      codePref.setEnabled(value.toString().length() > 0);
      return true;
    });
  }
}
