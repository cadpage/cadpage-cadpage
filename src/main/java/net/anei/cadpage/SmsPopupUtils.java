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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;

import net.anei.cadpage.donation.BatteryOptimizationSupportEvent;
import net.anei.cadpage.donation.NeedCadpageSupportApp1Event;
import net.anei.cadpage.donation.NeedCadpageSupportApp2Event;
import net.anei.cadpage.donation.NeedCadpageSupportAppEvent;
import net.anei.cadpage.donation.UpdateCadpageSupportAppEvent;

import static android.content.Context.POWER_SERVICE;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class SmsPopupUtils {

  public static final String CADPAGE_SUPPORT_PKG = "net.anei.cadpagesupport";
  private static final String CADPAGE_SUPPORT_CLASS = "net.anei.cadpagesupport.MainActivity";
  private static final int CADPAGE_SUPPORT_VERSION = 14;
  private static final int CADPAGE_SUPPORT_VERSION2 = 15;
  private static final int CADPAGE_SUPPORT_VERSION3 = 16;
  private static final int CADPAGE_SUPPORT_VERSION4 = 17;
  private static final int CADPAGE_SUPPORT_VERSION5 = 19;
  private static final int CADPAGE_SUPPORT_VERSION6 = 20;
  private static final String EXTRA_CADPAGE_LAUNCH = "net.anei.cadpage.LAUNCH";
  private static final String EXTRA_CADPAGE_PHONE = "net.anei.cadpage.CALL_PHONE";
  private static final String EXTRA_SUPPRESS_BATTERY_OPT = "new.anei.cadpage.SUPPRESS_BATTERY_OPT";

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
   * Check to see if the message support app is needed and/or installed
   * @param context current context
   * @return -1 if message support is not needed
   *          0 if message support is needed and installed and up to date
   *          1 if user was prompted to install or update message support
   */
  public static int checkMsgSupport(Context context) {
    return checkMsgSupport(context, true);
  }

  /**
   * Check to see if the message support app is needed and/or installed
   * @param context current context
   * @param prompt true to actually prompt user to install message app
   * @return -1 if message support is not needed
   *          0 if message support is needed and installed and up to date
   *          1 if user was/will be prompted to install or update message support
   */
  public static int checkMsgSupport(Context context, boolean prompt) {

    // If we are not processing SMS or MMS messages, there is no need for the support app
    String msgType = ManagePreferences.enableMsgType();
    if (!msgType.contains("S") && !msgType.contains("M")) return -1;

    // If support app is not available, I guess we are not going to need it
    if (!isSupportAppAvailable()) return -1;

    // See which version we need.  The basic version that was distributed earlier can handle
    // receiving SMS and MMS messages.  But sending text messages requires a newer version that
    // is only available from the download page and calling phone numbers requires and even newer
    // version.

    // Latest developments.  Since Android 10, the phone callback has not worked reliably.  To fix
    // this, phone callbacks now require an even newer version of the support app.

    //  Since Android 12, phone callback only works if battery optimization is disabled for the
    // support app.  A new version is required that can handle this switch
    int version;
    String callbackType = ManagePreferences.callbackTypeSummary();
    boolean callbackPhone = callbackType.contains("P");
    boolean needSMSSupport = !BuildConfig.REC_SMS_ALLOWED && msgType.contains("S");
    boolean needMMSSupport = !BuildConfig.REC_MMS_ALLOWED && msgType.contains("M");

    if (callbackPhone) {
      version = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ? CADPAGE_SUPPORT_VERSION5 : CADPAGE_SUPPORT_VERSION6;
    } else if (needMMSSupport && !ManagePreferences.useOldMMS()) {
      version = CADPAGE_SUPPORT_VERSION4;
    } else if (callbackType.contains("T")) {
      version = CADPAGE_SUPPORT_VERSION2;
    } else if (needSMSSupport || needMMSSupport){
      version = CADPAGE_SUPPORT_VERSION;
    } else {
      return -1;
    }

    // Get the installed support app version.  If latest versions, set ResponseSender instance
    int installedVersion = getSupportAppVersion(context);
    Log.v("Installed support version:" + installedVersion);
    if (installedVersion >= CADPAGE_SUPPORT_VERSION5) ResponseSender.setInstance();

    // See if the installed support app version meets are needs
    // If it does not, issue user prompt if requested.  In any case, return 1
    if (installedVersion < version) {
      if (prompt) {
        if (installedVersion <= 0) {
          if (needSMSSupport) {
            NeedCadpageSupportAppEvent.instance().launch(context);
          } else if (needMMSSupport) {
            NeedCadpageSupportApp1Event.instance().launch(context);
          } else {
            NeedCadpageSupportApp2Event.instance().launch(context);
          }
          Log.v("Requesting Cadpage Message Support app install");
        } else {
          UpdateCadpageSupportAppEvent.instance().launch(context);
          Log.v("Requesting Cadpage Message Support app upgrade");
        }
      }
      return 1;
    }

    // One last check.  If response button can initiate phone call, battery optimization for the
    // support app must be disabled
    if (BatteryOptimizationSupportEvent.instance().isEnabled()) {
      if (prompt) BatteryOptimizationSupportEvent.instance().launch(context);
      return 1;
    }

    // Fire off an intent to launch the support app.  If it installed and configured
    // correctly, it will quietly die without doing anything.  There should not be any way that
    // this can fail, but if it does, we will log an error and continue
    launchSupportApp(context, callbackPhone, false);
    return 0;
  }

  public static void launchSupportApp(Context context, boolean callbackPhone, boolean suppressBatteryOpt) {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    intent.setClassName(CADPAGE_SUPPORT_PKG, CADPAGE_SUPPORT_CLASS);
    intent.putExtra(EXTRA_CADPAGE_LAUNCH, true);
    if (callbackPhone) intent.putExtra(EXTRA_CADPAGE_PHONE, true);
    if (suppressBatteryOpt) intent.putExtra(EXTRA_SUPPRESS_BATTERY_OPT, true);

    try {
      context.startActivity(intent);
      Log.v("Cadpage support app installed and current w/phone:" + callbackPhone);
    } catch (Exception ex) {
      Log.e(ex);
    }
  }

  /**
   * Fix any Cadpage settings that are incompatible with the currently installed support app
   * @param context current context
   */
  public static void fixMsgSupport(Context context) {

    // If we are not processing SMS or MMS messages, nothing needs to be  done
    String msgType = ManagePreferences.enableMsgType();
    if (!msgType.contains("S") && !msgType.contains("M")) return;

    // If support app is not needed, nothing needs to be done
    if (!isSupportAppAvailable()) return;

    // Get the installed support app version.  If it is the latest version
    // the only thing we need to check is battery optimization most be turned off if
    // user wants to initiate phone calls.
    boolean fixed = false;
    int version = getSupportAppVersion(context);
    if (version == CADPAGE_SUPPORT_VERSION5) {
      if (!msgType.equals("C")) {
        if (ManagePreferences.removeCallbackCode(("P"))) fixed = true;
      }
    }

    // If not installed at all, turn off unsupported message processing
    else {
      if (version <= 0) {
        String newMsgType = msgType;
        if (!BuildConfig.REC_SMS_ALLOWED) newMsgType = newMsgType.replace("S", "");
        if (!BuildConfig.REC_MMS_ALLOWED) newMsgType = newMsgType.replace("M", "");
        if (!newMsgType.equals(msgType)) {
          msgType = newMsgType;
          ManagePreferences.setEnableMsgType(msgType);
          fixed = true;
        }
      }

      // We know that MMS downloads are not supported, so request the old MMS logic
      if (msgType.contains("M") && !ManagePreferences.useOldMMS()) {
        ManagePreferences.setUseOldMMS(true);
        fixed = true;
      }

      // Remove any callback codes that are not supported by the current support app
      if (!msgType.equals("C") && version < CADPAGE_SUPPORT_VERSION3) {
        String removeCode = version < CADPAGE_SUPPORT_VERSION2 ? "TP" : "P";
        if (ManagePreferences.removeCallbackCode(removeCode)) fixed = true;
      }
    }

    // If we fixed anything, see if we need to restore a visible preference
    if (fixed) {
      PreferenceRestorableFragment.restorePreferenceValue();
    }
  }

  public static boolean isSupportAppAvailable() {
    return !(BuildConfig.REC_MMS_ALLOWED && BuildConfig.SEND_ALLOWED);
  }

  /**
   * Get current version number of installed support app
   * @param context current context
   * @return version umber of support app if installed, zero otherwise
   */
  private static int getSupportAppVersion(Context context) {

    // See if support package is installed
    // if it is not, launch play store to install the update without further ado
    PackageManager pm = context.getPackageManager();
    try {
      PackageInfo pi = pm.getPackageInfo(CADPAGE_SUPPORT_PKG, 0);
      if (pi == null) return 0;
      return pi.versionCode;
    }
    catch (PackageManager.NameNotFoundException ex) {
      return 0;
    }
  }

  /**
   * Determine if we should default to old MMS support.  Which we only if the support app is
   * installed, but it is an older version that does not support the new MMS logic.
   * @param context current context
   * @return true if we should default to using old MMS logic
   */
  public static boolean isOldMMSDefault(Context context) {
    PackageManager pm = context.getPackageManager();
    try {
      PackageInfo pi = pm.getPackageInfo(CADPAGE_SUPPORT_PKG, 0);
      return pi.versionCode < CADPAGE_SUPPORT_VERSION4;
    }
    catch (PackageManager.NameNotFoundException ex) {
      return false;
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
    if (BuildConfig.REC_SMS_ALLOWED) return true;

    // The background start provisions started with Oreo.  Anything before that and we are good
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;

    // And if the user has followed our advice and turned off battery optimization for Cadpage,
    // we are good to go
    PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
    return pm.isIgnoringBatteryOptimizations(context.getPackageName());
  }
}