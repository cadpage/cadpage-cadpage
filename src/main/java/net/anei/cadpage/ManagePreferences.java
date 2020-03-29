package net.anei.cadpage;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.anei.cadpage.billing.BillingManager;
import net.anei.cadpage.donation.CheckPopupEvent;
import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.donation.UserAcctManager;
import net.anei.cadpage.parsers.ManageParsers;
import net.anei.cadpage.parsers.MsgParser;
import net.anei.cadpage.parsers.SplitMsgOptions;
import net.anei.cadpage.vendors.VendorManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.TwoStatePreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.core.content.ContextCompat;

import androidx.work.Constraints;
import androidx.work.NetworkType;

@SuppressWarnings({"RedundantIfStatement", "SimplifiableIfStatement", "unused"})
public class ManagePreferences implements SharedPreferences.OnSharedPreferenceChangeListener {
  
  // Preference version.  This needs to be incremented every time a new
  // configuration setting is added to force it to initialize properly
  // when the new release is first run.
  // (OK, if you know what you are doing, and the only new settings added
  // are boolean settings that default to false, you can get away with not
  // changing this)
  private static final int PREFERENCE_VERSION = 51;
  
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMddyyyy");
  
  // Name of file we build on a nobackup directory.  It's absence indicates that
  // Cadpage is starting up after an auto-restore
  private static final String CHECK_FILENAME = "HERE_I_AM";
  
  private static ManagePreferences prefs;
  private static String freeSubType;
  private static String paidSubType;

  public static void resetPreferenceVersion() {
    prefs.putInt(R.string.pref_version_key,  PREFERENCE_VERSION-1);
  }

  /**
   * Initialize the ManagePreferences class
   * @param context current context
   */
  public static void setupPreferences(Context context) {

    // Initialize the preference object
    prefs = new ManagePreferences(context);

    Object oldMergeOption = prefs.getPreference(R.string.pref_resp_merge_key);
    if (oldMergeOption instanceof Boolean) {
      String newMergeOption = ((Boolean)oldMergeOption ? "A" : "R");
      prefs.putString(R.string.pref_resp_merge_key, newMergeOption);
    }

    // Before we do anything else, see what the old preference version number was
    int oldVersion = prefs.getInt(R.string.pref_version_key, 0);

    // If the old version doesn't match the current version, we need to reload
    // the preference defaults and update the preference version
    if (oldVersion != PREFERENCE_VERSION) {
      PreferenceHeadersFragment.initializePreferences(context);
      prefs.putInt(R.string.pref_version_key, PREFERENCE_VERSION);
    }

    // While we have a context, create the file that will be used to check for
    // the autoreload status
    File checkFile = new File(ContextCompat.getNoBackupFilesDir(context), CHECK_FILENAME);

    // There are a lot of specialized preference fixes we have to make when
    // user upgrades from an earlier version of Cadpage.  Defaulting the use old MMS logic is
    // exceptional in that it should be applied the initial Cadpage installs as well as upgrades
    // to where it was first defined.
    if (oldVersion < 51) {
      if (SmsPopupUtils.isOldMMSDefault(context)) {
        prefs.putBoolean(R.string.pref_use_old_mms_key, true);
      }
    }

    // All of the other specialized preference fixes are only applied to a Cadpage upgrade.  The
    // are not performed for new cadpage installs.
    if (oldVersion > 0 && oldVersion < PREFERENCE_VERSION) {

      // If old version < 50, reverse the override volume control setting
      if (oldVersion < 50) {
        boolean value = prefs.getBoolean(R.string.pref_notif_override_volume_key);
        prefs.putBoolean(R.string.pref_notif_override_volume_key, !value);
      }

      // If old version < 49 use is upgrading to a version that requires explicit permission to
      // use their email account information
      //noinspection ConstantConditions
      if (oldVersion < 49) prefs.putBoolean(R.string.pref_account_security_upgrade, true);
      
      // If old version < 45 convert old lock_google option to new app_map_option option
      if (oldVersion < 45) {
        boolean lockGoogle = prefs.getBoolean(R.string.pref_lock_google_map_key);
        prefs.putString(R.string.pref_app_map_option_key, (lockGoogle ? "Google" : "Other"));
      }
      
      // If old version < 44, fix old problem with wrong default value for msg type 
      if (oldVersion < 44) {
        String value = prefs.getString(R.string.pref_enable_msg_type_key, "");
        if (value.equals("S")) prefs.putString(R.string.pref_enable_msg_type_key, "CS");
      }
      
      // If old version < 43 create the auto-backup check file
      if (oldVersion < 43) {
        try {
          //noinspection ResultOfMethodCallIgnored
          checkFile.createNewFile();
        } catch (IOException ex) {
          Log.e(ex);
        }
      }
        
      // If old version < 42 convert the old global response button type to 
      // individual response button types
      if (oldVersion < 42) {
        String respType = prefs.getString(R.string.pref_resp_type_key, "");
        for (int btn = 1; btn <= CALLBACK_BUTTON_CNT; btn++) {
          String tmp = respType;
          if (callbackButtonTitle(btn).length() == 0 || callbackButtonCode(btn).length() == 0) tmp = "";
          setCallbackButtonType(btn, tmp);
        }
      }
      
      // If old version was < 41, and user used to be sponsored by Cadpage, demo
      // day counter.  We are dropping Cadpage sponsored status for the general parsers
      // in this release.  If they loose our sponsored status, they should at least get
      // a full 30 day demo
      if (oldVersion < 41) {
        String location = location();
        if (location.startsWith("General")) setAuthRunDays(0);
      }
      
      // If old version was < 38, we need to convert the old process general alert
      // setting to a new general alert option setting.  This gets complicated
      // because there is no exact equivalent to old functionality which was
      // different from direct and text pages.  We will do the best we can by
      // assuming all alerts are direct pages if any direct paging vendor is
      // enabled
      
      // Recent changes require that ManagePreferences be initialized before 
      // VendorManager, and the above logic becomes problematic.  So now we assume
      // that everything is a text message.  But what we are really trusting is 
      // that there are very very few, if any, Cadpage upgrades to to this old version
      if (oldVersion < 38) {
        boolean oldGenAlert = prefs.getBoolean(R.string.pref_gen_alert_key);
        prefs.putString(R.string.pref_gen_alert_option_key, 
                        oldGenAlert ? "" : "BHNP");
      }
      
      // If old version was < 37, we need to clone two copies of the old paid year
      // and purchase date settings and one sponsor
      if (oldVersion < 37) {
        if (freeRider()) {
          setPaidYear(2, 9999);
        } else {
          int paidYear = paidYear();
          setPaidYear(1, paidYear);
          setPaidYear(2, paidYear);
        }
        
        String purchaseDate = purchaseDateString();
        setPurchaseDateString(1, purchaseDate);
        setPurchaseDateString(2, purchaseDate);
        
        String sponsor = sponsor();
        if (freeSub()) sponsor = "FREE";
        setSponsor(2, sponsor);
      }
      
      // If old version was < 35 the repeat enable key is gone and has to turn
      // into a disabled repeat interval
      if (oldVersion < 35) {
        boolean repeatEnabled = prefs.getBoolean(R.string.pref_notif_repeat_key);
        if (!repeatEnabled) prefs.putString(R.string.pref_notif_repeat_interval_key, "0");
      }
      
      // If old version was < 34, we need to convert the reminder repeat interval
      // from minutes to seconds
      if (oldVersion < 34) {
        String repeatInterval = prefs.getString(R.string.pref_notif_repeat_interval_key);
        int repeat = Integer.parseInt(repeatInterval)*60;
        if (repeat > 120) repeat = 120;
        prefs.putString(R.string.pref_notif_repeat_interval_key, Integer.toString(repeat));
      }
      
      // If old version was < 21, we need to reset the popup button configuration settings
      if (oldVersion < 21) {
        prefs.putString(R.string.pref_button1_key, context.getString(R.string.pref_button1_default));
        prefs.putString(R.string.pref_button2_key, context.getString(R.string.pref_button2_default));
        prefs.putString(R.string.pref_button3_key, context.getString(R.string.pref_button3_default));
        prefs.putString(R.string.pref_button4_key, context.getString(R.string.pref_button4_default));
        prefs.putString(R.string.pref_button5_key, context.getString(R.string.pref_button5_default));
        prefs.putString(R.string.pref_button6_key, context.getString(R.string.pref_button6_default));
      }
      
      // If old version < 17, add a 'C' to message type preference
      if (oldVersion < 17) {
        String msgType = enableMsgType();
        if (! msgType.startsWith("C")) {
          prefs.putString(R.string.pref_enable_msg_type_key, "C" + msgType);
        }
      }
      
      // Ditto if is a newer parser code that has been renamed,
      String location = location();
      String newLocation = convertOldLocationCode(location);
      if (! location.equals(newLocation)) {
        setLocation(newLocation);
        if (location.equals("MDCentreville")) {
          if (!overrideDefaults()) {
            setOverrideDefaults(true);
            setDefaultCity("Centreville");
            setDefaultState("MD");
          }
        }
      }
    }

    // Next see if Cadpage has been restored to the same or another device.  We
    // determine this by checking for the non-existence of a file in the no-backup
    // directory.
    try {
      if (checkFile.createNewFile()) {

        // If this is not an initial startup, we need to request a new registration ID
        if (oldVersion > 0) {
          FCMMessageService.resetInstanceId();

          // And set transfer status to "X" pending access to the current MEID value.
          // We will figure out latter whether we are running on a new device or not
          setTransferStatus("X");
        }
      }
    } catch (IOException ex) {
      Log.e(ex);
    }

    // Set the install date if it hasn't already been set
    setInstallDate();

    // Finally set the application enable status
    String enableStr = (enabled() ? enableMsgType() : "");
    SmsPopupUtils.enableSMSPopup(context, enableStr);
    
    // Really finally,while we have a context, look up the paid/free subscription
    // titles
    freeSubType = context.getString(R.string.free_subtype);
    paidSubType = context.getString(R.string.paid_subtype);

    // We run into problems if the change listeners are called during this setup process
    // so we don't arm them until now
    prefs.armListeners();
  }

