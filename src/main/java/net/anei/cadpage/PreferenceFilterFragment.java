package net.anei.cadpage;

import android.os.Bundle;

public class PreferenceFilterFragment extends PreferenceRestorableFragment {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preference_filter);
  }
}
