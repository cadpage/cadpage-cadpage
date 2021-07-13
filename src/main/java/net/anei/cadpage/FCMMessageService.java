package net.anei.cadpage;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.UserAcctManager;
import net.anei.cadpage.vendors.VendorManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class FCMMessageService extends FirebaseMessagingService {

  private static final int REFRESH_ID_TIMEOUT = 24*60*60*1000; // 1 day
  private static final String ACTION_REFRESH_ID = "net.anei.cadpage.FCMMessageService.REFRESH_ID";
  private static final String ACTION_ACTIVE911_REFRESH_ID = "net.anei.cadpage.FCMMessageService.ACTIVE911_REFRESH_ID";

  // Should probably be using this somewhere
  @SuppressWarnings("unused")
  private static final String GCM_PROJECT_ID = "1027194726673";


  @Override
  public void onCreate() {
    Log.v("FCMMessageService.onCreate()");
    super.onCreate();

    // Make sure everything is initialized
    if (!CadPageApplication.initialize(this)) return;
  }

  @Override
  public void onNewToken(String token) {

    Log.v("FCMMessageService:onNewToken()");
    VendorManager.instance().reconnect(getApplicationContext(), token,false);
  }

  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {

    Log.v("FCMMessageService:onMessageReceived()");
    Log.v("From: " + remoteMessage.getFrom());

    // If Cadpage is disabled, ignore incoming messages
    if (!ManagePreferences.enabled()) return;


    final Map<String, String> data = remoteMessage.getData();
    if (data == null) return;

    Log.v("Message data payload:" + data);

    // Get the vendor code
    String vendorCode = data.get("vendor");
    if (vendorCode == null) vendorCode = data.get("sponsor");

    // See what kind of message this is
    String type = data.get("type");
    if (type == null) type = "PAGE";

    // Get acknowledgment URL
    String ackURL = data.get("ack_url");


    // Ping just needs to be acknowledged
    if (type.equals("PING")) {
      sendAutoAck(ackURL, vendorCode);
      VendorManager.instance().checkVendorStatus(this, vendorCode);
      resetRefreshIDTimer("PING");
      return;
    }

    // Register and unregister requests are handled by VendorManager
    // which must be done on the main UI thread
    if (type.equals("REGISTER") || type.equals("UNREGISTER")) {
      final String type2 = type;
      final String vendorCode2 = vendorCode;
      final String account = data.get("account");
      final String token = data.get("token");
      final String dispatchEmail = data.get("dispatchEmail");
      CadPageApplication.runOnMainThread(new Runnable(){
        @Override
        public void run() {
          VendorManager.instance().vendorRequest(FCMMessageService.this, type2, vendorCode2, account, token, dispatchEmail);
        }
      });
      sendAutoAck(ackURL, vendorCode);
      resetRefreshIDTimer("VENDOR_" + type);
      return;
    }

    // Check vendor enabled status
    if (!VendorManager.instance().checkVendorStatus(this, vendorCode)) return;

    // Save timestamp
    final long timestamp = System.currentTimeMillis();

    // Retrieve message content from intent for from URL
    String content = data.get("content");
    if (content != null) {
      processContent(data, content, timestamp);
      sendAutoAck(ackURL, vendorCode);
      return;
    }

    String contentURL = data.get("content_url");
    if (contentURL != null) {
      HttpService.addHttpRequest(this, new HttpService.HttpRequest(Uri.parse(contentURL)){
        @Override
        public void processBody(String body) {
          FCMMessageService.this.processContent(data, body, timestamp);
        }
      });
      return;
    }
    Log.w("FCM message has no content");
  }

  private void processContent(Map<String, String> data, String content, long timestamp) {

    resetRefreshIDTimer("PAGE");

    // Reconstruct message from data from intent fields
    String from = data.get("sender");
    if (from == null) from = data.get("from");
    if (from == null) from = data.get("originally_from");
    if (from == null) from = "GCM";
    String subject = data.get("subject");
    if (subject == null) subject = "";
    String location = data.get("format");
    if (location != null && location.equals("unknown")) location = null;

    // Get vendor code
    String vendorCode = data.get("vendor");
    if (vendorCode == null) vendorCode = data.get("sponsor");

    // Whatever it is, update vendor contact time
    VendorManager.instance().updateLastContactTime(vendorCode, content);

    // Get the acknowledge URL and request code
    String ackURL = data.get("ack_url");
    String ackReq = data.get("ack_req");
    if (vendorCode == null && ackURL != null) {
      vendorCode = VendorManager.instance().findVendorCodeFromUrl(ackURL);
    }
    if (ackURL == null) ackReq = null;
    if (ackReq == null) ackReq = "";

    String callId = data.get("call_id");
    if (callId == null) callId = data.get("id");
    String serverTime = data.get("unix_time");
    if (serverTime ==  null) serverTime = data.get("unix_timestamp");
    if (serverTime == null) serverTime = data.get("date");
    // agency code = data.get("agency_code");
    String infoUrl = data.get("info_url");

    SmsMmsMessage msg = VendorManager.instance().getTestMessage(vendorCode, content);
    final SmsMmsMessage message = msg != null ? msg :
        new SmsMmsMessage(from, subject, content, timestamp,
                          location, vendorCode, ackReq, ackURL,
                          callId, serverTime, infoUrl);

    // Add to log buffer
    if (!SmsMsgLogBuffer.getInstance().add(message)) return;

    // If we are checking for split direct pages, pass this to the message accumulator
    // It will be responsible for calling SmsReceiver.processCadPage()
    if (message.getSplitMsgOptions().splitDirectPage()) {
      SmsMsgAccumulator.instance().addMsg(this, message, true);
    }

    // See if the current parser will accept this as a CAD page
    else {
      boolean isPage = message.isPageMsg(SmsMmsMessage.PARSE_FLG_FORCE);

      // This should never happen, 
      if (!isPage) return;

      // Process the message on the main thread
      SmsService.processCadPage(message);
    }
  }

  /**
   * Send auto acknowledgment when message is received
   * @param ackURL acknowledgment URL
   * @param vendorCode vendor code
   */
  private void sendAutoAck(String ackURL, String vendorCode) {
    sendResponseMsg(this, "", ackURL, "AUTO", vendorCode);
  }

  /**
   * send response messages
   * @param context current context
   * @param ackReq acknowledge request code
   * @param ackURL acknowledge URL
   * @param type request type to be sent
   */
  public static void sendResponseMsg(Context context, String ackReq, String ackURL, String type,
                                     String vendorCode) {
    if (ackURL == null) return;
    if (ackReq == null) ackReq = "";
    Uri.Builder bld = Uri.parse(ackURL).buildUpon().appendQueryParameter("type", type);

    // Add paid status if requested
    if (ackReq.contains("P")) {
      DonationManager.DonationStatus status = DonationManager.instance().status();
      String paid;
      String expireDate = null;
      if (status == DonationManager.DonationStatus.LIFE) {
        paid = "YES";
        expireDate = "LIFE";
      } else if (ManagePreferences.freeSub()) {
        paid = "NO";
      } else if (status == DonationManager.DonationStatus.PAID || status == DonationManager.DonationStatus.PAID_WARN) {
        paid = "YES";
        Date expDate = DonationManager.instance().expireDate();
        if (expDate != null) expireDate = DATE_FORMAT.format(expDate);
      } else {
        paid = "NO";
      }
      bld.appendQueryParameter("paid_status", paid);
      if (expireDate != null) bld.appendQueryParameter("paid_expire_date", expireDate);

      // also add phone number.  CodeMessaging wants this to identify users who
      // are getting text and direct pages
      String phone = UserAcctManager.instance().getPhoneNumber(context);
      if (phone != null) bld.appendQueryParameter("phone", phone);
    }

    // If a vendor code was specified, return status and version code associated with vendor
    VendorManager vm = VendorManager.instance();
    if (vendorCode != null) {
      if (!vm.isVendorDefined(vendorCode)) {
        bld.appendQueryParameter("vendor_status", "undefined");
      } else {
        bld.appendQueryParameter("vendor_status", vm.isRegistered(vendorCode) ? "registered" : "not_registered");
      }
    }

    // Add version code
    bld.appendQueryParameter("version", vm.getClientVersion(vendorCode));

    // Fix the server name if neccessary
    Uri uri = bld.build();
    uri = vm.fixServer(vendorCode, uri);

    // Send the request
    HttpService.addHttpRequest(context, new HttpService.HttpRequest(uri));
  }
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");


  /**
   * Reset the refresh registration ID timer, rescheduling the next refresh event
   * until REFRESH_ID_TIMEOUT msecs in the future
   * @param eventType Event type responsible for reset request
   */
  private static void resetRefreshIDTimer(String eventType) {

    long curTime = System.currentTimeMillis();
    ManagePreferences.setLastGcmEventType(eventType);
    ManagePreferences.setLastGcmEventTime(curTime);

    Log.v("Scheduling refresh event in " + REFRESH_ID_TIMEOUT + " msecs");
    OneTimeWorkRequest req =
        new OneTimeWorkRequest.Builder(RefreshIDWorker.class)
              .setInitialDelay(REFRESH_ID_TIMEOUT, TimeUnit.MILLISECONDS)
              .setConstraints(ManagePreferences.networkConstraint())
              .build();
    WorkManager mgr = WorkManager.getInstance();
    mgr.beginUniqueWork(ACTION_REFRESH_ID, ExistingWorkPolicy.REPLACE, req).enqueue();
  }

  @SuppressWarnings("WeakerAccess")  // CAN NOT BE PRIVATE!!!!
  public static class RefreshIDWorker extends Worker {

    public RefreshIDWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
      super(context, workerParams);
    }

    @NonNull
    public Result doWork() {
      Log.v("Refresh Direct Paging Registration");
      refreshID(getApplicationContext());
      return Result.success();
    }
  }

  /**
   * Called at startup to see if a scheduled refresh ID timer event is overdue.  In theory, this
   * should never happen.  But it has at least once, possibly because Cadpage was being updated
   * just when the timer event should have triggered.
   * @param context current context
   */
  public static void checkOverdueRefresh(Context context) {

    // This only happens if at least one direct paging vendor is enabled
    if (!VendorManager.instance().isRegistered()) return;

    // If we have gone past the time the last refresh event was scheduled, do it now
    long eventTime = ManagePreferences.lastGcmEventTime() + REFRESH_ID_TIMEOUT;
    if (System.currentTimeMillis() > eventTime) {
      Log.v("Perform overdue GCM refresh");
      refreshID(context);
    }
  }

  private static void refreshID(final Context context) {

    // Reset the refresh timer
    resetRefreshIDTimer("REFRESH");

    // There doesn't seem to be a way to do this anymore.  But if we ever figure it out
    // this is where it goes

    // But we do want to reconnect with each direct paging vendor
    VendorManager.instance().reconnect(context, false);
  }

  public static void registerActive911(Context context, long initDelay) {

    Log.v("Scheduling Active911 refresh " + initDelay + " msecs");

    OneTimeWorkRequest req =
        new OneTimeWorkRequest.Builder(RegisterActive911Worker.class)
            .setInitialDelay(initDelay, TimeUnit.MILLISECONDS)
            .setConstraints(ManagePreferences.networkConstraint())
            .build();
    WorkManager mgr = WorkManager.getInstance(context);
    mgr.beginUniqueWork(ACTION_ACTIVE911_REFRESH_ID, ExistingWorkPolicy.REPLACE, req).enqueue();
  }

  @SuppressWarnings("WeakerAccess")  // CAN NOT BE PRIVATE!!!!
  public static class RegisterActive911Worker extends Worker {

    public RegisterActive911Worker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
      super(context, workerParams);
    }

    @NonNull
    public Result doWork() {
      Log.v("Reconnect with Active911 service");
     VendorManager.instance().forceActive911Reregister(getApplicationContext());
     return Result.success();
    }
  }

  public static void resetInstanceId() {
    OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ResetIdWorker.class).build();
    WorkManager.getInstance().enqueue(req);
  }

  @SuppressWarnings("WeakerAccess")  // CAN NOT BE PRIVATE!!!!
  public static class ResetIdWorker extends Worker {

    public ResetIdWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
      super(context, workerParams);
    }

    @NonNull
    public Result doWork() {
      Log.v("Reset FCM instance ID");
      try {
        FirebaseInstanceId.getInstance().deleteInstanceId();
        Log.v("deleteInstanceId succeeded");
        return Result.success();
      } catch (IOException ex) {
        Log.e("DeleteInstanceId failed");
        Log.e(ex);
        return Result.failure();
      }
    }
  }


  /**
   * Generate an Email message with the current registration ID
   * @param context current context
   */
  public static void emailRegistrationId(final Context context) {

    getRegistrationId(new ProcessRegistrationId(){
      @Override
      public void run(String registrationId) {

        // Build send email intent and launch it
        String type = "GCM";
        Intent intent = new Intent(Intent.ACTION_SEND);
        String emailSubject = CadPageApplication.getNameVersion() + " " + type + " registration ID";
        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        intent.putExtra(Intent.EXTRA_TEXT, "My " + type + " registration ID is " + registrationId);
        intent.setType("message/rfc822");
        try {
          context.startActivity(Intent.createChooser(intent, context.getString(R.string.pref_email_title)));
        } catch (ActivityNotFoundException ex) {
          Log.e(ex);
        }

      }
    });
  }

  public interface ProcessRegistrationId {
    public void run(String registrationId);
  }

  public static void getRegistrationId(final ProcessRegistrationId regIdTask) {
    FirebaseInstanceId.getInstance().getInstanceId()
        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
          @Override
          public void onComplete(@NonNull Task<InstanceIdResult> task) {
            if (!task.isSuccessful()) {
              Log.e("getInstanceId failed", task.getException());
              return;
            }

            // Get new Instance ID token
            String registrationId = task.getResult().getToken();
            regIdTask.run(registrationId);
          }
        });
  }

}
