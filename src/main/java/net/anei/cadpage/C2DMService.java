package net.anei.cadpage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import net.anei.cadpage.HttpService.HttpRequest;
import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.UserAcctManager;
import net.anei.cadpage.donation.DonationManager.DonationStatus;
import net.anei.cadpage.vendors.VendorManager;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.SystemClock;

public class C2DMService extends IntentService {
  
  // Minimum and maximum time periods we will delay before asking Google's overloaded server
  // for another registration ID
  private static final int INIT_REREGISTER_DELAY = 3000;  // 3 seconds
  private static final int MAX_REREGISTER_DELAY = 3600000;  // 1 Hour
  
  // Refresh ID timeout.  We will automatically request a new registration ID if
  // nothing is received for this amount of time
  private static final int REGISTER_LOCK_TIMEOUT = 60*1000;    // 1 min
  
  private static final String ACTION_RETRY_REGISTER = "net.anei.cadpage.RETRY_REGISTER";
  private static final String ACTION_ACTIVE911_REFRESH_ID = "net.anei.cadpage.ACTIVE911_REFRESH_ID";
  private static final String EXTRA_DELAY = "net.anei.cadpage.EXTRA_DELAY";
  private static final String EXTRA_MAX_DELAY = "net.anei.cadpage.EXTRA_MAX_DELAY";
  private static final String GCM_PROJECT_ID = "1027194726673";

  // wakelock
  private static PowerManager.WakeLock sWakeLock;

  private static final Random RANDOM = new Random();
  
  private static GoogleCloudMessaging gcm = null; 

  public C2DMService() {
    super("C2DMService");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    return Service.START_REDELIVER_INTENT;
  }

  @Override
  protected void onHandleIntent(Intent intent) {

    try {

      Log.v("C2DMService: onHandleIntent()");

      if (ACTION_RETRY_REGISTER.equals(intent.getAction())) {
        retryRegisterRequest(intent);
        return;
      }

      if (ACTION_ACTIVE911_REFRESH_ID.equals(intent.getAction())) {
        if (!VendorManager.instance().forceActive911Reregister(this)) {
          long delay = intent.getLongExtra(EXTRA_DELAY, -1);
          long maxDelay = intent.getLongExtra(EXTRA_MAX_DELAY, -1);
          if (delay > 0 && maxDelay > 0) {
            delay = Math.min(delay*2, maxDelay);
          }
          registerActive911(this, delay, maxDelay);
        }
        return;
      }
    }

    // Any exceptions that get thrown should be rethrown on the dispatch thread
    catch (final Exception ex) {
      TopExceptionHandler.reportException(ex);
    }
  }

  @Override
  public void onDestroy() {
    Log.v("Shutting down C2DMService");
    if (sWakeLock != null) sWakeLock.release();
  }

  
  /**
   * Request a new C2DM registration ID
   * @param context current context
   * @return true if register request was initiated, false if there is not
   * component to handle C2DM registrations
   */
  public static boolean register(Context context) {
    return register(context, false);
  }
  
  /**
   * Request a new C2DM registration ID
   * @param context current context
   * @param auto true if this is an automatically generated registration request
   * @return true if register request was initiated, false if there is no
   * component to handle C2DM registrations
   */
  public static boolean register(Context context, boolean auto) {
    Log.w("GCM Register Request");
    return startRegisterRequest(context, 1, auto);
  }

  /**
   * Request that current C2DM registration be dropped
   * @param context current context
   */
  public static boolean unregister(Context context) {
    Log.w("GCM Unregister request");
    ManagePreferences.setRegistrationId(null);
    boolean result = startRegisterRequest(context, 2, true);
    EmailDeveloperActivity.logSnapshot(context, "General Unregister Request");
    return result;
  }
  
