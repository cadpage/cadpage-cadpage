package net.anei.cadpage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmsReceiver extends BroadcastReceiver {
  
  public static final String ACTION_SMS_RECEIVED =
      (MsgAccess.ALLOWED ? "android.provider.Telephony.SMS_RECEIVED"
                         : "net.anei.cadpage.Telephony.SMS_RECEIVED");

  @Override
  public synchronized void onReceive(Context context, Intent intent) {
    Log.v("SMSReceiver: onReceive()");
    if (!CadPageApplication.initialize(context)) return;
    ContentQuery.dumpIntent(intent);

    // Anything except an SMS received request should be ignored
    if (!ACTION_SMS_RECEIVED.equals(intent.getAction())) return;

    // We have a lot of CPU cycles to crunch through these alerts.  Ideally, we want to pass
    // everything to the SmsService where it can be run off of the the main thread.  But if are
    // running on an old version of Android that still supports blocking alerts from getting
    // to the default message app, and the user has not suppressed that behavior, then we have
    // no choice but do all of the work on the main thread :(
    if (!ManagePreferences.smspassthru()) {
      if (SmsService.processIntent(context, intent)) abortBroadcast();
    } else {
      SmsService.runIntentInService(context, intent);
    }
  }
}

