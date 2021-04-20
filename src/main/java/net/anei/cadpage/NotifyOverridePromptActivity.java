package net.anei.cadpage;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
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
    if (!CadPageApplication.initialize(this)) {
      finish();
      return;
    };

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.notify_override_prompt);

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

