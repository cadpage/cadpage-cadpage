package net.anei.cadpage.billing;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import net.anei.cadpage.donation.DonateEvent;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings({"SpellCheckingInspection"})
public abstract class Billing {

  public enum BillingStatus { UNKNOWN, OK, NOT_CONNECTED, NOT_SUPPORTED}

  private BillingStatus status = BillingStatus.UNKNOWN;

  private List<Runnable> eventQueue = null;

  private Activity donateActivity = null;
  private DonateEvent donateEvent = null;
  
  /**
   * @return true if billing is supported for this system
   */
  public boolean isSupported() {
    return status != BillingStatus.NOT_SUPPORTED;
  }

  /**
   * @return current status of billing
   */
  public BillingStatus getStatus() {
    return status;
  }

  /**
   * Initialize billing manager
   * @param context current context
   */
  abstract public void initialize(Context context);

  /**
   * Shutdown billing manager
   */
  public abstract void destroy();

  private boolean inProgress = false;
  
  /**
   * Queue runnable event to be run when billing is up and running
   * @param event Runnable event to be run when billing is supported
   */
  private void runWhenSupported(Runnable event) {

    if (status == BillingStatus.NOT_SUPPORTED) return;

    // We do not do any locking because we are only supposed to be called on the UI thread
    // But might be a good idea to confirm that
    if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
      throw new RuntimeException("Billing.runWhenSupported() needs to be called on UI thread");
    }

    // If billing is up and running, just run event
    if (isReady()) {
      event.run();
      return;
    }

    // Otherwise add the event to the event queue
    if (eventQueue == null) eventQueue = new ArrayList<>();
    eventQueue.add(event);

    // And make sure billing client is running
    connect();
  }

  private void connect() {

    // Do not connect if connect request is already in progress
    if (inProgress) return;

    inProgress = true;
    doConnect();
  }

  /**
   * @return true if billing client is ready to go
   */
  abstract boolean isReady();

  /**
   * Connect to billing client
   * must call setStatus() to report final status
   */
  abstract void doConnect();

  void setStatus(BillingStatus status) {
    inProgress = false;
    this.status = status;

    switch (status) {

      // All is well, run any queued events
      case OK:
        if (eventQueue != null) {
          for (Runnable event : eventQueue) event.run();
        }
        eventQueue = null;
        break;

      // Not conntected - retry after 5 min
      case NOT_CONNECTED:
        new Handler().postDelayed(() -> {
          if(this.status != BillingStatus.OK) connect();
        }, 300000L);
        break;

      // Not supported - end of story
      case NOT_SUPPORTED:
        eventQueue = null;
        break;
    }
  }

  /**
   * Request transaction history restore
   */
  public void restoreTransactions(final Context context) {
    runWhenSupported(() -> doRestoreTransactions(context));
  }

  abstract void doRestoreTransactions(Context context);

  /**
   * Request purchase of current year product
   * @param activity current activity
   * @param donateEvent donation event or null if none
   */
  public void startPurchase(Activity activity, DonateEvent donateEvent) {
    if (!isSupported()) return;

    if (activity == null) return;
    if (activity.isFinishing()) return;

    this.donateEvent = donateEvent;
    this.donateActivity = activity;

    doStartPurchase(activity);
  }

  /**
   * Initiate start purchase activity
   * must call endPurchase when complete
   * @param activity current activity
   */
  abstract void doStartPurchase(Activity activity);

  void endPurchase(boolean success) {
    if (success && donateEvent != null) donateEvent.closeEvents(donateActivity);
    donateEvent = null;
    donateActivity = null;
  }
}
