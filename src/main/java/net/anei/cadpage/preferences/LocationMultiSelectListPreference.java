package net.anei.cadpage.preferences;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceDataStore;

public class LocationMultiSelectListPreference extends MultiSelectListPreference {

  private final LocationManager locMgr;

  public LocationMultiSelectListPreference(Context context, final LocationManager locMgr,
                                           String key, String catName, final String[] values, String[] names) {
    super(context);
    this.locMgr = locMgr;
    setKey(key);
    setTitle(catName);
    setDialogTitle(catName);
    setEntryValues(values);
    setEntries(names);
    setPreferenceDataStore(new PreferenceDataStore(){
      @Nullable
      @Override
      public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        String[] locList = locMgr.getLocationList();
        Set<String> result = new HashSet<>();
        for (String loc : locList) {
          if (findIndexOfValue(loc) >= 0) result.add(loc);
        }
        return result;
      }

      @Override
      public void putStringSet(String key, @Nullable Set<String> values) {
        for (CharSequence loc : getEntryValues()) {
          String loc2 = loc.toString();
          boolean set = values.contains(loc2);
          locMgr.adjustLocation(set, loc2);
        }
      }
    });
  }
}
