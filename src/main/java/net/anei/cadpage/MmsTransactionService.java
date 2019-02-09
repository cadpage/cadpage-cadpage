/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.anei.cadpage;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.anei.cadpage.mms.GenericPdu;
import net.anei.cadpage.mms.PduParser;

/**
 * This service is responsible for retrieving the actual content of MMS messages
 * that have been determined to be possible CAD pages
 */
public class MmsTransactionService extends Service {

  private static final String MMS_URL = "content://mms";
  private static final Uri MMS_URI = Uri.parse("content://mms");

  private enum EventType {TRANSACTION_REQUEST, TIMEOUT, RETRY, QUIT}

  // Column names for query searches
  private static final String[] MMS_COL_LIST = new String[]{"_ID"};
  private static final String[] PART_COL_LIST = new String[]{"text", "_data"};

  // Retry retrieve content interval
  private static final int RETRY_INTERVAL = 1000;

  private ServiceHandler mServiceHandler;
  private PowerManager.WakeLock mWakeLock;

  // Cached copies of different preferences we might need during off thread processing
  private int mmsTimeout;

  @SuppressLint("NewApi")
  @Override
  public void onCreate() {
    if (Log.DEBUG) Log.v("MmsTransactionService.onCreate()");
    CadPageApplication.initialize(this);

    // Acquire simple power lock while we are running
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    assert pm != null;
    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CadPage:MMS Connectivity");
    mWakeLock.setReferenceCounted(false);
    mWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);

    // Start up the thread running the service.  Note that we create a
    // separate thread because the service normally runs in the process's
    // main thread, which we don't want to block.
    HandlerThread thread = new HandlerThread("MmsTransactionService");
    thread.start();

    mServiceHandler = new ServiceHandler(thread.getLooper());

  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (Log.DEBUG) Log.v("MmsTransactionService.onStart()");
    if (intent == null) return START_NOT_STICKY;

    if (!MsgAccess.ALLOWED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForeground(1, ManageNotification.getMiscNotification(this));
    }

    // Collect all of the preferences we might need while we are still on
    // the main thread;
    mmsTimeout = ManagePreferences.mmsTimeout() * 60000;

