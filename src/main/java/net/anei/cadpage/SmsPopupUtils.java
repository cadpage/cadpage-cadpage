package net.anei.cadpage;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import net.anei.cadpage.donation.NeedCadpageSupportAppEvent;
import net.anei.cadpage.donation.UpdateCadpageSupportAppEvent;

public class SmsPopupUtils {

  private static final String CADPAGE_SUPPORT_PKG = "net.anei.cadpagesupport";
  private static final String CADPAGE_SUPPORT_CLASS = "net.anei.cadpagesupport.MainActivity";
  private static final int CADPAGE_SUPPORT_VERSION = 14;
  private static final int CADPAGE_SUPPORT_VERSION2 = 15;
  private static final int CADPAGE_SUPPORT_VERSION3 = 16;
  private static final String EXTRA_CADPAGE_LAUNCH = "net.anei.cadpage.LAUNCH";
  private static final String EXTRA_CADPAGE_PHONE = "net.anei.cadpage.CALL_PHONE";

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
    if (!ManagePreferences.reqMsgSupport()) return -1;

    // If support app is not available, I guess we are not going to need it
    if (!isSupportAppAvailable()) return -1;

    // See which version we need.  The basic version that was distributed earlier can handle
    // receiving SMS and MMS messages.  But sending text messages requires a newer version that
    // is only available from the download page and calling phone numbers requires and even newer
    // version
    String callbackType = ManagePreferences.callbackTypeSummary();
    boolean callbackText = callbackType.contains("T");
    boolean callbackPhone = callbackType.contains("P");
    int version = (callbackPhone ? CADPAGE_SUPPORT_VERSION3 :
                   callbackText ? CADPAGE_SUPPORT_VERSION2 : CADPAGE_SUPPORT_VERSION);

    // See if support package is installed
    // if it is not, launch play store to install the update without further ado
    PackageManager pm = context.getPackageManager();
    try {
      PackageInfo pi = pm.getPackageInfo(CADPAGE_SUPPORT_PKG, 0);
      Log.v("Cadpage Message Support app version " + pi.versionCode + " is installed");
      if (pi.versionCode < version) {

        // event.isEnabled() always returns true.  But if we do not make the call, the optimizer
        // can call DonateActivity.launchActivity() before initializing NeedCadpageSupportAppEvent.
        if (prompt) {
          UpdateCadpageSupportAppEvent.instance().launch(context);
          Log.v("Requesting Capage Message Support app upgrade");
        }
        return 1;
      }
    }

    // If not installed, prompt user to either install it
    // or turn off text message support
    catch (PackageManager.NameNotFoundException ex) {

      // event.isEnabled() always returns true.  But if we do not make the call, the optimizer
      // can call DonateActivity.launchActivity() before initializing NeedCadpageSupportAppEvent.
      if (prompt) {
        NeedCadpageSupportAppEvent.instance().launch(context);
        Log.v("Requesting Cadpage Message Support app install");
      }
      return 1;
    }

    // Fire off an intent to launch the support app.  If it installed and configured
    // correctly, it will quietly die without doing anything.  There should not be any way that
    // this can fail, but if it does, we will log an error and continue
    if (prompt) {
      Intent intent = new Intent(Intent.ACTION_MAIN);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addCategory(Intent.CATEGORY_LAUNCHER);
      intent.setClassName(CADPAGE_SUPPORT_PKG, CADPAGE_SUPPORT_CLASS);
      intent.putExtra(EXTRA_CADPAGE_LAUNCH, true);
      if (callbackPhone) intent.putExtra(EXTRA_CADPAGE_PHONE, true);

      try {
        context.startActivity(intent);
        Log.v("Cadpage support app installed and current");
      } catch (Exception ex) {
        Log.e(ex);
      }
    }
    return 0;
  }

  private static boolean isSupportAppAvailable() {
    return !MsgAccess.ALLOWED;
  }
}