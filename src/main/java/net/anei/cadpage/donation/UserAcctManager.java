package net.anei.cadpage.donation;

import java.util.Arrays;
import java.util.regex.Pattern;

import net.anei.cadpage.Log;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.PermissionManager;
import net.anei.cadpage.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

public class UserAcctManager {

  private final Context context;
  private String phoneNumber = null;
  private boolean developer = false;

  private static final Pattern SDK_PTN = Pattern.compile(".*(?:^|[_\\W])sdk(?:$|[_\\W]).*");

  private UserAcctManager(Context context) {
    this.context = context;
    if (SDK_PTN.matcher(Build.PRODUCT).matches()) developer = true;
  }

  @SuppressLint({"MissingPermission", "HardwareIds"})
  public void reset() {

    phoneNumber = null;
    getPhoneNumber(context);

    // If we are running in an emulator, assume developer status
    if (SDK_PTN.matcher(Build.PRODUCT).matches()) {
      developer = true;
    }

    // Otherwise, see if first email is on our developer list
    else {
      String email = ManagePreferences.billingAccount();
      if (email != null) {
        String[] developers = context.getResources().getStringArray(R.array.donate_devel_list);
        developer = Arrays.asList(developers).contains(email);
      }
    }
  }

  /**
   * Clean extraneous stuff from user name / phone #
   * @param name user name/phone #
   * @return cleaned up user name/phone #
   */
  private String cleanName(String name) {
    int pt = name.indexOf('<');
    if (pt >= 0) name = name.substring(0, pt);
    name = name.replace("-", "");
    if (name.startsWith(".")) name = name.substring(1);
    if (name.endsWith(".")) name = name.substring(0, name.length() - 1);
    return name;
  }

  /**
   * @return identified user account name
   */
  public String getUser() {
    return ManagePreferences.billingAccount();
  }

  @SuppressLint({"MissingPermission", "HardwareIds"})
  public String getPhoneNumber(Context context) {
    if (phoneNumber == null) {


      boolean readPhoneStatePerm = PermissionManager.isGranted(context, PermissionManager.READ_PHONE_STATE);
      TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
      assert tMgr != null;

      // Which permission is required to call getLine1Number() is inconsistent.  Nexus systems require READ_SMS
      // permission, Sprint phones require READ_PHONE_STATE.  Rather than try to figure out which one is
      // require, we will just call it can catch any thrown exceptions.
      try {
        phoneNumber = tMgr.getLine1Number();
      } catch (Exception ignored) {
      }

      if (phoneNumber == null && readPhoneStatePerm) {

        // One user one time reported a security exception abort with this operation
        // Should not have happened, but we will catch it just it case it happens again
        try {
          phoneNumber = tMgr.getVoiceMailNumber();
        } catch (SecurityException ex) {
          Log.e(ex);
        }
      }
      if (phoneNumber != null) {
        int pt = phoneNumber.indexOf(',');
        if (pt >= 0) phoneNumber = phoneNumber.substring(0, pt).trim();
        phoneNumber = cleanName(phoneNumber);
        if (phoneNumber.startsWith("+")) phoneNumber = phoneNumber.substring(1);
        if (phoneNumber.startsWith("1")) phoneNumber = phoneNumber.substring(1);
      }
    }
    return phoneNumber;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  @SuppressLint("HardwareIds")
  public String getMEID() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      //noinspection deprecation
      return android.os.Build.SERIAL;
    }
    try {
      return Build.getSerial();
    } catch (SecurityException ex) {
      return "unknown";
    }
  }
  
  /**
   * Append account information to support message under construction
   * @param sb String builder holding message being constructed
   */
  public void addAccountInfo(StringBuilder sb) {
    sb.append('\n');
    String email = ManagePreferences.billingAccount();
    if (email == null) {
      sb.append("\nNo active email addresses");
    } else {
      sb.append("\nUser:");
      sb.append(email);
    }
    sb.append('\n');
    sb.append("Phone:");
    sb.append(getPhoneNumber());
    sb.append("\nMEID:");
    sb.append(getMEID());
    int overpaidDays = DonationManager.instance().getOverpaidDays();
    if (overpaidDays > 0) {
      sb.append("\nOverpaid:");
      sb.append(overpaidDays);
    }
    sb.append("\nReqAuthServer:");
    sb.append(DonationManager.instance().reqAuthServer());
  }
  
  /**
   * @return true if current user is a developer
   */
  public boolean isDeveloper() {
    return developer;
  }

  @SuppressLint("StaticFieldLeak")
  private static UserAcctManager instance = null;
  
  public static void setup(Context context) {
    instance = new UserAcctManager(context.getApplicationContext());
  }
  
  public static UserAcctManager instance() {
    return instance;
  }
}
