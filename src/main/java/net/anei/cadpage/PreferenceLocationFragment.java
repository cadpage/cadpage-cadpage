package net.anei.cadpage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.TwoStatePreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.parsers.ManageParsers;
import net.anei.cadpage.parsers.MsgParser;
import net.anei.cadpage.preferences.EditTextPreference;
import net.anei.cadpage.preferences.LocationManager;

public class PreferenceLocationFragment extends PreferenceRestorableFragment {

  private static final int REQ_SCANNER_CHANNEL = 1;

  private LocationManager locMgr;

  private String parserDefCity = "";
  private String parserDefState = "";
  private TwoStatePreference overrideFilterPref;
  private net.anei.cadpage.preferences.EditTextPreference filterPref;

  private TwoStatePreference overrideDefaultPref;
  private EditTextPreference defCityPref;
  private EditTextPreference defStatePref;

  private Preference scannerPref;
  private String saveLocation;

  public LocationManager getLocationManager() {
    return locMgr;
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_location, rootKey);

    // Save location so we can tell when it changes
    saveLocation = ManagePreferences.location();

    // Set up the location description summary
    Preference descPreference = findPreference(getString(R.string.pref_loc_desc_key));
    locMgr = new LocationManager(descPreference);
    locMgr.updateDisplay();

    // The location, filter override checkbox, and filter edit box have a complex
    // relationship.  The override checkbox is enabled only when the location parser
    // has a default parser to override.  If it doesn't then it is disabled by forced
    // to true.  The filter is enabled when the override box is checked, whether it
    // is enabled or not.  We have to do this ourselves because the Android dependency
    // logic considers the value to be false if it isn't enabled.

    // On top of all that, the general alert box is enabled only if the current
    // parser has a default filter OR a user filter has been specified

    filterPref = findPreference(getString(R.string.pref_filter_key));
    assert filterPref != null;
    filterPref.setOnPreferenceChangeListener((pref2, value) -> {
      if ("General".equals(saveLocation)) {
        DonationManager.instance().reset();
        MainDonateEvent.instance().refreshStatus();
      }
      return true;
    });

    overrideFilterPref = findPreference(getString(R.string.pref_override_filter_key));
    assert overrideFilterPref != null;
    filterPref.setEnabled(overrideFilterPref.isChecked());
    overrideFilterPref.setOnPreferenceChangeListener((preference, newValue) -> {
      filterPref.setEnabled((Boolean)newValue);
      return true;
    });

    adjustLocationChange(ManagePreferences.location(), false);
    locMgr.setOnPreferenceChangeListener((preference, newValue) -> {
      adjustLocationChange((String)newValue, true);
      return true;
    });

    // Have to play some games with the override default settings
    // If the override defaults is turned on, enable the default city and state items
    // If it is turned off, force the default city and state to the current parser
    // defaults and disable them.
    overrideDefaultPref = findPreference(getString(R.string.pref_override_default_key));
    defCityPref = findPreference(getString(R.string.pref_defcity_key));
    defStatePref = findPreference(getString(R.string.pref_defstate_key));

    overrideDefaultPref = findPreference(getString(R.string.pref_override_default_key));
    assert overrideDefaultPref != null;
    overrideDefaultPref.setOnPreferenceChangeListener((preference, newValue) -> {
      boolean value = (Boolean) newValue;
      if (! value) {
        defCityPref.setText(parserDefCity);
        defCityPref.refreshSummary();
        defStatePref.setText(parserDefState);
        defStatePref.refreshSummary();
      }
      defCityPref.setEnabled(value);
      defStatePref.setEnabled(value);
      return true;
    });

    // Make an initial call to our preference change listener to set up the
    // correct default summary displays
    overrideDefaultPref.getOnPreferenceChangeListener().
        onPreferenceChange(overrideDefaultPref, ManagePreferences.overrideDefaults());

