package net.anei.cadpage;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.TwoStatePreference;

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
    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        return ManagePreferences.checkReportPosition((ListPreference) preference, (String) newValue);
      }
    });

    // Disable the pass through option which is unusable starting in Kit Kat
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      pref = findPreference(getString(R.string.pref_passthrusms_key));
      deletePreference(pref);
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
            getActivity().getPackageManager().getPackageInfo(pkgName, 0);
            good = true;
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
    appMapPref.setEntries(appMapEntryList.toArray(new String[appMapEntryList.size()]));
    appMapPref.setEntryValues(appMapValueList.toArray(new String[appMapValueList.size()]));

    // The No Show In Call preference requires the READ_PHONE_STATE permission
    pref = findPreference(getString(R.string.pref_noShowInCall_key));
    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        return ManagePreferences.checkNoShowInCall((TwoStatePreference)preference, (Boolean)newValue);
      }
    });
  }

  /**
   * Remove preference from preference tree
   * @param pref Preference to be removed
   */
  private void deletePreference(Preference pref) {
    PreferenceGroup parent = findParent(getPreferenceScreen(), pref);
    if (parent != null) parent.removePreference(pref);
  }

  /**
   * Find parent of preference in preference tree
   * @param root - root of preference tree
   * @param pref - Preference
   */
  private PreferenceGroup findParent(PreferenceGroup root, Preference pref) {
    for (int ndx = 0; ndx < root.getPreferenceCount(); ndx++) {
      Preference child = root.getPreference(ndx);
      if (child == pref) return root;
      if (child instanceof PreferenceGroup) {
        PreferenceGroup parent = findParent((PreferenceGroup)child, pref);
        if (parent != null) return parent;
      }
    }
    return null;
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
