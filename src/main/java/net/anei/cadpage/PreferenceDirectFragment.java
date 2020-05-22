package net.anei.cadpage;

import android.os.Bundle;

import net.anei.cadpage.preferences.LocationManager;
import net.anei.cadpage.vendors.VendorManager;

import java.util.Objects;

import androidx.fragment.app.Fragment;
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

    // Set up C2DM vendor preference screen
    VendorManager.instance().setupPreference(Objects.requireNonNull(getActivity()), getPreferenceScreen());
    int vendorCnt = getPreferenceScreen().getPreferenceCount();

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preference_direct);

    // Set up the location description summary
    Preference locPreference = findPreference(getString(R.string.pref_category_location_key));
    locPreference.setSummaryProvider(locMgr.getSummaryProvider());

    getPreferenceScreen().setInitialExpandedChildrenCount(vendorCnt+1);
  }
}
