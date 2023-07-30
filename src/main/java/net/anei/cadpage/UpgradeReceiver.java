/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.anei.cadpage;



import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import static android.content.Context.POWER_SERVICE;

import net.anei.cadpage.vendors.VendorManager;

/**
 * Receives Intent.ACTION_MY_PACKAGE_REPLACED and BOOT_COMPLETED intents.  Checks for any
 * new conditions related to a Cadpage or Android system update that would cause Cadpage to fail
 * to process an incoming alert.  Corrective action is to ask user to run Cadpage, which will
 * check for and correct any of these conditions
 */
public class UpgradeReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v("UpgradeReceiver: onReceive()");
    if (!CadPageApplication.initialize(context)) return;
    ContentQuery.dumpIntent(intent);

    // See if there is some new condition that needs to be fixed
    if (!checkPermissions(context)) return;

    // Corrective action is to launch Cadpage, which will check for and prompt user to correct
    // any of these situations
    fixSettingProblem(context);
  }

  /**
   * Notify user of potentially fatal Cadpage settings issue
   * @param context current context
   */
  public static void fixSettingProblem(Context context) {
    Intent launchIntent = CadPageActivity.getLaunchIntent(context);
    ContentQuery.dumpIntent(launchIntent);

    // Prior to Android 10, we could just launch cadpage ourselves
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      context.startActivity(launchIntent);
    }

    // Since Android 10 we have to use a full screen notification to do this
    else {
      NotificationCompat.Builder nbuild = new NotificationCompat.Builder(context, ManageNotification.NOTIFY_CHANNEL_ID);
      nbuild.setAutoCancel(true);
      nbuild.setSmallIcon(R.drawable.ic_stat_notify);
      nbuild.setContentTitle(context.getString(R.string.notify_need_fix_title));
      nbuild.setContentText(context.getString(R.string.notify_need_fix_text));

      @SuppressLint("WrongConstant")
      PendingIntent notifIntent = PendingIntent.getActivity(context, 12776, launchIntent, CadPageApplication.FLAG_IMMUTABLE);

      nbuild.setContentIntent(notifIntent);
      nbuild.setFullScreenIntent(notifIntent, true);
      nbuild.setPriority(NotificationCompat.PRIORITY_MAX);
      nbuild.setCategory(NotificationCompat.CATEGORY_CALL);
      nbuild.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
      Notification notify = nbuild.build();

      NotificationManager myNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      assert myNM != null;
      myNM.notify(2776, notify);
    }
  }

  /**
   * Check for any new conditions that require some kind of corrective action
   * @param context current context
   * @return true if something needs to be fixed
   */
  private boolean checkPermissions(Context context) {

    // An upgrade to SDK level 31 requires that battery optimization be disabled for Cadpage
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
      if (!pm.isIgnoringBatteryOptimizations(context.getPackageName())) return true;
    }


    // An upgrade to SDK level 28 can cause regular system
    // notification audio alerts to crash if the read external data
    // permission has not been granted.  We already checked for this condition
    // during notification initialization which would have set the notifyAbort()
    // setting.  We just need to check that.
    if (ManagePreferences.notifyAbort() && ManagePreferences.notifyEnabled() &&
        !PermissionManager.isGranted(context, PermissionManager.CADPAGE_READ_AUDIO)) return true;

    // Since we bumped the target SDK level to 27, reading MMS messages requires SMS_READ permission
    // which was not needed before.  New logic checks for this situation and corrects it when the
    // main activity is started.  But that is too late to prevent a crash if an MMS message is
    // processed before launching the application.

    // To try and mitigate this sorry state of affairs, we listen for a broadcast that tells us
    // the app has been upgraded, check for the missing permission situation, and if detected,
    // launch this activity to fix things.  Once fixed, we shut down and no one is any wiser

    if (ManagePreferences.enableMsgType().contains("M") && BuildConfig.REC_MMS_ALLOWED &&
            !PermissionManager.isGranted(context, PermissionManager.READ_SMS)) return true;

    // Something else to check.  If user has upgraded to a message restricted version of
    // Cadpage, check to make sure the message support app is installed
    if ((!BuildConfig.REC_SMS_ALLOWED || !BuildConfig.REC_MMS_ALLOWED) &&
        SmsPopupUtils.checkMsgSupport(context, false) > 0) return true;

    return false;
  }
}

