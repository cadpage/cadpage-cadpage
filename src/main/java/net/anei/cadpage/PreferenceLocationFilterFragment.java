package net.anei.cadpage;

import android.os.Bundle;

import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.parsers.MsgParser;
import net.anei.cadpage.preferences.LocationManager;

import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.TwoStatePreference;

public class PreferenceLocationFilterFragment extends PreferenceRestorableFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    Fragment parent = getTargetFragment();
    assert parent != null;
    LocationManager locMgr = ((LocationManager.Provider) parent).getLocationManager();
    MsgParser parser = locMgr.getParser();

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_location_filter, rootKey);

    EditTextPreference filterPref = findPreference(getString(R.string.pref_filter_key));
    assert filterPref != null;
    filterPref.setOnPreferenceChangeListener((pref, value) -> {
      if ("General".equals(ManagePreferences.location())) {
        DonationManager.instance().reset();
        MainDonateEvent.instance().refreshStatus();
      }
      return true;
    });

    TwoStatePreference overrideFilterPref = findPreference(getString(R.string.pref_override_filter_key));
    assert overrideFilterPref != null;
    if (parser.getFilter().length() == 0) {
      overrideFilterPref.setChecked(true);
      overrideFilterPref.setEnabled(false);
    }
    filterPref.setEnabled(overrideFilterPref.isChecked());
    overrideFilterPref.setOnPreferenceChangeListener((preference, newValue) -> {
      filterPref.setEnabled((Boolean)newValue);
      return true;
    });
    overrideFilterPref.setSummaryOff(getString(R.string.pref_override_filter_summaryoff, parser.getFilter()));
  }
}
