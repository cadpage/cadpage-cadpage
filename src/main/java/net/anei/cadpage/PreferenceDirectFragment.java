package net.anei.cadpage;

import android.os.Bundle;

import net.anei.cadpage.donation.LocationTrackingEvent;
import net.anei.cadpage.preferences.LocationManager;
import net.anei.cadpage.vendors.VendorManager;

import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

public class PreferenceDirectFragment extends PreferenceFragment implements LocationManager.Provider {

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

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_direct, rootKey);

    // Add vendor specific preferences
    int vendorCnt = VendorManager.instance().setupPreference(requireActivity(), getPreferenceScreen());

    // Hide advanced preference options
    getPreferenceScreen().setInitialExpandedChildrenCount(vendorCnt+1);

    // Set up the location description summary
    Preference locPreference = findPreference(getString(R.string.pref_category_location_key));
    assert locPreference != null;
    locPreference.setSummaryProvider(locMgr.getSummaryProvider());
  }
}
