package net.anei.cadpage.preferences;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.preference.SwitchPreference;
import android.provider.Settings;
import android.util.AttributeSet;

@TargetApi(Build.VERSION_CODES.O_MR1)
public class DoNotDisturbSwitchPreference extends SwitchPreference {

  public DoNotDisturbSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public DoNotDisturbSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public DoNotDisturbSwitchPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public DoNotDisturbSwitchPreference(Context context) {
    super(context);
  }

  @Override
  protected void onClick() {
    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
    getContext().startActivity(intent);
  }

  public void refresh() {
    NotificationManager nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
    assert nm != null;
    setChecked(nm.isNotificationPolicyAccessGranted());
  }
}
