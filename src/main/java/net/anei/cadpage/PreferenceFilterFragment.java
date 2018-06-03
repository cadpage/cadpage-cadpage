package net.anei.cadpage;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import net.anei.cadpage.donation.MainDonateEvent;

public class PreferenceFilterFragment extends PreferenceFragment {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preference_filter);
  }
}
