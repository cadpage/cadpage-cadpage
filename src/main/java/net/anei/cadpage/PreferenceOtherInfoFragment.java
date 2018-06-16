package net.anei.cadpage;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import net.anei.cadpage.preferences.DialogPreference;

public class PreferenceOtherInfoFragment extends PreferenceFragment {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preference_other_info);

    // Set the version number in the about dialog preference
    final DialogPreference aboutPref =
        (DialogPreference) findPreference(getString(R.string.pref_about_key));
    aboutPref.setDialogTitle(CadPageApplication.getNameVersion());
    aboutPref.setDialogLayoutResource(R.layout.about);

  }
}
