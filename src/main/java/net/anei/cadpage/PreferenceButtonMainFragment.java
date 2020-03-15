package net.anei.cadpage;

import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

public class PreferenceButtonMainFragment extends PreferenceRestorableFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_button_main, rootKey);
  }
}
