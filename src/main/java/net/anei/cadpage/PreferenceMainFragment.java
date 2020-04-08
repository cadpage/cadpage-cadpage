package net.anei.cadpage;

import android.content.Context;
import android.os.Bundle;

import net.anei.cadpage.donation.DeveloperToolsManager;
import net.anei.cadpage.donation.MainDonateEvent;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

public class PreferenceMainFragment extends PreferenceFragment {

  private TwoStatePreference mEnabledPreference;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_main, rootKey);

    // Set up the payment status tracking screens
    Preference donate = findPreference(getString(R.string.pref_payment_status_key));
    MainDonateEvent.instance().setPreference(getActivity(), donate);

    // Save specific preferences we might need later
    mEnabledPreference = findPreference(getString(R.string.pref_enabled_key));

    // Add necessary permission checks
    Preference pref = findPreference(getString(R.string.pref_enable_msg_type_key));
    assert pref != null;
    pref.setOnPreferenceChangeListener((preference, newValue) -> ManagePreferences.checkPermEnableMsgType((ListPreference)preference, (String)newValue));

    // Add developer dialog preference if appropriate
    DeveloperToolsManager.instance().addPreference(getActivity(), getPreferenceScreen());
  }

  @Override
  public void onResume() {
    super.onResume();

    // Check for changes to values that are accessible from the widget
    mEnabledPreference.setChecked(ManagePreferences.enabled());
  }

  @Override
  public void onDestroy() {
    MainDonateEvent.instance().setPreference(null, null);
    super.onDestroy();
  }

  /**
   * initialize all uninitialized preferences
   * @param context current context
   */
  public static void initializePreferences(Context context) {
    for (int prefId : PREF_ID_LIST) {
      PreferenceManager.setDefaultValues(context, prefId, true);
    }
  }

  private static final int[] PREF_ID_LIST = new int[]{
    R.xml.preference_main,
    R.xml.preference_general,
    R.xml.preference_notification_old,
    R.xml.preference_additional,
    R.xml.preference_button,
    R.xml.preference_button_main,
    R.xml.preference_button_response,
    R.xml.preference_filter,
    R.xml.preference_location,
    R.xml.preference_split_merge_options,
    R.xml.preference_direct,
    R.xml.preference_other_info
  };
}
