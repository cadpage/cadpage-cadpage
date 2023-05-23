package net.anei.cadpage.donation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.anei.cadpage.BugReportGenerator;
import net.anei.cadpage.ContentQuery;
import net.anei.cadpage.FCMMessageService;
import net.anei.cadpage.Log;
import net.anei.cadpage.ManageBluetooth;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.SmsMmsMessage;
import net.anei.cadpage.SmsMsgLogBuffer;
import net.anei.cadpage.ManageUsb;
import net.anei.cadpage.SmsService;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceGroup;
import android.provider.Settings;
import android.widget.Toast;

/**
 * Handles special debugging tools and dialogs available only to developers
 */
public class DeveloperToolsManager {
  
  // private constructor
  private DeveloperToolsManager() {}
  
  public boolean addPreference(Activity context, PreferenceGroup group) {
    if (!UserAcctManager.instance().isDeveloper()) return false;
    group.addPreference(new DeveloperListPreference(context));
    return true;
  }
  
  
  private static final String[] entryList = new String[]{
      "Probe USB",
      "Discover Bluetooth",
      "Probe Bluetooth",
      "FCM: Report",
      "Stat: Lifetime",
      "Stat: Donate paid",
      "Stat: Donate warn",
      "Stat: Donate limbo",
      "Stat: Donate expired",
      "Stat: Demo",
      "Stat: Demo expired",
      "Stat: Toggle free subscription",
      "Stat: Toggle sponsor",
      "Stat: Google Play Server",
      "Stat: Cadpage Auth Server",
      "Stat: Cycle Subscription Status",
      "Reset release info",
      "Content Query",
      "Recent Tasks",
      "Stat: Roll Last Date",
      "Build Test Message",
      "Status test",
      "Generate Bug Report",
      "Test FCM MSG",
      "Crash!!!",
      "Do Not Disturb",
      "DND granted",
      "Recheck notify abort",
      "Set notify abort",
      "Emulate factory reset"
  };
  
  private static final String[] valueList = new String[]{
    "100", "101", "102",
    "33", "1", "2", "3", "4", "5", "6", "7", "8", "9", "91", "92", "93", "10", "11", "12", "13", "14", "15", "16", "19", "20", "21", "22", "23", "24", "25"
  };
  
  @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
  private class DeveloperListPreference extends ListPreference {
    
    private final Activity context;

    public DeveloperListPreference(Activity context) {
      super(context);
      this.context = context;
      setKey("pref_developer_tools");
      setTitle("Developer Debug Tools");
      setSummary("Only available for developers");
      setEntries(entryList);
      setEntryValues(valueList);
    }

    @Override
    protected boolean persistString(String value) {

      int val = Integer.parseInt(getValue());
      Log.v("Developer option:" + val);
      switch (val) {

        case 1:     // Stat: Donate free
          ManagePreferences.setFreeRider(true);
          ManagePreferences.setFreeSub(false);
          ManagePreferences.setPaidYear(2, 9999);
          break;

        case 2:     // Stat: Donate paid
          ManagePreferences.setAuthRunDays(100);
          ManagePreferences.setFreeRider(false);
          ManagePreferences.setAuthLocation(null);
          setExpireDate(+90);
          ManagePreferences.setInstallDate(ManagePreferences.purchaseDate());
          ManagePreferences.setFreeSub(false);
          break;

        case 3:     // Stat: Donate warn
          ManagePreferences.setAuthRunDays(100);
          ManagePreferences.setFreeRider(false);
          ManagePreferences.setAuthLocation(null);
          setExpireDate(DonationManager.EXPIRE_WARN_DAYS-2);
          ManagePreferences.setFreeSub(false);
          break;

        case 4:     // Stat: Donate Limbo
        case 5:     // Stat: Donate expire
          ManagePreferences.setAuthRunDays(100);
          ManagePreferences.setFreeRider(false);
          ManagePreferences.setAuthLocation(null);
          int dayDelta = (val == 4 ? 0 : -1);
          setExpireDate(dayDelta, ManagePreferences.releaseDate());
          ManagePreferences.setFreeSub(false);
          ManagePreferences.setExpireDate(null);
          break;

        case 6:     // Stat: Demo
          ManagePreferences.setFreeRider(false);
          ManagePreferences.setAuthLocation(null);
          resetPaidYear();
          ManagePreferences.setAuthRunDays(10);
          ManagePreferences.setFreeSub(false);
          break;

        case 7:     // Stat: Demo expired
          ManagePreferences.setFreeRider(false);
          ManagePreferences.setAuthLocation(null);
          resetPaidYear();
          ManagePreferences.setAuthRunDays(DonationManager.DEMO_LIMIT_DAYS+1);
          ManagePreferences.setFreeSub(false);
          break;

        case 8:    // Stat: Toggle free subscriber
          ManagePreferences.setFreeSub(!ManagePreferences.freeSub());
          break;

        case 9:     // Stat: Toggle Sponsor
          String sponsor = ManagePreferences.sponsor();
          sponsor = (sponsor == null ? "Philomath Fire & Rescue" : null);
          ManagePreferences.setSponsor(sponsor);
          break;

        case 91:  // Stat: Google Play Server
          ManagePreferences.setPurchaseDateString(1, ManagePreferences.purchaseDateString());
          ManagePreferences.setPaidYear(1, ManagePreferences.paidYear());
          ManagePreferences.setPurchaseDateString(2, null);
          ManagePreferences.setPaidYear(2, 0);
          ManagePreferences.resetPreferenceVersion();
          break;

        case 92:  // State Cadpage authorization serve
          ManagePreferences.setPurchaseDateString(2, ManagePreferences.purchaseDateString());
          ManagePreferences.setPaidYear(2, ManagePreferences.paidYear());
          ManagePreferences.setPurchaseDateString(1, null);
          ManagePreferences.setPaidYear(1, 0);
          ManagePreferences.resetPreferenceVersion();
          break;

        case 93:  // Cycle subscription status
          int subStatus = ManagePreferences.subStatus();
          subStatus = (subStatus + 1) % 3;
          ManagePreferences.setSubStatus(subStatus);
          String dispStatus = new String[]{
              "No subscription",
              "Cancelled subscription",
              "Renewing subscription"
          }[subStatus];
          Log.v(dispStatus);
          break;

        case 10:     // Reset preference info
          ManagePreferences.resetPreferenceVersion();
          break;

        case 11:     // Content Query
          ContentQuery.dumpEverything(context);
          ContentQuery.query(context);
          break;

        case 12:     // Recent tasks
          ContentQuery.dumpRecentTasks(context);
          break;

        case 13:    // Roll last date
          ManagePreferences.rollLastAuthDate("01012000");
          break;

        case 14:    // Build test message
          SmsMmsMessage message = getTestMessage();

          // Add to log buffer
          if (!SmsMsgLogBuffer.getInstance().add(message)) return true;

          // See if the current parser will accept this as a CAD page
          boolean isPage = message.isPageMsg(SmsMmsMessage.PARSE_FLG_FORCE);

          // This should never happen,
          if (!isPage) return true;

          // Process the message
          SmsService.processCadPage(message);
          break;

        case 15:    // Situation specific status test
          ManagePreferences.setPaidYear(2019);
          ManagePreferences.setInstallDate(buildDate("07182018"));
          ManagePreferences.setPurchaseDateString("01102016");
          ManagePreferences.setFreeRider(false);
          ManagePreferences.setSponsor(null);
          ManagePreferences.setFreeSub(false);
          ManagePreferences.setAuthLocation(null);
          ManagePreferences.setAuthRunDays(1);
          ManagePreferences.setAuthLastCheckTime(0L);
          break;

        case 16:    // generate bug report
          BugReportGenerator.generate();
          break;

        case 19:    // Build a specific FCM page message
          break;

        case 20:    // Throw exception to test crash reporting
          throw new RuntimeException("Test Exception Handling");

        case 21:    // Do not disturb
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            context.startActivity(intent);
          }
          break;

        case 22:    // DND Granted
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            assert nm != null;
            String result = nm.isNotificationPolicyAccessGranted() ? "Yes" : "No";
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
          }
          break;

