package net.anei.cadpage;

import net.anei.cadpage.billing.BillingManager;
import net.anei.cadpage.donation.LocationTrackingEvent;
import net.anei.cadpage.donation.CheckPopupEvent;
import net.anei.cadpage.donation.DonateActivity;
import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.HelpWelcomeEvent;
import net.anei.cadpage.donation.NeedAcctPermissionUpgradeEvent;
import net.anei.cadpage.donation.VendorEvent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.List;

public class CadPageActivity extends AppCompatActivity {

  private static final String EXTRA_NOTIFY = "net.anei.cadpage.CadPageActivity.NOTIFY";
  private static final String EXTRA_POPUP = "net.anei.cadpage.CadPageActivity.POPUP";
  private static final String EXTRA_MSG_ID = "net.anei.cadpage.CadPageActivity.MSG_ID";

  private static final String SAVED_SPLIT_SCREEN = "SPLIT_SCREEN";
  private static final String SAVED_MSG_ID = "MSG_ID";

  private static final String CALL_ALERT_TAG = "CALL_ALERT_TAG";

  private final PermissionManager permMgr = new PermissionManager(this);

  private SmsPopupFragment popupFragment = null;


  private static boolean initializing = false;

  private static boolean startup = false;

  private boolean needSupportApp;

  private boolean splitScreen;

  /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (Log.DEBUG) Log.v("CadPageActivity: onCreate()");
    super.onCreate(savedInstanceState);

    int startMsgId = -1;
    if (savedInstanceState != null) {
      splitScreen = savedInstanceState.getBoolean(SAVED_SPLIT_SCREEN, false);
      startMsgId = savedInstanceState.getInt(SAVED_MSG_ID, -1);
    }

    if (!CadPageApplication.initialize(this)) {
      finish();
      return;
    }

    ManagePreferences.setPermissionManager(permMgr);

    initializing = !ManagePreferences.initialized();

    BillingManager.instance().initialize(this);

    // Apparently only an activity can calculate the total screen size.
    // So do it now and save it in preferences so it will be included in
    // generated emails
    DisplayMetrics displaymetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
    int height = displaymetrics.heightPixels;
    int width = displaymetrics.widthPixels;
    ManagePreferences.setScreenSize("" + width + "X" + height);

    // See if we are running in split screen mode, and save the previous status
    // so we can tell if it changed
    FragmentManager fm = getSupportFragmentManager();
    boolean oldSplitScreen = splitScreen;
    splitScreen = isSplitScreenConfig();

    // If we are switching between split screen and non-split screen modes, backstack entries and
    // fragments left over from a previous orientation or mode cause all kinds of problems
    // Better to just get rid of all of them first
    if (splitScreen != oldSplitScreen) {
      Log.v("Resetting Fragment Manager");
      while (fm.getBackStackEntryCount() > 0) fm.popBackStackImmediate();
      List<Fragment> fragList = fm.getFragments();
      if (!fragList.isEmpty()) {
        FragmentTransaction ft = fm.beginTransaction();
        for (Fragment frag : fragList) ft.remove(frag);
        ft.commit();
        fm.executePendingTransactions();
      }
    }

    setContentView(splitScreen ? R.layout.cadpage_split : R.layout.cadpage);
    for (Fragment frag : fm.getFragments()) {
      if (frag instanceof SmsPopupFragment) {
        popupFragment = (SmsPopupFragment) frag;
      }
    }

