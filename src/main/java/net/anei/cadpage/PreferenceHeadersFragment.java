package net.anei.cadpage;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

public class PreferenceHeadersFragment extends PreferenceFragment {
  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_headers, rootKey);
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
    R.xml.preference_headers,
    R.xml.preference_general,
    R.xml.preference_notification_old,
    R.xml.preference_additional,
    R.xml.preference_button,
    R.xml.preference_button_main,
    R.xml.preference_button_response,
    R.xml.preference_filter,
    R.xml.preference_location,
    R.xml.preference_split_merge_options,
    R.xml.preference_direct,
    R.xml.preference_other_info
  };
}
