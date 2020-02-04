package net.anei.cadpage;

import android.os.Bundle;

public class PreferenceFilterFragment extends PreferenceFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_filter, rootKey);
  }
}
