package net.anei.cadpage;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.telephony.SmsManager;

public class ResponseSender {

  private final Activity activity;
  private SendSMSReceiver receiver = null;

  public ResponseSender(Activity activity) {
    this.activity = activity;
  }

  /**
   * Send SMS response message
   * @param target target phone number or address
   * @param message message to be sent
   */
  public void sendSMS(String target, String message){

    if (receiver == null) {
      receiver = new SendSMSReceiver();
      activity.registerReceiver(receiver, new IntentFilter(SMS_SENT));
      activity.registerReceiver(receiver, new IntentFilter(SMS_DELIVERED));
    }

    Intent sendIntent = new Intent(SMS_SENT);
    sendIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
    PendingIntent sentPI = PendingIntent.getBroadcast(activity, 0, sendIntent, 0);
    Intent deliverIntent = new Intent(SMS_DELIVERED);
    deliverIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
    PendingIntent deliveredPI = PendingIntent.getBroadcast(activity, 0, deliverIntent, 0);

    // The send logic apparently isn't as bulletproof as we like.  It sometimes
    // throws a null pointer exception on the other side of an RPC.  We can't
    // do much about it.
    SmsManager sms = SmsManager.getDefault();
    try {
      sms.sendTextMessage(target, null, message, sentPI, deliveredPI);
    } catch (NullPointerException ex) {

      Log.e(ex);
    }
  }

  public static class SendSMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent == null) return;
      String action = intent.getAction();
      if (action != null) {
        int pt = action.lastIndexOf('.');
        if (pt >= 0) action = action.substring(pt + 1);
      }
      String status;
      switch (getResultCode()) {

        case Activity.RESULT_OK:
          status = "OK";
          break;

        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
          status = "Generic failure";
          break;

        case SmsManager.RESULT_ERROR_NO_SERVICE:
          status = "No service";
          break;

        case SmsManager.RESULT_ERROR_NULL_PDU:
          status = "Null PDU";
          break;

        case SmsManager.RESULT_ERROR_RADIO_OFF:
          status = "Radio off";
          break;

        case Activity.RESULT_CANCELED:
          status = "Canceled";
          break;

        default:
          status = "" + getResultCode();
      }
      Log.v("SMS " + action + " status:" + status);
    }
  }
  private static final String SMS_SENT = "net.anei.cadpage.MsgOptionManager.SMS_SENT";
  private static final String SMS_DELIVERED = "net.anei.cadpage.MsgOptionManager.SMS_DELIVERED";

  /**
   * Call phone number to report response status
   * @param phone phone number to call
   */
  public void callPhone(String phone) {
    try {
      String urlPhone = "tel:" + phone;
      Intent intent = new Intent(Intent.ACTION_CALL);
      intent.setData(Uri.parse(urlPhone));
      activity.startActivity(intent);
    } catch (Exception e) {
      Log.v("SMSPopupActivity: Phone call failed" + e.getMessage());
    }
  }
}
