package net.anei.cadpage;

import net.anei.cadpage.billing.BillingManager;
import net.anei.cadpage.donation.UserAcctManager;
import net.anei.cadpage.vendors.VendorManager;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;

/**
 * Main CadPage application
 * which is where we need to do our one time initialization
 */
public class CadPageApplication extends Application {

  /* (non-Javadoc)
   * @see android.app.Application#onCreate()
   */
  @Override
  public void onCreate() {
    super.onCreate();
    initialize(this);
  }

  @SuppressLint("StaticFieldLeak")
  private static Context context = null;
  private static Thread mainThread = null;
  private static Handler mainHandler = null;

  /**
   * Initialize everything
   * @param callingContext current calling context
   * @return true if successful
   */
  public static synchronized boolean initialize(Context callingContext) {

    if (context != null) return !TopExceptionHandler.isInitFailure();

    Log.v("Initialization startup");

    Log.v("Message access " + (MsgAccess.ALLOWED ? "allowed" : "restricted"));

    context = callingContext.getApplicationContext();
    mainThread = Thread.currentThread();
    mainHandler = new Handler();
    getVersionInfo(context);
    try {

      UserAcctManager.setup(context);
      BillingManager.instance().initialize(context);
      ManagePreferences.setupPreferences(context);
      ManageNotification.setup(context);
      VendorManager.instance().setup(context);
      UserAcctManager.instance().reset();
      
      // Reload log buffer queue
      SmsMsgLogBuffer.setup(context);

      // Reload existing message queue
      SmsMessageQueue.setupInstance(context);

      // See if a new version of Cadpage has been installed
      if (ManagePreferences.newVersion(versionCode)) {
        
        // Reset vendor status
        VendorManager.instance().newReleaseReset();
      }

      // If a FCM registration was not forced normally, see if one is overdue
      else {
        FCMMessageService.checkOverdueRefresh(context);
      }

    } catch (Exception ex) {
      TopExceptionHandler.initializationFailure(context, ex);
      return false;
    }
    
    // Reinitialize any Widget triggers.  This shouldn't be necessary, but it
    // seems to help avoid sporadic problems with unresponsive Widgets.
    CadPageWidget.reinit(context);
  
    TopExceptionHandler.enable(context);
    Log.v("Initialization complete");
    return true;
    
  }

  private static String version = null;
  private static String nameVersion = null;
  private static int versionCode = -1;
  
  private static void getVersionInfo(Context context) {

    if (nameVersion == null) {
      //Try and find app version name and code
      PackageManager pm = context.getPackageManager();
      try {
        //Get version number, not sure if there is a better way to do this
        PackageInfo info = pm.getPackageInfo(SmsPopupConfigActivity.class.getPackage().getName(), 0);
        version = info.versionName;
        nameVersion = " v" + info.versionName;
        versionCode = info.versionCode;
      } catch (NameNotFoundException e) {
        version = "";
        nameVersion = "";
        versionCode = 0;
      }
      nameVersion = context.getString(R.string.app_name) + nameVersion;
    }
  }
  
  public static String getVersion() {
    return version;
  }
  
  public static String getNameVersion() {
    return nameVersion;
  }
  
  public static int getVersionCode() {
    return versionCode;
  }

  public static void runOnMainThread(Runnable runnable) {
    if (mainThread == Thread.currentThread()) {
      runnable.run();
    } else {
      mainHandler.post(runnable);
    }
  }

  
  public static Context getContext() {
    return context;
  }
}
