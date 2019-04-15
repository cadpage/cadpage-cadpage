package net.anei.cadpage;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

/**
 * Class handles the dialog popup reporting double audio alert configuration
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class NotifyOverridePromptActivity extends Safe40Activity {

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    if (Log.DEBUG) Log.v("NotifyOverridePromptActivity: onCreate()");
    CadPageApplication.initialize(this);

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.notify_override_prompt);

    // Extra text should only be displayed is vibrate setting is currently enabled
    View view8 = findViewById(R.id.NotifyOverrideExtra8Text);
    View view9 = findViewById(R.id.NotifyOverrideExtra9Text);
    if (ManageNotification.isVibrateEnabled(this)) {
      boolean v9 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
      view8.setVisibility(v9 ? View.GONE : View.VISIBLE);
      view9.setVisibility(v9 ? View.VISIBLE : View.GONE);
    } else {
      view8.setVisibility(View.GONE);
      view9.setVisibility(View.GONE);
    }

    // Cancel regular notification button
    // Opens chanel settings preferences so user can cancel the audio alert
    // We will check if this was actually done when we are resumed
    Button button = findViewById(R.id.NotifyOverrideCancelRegularBtn);
    button.setOnClickListener(new OnClickListener(){
      @Override
      public void onClick(View view) {
        PreferenceNotificationFragment.launchChannelConfig(NotifyOverridePromptActivity.this);
      }
    });

    // Cancel override alert button
    // Just turn of notify override preference and exit
    button = findViewById(R.id.NotifyOverrideCancelOverrideBtn);
    button.setOnClickListener(new OnClickListener(){
      @Override
      public void onClick(View view) {
        ManagePreferences.setNotifyOverride(false);
        finish();
      }
    });

    // Let it ride button just exits
    button = findViewById(R.id.NotifyOverrideGoodBtn);
    button.setOnClickListener(new OnClickListener(){
      @Override
      public void onClick(View view) {
        finish();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!ManageNotification.checkNotificationAlertConflict(this)) finish();
  }

  /**
   * Check if user has configured conflicting audio alerts, and if they have, launch
   * a dialog box to warn them about the situation
   * @param context - current context
   */
  static public void show(Context context) {
    Intent intent = new Intent(context, NotifyOverridePromptActivity.class);
    context.startActivity(intent);
  }
}

