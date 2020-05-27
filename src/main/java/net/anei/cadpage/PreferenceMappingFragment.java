package net.anei.cadpage;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import androidx.preference.ListPreference;

public class PreferenceMappingFragment extends PreferenceFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_mapping, rootKey);

    // mapping app preference only includes entries for ArcGIS Navigator and Waze that we want to remove
    // if the corresponding app is not installed on this device.  If the current value is set to
    // a value that is removed from the list, default it back to "Google"
    ListPreference appMapPref = findPreference(getString(R.string.pref_app_map_option_key));
    assert appMapPref != null;
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
            Activity activity = getActivity();
            assert(activity != null);
            activity.getPackageManager().getPackageInfo(pkg, 0);
            good = true;
            Log.v("package found");
            break;
          } catch (PackageManager.NameNotFoundException ignored) {}
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
  }
}