    // Set up Scanner channel selection preference
    scannerPref = findPreference(getString(R.string.pref_scanner_channel_key));
    if (scannerPref != null) {
      String channel = ManagePreferences.scannerChannel();
      scannerPref.setSummary(channel);
      scannerPref.setOnPreferenceClickListener(pref1 -> {

        // When clicked, ask the scanner app to select a favorite channel
        Intent intent = new Intent("com.scannerradio.intent.action.ACTION_PICK");
        try {
          startActivityForResult(intent, REQ_SCANNER_CHANNEL);
        } catch (Exception ex) {

          if (! (ex instanceof ActivityNotFoundException)) Log.e(ex);

          // Scanner radio either isn't installed, or isn't responding to the ACTION_PICK
          // request.  Check the package manager to which, if any, are currently installed
          Activity activity = getActivity();
          assert activity != null;
          PackageManager pkgMgr = activity.getPackageManager();
          String pkgName = "com.scannerradio_pro";
          boolean installed = false;
          try {
            pkgMgr.getPackageInfo(pkgName, 0);
            installed = true;
          } catch (PackageManager.NameNotFoundException ignored) {}
          if (! installed) {
            pkgName = "com.scannerradio";
            try {
              pkgMgr.getPackageInfo(pkgName, 0);
              installed = true;
            } catch (PackageManager.NameNotFoundException ignored) {}
          }

          // OK, show a dialog box asking if they want to install Scanner Radio
          final String pkgName2 = pkgName;
          new AlertDialog.Builder(getActivity())
              .setMessage(installed ? R.string.scanner_not_current : R.string.scanner_not_installed)
              .setPositiveButton(R.string.donate_btn_yes, (dialog, which) -> {
                Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkgName2));
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                  getActivity().startActivity(intent1);
                } catch (ActivityNotFoundException ex1) {
                  Log.e(ex1);
                }
              })
              .setNegativeButton(R.string.donate_btn_no, null)
              .create().show();

        }
        return true;
      });
    }
  }

  @Override
  public void onPause() {
    Log.v("PreferenceLocationFragment.onPause()");
    super.onPause();
  }

  /**
   * Make any necessary adjustments necessary
   * when the location preference is changed
   * @param location new location preference value
   * @param change true if location value has been changed
   */
  private void adjustLocationChange(String location, boolean change) {

    // If location changes, recalculate the donation status
    if (!location.equals(saveLocation)) {
      saveLocation = location;
      DonationManager.instance().reset();
      MainDonateEvent.instance().refreshStatus();
    }

    // Get the parser and see if it has a default filter
    // Save it in parserFilter so other preferences know what it is
    MsgParser parser = ManageParsers.getInstance().getParser(location);
    String parserFilter = parser.getFilter();
    parserDefCity = parser.getDefaultCity();
    parserDefState = parser.getDefaultState();

    // If the parser has a filter, enable the override checkbox, set its value to true
    // And insert the default filter value in the summary off message
    // And unilaterally enable the general alert box
    if (parserFilter.length() > 0) {
      overrideFilterPref.setEnabled(true);
      if (change) overrideFilterPref.setChecked(false);
      overrideFilterPref.setSummaryOff(getString(R.string.pref_override_filter_summaryoff, parserFilter));
      filterPref.setEnabled(overrideFilterPref.isChecked());
    }

    // If there is no parser filter, the override box is disabled but forced to true
    // the general alert box is enabled only if the user filter
    else {
      overrideFilterPref.setEnabled(false);
      overrideFilterPref.setChecked(true);
      filterPref.setEnabled(true);
    }

    // Any time the location parser changes, reset the override default loc setting
    // OK, its a little more complicated than that.  If the override default setting
    // is on, we can just turn it off and let the onPreferenceChange listener take
    // care of things.  If it is already off, we have to call the onPreferenceChange
    // listener ourselves so it will update the property summary displays
    // If that isn't complicated enough, the overrideDefaultPref setting won't
    // be initialized the first time we are called, but that first call has the
    // change parameter set to false, so this logic gets skipped.
    if (change) {
      if (overrideDefaultPref.isChecked()) {
        overrideDefaultPref.setChecked(false);
      } else {
        overrideDefaultPref.getOnPreferenceChangeListener().onPreferenceChange(overrideDefaultPref, false);
      }
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

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (requestCode == REQ_SCANNER_CHANNEL) {
      if (resultCode != Activity.RESULT_OK || data == null) return;
      Log.v("onActivityResult()");
      ContentQuery.dumpIntent(data);
      String description = data.getStringExtra("description");
      Intent scanIntent = data.getParcelableExtra("playIntent");
      if (description == null || scanIntent == null) return;
      ContentQuery.dumpIntent(scanIntent);

      ManagePreferences.setScannerChannel(description);
      scannerPref.setSummary(description);
      ManagePreferences.setScannerIntent(scanIntent);
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

}
