package net.anei.cadpage;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

public class PreferenceButtonResponseBtnFragment extends PreferenceRestorableFragment {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    Bundle args = getArguments();
    if (args == null) throw new RuntimeException("No arguments passed to PreferenceButtonResponseBtnFragment");
    final int button = args.getInt("button", -1);
    if (button < 0) throw new RuntimeException("No button defined for PreferenceButtonResponseBtnFragment");

    String defaultDesc = button == 1 ? getString(R.string.responding_text) :
                         button == 2 ? getString(R.string.not_responding_text) : "";

    Context context = getPreferenceManager().getContext();
    PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

    // Set up all of the preferences
    final ListPreference typePref = new net.anei.cadpage.preferences.ListPreference(context);
    typePref.setKey(getString(CALLBACK_TYPE_KEYS[button-1]));
    typePref.setTitle(getString(R.string.pref_resp_type_title));
    typePref.setSummary(getString(R.string.pref_resp_type_summary));
    typePref.setEntries(R.array.pref_resp_type_text);
    typePref.setEntryValues(R.array.pref_resp_type_values);
    typePref.setDefaultValue("");
    screen.addPreference(typePref);

    final EditTextPreference descPref = new net.anei.cadpage.preferences.EditTextPreference(context);
    descPref.setKey(getString(CALLBACK_TITLE_KEYS[button-1]));
    descPref.setTitle(getString(R.string.pref_desc_callback_title));
    descPref.setSummary(getString(R.string.pref_button_summary));
    descPref.setDefaultValue(defaultDesc);
    screen.addPreference(descPref);

    final EditTextPreference codePref = new net.anei.cadpage.preferences.EditTextPreference(context);
    codePref.setKey(getString(CALLBACK_KEYS[button-1]));
    codePref.setTitle(getString(R.string.pref_code_callback_title));
    codePref.setSummary(getString(R.string.pref_button_summary));
    codePref.setDialogMessage(R.string.pref_code_callback_msg);
    screen.addPreference(codePref);

    setPreferenceScreen(screen);

    // Code field is only enabled if response type is set to something
    codePref.setEnabled(typePref.getValue().length() > 0);
    typePref.setOnPreferenceChangeListener((preference, value) -> {

      // Check if we have appropriate permission
      if (!ManagePreferences.checkResponseType(button, (ListPreference)preference, value.toString())) return false;

      codePref.setEnabled(value.toString().length() > 0);
      return true;
    });
  }
  
  private static final int[] CALLBACK_TYPE_KEYS = new int[]{
    R.string.pref_callback1_type_key,
    R.string.pref_callback2_type_key,
    R.string.pref_callback3_type_key,
    R.string.pref_callback4_type_key,
    R.string.pref_callback5_type_key,
    R.string.pref_callback6_type_key,
  };

  private static final int[] CALLBACK_TITLE_KEYS = new int[]{
    R.string.pref_callback1_title_key,
    R.string.pref_callback2_title_key,
    R.string.pref_callback3_title_key,
    R.string.pref_callback4_title_key,
    R.string.pref_callback5_title_key,
    R.string.pref_callback6_title_key,
  };

  private static final int[] CALLBACK_KEYS = new int[]{
    R.string.pref_callback1_key,
    R.string.pref_callback2_key,
    R.string.pref_callback3_key,
    R.string.pref_callback4_key,
    R.string.pref_callback5_key,
    R.string.pref_callback6_key,
  };
}
