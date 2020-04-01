package net.anei.cadpage;

import android.content.Context;

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
}
