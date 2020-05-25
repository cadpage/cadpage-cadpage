package net.anei.cadpage;

import android.os.Build;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

public class PreferenceCallHistoryFragment extends PreferenceFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_call_history, rootKey);
  }

  // Save display text size so we can tell if it changed
  private String oldTextSize =null;

  @Override
  public void onStart() {
    oldTextSize = ManagePreferences.textSize();
    super.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();

    String textSize = ManagePreferences.textSize();
    if (! textSize.equals(oldTextSize)) {
      SmsMessageQueue.getInstance().notifyDataChange();
    }
  }
}
