package net.anei.cadpage;

import android.os.Bundle;

import net.anei.cadpage.preferences.DialogPreference;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

public class PreferenceOtherInfoFragment extends PreferenceFragment {
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

  private static final String DIALOG_FRAGMENT_TAG = "DialogPreference";

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    FragmentManager fragMgr = getParentFragmentManager();
    if (fragMgr.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) return;

    if (preference instanceof DialogPreference) {
      final PreferenceDialogFragment f = PreferenceDialogFragment.newInstance(preference.getKey());
      f.setTargetFragment(this, 0);
      f.show(fragMgr, DIALOG_FRAGMENT_TAG);
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }
}
