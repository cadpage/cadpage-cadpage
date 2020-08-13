package net.anei.cadpage;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

@SuppressWarnings("RedundantIfStatement")
public class SmsService extends IntentService {

  // wakelock
  private static PowerManager.WakeLock sWakeLock;

  public SmsService() {
    super("SmsService");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    if (!BuildConfig.MSG_ALLOWED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForeground(1, ManageNotification.getMiscNotification(this, R.string.notify_sms_alert));
    }

    if (!CadPageApplication.initialize(this)) return Service.START_NOT_STICKY;
    if (flags != 0) holdPowerLock(this);

    super.onStartCommand(intent, flags, startId);
    return Service.START_REDELIVER_INTENT;
  }

  @Override
  protected void onHandleIntent(Intent intent) {

    try {
      Log.v("SmsService: onHandleIntent()");

      processIntent(this, intent);
    }

    // Any exceptions that get thrown should be rethrown on the dispatch thread
    catch (final Exception ex) {
      TopExceptionHandler.reportException(ex);
    }
  }

  @Override
  public void onDestroy() {
    Log.v("Shutting down SmsService");
    if (sWakeLock != null) sWakeLock.release();
  }

  /**
   * Called from the broadcast receiver.
   * <p>
   * Will process the received intent, call handleMessage(), registered(),
   * etc. in background threads, with a wake lock, while keeping the service
   * alive.
   */
  static void runIntentInService(Context context, Intent intent) {

    // Otherwise, hold a power lock for the duration and
    // start the service to handle the intent
    holdPowerLock(context);
    intent.setClass(context, SmsService.class);
    if (!BuildConfig.MSG_ALLOWED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent);
    } else {
      context.startService(intent);
    }

  }

  private static void holdPowerLock(Context context) {
    synchronized (SmsService.class) {
      if (sWakeLock == null) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Log.LOGTAG+".C2DMService");
        sWakeLock.setReferenceCounted(false);
      }
      if(!sWakeLock.isHeld()) sWakeLock.acquire();
    }
  }

  public static boolean processIntent(Context context, Intent intent) {

    // Not sure how a null intent gets in here, but it happened to one user
    // so now we check for it
    if (intent == null) return false;

    // Otherwise convert Intent into an SMS/MSS message
    SmsMmsMessage message = null;
    if (SmsReceiver.ACTION_SMS_RECEIVED.equals(intent.getAction())) {
      SmsMessage[] messages = getMessagesFromIntent(intent);
      if (messages == null) return false;
      message = new SmsMmsMessage( messages,System.currentTimeMillis());
      if (message.getMessageBody().length() == 0) return false;

      // See if this is a vendor discovery query.  If it is, make it go away
      if (message.isDiscoveryQuery(context)) return true;

      // Save message for future test or error reporting use
      // If message is rejected as duplicate, don't do anything except call
      // abortbroadcast to keep it from going to anyone else
      if (! SmsMsgLogBuffer.getInstance().add(message)) {
        return (! ManagePreferences.smspassthru());
      }
    }

    // If we didn't get a message, bail out
    if (message == null) return false;

    // If this was an incomplete MMS message picked up by the process last
    // message option, bail out
    if (message.getMessageBody() == null) return false;

    // Class 0 SMS, let the system handle this
    if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_SMS &&
      message.getMessageClass() == SmsMessage.MessageClass.CLASS_0) return false;

    // Pass message to accumulator
    // If the accumulator accepted it, and we aren't passing messages to the
    // default messaging app, abort broadcast to any further receivers
    boolean grabbed = SmsMsgAccumulator.instance().addMsg(context, message);
    if (grabbed) {
      FilterOptions options = message.getFilterOptions();
      return options.blockTextMsgEnabled();
    }

    // That is all we have to do.  SmsMsgAccumulator passes complete
    // messages to processCadPage when it has them
    return false;
  }

  /**
   * Read the PDUs out of an SMS_RECEIVED_ACTION or a DATA_SMS_RECEIVED_ACTION intent.
   *
   * @param intent
   *           the intent to read from
   * @return an array of SmsMessages for the PDUs
   */
  public static SmsMessage[] getMessagesFromIntent(Intent intent) {
    Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
    if (messages == null) {
      return null;
    }
    if (messages.length == 0) {
      return null;
    }

    byte[][] pduObjs = new byte[messages.length][];

    for (int i = 0; i < messages.length; i++) {
      pduObjs[i] = (byte[]) messages[i];
    }
    byte[][] pdus = new byte[pduObjs.length][];
    int pduCount = pdus.length;
    SmsMessage[] msgs = new SmsMessage[pduCount];
    for (int i = 0; i < pduCount; i++) {
      pdus[i] = pduObjs[i];
      msgs[i] = SmsMessage.createFromPdu(pdus[i]);
    }
    return msgs;
  }

  /**
   * Final Cadpage processing. Called by any message processor once it is
   * determined this is a real Cadpage alert
   * @param message Cadpage message
   */
  public static void processCadPage(final SmsMmsMessage message) {

    // We can be called on different working threads and need to find a
    // context and get back on the working thread.
    CadPageApplication.runOnMainThread(new Runnable(){
      @Override
      public void run() {
        processCadPage(CadPageApplication.getContext(), message);
      }
    });
  }

  /**
   * Final process of message once we have determined it is a CAD page message
   * This will be called by MmsTransactionService to handle MMS messages
   * @param context current context
   * @param message message to be processed
   */
  private static void processCadPage(Context context, SmsMmsMessage message) {

    // If we are ignoring this message, drop out
    FilterOptions options = message.getFilterOptions();
    if (!options.historyEnabled()) return;

    // Add new message to the message queue
    SmsMessageQueue.getInstance().addNewMsg(message);

    // See if any notifications are enabled
    boolean notify = options.noticeEnabled();

    // Determine if application should pop up right now
    boolean process = startApp(context, options);

    // If either a screen display or notification was generated by this
    // message, it is time to acquire a wakelock to keep the device awake
    if (process || notify) ManageWakeLock.acquireFull(context);

    // And finally, launch the main application screen
    if (process) {

      // OK, things get complicated.
      // As of Android 10, we are not allowed to launch activities in response to background events.
      // If the main activity is visible, things will work normally.  If not, we have to start
      // a full screen notification, and launch Radio Scanner if requested
      if (CadPageApplication.restrictBackgroundActivity()) {
        Log.v("Launching full screen notification");
        ManageNotification.show(context, message, true, true);
        launchScanner(context);
      }

      // Otherwise launch the main activity normally
      else {
        CadPageActivity.launchActivity(context, notify, message);
      }
    }

    // If we did not launch the application screen, do the notification stuff
    // that is not going to be performed by the main app activity
    else if (notify) {
      ManageNotification.show(context, message);
      launchScanner(context);
    }
  }

  public static void launchScanner(Context context) {
    if (! ManagePreferences.activeScanner()) return;
    Intent scanIntent = ManagePreferences.scannerIntent();
    if (scanIntent == null) return;
    Log.v("Launching Scanner");
    scanIntent.putExtra("caller", "cadpage");
    SmsPopupUtils.sendImplicitBroadcast(context, scanIntent);
  }

  /**
   * Determine if application should be launched when CAD page is received
   * @param context current context
   * @return true if application should be launched
   */
  private static boolean startApp(Context context, FilterOptions options) {


    // If popup isn't enabled, this is as afar as we go
    if (! options.popupEnabled()) return false;

    // Fetch call state, if the user is in a call or the phone is ringing we don't want to show the popup
    if (ManagePreferences.noShowInCall()) {
      TelephonyManager mTM = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
      boolean callStateIdle = mTM.getCallState() == TelephonyManager.CALL_STATE_IDLE;
      if (!callStateIdle) return false;
    }

    // Otherwise OK
    return true;
  }
}
