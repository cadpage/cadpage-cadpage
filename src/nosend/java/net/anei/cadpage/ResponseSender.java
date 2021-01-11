package net.anei.cadpage;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.List;

public class ResponseSender {

  private final Activity activity;

  public ResponseSender(Activity activity) {
    this.activity = activity;
  }

  /**
   * Send SMS response message
   * @param target target phone number or address
   * @param message message to be sent
   */
  public void sendSMS(String target, String message){

    // Send intent to support app which does the real work
    Intent intent = new Intent("net.anei.cadpagesupport.SendSMS");
    intent.setClassName("net.anei.cadpagesupport", "net.anei.cadpagesupport.ResponseSender");
    intent.putExtra("target", target);
    intent.putExtra("message", message);

    List<ResolveInfo> rcvrs =
        activity.getPackageManager().queryBroadcastReceivers(intent, 0);
    if (rcvrs != null && ! rcvrs.isEmpty()) {
      activity.sendBroadcast(intent);
    }

    else {

      // Either support app is not installed, or they do not have the latest version
      // Fallback is to request the message app send the text message
      Log.v("Support app failed to process text request");
      intent = new Intent(Intent.ACTION_SENDTO);
      intent.setData(Uri.parse("smsto:" + target));
      intent.putExtra("sms_body", message);

      ContentQuery.dumpIntent(intent);
      try {
        activity.startActivity(intent);
      } catch (Exception ex2) {
        Log.e(ex2);
      }
    }
  }

  /**
   * Call phone number to report response status
   * @param phone phone number to call
   */
  public void callPhone(String phone) {

    // Send intent to support app which does the real work
    Intent intent = new Intent("net.anei.cadpagesupport.CALL_PHONE");
    intent.setClassName("net.anei.cadpagesupport", "net.anei.cadpagesupport.ResponseSender");
    intent.putExtra("phone", phone);

    List<ResolveInfo> rcvrs =
        activity.getPackageManager().queryBroadcastReceivers(intent, 0);
    if (rcvrs != null && ! rcvrs.isEmpty()) {
      activity.sendBroadcast(intent);
    }

    else {
      // Fallback is to ask dialer to prompt user to make the call
      try {
        String urlPhone = "tel:" + phone;
        intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse(urlPhone));
        activity.startActivity(intent);
      } catch (Exception e) {
        Log.v("SMSPopupActivity: Phone call failed" + e.getMessage());
      }
    }
  }
}
