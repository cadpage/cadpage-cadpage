package net.anei.cadpage;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;

import net.anei.cadpagesupport.IContentService;
import net.anei.cadpagesupport.IResponseSenderService;

import java.util.List;

public class ResponseSender {

  private final Activity activity;

  private IResponseSenderService mResponseSenderService = null;
  private ServiceConnection mServiceConnect = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.v("ResponseSenderService connected");
      mResponseSenderService = IResponseSenderService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.v("ResponseSenderService disconnected");
      mResponseSenderService = null;
    }
  };

  public ResponseSender(Activity activity) {
    this.activity = activity;

    Intent intent = new Intent();
    intent.setClassName("net.anei.cadpagesupport", "net.anei.cadpagesupport.ResponseSenderService");
    if (activity.bindService(intent, mServiceConnect, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT)) {
      Log.v("ResponseSenderService binding succeeded");
    } else {
      Log.v("ResponseSenderService binding failed");
    };
  }

  /**
   * Send SMS response message
   * @param target target phone number or address
   * @param message message to be sent
   */
  public void sendSMS(String target, String message){

    // First try invoking ResponseSenderService in the message support app
    if (mResponseSenderService != null) {
      try {
        Log.v("Send SMS Msg 1");
        mResponseSenderService.sendSMS(target, message);
        return;
      } catch (RemoteException ex) {
        Log.e(ex);
      }
    }

    // No go.  Try sending the request and to the broadcast receiver
    Intent intent = new Intent("net.anei.cadpagesupport.SendSMS");
    intent.setClassName("net.anei.cadpagesupport", "net.anei.cadpagesupport.ResponseSender");
    intent.putExtra("target", target);
    intent.putExtra("message", message);

    List<ResolveInfo> rcvrs =
        activity.getPackageManager().queryBroadcastReceivers(intent, 0);
    if (rcvrs != null && ! rcvrs.isEmpty()) {
      Log.v("Send SMS Msg 2");
      activity.sendBroadcast(intent);
    }

    else {

      // Either support app is not installed, or they do not have the latest version
      // Fallback is to request the message app send the text message
      Log.v("Send SMS Msg 3");
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

    // First try invoking ResponseSenderService in the message support app
    if (mResponseSenderService != null) {
      try {
        Log.v("Initiate response call 1");
        mResponseSenderService.callPhone(phone);
        return;
      } catch (RemoteException ex) {
        Log.e(ex);
      }
    }

    // No go.  Try sending the request and to the broadcast receiver
    Intent intent = new Intent("net.anei.cadpagesupport.CALL_PHONE");
    intent.setClassName("net.anei.cadpagesupport", "net.anei.cadpagesupport.ResponseSender");
    intent.putExtra("phone", phone);

    List<ResolveInfo> rcvrs =
        activity.getPackageManager().queryBroadcastReceivers(intent, 0);
    if (rcvrs != null && ! rcvrs.isEmpty()) {
      Log.v("Initiate response call 2");
      activity.sendBroadcast(intent);
    }

    else {
      // Fallback is to ask dialer to prompt user to make the call
      try {
        Log.v("Initiate response call 3");

        String urlPhone = "tel:" + phone;
        intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse(urlPhone));
        activity.startActivity(intent);
      } catch (Exception e) {
        Log.v("SMSPopupActivity: Phone call failed" + e.getMessage());
      }
    }
  }

  private static ResponseSender instance = null;

  public static void setInstance() {
    if (instance == null) {
      Activity activity = CadPageActivity.getCadPageActivity();
      if (activity != null) instance = new ResponseSender(activity);
    }
  }

  public static ResponseSender instance() {
    return instance;
  }
}
