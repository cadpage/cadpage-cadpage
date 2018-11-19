package net.anei.cadpage;

import android.content.Context;
import android.os.Process;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * In an attempt to cut down on reported ANR's, Cadpage no longer parsers historical calls
 * during startup processing.  Instead, it launches this service to handle historical message
 * parsing in a worker thread
 */
public class ParserServiceManager {

  final private ThreadPoolExecutor threadPool;

  private ParserServiceManager() {
    threadPool = new ThreadPoolExecutor(0, 1,
                                        0L, TimeUnit.SECONDS,
                                         new LinkedBlockingQueue<Runnable>(),
                                         new ThreadFactory() {
                                           @Override
                                           public Thread newThread(Runnable r) {
                                             Thread t = new Thread(r, "ParserService");
                                             t.setDaemon(true);
                                             t.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
                                             return t;
                                           }
                                         });
                                  }

  private enum RequestType {STARTUP, REPARSE_GENERAL, REPARSE_SPLIT_MSG}

  private static class ParserTask implements Runnable {

    private final RequestType request;
    private final int changeCode;

    private ParserTask(RequestType request, int changeCode) {
      this.request = request;
      this.changeCode = changeCode;
    }

    @Override
    public void run() {

      try {
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
            } catch (InterruptedException ignored) {}
          }
        }
      }

      // Any exceptions that get thrown should be rethrown on the dispatch thread
      catch (final Exception ex) {
        TopExceptionHandler.reportException(ex);
      }
    }
  }

  private void request(RequestType request) {
    request(request, -1);
  }

  private void request(RequestType request, int changeCode) {
    threadPool.execute(new ParserTask(request, changeCode));
  }

  private static final ParserServiceManager instance = new ParserServiceManager();

  public static void startup() {
    instance.request(RequestType.STARTUP);
  }

  public static void reparseGeneral() {
    instance.request(RequestType.REPARSE_GENERAL);
  }

  public static void reparseSplitMsg(int changeCode) {
    instance.request(RequestType.REPARSE_SPLIT_MSG, changeCode);
  }
}
