package net.anei.cadpage;

import android.os.Build;
import android.os.Bundle;

public class PreferenceScreenControlFragment extends PreferenceFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_screen_control, rootKey);
  }
}
