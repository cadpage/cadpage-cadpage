package net.anei.cadpage;

import net.anei.cadpage.billing.BillingManager;
import net.anei.cadpage.donation.Active911WarnEvent;
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
import android.content.pm.ActivityInfo;
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

public class CadPageActivity extends AppCompatActivity {

  private static final String EXTRA_NOTIFY = "net.anei.cadpage.CadPageActivity.NOTIFY";
  private static final String EXTRA_POPUP = "net.anei.cadpage.CadPageActivity.POPUP";
  private static final String EXTRA_MSG_ID = "net.anei.cadpage.CadPageActivity.MSG_ID";

  private final PermissionManager permMgr = new PermissionManager(this);

  private static boolean initializing = false;

  private static int lockMsgId = -1;

  private boolean needSupportApp;

  /* (non-Javadoc)
   * @see android.app.Activity#onCreate(android.os.Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (Log.DEBUG) Log.v("CadPageActivity: onCreate()");
    super.onCreate(savedInstanceState);
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
    ManagePreferences.setScreenSize(""+width+"X"+height);

    setContentView(R.layout.cadpage);

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

    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction ft = fragmentManager.beginTransaction();
    Fragment fragment = new CallHistoryFragment();
    ft.replace(R.id.cadpage_content, fragment);
    ft.commit();

    startup();
  }

  /* (non-Javadoc)
   * @see android.app.Activity#onNewIntent(android.content.Intent)
   */
  @Override
  protected void onNewIntent(Intent intent) {
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
    
    // We have an occasional problem when an old intent is sent with the
    // intent of restoring the Cadpage display.  But has several unfortunate
    // side effects like immediately cancelling the wake lock
    // Solution - when were were started with the intent of displaying a new
    // message, ignore all extraneous intents until we find the one that
    // displays the new page
    int msgId = intent.getIntExtra(EXTRA_MSG_ID, -1);
    if (msgId > 0 && msgId != lockMsgId) {
      Log.v("Discarding spurious intent");
      return;
    }
    lockMsgId = -1;

    // See if this request is going to pop up an alert window
    SmsMmsMessage msg;
    boolean force = ManagePreferences.forcePopup();
    if (force) ManagePreferences.setForcePopup(false);
    if (!force &&
        Intent.ACTION_MAIN.equals(intent.getAction()) &&
        intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
      msg = null;
    } else if (msgId >= 0) {
      msg = SmsMessageQueue.getInstance().getMessage(msgId);
    } else {
      if (!force) force = intent.getBooleanExtra(EXTRA_POPUP, false);
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
        if (Active911WarnEvent.instance().launch(CadPageActivity.this)) return;

        // Otherwise, launch the release info dialog if it hasn't already been displayed
        String oldRelease = ManagePreferences.release();
        String release = CadPageApplication.getVersion();
        if (!release.equals(oldRelease)) {
          ManagePreferences.setRelease(release);
          if (!trimRelease(release).equals(trimRelease(oldRelease))) {
            new ReleaseDialogFragment(this).show(getSupportFragmentManager(), "release");
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

    private final Activity activity;

    public ReleaseDialogFragment(Activity activity) {
      this.activity = activity;
    }

    @Override
    @NonNull public Dialog onCreateDialog(Bundle bundle) {
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
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    activityActive = true;

    // If we **REALLY** need the support app, and we asked the user
    // to install it, make sure that it has been installed and opened and
    // everything is OK.
    if (!BuildConfig.MSG_ALLOWED && needSupportApp) {
      needSupportApp = SmsPopupUtils.checkMsgSupport(this) > 0;
    }
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
  protected void onSaveInstanceState(Bundle outState) {

    outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
    super.onSaveInstanceState(outState);
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

  SmsPopupFragment popupFragment = null;
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
      popupFragment = null;
    }
  }

  public void showAlert(SmsMmsMessage message) {

    if (popupFragment ==  null) popupFragment = new SmsPopupFragment();
    popupFragment.setMessage(message);

    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction ft = fragmentManager.beginTransaction();
    ft.addToBackStack(null);

    String mode = ManagePreferences.popupMode();
    if (mode.equals("P")) {
      popupFragment.show(ft, "sms_popup");
    } else {
      ft.replace(R.id.cadpage_content, popupFragment).commit();
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
    if (msg != null) {
      lockMsgId = msg.getMsgId();
      intent.putExtra(EXTRA_MSG_ID, lockMsgId);
    }
    return intent;
  }

  public static boolean isInitializing() {
    return initializing;
  }
}
