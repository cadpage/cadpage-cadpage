package net.anei.cadpage;

public class Log {
  public final static String LOGTAG = "CadPage";

  public static final boolean DEBUG = true;

  private static boolean testMode = false;

  public static void setTestMode(boolean testMode) {
    Log.testMode = testMode;
  }

  public static void v(String msg) {
    if (testMode) System.err.println(msg);
    else android.util.Log.v(LOGTAG, msg);
  }
  
  public static void i(String msg) {
    if (testMode) System.err.println(msg);
    else android.util.Log.i(LOGTAG, msg);
  }
  
  public static void w(String msg) {
    if (testMode) System.err.println(msg);
    else android.util.Log.w(LOGTAG, msg);
  }

  public static void e(String msg) {
    if (testMode) System.err.println(msg);
    else android.util.Log.e(LOGTAG, msg);
  }

  public static void e(Throwable ex) {
    e(ex.getMessage(), ex);
  }

  public static void e(String msg, Throwable ex) {
    if (testMode) {
      System.err.println(msg);
      ex.printStackTrace(System.err);
    }
    else android.util.Log.e(LOGTAG, ex.getMessage(), ex);
  }
}
