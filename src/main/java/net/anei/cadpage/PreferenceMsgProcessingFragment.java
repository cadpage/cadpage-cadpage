package net.anei.cadpage;

import android.os.Build;
import android.os.Bundle;

import androidx.preference.Preference;

public class PreferenceMsgProcessingFragment extends PreferenceFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_msg_processing, rootKey);

    // old MMS logic option only enabled if we are processing MMS messages
    Preference pref = findPreference(getString(R.string.pref_use_old_mms_key));
    assert(pref != null);
    pref.setEnabled(ManagePreferences.enableMsgType().contains("M"));

  // Disable the pass through option which is unusable starting in Kit Kat
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      deletePreference(R.string.pref_passthrusms_key);
    }
  }
}
