package net.anei.cadpage.preferences;

import android.content.Context;
import androidx.preference.ListPreference;

public class LocationListPreference extends ListPreference {
  
  private final LocationManager locMgr;

  public LocationListPreference(Context context, LocationManager locMgr) {
    super(context);
    this.locMgr = locMgr;
  }

  @Override
  protected String getPersistedString(String defaultReturnValue) {
    return locMgr.getLocSetting();
  }

  @Override
  protected boolean persistString(String value) {
    locMgr.setNewLocation(value);
    return true;
  }
}
