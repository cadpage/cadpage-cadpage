package net.anei.cadpage.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import net.anei.cadpage.ManageNotification;
import net.anei.cadpage.NotifyVibratePromptActivity;

@TargetApi(Build.VERSION_CODES.O_MR1)
public class NewVibrateSwitchPreference extends SwitchPreference {

  public NewVibrateSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public NewVibrateSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public NewVibrateSwitchPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public NewVibrateSwitchPreference(Context context) {
    super(context);
  }

  @Override
  protected void onClick() {
    NotifyVibratePromptActivity.show(getContext());
  }

  public void refresh() {
    setChecked(ManageNotification.isVibrateEnabled(getContext()));
  }
}
