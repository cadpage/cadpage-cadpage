package net.anei.cadpage;

import android.os.Bundle;

import net.anei.cadpage.preferences.DialogPreference;

public class PreferenceOtherInfoFragment extends PreferenceRestorableFragment {
  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_other_info, rootKey);

    // Set the version number in the about dialog preference
    final DialogPreference aboutPref = findPreference(getString(R.string.pref_about_key));
    assert aboutPref != null;
    aboutPref.setDialogTitle(CadPageApplication.getNameVersion());
    aboutPref.setDialogLayoutResource(R.layout.about);

  }
}