    // Force screen on and override lock screen
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true);
      setTurnScreenOn(true);
      KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
      km.requestDismissKeyguard(this, null);
    } else {
      int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
              | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
              | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
      getWindow().addFlags(flags);
    }

    // If this activity is being recreated from a previous instance, all we need to do
    // is display the previously displayed alert.  Otherwise go through the normal
    // startup sequence
    if (savedInstanceState != null) {
      if (startMsgId >= 0) showAlert(startMsgId);
    } else {
      startup();
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(SAVED_SPLIT_SCREEN, splitScreen);

    if (popupFragment != null && popupFragment.isVisible()) {
      int msgId = popupFragment.getMsgId();
      outState.putInt(SAVED_MSG_ID, msgId);
    }
  }

  /**
   * @return true if we should use the split screen display
   */
  private boolean isSplitScreenConfig() {
    String mode = ManagePreferences.popupMode();
    return (mode.equals("S") &&
            getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onNewIntent(android.content.Intent)
   */
  @Override
  protected void onNewIntent(Intent intent) {
    Log.v("CadPageActivity.onNewIntent()");
    super.onNewIntent(intent);
    setIntent(intent);
    
    startup();
  }
  
  /**
   * Perform initial intent processing logic
   * whether called from onCreate or onNewIntent
   */
  private void startup() {
    Intent intent = getIntent();
    
    // Log intent for debug purposes
    Log.v("CadPageActivity.startup()");
    ContentQuery.dumpIntent(intent);
    
    int msgId = intent.getIntExtra(EXTRA_MSG_ID, -1);

    // See if this request is going to pop up an alert window
    SmsMmsMessage msg;
    if (msgId >= 0) {
      msg = SmsMessageQueue.getInstance().getMessage(msgId);
    } else {
      boolean force = intent.getBooleanExtra(EXTRA_POPUP, false);
      msg = SmsMessageQueue.getInstance().getDisplayMessage(force);
    }

    // If there is no message popup display and we were started by some kind of
    // user interaction, go through the startup special processing checks
    if (msg == null && (intent.getFlags() & Intent.FLAG_FROM_BACKGROUND) == 0) {

      // First clear any pending notification
      ClearAllReceiver.clearAll(this);

      // The rest of this involves possible interactions with the user, which might conflict
      // with the initial permission checking logic.  So rather than do it immediately, we stuff
      // it in a Runnable object to be executed when the initial permission checking is complete
      final boolean init = initializing;
      ManagePreferences.checkInitialPermissions(() -> {
        startup = true;
        needSupportApp = SmsPopupUtils.checkMsgSupport(CadPageActivity.this) > 0;
        if (needSupportApp) return;

        // If user upgraded to the release that implements improved email account security, and
        // we suspect that really need to give us that email account access, let them know now.
        if (NeedAcctPermissionUpgradeEvent.instance().launch(CadPageActivity.this)) return;

        // If Cadpage is not functional with current settings, start up the new user sequence
        HelpWelcomeEvent event;
        if ((event = HelpWelcomeEvent.instance()).isEnabled()) {
          event.setIntializing(init);
          DonateActivity.launchActivity(CadPageActivity.this, event, null);
          return;
        }

        // Check call popup window configuration
        if (CheckPopupEvent.instance().launch(CadPageActivity.this)) return;

        // If a new Active911 client may be highjacking alerts, warn user
        if (LocationTrackingEvent.instance().launch(CadPageActivity.this)) return;

        // Otherwise, launch the release info dialog if it hasn't already been displayed
        String oldRelease = ManagePreferences.release();
        String release = CadPageApplication.getVersion();
        if (!release.equals(oldRelease)) {
          ManagePreferences.setRelease(release);
          if (!trimRelease(release).equals(trimRelease(oldRelease))) {
            new ReleaseDialogFragment().show(getSupportFragmentManager(), "release");
          }
        }

        // If not, see if we have discovered a direct page vendor sending us text pages
        else {
          VendorEvent.instance(1).launch(CadPageActivity.this);
        }
      });
    }
    
    // Otherwise, if we should automatically display a call, do it now
    else if (msg != null) {

      // But first to the initial permission check
      ManagePreferences.checkInitialPermissions(null);

      // Before we open the call display window, see if notifications were requested
      // And if they were, see if we should launch the Scanner channel open.
      // We can't do this in the Broadcast Receiver because this window obscures it
      // completely, so their Activity won't launch.

      if (intent.getBooleanExtra(EXTRA_NOTIFY, false)) {
        ManageNotification.show(this, msg);
        SmsService.launchScanner(this);
      }

      // OK, go ahead and open the call display window
      if (!isFinishing()) {
        if (Log.DEBUG) Log.v("CadPageActivity Auto launch SmsPopup for " + msg.getMsgId());
        showAlert(msg);
      }
    }

    initializing = false;
  }

  /**
   * Trim off everything beyond the first 3 components of the release version
   * @param release release version
   * @return trimmed release version
   */
  private String trimRelease(String release) {
    int dotCnt = 0;
    int col = 0;
    while (col < release.length()) {
      char chr = release.charAt(col);
      if (chr == '.' || chr == '-') {
        if (++dotCnt >= 3) break;
      }
      else if (chr >= 'A' && chr <= 'Z') break;
      col++;
    }
    return release.substring(0,col);
  }

  public static class ReleaseDialogFragment extends DialogFragment {

    @Override
    @NonNull public Dialog onCreateDialog(Bundle bundle) {
      Activity activity = getActivity();

      int releaseId = (DonationManager.instance().isFreeVersion() ? R.string.free_release_text : R.string.release_text);
      final SpannableString s = new SpannableString(getText(releaseId));
      Linkify.addLinks(s, Linkify.WEB_URLS);
      final TextView view = new TextView(activity);
      view.setText(s);
      view.setMovementMethod(LinkMovementMethod.getInstance());

      return new AlertDialog.Builder(activity)
      .setIcon(R.drawable.ic_launcher)
      .setTitle(R.string.release_title)
      .setView(view)
      .setPositiveButton(android.R.string.ok, null)
      .create();
    }
  }

  private boolean activityActive = false;

  @Override
  protected void onStart() {
    if (Log.DEBUG) Log.v("CadPageActivity: onStart()");
    super.onStart();
  }

  @Override
  protected void onStop() {
    if (Log.DEBUG) Log.v("CadPageActivity: onStop()");
    super.onStop();
  }

  @Override
  protected void onResume() { 
    if (Log.DEBUG) Log.v("CadPageActivity: onResume()");
    super.onResume();

    activityActive = true;

    // If we **REALLY** need the support app, and we asked the user
    // to install it, make sure that it has been installed and opened and
    // everything is OK.  However, we do not want to call this when we are
    // initialiazing because that will duplicate the call previousily made in
    // startup()
    if (!(BuildConfig.MSG_ALLOWED && BuildConfig.SEND_ALLOWED) && !startup && needSupportApp) {
      needSupportApp = SmsPopupUtils.checkMsgSupport(this) > 0;
    }

    // If user switched to/from split screen mode, recreate this activity
    if (splitScreen != isSplitScreenConfig()) recreate();

    startup = false;
  }

  protected void onPause() {
    if (Log.DEBUG) Log.v("CadPageActivity: onPause()");
    super.onPause(); 
    activityActive = false; 
  } 
  
  public boolean onKeyUp(int keyCode, KeyEvent event)  {
    //noinspection SimplifiableIfStatement
    if (!activityActive) return false;
    return super.onKeyUp(keyCode, event);
  } 
  
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    //noinspection SimplifiableIfStatement
    if (!activityActive) return false;
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] granted) {
    ManagePreferences.onRequestPermissionsResult(requestCode, permissions, granted);
  }

  @Override
  protected void onDestroy() {
    ManagePreferences.releasePermissionManager(permMgr);
    super.onDestroy();
  }

  /**
   * Back key pressed
   */
  @Override
  public void onBackPressed() {

    // Suppress back activity if response button menu is visible
    if (ManageNotification.isActiveNotice()) return;

    // Otherwise carry on with back function
    super.onBackPressed();

    // Clear any active notification and wake locks
    ClearAllReceiver.clearAll(this);

    // Flag message acknowledgment
    if (popupFragment != null) {
      SmsMmsMessage message = popupFragment.getMessage();
      if (message != null) message.acknowledge(this);
    }
  }

  /**
   * Display call alert for selected msg ID
   * @param msgId selected msg ID.
   */
  private void showAlert(int msgId) {
    SmsMmsMessage msg = SmsMessageQueue.getInstance().getMessage(msgId);
    if (msg != null) showAlert(msg);
  }

  /**
   * Display call details for selected message
   * @param message message to be displayed
   */
  public synchronized void showAlert(SmsMmsMessage message) {

    // Synchronization shouuld not be necessary since all calls have to be on the UI thread.
    // But we are getting occasional crashes that can seemingly only be explained by
    // concurrent calls to this method.  So, we will add the synchronization check

    if (popupFragment == null) popupFragment = new SmsPopupFragment();
    popupFragment.setMessage(message);

    FragmentManager fragmentManager = getSupportFragmentManager();
    if (fragmentManager.findFragmentByTag(CALL_ALERT_TAG) != null) return;

    FragmentTransaction ft = fragmentManager.beginTransaction();
    ft.addToBackStack(null);

    String mode = ManagePreferences.popupMode();
    if (mode.equals("P")) {
      popupFragment.show(ft, CALL_ALERT_TAG);
    } else {
      ft.replace(R.id.call_history_frag, popupFragment, CALL_ALERT_TAG).commit();
    }
  }

  /**
   * Close the alert detail display
   */
  public void closeAlertDetail() {
    if (splitScreen) {
      popupFragment.setMessage(null);
    } else {
      getSupportFragmentManager().popBackStack();
    }
  }

  /**
   * Delete requested message
   * @param msg message to be deleted
   */
  public void deleteMsg(@NonNull SmsMmsMessage msg) {

    //  Delete message from message queue
    SmsMessageQueue.getInstance().deleteMessage(msg);

    // If it happens to be the currently displayed message, close the message detail display
    if (popupFragment != null) {
      if (msg == popupFragment.getMessage()) closeAlertDetail();
    }
  }

  /**
   * Launch activity
   */
  public static void launchActivity(Context context, boolean notify, SmsMmsMessage msg) {
    Intent intent = getLaunchIntent(context, true, notify, msg);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
    context.startActivity(intent);
  }
  
  /**
   * Build intent to launch this activity
   * @param context current context
   * @return Intent that will launch Cadpage
   */
  public static Intent getLaunchIntent(Context context) {
    return getLaunchIntent(context, false, false, null);
  }

  /**
   * Build intent to launch this activity
   * @param context current context
   * @param force force detail popup window
   * @return Intent that will launch Cadpage
   */
  public static Intent getLaunchIntent(Context context, boolean force) {
    return getLaunchIntent(context, force, false, null);
  }

  /**
   * Build intent to launch this activity
   * @param context current context
   * @param force force detail popup window
   * @return Intent that will launch Cadpage
   */
  public static Intent getLaunchIntent(Context context, boolean force, boolean notify, SmsMmsMessage msg) {
    Intent intent = new Intent(context, CadPageActivity.class);
    int flags =
            Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_NO_USER_ACTION;

    intent.setFlags(flags);
    if (force) intent.putExtra(EXTRA_POPUP, true);
    if (notify) intent.putExtra(EXTRA_NOTIFY, true);
    if (msg != null) intent.putExtra(EXTRA_MSG_ID, msg.getMsgId());
    return intent;
  }

  public static boolean isInitializing() {
    return initializing;
  }
}
