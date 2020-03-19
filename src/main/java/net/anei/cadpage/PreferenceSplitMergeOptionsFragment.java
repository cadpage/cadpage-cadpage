package net.anei.cadpage;

import android.os.Bundle;

public class PreferenceSplitMergeOptionsFragment extends PreferenceRestorableFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_split_merge_options, rootKey);
  }
}
