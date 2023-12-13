package net.anei.cadpage;

import android.os.Build;
import android.os.Bundle;

import androidx.preference.Preference;

public class PreferenceMsgProcessingFragment extends PreferenceFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_msg_processing, rootKey);

    // MMS options are only enabled if we are processing MMS messages
    boolean mms = ManagePreferences.enableMsgType().contains("M");

    Preference pref = findPreference(getString(R.string.pref_use_old_mms_key));
    assert(pref != null);
    pref.setEnabled(mms);

    pref = findPreference(getString(R.string.pref_mms_timeout_key));
    assert(pref != null);
    pref.setEnabled(mms);
  }
}
