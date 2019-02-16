package net.anei.cadpage;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import net.anei.cadpage.donation.DonateActivity;
import net.anei.cadpage.donation.InstallCadpageSupportAppEvent;
import net.anei.cadpage.donation.NeedCadpageSupportAppEvent;

public class SmsPopupUtils {

  private static final String CADPAGE_SUPPORT_PKG = "net.anei.cadpagesupport";
  private static final String CADPAGE_SUPPORT_CLASS = "net.anei.cadpagesupport.MainActivity";
  private static final int CADPAGE_SUPPORT_VERSION = 14;
  private static final String EXTRA_CADPAGE_LAUNCH = "net.anei.cadpage.LAUNCH";

  public static final Pattern NAME_ADDR_EMAIL_PATTERN =
    Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

  public static final Pattern QUOTED_STRING_PATTERN =
    Pattern.compile("\\s*\"([^\"]*)\"\\s*");

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

    // Update preference so it reflects in the preference activity
    SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor settings = myPrefs.edit();
    settings.commit();

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

      if (matches != null) {
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
  }

  /**
   * Check to see if the message support app is needed and/or installed
   * @param activity current activity
   * @return -1 if message support is not needed
   *          0 if message support is needed and installed and up to date
   *          1 if user was prompted to install or update message support
   */
  public static int checkMsgSupport(Activity activity) {

    // If we are still processing SMS or MMS messages, we need to some additional work
    String msgTypes = ManagePreferences.enableMsgType();
    if (!msgTypes.contains("S") && !msgTypes.contains("M")) return -1;

    // Ditto if support app is no longer available
    if (!isSupportAppAvailable()) return -1;

    // See if support package is installed
    // if it is not, launch play store to install the update without futher ado
    PackageManager pm = activity.getPackageManager();
    try {
      PackageInfo pi = pm.getPackageInfo(CADPAGE_SUPPORT_PKG, 0);
      if (pi.versionCode < CADPAGE_SUPPORT_VERSION) {
        Log.v("Requesting support app upgrade");
        InstallCadpageSupportAppEvent.install(activity);
        return 1;
      }
    }

    // If not installed, prompt user to either install it
    // or turn off text message support
    catch (PackageManager.NameNotFoundException ex) {

      // event.isEnabled() always returns true.  But if we do not make the call, the optimizer
      // can call DonateActivity.launcheActivity() before initializing NeedCadpageSupportAppEvent.
      NeedCadpageSupportAppEvent event = NeedCadpageSupportAppEvent.instance();
      if (event.isEnabled()) {
        Log.v("Requesting support app install");
        DonateActivity.launchActivity(activity, NeedCadpageSupportAppEvent.instance(), null);
        return 1;
      }
      return 0;
    }

    // Fire off an intent to launch the support app.  If it installed and configured
    // correctly, it will quietly die without doing anything.  There should not be any way that
    // this can fail, but if it does, we will log an error and continue
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    intent.setClassName(CADPAGE_SUPPORT_PKG, CADPAGE_SUPPORT_CLASS);
    intent.putExtra(EXTRA_CADPAGE_LAUNCH, true);

    try {
      activity.startActivity(intent);
      Log.v("Cadpage support app installed and current");
    } catch (Exception ex) {
      Log.e(ex);
    }
    return 0;
  }

  private static boolean isSupportAppAvailable() {
    return false;
  }
}