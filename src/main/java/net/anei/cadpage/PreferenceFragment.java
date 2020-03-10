package net.anei.cadpage;

import android.content.Context;

import org.spongycastle.util.Arrays;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

/**
 * Base class for preference fragment classes that need to delete unneeded
 * preferences from the preference tree
 */
abstract class PreferenceFragment extends PreferenceFragmentCompat {

  void deletePreference(int resId) {
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

  abstract protected int getPreferenceId();

  public PreferenceFragment() {
    if (!Arrays.contains(PREF_ID_LIST, getPreferenceId())) {

    }
  }

  /**
   * initialize all uninitialized preferences
   * @param context current context
   */
  public static void initializePreferences(Context context) {
    for (int prefId : PREF_ID_LIST) {
      PreferenceManager.setDefaultValues(context, prefId, true);
    }
  }

  private static final int[] PREF_ID_LIST = new int[]{
    R.xml.preference_general,
    R.xml.preference_notification_old,
    R.xml.preference_additional,
    R.xml.preference_button,
    R.xml.preference_filter,
    R.xml.preference_location,
    R.xml.preference_direct,
    R.xml.preference_other_info
  };

}
