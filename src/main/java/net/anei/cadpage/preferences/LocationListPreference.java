package net.anei.cadpage.preferences;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;

public class LocationListPreference extends ListPreference {
  
  private final LocationManager locMgr;
  private final PreferenceScreen parent;

  public LocationListPreference(Context context, LocationManager locMgr,
                                 PreferenceScreen parent) {
    super(context);
    this.locMgr = locMgr;
    this.parent = parent;
  }

  @Override
  protected void onPrepareDialogBuilder(Builder builder) {
    
    setValue(locMgr.getLocSetting());
    super.onPrepareDialogBuilder(builder);
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    if (positiveResult) {
      locMgr.setNewLocation(getValue());
      Dialog dlg = parent.getDialog();
      if (dlg !=  null) {
        dlg.dismiss();
      } else {
        Context context = getContext();
        if (context instanceof Activity) {
          ((Activity)context).finish();
        }
      }
    }
  }
}
