package net.anei.cadpage;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import net.anei.cadpage.donation.CheckPopupEvent;
import net.anei.cadpage.preferences.DoNotDisturbSwitchPreference;
import net.anei.cadpage.preferences.ExtendedSwitchPreference;
import net.anei.cadpage.preferences.NewVibrateSwitchPreference;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

public class PreferenceNotificationOverrideFragment extends PreferenceFragment {

  private ExtendedSwitchPreference mNotifOverridePreference;
  private DoNotDisturbSwitchPreference mDoNotDisturbSwitchPreference;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource

    addPreferencesFromResource(R.xml.preference_notification_override);

    final TwoStatePreference overrideSoundPref = findPreference(getString(R.string.pref_notif_override_sound_key));
    assert overrideSoundPref != null;
    overrideSoundPref.setOnPreferenceChangeListener((preference, newValue) -> ManagePreferences.checkOverrideNotifySound((TwoStatePreference) preference, (Boolean)newValue));

    mNotifOverridePreference = findPreference(getString(R.string.pref_notif_override_key));
    assert mNotifOverridePreference != null;
    mNotifOverridePreference.setOnDataChangeListener(preference -> ManagePreferences.checkOverrideNotifySound(overrideSoundPref));

    // Remove DoNotDisturb setting if it is not applicable
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      deletePreference(R.string.pref_notif_override_do_not_disturb_key);
    } else {
      mDoNotDisturbSwitchPreference = findPreference(getString(R.string.pref_notif_override_do_not_disturb_key));
    }
  }
}
