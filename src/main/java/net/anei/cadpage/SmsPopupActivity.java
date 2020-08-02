package net.anei.cadpage;


import android.app.Activity;
import android.os.Bundle;
import net.anei.cadpage.donation.DonationManager;
import net.anei.cadpage.donation.MainDonateEvent;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;


public class SmsPopupActivity extends AppCompatActivity {
  
  private static final String EXTRAS_MSG_ID = "SmsPopupActivity.MSG_ID";
  
  private final PermissionManager permMgr = new PermissionManager(this);

  private SmsPopupFragment fragment = null;

  @Override
  protected void onCreate(Bundle bundle) {
    Log.v("SmsPopupActivity.onCreate()");

    super.onCreate(bundle);
    if (!CadPageApplication.initialize(this)) {
      finish();
      return;
    }
    setContentView(R.layout.sms_popup);

    ActionBar actionBar = getSupportActionBar();
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.cadpage_alert);

    ManagePreferences.setPermissionManager(permMgr);

    fragment = new SmsPopupFragment();
    getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.sms_popup, fragment)
            .commit();

    processIntent(getIntent());
  }

  @Override
  public boolean onSupportNavigateUp() {
    finish();
    return true;
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    setIntent(intent);

    processIntent(intent);

  }

  @Override
  protected void onDestroy() {
    MainDonateEvent.instance().setButton(null, null, null);
    ManagePreferences.releasePermissionManager(permMgr);
    super.onDestroy();
  }
  
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] granted) {
    ManagePreferences.onRequestPermissionsResult(requestCode, permissions, granted);
  }

  // Populate views from intent
  private void processIntent(Intent intent) {

    // Log startup intent
    ContentQuery.dumpIntent(intent);

    // Check to see if Cadpage is operating in restricted mode, and if it is
    // launch the donation status menu.  We'll check the donation status again
    // when this menu is closed
    if (!DonationManager.instance().isEnabled()) {
      MainDonateEvent.instance().doEvent(this, null);
    }

    if (fragment == null) return;

    fragment.setMsgId(intent.getIntExtra(EXTRAS_MSG_ID, -1));
  }

  /*
   * Handle results of Donation status menu
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    // If Cadpage is still restricted, close activity
    if (!DonationManager.instance().isEnabled()) finish();
  }

  /**
   * Launch call display popup activity
   * @param activity parent activity
   * @param message message to be displayed
   */
  public static void launchActivity(Activity activity, SmsMmsMessage message) {
    launchActivity(activity, message.getMsgId());
  }
  
  /**
   * Launch call display popup activity
   * @param activity parent activity
   * @param msgId message ID of message to be displayed
   */
  public static void launchActivity(Activity activity, int msgId) {
    Intent popup = new Intent(activity, SmsPopupActivity.class);
    popup.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    popup.putExtra(EXTRAS_MSG_ID, msgId);
    ContentQuery.dumpIntent(popup);
    activity.startActivityForResult(popup, 0);
  }
}

