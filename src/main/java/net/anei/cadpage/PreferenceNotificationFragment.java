package net.anei.cadpage;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.TwoStatePreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;

import net.anei.cadpage.preferences.ExtendedSwitchPreference;
import net.anei.cadpage.preferences.OnDataChangeListener;

public class PreferenceNotificationFragment extends PreferenceFragment {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      addPreferencesFromResource(R.xml.preference_notification);
    } else {
      addPreferencesFromResource(R.xml.preference_notification_old);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Preference pref = findPreference(getString(R.string.pref_notif_config_key));
      pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public boolean onPreferenceClick(Preference preference) {
          Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
          intent.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
          intent.putExtra(Settings.EXTRA_CHANNEL_ID, ManageNotification.ALERT_CHANNEL_ID);
          startActivity(intent);
          return true;
        }
      });
    }

    final TwoStatePreference overrideSoundPref = (TwoStatePreference)findPreference(getString(R.string.pref_notif_override_sound_key));
    overrideSoundPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        return ManagePreferences.checkOverrideNotifySound((TwoStatePreference) preference, (Boolean)newValue);
      }
    });

    ExtendedSwitchPreference notifyOverridePref = (ExtendedSwitchPreference)findPreference(getString(R.string.pref_notif_override_key));
    notifyOverridePref.setOnDataChangeListener(new OnDataChangeListener(){
      @Override
      public void onDataChange(Preference preference) {
        ManagePreferences.checkOverrideNotifySound(overrideSoundPref);
      }
    });
  }
}
