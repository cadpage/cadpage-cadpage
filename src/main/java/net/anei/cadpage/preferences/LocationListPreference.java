package net.anei.cadpage.preferences;

import android.content.Context;

import net.anei.cadpage.PreferenceLocationMenuFragment;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceDataStore;

public class LocationListPreference extends ListPreference {

  private final PreferenceLocationMenuFragment parent;
  private final LocationManager locMgr;

  public LocationListPreference(Context context, PreferenceLocationMenuFragment parent, final LocationManager locMgr,
                                String key, String catName, String[]values, String[] names) {
    super(context);
    this.parent = parent;
    this.locMgr = locMgr;
    setKey(key);
    setTitle(catName);
    setDialogTitle(catName);
    setEntryValues(values);
    setEntries(names);
    setPreferenceDataStore(new PreferenceDataStore(){
      @Nullable
      @Override
      public String getString(String key, @Nullable String defValue) {
        return locMgr.getLocSetting();
      }

      @Override
      public void putString(String key, @Nullable String value) {
        locMgr.setNewLocation(value);
      }
    });
  }

  private boolean requestClose = false;

  @Override
  public boolean callChangeListener(Object newValue) {
    requestClose = super.callChangeListener(newValue);
    return requestClose;
  }

  @Override
  public void setValue(String value) {
    super.setValue(value);
    if (requestClose) this.parent.requestClose();
  }
}