    Message msg = mServiceHandler.obtainMessage(EventType.TRANSACTION_REQUEST.ordinal());
    msg.arg1 = startId;
    msg.obj = intent;
    mServiceHandler.sendMessage(msg);
    return Service.START_STICKY;
  }


  @Override
  public void onDestroy() {
    mWakeLock.release();
    mServiceHandler.sendEmptyMessage(EventType.QUIT.ordinal());
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  // 
  private class MmsMsgEntry {
    SmsMmsMessage message = null;  // Message we are working on
  }

  // Main thread handler class
  private class ServiceHandler extends Handler {

    // Actual queue of pending MMS transactions
    private final List<MmsMsgEntry> msgList = new LinkedList<>();

    private final ContentResolver qr = getContentResolver();
    private final MmsContentQuery mcq = new MmsContentQuery(MmsTransactionService.this);

    private class MmsContentObserver extends ContentObserver {
      private MmsContentObserver(Handler handler) {
        super(handler);
      }

      @Override
      public void onChange(boolean selfChange, Uri uri) {
        if (Log.DEBUG) Log.v("Mms Data Change  uri:" + uri);
        mmsDataChange();
        cleanup();
      }
    }

    private final MmsContentObserver observer = new MmsContentObserver(this);

    private ServiceHandler(Looper looper) {
      super(looper);

      qr.registerContentObserver(MMS_URI, true, observer);
    }

    /**
     * Handle incoming transaction requests.
     * The incoming requests are initiated by the MMSC Server or by the
     * MMS Client itself.
     */
    @Override
    public void handleMessage(Message msg) {

      try {

        EventType type = EventType.values()[msg.what];
        if (Log.DEBUG) Log.v("mmsTransactionService." + type);
        switch (type) {
          case QUIT:
            if (!msgList.isEmpty()) {
              Log.w("TransactionService exiting with transaction still pending");
            }
            getLooper().quit();
            return;

          case TRANSACTION_REQUEST:
            Intent intent = (Intent) msg.obj;
            mmsReceive(intent);
            break;

          case RETRY:
            MmsMsgEntry entry = (MmsMsgEntry) msg.obj;
            if (mmsDataChange(entry)) msgList.remove(entry);
            break;

          case TIMEOUT:
            entry = (MmsMsgEntry) msg.obj;
            if (msgList.remove(entry)) {
              SmsMsgLogBuffer.getInstance().add(entry.message.timeoutMarker());
            }
        }

        cleanup();
      }

      // Exceptions thrown on this thread should be caught and rethrown on the
      // main thread where our top level exception handler will catch them.
      catch (final RuntimeException ex) {
        CadPageApplication.runOnMainThread(new Runnable() {
          @Override
          public void run() {
            throw (ex);
          }
        });
      }
    }

    /**
     * Process initial incoming MMS notification
     *
     * @param intent MMS notification intent
     */
    private void mmsReceive(Intent intent) {
      // Get raw PDU push-data from the message and parse it
      byte[] pushData = intent.getByteArrayExtra("data");

      GenericPdu pdu = null;
      try {
        pdu = new PduParser(pushData).parse();
      } catch (Exception ex) {
        Log.e(ex);
        EmailDeveloperActivity.logSnapshot(MmsTransactionService.this, "MMS processing failure");
      }
      if (null == pdu) {
        Log.e("Invalid PUSH data");
        EmailDeveloperActivity.logSnapshot(MmsTransactionService.this, "Invalid PUSH data");
        return;
      }

      SmsMmsMessage message = pdu.getMessage();
      if (message == null) {
        EmailDeveloperActivity.logSnapshot(MmsTransactionService.this, "Empty MMS message");
        return;
      }

      // Ignore if we have already processed a notification for this message
      if (SmsMsgLogBuffer.getInstance().checkDuplicateNotice(message)) {
        EmailDeveloperActivity.logSnapshot(MmsTransactionService.this, "Duplicate MMS notice");
        return;
      }

      // Save message for future test or error reporting use
      // Duplicate message check is ignored for now because we do not yet have a message body
      SmsMsgLogBuffer.getInstance().add(message);

      // See if message passes override from filter
      // Without a message body, isPageMsg doesn't do anything more than
      // check the sender filter
      if (!message.isPageMsg()) return;

      // Otherwise, add to the list of messages that we are waiting for content from.
      MmsMsgEntry entry = new MmsMsgEntry();
      entry.message = message;
      msgList.add(entry);

      // Post a timeout event for this message to remove if from the queue if
      // we haven't received any data content
      Message msg = mServiceHandler.obtainMessage(EventType.TIMEOUT.ordinal());
      msg.obj = entry;
      mServiceHandler.sendMessageDelayed(msg, mmsTimeout);

      // Occasionally, the content change notice comes in before the push request
      // so will waste some time checking to see if the data content is already present
      mmsDataChange();
      cleanup();
    }

    /**
     * Process generic MMS content data change
     */
    private void mmsDataChange() {

      // Loop through all of the pending MMS message entries
      for (Iterator<MmsMsgEntry> iter = msgList.iterator(); iter.hasNext(); ) {
        if (mmsDataChange(iter.next())) iter.remove();
      }
    }

    /**
     * Check for retrieved message data for one particular message entry
     * @param entry entry to be checked
     * @return true if entry processing is complete and entry should be deleted
     */
    private boolean mmsDataChange(MmsMsgEntry entry) {

      final SmsMmsMessage message = entry.message;

      // If the content query system is not yet functioning, set up a
      // retry event to try this again later
      if (!mcq.isActive()) {
        Message msg = mServiceHandler.obtainMessage(EventType.RETRY.ordinal());
        msg.obj = entry;
        mServiceHandler.sendMessageDelayed(msg, RETRY_INTERVAL);
        return false;
      }

      // Start by finding the internal record number associated with this
      // message ID.  We used to only do this once and keep using the mame
      // record number, but it turns out that we go through two different
      // numbers while downloading MMS content
      Cursor cur;
      try {
        String msgId = message.getMmsMsgId();
        String contentLoc = message.getContentLoc();
        cur = mcq.query(MMS_URL, MMS_COL_LIST, "tr_id=? or m_id=? or m_id=?", new String[]{msgId, msgId, contentLoc}, null);
      } catch (IllegalStateException ex) {
        Log.e(ex);
        EmailDeveloperActivity.logSnapshot(MmsTransactionService.this, "MMS processing failure");
        return false;
      }
      if (cur == null) return false;

      int recNo;
      try {
        if (!cur.moveToFirst()) return false;
        recNo = cur.getInt(0);
      } finally {
        cur.close();
      }

      // OK, we have the desired record number
      // Now see if we can recover the content
      String partUrl = MMS_URL + '/' + recNo + "/part";
      cur = mcq.query(partUrl, PART_COL_LIST, null, null, null);

      String text;
      try {
        if (cur == null || !cur.moveToFirst()) {

          // Post a timeout event for this message to remove if from the queue if
          // we haven't received any data content
          Message msg = mServiceHandler.obtainMessage(EventType.RETRY.ordinal());
          msg.obj = entry;
          mServiceHandler.sendMessageDelayed(msg, RETRY_INTERVAL);
          return false;
        }

        // Almost there, we have a return value, try to retrieve a text component from it
        do {
          text = cur.getString(0);
          if (text == null) {
            byte[] ba = cur.getBlob(1);
            if (ba != null) text = new String(ba);
          }
          if (text != null) {
            text = text.trim();
            if (!text.startsWith("<smil>")) break;
            text = null;
          }
        } while (cur.moveToNext());
      } finally {
        if (cur != null) cur.close();
      }

      // for better or worse, we are done with this message,
      // Any returns from this point on should request message
      // entry be deleted

      // If we didn't retrieve any text info, give it up
      // Otherwise add the text body to our message
      // And update the message saved in the log buffer
      if (text == null) return true;
      message.setMessageBody(text);
      SmsMsgLogBuffer.getInstance().update(message);

      // If this is a CAD page, pop back to the main thread to perform the
      // rest of the CAD page message processing
      if (message.isPageMsg()) {
        CadPageApplication.runOnMainThread(new Runnable() {
          @Override
          public void run() {
            SmsService.processCadPage(message);
          }
        });
      }
      return true;
    }

    /**
     * Cleanup - shut down everything if message queue is empty
     */
    private void cleanup() {

      // If the message queue is empty, it is time to shut down
      if (msgList.size() == 0) {
        if (Log.DEBUG) Log.v("MmsTransactionService shutdown");
        qr.unregisterContentObserver(observer);
        CadPageApplication.runOnMainThread(new Runnable() {
          @Override
          public void run() {
            stopSelf();
          }
        });
      }
    }
  }

  public static void runIntentInService(Context context, Intent intent) {

    // Pass intent on the MmsTransactionService
    intent.setClass(context, MmsTransactionService.class);
    if (!MsgAccess.ALLOWED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent);
    } else {
      context.startService(intent);
    }

  }
}
