package net.anei.cadpage;

import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

public class PreferenceButtonResponseFragment extends PreferenceRestorableFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_button_response, rootKey);

    // Set summary provider for all of the response button settings
    for (int button = 1; button <= 6; button++) {
      int screenId =  RESPONSE_SCREEN_IDS[button-1];
      Preference pref =  findPreference(getString(screenId));
      assert pref != null;
      pref.setSummaryProvider(new ButtonProvider(button));
    }
  }

  private static class ButtonProvider implements Preference.SummaryProvider {
    private int button;

    public ButtonProvider(int button) {
      this.button = button;
    }

    @Override
    public CharSequence provideSummary(Preference preference) {
      return ManagePreferences.callbackButtonTitle(button);
    }
  }

  private static final int[] RESPONSE_SCREEN_IDS = new int[]{
    R.string.pref_callback1_screen_key,
    R.string.pref_callback2_screen_key,
    R.string.pref_callback3_screen_key,
    R.string.pref_callback4_screen_key,
    R.string.pref_callback5_screen_key,
    R.string.pref_callback6_screen_key,
  };
}
