package net.anei.cadpage.donation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;







//import net.anei.cadpage.ContentQuery;
import net.anei.cadpage.BugReportGenerator;
import net.anei.cadpage.ContentQuery;
import net.anei.cadpage.FCMMessageService;
import net.anei.cadpage.Log;
import net.anei.cadpage.ManageBluetooth;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.SmsMmsMessage;
import net.anei.cadpage.SmsMsgLogBuffer;
import net.anei.cadpage.SmsPopupUtils;
import net.anei.cadpage.ManageUsb;
import net.anei.cadpage.SmsService;
import net.anei.cadpage.billing.BillingManager;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.PreferenceGroup;
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
      "DND granted"
  };
  
  private static final String[] valueList = new String[]{
    "100", "101", "102",
    "33", "1", "2", "3", "4", "5", "6", "7", "8", "9", "91", "92", "93", "10", "11", "12", "13", "14", "15", "16", "19", "20", "21", "22"
  };
  
  private class DeveloperListPreference extends ListPreference {
    
    private final Activity context;

    public DeveloperListPreference(Activity context) {
      super(context);
      this.context = context;
      setTitle("Developer Debug Tools");
      setSummary("Only available for developers");
      setEntries(entryList);
      setEntryValues(valueList);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
      super.onDialogClosed(positiveResult);
      if (!positiveResult) return;
      
      int val = Integer.parseInt(getValue().toString());
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
          setPaidYear(0);
          setPurchaseDate(-200, -1);
          ManagePreferences.setInstallDate(ManagePreferences.purchaseDate());
          ManagePreferences.setFreeSub(false);
          break;

        case 3:     // Stat: Donate warn
          ManagePreferences.setAuthRunDays(100);
          ManagePreferences.setFreeRider(false);
          ManagePreferences.setAuthLocation(null);
          setPaidYear(-1);
          setPurchaseDate(DonationManager.EXPIRE_WARN_DAYS-2, -3);
          ManagePreferences.setFreeSub(false);
          break;

        case 4:     // Stat: Donate Limbo
        case 5:     // Stat: Donate expire
          ManagePreferences.setAuthRunDays(100);
          ManagePreferences.setFreeRider(false);
          ManagePreferences.setAuthLocation(null);
          setPaidYear(-1);
          int dayDelta = (val == 4 ? 0 : -1);
          setPurchaseDate(dayDelta, -1, ManagePreferences.releaseDate());
          ManagePreferences.setFreeSub(false);
          ManagePreferences.setExpireDate(null);
          break;

        case 6:     // Stat: Demo
          ManagePreferences.setFreeRider(false);
          ManagePreferences.setAuthLocation(null);
          setPaidYear();
          ManagePreferences.setAuthRunDays(10);
          ManagePreferences.setFreeSub(false);
          break;

        case 7:     // Stat: Demo expired
          ManagePreferences.setFreeRider(false);
          ManagePreferences.setAuthLocation(null);
          setPaidYear();
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
          ContentQuery.query(context);
          break;

        case 12:     // Recent tasks
          ContentQuery.dumpRecentTasks(context);
          break;

        case 13:    // Roll last date
          ManagePreferences.rollLastAuthDate("01012000");
          break;

        case 14:    // Build test message
          SmsMmsMessage message =
            new SmsMmsMessage("GCM", "TEAT664", "111-MIN 5 TONY ST HENDERSON AUCKLAND. (XStr CENTRAL PARK DR/) .RETAINING WALL ON FIRE . (x-401538144 y-227185845) #F2010720",
                              System.currentTimeMillis(), "Vendor/NewZealandPager/Revised,Utility/General/Default", "Active911",
                              "AL30/8/10[=Not Responding;ResponseResp=Resp;ResponseArriv=Arriv;ResponseCancl=Cancl;ResponseAvail=Avail;ResponseUnvl=Unvl]",
                              "https://access.active911.com/interface/cadpage_api.php?q=anxgGg4OSW",
                              null, null, null);

          // Add to log buffer
          if (!SmsMsgLogBuffer.getInstance().add(message)) return;

          // See if the current parser will accept this as a CAD page
          boolean isPage = message.isPageMsg(SmsMmsMessage.PARSE_FLG_FORCE);

          // This should never happen,
          if (!isPage) return;

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
          Intent intent = new Intent(context, FCMMessageService.class);
          intent.setAction("com.google.android.c2dm.intent.RECEIVE");
          intent.putExtra("vendor", "CodeMessaging");

          intent.putExtra("content", "CALL: SICK PERSON \r\nBOX: 1-1 \r\nADDR: 601 FAIR HAVEN CT\r\nUNIT: A12 A108 CCPM11 \r\nCITY: Federalsburg        \nDST: MD");
          intent.putExtra("format", "Cadpage2");

          context.startService(intent);
          break;

        case 20:    // Throw exception to test crash reporting
          throw new RuntimeException("Test Exception Handling");

        case 21:    // Do not disturb
          intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
          context.startActivity(intent);
          break;

        case 22:    // DND Granted
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            String result = nm.isNotificationPolicyAccessGranted() ? "Yes" : "No";
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
          }
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
    }
    
    private void setPurchaseDate(int dayOffset, int yearOffset, Date baseDate) {
      Date date = calcDate(dayOffset, yearOffset, baseDate);
      ManagePreferences.setPurchaseDate(date);
      ManagePreferences.setPurchaseDate(2, date);
    }
    
    private void setPurchaseDate(int dayOffset, int yearOffset) {
      Date date = calcDate(dayOffset, yearOffset);
      ManagePreferences.setPurchaseDate(date);
      ManagePreferences.setPurchaseDate(2, date);
    }
    
    private Date calcDate(int dayOffset, int yearOffset) {
      return calcDate(dayOffset, yearOffset, null);
    }
    
    private Date calcDate(int dayOffset, int yearOffset, Date baseDate) {
      if (baseDate == null) baseDate = new Date();
      Calendar cal = new GregorianCalendar();
      cal.setTime(baseDate);
      cal.set(Calendar.HOUR, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      cal.add(Calendar.DAY_OF_YEAR, dayOffset);
      cal.add(Calendar.YEAR, yearOffset);
      return cal.getTime();
    }
    
    private void setPaidYear() {
      setPaidYear(Integer.MIN_VALUE);
    }
    
    private void setPaidYear(int yearOffset) {
      setPaidYear(yearOffset, null);
    }
    
    private void setPaidYear(int yearOffset, Date baseDate) {
      if (baseDate == null) baseDate = new Date();
      Calendar cal = new GregorianCalendar();
      cal.setTime(baseDate);
      int year = cal.get(Calendar.YEAR);
      year = yearOffset == Integer.MIN_VALUE ? 0 : year+yearOffset;
      ManagePreferences.setPaidYear(year);
      ManagePreferences.setPaidYear(2, year);
    }
    
  }
  
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMddyyyy");
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
