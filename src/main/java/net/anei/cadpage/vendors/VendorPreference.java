package net.anei.cadpage.vendors;

import android.content.Context;
import androidx.preference.SwitchPreference;

class VendorPreference extends SwitchPreference {
  
  private final Vendor vendor;
  
  VendorPreference(Context context, Vendor vendor, int order) {
    super(context);
    
    // Initialize preference
    this.vendor = vendor;
    vendor.registerPreference(this);
    
    setOrder(order);
    setTitle(vendor.getTitleId());
    int summary = vendor.getSummaryId();
    if (summary > 0) setSummary(summary);
    update();
    
    // Onchange listener that always returns false.  User cannot actually
    // change the preference setting directly
    setOnPreferenceChangeListener((preference, newValue) -> false);
    
    setOnPreferenceClickListener(preference -> {
      VendorActivity.launchActivity(getContext(), VendorPreference.this.vendor);
      return true;
    });
  }

  void update() {
    setChecked(vendor.isEnabled());
  }
}