  public static void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
    prefs.mPrefs.registerOnSharedPreferenceChangeListener(listener);
  }

  public static void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
    prefs.mPrefs.unregisterOnSharedPreferenceChangeListener(listener);
  }

  /**
   * Determine if we are in the process of upgrading from and older version of Cadpage that did
   * not support the new Account Security features
   * @return true of we are
   */
  public static boolean isAccountSecurityUpgrade() {
    return prefs.getBoolean(R.string.pref_account_security_upgrade);
  }

  public static void clearAccountSecurityUpgrade() {
    prefs.putBoolean(R.string.pref_account_security_upgrade, false);
  }

  /**
   * Convert any old obsolete location codes to new equivalent
   * @param location location to be checked
   * @return the new location that should be used
   */
  private static String convertOldLocationCode(String location) {
    
    // Scan through all of the location codes looking to see if any of thm
    // match an old code
    String[] locs = location.split(",");
    boolean bad = false;
    for (String loc : locs) {
      if (!loc.equals(ManageParsers.convertLocationCode(loc))) {
        bad = true;
        break;
      }
    }
    
    // If not, the usual case, return the original location string
    if (!bad) return location;
    
    ArrayList<String> locList = new ArrayList<>();
    for (String loc : locs) {
      loc = ManageParsers.convertLocationCode(loc);
      if (!locList.contains(loc)) locList.add(loc);
    }
    
    Collections.sort(locList);
    StringBuilder sb = new StringBuilder();
    for (String loc : locList) {
      if (sb.length() > 0) sb.append(',');
      sb.append(loc);
    }
    return sb.toString();
  }
  
  /**
   * Backdoor allows test classes to substitute their own ManagerPreference object
   * for the standard one
   * @param prefs custom ManagerPreference object
   */
  protected static void setTestPreference(ManagePreferences prefs) {
    ManagePreferences.prefs = prefs;
  }
  
  public static void setScreenSize(String newVal) {
    prefs.putString(R.string.pref_screen_size_key, newVal);
  }
  
  public static boolean initialized() {
    if (prefs.getBoolean(R.string.pref_initialized_key)) return true;
    prefs.putBoolean(R.string.pref_initialized_key, true);
    return false;
  }

  public static boolean enabled() {
    return prefs.getBoolean(R.string.pref_enabled_key);
  }
  
  public static void setEnabled(boolean newVal) {
    prefs.putBoolean(R.string.pref_enabled_key, newVal);
  }
  
  public static String release() {
    return prefs.getString(R.string.pref_release_key, "");
  }
  
  public static void setRelease(String newRelease) {
    prefs.putString(R.string.pref_release_key, newRelease);
  }
  
  public static String enableMsgType() {
    return prefs.getString(R.string.pref_enable_msg_type_key);
  }

  public static void setEnableMsgType(String newVal) {
    prefs.putString(R.string.pref_enable_msg_type_key, newVal);
  }

  public static int mmsTimeout() {
    return prefs.getIntValue(R.string.pref_mms_timeout_key);
  }
  
  public static String location() {
    return prefs.getString(R.string.pref_location_key, "General");
  }
  
  public static void setLocation(String newLocation) {
    prefs.putString(R.string.pref_location_key, newLocation);
  }
  
  public static MsgParser getCurrentParser() {
    try {
      return ManageParsers.getInstance().getParser(location());
    } catch (RuntimeException ex) {
      setLocation("General");
      throw ex;
    }
  }
  
  public static boolean overrideFilter() {
    return prefs.getBoolean(R.string.pref_override_filter_key);
  }
  
  public static String filter() {
    return prefs.getString(R.string.pref_filter_key);
  }
  
  public static boolean showSource() {
    return prefs.getBoolean(R.string.pref_show_source_key);
  }
  
  public static boolean overrideDefaults() {
    return prefs.getBoolean(R.string.pref_override_default_key);
  }
  
  private static void setOverrideDefaults(boolean newVal) {
    prefs.putBoolean(R.string.pref_override_default_key, newVal);
  }
  
  public static String defaultCity() {
    return prefs.getString(R.string.pref_defcity_key);
  }
  
  private static void setDefaultCity(String newVal) {
    prefs.putString(R.string.pref_defcity_key, newVal);
  }
  
  public static String defaultState() {
    return prefs.getString(R.string.pref_defstate_key);
  }
  
  private static void setDefaultState(String newVal) {
    prefs.putString(R.string.pref_defstate_key, newVal);
  }
  
  public static int logLimit() {
    return prefs.getIntValue(R.string.pref_loglimit_key);
  }
  
  public static int partMsgTimeout() {
    return prefs.getIntValue(R.string.pref_msgtimeout_key);
  }

  public static boolean overrideActive911SplitOptions() {
    return prefs.getBoolean(R.string.pref_override_active911_split_key);
  }
  
  public static SplitMsgOptions getDefaultSplitMsgOptions() {
    return new SplitMsgOptionsDefault();
  }

  private static class SplitMsgOptionsDefault implements SplitMsgOptions {

    @Override
    public boolean splitDirectPage() {
      return prefs.getBoolean(R.string.pref_split_direct_page_key);
    }
     
    @Override
    public int splitMinMsg() {
      return prefs.getIntValue(R.string.pref_split_min_msg_key);
    }

    @Override
    public boolean splitBreakIns() {
      return false;
    }

    @Override
    public boolean splitBlankIns() {
      return prefs.getBoolean(R.string.pref_split_blank_ins_key);
    }
    
    @Override
    public boolean splitChkSender() {
      return prefs.getBoolean(R.string.pref_split_chk_sender_key);
    }
    
    @Override
    public boolean splitKeepLeadBreak() {
      return prefs.getBoolean(R.string.pref_split_keep_lead_break_key);
    }
    
    @Override
    public boolean revMsgOrder() {
      return prefs.getBoolean(R.string.pref_rev_msg_order_key);
    }

    @Override
    public boolean mixedMsgOrder() {
      return prefs.getBoolean(R.string.pref_mix_msg_order_key);
    }
    
    @Override
    public boolean noParseSubjectFollow() {
      return false;
    }

    @Override
    public int splitBreakLength() {
      return 0;
    }

    @Override
    public int splitBreakPad() {
      return 0;
    }

    @Override
    public boolean subjectColonField() {
      return false;
    }
  }

  /**
   * @return true if Cadpage will accomplish anything with current settings
   */
  public static boolean isFunctional() {

    // The text alert checks only count if text processing is enabled
    if (! enableMsgType().equals("C")) {

      // Specifying a location other than General is good
      String location = location();
      for (String part : location.split(",")) {
        if (!part.startsWith("General")) return true;
        }

      // Or any functional sender filter is good
      if (ManagePreferences.filter().trim().length() > 1) return true;
    }

    // Registered with any direct paging service is good
    if (VendorManager.instance().isRegistered()) return true;

    // Otherwise report nonfunctional status
    return false;
  }
  
  public static boolean suppressDupMsg() {
    return prefs.getBoolean(R.string.pref_suppress_dup_msg_key);
  }
  
  public static boolean overrideVendorLoc() {
    return prefs.getBoolean(R.string.pref_override_vendor_loc_key);
  }
  
  public static boolean activeScanner() {
    return prefs.getBoolean(R.string.pref_activate_scanner_key);
  }
  
  public static String scannerChannel() {
    String channel = prefs.getString(R.string.pref_scanner_channel_key, null);
    if (channel == null) channel = "<None Selected>";
    return channel;
  }
  
  public static void setScannerChannel(String newVal) {
    prefs.putString(R.string.pref_scanner_channel_key, newVal);
  }

  private static int scannerTimeout() {
    return prefs.getIntValue(R.string.pref_scanner_timeout_key);
  }
  
  public static Intent scannerIntent() {
    String value = prefs.getString(R.string.pref_scanner_channel_app_node_key, null);
    if (value == null) return null;
    if (!value.startsWith(SCAN_VERSION)) return null;
    value = value.substring(SCAN_VERSION.length());
    String[] fields = value.split(SCAN_FLD_DELIM);
    if (fields.length != 4) return null;
    Intent intent = new Intent(fields[0]);
    Bundle bundle = new Bundle();
    bundle.putString(SCAN_ACTION_KEY, fields[1]);
    bundle.putString(SCAN_DESCRIPTION_KEY, fields[2]);
    bundle.putString(SCAN_LOCALE_KEY, fields[3]);
    int timeout = scannerTimeout();
    if (timeout > 0) {
      bundle.putLong("duration", timeout*60L);
    }
    intent.putExtra(SCAN_BUNDLE_KEY, bundle);
    return intent;
  }
  
  public static void setScannerIntent(Intent playIntent) {
    String newVal = null;
    String action = playIntent.getAction();
    Bundle bundle = playIntent.getBundleExtra(SCAN_BUNDLE_KEY);
    if (bundle != null) {
      String action2 = bundle.getString(SCAN_ACTION_KEY);
      String description = bundle.getString(SCAN_DESCRIPTION_KEY);
      String locale = bundle.getString(SCAN_LOCALE_KEY);
      if (action != null && action2 != null && description != null && locale != null) {
        newVal = SCAN_VERSION + action + SCAN_FLD_DELIM + action2 + SCAN_FLD_DELIM + description + SCAN_FLD_DELIM + locale;
      }
    }
    prefs.putString(R.string.pref_scanner_channel_app_node_key, newVal);
  }
  
  private static final String SCAN_VERSION = "00";
  private static final String SCAN_BUNDLE_KEY = "com.twofortyfouram.locale.intent.extra.BUNDLE";
  private static final String SCAN_ACTION_KEY = "action";
  private static final String SCAN_DESCRIPTION_KEY = "description";
  private static final String SCAN_LOCALE_KEY = "localeDirectoryLine";
  private static final String SCAN_FLD_DELIM = "<!>";
  
  public static boolean publishPages() {
    return prefs.getBoolean(R.string.pref_publish_pages_key);
  }
  
  public static boolean notifyEnabled() {
    return prefs.getBoolean(R.string.pref_notif_enabled_key);
  }
  
  public static void setNotifyEnabled(boolean newVal) {
    prefs.putBoolean(R.string.pref_notif_enabled_key, newVal);
  }
  
  public static boolean notifyOverride() {
    return prefs.getBoolean(R.string.pref_notif_override_key);
  }

  public static void setNotifyOverride(boolean newVal) {
    prefs.putBoolean(R.string.pref_notif_override_key, newVal);
  }
  
  public static boolean notifyOverrideVolume() {
    return prefs.getBoolean(R.string.pref_notif_override_volume_key);
  }
  
  public static boolean notifyOverrideSound() {
    return prefs.getBoolean(R.string.pref_notif_override_sound_key);
  }
  
  public static boolean notifyOverrideLoop() {
    return prefs.getBoolean(R.string.pref_notif_override_loop_key);
  }

  public static String notifySound() {
    return prefs.getString(R.string.pref_notif_sound_key);
  }

  public static void setNotifySound(String newVal) {
    prefs.putString(R.string.pref_notif_sound_key, newVal);
  }
  
  public static int notifyTimeout() {
    return prefs.getIntValue(R.string.pref_notif_timeout_key);
  }
  
  public static boolean vibrate() {
    return prefs.getBoolean(R.string.pref_vibrate_key);
  }
  
  public static String vibratePattern() {
    return prefs.getString(R.string.pref_vibrate_pattern_key);
  }

  public static void setVibratePatternCustom(String pattern) {
    prefs.putString(R.string.pref_vibrate_pattern_custom_key, pattern);
  }
  
  public static String vibratePatternCustom() {
    if ( prefs.getString(R.string.pref_vibrate_pattern_custom_key) == null){
      return prefs.getString(R.string.pref_vibrate_pattern_key);
    }
    return prefs.getString(R.string.pref_vibrate_pattern_custom_key);
  }
  
  public static boolean flashLED() {
    return prefs.getBoolean(R.string.pref_flashled_key);
  }
  
  public static String flashLEDColor() {
    return prefs.getString(R.string.pref_flashled_color_key);
  }
  
  public static String textSize() {
   return  prefs.getString(R.string.pref_textsize_key);
  }
  
  public static String flashLEDColorCustom() {
    if (prefs.getString(R.string.pref_flashled_color_custom_key)==null){
      return prefs.getString(R.string.pref_flashled_color_key);
    }
    return prefs.getString(R.string.pref_flashled_color_custom_key);
  }
  
  public static String flashLEDPattern() {
    return prefs.getString(R.string.pref_flashled_pattern_key);
  }
  
  public static String flashLEDPatternCustom() {
    if (prefs.getString(R.string.pref_flashled_pattern_custom_key)==null){
      return prefs.getString(R.string.pref_flashled_pattern_key);
    }
    return prefs.getString(R.string.pref_flashled_pattern_custom_key);
  }
  
  public static long notifyDelay() {
    return Long.parseLong(prefs.getString(R.string.pref_notif_delay_key));
  }
  
  public static boolean notifyReqAck() {
    return prefs.getBoolean(R.string.pref_notif_req_ack_key);
  }
  
  public static int notifyRepeatInterval() {
    return prefs.getIntValue(R.string.pref_notif_repeat_interval_key);
  }

  public static boolean notifyCheckAbort() {
    return prefs.getBoolean(R.string.pref_notif_check_abort_key);
  }

  public static void setNotifyCheckAbort(boolean newVal) {
    prefs.putBoolean(R.string.pref_notif_check_abort_key, newVal);
  }

  public static boolean notifyAbort() {
    return prefs.getBoolean(R.string.pref_notif_abort_key);
  }

  public static void setNotifyAbort(boolean newVal) {

    // If we have identify a notification abort problem, clear the
    // initial permissions checked flag so the next startup will
    // report the problem.
    if (newVal) initialPermissionsChecked = false;
    prefs.putBoolean(R.string.pref_notif_abort_key, newVal);
  }

  public static boolean popupEnabled() {
    return prefs.getBoolean(R.string.pref_popup_enabled_key);
  }
  
  public static void setPopupEnabled(boolean newVal) {
    prefs.putBoolean(R.string.pref_popup_enabled_key, newVal);
  }
  
  public static boolean noShowInCall() {
    return prefs.getBoolean(R.string.pref_noShowInCall_key);
  }

  public static boolean smspassthru() {
    // This feature just doesn't work since Android 4.4
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) return true;

    // If donation status is disabled, we never block messages from falling
    // through to regular processor.
    if (!DonationManager.instance().isEnabled()) return true;
    return prefs.getBoolean(R.string.pref_passthrusms_key);
  }
  
  public static boolean screenOn() {
    return prefs.getBoolean(R.string.pref_screen_on_key);
  }
  
  public static int timeout() {
    return prefs.getIntValue(R.string.pref_timeout_key);
  }
  
  public static boolean dimScreen() {
    return prefs.getBoolean(R.string.pref_dimscreen_key);
  }
  
  public static boolean showPersonal() {
    return prefs.getBoolean(R.string.pref_show_personal_key);
  }
  
  public static boolean deleteUnopen() {
    return prefs.getBoolean(R.string.pref_delete_unopen_key);
  }
  
  public static String mapNetworkChk() {
    return prefs.getString(R.string.pref_map_network_chk_key);
  }

  public static Constraints networkConstraint() {
    NetworkType netReq = ManagePreferences.mapNetworkChk().charAt(0) != 'R' ? NetworkType.NOT_ROAMING : NetworkType.CONNECTED;
    return new Constraints.Builder().setRequiredNetworkType(netReq).build();
  }
  
  public static int gpsMapOption() {
    String val = prefs.getString(R.string.pref_gps_map_option_key);
    return val.charAt(0) - '0';
  }
  
  public static String appMapOption() {
    return prefs.getString(R.string.pref_app_map_option_key);
  }

  public static boolean showHistoryAddress() {
    return prefs.getBoolean(R.string.pref_show_history_address_key);
  }


  public static boolean navigateMap() {
    return prefs.getBoolean(R.string.pref_navigate_map_key);
  }
  
  public static String reportPosition() {
    return prefs.getString(R.string.pref_report_position_key);
  }
  
  public static void setReportPosition(String newVal) {
    prefs.putString(R.string.pref_report_position_key, newVal);
  }
  
  

  public static int historyCount() {
    return prefs.getIntValue(R.string.pref_history_limit_key);
  }
  
  public static String responseMerge() {
    return prefs.getString(R.string.pref_resp_merge_key);
  }
  
  private static final int[] POPUP_BUTTON_IDS = new int[]{
    R.string.pref_button1_key,
    R.string.pref_button2_key,
    R.string.pref_button3_key,
    R.string.pref_button4_key,
    R.string.pref_button5_key,
    R.string.pref_button6_key
  };
  
  public static final int POPUP_BUTTON_CNT = POPUP_BUTTON_IDS.length;
  public static int popupButton(int button) {
    String val =  prefs.getString(POPUP_BUTTON_IDS[button-1]);
    if (val.length() == 0) return 0;
    return Integer.parseInt(val);
  }
  
  private static final int[] CALLBACK_TYPE_IDS = new int[]{
    R.string.pref_callback1_type_key,
    R.string.pref_callback2_type_key,
    R.string.pref_callback3_type_key,
    R.string.pref_callback4_type_key,
    R.string.pref_callback5_type_key,
    R.string.pref_callback6_type_key,
  };
  
  public static final int CALLBACK_BUTTON_CNT = CALLBACK_TYPE_IDS.length;
  
  public static String callbackButtonType(int button) {
    return prefs.getString(CALLBACK_TYPE_IDS[button-1]);
  }
  
  private static void setCallbackButtonType(int button, String value) {
    prefs.putString(CALLBACK_TYPE_IDS[button-1], value);
  }
  
  private static final int[] CALLBACK_TITLE_IDS = new int[]{
    R.string.pref_callback1_title_key,
    R.string.pref_callback2_title_key,
    R.string.pref_callback3_title_key,
    R.string.pref_callback4_title_key,
    R.string.pref_callback5_title_key,
    R.string.pref_callback6_title_key,
  };
  
  public static String callbackButtonTitle(int button) {
    return prefs.getString(CALLBACK_TITLE_IDS[button-1]);
  }

  private static void setCallbackButtonTitle(int button, String value) {
    prefs.putString(CALLBACK_TITLE_IDS[button-1], value);
  }

  private static final int[] CALLBACK_CODE_IDS = new int[]{
    R.string.pref_callback1_key,
    R.string.pref_callback2_key,
    R.string.pref_callback3_key,
    R.string.pref_callback4_key,
    R.string.pref_callback5_key,
    R.string.pref_callback6_key,
  };
  
  public static String callbackButtonCode(int button) {
    return prefs.getString(CALLBACK_CODE_IDS[button-1]);
  }

  private static void setCallbackButtonCode(int button, String value) {
    prefs.putString(CALLBACK_CODE_IDS[button-1], value);
  }

  private static final int[] EXTRA_BUTTON_IDS = new int[]{
    R.string.pref_xtra_resp_button1_key,
    R.string.pref_xtra_resp_button2_key,
    R.string.pref_xtra_resp_button3_key
  };
  
  public static boolean altMapButton() {
    return prefs.getBoolean(R.string.pref_alt_map_button_key);
  }
  
  public static boolean mapPageButton() {
    return prefs.getBoolean(R.string.pref_map_page_button_key);
  }
  
  public static final int EXTRA_BUTTON_CNT = EXTRA_BUTTON_IDS.length;
  public static int extraButton(int button) {
    String val =  prefs.getString(EXTRA_BUTTON_IDS[button-1]);
    if (val.length() == 0) return 0;
    return Integer.parseInt(val);
  }
  
  public static FilterOptions genAlertOptions() {
    return new FilterOptions(prefs.getString(R.string.pref_gen_alert_option_key));
  }
  
  public static FilterOptions runReportOptions() {
    return new FilterOptions(prefs.getString(R.string.pref_run_report_option_key));
  }

  public static Date installDate() {
    String dateStr = prefs.getString(R.string.pref_install_date_key);
    try {
      return DATE_FORMAT.parse(dateStr);
    } catch (ParseException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  private static void setInstallDate() {
    if (prefs.getString(R.string.pref_install_date_key, null) != null) return;
    setInstallDate(new Date());
  }
  
  public static void setInstallDate(Date date) {
    String dateStr = DATE_FORMAT.format(date);
    prefs.putString(R.string.pref_install_date_key, dateStr);
    DonationManager.instance().reset();
    MainDonateEvent.instance().refreshStatus();
  }
  
  public static int paidYear() {
    return paidYear(0);
  }
  
  public static int paidYear(int type) {
    return prefs.getInt(PAID_YEAR_RES_IDS[type], 0);
  }
  
  public static void setPaidYear(int year) {
    setPaidYear(0, year);
  }
  
  public static void setPaidYear(int type, int year) {
    prefs.putInt(PAID_YEAR_RES_IDS[type], year);
  }
  
  private static final int[] PAID_YEAR_RES_IDS = new int[]{
    R.string.pref_paid_year_key,
    R.string.pref_paid_year_1_key,
    R.string.pref_paid_year_2_key,
    R.string.pref_paid_year_3_key
  };
  
  public static Date purchaseDate() {
    return purchaseDate(0);
  }
  
  private static Date purchaseDate(int type) {
    String dateStr = prefs.getString(PURCHASE_DATE_RES_IDS[type], null);
    if (dateStr == null) return null;
    try {
      return DATE_FORMAT.parse(dateStr);
    } catch (ParseException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  public static void setPurchaseDate(Date date) {
    setPurchaseDate(0, date);
  }
  
  public static void setPurchaseDate(int type, Date date) {
    String dateStr = DATE_FORMAT.format(date);
    prefs.putString(PURCHASE_DATE_RES_IDS[type], dateStr);
  }
  
  public static String purchaseDateString() {
    return purchaseDateString(0);
  }
  
  public static String purchaseDateString(int type) {
    return prefs.getString(PURCHASE_DATE_RES_IDS[type], null);
  }
  
  public static String currentDateString() {
    return DATE_FORMAT.format(new Date());
  }
  
  public static void setPurchaseDateString(String sDate) {
    setPurchaseDateString(0, sDate);
  }
  
  public static void setPurchaseDateString(int type, String  sDate) {
    prefs.putString(PURCHASE_DATE_RES_IDS[type], sDate);
  }
  
  private static final int[] PURCHASE_DATE_RES_IDS = new int[]{
    R.string.pref_purchase_date_key,
    R.string.pref_purchase_date_1_key,
    R.string.pref_purchase_date_2_key,
    R.string.pref_purchase_date_3_key
  };
  
  public static String sponsor() {
    return sponsor(0);
  }
  
  public static String sponsor(int type) {
    return prefs.getString(SPONSOR_RES_IDS[type], null);
  }
  
  public static void setSponsor(String sponsor) {
    setSponsor(0, sponsor);
  }
  
  public static void setSponsor(int type, String sponsor) {
    String oldSponsor = sponsor(type);
    if (oldSponsor == null && sponsor == null) return;
    prefs.putString(SPONSOR_RES_IDS[type], sponsor);
  }
  
  private static final int[] SPONSOR_RES_IDS = new int[]{
    R.string.pref_sponsor_key,
    R.string.pref_sponsor_1_key,
    R.string.pref_sponsor_2_key,
    R.string.pref_sponsor_3_key
  };

  public static int subStatus() {
    return subStatus(0);
  }

  public static int subStatus(int type) {
    if (type >= SUB_STATUS_IDS.length) return 0;
    return prefs.getInt(SUB_STATUS_IDS[type], 0);
  }

  public static void setSubStatus(int status) {
    setSubStatus(0, status);
  }

  public static void setSubStatus(int type, int status) {
    if (type >= SUB_STATUS_IDS.length) return;
    prefs.putInt(SUB_STATUS_IDS[type], status);
  }

  private static final int[] SUB_STATUS_IDS = new int[]{
      R.string.pref_sub_status_key,
      R.string.pref_sub_status_1_key
  };
  
  public static boolean freeRider() {
    return prefs.getBoolean(R.string.pref_free_rider_key);
  }
  
  public static void setFreeRider(boolean newVal) {
    if (newVal == freeRider()) return;
    prefs.putBoolean(R.string.pref_free_rider_key, newVal);
  }
  
  public static boolean freeSub() {
    return prefs.getBoolean(R.string.pref_free_sub_key);
  }
  
  public static void setFreeSub(boolean newVal) {
    if (newVal == freeSub()) return;
    prefs.putBoolean(R.string.pref_free_sub_key, newVal);
  }
  
  public static String subscriptionType() {
    return ManagePreferences.freeSub() ? freeSubType : paidSubType;
  }

  public static String authLocation() {
    return prefs.getString(R.string.pref_auth_location_key, "");
  }
  
  public static void setAuthLocation(String newVal) {
    if (newVal != null && newVal.equals(authLocation())) return;
    prefs.putString(R.string.pref_auth_location_key, newVal);
  }
  
  public static String authOrganization() {
    return prefs.getString(R.string.pref_auth_organization_key, "");
  }
  
  public static void setAuthOrganization(String newVal) {
    prefs.putString(R.string.pref_auth_organization_key, newVal);
  }
  
  public static Date authExtraDate() {
    String dateStr = prefs.getString(R.string.pref_auth_extra_date_key, null);
    if (dateStr == null) return null;
    try {
      return DATE_FORMAT.parse(dateStr);
    } catch (ParseException ex) {
      return null;
    }
  }
  
  public static int authExtraCnt() {
    return prefs.getInt(R.string.pref_auth_extra_cnt_key, 0);
  }
  
  public static void resetAuthExtra() {
    prefs.putString(R.string.pref_auth_extra_date_key, null);
    prefs.putInt(R.string.pref_auth_extra_cnt_key, 0);
  }
  
  public static void authExtraDay() {
    String sDate = DATE_FORMAT.format(new Date());
    prefs.putString(R.string.pref_auth_extra_date_key, sDate);
    int cnt = authExtraCnt();
    prefs.putInt(R.string.pref_auth_extra_cnt_key, cnt+1);
  }

  public static Date expireDate() {
    String dateStr = prefs.getString(R.string.pref_expire_date_key, null);
    if (dateStr == null) return null;
    try {
      return DATE_FORMAT.parse(dateStr);
    } catch (ParseException ex) {
      return null;
    }
  }

  public static void setExpireDate(Date expireDate) {
    String dateStr = expireDate == null ? null : DATE_FORMAT.format(expireDate);
    prefs.putString(R.string.pref_expire_date_key, dateStr);
  }

  public static Date releaseDate() {
    String sDate = prefs.context.getString(R.string.release_date);
    try {
      return DATE_FORMAT.parse(sDate);
    } catch (ParseException e) {
      return null;
    }
  }

  public static int calcAuthRunDays(Date date) {
    if (date == null) {
      prefs.putString(R.string.pref_auth_last_date_key, null);
      return 0;
    }
    int days = prefs.getInt(R.string.pref_auth_run_days_key, 0);
    String curDate = DATE_FORMAT.format(date);
    String lastDate = prefs.getString(R.string.pref_auth_last_date_key, null);
    if (lastDate == null || ! lastDate.equals(curDate)) {
      prefs.putString(R.string.pref_auth_last_date_key, curDate);
      if (lastDate != null) {
        days++;
        prefs.putInt(R.string.pref_auth_run_days_key, days);
      }
    }
    return days;
  }
  
  public static void setAuthRunDays(int days) {
    prefs.putInt(R.string.pref_auth_run_days_key, days);
    prefs.putString(R.string.pref_auth_last_date_key, null);
  }
  
  public static void rollLastAuthDate(String dateStr) {
    if (prefs.getString(R.string.pref_auth_last_date_key, null) != null) {
      prefs.putString(R.string.pref_auth_last_date_key, dateStr);
    }
  }
  
  public static long authLastCheckTime() {
    return prefs.getLong(R.string.pref_auth_last_check_time_key, 0L);
  }
  
  public static void setAuthLastCheckTime(long newVal) {
    prefs.putLong(R.string.pref_auth_last_check_time_key, newVal);
  }
  
  private static void setAuthLastCheckTime() {
    setAuthLastCheckTime(System.currentTimeMillis());
  }

  /**
   * Determine our startup status
   * @param versionCode current version code
   * @return true if something has changed requiring a new registration ID
   */
  public static boolean newVersion(int versionCode) {
    
    boolean result = false;
    
    // Check to see if the installed version has changed.  If it has
    // a new registration ID needs to be requested
    int prevVersion = prefs.getInt(R.string.pref_prev_version_code, 0);
    if (versionCode != prevVersion) {
      result = true;
      prefs.putInt(R.string.pref_prev_version_code, versionCode);
    }
    return result;
  }

  public static Date registerDate() {
    String dateStr = prefs.getString(R.string.pref_register_date_key, null);
    if (dateStr == null) return null;
    try {
      return DATE_FORMAT.parse(dateStr);
    } catch (ParseException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  public static void setRegisterDate() {
    Date curDate = new Date();
    setRegisterDate(curDate);
  }
  
  private static void setRegisterDate(Date date) {
    String dateStr = DATE_FORMAT.format(date);
    prefs.putString(R.string.pref_register_date_key, dateStr);
  }

  public static void setLedColorCustom(int color) {
    prefs.putString(R.string.pref_flashled_color_custom_key, "#" + Integer.toHexString(color));
  }
  
  public static String ledPattern() {
    return prefs.getString(R.string.pref_flashled_pattern_key, R.string.pref_flashled_pattern_default);
  }
  
  public static String ledPatternCustom() {
    return prefs.getString(R.string.pref_flashled_pattern_custom_key, R.string.pref_flashled_pattern_default);
  }

  public static void setLedPatternCustom(String pattern) {
    prefs.putString(R.string.pref_flashled_pattern_custom_key, pattern);
  }
  
  public static long lastLocTime() {
    return prefs.getLong(R.string.pref_last_loc_time_key, 0L);
  }
  
  public static void setLastLocTime(long newVal) {
    prefs.putLong(R.string.pref_last_loc_time_key, newVal);
  }
  
  public static float lastLocAcc() {
    return prefs.getFloat(R.string.pref_last_loc_acc_key, 1.E30F);
  }
  
  public static void setLastLocAcc(float newVal) {
    prefs.putFloat(R.string.pref_last_loc_acc_key, newVal);
  }

  public static long lastGcmEventTime() {
    return prefs.getLong(R.string.pref_last_gcm_event_time_key, 0L);
  }
  
  public static void setLastGcmEventTime(long newVal) {
    prefs.putLong(R.string.pref_last_gcm_event_time_key, newVal);
  }
  
  public static String lastGcmEventType() {
    return prefs.getString(R.string.pref_last_gcm_event_type_key, null);
  }
  
  public static void setLastGcmEventType(String newVal) {
    prefs.putString(R.string.pref_last_gcm_event_type_key, newVal);
  }
  
  public static int restoreVol() {
    return prefs.getInt(R.string.pref_restore_vol, -1);
  }
  
  public static void setRestoreVol(int newVal) {
    prefs.putInt(R.string.pref_restore_vol, newVal);
  }
  
  public static int restoreMode() {
    return prefs.getInt(R.string.pref_restore_mode, -1);
  }
  
  public static void setRestoreMode(int newVal) {
    prefs.putInt(R.string.pref_restore_mode, newVal);
  }

  public static String transferStatus() {
    return prefs.getString(R.string.pref_transfer_status_key, "N");
  }

  public static void resetTransferStatus() {
    setTransferStatus("N");
  }

  public static void setTransferStatus(String newVal) {
    prefs.putString(R.string.pref_transfer_status_key, newVal);
  }

  public static boolean noMapGpsLabel() {
    return prefs.getBoolean(R.string.pref_no_map_gps_label);
  }
  
  public static void setNoMapGpsLabel(boolean newVal) {
    prefs.putBoolean(R.string.pref_no_map_gps_label, newVal);
  }

  public static String billingAccount() {
    return prefs.getString(R.string.pref_billing_account_key, null);
  }

  public static void setBillingAccount(String newVal) {
    prefs.putString(R.string.pref_billing_account_key, newVal);
  }

  public static boolean forcePopup() {
    return prefs.getBoolean(R.string.pref_force_popup);
  }

  public static void setForcePopup(boolean newVal) {
    prefs.putBoolean(R.string.pref_force_popup, newVal);
  }

  public static boolean useOldMMS() {
    return prefs.getBoolean(R.string.pref_use_old_mms_key);
  }

  public static void setUseOldMMS(boolean newVal) {
    prefs.putBoolean(R.string.pref_use_old_mms_key, newVal);
  }

  public static void clearAll() {
    SharedPreferences.Editor settings = prefs.mPrefs.edit();
    settings.clear();
    settings.commit();
  }

  /**
   * Determine if message support app will be needed to support
   * the message restricted version of Cadpage
   * @return true if it will be required
   */
  public static boolean reqMsgSupport() {
    String msgType = enableMsgType();
    return msgType.contains("S") || msgType.contains("M");
  }

  /**
   * Determine if the latest version of the message support app will
   * be required to support the message restricted version of Cadpage
   * @return true if it will be required
   */
  public static String callbackTypeSummary() {
    boolean textResponse = false;
    boolean phoneResponse = false;
    for (int btn = 1; btn <= POPUP_BUTTON_CNT; btn++) {
      if (callbackButtonTitle(btn).length() > 0 &&
          callbackButtonCode(btn).length() > 0) {
        String type = callbackButtonType(btn);
        if (type.contains("T")) textResponse = true;
        if (type.contains("P")) phoneResponse = true;
      }
    }
    return (textResponse ? (phoneResponse ? "TP" : "T") :
             phoneResponse ? "P" : "");
  }

  public static boolean removeCallbackCode(String code) {
    boolean result = false;
    for (int btn = 1; btn <= POPUP_BUTTON_CNT; btn++) {
      if (callbackButtonTitle(btn).length() > 0 &&
          callbackButtonCode(btn).length() > 0) {
        String type = callbackButtonType(btn);
        if (type.length() > 0 && code.contains(type)) {
          result = true;
          setCallbackButtonType(btn, "");
          setCallbackButtonTitle(btn, "");
          setCallbackButtonCode(btn, "");
        }
      }
    }
    return result;
  }

  /**
   * Append configuration information to constructed message
   * @param context active context
   * @param sb StringBuilder object where message is constructed
   */
  public static void addConfigInfo(Context context, StringBuilder sb) {
    sb.append(String.format("\n\n----------\nSysinfo: %s\nModel: %s\nStatus: %s%s\n\n",
        Build.FINGERPRINT, Build.MODEL, 
        DonationManager.instance().status().toString(),
        DonationManager.instance().isPaidSubscriber()?"/Paid":""));
    
    sb.append("Preference_Configuration:\n");

    Map<String, ?> map = prefs.mPrefs.getAll();
    
    for (int key : PREFERENCE_KEYS) {
      String keyName = context.getString(key);
      Object value = map.get(keyName);
      if (key == R.string.pref_last_gcm_event_time_key){
        try {
          long time = (Long)value;
          if (time > 0) {
            value = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS").format(time);
          }
        } catch (Exception ex) {
          value = null;
        }
      }
      sb.append(String.format("%s: %s\n", keyName, value));
    }

    // Add locale info
    sb.append(String.format("locale: %s\n",
        context.getResources().getConfiguration().locale.getDisplayName()));
    
    // Add permission info
    PermissionManager.addPermissionInfo(context, sb);
    
    // Add Vendor config info
    VendorManager.instance().addStatusInfo(sb);
  }
  
  private static final int PERM_REQ_INITIAL = 1;
  private static final int PERM_REQ_SMS_MMS = 2;
  private static final int PERM_REQ_REPORT_POSITION = 3;
  private static final int PERM_REQ_RESP_TYPE_BTN1 = 5;
//  private static final int PERM_REQ_RESP_TYPE_BTN2 = 6;
//  private static final int PERM_REQ_RESP_TYPE_BTN3 = 7;
//  private static final int PERM_REQ_RESP_TYPE_BTN4 = 8;
//  private static final int PERM_REQ_RESP_TYPE_BTN5 = 9;
//  private static final int PERM_REQ_RESP_TYPE_BTN6 = 10;
  private static final int PERM_REQ_NO_SHOW_IN_CALL = 11;
  private static final int PERM_REQ_PHONE_INFO = 12;
  private static final int PERM_REQ_LOCATION_TRACKING = 13;
  private static final int PERM_REQ_USER_ALERT_SOUND = 14;
  private static final int PERM_REQ_NOTIFY_ABORT = 15;
  private static final int PERM_APP_MAP_OPTION = 16;
  private static final int PERM_REQ_LIMIT = 16;
  
  private static final PermissionChecker[] checkers = new PermissionChecker[PERM_REQ_LIMIT];

  
  private static PermissionManager permMgr = null;
  private static final Stack<PermissionManager> permMgrStack = new Stack<>();

  /**
   * Set the current permission manager, should be called by the onCreate() method of
   * any activity that might call some our permission checking methods
   * @param permMgr current permission manager
   */
  public static void setPermissionManager(PermissionManager permMgr) {
    ManagePreferences.permMgr = permMgr;
    permMgrStack.push(permMgr);
  }

  /**
   * @return current permission manager
   */
  public static PermissionManager getPermissionManager() {
    return permMgr;
  }
  
  /**
   * Release current permission manager
   * this should be called in the onDestroy() method of any Activity that previously
   * called setPermissionManager
   * @param myPermMgr permission manager being released
   */
  public static void releasePermissionManager(PermissionManager myPermMgr) {
    permMgrStack.remove(myPermMgr);
    if (permMgrStack.empty()) {
      permMgr = null;
    } else {
      permMgr = permMgrStack.peek();
    }
  }
  
  /**
   * @return true if permission system has been properly enabled and
   * can be invoked.  The only time this may not be true is during app
   * initialization before the Call history activity is initialized.
   */
  private static boolean isPermissionsInitialized() {
    return permMgr != null;
  }
  
  /********************************************************************
   * Initial permission checking (checks all of the other preference permissions)
   ********************************************************************/

  private static boolean initialPermissionsChecked = false;
  public static void checkInitialPermissions(Runnable run) {

    // This can not be called multiple times when Cadpage is launched seccsessively.  Since it is not
    // possible for permissions be be revoked while the app is running (it is killed if they are)
    // we only need to make this rather complicated check one time.
    if (initialPermissionsChecked) {
      if (run != null) run.run();
    } else {
      initialPermissionsChecked = true;
      initialPermissionChecker.check(true, run);
    }
  }
  
  private static final InitialPermissionChecker initialPermissionChecker = new InitialPermissionChecker();
  
  @SuppressWarnings("WeakerAccess")
  public static class InitialPermissionChecker extends PermissionChecker {

    Runnable run;

    // List of checkers that reported permission failures
    List<PermissionChecker> failedCheckers = null;
    
    public InitialPermissionChecker() {
      super(PERM_REQ_INITIAL);
    }

    public void check(boolean b, Runnable run) {
      this.run = run;
      super.check(b);
    }

    @Override
    public void checkPermission() {

      failedCheckers = new ArrayList<>();

      // Loop through all the defined permission checkers
      for (PermissionChecker checker : checkers) {
        
        // We are only interested in the preference permission checkers
        if (checker instanceof PrefPermissionChecker) {
        
          // If it failed, add it to the failed checker list
          PrefPermissionChecker<?,?> pchecker = (PrefPermissionChecker<?,?>)checker;
          if (!pchecker.check()) failedCheckers.add(pchecker);
        }
        
        // OK, we are also interested in the account permissions, which has to handle
        // two specific situations
        else if (checker instanceof PhoneInfoChecker) {
          final Context context = CadPageApplication.getContext();

          // First, if Cadpage has been restored from a backup, we need READ_PHONE_STATE permission
          // to retrieve the MEID number so we can tell if we have been restored to a different
          // device
          boolean checkTransferStatus = false;
          int explainId = 0;
          String oldMeid = null;
          String transferStatus = transferStatus();
          if (transferStatus.equals("X")) {

            // If we are not registered with a direct paging vendor, just turn the transfer status off
            if (!VendorManager.instance().isRegistered()) {
              setTransferStatus("N");
            } else {

              // If we do not have an old MEID to compare things to, we cannot tell if the MEID
              // has changed, so just assume that it has
              oldMeid = prefs.getString(R.string.pref_prev_meid_key, null);
              if (!isValidMEID(oldMeid))  {
                setTransferStatus("Y");
              }

              // Otherwise we will have to wait until READ_PHONE_STATE permission has been requested
              else {
                checkTransferStatus = true;
                explainId = R.string.perm_acct_info_for_restore;
              }

              // If we resolved the ambiguous transfer status, send it to the direct paging vendors
              if (!checkTransferStatus) {
                VendorManager.instance().reconnect(CadPageApplication.getContext(), false);
              }
            }
          }

          // If we have decided that we do not need to check the transfer status, we can reset
          // the old MEID value now
          if (!checkTransferStatus) isNewDevice(oldMeid);

          // The second case is if we are about to perform an automatic payment status recalculation
          final boolean checkPaymentStatus = DonationManager.instance().checkPaymentStatus(context);
          if (checkPaymentStatus) explainId = R.string.perm_acct_info_for_auto_recalc;

          // OK, If we determined that we need the READ_PHONE_STATE permission, let's ask for it
          if (checkTransferStatus || checkPaymentStatus) {
            final boolean checkTransferStatus2 = checkTransferStatus;
            final String oldMeid2 = oldMeid;
            PhoneInfoChecker aChecker = (PhoneInfoChecker)checker;
            if (!aChecker.check(new PermissionAction(){
              @Override
              public void run(boolean ok, String[] permissions, int[] granted) {

                // Somewhat surprising, we do not care whether it was granted or not
                // If we needed to resolve the transfer status, do so and send it to the vendors
                if (checkTransferStatus2) {
                  setTransferStatus(isNewDevice(oldMeid2) ? "Y" : "R");
                  VendorManager.instance().reconnect(CadPageApplication.getContext(), false);
                }

                // and if a payment status recalcluation is pending, go do it
                if (checkPaymentStatus) {
                  BillingManager.instance().restoreTransactions(context);
                  ManagePreferences.setAuthLastCheckTime();
                }
              }
            }, explainId, false)) {
              failedCheckers.add(aChecker);
            }
          }
        }
      }
      
      // No failures, life is good
      if (failedCheckers.isEmpty()) {
        failedCheckers = null;
        if (run != null) run.run();
        return;
      }

      // Otherwise run through all of the failed checkers and accumulate
      // their missing permissions in our requested permission list
      for (PermissionChecker checker : failedCheckers) {
        List<String>reqPerms = checker.getReqPermissions();
        List<Integer>reqExpIds = checker.getReqExplainIds();
        for (int ndx = 0; ndx<reqPerms.size(); ndx++) {
          requestPermission(reqPerms.get(ndx), reqExpIds.get(ndx));
        }
      }
    }

    private boolean isNewDevice(String oldMeid) {
      String meid = UserAcctManager.instance().getMEID();
      if (!isValidMEID(meid)) return true;
      boolean newDevice = !meid.equals(oldMeid);
      if (newDevice) prefs.putString(R.string.pref_prev_meid_key, meid);
      return newDevice;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isValidMEID(String meid) {
      return meid != null && meid.length() > 0 && !meid.equalsIgnoreCase("unknown");
    }

    @Override
    public void onRequestPermissionResult(String[] permissions, int[] granted) {
      
      if (failedCheckers == null) return;
 
      // When we get the final permission granted result
      // Loop back through all of the failed permission checkers reported
      // the granted status for the permissions they requested
      for (PermissionChecker checker : failedCheckers) {
        List<String> permList = checker.getReqPermissions();

        // There is absolutely no possible way that permList can be null.
        // Yet I have seen it happen twice in developer testing but still cannot
        // reproduce it  Arrgh!!!!!!!!!!!
        if (permList == null) {
          Log.e(checker.toString() + "returned null permission list");
          continue;
        }
        String[] perms = permList.toArray(new String[permList.size()]);
        int[] stats = new int[perms.length];
        for (int ndx = 0; ndx<perms.length; ndx++) {
          String perm = perms[ndx];
          for (int ndx2 = 0; ndx2 < permissions.length; ndx2++) {
            if (perm.equals(permissions[ndx2])) {
              stats[ndx] = granted[ndx2];
              break;
            }
          }
        }
        checker.onRequestPermissionResult(perms, stats);
      }
      
      failedCheckers = null;
      if (run != null) run.run();
    }
  }
  
  /********************************************************************
   * Permission checking the enable Msg Type preference
   ********************************************************************/
  public static boolean checkPermEnableMsgType(ListPreference pref, String value) {
    return enableMsgTypeChecker.check(pref, value);
  }
  public static boolean setEnableMsgType(String value, Runnable run) {
    return enableMsgTypeChecker.setValue(null, value, run);
  }
  private static final EnableMsgTypeChecker enableMsgTypeChecker = new EnableMsgTypeChecker();
  
  private static class EnableMsgTypeChecker extends ListPermissionChecker {
    
    public EnableMsgTypeChecker() {
      super(PERM_REQ_SMS_MMS, R.string.pref_enable_msg_type_key);
    }

    @Override
    protected String checkPermission(String value) {

      // If this variant of Cadpage is not allowed to access messages, there is no
      // requirement that the permission be granted here
      if (!BuildConfig.MSG_ALLOWED) return value;
      
      // This gets tricky because one or both permissions may be needed
      // if the values contains an S, we need RECEIVE_SMS permission
      // if the value contains an M, we need RECEIVE_MMS and READ_SMS permission
      value = check(value, "S", PermissionManager.RECEIVE_SMS);
      value = check(value, "M", PermissionManager.READ_SMS, PermissionManager.RECEIVE_MMS);
      return value;
    }
    
    private String check(String value, String code, String ... reqPerms) {
      if (value.contains(code)) {
        boolean missingPerm = false;
        for (String reqPerm : reqPerms) {
          if (!checkRequestPermission(reqPerm)) missingPerm = true;
        }
        if (missingPerm) value = value.replace(code, "");
      }
      return value;
    }
  }
  
  /********************************************************************
   * Permission checking the report position preference
   ********************************************************************/
  public static boolean checkReportPosition(ListPreference pref, String value) {
    return reportPositionChecker.check(pref, value);
  }
  
  private static final ReportPositionChecker reportPositionChecker = new ReportPositionChecker();
  
  @SuppressWarnings("WeakerAccess")
  private static class ReportPositionChecker extends ListPermissionChecker {
    
    public ReportPositionChecker() {
      super(PERM_REQ_REPORT_POSITION, R.string.pref_report_position_key);
    }

    @Override
    protected String checkPermission(String value) {
      
      // A Y (always) value required the ACCESS_FINE_LOCATION permission
      // A A (ask) value will request the permission only when the users requests location tracking
      if (!value.equals("Y")) return null;

      // We need to check two permissions, and cannot use the && shortcut because the checkRequestPermission
      // method has necessary side effects requiring that both calls be made
      boolean g1 = checkRequestPermission(PermissionManager.ACCESS_FINE_LOCATION);
      boolean g2 = checkRequestPermission(PermissionManager.ACCESS_BACKGROUND_LOCATION);
      return g1 && g2 ? null : "A";
    }
  }
  
  /********************************************************************
   * Permission checking the response button type preference
   ********************************************************************/
  public static boolean checkResponseType(int button, ListPreference pref, String value) {
    return responseTypeCheckers[button-1].check(pref, value);
  }
  
  // List of preference checkers for the 6 response type buttons
  private static final ResponseTypePermissionChecker[] responseTypeCheckers = new ResponseTypePermissionChecker[]{
      new ResponseTypePermissionChecker(1),
      new ResponseTypePermissionChecker(2),
      new ResponseTypePermissionChecker(3),
      new ResponseTypePermissionChecker(4),
      new ResponseTypePermissionChecker(5),
      new ResponseTypePermissionChecker(6)
  };
  
  private static class ResponseTypePermissionChecker extends ListPermissionChecker {
    
    public ResponseTypePermissionChecker(int button) {
      super(PERM_REQ_RESP_TYPE_BTN1-1+button, CALLBACK_TYPE_IDS[button-1]);
    }

    @Override
    protected String checkPermission(String value) {

      // If we are not allowed to initiate phone calls, there is nothing to check
      if (!BuildConfig.MSG_ALLOWED) return null;

      // A value of P requires CALL_PHONE permission
      // A value of T requires SMS_SEND permission
      String reqPerm = value.equals("P") ? PermissionManager.CALL_PHONE :
                       value.equals("T") ? PermissionManager.SEND_SMS : null;
      if (reqPerm == null) return null;
      return checkRequestPermission(reqPerm) ? null : "";
    }
  }
  
  /********************************************************************
   * Permission checking the no show in call preference
   ********************************************************************/
  public static boolean checkNoShowInCall(TwoStatePreference pref, boolean value) {
    return noShowInCallChecker.check(pref, value);
  }
  
  private static final NoShowInCallChecker noShowInCallChecker = new NoShowInCallChecker();
  
  private static class NoShowInCallChecker extends CheckBoxPermissionChecker {
    
    public NoShowInCallChecker() {
      super(PERM_REQ_NO_SHOW_IN_CALL, R.string.pref_noShowInCall_key);
    }

    @Override
    protected Boolean checkPermission(Boolean value) {
      
      // true value requires READ_PHONE_STATE permission
      if (!value) return null;
      if (checkRequestPermission(PermissionManager.READ_PHONE_STATE)) return null;
      return false;
    }
  }
  
  /********************************************************************
   * Permission checking the user selectable sound override setting
   ********************************************************************/
  public static boolean checkOverrideNotifySound(TwoStatePreference pref, Boolean value) {
    return overrideNotifySoundChecker.check(pref, value);
  }
  
  public static boolean checkOverrideNotifySound(TwoStatePreference pref) {
    return overrideNotifySoundChecker.check(pref);
  }
  
  private static final OverrideNotifySoundChecker overrideNotifySoundChecker = new OverrideNotifySoundChecker();
  
  private static class OverrideNotifySoundChecker extends CheckBoxPermissionChecker {
    
    public OverrideNotifySoundChecker() {
      super(PERM_REQ_USER_ALERT_SOUND, R.string.pref_notif_override_sound_key);
    }

    @Override
    protected Boolean checkPermission(Boolean value) {
      
      // None of this matters if notification override is not set
      if (!notifyOverride()) return null;
      
      // false value requires READ_EXTERNAL_STORAGE permission
      // because it may need to read audio alert files on external storage
      if (value) return null;
      if (checkRequestPermission(PermissionManager.READ_EXTERNAL_STORAGE, R.string.perm_user_sound_override)) return null;
      return true;
    }
  }

  /********************************************************************
   * Permission checking when the system will not play override sound without external storage permission
   * This not a user adjustable setting, it gets set when the Android system throws a security exception
   * when we try to show a notification.  It is only checked during the initialization permision check
   ********************************************************************/

  static {
    new NotifyAbortChecker();
  }
  private static class NotifyAbortChecker extends CheckBoxPermissionChecker {

    public NotifyAbortChecker() {
      super(PERM_REQ_NOTIFY_ABORT, R.string.pref_notif_abort_key);
    }

    @Override
    protected Boolean checkPermission(Boolean value) {

      // If notifications are not enabled, no problem
      if (!notifyEnabled()) return null;

      // true value requires READ_EXTERNAL_STORAGE permission
      // because it may need to read audio alert files on external storage
      if (!value) return null;
      if (checkRequestPermission(PermissionManager.READ_EXTERNAL_STORAGE, R.string.perm_notify_abort)) return null;
      return false;
    }
  }

  /********************************************************************
   * Permission checking the map app option preference
   ********************************************************************/
  public static boolean checkAppMapOption(ListPreference pref, String value) {
    return appMapOptionChecker.check(pref, value);
  }

  private static final AppMapOptionChecker appMapOptionChecker = new AppMapOptionChecker();

  @SuppressWarnings("WeakerAccess")
  private static class AppMapOptionChecker extends ListPermissionChecker {

    public AppMapOptionChecker() {
      super(PERM_APP_MAP_OPTION, R.string.pref_app_map_option_key);
    }

    @Override
    protected String checkPermission(String value) {

      if (!value.equals("OsmAnd")) return null;
      return checkRequestPermission(PermissionManager.ACCESS_FINE_LOCATION) ? null : "Google";
    }
  }


  /********************************************************************************
   * Generic permission checker used to handle ListPreference preference values
   *********************************************************************************/
  
  private abstract static class ListPermissionChecker extends StringPermissionChecker<ListPreference> {
    
    protected ListPermissionChecker(int permReq, int resPrefId) {
      super(permReq, resPrefId);
    }
    
    @Override
    protected String getPreference(ListPreference preference) {
      return preference.getValue();
    }

    @Override
    protected void setPreference(ListPreference preference, String value) {
      preference.setValue(value);
    }
  }
  
  /********************************************************************************
   * Generic permission checker used to handle TwoStatePreference preference values
   *********************************************************************************/
  
  private abstract static class CheckBoxPermissionChecker extends BooleanPermissionChecker<TwoStatePreference> {
    
    protected CheckBoxPermissionChecker(int permReq, int resPrefId) {
      super(permReq, resPrefId);
    }

    @Override
    protected Boolean getPreference(TwoStatePreference preference) {
      return preference.isChecked();
    }

    @Override
    protected void setPreference(TwoStatePreference preference, Boolean value) {
      preference.setChecked(value);
    }
  }
  
  /******************************************************************
   * Generic permission checker used to adjust String setting values
   ******************************************************************/ 
  private abstract static class StringPermissionChecker<P extends Preference> extends PrefPermissionChecker<String, P> {
    
    protected StringPermissionChecker(int permReq, int resPrefId) {
      super(permReq, resPrefId);
    }

    @Override
    protected String getPrefValue(int resPrefId) {
      return prefs.getString(resPrefId);
    }

    @Override
    protected void savePrefValue(int resPrefId, String value) {
      prefs.putString(resPrefId, value);
    }
  }
  
  /******************************************************************
   * Generic permission checker used to adjust Boolean setting values
   ******************************************************************/ 
  private abstract static class BooleanPermissionChecker<P extends Preference> extends PrefPermissionChecker<Boolean, P> {
    
    protected BooleanPermissionChecker(int permReq, int resPrefId) {
      super(permReq, resPrefId);
    }

    @Override
    protected Boolean getPrefValue(int resPrefId) {
      return prefs.getBoolean(resPrefId);
    }

    @Override
    protected void savePrefValue(int resPrefId, Boolean value) {
      prefs.putBoolean(resPrefId, value);
    }
  }

  /* *****************************************************************/

  /**
   * Generic permission checker tied to particular values of a setting
   * V - type of setting value
   * P - Preference subclass that displays and sets this value
   */
  
  private abstract static class PrefPermissionChecker<V, P extends Preference>  extends PermissionChecker {
    private final int resPrefId;
    private P preference;
    private V value;
    private V newValue;
    private Runnable run;
    private boolean recurseCheck = false;
    
    /**
     * Constructor
     * @param permReq Permission request ID
     * @param resPrefId Preference resource ID
     */
    protected PrefPermissionChecker(int permReq, int resPrefId) {
      super(permReq);
      this.resPrefId = resPrefId;
    }
    
    /**
     * Called during initialization to check that permissions required for
     * current setting value have all been granted 
     * @return true everything is good to go, false if a permission needs to be granted
     */
    public boolean check() {

      // There is no Preference during startup
      // but get and save the current preference value
      this.preference = null;
      this.value = getPrefValue(resPrefId);
      this.run = null;

      // Make the primary permission check without displaying a user request
      // screen.  If there is a missing preference, set the setting to the
      // value that is permitted under current permissions
      if (!super.check(false)) {
        savePrefValue(resPrefId, newValue);
        return false;
      }
      return true;
    }

    /**
     * Called when the user attempts to change a value from the
     * configuration settings screen
     * @param preference Preference object used to change setting value
     * @param value new requested value of setting
     * @return true if setting change should be accepted, false if it
     * should be rejected
     */
    public boolean check(P preference, V value) {

      // If we are being called recursively, always return true
      // This may happen when a permission failure requires that we set a permission to some
      // default value, which turns around and call the check() method recursively
      if (recurseCheck) return true;
      recurseCheck = true;

      // Save the preference and requested values and
      // call the main check permission method, requesting
      // a user permission screen
      try {
        this.preference = preference;
        this.value = value;
        this.run = null;
        return super.check(true);
      } finally {
        recurseCheck = false;
      }
    }

    /**
     * Attempt to set preference value
     * @param pref preference controlling this value, or null if none
     * @param value requested new preference value
     * @return if value could be set immediately
     */
    public boolean setValue(P pref, V value) {
      return setValue(pref, value, null);
    }

    /**
     * Attempt to set preference value
     * @param pref preference controlling this value, or null if none
     * @param value requested new preference value
     * @param run runnable to be executed if value is set correctly
     * @return if value could be set immediately
     */
    public boolean setValue(P pref, V value, Runnable run) {
      this.preference = pref;
      this.value = value;
      this.run = run;

      if (super.check(true)) {
        savePrefValue(resPrefId, value);
        if (pref != null) setPreference(pref, value);
        if (run != null) run.run();
        return true;
      }
      return false;
    }

    /**
     * Called when user changes some other preference value which may affect
     * the validity of this preference setting
     * @param preference Preference object use to configure setting
     * @return true everything is good to go, false if a permission needs to be granted
     */
    public boolean check(P preference) {
      this.preference = preference;
      this.value = getPreference(preference);
      if (super.check(true)) return true;
      savePrefValue(resPrefId, newValue);
      setPreference(preference, newValue);
      return false;
    }
    
    @Override
    public void checkPermission() {
      newValue = checkPermission(value);
    }
    
    @Override
    public void onRequestPermissionResult(String [] permissions, int[] granted) {
      
      // Call the superclass method, which only resets things
      super.onRequestPermissionResult(permissions, granted);
      
      // If permissions were fully granted, accept the originally requested value
      V newValue = value;
      if (!PermissionManager.isGranted(granted)) {
        
        // If nothing was granted, or if there was only one permission requested
        // just return leaving the setting value unchanged
        if (permissions == null || granted == null || granted.length < 2) return;
        
        // Otherwise, make another call to checkPermission to see what setting
        // value is permitted now with the current permissions
        newValue = checkPermission(value);
        if (newValue == null) newValue = value;
      }
      
      // Change the setting value, and change the value displayed in the preference screen
      savePrefValue(resPrefId, newValue);
      if (preference != null) setPreference(preference, newValue);
      if (run != null) run.run();
    }
    
    /**
     * Abstract method to return the current preference setting value
     * @param resPrefId preference resource ID
     * @return the current value of the preference setting
     */
    protected abstract V getPrefValue(int resPrefId);

    /**
     * Abstract method to set the preference setting value
     * @param resPrefId preference resource ID
     * @param value new value to save in preference setting
     */
    protected abstract void savePrefValue(int resPrefId, V value);
    
    /**
     * Abstract method to retrieve screen preference value
     * @param preference Screen preference
     * @return current value of screen preference
     */
    protected abstract V getPreference(P preference);
    
    /**
     * Abstract method to set a Screen Preference value
     * @param preference Screen preference to be set
     * @param value new value to be displayed by preference screen
     */
    protected abstract void setPreference(P preference, V value);
    
    /**
     * Abstract method to check if setting value is permitted by currently
     * granted permissions
     * @param value the requested settings value  
     * @return the (possibly) adjusted setting value that is permitted
     * by the currently granted permissions, or null if the original value is OK
     */
    protected abstract V checkPermission(V value);
  }
  
  /*************************************************************************
   * Permission checking when user requests run time location tracking
   ************************************************************************/
  public static boolean checkPermLocationTracking(PermissionAction action) {
    return locationTrackingChecker.check(action);
  }
  
  private static final LocationTrackingChecker locationTrackingChecker = new LocationTrackingChecker();
  
  private static class LocationTrackingChecker extends ActionPermissionChecker {
    
    public LocationTrackingChecker() {
      super(PERM_REQ_LOCATION_TRACKING);
    }
    
    public boolean check(PermissionAction action) {
      return super.check(action, true);
    }

    @Override
    protected void checkPermission() {
      checkRequestPermission(PermissionManager.ACCESS_FINE_LOCATION);
      checkRequestPermission(PermissionManager.ACCESS_BACKGROUND_LOCATION);
    }
  }
  
  /*************************************************************************
   * Permission checking for all actions that require phone information
   ************************************************************************/
  public static boolean checkPermPhoneInfo(PermissionAction action, int explainId) {
    return accountInfoChecker.check(action, explainId, true);
  }

  private static final PhoneInfoChecker accountInfoChecker = new PhoneInfoChecker();
  
  private static class PhoneInfoChecker extends ActionPermissionChecker {

    private int explainId;

    public PhoneInfoChecker() {
      super(PERM_REQ_PHONE_INFO);
    }
    
    public boolean check(PermissionAction action, int explainId, boolean request) {
      this.explainId = explainId;
      return check(action, request);
    }

    @Override
    protected void checkPermission() {
      // checkRequestPermission(PermissionManager.READ_SMS, explainId);
      checkRequestPermission(PermissionManager.READ_PHONE_STATE, explainId);
    }

    @Override
    public void onRequestPermissionResult(String[] permissions, int[] granted) {
      
      // If any requested permission were granted, reset the user account information
      if (granted != null) {
        for (int g : granted) {
          if (g == PackageManager.PERMISSION_GRANTED) {
            UserAcctManager.instance().reset();
            break;
          }
        }
      }
      
      // Call the superclass method that will call the action callback
      super.onRequestPermissionResult(permissions, granted);
    }
  }
  
  /**********************************************************************************
   * Generic permission checker that is not tied to a specific preference value,
   * rather it is invoked whenever the user requests some action that requires some
   * specific permissions
   **********************************************************************************/
  private abstract static class ActionPermissionChecker extends PermissionChecker {
    
    private PermissionAction action;

    public ActionPermissionChecker(int permReq) {
      super(permReq);
    }
    
    public boolean check(PermissionAction action, boolean request) {
      this.action = action;
      
      // This might (rarely) be called before the call history screen is initialized
      // In which case we can not really request more permissions so we will just have
      // to run with what we have now
      if (!isPermissionsInitialized() || super.check(request)) {
        if (action != null) action.run(true, null, null);
        return true;
      }
      return false;
    }

    @Override
    public void onRequestPermissionResult(String[] permissions, int[] granted) {
      super.onRequestPermissionResult(permissions, granted);
      if (action != null) {
        action.run(PermissionManager.isGranted(granted), permissions, granted);
      }
    }
  }
  
  /**
   * Interface passing callback that will be invoked to perform Permission dependent action
   */
  @SuppressWarnings("unused")
  public interface PermissionAction {
    
    /**
     * Called when final permission status has been determined
     * @param ok true if all requested permissions have been granted
     * @param permissions list of requested permissions
     * @param granted list of requested permission statuses
     */
    void run(boolean ok, String[] permissions, int[] granted);
  }
  
  /*************************************************************************
   * This is the base level for all of the permission checker classes
   *************************************************************************/
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private abstract static class PermissionChecker {
    
    final int permReq;
    
    private List<String> reqPermissions;
    private List<Integer> reqExplainIds;

    /**
     * Constructor
     * @param permReq a unique assigned permission checker request number for this checker
     */
    public PermissionChecker(int permReq) {
      
      // User the request number to save this checker in the checkers array
      this.permReq = permReq;
      if (checkers[permReq-1] != null) {
        throw new RuntimeException("Duplicate permission checkers using request ID:" + permReq);
      }
      checkers[permReq-1] = this;
    }
    
    /**
     * This is called to perform the necessary permission checks
     * @param request true if user request for missing permissions should be displayed
     * @return true everything is good to go, false if a permission needs to be granted
     */
    public boolean check(boolean request) {
      
      // initialize arrays of requested permissions and explanation resource ID's
      reqPermissions = new ArrayList<>();
      reqExplainIds = new ArrayList<>();
      
      // Call the main permission checker.  This is supposed to call one of
      // the requestPermission() methods for any permissions that need to be requested
      checkPermission();
      
      // If any permissions need to be requested, make the request now
      if (reqPermissions.size() > 0) {
        if (request) requestPermissions();
        return false;
      }
      
      // Otherwise just return
      return true;
    }
    
    /**
     * Abstract method to perform the actual permission checks
     * It should call one of the requestPermission() methods for any
     * necessary permission that is not currently granted
     */
    protected abstract void checkPermission();
    
    /**
     * Convenience method that checks to see if specific permission has been
     * granted, and if it has not calls requestPermission to request it
     * @param reqPerm requested permission
     * @return true if permission is granted, false if not
     */
    protected boolean checkRequestPermission(String reqPerm) {
      return checkRequestPermission(reqPerm, 0);
    }
    
    /**
     * Convenience method that checks to see if specific permission has been
     * granted, and if it has not calls requestPermission to request it
     * @param reqPerm requested permission
     * @param explainId resource ID of text explaining why permission is needed
     * @return true if permission is granted, false if not
     */
    protected boolean checkRequestPermission(String reqPerm, int explainId) {
      if (permMgr.isGranted(reqPerm)) return true;
      requestPermission(reqPerm, explainId);
      return false;
    }
    
    /**
     * Called to request a currently ungranted permission
     * @param reqPerm permission requested
     */
    protected void requestPermission(String reqPerm) {
      requestPermission(reqPerm, 0);
    }
    
    /**
     * Called to request a currently ungranted permission
     * @param reqPerm permission requested
     * @param explainId resource Id that defines a text explanation for
     * why this permission is needed.  Can be zero in no explanation is
     * to be provided
     */
    protected void requestPermission(String reqPerm, int explainId) {
      if (reqPermissions != null) {
        int ndx = reqPermissions.indexOf(reqPerm);
        if (ndx >= 0) {
          if (reqExplainIds.get(ndx) == 0) reqExplainIds.set(ndx, explainId);
        } else {
          reqPermissions.add(reqPerm);
          reqExplainIds.add(explainId);
        }
      }
    }
    
    /** 
     * Issue the system call to actually request any required permissions from the end user
     */
    private void requestPermissions() {
      String[] perms = reqPermissions.toArray(new String[reqPermissions.size()]);
      int[] expIds = new int[reqExplainIds.size()];
      for (int j = 0; j<expIds.length; j++) expIds[j] = reqExplainIds.get(j);
      permMgr.request(permReq, perms, expIds);
    }
    
    /**
     * @return list of permissions that should be requested
     */
    public List<String> getReqPermissions() {
      return reqPermissions;
    }
    
    /**
     * @return list of explanation resource ID's corresponding to requested permissions
     */
    public List<Integer> getReqExplainIds() {
      return reqExplainIds;
    }
    
    /**
     * This is called to indicate the results of the permission request.  This method
     * doesn't do much other than reset the permission request arrays.  It will be
     * overridden in subclasses that have to actually do something
     * @param permissions list of permissions
     * @param granted list of permission statuses
     */
    public void onRequestPermissionResult(String [] permissions, int[] granted) {
      reqPermissions = null;
      reqExplainIds = null;
    }
  }
  
  /* *************************************************************************************************/
  
  /**
   * Called to indicate the results of a permission request
   * @param requestCode permission request code
   * @param permissions array of requested permissions
   * @param granted granted status of each permission requested
   * @return true if this was generated by one of our permission checkers,
   * false otherwise.
   */
  public static boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] granted) {

    // Find the corresponding PermissionChecker object and call it's onRequestPermissionResult method
    if (requestCode < 1 || requestCode > PERM_REQ_LIMIT) return false;
    PermissionChecker checker = checkers[requestCode-1];
    if (checker == null) return false;
    
    // If checker results have already been cleared, do not call again
    if (checker.getReqPermissions() != null) {
      checker.onRequestPermissionResult(permissions, granted);
    }
    return true;
  }
  
  private Context context;
  private SharedPreferences mPrefs;

  @SuppressWarnings("unused")
  private interface PreferenceChangeListener {
    void preferenceChanged(String key, Object newVal);
  }
  private final Map<String, PreferenceChangeListener>listenerMap =
      new HashMap<>();

  // Dummy default construct used to create a test preferences object for test purposes
  protected ManagePreferences() {}

  
  private ManagePreferences(Context context) {
    this.context = context;
    mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
  }

  PreferenceChangeListener msgSupportListener = (key, newVal) -> {
    if (SmsPopupUtils.checkMsgSupport(context) == 1) {
      PreferenceRestorableFragment.setPreferenceKey(key);
    }
  };

  private void armListeners() {
    mPrefs.registerOnSharedPreferenceChangeListener(this);

    registerListener(R.string.pref_enabled_key, (String key, Object newVal) -> {
      boolean enabled = (Boolean)newVal;
      String enableStr = (enabled ? enableMsgType() : "");
      SmsPopupUtils.enableSMSPopup(context, enableStr);
      CadPageWidget.update(context);
    });

    registerListener(R.string.pref_enable_msg_type_key, msgSupportListener);
    registerListener(R.string.pref_use_old_mms_key, msgSupportListener);
    for (int btn = 0; btn < CALLBACK_BUTTON_CNT; btn++) {
      registerListener(CALLBACK_TYPE_IDS[btn], msgSupportListener);
      registerListener(CALLBACK_TITLE_IDS[btn], msgSupportListener);
      registerListener(CALLBACK_CODE_IDS[btn], msgSupportListener);
    }

    registerListener(R.string.pref_notif_enabled_key, (String key, Object newVal) -> CadPageWidget.update(context));

    registerListener(R.string.pref_popup_enabled_key, (String key, Object newVal) -> {
      CadPageWidget.update(context);
      CheckPopupEvent.instance().launch(context);
    });

    registerListener(R.string.pref_show_history_address_key, (String key, Object newVal) -> SmsMessageQueue.getInstance().notifyDataChange());
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    notifyListeners(key);
  }

  private void notifyListeners(String key) {
    PreferenceChangeListener listener = listenerMap.get(key);
    if (listener != null) {
      listener.preferenceChanged(key, mPrefs.getAll().get(key));
    }
  }

  private void registerListener(int resId, PreferenceChangeListener listener) {
    listenerMap.put(context.getString(resId), listener);
  }
  
  private Object getPreference(int resPrefId) {
    return mPrefs.getAll().get(context.getString(resPrefId));
  }
  
  private boolean getBoolean(int resPrefId) {
    return mPrefs.getBoolean(context.getString(resPrefId), false);
  }
  
  private String getString(int resPrefId) {
    String result = mPrefs.getString(context.getString(resPrefId), null);
    if (result == null) result = "";
    return result;
  }

  private String getString(int resPrefId, int resDefaultId) {
    return mPrefs.getString(context.getString(resPrefId), context.getString(resDefaultId));
  }

  private String getString(int resPrefId, String defaultVal) {
    return mPrefs.getString(context.getString(resPrefId), defaultVal);
  }
  
  private int getIntValue(int resPrefId) {
    return getIntValue(resPrefId, 0);
  }
  
  private int getIntValue(int resPrefId, int defValue) {
    String val = getString(resPrefId, "");
    if (val.length() == 0) return defValue;
    return Integer.parseInt(val);
  }
  
  private int getInt(int resPrefId, int defValue) {
    return mPrefs.getInt(context.getString(resPrefId), defValue);
  }
  
  @SuppressWarnings("unused")
  protected int getInt(int resPrefId) {
    int result = mPrefs.getInt(context.getString(resPrefId), Integer.MAX_VALUE);
    if (result == Integer.MAX_VALUE) throw new RuntimeException("No configured preference value found");
    return result;
  }
  
  private long getLong(int resPrefId, long defValue) {
    return mPrefs.getLong(context.getString(resPrefId), defValue);
  }
  
  private long getLong(int resPrefId) {
    long result = mPrefs.getLong(context.getString(resPrefId), Long.MAX_VALUE);
    if (result == Long.MAX_VALUE) throw new RuntimeException("No configured preference value found");
    return result;
  }
  
  private float getFloat(int resPrefId, float defValue) {
    return mPrefs.getFloat(context.getString(resPrefId), defValue);
  }
  
  protected float getFloat(int resPrefId) {
    float result = getFloat(resPrefId, Float.MAX_VALUE);
    if (result == Float.MAX_VALUE) throw new RuntimeException("No configured preference value found");
    return result;
  }

  private void putBoolean(int resPrefId, boolean newVal) {
    SharedPreferences.Editor settings = mPrefs.edit();
    String key = context.getString(resPrefId);
    settings.putBoolean(key, newVal);
    settings.apply();
  }

  private void putString(int resPrefId, String newVal) {
    SharedPreferences.Editor settings = mPrefs.edit();
    String key = context.getString(resPrefId);
    settings.putString(key, newVal);
    settings.apply();
  }

  private void putInt(int resPrefId, int newVal) {
    SharedPreferences.Editor settings = mPrefs.edit();
    String key = context.getString(resPrefId);
    settings.putInt(key, newVal);
    settings.apply();
  }

  private void putLong(int resPrefId, long newVal) {
    SharedPreferences.Editor settings = mPrefs.edit();
    String key = context.getString(resPrefId);
    settings.putLong(key, newVal);
    settings.apply();
  }
  
  private void putFloat(int resPrefId, float newVal) {
    SharedPreferences.Editor settings = mPrefs.edit();
    String key = context.getString(resPrefId);
    settings.putFloat(key, newVal);
    settings.apply();
  }

  // Array of preference keys to include in email
  private static final int[] PREFERENCE_KEYS = {
      R.string.pref_screen_size_key,
      R.string.pref_initialized_key,
      R.string.pref_enabled_key,
      
      R.string.pref_location_key,
      R.string.pref_override_filter_key,
      R.string.pref_filter_key,
      R.string.pref_show_source_key,
      R.string.pref_override_default_key,
      R.string.pref_defcity_key,
      R.string.pref_defstate_key,
      R.string.pref_enable_msg_type_key,
      R.string.pref_use_old_mms_key,
      R.string.pref_timeout_key,
      R.string.pref_mms_timeout_key,
      R.string.pref_loglimit_key,
      R.string.pref_override_active911_split_key,
      R.string.pref_split_direct_page_key,
      R.string.pref_msgtimeout_key,
      R.string.pref_split_min_msg_key,
      R.string.pref_rev_msg_order_key,
      R.string.pref_mix_msg_order_key,
      R.string.pref_split_blank_ins_key,
      R.string.pref_split_chk_sender_key,
      R.string.pref_split_keep_lead_break_key,
      R.string.pref_suppress_dup_msg_key,
      R.string.pref_override_vendor_loc_key,
      R.string.pref_activate_scanner_key,
      R.string.pref_scanner_channel_key,
      R.string.pref_scanner_channel_app_node_key,
      R.string.pref_scanner_timeout_key,
      R.string.pref_publish_pages_key,
      
      R.string.pref_notif_enabled_key,
      R.string.pref_notif_override_key,
      R.string.pref_notif_override_volume_key,
      R.string.pref_notif_override_do_not_disturb_key,
      R.string.pref_notif_override_sound_key,
      R.string.pref_notif_override_loop_key,
      R.string.pref_notif_sound_key,
      R.string.pref_notif_repeat_interval_key,
      R.string.pref_notif_timeout_key,
      R.string.pref_notif_delay_key,
      R.string.pref_notif_req_ack_key,
      R.string.pref_notif_check_abort_key,
      R.string.pref_notif_abort_key,
      
      R.string.pref_vibrate_key,
      R.string.pref_vibrate_pattern_key,
      R.string.pref_vibrate_pattern_custom_key,
      
      R.string.pref_flashled_key,
      R.string.pref_flashled_color_key,
      R.string.pref_flashled_color_custom_key,
      R.string.pref_flashled_pattern_key,
      R.string.pref_flashled_pattern_custom_key,

      R.string.pref_history_limit_key,
      R.string.pref_delete_unopen_key,
      R.string.pref_map_network_chk_key,
      R.string.pref_gps_map_option_key,
      R.string.pref_app_map_option_key,
      R.string.pref_show_history_address_key,
      R.string.pref_navigate_map_key,
      R.string.pref_report_position_key,
      
      R.string.pref_popup_enabled_key,
      R.string.pref_noShowInCall_key,
      R.string.pref_passthrusms_key,
      
      R.string.pref_screen_on_key,
      R.string.pref_textsize_key,
      R.string.pref_dimscreen_key,
      R.string.pref_show_personal_key,
      
      R.string.pref_button1_key,
      R.string.pref_button2_key,
      R.string.pref_button3_key,
      R.string.pref_button4_key,
      R.string.pref_button5_key,
      R.string.pref_button6_key,
      
      R.string.pref_alt_map_button_key,
      R.string.pref_map_page_button_key,
      
      R.string.pref_resp_type_key,
      R.string.pref_resp_merge_key,
      R.string.pref_callback1_title_key,
      R.string.pref_callback1_key,
      R.string.pref_callback2_title_key,
      R.string.pref_callback2_key,
      R.string.pref_callback3_title_key,
      R.string.pref_callback3_key,
      R.string.pref_callback4_title_key,
      R.string.pref_callback4_key,
      R.string.pref_callback5_title_key,
      R.string.pref_callback5_key,
      R.string.pref_callback6_title_key,
      R.string.pref_callback6_key,
      
      R.string.pref_xtra_resp_button1_key,
      R.string.pref_xtra_resp_button2_key,
      R.string.pref_xtra_resp_button3_key,
      
      R.string.pref_gen_alert_option_key,
      R.string.pref_run_report_option_key,

      R.string.pref_paid_year_key,
      R.string.pref_install_date_key,
      R.string.pref_purchase_date_key,
      R.string.pref_free_rider_key,
      R.string.pref_sponsor_key,
      R.string.pref_sponsor_1_key,
      R.string.pref_sponsor_2_key,
      R.string.pref_sponsor_3_key,
      R.string.pref_sub_status_key,
      R.string.pref_sub_status_1_key,
      R.string.pref_free_sub_key,
      R.string.pref_auth_location_key,
      R.string.pref_auth_extra_date_key,
      R.string.pref_auth_extra_cnt_key,
      R.string.pref_auth_last_date_key,
      R.string.pref_auth_run_days_key,
      R.string.pref_auth_last_check_time_key,
      R.string.pref_paid_year_1_key,
      R.string.pref_purchase_date_1_key,
      R.string.pref_paid_year_2_key,
      R.string.pref_purchase_date_2_key,
      R.string.pref_paid_year_3_key,
      R.string.pref_purchase_date_3_key,
      R.string.pref_expire_date_key,
      
      R.string.pref_prev_version_code,
      R.string.pref_register_date_key,

      R.string.pref_last_loc_time_key,
      R.string.pref_last_loc_acc_key,
      
      R.string.pref_last_gcm_event_type_key,
      R.string.pref_last_gcm_event_time_key,
      R.string.pref_restore_vol,

      R.string.pref_prev_meid_key,
      R.string.pref_transfer_status_key,
      
      R.string.pref_no_map_gps_label,

      R.string.pref_billing_account_key,
      R.string.pref_force_popup
  };
}
