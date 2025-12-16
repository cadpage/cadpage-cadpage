package net.anei.cadpage;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

public class MMSDownloader {

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
  public static boolean downloadMMS(Context context, SmsManager smsManager, String contentLoc,
                                    Uri downloadUri, PendingIntent pIntent) {

    final Bundle configOverrides = smsManager.getCarrierConfigValues();

    if (TextUtils.isEmpty(configOverrides.getString(SmsManager.MMS_CONFIG_USER_AGENT))) {
      configOverrides.remove(SmsManager.MMS_CONFIG_USER_AGENT);
    }

    if (TextUtils.isEmpty(configOverrides.getString(SmsManager.MMS_CONFIG_UA_PROF_URL))) {
      configOverrides.remove(SmsManager.MMS_CONFIG_UA_PROF_URL);
    }

    try {
      smsManager.downloadMultimediaMessage(context,
          contentLoc,
          downloadUri,
          configOverrides,
          pIntent);
      return true;
    } catch (Exception ex) {
      Log.e(ex);
      return false;
    }
  }
}
