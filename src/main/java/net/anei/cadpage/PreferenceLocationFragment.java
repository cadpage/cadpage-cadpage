package net.anei.cadpage;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import net.anei.cadpage.preferences.LocationManager;

public class PreferenceLocationFragment extends PreferenceRestorableFragment implements LocationManager.Provider {

  private LocationManager locMgr;

  @Override
  public LocationManager getLocationManager() {
    return locMgr;
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    Fragment parent = getTargetFragment();
    locMgr =
      parent instanceof LocationManager.Provider
        ? ((LocationManager.Provider) parent).getLocationManager()
        : new LocationManager();

    Bundle args = getArguments();
    boolean direct = args != null && args.getBoolean("direct");

    // Load the preferences from an XML resource
    setPreferencesFromResource(direct ? R.xml.preference_direct_location : R.xml.preference_location, rootKey);

    // Set up the location description summary
    Preference descPreference = findPreference(getString(R.string.pref_loc_desc_key));
    assert descPreference != null;
    descPreference.setSummaryProvider(locMgr.getSummaryProvider());

    if (!direct) {
      Preference filterPref = findPreference(getString(R.string.pref_loc_filter_key));
      assert filterPref != null;
      filterPref.setSummaryProvider(preference -> {
        String result = ManagePreferences.overrideFilter()
          ? ManagePreferences.filter()
          : locMgr.getParser().getFilter();
        if (result.length() == 0) result = "Disabled";
        return result;
      });
    }
  }
}
