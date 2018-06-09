package net.anei.cadpage;

import android.os.Bundle;
import android.preference.TwoStatePreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import net.anei.cadpage.donation.DeveloperToolsManager;
import net.anei.cadpage.donation.DonateActivity;
import net.anei.cadpage.donation.EnableEmailAccessEvent;
import net.anei.cadpage.donation.MainDonateEvent;

public class PreferenceGeneralFragment extends PreferenceFragment {

  private TwoStatePreference mEnabledPreference;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preference_general);

    // Save specific preferences we might need later
    mEnabledPreference = (TwoStatePreference) findPreference(getString(R.string.pref_enabled_key));

    // Set up the payment status tracking screens
    Preference donate = findPreference(getString(R.string.pref_payment_status_key));
    MainDonateEvent.instance().setPreference(getActivity(), donate);

    Preference pref = findPreference(getString(R.string.pref_grant_account_access_key));
    MainDonateEvent.instance().setGrantAccountPref((TwoStatePreference)pref);
    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!(Boolean)newValue) return true;
        DonateActivity.launchActivity(getActivity(), EnableEmailAccessEvent.instance(), null);
        return false;
      }
    });

    // Add developer dialog preference if appropriate
    DeveloperToolsManager.instance().addPreference(getActivity(), getPreferenceScreen());
  }

  @Override
  public void onResume() {
    super.onResume();

    // Check for changes to values that are accessable from the widget
    mEnabledPreference.setChecked(ManagePreferences.enabled());
  }

  @Override
  public void onDestroy() {
    MainDonateEvent.instance().setPreference(null, null);
    MainDonateEvent.instance().setGrantAccountPref(null);
    super.onDestroy();
  }
}
