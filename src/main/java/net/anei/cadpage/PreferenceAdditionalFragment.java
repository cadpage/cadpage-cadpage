package net.anei.cadpage;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.TwoStatePreference;

import net.anei.cadpage.donation.CheckPopupEvent;

import java.util.ArrayList;
import java.util.List;

public class PreferenceAdditionalFragment extends PreferenceFragment {

  private TwoStatePreference mPopupEnabledPreference;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preference_additional);

    // Save specific preferences we might need later
    mPopupEnabledPreference = (TwoStatePreference) findPreference(getString(R.string.pref_popup_enabled_key));

    Preference pref = findPreference(getString(R.string.pref_report_position_key));
    pref.setOnPreferenceChangeListener((preference, newValue) -> ManagePreferences.checkReportPosition((ListPreference) preference, (String) newValue));

    // Disable the pass through option which is unusable starting in Kit Kat
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      deletePreference(R.string.pref_passthrusms_key);
    }

    // Disable the screen control options which are ignored starting in Android 10
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      deletePreference(R.string.pref_timeout_key);
      deletePreference(R.string.pref_screen_on_key);
      deletePreference(R.string.pref_dimscreen_key);
    }

    // mapping app preference only includes entries for ArcGIS Navigator and Waze that we want to remove
    // if the corresponding app is not installed on this device.  If the current value is set to
    // a value that is removed from the list, default it back to "Google"
    ListPreference appMapPref = (ListPreference)findPreference(getString(R.string.pref_app_map_option_key));
    String oldVal = appMapPref.getValue();
    CharSequence[] appMapEntries = appMapPref.getEntries();
    CharSequence[] appMapValues = appMapPref.getEntryValues();
    List<String> appMapEntryList = new ArrayList<>();
    List<String> appMapValueList = new ArrayList<>();
    for (int ndx = 0; ndx < appMapValues.length; ndx++) {
      String value = appMapValues[ndx].toString();
      String pkgName = (value.equals("ArcGIS Navigator") ? "com.esri.navigator" :
                        value.equals("Waze") ? "com.waze" :
                        value.equals("OsmAnd") ? "net.osmand.plus,net.osmand" : null);
      if (pkgName != null) {
        boolean good = false;
        for (String pkg : pkgName.split(",")) {
          try {
            Log.v("Check package " + pkg);
            getActivity().getPackageManager().getPackageInfo(pkg, 0);
            good = true;
            Log.v("package found");
            break;
          } catch (PackageManager.NameNotFoundException ex2) {}
        }
        if (!good) {
          if (value.equals(oldVal)) appMapPref.setValue("Google");
          continue;
        }
      }
      appMapValueList.add(value);
      appMapEntryList.add(appMapEntries[ndx].toString());
    }
    appMapPref.setEntries(appMapEntryList.toArray(new String[0]));
    appMapPref.setEntryValues(appMapValueList.toArray(new String[0]));

    // And add a permission check for the OSM And option
    appMapPref.setOnPreferenceChangeListener((preference, newValue) -> ManagePreferences.checkAppMapOption((ListPreference) preference, (String) newValue));

    // The No Show In Call preference requires the READ_PHONE_STATE permission
    pref = findPreference(getString(R.string.pref_noShowInCall_key));
    pref.setOnPreferenceChangeListener((preference, newValue) -> ManagePreferences.checkNoShowInCall((TwoStatePreference)preference, (Boolean)newValue));
  }

  private void deletePreference(int resId) {
    String prefKey = getString(resId);
    deletePreference(getPreferenceScreen(), prefKey);
  }

  private boolean deletePreference(PreferenceGroup root, String prefKey) {
    for (int ndx = 0; ndx < root.getPreferenceCount(); ndx++) {
      Preference child = root.getPreference(ndx);
      if (child instanceof PreferenceGroup) {
        if (deletePreference((PreferenceGroup)child, prefKey)) return true;
      }
      else if (prefKey.equals(child.getKey())) {
        root.removePreference(child);
        return true;
      }
    }
    return false;
  }

  // Save display text size so we can tell if it changed
  private String oldTextSize =null;

  @Override
  public void onStart() {
    oldTextSize = ManagePreferences.textSize();
    super.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();

    String textSize = ManagePreferences.textSize();
    if (! textSize.equals(oldTextSize)) {
      SmsMessageQueue.getInstance().notifyDataChange();
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    // Check for changes to values that are accessable from the widget
    mPopupEnabledPreference.setChecked(ManagePreferences.popupEnabled());
  }
}
