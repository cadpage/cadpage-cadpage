package net.anei.cadpage.donation;

import net.anei.cadpage.CadPageApplication;
import net.anei.cadpage.ManageBluetooth;
import net.anei.cadpage.ManagePreferences;
import net.anei.cadpage.PermissionManager;
import net.anei.cadpage.SmsMessageQueue;
import net.anei.cadpage.SmsMmsMessage;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Window;

public class DonateActivity extends Activity {
  
  private static final String EXTRA_SCREEN_NAME = "net.anei.cadpage.DonateActivity.SCREEN_NAME";
  private static final String EXTRA_MSG_ID =      "net.anei.cadpage.DonateActivity.MSG_ID";

  private final PermissionManager permMgr = new PermissionManager(this);

  private DonateScreenBaseEvent event;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    CadPageApplication.initialize(this);
    ManagePreferences.setPermissionManager(permMgr);
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    String classname = getIntent().getStringExtra(EXTRA_SCREEN_NAME);
    event = DonateScreenEvent.getScreenEvent(classname);
    int msgId = getIntent().getIntExtra(EXTRA_MSG_ID, -1);
    SmsMmsMessage msg = msgId<0 ? null : SmsMessageQueue.getInstance().getMessage(msgId);
    event.create(this, msg);
  }

  public void switchEvent(DonateScreenBaseEvent event, SmsMmsMessage msg) {
    this.event = event;
    event.create(this, msg);
  }

  @Override
  protected void onStart() {
    super.onStart();
    event.onStart(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (resultCode >= ManageBluetooth.BLUETOOTH_REQ) {
      if (ManageBluetooth.instance().onActivityResult(this, requestCode, resultCode)) return;
    }
    
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == DonateEvent.RESULT_CLOSE_ALL) {
      setResult(DonateEvent.RESULT_CLOSE_ALL);
      finish();
    }
    else {
      event.followup(this, requestCode, resultCode, data);
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (isFinishing()) return null;
    return event.createDialog(this, id);
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
   * Create intent that can be used to launch this activity
   * @param context current context
   * @param event event to to displayed
   * @param msg message associated with this event
   */
  public static void launchActivity(Context context, DonateScreenBaseEvent event, SmsMmsMessage msg) {

    // See if event should short circuit screen activity
    if (context instanceof Activity) {
      if (!event.launchActivity((Activity)context)) return;
    }

    Intent popup = new Intent(context, DonateActivity.class);
    popup.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    popup.putExtra(EXTRA_SCREEN_NAME, event.getClass().getName());
    if (msg != null) popup.putExtra(EXTRA_MSG_ID, msg.getMsgId());
    if (context instanceof Activity) {
      ((Activity)context).startActivityForResult(popup, 0);
    }
    else {
      popup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(popup);
    }
  }
}
