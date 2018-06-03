package net.anei.cadpage;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.BaseAdapter;

import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.preferences.EditTextPreference;

public class PreferenceButtonFragment extends PreferenceFragment {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preference_button);

    // Set up the response button preferences
    PreferenceScreen parent = (PreferenceScreen)findPreference(getString(R.string.pref_resp_button_config_key));
    setupResponseButtonConfig(parent, 1, R.string.pref_callback1_screen_key, R.string.pref_callback1_type_key, R.string.pref_callback1_title_key, R.string.pref_callback1_key);
    setupResponseButtonConfig(parent, 2, R.string.pref_callback2_screen_key, R.string.pref_callback2_type_key, R.string.pref_callback2_title_key, R.string.pref_callback2_key);
    setupResponseButtonConfig(parent, 3, R.string.pref_callback3_screen_key, R.string.pref_callback3_type_key, R.string.pref_callback3_title_key, R.string.pref_callback3_key);
    setupResponseButtonConfig(parent, 4, R.string.pref_callback4_screen_key, R.string.pref_callback4_type_key, R.string.pref_callback4_title_key, R.string.pref_callback4_key);
    setupResponseButtonConfig(parent, 5, R.string.pref_callback5_screen_key, R.string.pref_callback5_type_key, R.string.pref_callback5_title_key, R.string.pref_callback5_key);
    setupResponseButtonConfig(parent, 6, R.string.pref_callback6_screen_key, R.string.pref_callback6_type_key, R.string.pref_callback6_title_key, R.string.pref_callback6_key);
  }

  /**
   * Setup correlations between the different response button preferences
   * @param screenResId ID of response button screen preference
   * @param typeResId ID of response button type preference
   * @param descResId ID of response button description preference
   * @param codeResId ID of response button phone/code preference
   */
  private void setupResponseButtonConfig(final PreferenceScreen parent, final int button,
                                         int screenResId, int typeResId, int descResId, int codeResId) {

    // Find all of the preferences
    final PreferenceScreen screenPref = (PreferenceScreen)findPreference(getString(screenResId));
    final ListPreference typePref = (ListPreference)findPreference(getString(typeResId));
    final EditTextPreference descPref = (EditTextPreference)findPreference(getString(descResId));
    final EditTextPreference codePref = (EditTextPreference)findPreference(getString(codeResId));

    // Lock screen summary to value of description preference
    screenPref.setSummary(descPref.getText());
    descPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        screenPref.setSummary(newValue.toString());

        // Required to actually force display change :(
        ((BaseAdapter)parent.getRootAdapter()).notifyDataSetChanged();
        return true;
      }
    });

    // Code field is only enabled if response type is set to something
    codePref.setEnabled(typePref.getValue().length() > 0);
    typePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object value) {

        // Check if we have appropriate permission
        if (!ManagePreferences.checkResponseType(button, (ListPreference)preference, value.toString())) return false;

        codePref.setEnabled(value.toString().length() > 0);
        return true;
      }
    });
  }

}
