package net.anei.cadpage.preferences;

import android.app.Activity;
import android.app.Dialog;
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

//  @Override
//  protected void onDialogClosed(boolean positiveResult) {
//    super.onDialogClosed(positiveResult);
//    if (positiveResult) {
//      locMgr.setNewLocation(getValue());
//      Dialog dlg = parent.getDialog();
//      if (dlg !=  null) {
//        dlg.dismiss();
//      } else {
//        Context context = getContext();
//        if (context instanceof Activity) {
//          ((Activity)context).finish();
//        }
//      }
//    }
//  }
}
