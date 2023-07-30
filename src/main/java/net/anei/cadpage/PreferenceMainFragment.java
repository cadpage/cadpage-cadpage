package net.anei.cadpage;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import net.anei.cadpage.donation.DeveloperToolsManager;
import net.anei.cadpage.donation.MainDonateEvent;
import net.anei.cadpage.preferences.LocationManager;
import net.anei.cadpage.vendors.VendorManager;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

public class PreferenceMainFragment extends PreferenceRestorableFragment implements LocationManager.Provider{

  private TwoStatePreference mEnabledPreference;
  private Preference locPreference;

  private LocationManager locMgr;

  @Override
  public LocationManager getLocationManager() {
    return locMgr;
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    locMgr = new LocationManager();

    // Load the preferences from an XML resource
    setPreferencesFromResource(R.xml.preference_main, rootKey);

    // Set up the payment status tracking screens
    Preference donate = findPreference(getString(R.string.pref_payment_status_key));
    MainDonateEvent.instance().setPreference(getActivity(), donate);

    // Set up the location description summary
    locPreference = findPreference(getString(R.string.pref_category_location_key));
    assert locPreference != null;
    locPreference.setSummaryProvider(locMgr.getSummaryProvider());
    enableLocPreference(locPreference, ManagePreferences.enableMsgType());

    // Save specific preferences we might need later
    mEnabledPreference = findPreference(getString(R.string.pref_enabled_key));

    // Add necessary permission checks
    Preference pref = findPreference(getString(R.string.pref_enable_msg_type_key));
    assert pref != null;
    pref.setOnPreferenceChangeListener((preference, newValue) -> {
      if (!ManagePreferences.checkPermEnableMsgType((ListPreference)preference, (String)newValue)) return false;
      enableLocPreference(locPreference, (String)newValue);
      return true;
    });

    pref = findPreference(getString(R.string.pref_category_notification_key));
    assert pref != null;
    pref.setSummaryProvider((pref2) -> {
      if (!ManagePreferences.notifyEnabled()) return "off";
      return getString(R.string.pref_on_for) + ' ' + displayTime(ManagePreferences.notifyTimeout()) +
             "; " + getString(R.string.pref_repeat) + ' ' + displayTime(ManagePreferences.notifyRepeatInterval());

    });

    // Set up summary displays
    pref = findPreference(getString(R.string.pref_category_scanner_key));
    assert pref != null;
    pref.setSummaryProvider((pref2) -> {
      String channel = ManagePreferences.scannerChannel();
      if (channel.equals("<None selected")) return channel;
      String auto = getString(ManagePreferences.activeScanner() ? R.string.auto : R.string.manual);
      return auto + " - " + channel;
    });

    // Disable the screen control options which are ignored starting in Android 10
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      deletePreference(R.string.pref_category_screen_control_key);
      PreferenceScreen ps = getPreferenceScreen();
      ps.setInitialExpandedChildrenCount(ps.getInitialExpandedChildrenCount()-1);
    }

    // Add developer dialog preference if appropriate
    DeveloperToolsManager.instance().addPreference(requireActivity(), getPreferenceScreen());
  }

  private void enableLocPreference(Preference locPreference, String enableMsgType) {
    boolean enable = enableMsgType.contains("S") || enableMsgType.contains("M") || VendorManager.instance().isLocationRequired();
    locPreference.setEnabled(enable);
  }

  private String displayTime(int secs) {
    if (secs <= 0) {
      return getString(R.string.pref_off);
    } else if (secs < 60) {
      return Integer.toString(secs) + ' ' + getString(R.string.pref_sec);
    } else {
      return Integer.toString(secs/60) + ' ' + getString(R.string.pref_min);
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    // Check for changes to values that are accessible from the widget
    mEnabledPreference.setChecked(ManagePreferences.enabled());

    // And a possible change to the location enabled status
    // (If user enabled or disabled the Cadpage paging service
    enableLocPreference(locPreference, ManagePreferences.enableMsgType());
  }

  @Override
  public void onDestroy() {
    MainDonateEvent.instance().setPreference(null, null);
    super.onDestroy();
  }

  /**
   * initialize all uninitialized preferences
   * @param context current context
   */
  public static void initializePreferences(Context context) {
    for (int prefId : PREF_ID_LIST) {
      PreferenceManager.setDefaultValues(context, prefId, true);
    }
  }

  private static final int[] PREF_ID_LIST = new int[]{
    R.xml.preference_main,
    R.xml.preference_location,
      R.xml.preference_location_filter,
      R.xml.preference_location_defaults,
    R.xml.preference_notification_old,
      R.xml.preference_notification_override,
    R.xml.preference_screen_control,
    R.xml.preference_call_history,
    R.xml.preference_call_detail,
    R.xml.preference_mapping,
    R.xml.preference_direct,
      R.xml.preference_direct_location,
    R.xml.preference_support,
    R.xml.preference_other_info,
    R.xml.preference_filter,
    R.xml.preference_scanner_radio,
    R.xml.preference_button,
      R.xml.preference_button_main,
      R.xml.preference_button_response,
    R.xml.preference_msg_processing,
    R.xml.preference_split_merge_options,
  };
}
