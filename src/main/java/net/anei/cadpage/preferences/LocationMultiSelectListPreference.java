package net.anei.cadpage.preferences;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

import androidx.preference.MultiSelectListPreference;

public class LocationMultiSelectListPreference extends MultiSelectListPreference {

  private final LocationManager locMgr;

  public LocationMultiSelectListPreference(Context context, LocationManager locMgr) {
    super(context);
    this.locMgr = locMgr;
  }

  @Override
  public Set<String> getPersistedStringSet(Set<String> defaultReturnValue) {
    String[] locList = locMgr.getLocationList();
    Set<String> result = new HashSet<String>();
    for (String loc : locList) {
      if (findIndexOfValue(loc) >= 0) result.add(loc);
    }
    return result;
  }

  @Override
  public boolean persistStringSet(Set<String> value) {
    for (CharSequence loc : getEntryValues()) {
      String loc2 = loc.toString();
      boolean set = value.contains(loc2.toString());
      locMgr.adjustLocation(set, loc2);
    }
    return true;
  }
}
