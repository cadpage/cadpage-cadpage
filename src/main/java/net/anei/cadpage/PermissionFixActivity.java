package net.anei.cadpage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Dummy activity whose only job is to request SMS_READ permission if MMS message processing
 * has been enabled.  MMS_RECEIVE has already been granted, so no user prompts will actually be
 * made, and this activity shuts down before the user actually gets to see anything.  But we need
 * an activity to make the request, and this is going to be it.
 */
public class PermissionFixActivity extends AppCompatActivity {

  private final PermissionManager permMgr = new PermissionManager(this);

  /* (non-Javadoc)
   * @see android.app.Activity#onCreate(android.os.Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (Log.DEBUG) Log.v("PermissionFixActivity: onCreate()");
    CadPageApplication.initialize(this);
    super.onCreate(savedInstanceState);

    // If initialization failure in progress, shut down without doing anything
    if (TopExceptionHandler.isInitFailure()) {
      finish();
      return;
    }
    
    ManagePreferences.setPermissionManager(permMgr);

    // checkInitialPermissions does the real work, we just call it
    ManagePreferences.checkInitialPermissions(null);

    // Once that is done, we can shut down
    finish();
  }

  @Override
  protected void onDestroy() {
    ManagePreferences.releasePermissionManager(permMgr);
    super.onDestroy();
  }

  public static void checkMMSPermission(Context context) {

    // Since we bumped the taget SDK level to 27, reading MMS messages requires SMS_READ permission
    // which was not needed before.  New logic checks for this situation and corrects it when the
    // main activity is started.  But that is too late to prevent a crash if an MMS message is
    // processed before launching the application.

    // To try and mitigate this sorry state of affairs, we listen for a broadcast that tells us
    // the app has been upgraded, check for the missing permission situation, and if detected,
    // launch this activity to fix things.  Once fixed, we shut down and no one is any wiser

    if (ManagePreferences.enableMsgType().contains("M") &&
        !PermissionManager.isGranted(context, PermissionManager.READ_SMS)) {
      Intent intent = new Intent(context, PermissionFixActivity.class);
      int flags = Intent.FLAG_ACTIVITY_NEW_TASK |
                  Intent.FLAG_ACTIVITY_SINGLE_TOP |
                  Intent.FLAG_ACTIVITY_CLEAR_TOP;
      intent.setFlags(flags);
      context.startActivity(intent);
    }
  }
}
