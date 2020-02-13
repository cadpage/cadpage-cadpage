package net.anei.cadpage;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;

import java.util.List;

import androidx.annotation.RequiresApi;

public class MMSDownloader {

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
  public static boolean downloadMMS(Context context, SmsManager smsManager, String contentLoc,
                                    Uri downloadUri, PendingIntent pIntent) {

    // Send intent to support app which does the real work
    Intent intent = new Intent("net.anei.cadpagesupport.MMS_DOWNLOAD", downloadUri);
    intent.setClassName("net.anei.cadpagesupport", "net.anei.cadpagesupport.ResponseSender");
    intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    intent.putExtra("content_uri", contentLoc);
    intent.putExtra("subscription_id", smsManager.getSubscriptionId());
    intent.putExtra("report_intent", pIntent);

    List<ResolveInfo> rcvrs =
        context.getPackageManager().queryBroadcastReceivers(intent, 0);
    if (rcvrs != null && !rcvrs.isEmpty()) {
      context.sendBroadcast(intent);
      return true;
    } else {
      return false;
    }
  }
}
