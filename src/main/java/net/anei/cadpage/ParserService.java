package net.anei.cadpage;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

/**
 * In an attempt to cut down on reported ANR's, Cadpage no longer parsers historical calls
 * durring startup processing.  Instead, it launches this service to handle historical message
 * parsing in a worker thread
 */
public class ParserService extends IntentService {

  private enum RequestType {STARTUP, REPARSE_GENERAL, REPARSE_SPLIT_MSG}

  ;

  private static final String EXTRA_REQ_TYPE = "net.anei.cadpage.ParserServer.REQ_TYPE";
  private static final String EXTRA_CHANGE_CODE = "net.anei.cadpage.ParserServier.CHANGE_CODE";

  public ParserService() {
    super("ParserService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {

    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

    int iRequest = intent.getIntExtra(EXTRA_REQ_TYPE, -1);
    if (iRequest < 0) return;
    RequestType[] reqList = RequestType.values();
    if (iRequest >= reqList.length) return;
    RequestType request = reqList[iRequest];
    int changeCode = intent.getIntExtra(EXTRA_CHANGE_CODE, -1);

    for (SmsMmsMessage msg : SmsMessageQueue.getInstance().getMessageList()) {
      boolean update = false;
      switch (request) {
        case STARTUP:
          update = msg.updateParseInfo();
          break;

        case REPARSE_GENERAL:
          update = msg.reparseGeneral();
          break;

        case REPARSE_SPLIT_MSG:
          update = msg.splitOptionChange(changeCode);
          break;
      }

      if (update) {
        CadPageApplication.runOnMainThread(new Runnable() {
          @Override
          public void run() {
            SmsMessageQueue.getInstance().notifyDataChange(true);
          }
        });
        try {
          Thread.sleep(20);
        } catch (InterruptedException e) {}
      }
    }
  }

  public static void startup(Context context) {
    startService(context, RequestType.STARTUP);
  }

  public static void reparseGeneral(Context context) {
    startService(context, RequestType.REPARSE_GENERAL);
  }

  public static void reparseSplitMsg(Context context, int changeCode) {
    startService(context, RequestType.REPARSE_SPLIT_MSG, changeCode);
  }

  private static void startService(Context context, RequestType request) {
    startService(context, request, -1);
  }

  private static void startService(Context context, RequestType request, int changeCode) {
    // start the service to handle the intent
    Intent intent = new Intent();
    intent.setClass(context, ParserService.class);
    intent.putExtra(EXTRA_REQ_TYPE, request.ordinal());
    if (changeCode >= 0) intent.putExtra(EXTRA_CHANGE_CODE, changeCode);
    context.startService(intent);
  }
}