        case 23:    // Recheck notify abort
          ManagePreferences.setNotifyCheckAbort(false);
          break;

        case 24:    // Set notify abort
          ManagePreferences.setNotifyAbort(true);
          break;

        case 25:  // emulate factory reset recovery
          ManagePreferences.resetCheckFile();
          break;

        case 33:    // FCM: Report
          FCMMessageService.emailRegistrationId(context);
          break;

        case 100: // USB Probe
          ManageUsb.instance().probe(context);
          break;

        case 101: // Bluetooth discovery
          ManageBluetooth.instance().enableDiscovery(context);
          break;

        case 102: // Bluetooth probe
          ManageBluetooth.instance().probe(context);
          break;
      }
      DonationManager.instance().reset();
      MainDonateEvent.instance().refreshStatus();
      return true;
    }

    private void setExpireDate(int dayOffset) {
      setExpireDate(dayOffset, new Date());
    }

    private void setExpireDate(int dayOffset, Date baseDate) {
      Calendar cal = new GregorianCalendar();
      cal.setTime(baseDate);
      cal.set(Calendar.HOUR, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      cal.add(Calendar.DAY_OF_YEAR, dayOffset);
      cal.add(Calendar.YEAR, -1);

      int year = cal.get(Calendar.YEAR);
      ManagePreferences.setPaidYear(year);
      ManagePreferences.setPaidYear(2, year);

      Date date = cal.getTime();
      ManagePreferences.setPurchaseDate(date);
      ManagePreferences.setPurchaseDate(2, date);
    }

    private void resetPaidYear() {
      ManagePreferences.setPaidYear(0);
      ManagePreferences.setPaidYear(2, 0);
    }
  }

  public static SmsMmsMessage getTestMessage() {
    if (!UserAcctManager.instance().isDeveloper()) return null;
    return new SmsMmsMessage(
        "GCM",
        "",
        "SMALL MISC FIRE;;HWY 20/MP 41;;;PHILOMATH;2986;;BLF;BLDGT,ODFS,PHILO;Radio Channel: MP  [12/06/20 09:06:36 JONESM]\n;;4152462121;12/06/2020 09:05:16;2020190749;",
        System.currentTimeMillis(),
        "US/OR/Benton,Utility/General/Default",
        "Active911",
        "AL30/8/10[Not Responding;Response201=201;Response202=202;Response203=203;ResponseAck=Ack;ResponseUnvl=Unvl]",
        "https://access.active911.com/interface/cadpage_api.php?q=a13QeG3",
        "-1",
        null,
        "http://active911.com/a13QeG3");

  }

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMddyyyy");
  @SuppressWarnings("SameParameterValue")
  private Date buildDate(String dateStr) {
    try {
      return DATE_FORMAT.parse(dateStr);
    } catch (ParseException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static final DeveloperToolsManager instance = new DeveloperToolsManager();
  
  public static DeveloperToolsManager instance() {
    return instance;
  }
}
