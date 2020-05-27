package net.anei.cadpage;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.parsers.MsgParser;
import net.anei.cadpage.preferences.LocationManager;

public class PreferenceLocationFragment extends PreferenceRestorableFragment implements LocationManager.Provider {

  private LocationManager locMgr;

  private String saveLocation;

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

    // Save location so we can tell when it changes
    saveLocation = ManagePreferences.location();

    // Set up the location description summary
    Preference descPreference = findPreference(getString(R.string.pref_loc_desc_key));
    assert descPreference != null;
    descPreference.setSummaryProvider(locMgr.getSummaryProvider());

    // And setup a location change preference listener
    locMgr.setOnPreferenceChangeListener((preference, newValue) -> {
      adjustLocationChange((String)newValue);
      return true;
    });

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

  /**
   * Make any necessary adjustments necessary
   * when the location preference is changed
   * @param location new location preference value
   */
  private void adjustLocationChange(String location) {

    // If location changes, reset the filter and default city/state settings
    if (!location.equals(saveLocation)) {
      saveLocation = location;

      MsgParser parser = locMgr.getParser();
      ManagePreferences.setOverrideFilter(parser.getFilter().length() == 0);
      ManagePreferences.setFilter(parser.getFilter());
      ManagePreferences.setOverrideDefaults(false);
      ManagePreferences.setDefaultCity(parser.getDefaultCity());
      ManagePreferences.setDefaultState(parser.getDefaultState());

      // And recalculate payment status
      DonationManager.instance().reset();
      MainDonateEvent.instance().refreshStatus();
    }
  }

  // If location code changes during this session, force a rebuild of
  // the call history data on the off chance that a general format message
  // can use the new location code.
  private String oldLocation = null;

  @Override
  public void onStart() {

    // Save the setting that might be important if they change
    oldLocation = ManagePreferences.location();

    super.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();

    String location = ManagePreferences.location();
    if (!location.equals(oldLocation)) {
      SmsMessageQueue.getInstance().reparseGeneral();
    }
  }
}
