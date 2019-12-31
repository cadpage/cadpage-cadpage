package net.anei.cadpage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.view.KeyEvent;

import java.util.List;

@SuppressWarnings("SimplifiableIfStatement")
public class SmsPopupConfigActivity extends PreferenceActivity {

  public static final String EXTRA_PREFERENCE = "PreferenceActivity.PREFERENCE";

  private final PermissionManager permMgr = new PermissionManager(this);

  @Override
  public void onCreate(Bundle savedInstanceState) {

    if (!CadPageApplication.initialize(this)) {
      finish();
      return;
    }

    super.onCreate(savedInstanceState);

    ManagePreferences.setPermissionManager(permMgr);
  }

  @Override
  public void onBuildHeaders(List<Header> target) {
    loadHeadersFromResource(R.xml.preference_headers, target);
  }

  @Override
  protected boolean isValidFragment(String fragmentName) {
    return fragmentName.startsWith("net.anei.cadpage.Preference") && fragmentName.endsWith("Fragment");
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

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    Log.v("SmsPopupConfigActivity:onActivityResult - Req:" + requestCode + "  Res:" + resultCode);
    if (data != null) ContentQuery.dumpIntent(data);

    if (resultCode >= ManageBluetooth.BLUETOOTH_REQ) {
      if (ManageBluetooth.instance().onActivityResult(this, requestCode, resultCode)) return;
    }
   
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onResume() {
    super.onResume();

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    activityActive = true; 
  }

  // This is all supposed to work around a bug causing crashes for
  // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
  
  private boolean activityActive = false;
  
  protected void onPause() {
    super.onPause();
    activityActive = false;
  } 
  
  public boolean onKeyUp(int keyCode, KeyEvent event)  {
     if (!activityActive) return false;
     return super.onKeyUp(keyCode, event);
  } 
  
  public boolean onKeyDown(int keyCode, KeyEvent event) { 
     if (!activityActive) return false;
     return super.onKeyDown(keyCode, event);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {

    outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
    super.onSaveInstanceState(outState);
  }

  /**
   * initialize all uninitialized preferences
   * @param context current context
   */
  public static void initializePreferences(Context context) {
    PreferenceManager.setDefaultValues(context, R.xml.preference_general, true);
    PreferenceManager.setDefaultValues(context, R.xml.preference_notification_old, true);
    PreferenceManager.setDefaultValues(context, R.xml.preference_additional, true);
    PreferenceManager.setDefaultValues(context, R.xml.preference_button, true);
    PreferenceManager.setDefaultValues(context, R.xml.preference_filter, true);
    PreferenceManager.setDefaultValues(context, R.xml.preference_location, true);
    PreferenceManager.setDefaultValues(context, R.xml.preference_direct, true);
    PreferenceManager.setDefaultValues(context, R.xml.preference_other_info, true);
  }

  /**
   * Launch the Select Single Location setting
   * @param activity current activity
   */
  public static void selectLocation(Activity activity) {
    Intent intent = new Intent(activity, SmsPopupConfigActivity.class);
    intent.putExtra(EXTRA_SHOW_FRAGMENT, PreferenceLocationFragment.class.getName());
    Bundle bundle = new Bundle();
    bundle.putInt(EXTRA_PREFERENCE, R.string.pref_location_tree_key);
    intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle);
    activity.startActivityForResult(intent, 0);
  }
}