package net.anei.cadpage;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import net.anei.cadpage.donation.MainDonateEvent;
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

    // Email developer response
    Preference emailPref = findPreference(getString(R.string.pref_email_key));
    emailPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
      @Override
      public boolean onPreferenceClick(Preference preference) {
        EmailDeveloperActivity.sendGeneralEmail(getActivity());
        return true;
      }});

  }
}
