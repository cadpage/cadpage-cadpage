package net.anei.cadpage;

import android.os.Bundle;

import net.anei.cadpage.vendors.VendorManager;

import java.util.Objects;

public class PreferenceDirectFragment extends PreferenceFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_direct, rootKey);

    // Set up C2DM vendor preference screen
    VendorManager.instance().setupPreference(Objects.requireNonNull(getActivity()), getPreferenceScreen());

  }
}
