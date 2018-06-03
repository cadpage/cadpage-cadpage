package net.anei.cadpage;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import net.anei.cadpage.vendors.VendorManager;

public class PreferenceDirectFragment extends PreferenceFragment {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preference_direct);

    // Set up C2DM vendor preference screen
    VendorManager.instance().setupPreference(getActivity(), getPreferenceScreen());

  }
}
