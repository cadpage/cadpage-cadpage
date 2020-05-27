package net.anei.cadpage;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.TwoStatePreference;
import androidx.preference.Preference;
import android.provider.Settings;
import androidx.annotation.RequiresApi;

import net.anei.cadpage.donation.CheckPopupEvent;
import net.anei.cadpage.preferences.NewVibrateSwitchPreference;

import java.util.Objects;

public class PreferenceNotificationFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {


  private static final String ACCEPT_CONFLICT_KEY = "accept_conflict";

  private boolean acceptConflict = false;

  private TwoStatePreference mNotifEnabledPreference;
  private NewVibrateSwitchPreference mNewVibrateSwitchPreference = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      addPreferencesFromResource(R.xml.preference_notification);
    } else {
      addPreferencesFromResource(R.xml.preference_notification_old);
    }

    // If a double alert sound conflict already exists, we will not continue warning users about it
    if (savedInstanceState != null) {
      acceptConflict = savedInstanceState.getBoolean(ACCEPT_CONFLICT_KEY, false);
    } else {
      acceptConflict = ManageNotification.checkNotificationAlertConflict(getActivity());
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Preference pref = findPreference(getString(R.string.pref_notif_config_key));
      assert pref != null;
      pref.setOnPreferenceClickListener(preference -> {
        launchChannelConfig(Objects.requireNonNull(getActivity()));
        return true;
      });

      mNewVibrateSwitchPreference = findPreference(getString(R.string.pref_vibrate_key));
    }

    mNotifEnabledPreference = findPreference(getString(R.string.pref_notif_enabled_key));

    Preference pref = findPreference(getString(R.string.pref_notif_override_category_key));
    assert pref != null;
    pref.setSummaryProvider((pref2) -> {
      if (!ManagePreferences.notifyOverride()) return "Off";
      return "On; Max Vol " + (ManagePreferences.notifyOverrideVolume() ? "On" : "Off") +
             "; Loop " +
             (ManagePreferences.notifyOverrideLoop() ? "On" : "Off");
    });

    ManagePreferences.registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    // Check if we should bring up the double audio alert warning
    checkNotificationAlertConflict();
  }

  @Override
  public void onResume() {
    super.onResume();

    // Check if we should bring up the double audio alert warning
    checkNotificationAlertConflict();

    // And make sure popup enabled configurations are consistent
    CheckPopupEvent.instance().launch(getActivity());

    // If any setting have changed, make sure that the correct value is being displayed
    mNotifEnabledPreference.setChecked(ManagePreferences.notifyEnabled());
    if (mNewVibrateSwitchPreference != null) mNewVibrateSwitchPreference.refresh();
  }

  private void checkNotificationAlertConflict() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
    Context context = getActivity();
    boolean conflict = ManageNotification.checkNotificationAlertConflict(context);
    if (conflict && !acceptConflict) {
      NotifyOverridePromptActivity.show(context);
    }
    acceptConflict = conflict;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(ACCEPT_CONFLICT_KEY, acceptConflict);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    ManagePreferences.unregisterOnSharedPreferenceChangeListener(this);
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  public static void launchChannelConfig(Context context) {
    Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
    intent.putExtra(Settings.EXTRA_CHANNEL_ID, ManageNotification.ALERT_CHANNEL_ID);
    context.startActivity(intent);
  }
}
