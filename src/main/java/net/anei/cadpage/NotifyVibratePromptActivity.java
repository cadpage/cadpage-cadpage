package net.anei.cadpage;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/**
 * Class handles the dialog popup reporting double audio alert configuration
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class NotifyVibratePromptActivity extends Safe40Activity {

  private boolean vibrateStatus;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    if (Log.DEBUG) Log.v("NotifyOverridePromptActivity: onCreate()");
    if (!CadPageApplication.initialize(this)) {
      finish();
      return;
    };

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    WindowCompat.enableEdgeToEdge(getWindow());
    setContentView(R.layout.notify_vibrate_prompt);

    View view = findViewById(android.R.id.content);
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
      Insets ins = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
      v.setPadding(ins.left, ins.top, ins.right, ins.bottom);
      return WindowInsetsCompat.CONSUMED;
    });

    // Save the current vibrate status
    vibrateStatus = ManageNotification.isVibrateEnabled(this);

    // Text varies depending on the current vibrate status and
    // whether notification override is enabled
    boolean v9 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    int textId;

    if (vibrateStatus) {
      textId = v9 ? R.string.notify_vibrate_prompt_off_v9_text : R.string.notify_vibrate_prompt_off_v8_text;
    } else if (!ManagePreferences.notifyOverride()) {
      textId = v9 ? R.string.notify_vibrate_prompt_on_v9_text : R.string.notify_vibrate_prompt_on_v8_text;
    } else {
      textId = v9 ? R.string.notify_vibrate_prompt_on_override_v9_text : R.string.notify_vibrate_prompt_on_override_v8_text;
    }
    TextView tview = findViewById(R.id.NotifyVibrateText);
    tview.setText(textId);

    // go do it button
    // Opens chanel settings preferences so user change the appropriate settings
    Button button = findViewById(R.id.NotifyVibrateRegularBtn);
    button.setOnClickListener(new OnClickListener(){
      @Override
      public void onClick(View view) {
        PreferenceNotificationFragment.launchChannelConfig(NotifyVibratePromptActivity.this);
      }
    });

    // Let it ride button just exits
    button = findViewById(R.id.NotifyVibrateCancelBtn);
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

    // If user succeeded in changing vibrate status, exit
    if (ManageNotification.isVibrateEnabled(this) != vibrateStatus) finish();
  }

  /**
   * Prompt user with instructions on how to change the vibrate setting
   * @param context - current context
   */
  static public void show(Context context) {
    Intent intent = new Intent(context, NotifyVibratePromptActivity.class);
    context.startActivity(intent);
  }
}

