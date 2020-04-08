package net.anei.cadpage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.annotation.NonNull;
import android.view.KeyEvent;

@SuppressWarnings("SimplifiableIfStatement")
public class SmsPopupConfigActivity extends AppCompatActivity
  implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback{

  public static final String EXTRA_SELECT_LOCATION = "PreferenceActivity.SELECT_LOCATION";

  private final PermissionManager permMgr = new PermissionManager(this);

  @Override
  public void onCreate(Bundle savedInstanceState) {

    if (!CadPageApplication.initialize(this)) {
      finish();
      return;
    }

    super.onCreate(savedInstanceState);

    ManagePreferences.setPermissionManager(permMgr);

    setContentView(R.layout.settings);

    boolean selectLocation = getIntent().getBooleanExtra(EXTRA_SELECT_LOCATION, false);
    PreferenceFragment fragment = selectLocation ? new PreferenceLocationMenuFragment() : new PreferenceMainFragment();
    getSupportFragmentManager()
      .beginTransaction()
      .replace(R.id.settings_content, fragment)
      .commit();
  }

  @Override
  public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {

    // Instantiate the new Fragment
    final Bundle args = pref.getExtras();
    final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
      getClassLoader(),
      pref.getFragment());
    fragment.setArguments(args);
    fragment.setTargetFragment(caller, 0);

    // Replace the existing Fragment with the new Fragment
    getSupportFragmentManager().beginTransaction()
      .replace(R.id.settings_content, fragment)
      .addToBackStack(null)
      .commit();
    return true;
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
   * Open main settings menu
   * @param context current context
   */
  public static void launchSettings(Context context) {
    Intent intent = new Intent(context, SmsPopupConfigActivity.class);
    context.startActivity(intent);
  }

  /**
   * Launch the Select Single Location setting
   * @param activity current activity
   */
  public static void selectLocation(Activity activity) {
    Intent intent = new Intent(activity, SmsPopupConfigActivity.class);
    intent.putExtra(EXTRA_SELECT_LOCATION, true);
    activity.startActivityForResult(intent, 0);
  }
}