  /**
   * Launch initial GCM register/unregister request
   * @param context current context
   * @param reqCode 1 - register, 2 - unregister
   * @param auto true if this is an automatically generated registration request
   * @return true if request was initiated by user
   */
  private static boolean startRegisterRequest(Context context, int reqCode, boolean auto) {
    
    if (reqCode == 1) resetRefreshIDTimer(context, "REGISTER");
    
    // Don't do anything if we already have an active ongoing request for this type
    if (!ManagePreferences.registerReqLock(reqCode, REGISTER_LOCK_TIMEOUT)) return true;
    
    // Launch the request
    return startRegisterRequest(context, reqCode, (auto ? INIT_REREGISTER_DELAY : 0));
  }

  private static void resetRefreshIDTimer(Context context, String register) {
  }

  /**
   * Launch initial or repeat GCM register/unregister request
   * @param context current context
   * @param reqCode 1 - register, 2 - unregister
   * @param delayMS If request fails for any reason, delay for this amount of time before issuing a second request
   * @return true if request was initiated
   */
  private static boolean startRegisterRequest(Context context, int reqCode, int delayMS) {

    if (Log.DEBUG) Log.v("startRegisterRequest:" + reqCode + " - " + delayMS);
    ManagePreferences.setReregisterDelay(delayMS);

    // Register and Unregister are blocking methods that must be run
    // off of the UI thread
    new AsyncTask<Integer, Void, String>() {
      @Override
      protected String doInBackground(Integer... parms) {
        try {
          switch (parms[0]) {
          case 1:
            String regid = getGCM().register(GCM_PROJECT_ID);
            return "REG:" + regid;

          case 2:
            getGCM().unregister();
            return "URG:";
          }
        } catch (IOException ex) {
          return "FAI:" + ex.getMessage();
        }
        return "???";
      }

      @Override
      protected void onPostExecute(String result) {
        if (result.startsWith("REG:")) {
          registrationSuccess(result.substring(4));
        } else if (result.startsWith("URG:")) {
          registrationCancelled();
        } else if (result.startsWith("FAI:")) {
          registrationFailure(result.substring(4));
        }
      }
    }.execute(reqCode);
    return true;
  }

  private static void registrationSuccess(String regId) {
    Log.w("C2DM registration succeeded: " + regId);
    boolean change = ManagePreferences.setRegistrationId(regId);
    ManagePreferences.registerReqRelease();
    VendorManager.instance().registerC2DMId(CadPageApplication.getContext(), change, regId);
  }

  private static void registrationCancelled() {
    Log.w("C2DM registration cancelled");
    Context context = CadPageApplication.getContext();
    ManagePreferences.setRegistrationId(null);
    ManagePreferences.registerReqRelease();
    VendorManager.instance().unregisterC2DMId(context);
    EmailDeveloperActivity.logSnapshot(context, "GCM Registration unregister report");
  }

  private static void registrationFailure(String error) {
    Log.w("C2DM registration failed: " + error);
    Context context = CadPageApplication.getContext();
    error = retryRegistration(error);
    if (error != null) {
      ManagePreferences.setRegistrationId(null);
      ManagePreferences.registerReqRelease();
      VendorManager.instance().failureC2DMId(context, error);
    }
    EmailDeveloperActivity.logSnapshot(context, "GCM Registration failure report");
  }

  private static synchronized GoogleCloudMessaging getGCM() {
    if (gcm == null) gcm = GoogleCloudMessaging.getInstance(CadPageApplication.getContext());
    return gcm;
  }
  
