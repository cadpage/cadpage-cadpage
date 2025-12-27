package net.anei.cadpage;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import net.anei.cadpage.donation.TextAlertGone1Event;
import net.anei.cadpage.donation.TextAlertGone2Event;
import net.anei.cadpage.donation.TextAlertWarn1Event;
import net.anei.cadpage.donation.TextAlertWarn2Event;

/**
 * This class handles interactions with the Cadpage support app.  The support app is a
 * separately installed application that performs all of the functions that Google Play store
 * no longer the Play Store version of Cadpage to do.   Most recently, Google has come down on
 * this bit of subterfuge, so Cadpage no longer prompts users to install the support app, but it
 * will continue to use it if it has been previously installed.
 */
public class SupportApp {

  public static final String CADPAGE_SUPPORT_PKG = "net.anei.cadpagesupport";
  private static final String CADPAGE_SUPPORT_CLASS = "net.anei.cadpagesupport.MainActivity";
  private static final int CADPAGE_SUPPORT_VERSION = 14;
  private static final int CADPAGE_SUPPORT_VERSION2 = 15;
  private static final int CADPAGE_SUPPORT_VERSION3 = 16;
  private static final int CADPAGE_SUPPORT_VERSION4 = 17;
  private static final int CADPAGE_SUPPORT_VERSION5 = 19;
  private static final int CADPAGE_SUPPORT_BAD_VERSION6 = 20;
  private static final int CADPAGE_SUPPORT_VERSION6 = 21;
  private static final String EXTRA_CADPAGE_LAUNCH = "net.anei.cadpage.LAUNCH";
  private static final String EXTRA_CADPAGE_PHONE = "net.anei.cadpage.CALL_PHONE";
  private static final String EXTRA_SUPPRESS_BATTERY_OPT = "new.anei.cadpage.SUPPRESS_BATTERY_OPT";

  private boolean supportAppInstalled = false;
  private boolean recMsgSupport = false;
  private boolean newMmsSupport = false;
  private boolean sendMsgSupport = false;
  private boolean callSupport = false;

  public boolean isRecMsgSupported() {
    return recMsgSupport;
  }

  public boolean isSendMsgSupported() {
    return sendMsgSupport;
  }

  public boolean isNewMmsSupported() {
    return newMmsSupport;
  }

  /**
   * Initialize support app module
   * @param context - current context
   */
  private SupportApp(Context context) {

    // If this if the full version of Cadpage, everything is supported
    if (BuildConfig.FULL_SUPPORT) {
      recMsgSupport = newMmsSupport = sendMsgSupport = callSupport = true;
      return;
    }

    // Otherwise, see what version is currently installed and what all it will support
    int installedVersion = getSupportAppVersion(context);
    Log.v("Installed support version:" + installedVersion);

    supportAppInstalled = installedVersion > 0;
    callSupport = (installedVersion >= (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ? CADPAGE_SUPPORT_VERSION5 : CADPAGE_SUPPORT_VERSION6));
    newMmsSupport = (installedVersion >= CADPAGE_SUPPORT_VERSION4);
    sendMsgSupport = (installedVersion >= CADPAGE_SUPPORT_VERSION2);
    recMsgSupport = (installedVersion >= CADPAGE_SUPPORT_VERSION);

    // If latest version, initialize ResponseSender
    if (installedVersion >= CADPAGE_SUPPORT_VERSION5) ResponseSender.setInstance();
  }

  /**
   * Prompt user about anything concerning support app
   * @param context current context
   * @param warn true if user should be warned about continueing to use support app
   * @return true if user was warned of a problem that must be addressed
   */
  public boolean prompt(Context context, boolean warn) {
    return prompt(context, warn, 0, null);
  }

  /**
   * Prompt user about anything concerning support app
   * @param context current context
   * @param warn true if user should be warned about continuing to use support app
   * @param req request code
   *            0 - check existing preference values
   *            1 - check new enable message type preference value
   *            2 - check new response button type preference value
   * @param newValue new preference value to be checked
   * @return true if user was warned of a problem that must be addressed
   */
  public boolean prompt(Context context, boolean warn, int req, String newValue) {

    // If full version of Cadpage, nothing needs to be checked
    if (BuildConfig.FULL_SUPPORT) return false;

    // Otherwise, see what capabilities we will need
    // See which version we need.  The basic version that was distributed earlier can handle
    // receiving SMS and MMS messages.  But sending text messages requires a newer version that
    // is only available from the download page and calling phone numbers requires and even newer
    // version.
    String enableMsgType = req == 1 ? newValue : ManagePreferences.realEnableMsgType();
    String callbackType = req == 2 ? newValue : ManagePreferences.callbackTypeSummary();
    boolean recMsg = false;
    boolean callbackPhone = false;
    boolean callbackText = false;
    if (req == 0 || req == 1) {
      recMsg = !enableMsgType.equals("C");
    }
    if (req == 0 || req == 2) {
      callbackPhone = callbackType.contains("P");
      callbackText = callbackType.contains("T");
    }

    // OK, can we support all of this with the current support app?
    if (recMsg && !recMsgSupport ||
        callbackText && !sendMsgSupport ||
        callbackPhone && !callSupport) {

      // No such luck, user is going to have to fix something
      if (callbackPhone || callbackText) {
        TextAlertGone2Event.instance().launch(context);
      } else {
        TextAlertGone1Event.instance().launch(context);
      }
      Log.v("Text message support error");
      return true;
    }

    // If we can, we still need to warn them if they are using the support app because it won't
    // be around forever
    else if (warn) {
      if (callbackText || callbackPhone) {
        TextAlertWarn2Event.instance().launch(context);
      } else if (recMsg) {
        TextAlertWarn1Event.instance().launch(context);
      }
      Log.v("Text message support warning");
    }

    // Fire off an intent to launch the support app.  If it installed and configured
    // correctly, it will quietly die without doing anything.
    if (supportAppInstalled) {
      launchSupportApp(context, callbackPhone, callbackPhone);
    }

    return false;
  }

  private void launchSupportApp(Context context, boolean callbackPhone, boolean suppressBatteryOpt) {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
   */
  public void fixMsgSupport() {

    // If full version, nothing needs to be fixed
    if (BuildConfig.FULL_SUPPORT) return;

    // Since we are trying to phase out the support app, the fix is to turn off everything
    // that might depend on it
    boolean fixed = false;

    String msgType = ManagePreferences.realEnableMsgType();
    String newMsgType = "C";
    if (!newMsgType.equals(msgType)) {
      msgType = newMsgType;
      ManagePreferences.setEnableMsgType(msgType);
      fixed = true;
    }

    if (ManagePreferences.removeCallbackCode("TP")) fixed = true;

    // If we fixed anything, see if we need to restore a visible preference
    if (fixed) {
      PreferenceRestorableFragment.restorePreferenceValue();
    }
  }

  /**
   * Get current version number of installed support app
   * @param context current context
   * @return version umber of support app if installed, zero otherwise
   */
  private int getSupportAppVersion(Context context) {

    // See if support package is installed
    // if it is not, return zero
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

  public static void initialize(Context context) {
    instance = new SupportApp(context);
  }

  private static SupportApp instance;

  public static SupportApp instance() {
    return instance;
  }
}
