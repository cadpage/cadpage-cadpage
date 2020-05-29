package net.anei.cadpage;


import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import android.os.Bundle;
import android.provider.Settings;

import net.anei.cadpage.preferences.ExtendedSwitchPreference;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

public class PreferenceNotificationOverrideFragment extends PreferenceFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    // Load the preferences from an XML resource

    addPreferencesFromResource(R.xml.preference_notification_override);

    final TwoStatePreference overrideSoundPref = findPreference(getString(R.string.pref_notif_override_sound_key));
    assert overrideSoundPref != null;
    overrideSoundPref.setOnPreferenceChangeListener((preference, newValue) -> ManagePreferences.checkOverrideNotifySound((TwoStatePreference) preference, (Boolean)newValue));

    ExtendedSwitchPreference mNotifOverridePreference = findPreference(getString(R.string.pref_notif_override_key));
    assert mNotifOverridePreference != null;
    mNotifOverridePreference.setOnDataChangeListener(preference -> ManagePreferences.checkOverrideNotifySound(overrideSoundPref));
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
      if (existingValue.length() == 0) {
        // Select "Silent"
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
      } else {
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
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
}