  /**
   * Called after a registration error has been reported
   * @param error
   * @return error status to be reported to user or null if request
   * has been rescheduled and no error status should be reported
   */
  private static String retryRegistration(String error) {
    
    // We can only recover from the SERVICE_NOT_AVAILABLE error
    // Lately PHONE_REGISTRATION_ERROR appears to be a recoverable error
    // But we can at least check to make sure there is an identifiable user account
    // Google is having problems with some systems returning AUTHENTICATION_FAILED status for unknown
    // reasons, so we will try to generate a bug report to help them out
    if (!error.equals("SERVICE_NOT_AVAILABLE")) {
      if (error.equals("AUTHENTICATION_FAILED")) BugReportGenerator.generate();
      return error;
    }
    
    // See if request should be rescheduled
    int req = ManagePreferences.registerReq();
    int delayMS = ManagePreferences.reregisterDelay();
    if (req == 0 || delayMS == 0) return error;

    // Since PHONE_REGISTRATION_ERROR isn't really recoverable, 
    // we will give up on it when we hit the maximum delay time
    if (delayMS == MAX_REREGISTER_DELAY && error.equals("PHONE_REGISTRATION_ERROR")) {
      Log.v("C2DMService terminating registration retries");
      ManagePreferences.setRegisterReq(0);
      ManagePreferences.setReregisterDelay(0);
      return error + "_HARD";
    }
    
    // If it should compute how long to delay before reissuing the request.  Since we are
    // potentially running in synch with thousands of other requesters trying to overload
    // Google's server, we add a randomizing factor into the actual delay time so we don't
    // all hit it at once.
    Context context = CadPageApplication.getContext();
    int realDelayMS = delayMS/2 + RANDOM.nextInt(delayMS); 
    Intent retryIntent = new Intent(ACTION_RETRY_REGISTER);
    retryIntent.setClass(context, C2DMRetryReceiver.class);
    PendingIntent retryPendingIntent = PendingIntent.getBroadcast(context, 0, retryIntent, 0);
    AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + realDelayMS, retryPendingIntent);
    Log.v("Rescheduling request in " + realDelayMS + " / " + delayMS + " msecs");
    ContentQuery.dumpIntent(retryIntent);
    return null;
  }

  /**
   * Called when the retry register event scheduled by retryRegistration() goes off.
   * This is where we actually do the followup registration request
   */
  private void retryRegisterRequest(Intent intent) {
    Log.w("Processing C2DM Retry request");
    ContentQuery.dumpIntent(intent);

    // Get the registration information
    int req = ManagePreferences.registerReq();
    int delayMS = ManagePreferences.reregisterDelay();
    if (req == 0 || delayMS == 0) return;
    
    // Double the delay that will be invoked if this request also fails
    // subject to an absolute maximum value
    delayMS *= 2;
    if (delayMS > MAX_REREGISTER_DELAY) delayMS = MAX_REREGISTER_DELAY;
    
    // Fire off the request
    startRegisterRequest(this, req, delayMS);
  }

  public static void registerActive911(Context context, long initDelay, long maxDelay) {

    long curTime = System.currentTimeMillis();

    Log.v("Scheduling Active911 refresh " + initDelay + " msecs");

    AlarmManager myAM = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    Intent refreshIntent = new Intent(context, C2DMRetryReceiver.class);
    refreshIntent.setAction(ACTION_ACTIVE911_REFRESH_ID);
    refreshIntent.putExtra(EXTRA_DELAY, initDelay);
    refreshIntent.putExtra(EXTRA_MAX_DELAY, maxDelay);

    PendingIntent refreshPendingIntent =
      PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    myAM.set(AlarmManager.RTC_WAKEUP, curTime+initDelay, refreshPendingIntent);
  }

  /**
   * Generate an Email message with the current registration ID
   * @param context current context
   */
  public static void emailRegistrationId(Context context) {
    
    // Build send email intent and launch it
    String type = "GCM";
    Intent intent = new Intent(Intent.ACTION_SEND);
    String emailSubject = CadPageApplication.getNameVersion() + " " + type + " registrion ID";
    intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
    intent.putExtra(Intent.EXTRA_TEXT, "My " + type + " registration ID is " + ManagePreferences.registrationId());
    intent.setType("message/rfc822");
    try {
      context.startActivity(Intent.createChooser(intent, context.getString(R.string.pref_email_title)));
    } catch (ActivityNotFoundException ex) {
      Log.e(ex);
    }
  }
}
