package net.anei.cadpage.preferences;

import android.content.Context;
import androidx.preference.SwitchPreference;

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
  protected boolean getPersistedBoolean(boolean defaultReturnValue) {
    return locMgr.isSet(location);
  }

  @Override
  protected void onClick() {
    super.onClick();
    locMgr.adjustLocation(isChecked(), location);
  }

}
