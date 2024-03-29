package net.anei.cadpage.donation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.anei.cadpage.Log;
import net.anei.cadpage.R;
import net.anei.cadpage.SmsMmsMessage;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;

@SuppressWarnings("ResultOfMethodCallIgnored")
public abstract class DonateScreenBaseEvent extends DonateEvent {

  private final int titleId;
  private final int winTitleId;
  private int textId;
  private final int layout;
  private TextView textView;
  private Activity activity;

  protected DonateScreenBaseEvent(AlertStatus alertStatus, int titleId, int textId,
                                  int layout) {
    this(alertStatus, titleId, -1, textId, layout);
  }

  protected DonateScreenBaseEvent(int titleId, int winTitleId, int textId, int layout) {
    this(null, titleId, winTitleId, textId, layout);
  }

  private DonateScreenBaseEvent(AlertStatus alertStatus, int titleId, int winTitleId, int textId,
                                int layout) {
    super(alertStatus, titleId);
    registerScreenEvent(this);
    this.titleId = titleId;
    this.winTitleId = winTitleId;
    this.textId = textId;
    this.layout = layout;
  }
  
  protected boolean overrideWindowTitle() {
    return winTitleId >= 0;
  }

  /**
   * Called to create the associated Donate activity
   * @param activity new activity being created
   * @param msg message associated with this event
   */
  public void create(final Activity activity, SmsMmsMessage msg) {

    this.activity = activity;
    
    // Double check that event is still enabled.
    // It isn't that we really worry about showing an inappropriate display
    // as we are making sure that isEnabled has been called for this activity
    // as it sometimes has some required side effects.
    if (!isEnabled(msg)) {
      closeEvents(activity);
      return;
    }

    activity.setContentView(layout);
    
    // Set heading color if appropriate
    // There is one and only one status event that is not really a payment status.
    // Very sloppy, but we will check for that and overwrite the normal title text
    TextView view = activity.findViewById(R.id.DonateStatusView);
    if (overrideWindowTitle()) view.setText(activity.getString(winTitleId >= 0 ? winTitleId : titleId));
    setTextColor(view);
    
    // Set up main box text and color
    try {
      textView = activity.findViewById(R.id.DonateTextView);
      textView.setText(activity.getString(textId, getTextParms(activity, PARM_TEXT)));
      setTextColor(textView);
    } catch (RuntimeException ex) {
      throw new RuntimeException(this.getClass().getName(), ex);
    }
  }

  public void setTextId(int textId) {
    this.textId = textId;
    textView.setText(activity.getString(textId, getTextParms(activity, PARM_TEXT)));
  }

  @Override
  protected void doEvent(Activity activity, SmsMmsMessage msg) {
    DonateActivity.launchActivity(activity, this, msg);
  }

  /**
   * Create dialog in response to showDialog call
   * @param id ID code passed to showDialog
   * @return created dialog
   */
  public Dialog createDialog(Activity activity, int id) {
    return new AlertDialog.Builder(activity)
        .setIcon(R.drawable.ic_launcher)
        .setTitle(R.string.pref_payment_status_title)
        .setMessage(id)
        .setPositiveButton(R.string.donate_btn_done, null)
        .show();
  }
  
  /**
   * Open event screen window popup
   * @param context current context
   */
  public void open(Context context) {
    DonateActivity.launchActivity(context, this, null);
  }
  
  
  
  // Map use to identify Screen events by classname
  private static final Map<String, DonateScreenBaseEvent> screenEventMap = new HashMap<>();

  /**
   * Register a Donate screen event for future retrieval
   * @param event Event to be registered
   */
  private static void registerScreenEvent(DonateScreenBaseEvent event) {
    screenEventMap.put(event.getClass().getName(), event);
  }
  
  /**
   * Retrieve a registered Donate screen event
   * @param classname class name of registered event
   * @return registered donate screen event
   */
  @SuppressWarnings("unchecked")
  public static DonateScreenBaseEvent getScreenEvent(String classname) {
    
    // Very rarely, we will called before the registration map has been initialized
    // How this can happen is not entirely clear, but by coding a reference to the
    // main donation event, and the two paging events that are not part of the
    // main donation event menu, we can pretty much assure that everything has
    // been instantiated which will set up the class map.
    MainDonateEvent.instance();
    PagingProfileEvent.instance();
    PagingSubRequiredEvent.instance();
    HelpWelcomeEvent.instance();
    
    // Except for Vendor1Event which isn't in the main menu.   So we will invoke it
    // as well
    VendorEvent.instance(1);
    
    DonateScreenBaseEvent event = screenEventMap.get(classname);
    if (event == null) {

      // Yet it still keeps happening (rarely) possibly because Cadpage is killed and
      // restarted while one of our Donate activity windows is open.  Ultimate fallback is
      // to use reflection to call the Donate event instance method.
      Log.e("No Event registered for " + classname);
      try {
        Class cls = Class.forName(classname);
        Method instanceMethod = cls.getMethod("instance");
        event = (DonateScreenBaseEvent)instanceMethod.invoke(null);
        Log.e("Event successfully retrieved");
      } catch (Exception ex) {
        throw new RuntimeException("Error creating " + classname, ex);
      }
    }
    return event;
  }

  /**
   * Determine if activity launch request should proceed
   * @param activity current activity
   * @return true to proceed with launch, false to abort
   */
  public boolean launchActivity(Activity activity) {
    return true;
  }

  /**
   * Called to perform and status changes that resulted while this activity was suspended
   * @param activity current activity
   */
  public void onRestart(DonateActivity activity) {}

  /**
   * Called to clean up things when activity is destroyed
   * @param activity current activity
   */
  public void onDestroy(DonateActivity activity) {}

  /**
   * Check to see if we need to display the check popup config warning
   * and display it if needed
   * @param context current context
   * @return true if check popup config warning window has been started
   */
  public boolean launch(Context context) {
    return launch(context, null);
  }

  /**
   * Check to see if we need to display the check popup config warning
   * and display it if needed
   * @param context current context
   * @param msg current message
   * @return true if check popup config warning window has been started
   */
  public boolean launch(Context context, SmsMmsMessage msg) {
    if (!isEnabled()) return false;
    DonateActivity.launchActivity(context, this, msg);
    return true;
  }

}
