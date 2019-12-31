package net.anei.cadpage;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.TwoStatePreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import androidx.annotation.RequiresApi;

import net.anei.cadpage.donation.CheckPopupEvent;
import net.anei.cadpage.preferences.DoNotDisturbSwitchPreference;
import net.anei.cadpage.preferences.ExtendedSwitchPreference;
import net.anei.cadpage.preferences.NewVibrateSwitchPreference;
import net.anei.cadpage.preferences.OnDataChangeListener;

public class PreferenceNotificationFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

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
      pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public boolean onPreferenceClick(Preference preference) {
          launchChannelConfig(getActivity());
          return true;
        }
      });

      mNewVibrateSwitchPreference = (NewVibrateSwitchPreference)findPreference(getString(R.string.pref_vibrate_key));
    }

    final TwoStatePreference overrideSoundPref = (TwoStatePreference)findPreference(getString(R.string.pref_notif_override_sound_key));
    overrideSoundPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        return ManagePreferences.checkOverrideNotifySound((TwoStatePreference) preference, (Boolean)newValue);
      }
    });

    mNotifEnabledPreference = (TwoStatePreference)findPreference(getString(R.string.pref_notif_enabled_key));
    mNotifOverridePreference = (ExtendedSwitchPreference)findPreference(getString(R.string.pref_notif_override_key));
    mNotifOverridePreference.setOnDataChangeListener(new OnDataChangeListener(){
      @Override
      public void onDataChange(Preference preference) {
        ManagePreferences.checkOverrideNotifySound(overrideSoundPref);
      }
    });

    mDoNotDisturbSwitchPreference = (DoNotDisturbSwitchPreference)findPreference(getString(R.string.pref_notif_override_do_not_disturb_key));

    ManagePreferences.registerOnSharedPreferenceChangeListener(this);
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
  public void onSaveInstanceState(Bundle outState) {
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
