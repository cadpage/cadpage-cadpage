package net.anei.cadpage;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.TwoStatePreference;
import androidx.preference.Preference;
import android.provider.Settings;
import androidx.annotation.RequiresApi;

import net.anei.cadpage.donation.CheckPopupEvent;
import net.anei.cadpage.preferences.DoNotDisturbSwitchPreference;
import net.anei.cadpage.preferences.ExtendedSwitchPreference;
import net.anei.cadpage.preferences.NewVibrateSwitchPreference;

import java.util.Objects;

public class PreferenceNotificationFragment extends PreferenceRestorableFragment implements SharedPreferences.OnSharedPreferenceChangeListener {


  private static final String ACCEPT_CONFLICT_KEY = "accept_conflict";

  private boolean acceptConflict = false;

  private TwoStatePreference mNotifEnabledPreference;
  private ExtendedSwitchPreference mNotifOverridePreference;
  private NewVibrateSwitchPreference mNewVibrateSwitchPreference = null;
  private DoNotDisturbSwitchPreference mDoNotDisturbSwitchPreference;

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

    final TwoStatePreference overrideSoundPref = findPreference(getString(R.string.pref_notif_override_sound_key));
    assert overrideSoundPref != null;
    overrideSoundPref.setOnPreferenceChangeListener((preference, newValue) -> ManagePreferences.checkOverrideNotifySound((TwoStatePreference) preference, (Boolean)newValue));

    mNotifEnabledPreference = findPreference(getString(R.string.pref_notif_enabled_key));
    mNotifOverridePreference = findPreference(getString(R.string.pref_notif_override_key));
    assert mNotifOverridePreference != null;
    mNotifOverridePreference.setOnDataChangeListener(preference -> ManagePreferences.checkOverrideNotifySound(overrideSoundPref));

    // Remove DoNotDisturb setting if it is not applicable
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      deletePreference(R.string.pref_notif_override_do_not_disturb_key);
    } else {
      mDoNotDisturbSwitchPreference = findPreference(getString(R.string.pref_notif_override_do_not_disturb_key));
    }

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
    mNotifOverridePreference.setChecked(ManagePreferences.notifyOverride());
    if (mDoNotDisturbSwitchPreference != null) mDoNotDisturbSwitchPreference.refresh();
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

  private static final int REQUEST_CODE_ALERT_RINGTONE = 9991;

  @Override
  public boolean onPreferenceTreeClick(Preference preference) {
    if (preference.getKey().equals(getString(R.string.pref_notif_sound_key))) {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, preference.getTitle());
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);

        String existingValue = ManagePreferences.notifySound();
        if (existingValue != null) {
            if (existingValue.length() == 0) {
              // Select "Silent"
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
            } else {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
            }
        } else {
          // No ringtone has been selected, set to the default
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
        }

        startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE);
        return true;
    } else {
        return super.onPreferenceTreeClick(preference);
    }
}

@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
        Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        String ringtoneStr = ringtone != null ? ringtone.toString() : "";
        ManagePreferences.setNotifySound(ringtoneStr);
    } else {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
  @RequiresApi(api = Build.VERSION_CODES.O)
  public static void launchChannelConfig(Context context) {
    Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
    intent.putExtra(Settings.EXTRA_CHANNEL_ID, ManageNotification.ALERT_CHANNEL_ID);
    context.startActivity(intent);
  }
}
