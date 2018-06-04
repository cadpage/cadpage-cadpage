package net.anei.cadpage.preferences;

import android.content.Context;
import android.preference.SwitchPreference;
import android.view.View;

public class LocationSwitchPreference extends SwitchPreference {
  
  private final String location;
  private final LocationManager locMgr;

  public LocationSwitchPreference(Context context, String location, String name,
                                  LocationManager locMgr) {
    super(context);
    this.location = location;
    this.locMgr = locMgr;
    
    setTitle(name);
  }

  @Override
  protected void onBindView(View view) {
    setChecked(locMgr.isSet(location));
    super.onBindView(view);
  }

  @Override
  protected void onClick() {
    super.onClick();
    locMgr.adjustLocation(isChecked(), location);
  }

}
