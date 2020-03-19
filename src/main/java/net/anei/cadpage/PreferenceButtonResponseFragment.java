package net.anei.cadpage;

import android.os.Bundle;

public class PreferenceButtonResponseFragment extends PreferenceRestorableFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_button_response, rootKey);
  }
}
