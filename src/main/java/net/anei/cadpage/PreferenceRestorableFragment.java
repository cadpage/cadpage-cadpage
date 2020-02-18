package net.anei.cadpage;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.TwoStatePreference;

import net.anei.cadpage.preferences.ListPreference;

import androidx.annotation.Nullable;

/**
 * PreferenceFragment subclass that adds the ability to restore a the visible preference status
 * of a preference whose value has been changed without it's knowledge
 */
public class PreferenceRestorableFragment extends PreferenceFragment {

  private static PreferenceRestorableFragment curInstance = null;
  private static String curPrefKey = null;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    curInstance = this;
    curPrefKey = null;
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onDestroy() {
    curInstance = null;
    super.onDestroy();
  }

  /**
   * Restore the visible value of a preference if it is displayed by this fragment
   */
  private void doRestorePreferenceValue() {
    if (curPrefKey == null) return;
    Preference pref = findPreference(curPrefKey);
    if (pref == null) return;
    SharedPreferences prefs = pref.getSharedPreferences();
    if (prefs == null) return;

    if (pref instanceof ListPreference) {
      ((ListPreference)pref).setValue(prefs.getString(curPrefKey, ""));
    } else if (pref instanceof TwoStatePreference) {
      ((TwoStatePreference)pref).setChecked(prefs.getBoolean(curPrefKey, false));
    } else {
      throw new RuntimeException("showPreferenceValue() not supported for " + curPrefKey + ':' + pref.getClass().getName());
    }
  }

  public static void setPreferenceKey(String prefKey) {
    Log.v("PreferenceRestoreableFragment.setPreferenceKey(" + prefKey + ")");
    curPrefKey = prefKey;
  }

  /**
   * Restore the visible value of the current preference if it is displayed by this fragment
   */
  public static void restorePreferenceValue() {
    if (curInstance == null) return;
    curInstance.doRestorePreferenceValue();
  }
}
