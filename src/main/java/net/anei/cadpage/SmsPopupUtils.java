package net.anei.cadpage;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;

import static android.content.Context.POWER_SERVICE;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class SmsPopupUtils {

  /**
   * Enables or disables the main SMS receiver
   */
  public static void enableSMSPopup(Context context, String enable) {
    enableComponent(context, SmsReceiver.class, enable.contains("S"));
    enableComponent(context, PushReceiver.class, enable.contains("M"));
  }

  private static void enableComponent(Context context, Class<?> cls, boolean enable) {
    PackageManager pm = context.getPackageManager();
    ComponentName cn = new ComponentName(context, cls);

    if (enable) {
      pm.setComponentEnabledSetting(cn,
          PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
          PackageManager.DONT_KILL_APP);

    } else {
      pm.setComponentEnabledSetting(cn,
          PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
          PackageManager.DONT_KILL_APP);
    }
  }

  /**
   * Determine if we have Internet connectivity
   * @param context current context
   * @return true if Internet connectivity is established
   */
  public static boolean haveNet(Context context){

    // If configured to always map request, return true
    char checkNetwork = ManagePreferences.mapNetworkChk().charAt(0);
    if (checkNetwork == 'A') return true;

    // Otherwise, check to see if we have connectivity
    // If we don't return false
    ConnectivityManager mgr = ((ConnectivityManager) 
        context.getSystemService(Context.CONNECTIVITY_SERVICE));
    assert mgr != null;
    NetworkInfo info = mgr.getActiveNetworkInfo();
    if (info == null || !info.isConnected()) {
      showNetworkFailure(context, R.string.network_err_not_connected);
      return false;
    }
    
    // If we don't care about the roaming status, return true;
    if (checkNetwork == 'R') return true;
    
    // Otherwise return true if we are not currently roaming
    if (info.isRoaming()) {
      showNetworkFailure(context, R.string.network_err_roaming);
      return false;
    }
    return true;
  }
  
  private static void showNetworkFailure(Context context, int resId) {

    // We can not, and should not, display a dialog from a background service
    if (! (context instanceof Activity)) return;

    // Otherwise display the warning dialog
    new AlertDialog.Builder(context)
    .setIcon(R.drawable.ic_launcher).setTitle(R.string.network_err_title)
    .setPositiveButton(R.string.donate_btn_OK, null)
    .setMessage(resId)
    .create().show();
  }

  public static void sendImplicitBroadcast(Context context, Intent intent) {
    sendImplicitBroadcast(context, intent, null);
  }

  public static void sendImplicitBroadcast(Context context, Intent intent, String permission) {

    if (intent.getComponent() != null) {
      ContentQuery.dumpIntent(intent);
      context.sendBroadcast(intent, permission);
    } else {
      PackageManager pm = context.getPackageManager();
      List<ResolveInfo> matches = pm.queryBroadcastReceivers(intent, 0);

      for (ResolveInfo resolveInfo : matches) {
        ComponentName cn =
            new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName,
                resolveInfo.activityInfo.name);

        intent.setComponent(cn);
        ContentQuery.dumpIntent(intent);
        context.sendBroadcast(intent, permission);
      }
      intent.setComponent(null);
    }
  }

  /**
   * Set alarm to trigger at exact real time
   * @param context current context
   * @param time time alarm should trigger
   * @param pendingIntent intent to launch at trigger time
   */
  public static void setExactTime(Context context, long time, PendingIntent pendingIntent) {
    AlarmManager myAM = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (useExactAlarm(context)) {
      myAM.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    } else {
      myAM.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }
  }

  /**
   * @return true if we should use the exact time alarm instead of the normal status alarm
   * @param context Current context
   */
  private static boolean useExactAlarm(Context context) {

    // For anything up to Android 12 we are good to go
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;

    // Starting with Android 12 we either need SCHEDULE_EXACT_ALARM permission or to have
    // suppressed battery optimization.  Suppressing battery optimization is required for other
    // reasons, and Google is about to start restricting SCHEDULE_EXACT_ALARM permission.  So
    // we quit declaring it and will double check that the user really did turn off battery
    // optimization
    PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
    return pm.isIgnoringBatteryOptimizations(context.getPackageName());
  }

  private static boolean foregroundServiceLaunch = false;

  /**
   * Start new background service
   * context - current context
   * intent - intent used to launch service
   */
  @SuppressLint("NewApi")
  public static void startService(Context context, Intent intent) {
    foregroundServiceLaunch = !allowBackgroundService(context);
    if (foregroundServiceLaunch) {

      // Foreground service launches are flatly prohibited in Android 12.  If the user has
      // disregarded all of our warnings that battery optimization needs to be disabled, all
      // we can do is generate another warning and give up
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        UpgradeReceiver.fixSettingProblem(context);
      } else {
        context.startForegroundService(intent);
      }
    } else {
      context.startService(intent);
    }
  }

  /**
   * Switch new service to foreground mode if necessary
   * @param service new service
   * @param id Notification ID
   * @param nf Notification
   */
  public static void startForeground(Service service, int id, Notification nf) {
    if (foregroundServiceLaunch) {
      service.startForeground(id, nf);
    }
  }

  /**
   * Determine if we are allowed to start a background service
   * @param context current context
   * @return true if background service launches are permitted
   */
  private  static boolean allowBackgroundService(Context context) {

    // If we are not running the crippled version of Cadpage, then the system knows
    // we are responding to an incoming text message and background starts are acceptable
    if (BuildConfig.FULL_SUPPORT) return true;

    // The background start provisions started with Oreo.  Anything before that and we are good
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;

    // And if the user has followed our advice and turned off battery optimization for Cadpage,
    // we are good to go
    PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
    return pm.isIgnoringBatteryOptimizations(context.getPackageName());
  }
}