package net.anei.cadpage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmsReceiver extends BroadcastReceiver {
  
  public static final String ACTION_SMS_RECEIVED =
      (BuildConfig.REC_SMS_ALLOWED ? "android.provider.Telephony.SMS_RECEIVED"
                                   : "net.anei.cadpage.Telephony.SMS_RECEIVED");

  @Override
  public synchronized void onReceive(Context context, Intent intent) {
    Log.v("SMSReceiver: onReceive()");
    if (!CadPageApplication.initialize(context)) return;
    ContentQuery.dumpIntent(intent);

    // Anything except an SMS received request should be ignored
    if (!ACTION_SMS_RECEIVED.equals(intent.getAction())) return;

    // We have a lot of CPU cycles to crunch through these alerts.  We want to pass
    // everything to the SmsService where it can be run off of the the main thread.
    SmsService.runIntentInService(context, intent);
  }
}

